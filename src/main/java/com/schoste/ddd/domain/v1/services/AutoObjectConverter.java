package com.schoste.ddd.domain.v1.services;

import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.domain.v1.exceptions.InvocationException;

/**
 * Interface to the version 1 automated object converter
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface AutoObjectConverter
{
	/**
	 * Enriches the property of a given object with properties from another object.
	 * Which properties of the target object are set is defined by methods annotated
	 * with the {@see com.schoste.ddd.domain.annotations.AutoSet} annotation.
	 * 
	 * @param src the object to get the data from
	 * @param dst the object to write the data to
	 * @throws InvocationException thrown if invoking getters or setters fails
	 * @throws DomainException re-throws every exception as domain layer exception
	 */
	void convert(Object src, Object dst) throws InvocationException, DomainException;
}
