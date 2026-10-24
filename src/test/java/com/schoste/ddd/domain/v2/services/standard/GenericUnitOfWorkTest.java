package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.services.GenericRepository;
import com.schoste.ddd.domain.v2.services.UnitOfWork;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * End-to-end tests of a repository together with a Unit of Work ({@link UnitOfWork}) and a real DAO (layer W of the test grid).
 * They only use the public interfaces, so they run on every DAO implementation.
 * The Javadoc of each test names its id in the test grid.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the identifiers of the entities and data objects
 * @param <E> the entity class
 * @param <DO> the data object class
 * @param <R> the repository class
 */
public abstract class GenericUnitOfWorkTest<ID extends Serializable, E extends Entity<ID>, DO extends GenericDataObject<ID>, R extends GenericRepository<ID, E, DO>>
{
	@Autowired
	protected ApplicationContext applicationContext;

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
	protected abstract GenericDataAccessObject<ID, DO> getDataAccessObject();

	/**
	 * Changes the values of an entity
	 *
	 * @param entity the entity to change
	 */
	protected abstract void changeValues(E entity);

	/**
	 * Checks if an entity has the values of a data object
	 *
	 * @param entity the entity to check
	 * @param dataObject the data object with the expected values
	 * @return true if the entity has the values of the data object
	 */
	protected abstract boolean hasValuesOf(E entity, DO dataObject);

	@SuppressWarnings("unchecked")
	protected GenericRepositoryImpl<ID, E, DO> getRepositoryImpl()
	{
		return (GenericRepositoryImpl<ID, E, DO>) this.getRepository();
	}

	/**
	 * Creates a new Unit of Work for the repository under test
	 *
	 * @return the new Unit of Work
	 */
	protected UnitOfWork createUnitOfWork()
	{
		return this.applicationContext.getBean(UnitOfWork.class, this.getRepository());
	}

	/**
	 * Adds a new entity and commits it
	 *
	 * @param uow the Unit of Work to commit with
	 * @return the persisted entity
	 * @throws Exception re-throws every exception
	 */
	protected E persistEntity(UnitOfWork uow) throws Exception
	{
		E entity = this.getRepository().createEntity();

		this.getRepository().add(entity);

		uow.commit();

		return entity;
	}

	protected static <T> List<T> collect(Stream<T> stream)
	{
		try (Stream<T> closingStream = stream)
		{
			return closingStream.collect(Collectors.toList());
		}
	}

	protected static <T> boolean containsSame(Collection<T> objects, T object)
	{
		return objects.stream().anyMatch(o -> o == object);
	}

	protected void clearAndReset() throws Exception
	{
		this.getDataAccessObject().clear();
		this.getRepository().reset();
	}

	@Before
	public void setUp() throws Exception
	{
		this.clearAndReset();
	}

	@After
	public void tearDown() throws Exception
	{
		this.clearAndReset();
	}

