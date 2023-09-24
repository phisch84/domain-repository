/**
 * 
 */
package com.schoste.ddd.domain.v1.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines the method which should be used to get the value which should be set
 * during model conversion. The annotated method (setter method) must accept only
 * one parameter. This one parameter will be obtained by the referred method (getter method).
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AutoSet
{
	/**
	 * Gets or sets the class which defines the method to use to get the value.
	 * If this value is null, the converter will try to find a class named {@see AutoConvert#className()}.
	 * If neither {@see AutoConvert#clazz()} nor {@see AutoConvert#className()} are defined,
	 * the converter will search the conversion methods in the instance of the source model.
	 * 
	 * @return a class
	 */
	Class<?> clazz() default Object.class;
	
	/**
	 * Gets or sets the name of the class which defines the method to use to get the value.
	 * If {@see AutoConvert#clazz()} is not null, this property is ignored.
	 * 
	 * @return a full class name (with package name)
	 */
	String className() default "";

	/**
	 * Gets or sets the name of the method to use to get the value (getter method).
	 * If this property is not defined, the converter will try to guess the conversion method itself.
	 * 
	 * @return a method name
	 */
	String methodName() default "";
}
