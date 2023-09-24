package com.schoste.ddd.domain.v1.models;

/**
 * Generic interface to all Domain Objects
 * 
 * @author Philipp Schosteritsch
 */
public interface DomainObject 
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
	 * If the identifier is greater than zero, the domain objects was already persisted.
	 * If the identifier is lass than zero, the domain object was not yet persisted but
	 * added to a repository.
	 * If the identifier is zero the domain object was not persisted yet and not added to
	 * a repository yet.
	 * 
	 * @return the unique id of the domain object.
	 */
	public int getId();
	
	/**
	 * Sets the unique id of the domain object.
	 * Domain objects of a certain repository must have a unique identifier.
	 * If the identifier is greater than zero, the domain objects was already persisted.
	 * If the identifier is lass than zero, the domain object was not yet persisted but
	 * added to a repository.
	 * If the identifier is zero the domain object was not persisted yet and not added to
	 * a repository yet.
	 * 
	 * @param id the unique id of the domain object.
	 */
	public void setId(int id);
	
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
