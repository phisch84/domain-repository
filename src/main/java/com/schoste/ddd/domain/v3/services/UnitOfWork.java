package com.schoste.ddd.domain.v3.services;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * A Unit of Work writes the changes of entities, which its repositories record, to the storage on {@link UnitOfWork#commit()},
 * or discards them on {@link UnitOfWork#rollback()}. Repositories never write to the storage themselves.
 *
 * A Unit of Work registers itself as change listener ({@link RepositoryChangeListener}) of its repositories and records
 * every change the repositories report, regardless of who made the change. Therefore, use one Unit of Work per repository
 * at a time: Units of Work registered with the same repository share its changes, i.e. each of them records the same changes,
 * commits them and rolls them back.
 *
 * {@link java.io.Closeable#close()} rolls back the changes which haven't been committed and unregisters the Unit of Work from its
 * repositories, so a Unit of Work is best used in a try-with-resources statement.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface UnitOfWork extends RepositoryChangeListener
{
	/**
	 * Writes the recorded changes to the storage through the repositories' DAOs:
	 * <ul>
	 * <li>added entities are saved; they get the actual id of their data object and the state Unchanged;</li>
	 * <li>modified entities are saved; they get the state Unchanged;</li>
	 * <li>the data objects of removed entities are deleted (see {@code DAO.delete(DataObject)}); the entities get the state Detached.</li>
	 * </ul>
	 * By itself, the commit isn't atomic: it isn't a transaction across DAOs or repositories. If it fails part-way, the changes written
	 * so far stay written, and the changes which haven't been written yet stay recorded. Transactions ({@link DomainTransactionManager})
	 * make it atomic, also together with other commits; implementations say how they use them (see
	 * {@code UnitOfWorkImpl.setTransactionManager(DomainTransactionManager)}).
	 *
	 * @throws DAOException re-throws exceptions from the DAOs
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void commit() throws DAOException, DomainException;
	
	/**
	 * Discards all changes which have been registered with this Unit of Work and haven't been committed yet.
	 * Nothing is written to the underlying data sources.
	 *
	 * The changes are discarded by letting the repositories forget the affected entities, since the storage is the gold source:
	 * every entity which has been added, flagged for removal or marked as modified since the last {@link UnitOfWork#commit()}
	 * is evicted from its repository's cache (and from its pending removals), and its state is set to Detached.
	 * The in-memory values of the entities are not restored. To continue working with an entity, get it from the repository again;
	 * the repository then provides a new instance with the state of the storage (or none, if the entity hadn't been persisted).
	 * Other Units of Work registered with the same repositories have recorded the same changes (see {@link UnitOfWork});
	 * the entities they would commit are Detached now, so committing them afterwards leads to undefined results.
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
