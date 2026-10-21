package com.schoste.ddd.domain.v2.entities;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Generic interface to all Domain Objects, a.k.a. Entities.
 * Entities must have at least one corresponding Data object ({@link GenericDataObject}),
 * but they can also be a composition of multiple data objects.
 *
 * @author Philipp Schosteritsch
 *
 * @param <ID> the data type of the entity's Identifier. It is the same as the identifier of the corresponding data object
 */
public interface Entity<ID extends Serializable>
{
	/**
	 * States of the entity with regards to DAL operations
	 * 
	 * @author Philipp Schosteritsch
	 */
	public enum State
	{
		/**
		 * The entity has equal values with its corresponding Data Object
		 */
		Unchanged(0),
		
		/**
		 * The entity was not yet persisted
		 */
		Added(1),
		
		/**
		 * The entity was persisted but needs to be committed to the DAL
		 * because its values were changed (do not match the values of the corresponding
		 * Data Object)
		 */
		Modified(2),
		
		/**
		 * The entity should be deleted from the DAL
		 */
		Deleted(3),
		
		/**
		 * The entity should not be regarded for DAL operations
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
	 * Gets the unique ID of the entity.
	 * Domain objects of a certain repository must have a unique Identifier.
	 * Domain objects created by a repository get a virtual ID from the repository's data access object
	 * until they are persisted (see {@link com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject#isVirtualEID(Serializable)}).
	 * If the Identifier is null, the entity was not created by a repository.
	 *
	 * @return the unique ID of the entity.
	 */
	public ID getId();

	/**
	 * Sets the unique ID of the entity.
	 * Domain objects of a certain repository must have a unique Identifier.
	 * Domain objects created by a repository get a virtual ID from the repository's data access object
	 * until they are persisted (see {@link com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject#isVirtualEID(Serializable)}).
	 * If the Identifier is null, the entity was not created by a repository.
	 *
	 * @param ID the unique ID of the entity.
	 */
	public void setId(ID ID);
	
	/**
	 * Gets the entity's state which is evaluated by Units of Work
	 * 
	 * @return the entity's state
	 */
	public State getState();
	
	/**
	 * Sets the entity's state which is evaluated by Units of Work.
	 * Usually you only need to defined the Modified state manually.
	 * Other states are supposed to be defined by the corresponding repositories.
	 * 
	 * @param state the entity's state
	 */
	public void setState(State state);
}
