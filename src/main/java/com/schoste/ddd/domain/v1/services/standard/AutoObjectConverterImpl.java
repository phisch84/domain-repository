package com.schoste.ddd.domain.v1.services.standard;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.domain.v1.exceptions.InvocationException;
import com.schoste.ddd.domain.v1.services.AutoObjectConverter;

/**
 * Implementation of the version 1 auto object converter
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
abstract public class AutoObjectConverterImpl implements AutoObjectConverter
{
	private class Getter
	{
		private Object invoker;
		private Method method;

		public Getter(Object invoker, Method method)
		{
			this.invoker = invoker;
			this.method = method;
		}

		public Object get(Object invoker) throws Exception
		{
			Object actualInvoker = (this.invoker != null) ? this.invoker : invoker;

			return this.method.invoke(actualInvoker);
		}
	}

	private class AutoSetInfo
	{
		private Class<?> clazz;
		private String className;
		private String methodName;

		public Class<?> getClazz() { return clazz; }
		public String getClassName() { return className; }
		public String getMethodName() { return methodName; }

		public AutoSetInfo(com.schoste.ddd.infrastructure.dal.v2.annotations.AutoSet autoSet)
		{
			this.clazz = autoSet.clazz();
			this.className = autoSet.className();
			this.methodName = autoSet.methodName();
		}

		public AutoSetInfo(com.schoste.ddd.domain.v1.annotations.AutoSet autoSet)
		{
			this.clazz = autoSet.clazz();
			this.className = autoSet.className();
			this.methodName = autoSet.methodName();
		}
	}

	protected Map<Class<?>, Map<Method, Getter>> conversionMethodMap = new HashMap<>();

	/**
	 * Gets the instance to an object for the given class
	 * 
	 * @param clazz the class to get the instance for
	 * @return an instance to an object of the given class
	 * @throws Exception re-throws every exception
	 */
	abstract protected Object getInstance(Class<?> clazz) throws Exception;

	/**
	 * Gets the instance to an object for the given class
	 * 
	 * @param className the full name of the class to get the instance for
	 * @return an instance to an object of the given class
	 * @throws Exception re-throws every exception
	 */
	abstract protected Object getInstance(String className) throws Exception;

	/**
	 * Computes and caches which methods should be called and setters and getters
	 * to convert instances of classes.
	 * 
	 * @param srcClass the class of the object to get values from
	 * @param dstClass the class of the object to set values
	 * @throws NoSuchMethodException thrown if the getter method cannot be determined
	 * @throws Exception re-throws every exception
	 */
	protected void buildConversionMethodMap(Class<?> srcClass, Class<?> dstClass) throws NoSuchMethodException, Exception
	{
		Method[] methods = dstClass.getMethods();
		Map<Method, Getter> methodMap = new HashMap<>();

		for (Method setterMethod : methods)
		{
			AutoSetInfo autoSetInfo = null;

			com.schoste.ddd.infrastructure.dal.v2.annotations.AutoSet dalAnnotation = setterMethod.getAnnotation(com.schoste.ddd.infrastructure.dal.v2.annotations.AutoSet.class);
			if (dalAnnotation != null) autoSetInfo = new AutoSetInfo(dalAnnotation);

			com.schoste.ddd.domain.v1.annotations.AutoSet domainAnnotation = setterMethod.getAnnotation(com.schoste.ddd.domain.v1.annotations.AutoSet.class);
			if (domainAnnotation != null) autoSetInfo = new AutoSetInfo(domainAnnotation);

			if (autoSetInfo == null) continue;

			Class<?> clazz = srcClass;
			Object invoker = null;

			if (!autoSetInfo.getClazz().equals(Object.class))
			{
				clazz = autoSetInfo.getClazz();
				invoker = this.getInstance(clazz);
			}
			else if (!autoSetInfo.getClassName().equals("")) 
			{
				invoker = this.getInstance(autoSetInfo.getClassName());
				clazz = invoker.getClass();
			}

			String getterMethodName = autoSetInfo.getMethodName();

			if (getterMethodName.equals(""))
			{
				String setterMethodName = setterMethod.getName();

				if (setterMethodName.startsWith("set")) getterMethodName = "get"+setterMethodName.substring(3);
				else getterMethodName = setterMethodName;
			}

			Method getterMethod = clazz.getMethod(getterMethodName);
			Getter getter = new Getter(invoker, getterMethod);

			methodMap.put(setterMethod, getter);
		}

		this.conversionMethodMap.put(srcClass, methodMap);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void convert(Object src, Object dst) throws InvocationException, DomainException
	{
		if (src == null) return;
		if (dst == null) return;

		Class<?> srcClass = src.getClass();

		try
		{
			if (!this.conversionMethodMap.containsKey(srcClass)) this.buildConversionMethodMap(srcClass, dst.getClass());
	
			Map<Method, Getter> methodMap = this.conversionMethodMap.get(srcClass);
			Collection<Method> setterMethods = methodMap.keySet();
	
			for (Method setterMethod : setterMethods)
			{
				Getter getter = methodMap.get(setterMethod);
				Object invoker = src;
				Method invokingMethod = getter.method;
	
				try
				{
					Object arg = getter.get(src);

					invoker = dst;
					invokingMethod = setterMethod;
					invokingMethod.invoke(dst, arg);
				}
				catch (Exception ex)
				{
					throw new InvocationException(invoker, invokingMethod, ex);
				}
			}
		}
		catch (InvocationException ie)
		{
			throw ie;
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
}
