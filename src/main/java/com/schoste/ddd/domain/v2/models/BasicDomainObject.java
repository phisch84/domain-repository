package com.schoste.ddd.domain.v2.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Basic domain object implementation
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the domain object's identifier
 */
public abstract class BasicDomainObject<ID extends Serializable> implements DomainObject<ID>
{
	protected ID id;
	protected State state;

	/**
	 * Creates a new instance with a detached state and no id (null)
	 */
	public BasicDomainObject()
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
	
	/**
	 * Returns the id of the instance as hash code
	 */
	@Override
	public int hashCode()
	{
		return Objects.hashCode(this.id);
	}
	
	/**
	 * Compares two instances by comparing the id of them
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		BasicDomainObject<?> other = (BasicDomainObject<?>) obj;

		return Objects.equals(this.id, other.id);
	}
}
