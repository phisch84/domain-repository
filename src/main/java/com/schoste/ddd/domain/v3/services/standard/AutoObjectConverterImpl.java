package com.schoste.ddd.domain.v3.services.standard;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.exceptions.InvocationException;
import com.schoste.ddd.domain.v3.services.AutoObjectConverter;

/**
 * Standard implementation of the {@link AutoObjectConverter}, used by default by {@link GenericRepositoryImpl}.
 * It is thread-safe and ready to use without Spring.
 *
 * If an AutoSet annotation names a class ({@code clazz} or {@code className}), the converter calls the getter on an instance of
 * that class instead of the source object. The instance is created once per annotated setter by
 * {@link AutoObjectConverterImpl#getInstance(Class)} or {@link AutoObjectConverterImpl#getInstance(String)}, which use the
 * class's no-argument constructor by default. Override both methods to get the instances elsewhere, e.g. from Spring.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class AutoObjectConverterImpl implements AutoObjectConverter
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

		public AutoSetInfo(com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet autoSet)
		{
			this.clazz = autoSet.clazz();
			this.className = autoSet.className();
			this.methodName = autoSet.methodName();
		}

		public AutoSetInfo(com.schoste.ddd.domain.v3.annotations.AutoSet autoSet)
		{
			this.clazz = autoSet.clazz();
			this.className = autoSet.className();
			this.methodName = autoSet.methodName();
		}
	}

	/**
	 * The setters and their getters per pair of source and destination class ({@code Arrays.asList(srcClass, dstClass)})
	 */
	private final Map<List<Class<?>>, Map<Method, Getter>> conversionMethodMap = new ConcurrentHashMap<>();

	/**
	 * Creates a new converter
	 */
	public AutoObjectConverterImpl()
	{
	}

	/**
	 * Gets the instance of a class named by {@code AutoSet.clazz()}, on which the getter is called.
	 * By default, the instance is created by the class's no-argument constructor.
	 *
	 * @param clazz the class to get the instance for
	 * @return an instance of the given class
	 * @throws Exception re-throws every exception
	 */
	protected Object getInstance(Class<?> clazz) throws Exception
	{
		return clazz.getDeclaredConstructor().newInstance();
	}

	/**
	 * Gets the instance of a class named by {@code AutoSet.className()}, on which the getter is called.
	 * By default, the class is loaded by {@link Class#forName(String)} and the instance is created by its no-argument constructor.
	 *
	 * @param className the full name of the class to get the instance for
	 * @return an instance of the given class
	 * @throws Exception re-throws every exception
	 */
	protected Object getInstance(String className) throws Exception
	{
		return this.getInstance(Class.forName(className));
	}

	/**
	 * Computes which setters of the destination class are called with the values of which getters
	 * to convert instances of the source class into instances of the destination class.
	 *
	 * @param srcClass the class of the object to get values from
	 * @param dstClass the class of the object to set values
	 * @return the setters and their getters
	 * @throws NoSuchMethodException thrown if the getter method cannot be determined
	 * @throws Exception re-throws every exception
	 */
	private Map<Method, Getter> buildConversionMethodMap(Class<?> srcClass, Class<?> dstClass) throws NoSuchMethodException, Exception
	{
		Method[] methods = dstClass.getMethods();
		Map<Method, Getter> methodMap = new HashMap<>();

		for (Method setterMethod : methods)
		{
			AutoSetInfo autoSetInfo = null;

			com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet dalAnnotation = setterMethod.getAnnotation(com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet.class);
			if (dalAnnotation != null) autoSetInfo = new AutoSetInfo(dalAnnotation);

			com.schoste.ddd.domain.v3.annotations.AutoSet domainAnnotation = setterMethod.getAnnotation(com.schoste.ddd.domain.v3.annotations.AutoSet.class);
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

		return methodMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void convert(Object src, Object dst) throws InvocationException, DomainException
	{
		if (src == null) return;
		if (dst == null) return;

		List<Class<?>> classes = Arrays.asList(src.getClass(), dst.getClass());

		try
		{
			Map<Method, Getter> methodMap = this.conversionMethodMap.get(classes);

			// computed outside of the map, since computeIfAbsent() can't throw checked exceptions; computing twice is harmless
			if (methodMap == null)
			{
				methodMap = this.buildConversionMethodMap(src.getClass(), dst.getClass());

				this.conversionMethodMap.put(classes, methodMap);
			}

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
