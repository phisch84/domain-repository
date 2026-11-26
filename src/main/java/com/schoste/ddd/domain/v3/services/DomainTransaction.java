package com.schoste.ddd.domain.v3.services;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * A transaction begun by a {@link DomainTransactionManager}. The commits of the Units of Work which the beginning thread makes until
 * the transaction is completed join it. Each transaction is completed exactly once, by {@link #commit()} or {@link #rollback()};
 * {@link #close()} rolls it back if it hasn't been completed yet.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DomainTransaction extends AutoCloseable
{
	/**
	 * Determines if this is the outermost transaction of its thread, which commits or rolls back the storage. An inner transaction
	 * has joined a transaction which the thread had already (see {@link DomainTransactionManager}).
	 *
	 * @return true if this transaction began the transaction of the storage, false if it joined one
	 */
	public boolean isOutermost();

	/**
	 * Commits the transaction. The outermost transaction stores all changes which the Units of Work have committed in it; an inner
	 * transaction doesn't store anything. If the storage is rolled back instead (because the transaction is marked for rollback, or
	 * storing fails), the repositories forget the entities committed in the transaction, and an exception is thrown.
	 *
	 * @throws IllegalStateException thrown if the transaction has been completed already, if it is used by another thread than the one
	 * which began it, or if it is the outermost transaction and inner transactions haven't been completed yet (the transaction stays open)
	 * @throws DAOException thrown if the storage has been rolled back instead of committed; re-throws exceptions of the storage
	 * @throws DomainException re-throws every other exception as DomainException
	 */
	public void commit() throws IllegalStateException, DAOException, DomainException;

	/**
	 * Rolls the transaction back: the repositories forget the entities which the Units of Work have committed in it. The outermost
	 * transaction discards their changes in the storage; an inner transaction marks the outermost one for rollback.
	 *
	 * @throws IllegalStateException thrown if the transaction has been completed already, or if it is used by another thread than the one
	 * which began it
	 * @throws DAOException re-throws exceptions of the storage
	 * @throws DomainException re-throws every other exception as DomainException
	 */
	public void rollback() throws IllegalStateException, DAOException, DomainException;

	/**
	 * Rolls the transaction back ({@link #rollback()}) if it hasn't been completed yet; otherwise does nothing.
	 *
	 * @throws DAOException re-throws exceptions of the storage
	 * @throws DomainException re-throws every other exception as DomainException
	 */
	@Override
	public void close() throws DAOException, DomainException;
}
