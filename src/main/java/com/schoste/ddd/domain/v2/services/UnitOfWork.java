package com.schoste.ddd.domain.v2.services;

import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;

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
	 * @throws IllegalStateException thrown if an object in a repository does not implement the {@link com.schoste.ddd.domain.v2.entities.Entity} interface
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
	 */
	public void commit() throws DALException, DomainException;
	
	/**
	 * Discards all changes which have been registered with this Unit of Work and haven't been committed yet.
	 * Nothing is written to the underlying data sources.
	 *
	 * The changes are discarded by letting the repositories forget the affected entities, since the storage is the gold source:
	 * every entity which has been added, flagged for removal or marked as modified since the last {@link UnitOfWork#commit()}
	 * is evicted from its repository's cache (and from its pending removals), and its state is set to Detached.
	 * The in-memory values of the entities are not restored. To continue working with an entity, get it from the repository again;
	 * the repository then provides a new instance with the state of the storage (or none, if the entity hadn't been persisted).
	 * Changes registered with other Units of Work are not affected.
	 *
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
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
