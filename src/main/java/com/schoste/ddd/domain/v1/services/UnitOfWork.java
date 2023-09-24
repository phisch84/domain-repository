package com.schoste.ddd.domain.v1.services;

import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;

/**
 * Interface to Units of Work (UOWs) which commit changes
 * of several repositories to their underlying data sources.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface UnitOfWork extends RepositoryChangeListener
{
	/**
	 * Persists all changes to the underlying data source
	 * 
	 * @throws IllegalStateException thrown if an object in a repository does not implement the DomainObject interface
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	public void commit() throws DALException, DomainException;
	
	/**
	 * Reverts all changes of the repositories
	 * 
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	public void rollback() throws DomainException;

	/**
	 * Adds a listener which will be notified after {@link UnitOfWork#commit()} and {@link UnitOfWork#rollback()} have executed.
	 * 
	 * @param listener the listener to add
	 * @exception IllegalArgumentException thrown if listener is null
	 */
	public void addListener(UnitOfWorkListener listener) throws IllegalArgumentException;

	/**
	 * Removes a listener which will be notified after {@link UnitOfWork#commit()} and {@link UnitOfWork#rollback()} have executed.
	 * 
	 * @param listener the listener to remove
	 * @exception IllegalArgumentException thrown if listener is null
	 */
	public void removeListener(UnitOfWorkListener listener) throws IllegalArgumentException;
}
