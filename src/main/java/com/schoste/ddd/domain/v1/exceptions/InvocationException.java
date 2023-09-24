package com.schoste.ddd.domain.v1.exceptions;

import java.lang.reflect.Method;

/**
 * Indicates an error during method invocations via reflection
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class InvocationException extends DomainException
{
	private static final long serialVersionUID = 2580563565937648317L;
	private Object invoker;
	private Method invokedMethod;

	/**
	 * Gets the object on which the method was invoked
	 * 
	 * @return the object on which the method was invoked
	 */
	public Object getInvoker() { return this.invoker; }

	/**
	 * Gets the method that cause the exception
	 * 
	 * @return the method that cause the exception
	 */
	public Method getInvokedMethod() { return this.invokedMethod; }

	/**
	 * Creates a new instance of this exception for the causing method.
	 * 
	 * @param invoker the object on which the method is invoked
	 * @param invokedMethod the method that caused the exception
	 * @param inner the actual exception
	 */
	public InvocationException(Object invoker, Method invokedMethod, Exception inner)
	{	
		super(String.format("%s %s#%s", inner.getClass().getName(), (invoker == null) ? "null" : invoker.getClass().getName(), (invokedMethod == null) ? "null" : invokedMethod.getName()), inner);

		this.invoker = invoker;
		this.invokedMethod = invokedMethod;
	}
}
