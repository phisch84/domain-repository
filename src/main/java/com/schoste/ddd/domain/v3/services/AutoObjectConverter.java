package com.schoste.ddd.domain.v3.services;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.exceptions.InvocationException;

/**
 * Copies values between data objects and entities: the repository uses it to set an entity from a data object and vice versa.
 * The setters of the destination object which are annotated with AutoSet are called with the values of their getters
 * ({@link com.schoste.ddd.domain.v3.annotations.AutoSet} on entities, {@code com.schoste.ddd.infrastructure.dao.v4.annotations.AutoSet}
 * on data objects). The standard implementation is {@link com.schoste.ddd.domain.v3.services.standard.AutoObjectConverterImpl}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface AutoObjectConverter
{
	/**
	 * Calls the setters of the destination object which are annotated with AutoSet with the values of their getters.
	 * Other setters aren't called. Does nothing if src or dst is null.
	 *
	 * @param src the object to get the values from, e.g. a data object
	 * @param dst the object to set the values of, e.g. an entity
	 * @throws InvocationException thrown if a getter or setter can't be called or throws; tells which one
	 * @throws DomainException re-throws every other exception as {@link DomainException}, e.g. if a getter can't be found
	 */
	void convert(Object src, Object dst) throws InvocationException, DomainException;
}
