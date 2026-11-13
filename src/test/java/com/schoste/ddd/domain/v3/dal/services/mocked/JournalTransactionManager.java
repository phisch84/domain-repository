package com.schoste.ddd.domain.v3.dal.services.mocked;

import java.util.ArrayList;
import java.util.List;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOTransactionManager;

/**
 * Transaction manager of the in-memory DAOs {@link TransactionalExampleDAOImpl} which share a resource key: a transaction is a journal
 * of the changes made in it; rolling it back undoes them in reverse order, committing it forgets the journal.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class JournalTransactionManager extends AbstractDAOTransactionManager<JournalTransactionManager.Journal>
{
	/**
	 * The undo actions of the changes made in a transaction, in the order of the changes.
	 */
	public static class Journal
	{
		protected final List<Runnable> undoActions = new ArrayList<>();

		/**
		 * Records how to undo a change.
		 *
		 * @param undoAction the action which undoes the change
		 */
		public void record(Runnable undoAction)
		{
			this.undoActions.add(undoAction);
		}
	}

	protected final Object resourceKey;
	protected int commits = 0;
	protected int rollbacks = 0;

	/**
	 * Creates a transaction manager for the DAOs with a resource key.
	 *
	 * @param resourceKey the resource key of the DAOs
	 */
	public JournalTransactionManager(Object resourceKey)
	{
		this.resourceKey = resourceKey;
	}

	@Override
	protected Object getResourceKey()
	{
		return this.resourceKey;
	}

	@Override
	protected Journal doBegin() throws Exception
	{
		return new Journal();
	}

	@Override
	protected synchronized void doCommit(Journal journal) throws Exception
	{
		this.commits++;
	}

	@Override
	protected synchronized void doRollback(Journal journal) throws Exception
	{
		for (int i = journal.undoActions.size() - 1; i >= 0; i--) journal.undoActions.get(i).run();

		this.rollbacks++;
	}

	@Override
	protected void doRelease(Journal journal) throws Exception
	{
		journal.undoActions.clear();
	}

	/**
	 * Gets the number of storage transactions committed so far.
	 *
	 * @return the number of commits
	 */
	public synchronized int getCommits() { return this.commits; }

	/**
	 * Gets the number of storage transactions rolled back so far.
	 *
	 * @return the number of rollbacks
	 */
	public synchronized int getRollbacks() { return this.rollbacks; }
}
