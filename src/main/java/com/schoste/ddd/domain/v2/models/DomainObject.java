package com.schoste.ddd.domain.v2.models;

import java.io.Serializable;

/**
 * Generic interface to all Domain Objects
 *
 * @author Philipp Schosteritsch
 *
 * @param <ID> the data type of the domain object's identifier. It is the same as the identifier of the corresponding data object
 */
public interface DomainObject<ID extends Serializable>
{
	/**
	 * States of the domain object with regards to DAL operations
	 * 
	 * @author Philipp Schosteritsch
	 */
	public enum State
	{
		/**
		 * The domain object has equal values with its corresponding Data Object
		 */
		Unchanged(0),
		
		/**
		 * The domain object was not yet persisted
		 */
		Added(1),
		
		/**
		 * The domain object was persisted but needs to be committed to the DAL
		 * because its values were changed (do not match the values of the corresponding
		 * Data Object)
		 */
		Modified(2),
		
		/**
		 * The domain object should be deleted from the DAL
		 */
		Deleted(3),
		
		/**
		 * The domain object should not be regarded for DAL operations
		 */
		Detached(4);

		private final int value;
		
		State(int value)
		{
			this.value = value;
		}
		
		int getValue()
		{
			return this.value;
		}
	};
	
	/**
	 * Gets the unique id of the domain object.
	 * Domain objects of a certain repository must have a unique identifier.
	 * Domain objects created by a repository get a virtual id from the repository's data access object
	 * until they are persisted (see {@link com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject#isVirtualId(Serializable)}).
	 * If the identifier is null, the domain object was not created by a repository.
	 *
	 * @return the unique id of the domain object.
	 */
	public ID getId();

	/**
	 * Sets the unique id of the domain object.
	 * Domain objects of a certain repository must have a unique identifier.
	 * Domain objects created by a repository get a virtual id from the repository's data access object
	 * until they are persisted (see {@link com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject#isVirtualId(Serializable)}).
	 * If the identifier is null, the domain object was not created by a repository.
	 *
	 * @param id the unique id of the domain object.
	 */
	public void setId(ID id);
	
	/**
	 * Gets the domain object's state which is evaluated by Units of Work
	 * 
	 * @return the domain object's state
	 */
	public State getState();
	
	/**
	 * Sets the domain object's state which is evaluated by Units of Work.
	 * Usually you only need to defined the Modified state manually.
	 * Other states are supposed to be defined by the corresponding repositories.
	 * 
	 * @param state the domain object's state
	 */
	public void setState(State state);
}
