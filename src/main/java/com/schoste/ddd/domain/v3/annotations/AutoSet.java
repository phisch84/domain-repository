package com.schoste.ddd.domain.v3.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a setter of the destination object of an {@link com.schoste.ddd.domain.v3.services.AutoObjectConverter}, which the
 * converter calls with the value of a getter of the source object. Destination objects are e.g. entities, which the
 * repository converts from data objects, or the data transfer objects of an application layer, which are converted from
 * entities. The setter must be public and take exactly one parameter; it gets the value returned by a public getter without
 * parameters.
 *
 * Which getter is called:
 * <ul>
 * <li>its name is {@link AutoSet#methodName()}, or, if that isn't defined, the setter's name with "set" replaced by "get"
 * (e.g. {@code getName()} for {@code setName(String)}; a setter whose name doesn't start with "set" uses its own name).
 * Getters starting with "is" aren't found automatically; name them by {@link AutoSet#methodName()};</li>
 * <li>it is called on the source object, unless {@link AutoSet#clazz()} or {@link AutoSet#className()} names another class:
 * then it is called on an instance of that class (e.g. a class which provides a default value).</li>
 * </ul>
 * If the getter doesn't exist, the conversion fails.
 *
 * Which annotation to use depends on the layer of the annotated class, not on the direction of the conversion: this one for
 * classes of the domain layer and above (entities, data transfer objects), and
 * {@code com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet} of the {@code dao4} library for data objects, since the
 * infrastructure layer must not depend on the domain layer. Both have the same attributes and are evaluated the same way.
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
 * Setters which aren't annotated aren't called by the converter; set their values in the code which uses the converter
 * instead, e.g. in {@code GenericRepositoryImpl.doCompleteEntity(DataObject, Entity)} for entities.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface AutoSet
{
	/**
	 * Defines the class on whose instance the getter is called, instead of the source object.
	 * The instance is created by the converter (see {@link com.schoste.ddd.domain.v3.services.standard.AutoObjectConverterImpl}),
	 * once per converter and pair of source and destination class; it is reused for all their conversions.
	 *
	 * @return the class, or {@link Object} if not defined
	 */
	Class<?> clazz() default Object.class;

	/**
	 * Defines the full name (with package) of the class on whose instance the getter is called, instead of the source object.
	 * The instance is created like the one of {@link AutoSet#clazz()}.
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
