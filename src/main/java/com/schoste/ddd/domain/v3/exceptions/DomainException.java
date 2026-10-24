package com.schoste.ddd.domain.v3.exceptions;

/**
 * Basic class for all exceptions that originate from the domain layer
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class DomainException extends Exception
{
	private static final long serialVersionUID = 294841790973695190L;

	/**
	 * Creates a new instance with a message
	 *
	 * @param message the message of the exception
	 */
	public DomainException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new instance with a cause
	 *
	 * @param inner the cause of the exception
	 */
	public DomainException(Throwable inner)
	{
		super(inner);
	}

	/**
	 * Creates a new instance with a message and a cause
	 *
	 * @param message the message of the exception
	 * @param inner the cause of the exception
	 */
	public DomainException(String message, Throwable inner)
	{
		super(message, inner);
	}
}
