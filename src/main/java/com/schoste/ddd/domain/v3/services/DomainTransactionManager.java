package com.schoste.ddd.domain.v3.services;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Begins transactions which span several commits of Units of Work ({@link UnitOfWork#commit()}), so that their changes are stored
 * all together or not at all, and the repositories stay consistent with the storage either way.
 *
 * A transaction of the domain wraps a transaction of the storage ({@link com.schoste.ddd.infrastructure.dao.v4.services.DAOTransactionManager}):
 * the DAO calls of the Units of Work join it. In addition, it remembers the entities which the Units of Work commit while it is
 * open. If it is rolled back, or its commit fails, the storage discards their changes, and their repositories forget them, as
 * {@link UnitOfWork#rollback()} does: the entities get the state Detached, and getting them from the repositories again provides
 * new instances with the state of the storage (or none, for entities which had been added).
 * <pre>
 * try (DomainTransaction transaction = transactionManager.begin())
 * {
 *     customerRepository.add(customer);
 *     unitOfWork.commit();                 // the customer gets its actual id
 *
 *     order.setCustomerId(customer.getId());
 *     orderRepository.add(order);
 *     unitOfWork.commit();
 *
 *     transaction.commit();                // without commit, closing the transaction rolls both commits back
 * }
 * </pre>
 * Rules which hold for every transaction:
 * <ul>
 * <li><b>Threads:</b> a transaction belongs to the thread which began it. The commits of all Units of Work in that thread join it.</li>
 * <li><b>Nesting:</b> {@link #begin()} on a thread which has a transaction already joins it: the returned transaction is an
 * <i>inner</i> one ({@link DomainTransaction#isOutermost()} is false). Committing an inner transaction doesn't store anything;
 * rolling it back, or closing it without commit, forgets the entities committed in it and marks the whole transaction for rollback.
 * Only the outermost transaction commits or rolls back the storage.</li>
 * <li><b>Listeners</b> of the Units of Work ({@link UnitOfWorkListener}) are notified when a Unit of Work commits, also if the
 * transaction is rolled back afterwards.</li>
 * </ul>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DomainTransactionManager
{
	/**
	 * Begins a transaction on the current thread, or joins the one the thread has already (see the rules on nesting above).
	 *
	 * @return the transaction; it must be committed or rolled back, best by a try-with-resources statement, which rolls back
	 * a transaction that hasn't been committed
	 * @throws DAOException re-throws exceptions of the storage
	 * @throws DomainException re-throws every other exception as DomainException
	 */
	public DomainTransaction begin() throws DAOException, DomainException;

	/**
	 * Determines if the current thread has a transaction of this manager.
	 *
	 * @return true if the current thread has a transaction, which the commits of Units of Work in the thread join
	 */
	public boolean isActive();
}
