package com.schoste.ddd.domain.v3.dal.services.mocked;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOTransactionManager;

/**
 * In-memory DAO which joins the transactions of a {@link JournalTransactionManager} with the same resource key: while the calling
 * thread has one, every saved data object is recorded in its journal, so that rolling it back restores the previous state.
 * It can be told to fail saving data objects with a certain value of property 1 ({@link #failSavesOf(String)}); a failure in a
 * transaction marks it for rollback, as the DAO contract demands.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class TransactionalExampleDAOImpl extends ExampleDAOImpl
{
	protected final Object resourceKey;
	protected String failingProperty1 = null;

	/**
	 * Creates a DAO which joins the transactions of the given resource key.
	 *
	 * @param resourceKey the resource key of its {@link JournalTransactionManager}
	 */
	public TransactionalExampleDAOImpl(Object resourceKey)
	{
		this.resourceKey = resourceKey;
	}

	/**
	 * Lets the DAO fail to save data objects whose property 1 has the given value.
	 *
	 * @param property1 the value which fails, or null to fail nothing
	 */
	public synchronized void failSavesOf(String property1)
	{
		this.failingProperty1 = property1;
	}

	/**
	 * Gets the stored data objects, by id, as copies.
	 *
	 * @return the stored data objects
	 * @throws Exception re-throws every exception
	 */
	public synchronized Map<Long, ExampleDO> getStored() throws Exception
	{
		Map<Long, ExampleDO> stored = new HashMap<>();

		for (Map.Entry<Long, ExampleDO> entry : this.dataObjects.entrySet()) stored.put(entry.getKey(), this.copyOf(entry.getValue()));

		return stored;
	}

	@Override
	protected synchronized void doSave(ExampleDO dataObject) throws Exception
	{
		JournalTransactionManager.Journal journal = AbstractDAOTransactionManager.getBoundResource(this.resourceKey, JournalTransactionManager.Journal.class);

		if ((this.failingProperty1 != null) && Objects.equals(this.failingProperty1, dataObject.getProperty1()))
		{
			AbstractDAOTransactionManager.setRollbackOnly(this.resourceKey);

			throw new IllegalStateException("saving failed: " + dataObject.getProperty1());
		}

		Long previousId = dataObject.getId();
		ExampleDO previous = this.isVirtualId(previousId) ? null : this.dataObjects.get(previousId);

		super.doSave(dataObject);

		if (journal == null) return;

		Long id = dataObject.getId();

		journal.record(() ->
		{
			synchronized (this)
			{
				if (previous == null) this.dataObjects.remove(id);
				else this.dataObjects.put(id, previous);
			}
		});
	}
}
