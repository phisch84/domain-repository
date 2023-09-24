package com.schoste.ddd.domain.v1.exceptions;

import com.schoste.ddd.domain.v1.services.DomainExceptionHandler;

/**
 * Basic class for all exceptions that originate from the domain layer
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class DomainException extends Exception
{
	private static final long serialVersionUID = 294841790973695190L;

	/**
	 * Instance to a handler for {@see DomainException}.
	 * If not null {@ DomainExceptionHandler#onExceptionCreated(DomainException)} will be
	 * called whenever a {@see DomainException} is created.
	 */
	public static DomainExceptionHandler handler = null;

	protected static void invokeHandler(DomainException exception)
	{
		try
		{
			if (handler != null) handler.onExceptionCreated(exception);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	public DomainException(String message)
	{
		super(message);
		
		invokeHandler(this);
	}

	public DomainException(Throwable inner)
	{
		super(inner);

		invokeHandler(this);
	}

	public DomainException(String message, Throwable inner)
	{
		super(message, inner);

		invokeHandler(this);
	}
}
