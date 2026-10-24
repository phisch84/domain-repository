package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.domain.v2.dal.services.TestDataAccessObject;
import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.exceptions.DataObjectConversionException;
import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.domain.v2.services.GenericRepository;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
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
	 * Puts a data object into the storage, replacing a stored one with the same id
	 *
	 * @param id the id of the data object, or null for a new actual id
	 * @param isDeleted the deleted flag of the data object
	 * @param changeValues if true, the values of the data object are changed (see {@link #changeValues(GenericDataObject)})
	 * @param modifiedTimeStamp the modification time stamp of the data object, or null for the current time
	 * @return the stored data object
	 * @throws Exception re-throws every exception
	 */
	protected DO seedDataObject(ID id, boolean isDeleted, boolean changeValues, Long modifiedTimeStamp) throws Exception
	{
		DO dataObject = this.getDataAccessObject().createDataObject();

		if (id != null) dataObject.setId(id);
		if (changeValues) this.changeValues(dataObject);
		if (modifiedTimeStamp != null) dataObject.setModifiedTimeStamp(modifiedTimeStamp);

		dataObject.setIsDeleted(isDeleted);

		this.getDataAccessObject().seed(dataObject);

		return dataObject;
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
		return this.seedDataObject(null, isDeleted, false, null);
	}

	/**
	 * Puts a new data object with an actual id and a given modification time stamp into the storage
	 *
	 * @param modifiedTimeStamp the modification time stamp of the data object
	 * @return the stored data object
	 * @throws Exception re-throws every exception
	 */
	protected DO seedDataObject(long modifiedTimeStamp) throws Exception
	{
		return this.seedDataObject(null, false, false, modifiedTimeStamp);
	}

	/**
	 * Checks if an id is cached by the repository (with or without a live entity)
	 *
	 * @param id the id to check
	 * @return true if the id is cached
	 */
	protected boolean isCached(ID id)
	{
		return this.getRepositoryImpl().loadedEntities.containsKey(id);
	}

	@SafeVarargs
	protected final ID[] ids(ID... ids)
	{
		return ids;
	}

	protected Set<ID> idsOf(Collection<E> entities)
	{
		return entities.stream().map(Entity::getId).collect(Collectors.toSet());
	}

	protected static <T> boolean containsSame(Collection<T> objects, T object)
	{
		return objects.stream().anyMatch(o -> o == object);
	}

	protected E findById(Collection<E> entities, ID id)
	{
		return entities.stream().filter(entity -> entity.getId().equals(id)).findFirst().orElse(null);
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
		DO changedDataObject = this.seedDataObject(dataObject.getId(), false, true, null);

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

	// ---------------------------------------------------------------------------------------------------------------
	// get(ID[]) / get(Collection)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * S1: null isn't accepted as id array.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getIds_nullArray_throwsIllegalArgument() throws Exception
	{
		this.getRepository().get((ID[]) null);
	}

	/**
	 * S1: null isn't accepted as id collection.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getCollection_null_throwsIllegalArgument() throws Exception
	{
		this.getRepository().get((Collection<ID>) null);
	}

	/**
	 * S2: no ids provide an empty stream without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_emptyIds_returnsEmptyStreamWithoutQuery() throws Exception
	{
		this.seedDataObject(false);

		Assert.assertTrue(collect(this.getRepository().get(this.ids())).isEmpty());
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S3: known entities are provided as the same instances without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_onlyKnownIds_returnsCachedInstancesWithoutQuery() throws Exception
	{
		E loaded = this.loadEntity();
		E added = this.addEntity();

		this.seedDataObject(false);
		this.getDataAccessObject().resetStatistics();

		Collection<E> entities = collect(this.getRepository().get(this.ids(loaded.getId(), added.getId())));

		Assert.assertEquals(2, entities.size());
		Assert.assertTrue(containsSame(entities, loaded));
		Assert.assertTrue(containsSame(entities, added));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S4: known entities are provided as the same instances; the DAO is only asked for the unknown ids,
	 * whose entities are loaded with the state Unchanged and cached.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_knownAndUnknownIds_loadsOnlyUnknownIds() throws Exception
	{
		E known = this.loadEntity();
		DO unknownDataObject = this.seedDataObject(false);

		this.getDataAccessObject().resetStatistics();

		Collection<E> entities = collect(this.getRepository().get(this.ids(known.getId(), unknownDataObject.getId())));
		E loaded = this.findById(entities, unknownDataObject.getId());

		Assert.assertEquals(2, entities.size());
		Assert.assertTrue(containsSame(entities, known));
		Assert.assertNotNull(loaded);
		Assert.assertEquals(Entity.State.Unchanged, loaded.getState());
		Assert.assertEquals(1, this.getDataAccessObject().getQueries().size());
		Assert.assertEquals("get(ids)", this.getDataAccessObject().getQueries().get(0).getMethod());
		Assert.assertEquals(Arrays.asList(unknownDataObject.getId()), this.getDataAccessObject().getQueries().get(0).getIds());

		this.getDataAccessObject().resetStatistics();

		Assert.assertSame(loaded, this.getRepository().get(unknownDataObject.getId()));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S5: each entity is provided once, even if its id is requested multiple times (Q10).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_duplicateIds_providesEachEntityOnce() throws Exception
	{
		E known = this.loadEntity();
		DO unknownDataObject = this.seedDataObject(false);

		this.getDataAccessObject().resetStatistics();

		Collection<E> entities = collect(this.getRepository().get(this.ids(known.getId(), unknownDataObject.getId(), known.getId(), unknownDataObject.getId())));

		Assert.assertEquals(2, entities.size());
		Assert.assertEquals(Arrays.asList(unknownDataObject.getId()), this.getDataAccessObject().getQueries().get(0).getIds());
	}

	/**
	 * S6: the entity of a pending removal is omitted without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_pendingRemoval_omitsWithoutQuery() throws Exception
	{
		E entity = this.loadEntity();

		this.getRepository().remove(entity);
		this.getDataAccessObject().resetStatistics();

		Assert.assertTrue(collect(this.getRepository().get(this.ids(entity.getId()))).isEmpty());
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S7: a virtual id without a known entity is omitted without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_unknownVirtualId_omitsWithoutQuery() throws Exception
	{
		E entity = this.getRepository().createEntity();

		Assert.assertTrue(collect(this.getRepository().get(this.ids(entity.getId()))).isEmpty());
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S8: a garbage collected id without a data object is omitted and removed from the cache.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_garbageCollectedIdWithoutDataObject_omitsAndRemovesIdFromCache() throws Exception
	{
		E entity = this.loadEntity();

		this.getDataAccessObject().delete(entity.getId());
		this.simulateGc(entity.getId());

		Assert.assertTrue(collect(this.getRepository().get(this.ids(entity.getId()))).isEmpty());
		Assert.assertFalse(this.isCached(entity.getId()));
	}

	/**
	 * S9: a garbage collected id whose data object is flagged deleted is omitted and removed from the cache (Q11).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_garbageCollectedIdFlaggedDeleted_omitsAndRemovesIdFromCache() throws Exception
	{
		E entity = this.loadEntity();

		this.seedDataObject(entity.getId(), true, false, null);
		this.simulateGc(entity.getId());

		Assert.assertTrue(collect(this.getRepository().get(this.ids(entity.getId()))).isEmpty());
		Assert.assertFalse(this.isCached(entity.getId()));
	}

	/**
	 * S10: a garbage collected id with a data object is loaded as a new instance.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_garbageCollectedIdWithDataObject_loadsNewInstance() throws Exception
	{
		E entity = this.loadEntity();

		this.simulateGc(entity.getId());

		Collection<E> entities = collect(this.getRepository().get(this.ids(entity.getId())));

		Assert.assertEquals(1, entities.size());
		Assert.assertFalse(containsSame(entities, entity));
		Assert.assertTrue(this.isCached(entity.getId()));
	}

	/**
	 * S11: a second call with the same ids provides the same instances without querying the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_calledTwice_returnsSameInstances() throws Exception
	{
		DO dataObject1 = this.seedDataObject(false);
		DO dataObject2 = this.seedDataObject(false);

		Collection<E> first = collect(this.getRepository().get(this.ids(dataObject1.getId(), dataObject2.getId())));

		this.getDataAccessObject().resetStatistics();

		Collection<E> second = collect(this.getRepository().get(this.ids(dataObject1.getId(), dataObject2.getId())));

		Assert.assertEquals(2, second.size());
		for (E entity : first) Assert.assertTrue(containsSame(second, entity));
		Assert.assertTrue(this.getQueriedMethods().isEmpty());
	}

	/**
	 * S12: closing the returned stream closes the stream of the DAO (Q15).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_streamClosed_closesDataAccessObjectStream() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		collect(this.getRepository().get(this.ids(dataObject.getId())));

		Assert.assertEquals(1, this.getDataAccessObject().getOpenedStreams());
		Assert.assertEquals(1, this.getDataAccessObject().getClosedStreams());
	}

	/**
	 * S13: a failure of the DAO's stream while the returned stream is consumed is thrown as {@link DALRuntimeException} (Q12).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_daoStreamFails_throwsDALRuntimeException() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		this.getDataAccessObject().failNextStream(new IllegalStateException("test"));

		try
		{
			collect(this.getRepository().get(this.ids(dataObject.getId())));

			Assert.fail();
		}
		catch (DALRuntimeException e)
		{
			Assert.assertEquals(1, this.getDataAccessObject().getClosedStreams());
		}
	}

	/**
	 * S14: a failure of the conversion while the returned stream is consumed is thrown as {@link DataObjectConversionException}
	 * with the data object's id and the original exception as cause (Q12).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getIds_conversionFails_throwsDataObjectConversionExceptionWithId() throws Exception
	{
		DO dataObject = this.seedDataObject(false);
		RuntimeException failure = new RuntimeException("test");

		this.failNextConversion(failure);

		try
		{
			collect(this.getRepository().get(this.ids(dataObject.getId())));

			Assert.fail();
		}
		catch (DataObjectConversionException e)
		{
			Assert.assertEquals(dataObject.getId(), e.getDataObjectId());
			Assert.assertSame(failure, e.getCause());
		}
	}

	/**
	 * S15: {@link GenericRepository#get(Collection)} behaves like {@link GenericRepository#get(Serializable[])}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getCollection_knownUnknownAndDuplicateIds_behavesLikeArray() throws Exception
	{
		E known = this.loadEntity();
		DO unknownDataObject = this.seedDataObject(false);

		this.getDataAccessObject().resetStatistics();

		Collection<E> fromCollection = collect(this.getRepository().get(Arrays.asList(known.getId(), unknownDataObject.getId(), known.getId())));
		Collection<E> fromArray = collect(this.getRepository().get(this.ids(known.getId(), unknownDataObject.getId(), known.getId())));

		Assert.assertEquals(2, fromCollection.size());
		Assert.assertTrue(containsSame(fromCollection, known));
		for (E entity : fromCollection) Assert.assertTrue(containsSame(fromArray, entity));
		Assert.assertEquals(Arrays.asList(unknownDataObject.getId()), this.getDataAccessObject().getQueries().get(0).getIds());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// getAll(false)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * A1: an empty repository with an empty storage provides nothing.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_emptyRepositoryAndStorage_returnsEmpty() throws Exception
	{
		Assert.assertTrue(collect(this.getRepository().getAll(false)).isEmpty());
	}

	/**
	 * A2: added entities which haven't been persisted yet are included (Q13).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_addedEntity_isIncluded() throws Exception
	{
		E entity = this.addEntity();

		Collection<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
	}

	/**
	 * A3: a known entity whose data object is in the storage is provided once, as the same instance.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_knownEntityWithDataObject_providesItOnceAsSameInstance() throws Exception
	{
		E entity = this.loadEntity();

		Collection<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
	}

	/**
	 * A4: data objects without a known entity are loaded as Unchanged entities and cached,
	 * so a second call provides the same instances.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_dataObjectsWithoutKnownEntity_loadsAndCachesUnchangedEntities() throws Exception
	{
		this.seedDataObject(false);
		this.seedDataObject(false);

		Collection<E> first = collect(this.getRepository().getAll(false));
		Collection<E> second = collect(this.getRepository().getAll(false));

		Assert.assertEquals(2, first.size());
		for (E entity : first) Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		Assert.assertEquals(2, second.size());
		for (E entity : first) Assert.assertTrue(containsSame(second, entity));
	}

	/**
	 * A5: the entity of a pending removal is omitted, although its data object is still in the storage.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_pendingRemoval_isOmitted() throws Exception
	{
		E entity = this.loadEntity();

		this.getRepository().remove(entity);

		Assert.assertTrue(collect(this.getRepository().getAll(false)).isEmpty());
	}

	/**
	 * A6: data objects flagged deleted are omitted.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_dataObjectFlaggedDeleted_isOmitted() throws Exception
	{
		this.seedDataObject(true);

		Assert.assertTrue(collect(this.getRepository().getAll(false)).isEmpty());
	}

	/**
	 * A7: a cached entity keeps its values if its data object is modified in the storage (Q3).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_dataObjectChangedInStorage_providesCachedInstanceWithOldValues() throws Exception
	{
		DO dataObject = this.seedDataObject(false);
		E entity = this.getRepository().get(dataObject.getId());

		this.seedDataObject(dataObject.getId(), false, true, null);

		Collection<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
		Assert.assertTrue(this.hasValuesOf(entity, dataObject));
	}

	/**
	 * A8: a cached entity is still provided if its data object is deleted in the storage (Q3, O3).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_dataObjectDeletedInStorage_providesCachedInstance() throws Exception
	{
		E entity = this.loadEntity();

		this.getDataAccessObject().delete(entity.getId());

		Collection<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
	}

	/**
	 * A9: a garbage collected id with a data object is loaded as a new instance.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_garbageCollectedIdWithDataObject_loadsNewInstance() throws Exception
	{
		E entity = this.loadEntity();

		this.simulateGc(entity.getId());

		Collection<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, entities.size());
		Assert.assertFalse(containsSame(entities, entity));
		Assert.assertTrue(this.isCached(entity.getId()));
	}

	/**
	 * A10: a garbage collected id without a data object is omitted and removed from the cache.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_garbageCollectedIdWithoutDataObject_omitsAndRemovesIdFromCache() throws Exception
	{
		E entity = this.loadEntity();

		this.getDataAccessObject().delete(entity.getId());
		this.simulateGc(entity.getId());

		Assert.assertTrue(collect(this.getRepository().getAll(false)).isEmpty());
		Assert.assertFalse(this.isCached(entity.getId()));
	}

	/**
	 * A11: closing the returned stream closes the stream of the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_streamClosed_closesDataAccessObjectStream() throws Exception
	{
		this.seedDataObject(false);

		collect(this.getRepository().getAll(false));

		Assert.assertEquals(1, this.getDataAccessObject().getOpenedStreams());
		Assert.assertEquals(1, this.getDataAccessObject().getClosedStreams());
	}

	/**
	 * A12: a failure of the DAO's stream while the returned stream is consumed is thrown as {@link DALRuntimeException}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = DALRuntimeException.class)
	public void getAll_daoStreamFails_throwsDALRuntimeException() throws Exception
	{
		this.seedDataObject(false);
		this.getDataAccessObject().failNextStream(new IllegalStateException("test"));

		collect(this.getRepository().getAll(false));
	}

	/**
	 * A12: a failure of the conversion while the returned stream is consumed is thrown as {@link DataObjectConversionException}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_conversionFails_throwsDataObjectConversionExceptionWithId() throws Exception
	{
		DO dataObject = this.seedDataObject(false);

		this.failNextConversion(new RuntimeException("test"));

		try
		{
			collect(this.getRepository().getAll(false));

			Assert.fail();
		}
		catch (DataObjectConversionException e)
		{
			Assert.assertEquals(dataObject.getId(), e.getDataObjectId());
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// getAll(true)
	// ---------------------------------------------------------------------------------------------------------------

	protected static final long BEFORE_LAST_MODIFICATION = 1000L;
	protected static final long LAST_MODIFICATION = 2000L;
	protected static final long AFTER_LAST_MODIFICATION = 3000L;

	/**
	 * R1: only data objects modified after the DAO's last modification time stamp are loaded; known entities are included.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_olderAndNewerDataObjects_providesNewerOnesAndKnownEntities() throws Exception
	{
		DO olderDataObject = this.seedDataObject(BEFORE_LAST_MODIFICATION);
		DO newerDataObject = this.seedDataObject(AFTER_LAST_MODIFICATION);
		E added = this.addEntity();

		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);

		Collection<E> entities = collect(this.getRepository().getAll(true));

		Assert.assertEquals(new HashSet<>(Arrays.asList(newerDataObject.getId(), added.getId())), this.idsOf(entities));
		Assert.assertNull(this.findById(entities, olderDataObject.getId()));
	}

	/**
	 * R2: after the stream was consumed to its end, the next call doesn't report the same data objects again;
	 * only the known entities are provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_consumedToEnd_reportsNothingNewNextTime() throws Exception
	{
		this.seedDataObject(AFTER_LAST_MODIFICATION);
		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);

		Collection<E> first = collect(this.getRepository().getAll(true));
		Collection<E> second = collect(this.getRepository().getAll(true));

		Assert.assertEquals(AFTER_LAST_MODIFICATION, this.getDataAccessObject().getLastModifiedTimeStamp());
		Assert.assertEquals(1, first.size());
		Assert.assertEquals(1, second.size());
		Assert.assertTrue(containsSame(second, first.iterator().next()));
	}

	/**
	 * R3: if the stream isn't consumed to its end, the next call reports the same data objects again (Q14).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_notConsumedToEnd_reportsSameDataObjectsAgain() throws Exception
	{
		DO dataObject1 = this.seedDataObject(AFTER_LAST_MODIFICATION);
		DO dataObject2 = this.seedDataObject(AFTER_LAST_MODIFICATION + 1);

		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);

		try (Stream<E> entityStream = this.getRepository().getAll(true))
		{
			Assert.assertTrue(entityStream.findFirst().isPresent());
		}

		Assert.assertEquals(LAST_MODIFICATION, this.getDataAccessObject().getLastModifiedTimeStamp());
		Assert.assertEquals(new HashSet<>(Arrays.asList(dataObject1.getId(), dataObject2.getId())), this.idsOf(collect(this.getRepository().getAll(true))));
	}

	/**
	 * R4: a garbage collected id which isn't reported by getRecent() is requested at the end of the stream and provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_garbageCollectedIdNotReported_requestsItAtTheEnd() throws Exception
	{
		DO dataObject = this.seedDataObject(BEFORE_LAST_MODIFICATION);
		E entity = this.getRepository().get(dataObject.getId());

		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);
		this.simulateGc(entity.getId());
		this.getDataAccessObject().resetStatistics();

		Collection<E> entities = collect(this.getRepository().getAll(true));

		Assert.assertEquals(1, entities.size());
		Assert.assertFalse(containsSame(entities, entity));
		Assert.assertEquals(Arrays.asList("getRecent", "get(ids)"), this.getQueriedMethods());
		Assert.assertEquals(Arrays.asList(dataObject.getId()), this.getDataAccessObject().getQueries().get(1).getIds());
	}

	/**
	 * R5: without garbage collected ids, no ids are requested.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_noGarbageCollectedIds_doesNotRequestIds() throws Exception
	{
		DO dataObject = this.seedDataObject(BEFORE_LAST_MODIFICATION);
		E entity = this.getRepository().get(dataObject.getId());

		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);
		this.getDataAccessObject().resetStatistics();

		Collection<E> entities = collect(this.getRepository().getAll(true));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
		Assert.assertEquals(Arrays.asList("getRecent"), this.getQueriedMethods());
	}

	/**
	 * R6: after a reset, data objects which haven't changed since the DAO's last modification time stamp aren't provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_afterReset_doesNotProvideUnchangedDataObjects() throws Exception
	{
		DO dataObject = this.seedDataObject(BEFORE_LAST_MODIFICATION);

		this.getRepository().get(dataObject.getId());
		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);
		this.getRepository().reset();

		Assert.assertTrue(collect(this.getRepository().getAll(true)).isEmpty());
	}

	/**
	 * R7: added entities which haven't been persisted yet are included.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllRecent_addedEntity_isIncluded() throws Exception
	{
		E entity = this.addEntity();

		this.getDataAccessObject().setLastModifiedTimeStamp(LAST_MODIFICATION);

		Collection<E> entities = collect(this.getRepository().getAll(true));

		Assert.assertEquals(1, entities.size());
		Assert.assertTrue(containsSame(entities, entity));
	}
}
