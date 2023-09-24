package com.schoste.ddd.domain.v1.services;

import com.schoste.ddd.domain.v1.exceptions.DomainException;

/**
 * Version 1 interface to a handler of {@see DomainException}
 * If there is an implementation and the instance is set at {@see DomainException#handler}, this handler is
 * called whenever a {@see DomainException} is created.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface DomainExceptionHandler
{
	/**
	 * Called by constructors of {@see DomainException}
	 * 
	 * @param exception the exception that was created
	 */
	void onExceptionCreated(DomainException exception);
}
