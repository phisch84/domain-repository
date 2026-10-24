package com.schoste.ddd.domain.v3.entities;

import java.io.Serializable;

/**
 * Basic entity implementation
 *
 * Entities are only equal to themselves ({@link Object#equals(Object)} and {@link Object#hashCode()} aren't overridden), because
 * their id changes while they are used: committing a new entity replaces its virtual id with the actual one, and the repository
 * and the Units of Work keep entities in hash-based collections. Use {@link Entity#isSameEntityAs(Entity)} to compare entities by
 * their id. A subclass may override {@link Object#equals(Object)} and {@link Object#hashCode()} itself, but only based on fields which
 * never change while the entity is used.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the entity's identifier
 */
public abstract class BasicEntity<ID extends Serializable> implements Entity<ID>
{
	protected ID id;
	protected State state;

	/**
	 * Creates a new instance with a detached state and no id (null)
	 */
	public BasicEntity()
	{
		this.state = State.Detached;
		this.id = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ID getId() { return this.id; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(ID id) { this.id = id; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public State getState() { return this.state; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setState(State state) { this.state = state; }
}
