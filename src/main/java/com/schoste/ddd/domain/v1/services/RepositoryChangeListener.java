package com.schoste.ddd.domain.v1.services;

import java.io.Closeable;

import com.schoste.ddd.domain.v1.exceptions.DomainException;

/**
 * Interface to listeners which handle change notifications from repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface RepositoryChangeListener extends Closeable
{
	/**
	 * Called after a new object is added to the repository
	 * 
	 * @param repository the repository where the object was added
	 * @param obj the object that was added to the repository
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	void onObjectAdded(GenericRepository<?,?> repository, Object obj) throws DomainException;
	
	/**
	 * Called after an object is removed from the repository
	 * 
	 * @param repository the repository from which the object was removed
	 * @param obj the object that was removed from the repository
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	void onObjectRemoved(GenericRepository<?,?> repository, Object obj) throws DomainException;

	/**
	 * Called after an object was marked as modified by the repository
	 * 
	 * @param repository the repository from which manages the object
	 * @param obj the object that was modified
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	void onObjectModified(GenericRepository<?,?> repository, Object obj) throws DomainException;
	
	/**
	 * Called before the repository is reloaded
	 * 
	 * @param repository the repository for which to reset the change logs
	 * @throws DomainException re-throws all exceptions as {@see DomainException}
	 */
	public void onReload(GenericRepository<?,?> repository) throws DomainException;
}
