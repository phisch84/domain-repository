package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.domain.v2.dal.services.TestDataAccessObject;
import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.domain.v2.services.GenericRepository;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Contract tests of {@link GenericRepository} (layer R of the test grid): one test per cell of the behavior matrix,
 * through the public interface of the repository, on a DAO which records how it is used ({@link TestDataAccessObject}).
 * The Javadoc of each test names its id in the test grid.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the identifiers of the entities and data objects
 * @param <E> the entity class
 * @param <DO> the data object class
 * @param <R> the repository class
 */
public abstract class GenericRepositoryContractTest<ID extends Serializable, E extends Entity<ID>, DO extends GenericDataObject<ID>, R extends GenericRepository<ID, E, DO>>
{
	protected RepositoryRecordingListener listener;

	/**
	 * Gets the repository under test. It must extend {@link GenericRepositoryImpl}.
	 *
	 * @return the repository under test
	 */
	protected abstract R getRepository();

	/**
	 * Gets the DAO of the repository under test
	 *
	 * @return the DAO of the repository under test
	 */
	protected abstract TestDataAccessObject<ID, DO> getDataAccessObject();

	/**
	 * Changes the values of a data object, e.g. to simulate a modification of the storage by others
	 *
	 * @param dataObject the data object to change
	 */
	protected abstract void changeValues(DO dataObject);

	/**
	 * Checks if an entity has the values of a data object
	 *
	 * @param entity the entity to check
	 * @param dataObject the data object with the expected values
	 * @return true if the entity has the values of the data object
	 */
	protected abstract boolean hasValuesOf(E entity, DO dataObject);

	/**
	 * Lets the repository's next conversion of a data object into an entity fail
	 *
	 * @param failure the exception the next conversion throws
	 */
	protected abstract void failNextConversion(RuntimeException failure);

	@SuppressWarnings("unchecked")
	protected GenericRepositoryImpl<ID, E, DO> getRepositoryImpl()
	{
		return (GenericRepositoryImpl<ID, E, DO>) this.getRepository();
	}

	/**
	 * Puts a new data object with an actual id into the storage
	 *
	 * @param isDeleted the deleted flag of the data object
	 * @return the stored data object
	 * @throws Exception re-throws every exception
	 */
	protected DO seedDataObject(boolean isDeleted) throws Exception
	{
		DO dataObject = this.getDataAccessObject().createDataObject();

		dataObject.setIsDeleted(isDeleted);

		this.getDataAccessObject().seed(dataObject);

		return dataObject;
	}

	/**
	 * Puts a data object with the given id into the storage, replacing a stored one with the same id
	 *
	 * @param id the id of the data object
	 * @return the stored data object
	 * @throws Exception re-throws every exception
	 */
	protected DO seedDataObject(ID id) throws Exception
	{
		DO dataObject = this.getDataAccessObject().createDataObject();

		dataObject.setId(id);

		this.getDataAccessObject().seed(dataObject);

		return dataObject;
	}

