package com.schoste.ddd.domain.v3.services.standard;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.mocked.JournalTransactionManager;
import com.schoste.ddd.domain.v3.dal.services.mocked.TransactionalExampleDAOImpl;
import com.schoste.ddd.domain.v3.entities.Entity.State;
import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.models.ExampleDomainObject;
import com.schoste.ddd.domain.v3.services.DomainTransaction;
import com.schoste.ddd.domain.v3.services.DomainTransactionManager;
import com.schoste.ddd.domain.v3.services.UnitOfWork;
import com.schoste.ddd.domain.v3.services.standard.PlainJavaRepositoryTest.PlainRepository;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Tests of transactions of the domain ({@link DomainTransactionManager}, {@link DomainTransaction}) and of Units of Work which commit
 * in transactions ({@link UnitOfWorkImpl#setTransactionManager(DomainTransactionManager)}): the changes of one commit, and of several
 * commits in one transaction, are stored all together or not at all, and after a rollback the repositories forget the entities whose
 * changes have been discarded, so they stay consistent with the storage.
 * <p>
 * Two repositories with their own DAOs share one in-memory storage with transactions ({@link TransactionalExampleDAOImpl},
 * {@link JournalTransactionManager}). The DAOs fail to save entities whose property 1 is {@value #FAILING}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class DomainTransactionTest
{
	protected static final String FAILING = "failing";

	protected JournalTransactionManager daoTransactionManager;
	protected DomainTransactionManager transactionManager;
	protected TransactionalExampleDAOImpl dao1;
	protected TransactionalExampleDAOImpl dao2;
	protected PlainRepository repository1;
	protected PlainRepository repository2;

	@Before
	public void setUp()
	{
		Object resourceKey = new Object();

		this.daoTransactionManager = new JournalTransactionManager(resourceKey);
		this.transactionManager = new DomainTransactionManagerImpl(this.daoTransactionManager);
		this.dao1 = new TransactionalExampleDAOImpl(resourceKey);
		this.dao2 = new TransactionalExampleDAOImpl(resourceKey);
		this.dao1.failSavesOf(FAILING);
		this.dao2.failSavesOf(FAILING);
		this.repository1 = new PlainRepository(this.dao1);
		this.repository2 = new PlainRepository(this.dao2);
	}

	@After
	public void tearDown()
	{
		Assert.assertFalse("a test left a domain transaction open", this.transactionManager.isActive());
		Assert.assertFalse("a test left a storage transaction open", this.daoTransactionManager.isActive());
	}

	/**
	 * Creates a Unit of Work of both repositories which commits in transactions of the manager under test.
	 */
	protected UnitOfWorkImpl createTransactionalUnitOfWork()
	{
		List<GenericRepositoryImpl<?,?,?>> repositories = Arrays.asList(this.repository1, this.repository2);

		return new UnitOfWorkImpl(repositories, this.transactionManager);
	}

	/**
	 * Creates a Unit of Work of both repositories without a transaction manager.
	 */
	protected UnitOfWorkImpl createUnitOfWork()
	{
		List<GenericRepositoryImpl<?,?,?>> repositories = Arrays.asList(this.repository1, this.repository2);

		return new UnitOfWorkImpl(repositories);
	}

	protected static ExampleDomainObject add(PlainRepository repository, String property1) throws Exception
	{
		ExampleDomainObject entity = repository.createEntity();

		entity.setProperty1(property1);
		repository.add(entity);

		return entity;
	}

	/**
	 * Adds an entity and commits it with a Unit of Work without transaction manager, outside of any transaction.
	 */
	protected ExampleDomainObject persist(PlainRepository repository, String property1) throws Exception
	{
		try (UnitOfWork uow = this.createUnitOfWork())
		{
			ExampleDomainObject entity = add(repository, property1);

			uow.commit();

			return entity;
		}
	}

	protected static String stored(TransactionalExampleDAOImpl dao) throws Exception
	{
		StringBuilder values = new StringBuilder();

		for (ExampleDO dataObject : dao.getStored().values())
		{
			values.append((values.length() > 0) ? "," : "").append(dataObject.getProperty1()).append(dataObject.isDeleted() ? "(deleted)" : "");
		}

		return values.toString();
	}

	protected static void assertForgotten(PlainRepository repository, ExampleDomainObject entity) throws Exception
	{
		Assert.assertEquals(State.Detached, entity.getState());
		Assert.assertNotSame(entity, repository.get(entity.getId()));
	}

	// ---- one commit of a Unit of Work with a transaction manager ----

	@Test
	public void commit_withTransactionManager_storesAllRepositories() throws Exception
	{
		try (UnitOfWork uow = this.createTransactionalUnitOfWork())
		{
			ExampleDomainObject entity1 = add(this.repository1, "a");
			ExampleDomainObject entity2 = add(this.repository2, "b");

			uow.commit();

			Assert.assertEquals(State.Unchanged, entity1.getState());
			Assert.assertEquals(State.Unchanged, entity2.getState());
			Assert.assertSame(entity1, this.repository1.get(entity1.getId()));
		}

		Assert.assertEquals("a", stored(this.dao1));
		Assert.assertEquals("b", stored(this.dao2));
		Assert.assertEquals(1, this.daoTransactionManager.getCommits());
	}

	@Test
	public void commit_withTransactionManagerFailingInSecondRepository_storesNothingAndForgetsAll() throws Exception
	{
		ExampleDomainObject modified = this.persist(this.repository1, "stored");
		ExampleDomainObject removed = this.persist(this.repository1, "removed");

		try (UnitOfWork uow = this.createTransactionalUnitOfWork())
		{
			ExampleDomainObject added = add(this.repository1, "a");
			ExampleDomainObject failing = add(this.repository2, FAILING);

			modified.setProperty1("changed");
			this.repository1.setModified(modified);
			this.repository1.remove(removed);

			try
			{
				uow.commit();
				Assert.fail("the commit didn't fail");
			}
			catch (DAOException e) { }

			Assert.assertEquals(State.Detached, added.getState());
			Assert.assertEquals(State.Detached, failing.getState());
			assertForgotten(this.repository1, modified);
			Assert.assertNull(this.repository1.get(added.getId()));
		}

		Assert.assertEquals("stored,removed", stored(this.dao1));
		Assert.assertEquals("", stored(this.dao2));
		Assert.assertEquals("stored", this.repository1.get(modified.getId()).getProperty1());
		Assert.assertEquals(State.Unchanged, this.repository1.get(removed.getId()).getState());
	}

	@Test
	public void commit_withoutTransactionManagerFailing_keepsWhatHasBeenWritten() throws Exception
	{
		try (UnitOfWork uow = this.createUnitOfWork())
		{
			ExampleDomainObject written = add(this.repository1, "a");
			ExampleDomainObject failing = add(this.repository2, FAILING);

			try
			{
				uow.commit();
				Assert.fail("the commit didn't fail");
			}
			catch (DAOException e) { }

			// not atomic: the first repository has been written, the second one's change is still recorded
			Assert.assertEquals(State.Unchanged, written.getState());
			Assert.assertEquals(State.Added, failing.getState());
		}

		Assert.assertEquals("a", stored(this.dao1));
	}

	@Test
	public void setTransactionManager_unitOfWork_commitsInTransaction() throws Exception
	{
		try (UnitOfWorkImpl uow = this.createUnitOfWork())
		{
			uow.setTransactionManager(this.transactionManager);

			add(this.repository1, "a");
			add(this.repository2, FAILING);

			try
			{
				uow.commit();
				Assert.fail("the commit didn't fail");
			}
			catch (DAOException e) { }
		}

		Assert.assertEquals("", stored(this.dao1));
	}

	// ---- a transaction which spans several commits ----

	@Test
	public void transaction_severalCommits_storesThemOnCommit() throws Exception
	{
		try (DomainTransaction transaction = this.transactionManager.begin())
		{
			Assert.assertTrue(transaction.isOutermost());
			Assert.assertTrue(this.transactionManager.isActive());

			ExampleDomainObject parent;

			try (UnitOfWork uow = this.createTransactionalUnitOfWork())
			{
				parent = add(this.repository1, "parent");

				uow.commit();
			}

			// the parent has its actual id before the transaction is committed
			Assert.assertFalse(this.repository1.isVirtualId(parent.getId()));

			try (UnitOfWork uow = this.createUnitOfWork())
			{
				add(this.repository2, "child of " + parent.getId());

				uow.commit();
			}

			transaction.commit();
		}

		Assert.assertEquals("parent", stored(this.dao1));
		Assert.assertTrue(stored(this.dao2).startsWith("child of "));
		Assert.assertEquals(1, this.daoTransactionManager.getCommits());
	}

	@Test
	public void transaction_rollback_discardsAllCommitsAndForgetsTheirEntities() throws Exception
	{
		ExampleDomainObject modified = this.persist(this.repository1, "stored");
		ExampleDomainObject added1;
		ExampleDomainObject added2;

		try (DomainTransaction transaction = this.transactionManager.begin())
		{
			try (UnitOfWork uow = this.createTransactionalUnitOfWork())
			{
				added1 = add(this.repository1, "a");
				modified.setProperty1("changed");
				this.repository1.setModified(modified);

				uow.commit();
			}

			// a Unit of Work without own transaction manager joins the transaction as well
			try (UnitOfWork uow = this.createUnitOfWork())
			{
				added2 = add(this.repository2, "b");

				uow.commit();
			}

			transaction.rollback();
		}

		Assert.assertEquals("stored", stored(this.dao1));
		Assert.assertEquals("", stored(this.dao2));
		assertForgotten(this.repository1, added1);
		assertForgotten(this.repository2, added2);
		assertForgotten(this.repository1, modified);
		Assert.assertNull(this.repository1.get(added1.getId()));
		Assert.assertEquals("stored", this.repository1.get(modified.getId()).getProperty1());
	}

	@Test
	public void transaction_closeWithoutCommit_rollsBack() throws Exception
	{
		ExampleDomainObject added;

		try (DomainTransaction transaction = this.transactionManager.begin())
		{
			try (UnitOfWork uow = this.createUnitOfWork())
			{
				added = add(this.repository1, "a");

				uow.commit();
			}
		}

		Assert.assertEquals("", stored(this.dao1));
		assertForgotten(this.repository1, added);
	}

	@Test
	public void transaction_failingLaterCommit_rollsBackEarlierCommitsOnCommit() throws Exception
	{
		ExampleDomainObject earlier;

		try (DomainTransaction transaction = this.transactionManager.begin())
		{
			try (UnitOfWork uow = this.createUnitOfWork())
			{
				earlier = add(this.repository1, "earlier");

				uow.commit();
			}

			try (UnitOfWork uow = this.createUnitOfWork())
			{
				add(this.repository2, FAILING);

				uow.commit();
				Assert.fail("the commit didn't fail");
			}
			catch (DAOException e) { }

			try
			{
				transaction.commit();
				Assert.fail("committed a transaction with a failed commit");
			}
			catch (DAOException e) { }
		}

		Assert.assertEquals("", stored(this.dao1));
		assertForgotten(this.repository1, earlier);
	}

	@Test
	public void transaction_innerRollback_forgetsItsEntitiesAndMarksOuterForRollback() throws Exception
	{
		ExampleDomainObject outerEntity;
		ExampleDomainObject innerEntity;

		try (DomainTransaction outer = this.transactionManager.begin())
		{
			try (UnitOfWork uow = this.createUnitOfWork())
			{
				outerEntity = add(this.repository1, "outer");

				uow.commit();
			}

			try (DomainTransaction inner = this.transactionManager.begin())
			{
				Assert.assertFalse(inner.isOutermost());

				try (UnitOfWork uow = this.createUnitOfWork())
				{
					innerEntity = add(this.repository1, "inner");

					uow.commit();
				}

				inner.rollback();
			}

			Assert.assertEquals(State.Detached, innerEntity.getState());
			Assert.assertEquals(State.Unchanged, outerEntity.getState());

			try
			{
				outer.commit();
				Assert.fail("committed a transaction marked for rollback");
			}
			catch (DAOException e) { }
		}

		Assert.assertEquals("", stored(this.dao1));
		assertForgotten(this.repository1, outerEntity);
	}

	@Test
	public void transaction_innerCommitOuterRollback_forgetsEntitiesOfInner() throws Exception
	{
		ExampleDomainObject innerEntity;

		try (DomainTransaction outer = this.transactionManager.begin())
		{
			try (DomainTransaction inner = this.transactionManager.begin())
			{
				try (UnitOfWork uow = this.createTransactionalUnitOfWork())
				{
					innerEntity = add(this.repository1, "inner");

					uow.commit();
				}

				inner.commit();
			}

			Assert.assertEquals(State.Unchanged, innerEntity.getState());

			outer.rollback();
		}

		Assert.assertEquals("", stored(this.dao1));
		assertForgotten(this.repository1, innerEntity);
	}

	@Test
	public void transaction_entitiesCommittedBeforeIt_areKept() throws Exception
	{
		ExampleDomainObject before = this.persist(this.repository1, "before");

		try (DomainTransaction transaction = this.transactionManager.begin())
		{
			try (UnitOfWork uow = this.createUnitOfWork())
			{
				add(this.repository1, "in transaction");

				uow.commit();
			}

			transaction.rollback();
		}

		Assert.assertEquals("before", stored(this.dao1));
		Assert.assertEquals(State.Unchanged, before.getState());
		Assert.assertSame(before, this.repository1.get(before.getId()));
	}

	@Test
	public void transaction_completingTwice_throwsIllegalStateException() throws Exception
	{
		DomainTransaction transaction = this.transactionManager.begin();

		transaction.commit();
		transaction.close();

		try
		{
			transaction.commit();
			Assert.fail("committed twice");
		}
		catch (IllegalStateException e) { }

		try
		{
			transaction.rollback();
			Assert.fail("rolled back after commit");
		}
		catch (IllegalStateException e) { }
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructor_nullDAOTransactionManager_throwsIllegalArgumentException() throws DomainException
	{
		new DomainTransactionManagerImpl(null);
	}
}
