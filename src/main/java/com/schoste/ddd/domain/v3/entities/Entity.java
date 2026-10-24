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
	 * The states of an entity, which tell the Unit of Work what to do with it on commit
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
		 * The entity is registered in the repository (its id is known to the repository) and was persisted, but needs to be saved by the next {@link UnitOfWork#commit()}
		 * because its values were changed (they do not match the values of the corresponding Data Object)
		 */
		Modified(2),
		
		/**
		 * The entity was removed from the repository; its data object is deleted by the next {@link UnitOfWork#commit()}
		 */
		Deleted(3),
		
		/**
		 * The entity isn't managed by the repository: it hasn't been added yet, it was removed before it was persisted, its removal was committed, or the repository forgot it (e.g. by {@link UnitOfWork#rollback()} or {@link GenericRepository#reset()})
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
	 * Entities created by a repository get a virtual ID from the repository when they are newly created (see {@link GenericRepository#createEntity()})
	 * until they are persisted. Once they have been persisted, their ID is updated with the ID of their data object.
	 *
	 * @return the unique ID of the entity.
	 */
	public ID getId();

	/**
	 * Sets the unique ID of the entity.
	 * Entities of a repository must have a unique Identifier.
	 * Entities created by a repository get a virtual ID from the repository when they are newly created (see {@link GenericRepository#createEntity()})
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
	 * {@link GenericRepository#createEntity()}, {@link GenericRepository#add(Entity)}, {@link GenericRepository#setModified(Entity)},
	 * and {@link GenericRepository#remove(Entity)}.
	 * 
	 * @param state the entity's state
	 */
	public void setState(State state);

	/**
	 * Checks if another entity represents the same entity of the domain, i.e. it is of the same class and has the same id.
	 * Use this method instead of {@link Object#equals(Object)} to compare entities by their id: {@link BasicEntity} uses identity
	 * for {@link Object#equals(Object)}, since the id of an entity changes when it is persisted.
	 *
	 * @param other the entity to compare with
	 * @return true if both entities are of the same class and have the same id; false if other is null or one of the ids is null
	 */
	public default boolean isSameEntityAs(Entity<ID> other)
	{
		if ((other == null) || (this.getId() == null) || (this.getClass() != other.getClass())) return false;

		return this.getId().equals(other.getId());
	}
}
