package com.schoste.ddd.domain.v1.services;

import java.util.Collection;

/**
 * Interface for listeners of the {@link UnitOfWork}
 * These listeners will be called after actions like {@link UnitOfWork#commit()} and {@link UnitOfWork#rollback()} are called.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface UnitOfWorkListener
{
	/**
	 * Called by the {@link UnitOfWork} after {@link UnitOfWork#rollback()} was executed containing
	 * all the objects which were rolled back.
	 * 
	 * @param objs the objects which were rolled back
	 */
	void afterRollback(Collection<Object> objs);

	/**
	 * Called by the {@link UnitOfWork} after {@link UnitOfWork#commit()} was executed containing
	 * all the new objects which were persisted.
	 * 
	 * @param objs the objects which were persisted
	 */
	void afterPersistNew(Collection<Object> objs);

	/**
	 * Called by the {@link UnitOfWork} after {@link UnitOfWork#commit()} was executed containing
	 * all the existing objects which were persisted.
	 * 
	 * @param objs the objects which were persisted
	 */
	void afterPersistExisting(Collection<Object> objs);

	/**
	 * Called by the {@link UnitOfWork} after {@link UnitOfWork#commit()} was executed containing
	 * all the new objects which were deleted.
	 * 
	 * @param objs the objects which were deleted
	 */
	void afterDelete(Collection<Object> objs);

	/**
	 * Called by the {@link UnitOfWork} after it was notified about the reload of a repository
	 *
	 * @param objs all the objects that were put into detached state
	 */
	void afterReload(Collection<Object> objs);
}
