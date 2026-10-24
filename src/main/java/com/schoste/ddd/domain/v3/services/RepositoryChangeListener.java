package com.schoste.ddd.domain.v3.services;

import java.io.Closeable;
import java.util.Collection;

import com.schoste.ddd.domain.v3.exceptions.DomainException;

/**
 * Interface to listeners which handle change notifications from repository
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface RepositoryChangeListener extends Closeable
{
	/**
	 * Called after a new object is added to the repository
	 * 
	 * @param repository the repository where the object was added
	 * @param obj the object that was added to the repository
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
	 */
	void onObjectAdded(GenericRepository<?,?,?> repository, Object obj) throws DomainException;
	
	/**
	 * Called after an object is removed from the repository
	 * 
	 * @param repository the repository from which the object was removed
	 * @param obj the object that was removed from the repository
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
	 */
	void onObjectRemoved(GenericRepository<?,?,?> repository, Object obj) throws DomainException;

	/**
	 * Called after an object was marked as modified by the repository
	 * 
	 * @param repository the repository from which manages the object
	 * @param obj the object that was modified
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
	 */
	void onObjectModified(GenericRepository<?,?,?> repository, Object obj) throws DomainException;
	
	/**
	 * Called after the repository has refreshed entities from the storage
	 * (see {@link GenericRepository#refresh()} and {@link GenericRepository#refresh(com.schoste.ddd.domain.v3.entities.Entity)}).
	 * Only entities with the state Unchanged are refreshed, so pending changes aren't affected.
	 *
	 * @param repository the repository which has refreshed the entities
	 * @param refreshedObjects the entities whose values were updated; they still have the state Unchanged
	 * @param forgottenObjects the entities which were forgotten because their data objects were deleted in the storage; their state is Detached
	 * @throws DomainException re-throws all exceptions as {@link DomainException}
	 */
	public void onRefreshed(GenericRepository<?,?,?> repository, Collection<Object> refreshedObjects, Collection<Object> forgottenObjects) throws DomainException;
}
