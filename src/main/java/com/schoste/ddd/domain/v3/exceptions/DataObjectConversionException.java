package com.schoste.ddd.domain.v3.exceptions;

import java.io.Serializable;

/**
 * Indicates that a data object couldn't be converted into an entity while a stream returned by
 * {@link com.schoste.ddd.domain.v3.services.GenericRepository#get(java.util.Collection)} or
 * {@link com.schoste.ddd.domain.v3.services.GenericRepository#getAll(boolean)} was consumed
 * (see {@link com.schoste.ddd.domain.v3.services.GenericEntityLoader}).
 *
 * The exception is unchecked, since streams can't throw checked exceptions.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class DataObjectConversionException extends RuntimeException
{
	private static final long serialVersionUID = -4170383725349624418L;

	/**
	 * The id of the data object which couldn't be converted
	 */
	private Serializable dataObjectId;

	/**
	 * Gets the id of the data object which couldn't be converted
	 *
	 * @return the id of the data object which couldn't be converted
	 */
	public Serializable getDataObjectId() { return this.dataObjectId; }

	/**
	 * Creates a new instance of this exception
	 *
	 * @param dataObjectId the id of the data object which couldn't be converted
	 * @param cause the original exception, or null if the conversion didn't provide an entity
	 */
	public DataObjectConversionException(Serializable dataObjectId, Throwable cause)
	{
		super(String.format("%s", dataObjectId), cause);

		this.dataObjectId = dataObjectId;
	}
}
