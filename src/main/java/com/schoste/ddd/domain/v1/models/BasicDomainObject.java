package com.schoste.ddd.domain.v1.models;

/**
 * Basic domain object implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public abstract class BasicDomainObject implements DomainObject
{
	protected int id;
	protected State state;

	/**
	 * Creates a new instance with a detached state and an Id = 0
	 */
	public BasicDomainObject()
	{
		this.state = State.Detached;
		this.id = 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getId() { return this.id; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setId(int id) { this.id = id; }

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
		return this.id;
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

		BasicDomainObject other = (BasicDomainObject) obj;
		
		return (this.id == other.id);
	}
}
