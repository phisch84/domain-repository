package com.schoste.ddd.domain.v2.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.services.standard.GenericRepositoryImpl;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Basic test class for all repository implementations
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the identifiers of the domain objects and data objects
 * @param <T> the domain object class
 * @param <DO> the data object class
 * @param <R> the repository class
 */
public abstract class GenericRepositoryTest<ID extends Serializable, T extends Entity<ID>, DO extends GenericDataObject<ID>, R extends GenericRepository<ID, T, DO>>
{
	@Autowired
	protected ApplicationContext applicationContext;

	@Autowired
	protected GenericDataAccessObject<ID, DO> repoDataAccessObject;

	protected abstract R getRepository();

	/**
	 * Checks if a domain object was persisted already, i.e. its id is not virtual
	 *
	 * @param domainObject the domain object to check
	 * @return true if the domain object has an id which is not virtual, false otherwise
	 */
	protected boolean isPersisted(T domainObject)
	{
		return (domainObject.getId() != null) && !this.repoDataAccessObject.isVirtualId(domainObject.getId());
	}
	
	/**
	 * Gets the initialized UoW to use for committing
	 * 
	 * @return the initialized UoW to use for committing
	 */
	protected UnitOfWork getUnitOfWork() throws Exception
	{
		return this.applicationContext.getBean(UnitOfWork.class, this.getRepository());
	}
	
	/**
	 * Gets all repositories which should be tracked by the UoW
	 * 
	 * @return an array of repositories to be passed on to the UoW
	 */
	protected GenericRepositoryImpl<?,?,?>[] getRepositories()
	{
		GenericRepositoryImpl<?,?,?>[] repositories = new GenericRepositoryImpl<?,?,?>[1];

		repositories[0] = (GenericRepositoryImpl<?,?,?>) this.getRepository();
		
		return repositories;
	}
	
	private static String getEnclosingMethodName()
	{
		return (new Throwable()).getStackTrace()[1].getMethodName();
	}
	
	/**
	 * Creates a new instance of a domain object.
	 * Overwrite the method to control the creation of test domain objects
	 * 
	 * @param callingMethodName the name of the method that called
	 * @param callId a number that identifies the position of the call
	 * @return a new domain model
	 * @throws Exception re-throws every exception
	 */
	protected T createDomainObject(String callingMethodName, int callId) throws Exception
	{
		return this.getRepository().createEntity();
	}
	
	/**
	 * Creates Data Object in the Data Access Object(s) which can be used by the repository under test
	 * 
	 * @param callingMethodName the name of the method that called
	 * @param callId a number that identifies the position of the call
	 * @throws Exception re-throws every exception
	 */
	protected abstract void createDataObjects(String callingMethodName, int callId) throws Exception;
	
	/**
	 * Compares two domain objects in the context of testing.
	 * 
	 * @param one the object to compare to another
	 * @param another the object to compare to one
	 * @return true if they are considered equal, false otherwise
	 * @throws Exception re-throws every exception
	 */
	protected abstract boolean compareDomainObjects(T one, T another) throws Exception;
	
	/**
	 * Removes all data objects from the data access object and resets all repositories,
	 * so every test starts with empty storage and doesn't leave data objects behind
	 */
	protected void clearAndResetRepositories() throws Exception
	{
		this.repoDataAccessObject.clear();

		GenericRepositoryImpl<?,?,?>[] repositories = this.getRepositories();

		for (GenericRepositoryImpl<?,?,?> repository : repositories) repository.reset();
	}

	/**
	 * Clears the storage and resets all repositories before executing any test
	 */
	@Before
	public void setUp() throws Exception
	{
		this.clearAndResetRepositories();
	}

	/**
	 * Clears the storage and resets all repositories after executing any test
	 */
	@After
	public void tearDown() throws Exception
	{
		this.clearAndResetRepositories();
	}
	
