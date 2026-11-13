package com.schoste.ddd.domain.v3.services.standard;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.services.DomainTransaction;
import com.schoste.ddd.domain.v3.services.DomainTransactionManager;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.services.DAOTransaction;
import com.schoste.ddd.infrastructure.dao.v4.services.DAOTransactionManager;

/**
 * Standard implementation of the {@link DomainTransactionManager} for repositories which extend {@link GenericRepositoryImpl}
 * and Units of Work of the class {@link UnitOfWorkImpl}.
 *
 * Each transaction of the domain begins (or joins) a transaction of the storage with the {@link DAOTransactionManager} passed to the
 * constructor, so it spans the DAOs of that storage. The open transactions of a thread form a stack: the {@link UnitOfWorkImpl}
 * registers the entities it has written with the innermost one. When an inner transaction is committed, its entities are handed on
 * to the enclosing one; when a transaction is rolled back, or the storage is rolled back instead of committed, the repositories
 * forget its entities ({@code GenericRepositoryImpl.forget(Object)}).
 * <pre>
 * DomainTransactionManager transactionManager = new DomainTransactionManagerImpl(new HibernateDAOTransactionManager(sessionFactory));
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class DomainTransactionManagerImpl implements DomainTransactionManager
{
	/**
	 * The open transactions of each thread; the innermost one is first.
	 */
	private static final ThreadLocal<Deque<Transaction>> OPEN_TRANSACTIONS = ThreadLocal.withInitial(ArrayDeque::new);

	/**
	 * Registers entities which a Unit of Work has written to the storage with the innermost transaction of the current thread,
	 * so that their repository forgets them if the transaction is rolled back. Does nothing if the thread has no transaction.
	 *
	 * @param repository the repository of the entities
	 * @param entities the written entities
	 */
	static void registerWritten(GenericRepositoryImpl<?,?,?> repository, Collection<Object> entities)
	{
		Transaction transaction = OPEN_TRANSACTIONS.get().peekFirst();

		if (transaction == null) return;

		for (Object entity : entities) transaction.written.add(new SimpleEntry<>(repository, entity));
	}

	/**
	 * Marks the innermost transaction of the current thread for rollback, e.g. after a Unit of Work has failed to commit in it.
	 * Does nothing if the thread has no transaction.
	 */
	static void markForRollback()
	{
		Transaction transaction = OPEN_TRANSACTIONS.get().peekFirst();

		if (transaction != null) transaction.rollbackOnly = true;
	}

	/**
	 * A transaction begun by {@link DomainTransactionManagerImpl#begin()}.
	 */
	protected class Transaction implements DomainTransaction
	{
		/**
		 * The transaction of the storage which this transaction began or joined.
		 */
		protected final DAOTransaction daoTransaction;

		/**
		 * The thread which began the transaction.
		 */
		protected final Thread thread = Thread.currentThread();

		/**
		 * The entities written in this transaction, with their repositories, in the order of writing.
		 */
		protected final List<Map.Entry<GenericRepositoryImpl<?,?,?>, Object>> written = new ArrayList<>();

		/**
		 * Whether a Unit of Work has failed to commit in this transaction, or in a committed inner transaction.
		 */
		protected boolean rollbackOnly = false;

		/**
		 * Whether this transaction has been committed or rolled back.
		 */
		protected boolean completed = false;

		/**
		 * Creates a transaction.
		 *
		 * @param daoTransaction the transaction of the storage which this transaction began or joined
		 */
		protected Transaction(DAOTransaction daoTransaction)
		{
			this.daoTransaction = daoTransaction;
		}

		/**
		 * Gets the manager which began this transaction.
		 *
		 * @return the manager
		 */
		protected DomainTransactionManagerImpl getManager()
		{
			return DomainTransactionManagerImpl.this;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isOutermost()
		{
			return this.daoTransaction.isOutermost();
		}

		/**
		 * Checks that this transaction can be completed by the current thread.
		 *
		 * @throws IllegalStateException thrown if it has been completed already, or if it belongs to another thread
		 */
		protected void checkCompletable() throws IllegalStateException
		{
			if (this.completed) throw new IllegalStateException("The transaction has been completed already");
			if (this.thread != Thread.currentThread()) throw new IllegalStateException("The transaction belongs to another thread");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void commit() throws IllegalStateException, DAOException, DomainException
		{
			this.checkCompletable();

			Deque<Transaction> openTransactions = OPEN_TRANSACTIONS.get();

			if (openTransactions.peekFirst() != this) throw new IllegalStateException("Inner transactions haven't been completed yet");

			if (!this.isOutermost())
			{
				this.daoTransaction.commit();
				this.completed = true;
				openTransactions.removeFirst();

				// the enclosing transaction decides about the entities written in this one
				Transaction enclosing = openTransactions.peekFirst();

				if (enclosing != null)
				{
					enclosing.written.addAll(this.written);
					enclosing.rollbackOnly |= this.rollbackOnly;
				}

				return;
			}

			if (this.rollbackOnly)
			{
				this.rollback();

				throw new DAOException(new IllegalStateException("The transaction has been rolled back, since a Unit of Work failed to commit in it"));
			}

			try
			{
				this.daoTransaction.commit();
			}
			catch (DAOException e)
			{
				// the storage has been rolled back
				this.completed = true;
				openTransactions.removeFirst();

				this.forgetWritten(e);

				throw e;
			}

			this.completed = true;
			openTransactions.removeFirst();
			this.written.clear();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void rollback() throws IllegalStateException, DAOException, DomainException
		{
			this.checkCompletable();

			Deque<Transaction> openTransactions = OPEN_TRANSACTIONS.get();
			Exception failure = null;

			try
			{
				this.daoTransaction.rollback();
			}
			catch (Exception e)
			{
				failure = e;
			}

			this.completed = true;

			// inner transactions which are still open are rolled back with this one
			while (!openTransactions.isEmpty())
			{
				Transaction transaction = openTransactions.removeFirst();

				if (transaction != this)
				{
					transaction.completed = true;
					transaction.forgetWritten(failure);

					continue;
				}

				failure = this.forgetWritten(failure);

				break;
			}

			if (failure instanceof DAOException) throw (DAOException) failure;
			if (failure instanceof DomainException) throw (DomainException) failure;
			if (failure != null) throw new DomainException(failure);
		}

		/**
		 * Lets the repositories forget the entities written in this transaction, in reverse order of writing.
		 * All entities are forgotten, also if forgetting some of them fails.
		 *
		 * @param failure an exception which has occurred already, or null
		 * @return the exception which has occurred already, with the failures of forgetting added as suppressed exceptions; or a
		 * {@link DomainException} of the first failure of forgetting if none had occurred; or null if nothing failed
		 */
		protected Exception forgetWritten(Exception failure)
		{
			for (int i = this.written.size() - 1; i >= 0; i--)
			{
				Map.Entry<GenericRepositoryImpl<?,?,?>, Object> entry = this.written.get(i);

				try
				{
					entry.getKey().forget(entry.getValue());
				}
				catch (Exception e)
				{
					if (failure == null) failure = new DomainException(e);
					else failure.addSuppressed(e);
				}
			}

			this.written.clear();

			return failure;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws DAOException, DomainException
		{
			if (!this.completed) this.rollback();
		}
	}

	/**
	 * The manager of the transactions of the storage.
	 */
	protected final DAOTransactionManager daoTransactionManager;

	/**
	 * Creates a transaction manager whose transactions span the DAOs of a storage.
	 *
	 * @param daoTransactionManager the manager of the transactions of the storage
	 * @throws IllegalArgumentException thrown if parameter daoTransactionManager is null
	 */
	public DomainTransactionManagerImpl(DAOTransactionManager daoTransactionManager) throws IllegalArgumentException
	{
		if (daoTransactionManager == null) throw new IllegalArgumentException("daoTransactionManager");

		this.daoTransactionManager = daoTransactionManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DomainTransaction begin() throws DAOException, DomainException
	{
		try
		{
			Transaction transaction = new Transaction(this.daoTransactionManager.begin());

			OPEN_TRANSACTIONS.get().addFirst(transaction);

			return transaction;
		}
		catch (DAOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive()
	{
		for (Iterator<Transaction> transactions = OPEN_TRANSACTIONS.get().iterator(); transactions.hasNext(); )
		{
			if (transactions.next().getManager() == this) return true;
		}

		return false;
	}
}