	/**
	 * Arranges an entity with the state Unchanged which is known to the repository (situation "Unch")
	 *
	 * @return the loaded entity
	 * @throws Exception re-throws every exception
	 */
	protected E loadEntity() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		return this.getRepository().get(dataObject.getId());
	}

	/**
	 * Arranges an entity with the state Added which is known to the repository (situation "Added")
	 *
	 * @return the added entity
	 * @throws Exception re-throws every exception
	 */
	protected E addEntity() throws Exception
	{
		E entity = this.getRepository().createEntity();

		this.getRepository().add(entity);

		return entity;
	}

	/**
	 * Simulates that the garbage collector has claimed the entity cached for an id. The id stays cached.
	 * Instances of the entity held by the test become unknown to the repository (situation "GC").
	 *
	 * @param id the id of the entity
	 */
	protected void simulateGc(ID id)
	{
		this.getRepositoryImpl().loadedEntities.put(id, new WeakReference<E>(null));
	}

	/**
	 * Gets the names of the methods queried at the DAO since the last {@link TestDataAccessObject#resetStatistics()}
	 *
	 * @return the names of the queried methods in the order of the queries
	 */
	protected List<String> getQueriedMethods()
	{
		return this.getDataAccessObject().getQueries().stream().map(TestDataAccessObject.Query::getMethod).collect(Collectors.toList());
	}

	protected static <T> Collection<T> collect(Stream<T> stream)
	{
		try (Stream<T> closingStream = stream)
		{
			return closingStream.collect(Collectors.toList());
		}
	}

	protected void clearAndReset() throws Exception
	{
		this.getDataAccessObject().clear();
		this.getRepository().reset();
		this.getDataAccessObject().resetStatistics();
	}

	@Before
	public void setUp() throws Exception
	{
		this.clearAndReset();

		this.listener = new RepositoryRecordingListener();
		this.getRepository().addChangeListener(this.listener);
	}

	@After
	public void tearDown() throws Exception
	{
		this.clearAndReset();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// createEntity / isVirtualId
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * C1: new entities are Detached and have unique virtual ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_calledRepeatedly_returnsDetachedEntitiesWithUniqueVirtualIds() throws Exception
	{
		Collection<ID> ids = new HashSet<>();

		for (int i=0; i<5; i++)
		{
			E entity = this.getRepository().createEntity();

			Assert.assertNotNull(entity);
			Assert.assertEquals(Entity.State.Detached, entity.getState());
			Assert.assertTrue(this.getRepository().isVirtualId(entity.getId()));
			Assert.assertTrue(ids.add(entity.getId()));
		}
	}

	/**
	 * C2: a new entity isn't known to the repository, and no listener is notified.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_newEntity_isUnknownAndNotNotified() throws Exception
	{
		E entity = this.getRepository().createEntity();

		Assert.assertNull(this.getRepository().get(entity.getId()));
		Assert.assertFalse(collect(this.getRepository().getAll(false)).contains(entity));
		Assert.assertTrue(this.listener.isEmpty());
	}

	/**
	 * V1: null isn't accepted as id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void isVirtualId_null_throwsIllegalArgument() throws Exception
	{
		this.getRepository().isVirtualId(null);
	}

	/**
	 * V2: ids of new entities are virtual, ids of stored data objects are actual ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void isVirtualId_createdAndStoredIds_reportsVirtualAndActual() throws Exception
	{
		Assert.assertTrue(this.getRepository().isVirtualId(this.getRepository().createEntity().getId()));
		Assert.assertFalse(this.getRepository().isVirtualId(this.seedDataObject(false).getId()));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// get(ID)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * G1: null isn't accepted as id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void get_null_throwsIllegalArgument() throws Exception
	{
		this.getRepository().get((ID) null);
	}

	/**
	 * G2: an added entity is returned as the same instance without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_addedEntity_returnsSameInstanceWithoutQuery() throws Exception
	{
		E entity = this.addEntity();

		Assert.assertSame(entity, this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G3: an Unchanged entity is returned as the same instance without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_unchangedEntity_returnsSameInstanceWithoutQuery() throws Exception
	{
		E entity = this.loadEntity();

		this.getDataAccessObject().resetStatistics();

		Assert.assertSame(entity, this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G3: a Modified entity is returned as the same instance without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_modifiedEntity_returnsSameInstanceWithoutQuery() throws Exception
	{
		E entity = this.loadEntity();

		this.getRepository().setModified(entity);
		this.getDataAccessObject().resetStatistics();

		Assert.assertSame(entity, this.getRepository().get(entity.getId()));
		Assert.assertEquals(Entity.State.Modified, entity.getState());
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G4: an entity whose removal is pending is treated as non-existent, without querying the DAO (Q4).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_pendingRemoval_returnsNullWithoutQuery() throws Exception
	{
		E entity = this.loadEntity();

		this.getRepository().remove(entity);
		this.getDataAccessObject().resetStatistics();

		Assert.assertNull(this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G5: the entity of an unknown actual id is loaded from the DAO, gets the state Unchanged and is cached.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_unknownIdWithDataObject_loadsAndCachesUnchangedEntity() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		E entity = this.getRepository().get(dataObject.getId());

		Assert.assertNotNull(entity);
		Assert.assertEquals(dataObject.getId(), entity.getId());
		Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		Assert.assertSame(entity, this.getRepository().get(dataObject.getId()));
		Assert.assertEquals(1, this.getQueriedMethods().size());
	}

	/**
	 * G6: an actual id without a data object returns null.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_unknownIdWithoutDataObject_returnsNull() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		this.getDataAccessObject().clear();
		this.getDataAccessObject().resetStatistics();

		Assert.assertNull(this.getRepository().get(dataObject.getId()));
		Assert.assertEquals(1, this.getQueriedMethods().size());
	}

	/**
	 * G7: a data object flagged deleted is treated as non-existent.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_dataObjectFlaggedDeleted_returnsNull() throws Exception
	{
		DO dataObject = this.seedDataObject(true);

		Assert.assertNull(this.getRepository().get(dataObject.getId()));
	}

	/**
	 * G8: a virtual id without a known entity returns null without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_unknownVirtualId_returnsNullWithoutQuery() throws Exception
	{
		E entity = this.getRepository().createEntity();

		Assert.assertNull(this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G9: if the entity of an actual id has been claimed by the garbage collector, a new instance is loaded from the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_garbageCollectedActualId_loadsNewInstance() throws Exception
	{
		E entity = this.loadEntity();

		this.simulateGc(entity.getId());
		this.getDataAccessObject().resetStatistics();

		E reloaded = this.getRepository().get(entity.getId());

		Assert.assertNotNull(reloaded);
		Assert.assertNotSame(entity, reloaded);
		Assert.assertEquals(Entity.State.Unchanged, reloaded.getState());
		Assert.assertEquals(1, this.getQueriedMethods().size());
	}

	/**
	 * G10: if the entity of a virtual id has been claimed by the garbage collector, null is returned without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_garbageCollectedVirtualId_returnsNullWithoutQuery() throws Exception
	{
		E entity = this.addEntity();

		this.simulateGc(entity.getId());

		Assert.assertNull(this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G11: a cached entity keeps its values if its data object is modified in the storage (Q3).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_dataObjectChangedInStorage_returnsCachedInstanceWithOldValues() throws Exception
	{
		DO dataObject = this.seedDataObject(false);
		E entity = this.getRepository().get(dataObject.getId());
		DO changedDataObject = this.seedDataObject(dataObject.getId());

		this.changeValues(changedDataObject);
		this.getDataAccessObject().resetStatistics();

		Assert.assertSame(entity, this.getRepository().get(dataObject.getId()));
		Assert.assertTrue(this.hasValuesOf(entity, dataObject));
		Assert.assertFalse(this.hasValuesOf(entity, changedDataObject));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G11: a cached entity is still returned if its data object is deleted in the storage (Q3).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_dataObjectDeletedInStorage_returnsCachedInstance() throws Exception
	{
		E entity = this.loadEntity();

		this.getDataAccessObject().delete(entity.getId());
		this.getDataAccessObject().resetStatistics();

		Assert.assertSame(entity, this.getRepository().get(entity.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * G12: a failure of the DAO is thrown as {@link DALException}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_daoFails_throwsDALException() throws Exception
	{
		DO dataObject = this.seedDataObject(false);
		DALException failure = new DALException();

		this.getDataAccessObject().failNextQuery(failure);

		try
		{
			this.getRepository().get(dataObject.getId());

			Assert.fail();
		}
		catch (DALException e)
		{
			Assert.assertSame(failure, e);
		}
	}

	/**
	 * G13: a failure of the conversion is thrown as {@link DomainException}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_conversionFails_throwsDomainException() throws Exception
	{
		DO dataObject = this.seedDataObject(false);
		RuntimeException failure = new RuntimeException("test");

		this.failNextConversion(failure);

		try
		{
			this.getRepository().get(dataObject.getId());

			Assert.fail();
		}
		catch (DomainException e)
		{
			Assert.assertSame(failure, e.getCause());
		}
	}
}
