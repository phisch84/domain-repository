package com.schoste.ddd.domain.v2.services;

import com.schoste.ddd.domain.v2.exceptions.DomainException;

/**
 * Version 1 interface to a handler of {@link DomainException}
 * If there is an implementation and the instance is set at {@link DomainException#handler}, this handler is
 * called whenever a {@link DomainException} is created.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface DomainExceptionHandler
{
	/**
	 * Called by constructors of {@link DomainException}
	 * 
	 * @param exception the exception that was created
	 */
	void onExceptionCreated(DomainException exception);
}
