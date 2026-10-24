package com.schoste.ddd.domain.v3.exceptions;

/**
 * Indicates that no instance for a Data Object (DO) could be created or loaded.
 * This might be because {@link com.schoste.ddd.infrastructure.dao.v4.services.DAO#createDataObject()}
 * of the implementing DAO returns null, or because the data object of a persisted entity
 * wasn't found by {@link com.schoste.ddd.infrastructure.dao.v4.services.DAO#get(java.io.Serializable)}.
 *  
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class DataObjectNullException extends DomainException
{
	private static final long serialVersionUID = 3562819323078894333L;

	/**
	 * The class of the DAO which didn't provide the data object
	 */
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
