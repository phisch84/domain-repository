package com.schoste.ddd.domain.v3.entities;

import java.io.Serializable;

import com.schoste.ddd.domain.v3.services.GenericRepository;
import com.schoste.ddd.domain.v3.services.UnitOfWork;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Generic interface to all Domain Objects, a.k.a. Entities.
 * Entities must have at least one corresponding Data object ({@link DataObject}),
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
		 * The entity is registered in the repository (its id is known to the repository) and its values are equal to those of its corresponding Data Object
		 */
		Unchanged(0),
		
		/**
		 * The entity has not been persisted yet, but is supposed to be persisted by the next call to {@link UnitOfWork#commit()}
		 */
		Added(1),
		
		/**
		 * The entity is registered in the repository (its id is known to the repository) and was persisted, but needs to be committed to the DAL
		 * because its values were changed (they do not match the values of the corresponding Data Object)
		 */
		Modified(2),
		
		/**
		 * The entity should be deleted from the DAL
		 */
		Deleted(3),
		
		/**
		 * The entity doesn't exist in the repository and should not be regarded for DAL operations
		 */
		Detached(4);

		private final int value;
		
		State(int value)
		{
			this.value = value;
		}

		/**
		 * Gets the integer value of the {@link State}
		 * 
		 * @return the integer value of the {@link State}
		 */
		int getValue()
		{
			return this.value;
		}
	};
	
	/**
	 * Gets the unique ID of the entity.
	 * Entities of a repository must have a unique Identifier.
	 * Entities created by a repository get a virtual ID from the repository when they are newly created (see {@link GenericRepository#createEntity(Object...)})
	 * until they are persisted. Once they have been persisted, their ID is updated with the ID of their data object.
	 *
	 * @return the unique ID of the entity.
	 */
	public ID getId();

	/**
	 * Sets the unique ID of the entity.
	 * Entities of a repository must have a unique Identifier.
	 * Entities created by a repository get a virtual ID from the repository when they are newly created (see {@link GenericRepository#createEntity(Object...)})
	 * until they are persisted. Once they have been persisted, their ID is updated with the ID of their data object.
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
	 * The states are intended to be managed by the entity's repository. See:
	 * {@link GenericRepository#createEntity(Object...)}, {@link GenericRepository#add(Entity)}, {@link GenericRepository#setModified(Entity)},
	 * and {@link GenericRepository#remove(Entity)}.
	 * 
	 * @param state the entity's state
	 */
	public void setState(State state);
}
