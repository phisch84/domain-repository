package com.schoste.ddd.domain.v3.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a setter of an entity which the repository calls when it converts a data object into the entity
 * (see {@link com.schoste.ddd.domain.v3.services.AutoObjectConverter}). The setter must be public and take exactly one parameter;
 * it gets the value returned by a getter without parameters.
 *
 * Which getter is called:
 * <ul>
 * <li>its name is {@link AutoSet#methodName()}, or, if that isn't defined, the setter's name with "set" replaced by "get"
 * (e.g. {@code getName()} for {@code setName(String)}). Getters starting with "is" aren't found automatically; name them by
 * {@link AutoSet#methodName()};</li>
 * <li>it is called on the data object, unless {@link AutoSet#clazz()} or {@link AutoSet#className()} names another class: then
 * it is called on an instance of that class (e.g. a class which provides a default value).</li>
 * </ul>
 *
 * This annotation is for entities, i.e. for the direction from data object to entity. For the opposite direction, setters of
 * data objects are annotated with {@code com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet} of the {@code dao4} library,
 * which has the same attributes; it is defined there because data objects belong to the infrastructure layer, which must not
 * depend on the domain layer.
 *
 * Example: the repository sets the entity's name and email from the data object's {@code getName()} and {@code getEmailAddress()}.
 * <pre>
 * public class Customer extends BasicEntity&lt;Long&gt;
 * {
 *     &#64;AutoSet
 *     public void setName(String name) { ... }
 *
 *     &#64;AutoSet(methodName = "getEmailAddress")
 *     public void setEmail(String email) { ... }
 * }
 * </pre>
 *
 * Setters which aren't annotated aren't called by the converter; set their values in
 * {@code GenericRepositoryImpl.doCompleteEntity(DataObject, Entity)} instead.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AutoSet
{
	/**
	 * Defines the class on whose instance the getter is called, instead of the data object.
	 * The instance is created by the converter (see {@link com.schoste.ddd.domain.v3.services.standard.AutoObjectConverterImpl}).
	 *
	 * @return the class, or {@link Object} if not defined
	 */
	Class<?> clazz() default Object.class;

	/**
	 * Defines the full name (with package) of the class on whose instance the getter is called, instead of the data object.
	 * Ignored if {@link AutoSet#clazz()} is defined.
	 *
	 * @return the full class name, or an empty string if not defined
	 */
	String className() default "";

	/**
	 * Defines the name of the getter. If it isn't defined, the getter's name is the setter's name with "set" replaced by "get".
	 *
	 * @return the getter's name, or an empty string if not defined
	 */
	String methodName() default "";
}