	/**
	 * W1: committing an added entity persists it; it becomes Unchanged, gets an actual id and stays the cached instance.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commit_addedEntity_persistsItAsUnchangedWithActualId() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.getRepository().createEntity();
		ID virtualId = entity.getId();

		this.getRepository().add(entity);

		Assert.assertEquals(virtualId, entity.getId());

		uow.commit();

		Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		Assert.assertNotEquals(virtualId, entity.getId());
		Assert.assertFalse(this.getRepository().isVirtualId(entity.getId()));
		Assert.assertSame(entity, this.getRepository().get(entity.getId()));
		Assert.assertNotNull(this.getDataAccessObject().get(entity.getId()));
	}

	/**
	 * W2: only committed entities become Unchanged; entities added after the commit stay Added.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commit_entitiesAddedBeforeAndAfter_onlyCommittedOnesAreUnchanged() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		Collection<E> committed = new ArrayList<>();
		Collection<E> uncommitted = new ArrayList<>();

		for (int i=0; i<5; i++) committed.add(this.getRepository().createEntity());
		for (E entity : committed) this.getRepository().add(entity);

		uow.commit();

		for (int i=0; i<5; i++) uncommitted.add(this.getRepository().createEntity());
		for (E entity : uncommitted) this.getRepository().add(entity);

		List<E> entities = collect(this.getRepository().getAll(false));

		Assert.assertEquals(10, entities.size());
		for (E entity : committed) Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		for (E entity : uncommitted) Assert.assertEquals(Entity.State.Added, entity.getState());
		Assert.assertEquals(5, collect(this.getDataAccessObject().getAll()).size());
	}

	/**
	 * W3: committing a modified entity writes its values to the storage; it becomes Unchanged.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commit_modifiedEntity_updatesStorage() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.persistEntity(uow);

		this.changeValues(entity);
		this.getRepository().setModified(entity);

		uow.commit();

		Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		Assert.assertTrue(this.hasValuesOf(entity, this.getDataAccessObject().get(entity.getId())));
	}

	/**
	 * W4: committing a removal deletes the data object; the entity becomes Detached and its removal is no longer pending.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commit_removedEntity_deletesItFromStorage() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.persistEntity(uow);

		this.getRepository().remove(entity);

		Assert.assertNull(this.getRepository().get(entity.getId()));

		uow.commit();

		DO dataObject = this.getDataAccessObject().get(entity.getId());

		Assert.assertEquals(Entity.State.Detached, entity.getState());
		Assert.assertTrue((dataObject == null) || dataObject.getIsDeleted());
		Assert.assertFalse(this.getRepositoryImpl().isPendingRemoval(entity.getId()));
		Assert.assertNull(this.getRepository().get(entity.getId()));
	}

	/**
	 * W5: adding several entities and removing some of them keeps the remaining ones.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commit_addAndRemoveMultiple_keepsTheRemainingEntities() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		List<E> entities = new ArrayList<>();

		for (int i=0; i<10; i++) entities.add(this.persistEntity(uow));

		List<E> removed = entities.subList(0, 5);
		List<E> remaining = entities.subList(5, 10);

		for (E entity : removed) this.getRepository().remove(entity);

		uow.commit();

		List<E> actual = collect(this.getRepository().getAll(false));

		Assert.assertEquals(5, actual.size());
		for (E entity : removed) Assert.assertEquals(Entity.State.Detached, entity.getState());
		for (E entity : removed) Assert.assertFalse(containsSame(actual, entity));
		for (E entity : remaining) Assert.assertEquals(Entity.State.Unchanged, entity.getState());
		for (E entity : remaining) Assert.assertTrue(containsSame(actual, entity));
	}

	/**
	 * W6: rolling back an added entity detaches it; nothing is persisted, not even by a later commit (Q16).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void rollback_addedEntity_detachesItWithoutPersisting() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.getRepository().createEntity();

		this.getRepository().add(entity);

		uow.rollback();
		uow.commit();

		Assert.assertEquals(Entity.State.Detached, entity.getState());
		Assert.assertNull(this.getRepository().get(entity.getId()));
		Assert.assertTrue(collect(this.getRepository().getAll(false)).isEmpty());
		Assert.assertTrue(collect(this.getDataAccessObject().getAll()).isEmpty());
	}

	/**
	 * W7: rolling back a removal forgets the entity; the data object stays in the storage and is loaded as a new instance (Q16).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void rollback_removedEntity_forgetsItAndLoadsItAgain() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.persistEntity(uow);

		this.getRepository().remove(entity);

		uow.rollback();
		uow.commit();

		E reloaded = this.getRepository().get(entity.getId());

		Assert.assertEquals(Entity.State.Detached, entity.getState());
		Assert.assertNotNull(reloaded);
		Assert.assertNotSame(entity, reloaded);
		Assert.assertEquals(Entity.State.Unchanged, reloaded.getState());
	}

	/**
	 * W8: rolling back a modification forgets the entity; the change isn't written by a later commit,
	 * and the entity is loaded again with the values of the storage (Q16).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void rollback_modifiedEntity_forgetsItAndDiscardsTheChange() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E entity = this.persistEntity(uow);

		this.changeValues(entity);
		this.getRepository().setModified(entity);

		uow.rollback();
		uow.commit();

		DO dataObject = this.getDataAccessObject().get(entity.getId());
		E reloaded = this.getRepository().get(entity.getId());

		Assert.assertEquals(Entity.State.Detached, entity.getState());
		Assert.assertFalse(this.hasValuesOf(entity, dataObject));
		Assert.assertNotSame(entity, reloaded);
		Assert.assertTrue(this.hasValuesOf(reloaded, dataObject));
	}

	/**
	 * W9: closing a Unit of Work with uncommitted changes rolls them back.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void close_uncommittedChanges_rollsThemBack() throws Exception
	{
		UnitOfWork uow = this.createUnitOfWork();
		E removed = this.persistEntity(uow);
		E added = this.getRepository().createEntity();

		this.getRepository().add(added);
		this.getRepository().remove(removed);

		uow.close();

		Assert.assertEquals(Entity.State.Detached, added.getState());
		Assert.assertEquals(Entity.State.Detached, removed.getState());
		Assert.assertNotNull(this.getDataAccessObject().get(removed.getId()));
		Assert.assertEquals(1, collect(this.getDataAccessObject().getAll()).size());
	}
}
