package com.schoste.ddd.domain.v1.exceptions;

/**
 * Indicates that no instance for a Data Object (DO) could be created.
 * This might be because the {@see GenericDAO#createDataObject()} method returns
 * null on the implementing class.
 *  
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class DataObjectNullException extends DomainException
{
	private static final long serialVersionUID = 3562819323078894333L;

	private Class<?> daoClass;

	/**
	 * Gets the class of the DAO which did not create an instance of the DO
	 * 
	 * @return the class of the DAO which did not create an instance of the DO
	 */
	public Class<?> getDAOClass() { return this.daoClass; }
	
	/**
	 * Creates a new instance of this exception
	 * 
	 * @param daoClass the class of the DAO which did not create an instance of the DO
	 */
	public DataObjectNullException(Class<?> daoClass) 
	{
		super(String.format("%s", (daoClass != null) ? daoClass.getName() : "null"));

		this.daoClass = daoClass;
	}

}