	/**
	 * Ensures that domain objects are created correctly and filled with all
	 * expected default values by the repo (the id of the new object is
	 * virtual and the state of the object is Detached).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testCreateObject() throws Exception
	{
		int NUM_NEW_DOS = 5;
		Collection<ID> domainObjectIds = new ArrayList<ID>(NUM_NEW_DOS);

		for (int i=0; i<NUM_NEW_DOS; i++)
		{
			T domainObject = this.createDomainObject(getEnclosingMethodName(), 0);

			// Make sure the Domain Object was actually created
			Assert.assertNotNull(domainObject);

			ID domainObjectId = domainObject.getId();

			// Make sure the Domain Object has a virtual Id
			Assert.assertNotNull(domainObjectId);
			Assert.assertTrue(this.repoDataAccessObject.isVirtualId(domainObjectId));

			// Make sure the Id of the Domain Object is equal on subsequent calls
			Assert.assertEquals(domainObjectId, domainObject.getId());
			Assert.assertEquals(domainObjectId, domainObject.getId());

			// Make sure the Id of the Domain Object is unique
			Assert.assertFalse(domainObjectIds.contains(domainObjectId));
			
			domainObjectIds.add(domainObject.getId());

			// Make sure the state of the new Domain Object is Detached
			Assert.assertEquals(Entity.State.Detached, domainObject.getState());
			
		}
	}
	
	/**
	 * Tests if an added object can be retrieved by its id without committing it
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddAndGetWithoutCommit() throws Exception
	{
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		ID domainObjectId = newDomainObject.getId();

		this.getRepository().add(newDomainObject);

		T domainObject = this.getRepository().get(domainObjectId);
		
		Assert.assertNotNull(domainObject);
		Assert.assertEquals(domainObjectId, domainObject.getId());		
		Assert.assertTrue(this.compareDomainObjects(newDomainObject, domainObject));
	}

	/**
	 * Tests if an added object can be retrieved by its id after it
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddAndGetWithCommit() throws Exception
	{
		UnitOfWork uow = this.getUnitOfWork();

		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		ID oldDomainObjectId = newDomainObject.getId();

		this.getRepository().add(newDomainObject);

		uow.commit();

		Assert.assertNotEquals(oldDomainObjectId, newDomainObject.getId());
		
		T domainObject = this.getRepository().get(newDomainObject.getId());
		
		Assert.assertNotNull(domainObject);
		Assert.assertNotEquals(oldDomainObjectId, domainObject.getId());
		Assert.assertTrue(this.compareDomainObjects(newDomainObject, domainObject));
	}

	/**
	 * Ensures that adding one newly created domain object actually adds it to the repository
	 * so it can be retrieved later.
	 * Also ensures that the id of the domain object isn't changed before it is committed.
	 * Also ensures that the state of the domain objects is set to Added after adding it.
	 * Also ensures that the state of the domain objects is set to Unchanged after committing.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAdd() throws Exception
	{
		UnitOfWork uow = this.getUnitOfWork();
		
		Collection<T> oldDomainObjects = this.getRepository().getAll(false).toList();
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		ID oldDomainObjectId = newDomainObject.getId();

		this.getRepository().add(newDomainObject);

		Assert.assertEquals(Entity.State.Added, newDomainObject.getState());
		Assert.assertEquals(oldDomainObjectId, newDomainObject.getId());

		uow.commit();

		Assert.assertEquals(Entity.State.Unchanged, newDomainObject.getState());
		Assert.assertTrue(this.isPersisted(newDomainObject));

		Collection<T> gotDomainObjects = this.getRepository().getAll(false).toList();
		
		Assert.assertEquals(oldDomainObjects.size() +1, gotDomainObjects.size());
		
		for (T oldDomainObject : oldDomainObjects)
		{
			Assert.assertNotEquals(oldDomainObject, newDomainObject);
			Assert.assertEquals(Entity.State.Unchanged, oldDomainObject.getState());
		}
		
		boolean wasEqual = true;
		
		for (T gotDomainObject : gotDomainObjects)
		{
			if (newDomainObject.equals(gotDomainObject))
			{
				wasEqual = true;
				break;
			}
		}
		
		Assert.assertTrue(wasEqual);
	}
	
	/**
	 * Tests adding multiple objects subsequently. Ensures that only committed objects have the
	 * state Unchanged.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddMultiple() throws Exception
	{
		int NUM_NEW_DOS = 5;
		
		UnitOfWork uow = this.getUnitOfWork();
		
		Collection<T> oldDomainObjects = this.getRepository().getAll(false).toList();
		
		// Add new Domain Objects for committing
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), i));
		
		Collection<T> newDomainObjects = this.getRepository().getAll(false).toList();
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS, newDomainObjects.size());

		uow.commit();
		
		// Add new Domain Objects after committing and see if all objects are in the repository
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), NUM_NEW_DOS+i));
		
		Collection<T> newNewDomainObjects = this.getRepository().getAll(false).toList();
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS+NUM_NEW_DOS, newNewDomainObjects.size());
		
		// Test the states of the Domain Objects
		ArrayList<T> newNewDomainObjectsList = new ArrayList<T>(newNewDomainObjects);
		
		for (int i=0; i<newNewDomainObjectsList.size(); i++)
		{
			T domainObject = newNewDomainObjectsList.get(i);
			
			// If the Domain Object was committed, its state is supposed to be Unchanged 
			if (newDomainObjects.contains(domainObject)) Assert.assertEquals(Entity.State.Unchanged, domainObject.getState());
			
			// otherwise it should still be Added
			else Assert.assertEquals(Entity.State.Added, domainObject.getState());
		}
	}

	/**
	 * Tests adding an existing Domain Object twice.
	 * Ensures the state doesn't change from Added to anything else before committing.
	 * Ensures the state changes from Added to Unchanged after committing.
	 * Ensures the id doesn't change before committing.
	 * Ensures the id changes after committing.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddExisting() throws Exception
	{
		UnitOfWork uow = this.getUnitOfWork();
		
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		ID newDomainObjectId = newDomainObject.getId();
		
		Assert.assertEquals(Entity.State.Detached, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());

		this.getRepository().add(newDomainObject);

		Assert.assertEquals(Entity.State.Added, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());

		this.getRepository().add(newDomainObject);

		Assert.assertEquals(Entity.State.Added, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());
		
		uow.commit();

		Assert.assertEquals(Entity.State.Unchanged, newDomainObject.getState());
		Assert.assertNotEquals(newDomainObjectId, newDomainObject.getId());
	}

	/**
	 * Tests adding a new Domain Object and removing it without committing it.
	 * The state of the Domain Object should be Detached after removing, because
	 * it was not persisted yet.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddRemoveNoCommit() throws Exception
	{
		@SuppressWarnings("unused") // Still add a UoW to make sure it doesn't interfere
		UnitOfWork uow = this.getUnitOfWork();
		
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		
		this.getRepository().add(newDomainObject);
		this.getRepository().remove(newDomainObject);
		
		Assert.assertEquals(Entity.State.Detached, newDomainObject.getState());
		
		ArrayList<T> domainObjectsList = new ArrayList<T>(this.getRepository().getAll(false).toList());
		
		Assert.assertFalse(domainObjectsList.contains(newDomainObject));
	}
	
	/**
	 * Tests adding a new Domain Object and removing it with committing it.
	 * The state of the Domain Object should be Deleted after removing, because
	 * it was already persisted.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddRemoveWithCommit() throws Exception
	{
		UnitOfWork uow = this.getUnitOfWork();
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		
		this.getRepository().add(newDomainObject);
		
		uow.commit();
		
		Collection<T> oldDomainObjects = this.getRepository().getAll(false).toList();
		
		this.getRepository().remove(newDomainObject);
		
		// Before the repo is committed, the state is supposed to be Deleted
		Assert.assertEquals(Entity.State.Deleted, newDomainObject.getState());

		uow.commit();

		ArrayList<T> domainObjectsList = new ArrayList<T>(this.getRepository().getAll(false).toList());
		
		Assert.assertEquals(oldDomainObjects.size() -1, domainObjectsList.size());
		Assert.assertFalse(domainObjectsList.contains(newDomainObject));
		
		// After the repo is committed, the state is supposed to be Detached
		Assert.assertEquals(Entity.State.Detached, newDomainObject.getState());
	}
	
	/**
	 * Tests adding new Domain Objects and removing some with committing it.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testAddRemoveMultipleWithCommit() throws Exception
	{
		int NUM_NEW_DOS = 10;
		int NUM_REM_DOS =  5;
		
		UnitOfWork uow = this.getUnitOfWork();

		Collection<T> oldDomainObjects = this.getRepository().getAll(false).toList();
		
		// Add some domain objects
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), i));
				
		uow.commit();
		
		ArrayList<T> addedDomainObjectsList = new ArrayList<T>(this.getRepository().getAll(false).toList());
		
		Assert.assertEquals(oldDomainObjects.size() + NUM_NEW_DOS, addedDomainObjectsList.size());

		for (T domainObject : addedDomainObjectsList) Assert.assertTrue(this.isPersisted(domainObject));
		
		
		// Remove some domain objects from the added objects
		ArrayList<T> toRemoveDomainObjectsList = new ArrayList<T>(NUM_REM_DOS);

		Collections.shuffle(addedDomainObjectsList);

		for (int i=0; i<NUM_REM_DOS; i++)
		{
			T domainObjectToRemove = addedDomainObjectsList.get(i);
			
			toRemoveDomainObjectsList.add(domainObjectToRemove);
			addedDomainObjectsList.remove(domainObjectToRemove);
			
			this.getRepository().remove(domainObjectToRemove);
			
			// Before the repo is committed, the state is supposed to be Deleted
			Assert.assertEquals(Entity.State.Deleted, domainObjectToRemove.getState());
		}
		
		uow.commit();

		ArrayList<T> actualDomainObjectsList = new ArrayList<T>(this.getRepository().getAll(false).toList());
		
		for (int i=0; i<toRemoveDomainObjectsList.size(); i++)
		{
			T domainObjectToRemove = toRemoveDomainObjectsList.get(i);

			// After the repo is committed, the state is supposed to be Detached
			Assert.assertEquals(Entity.State.Detached, domainObjectToRemove.getState());
			
			Assert.assertFalse(actualDomainObjectsList.contains(domainObjectToRemove));
		}
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS -NUM_REM_DOS, actualDomainObjectsList.size());
		
		for (int i=0; i<addedDomainObjectsList.size(); i++)
		{
			T existingDomainObject = addedDomainObjectsList.get(i);

			Assert.assertTrue(actualDomainObjectsList.contains(existingDomainObject));

			// After the repo is committed, the state is supposed to be Unchanged
			Assert.assertEquals(Entity.State.Unchanged, existingDomainObject.getState());			
		}
	}

	/**
	 * Tests if a domain object is loaded from DAO when using the get method
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetNotAdded() throws Exception
	{
		UnitOfWork uow = this.getUnitOfWork();
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		
		this.getRepository().add(newDomainObject);
		
		uow.commit();
		
		ID objectId = newDomainObject.getId();

		((GenericRepositoryImpl<?,?,?>)this.getRepository()).reset();
		
		T hopefullyLoadedDomainObject = this.getRepository().get(objectId);
		
		Assert.assertNotNull(hopefullyLoadedDomainObject);
		Assert.assertEquals(objectId, hopefullyLoadedDomainObject.getId());
	}

	/**
	 * Asserts that the {@link GenericRepository#getAll(java.util.function.Predicate)} method of a Repository implementation
	 * actually returns a stream with all expected objects since no predicate (null) was provided
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllStreamedWithoutPredicate() throws Exception
	{
		try (Stream<T> doStream = this.getRepository().getAll(false))
		{
			// Loaded domain objects were persisted, so their ids can't be virtual
			doStream.forEach(domainObject
				-> Assert.assertTrue(this.isPersisted(domainObject)));
		}
	}

}
