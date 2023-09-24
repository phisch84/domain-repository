package com.schoste.ddd.domain.v1.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.domain.v1.models.DomainObject.State;
import com.schoste.ddd.domain.v1.services.standard.GenericRepositoryImpl;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Basic test class for all repository implementations
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <T> the domain object class
 * @param <DO> the data object class
 * @param <R> the repository class
 */
public abstract class GenericRepositoryTest<T extends DomainObject, DO extends GenericDataObject, R extends GenericRepository<T, DO>> 
{
	@Autowired
	protected ApplicationContext applicationContext;
	
	@Autowired
	protected GenericDataAccessObject<DO> repoDataAccessObject;
	
	protected abstract R getRepository();
	
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
	protected GenericRepositoryImpl<?,?>[] getRepositories()
	{
		GenericRepositoryImpl<?,?>[] repositories = new GenericRepositoryImpl<?,?>[1];
		
		repositories[0] = (GenericRepositoryImpl<?,?>) this.getRepository();
		
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
		return this.getRepository().createObject();
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
	 * Resets all repositories before executing any test
	 */
	@Before
	public void resetRepositories() throws Exception
	{
		GenericRepositoryImpl<?,?>[] repositories = this.getRepositories();
		
		for (GenericRepositoryImpl<?,?> repository : repositories) repository.reset();
	}
	
	/**
	 * Ensures that domain objects are created correctly and filled with all
	 * expected default values by the repo (the id of the new object is less
	 * than zero and the state of the object is Detached).
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testCreateObject() throws Exception
	{
		int NUM_NEW_DOS = 5;
		Collection<Integer> domainObjectIds = new ArrayList<Integer>(NUM_NEW_DOS);
		
		for (int i=0; i<NUM_NEW_DOS; i++)
		{
			T domainObject = this.createDomainObject(getEnclosingMethodName(), 0);

			// Make sure the Domain Object was actually created
			Assert.assertNotNull(domainObject);
			
			int domainObjectId = domainObject.getId();
			
			// Make sure the Id of the Domain Object is equal on subsequent calls
			Assert.assertEquals(domainObjectId, domainObject.getId());
			Assert.assertEquals(domainObjectId, domainObject.getId());

			// Make sure the Id of the Domain Object is unique
			Assert.assertFalse(domainObjectIds.contains(domainObjectId));
			
			domainObjectIds.add(domainObject.getId());

			// Make sure the state of the new Domain Object is Detached
			Assert.assertEquals(DomainObject.State.Detached, domainObject.getState());
			
		}
	}
	
	/**
	 * Tests if objects are correctly reloaded
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testReload() throws Exception
	{
		int NUM_NEW_DOS = 10;
		int NUM_VIRT_DOS = 5;
		int NUM_DEL_DOS = 5;
		
		UnitOfWork uow = this.getUnitOfWork();
		Collection<T> existingDomainObjects = this.getRepository().getAll();
		
		for (T domainObject : existingDomainObjects) this.getRepository().remove(domainObject);
		
		uow.commit();
		
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), i));
		
		uow.commit();

		List<T> newDomainObjects = new ArrayList<>(this.getRepository().getAll());
		List<T> delDomainObjects = new ArrayList<>(NUM_DEL_DOS);
		List<T> modDomainObjects = new ArrayList<>(NUM_NEW_DOS-NUM_DEL_DOS);
		List<T> virDomainObjects = new ArrayList<>(NUM_VIRT_DOS);

		for (int i=0; i<NUM_VIRT_DOS; i++) 
		{
			T virtualDomainObject = this.createDomainObject(getEnclosingMethodName(), NUM_NEW_DOS+i);
			
			this.getRepository().add(virtualDomainObject);
			
			virDomainObjects.add(virtualDomainObject);
		}

		Collections.shuffle(newDomainObjects);
		
		for (int i=0; i<NUM_DEL_DOS; i++) 
		{
			this.getRepository().remove(newDomainObjects.get(i));
			
			delDomainObjects.add(newDomainObjects.get(i));
		}
		
		for (int i=NUM_DEL_DOS; i<NUM_NEW_DOS -1; i++) 
		{
			this.getRepository().setModified(newDomainObjects.get(i));
			
			modDomainObjects.add(newDomainObjects.get(i));
		}
		
		T unmodifiedDomainObject = newDomainObjects.get(NUM_NEW_DOS -1);
		
		this.getRepository().reload();
		
		// If this assertion fails, make sure you set the scope of your data object to prototype in the bean definition!
		Assert.assertEquals(State.Unchanged, unmodifiedDomainObject.getState());
		
		for (T domainObject : modDomainObjects) Assert.assertEquals(State.Unchanged, domainObject.getState());
		for (T domainObject : delDomainObjects) Assert.assertEquals(State.Detached, domainObject.getState());
		for (T domainObject : virDomainObjects) Assert.assertEquals(State.Detached, domainObject.getState());
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
		int domainObjectId = newDomainObject.getId();
		
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
		int oldDomainObjectId = newDomainObject.getId();
		
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
		
		Collection<T> oldDomainObjects = this.getRepository().getAll();
		T newDomainObject = this.createDomainObject(getEnclosingMethodName(), 0);
		int oldDomainObjectId = newDomainObject.getId();
		
		this.getRepository().add(newDomainObject);
		
		Assert.assertEquals(DomainObject.State.Added, newDomainObject.getState());
		Assert.assertEquals(oldDomainObjectId, newDomainObject.getId());
		
		uow.commit();
		
		Assert.assertEquals(DomainObject.State.Unchanged, newDomainObject.getState());
		Assert.assertTrue(newDomainObject.getId() > 0);

		Collection<T> gotDomainObjects = this.getRepository().getAll();
		
		Assert.assertEquals(oldDomainObjects.size() +1, gotDomainObjects.size());
		
		for (T oldDomainObject : oldDomainObjects)
		{
			Assert.assertNotEquals(oldDomainObject, newDomainObject);
			Assert.assertEquals(DomainObject.State.Unchanged, oldDomainObject.getState());
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
		
		Collection<T> oldDomainObjects = this.getRepository().getAll();
		
		// Add new Domain Objects for committing
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), i));
		
		Collection<T> newDomainObjects = this.getRepository().getAll();
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS, newDomainObjects.size());

		uow.commit();
		
		// Add new Domain Objects after committing and see if all objects are in the repository
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), NUM_NEW_DOS+i));
		
		Collection<T> newNewDomainObjects = this.getRepository().getAll();
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS+NUM_NEW_DOS, newNewDomainObjects.size());
		
		// Test the states of the Domain Objects
		ArrayList<T> newNewDomainObjectsList = new ArrayList<T>(newNewDomainObjects);
		
		for (int i=0; i<newNewDomainObjectsList.size(); i++)
		{
			T domainObject = newNewDomainObjectsList.get(i);
			
			// If the Domain Object was committed, its state is supposed to be Unchanged 
			if (newDomainObjects.contains(domainObject)) Assert.assertEquals(DomainObject.State.Unchanged, domainObject.getState());
			
			// otherwise it should still be Added
			else Assert.assertEquals(DomainObject.State.Added, domainObject.getState());
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
		int newDomainObjectId = newDomainObject.getId();
		
		Assert.assertEquals(DomainObject.State.Detached, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());

		this.getRepository().add(newDomainObject);

		Assert.assertEquals(DomainObject.State.Added, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());

		this.getRepository().add(newDomainObject);

		Assert.assertEquals(DomainObject.State.Added, newDomainObject.getState());
		Assert.assertEquals(newDomainObjectId, newDomainObject.getId());
		
		uow.commit();

		Assert.assertEquals(DomainObject.State.Unchanged, newDomainObject.getState());
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
		
		Assert.assertEquals(DomainObject.State.Detached, newDomainObject.getState());
		
		ArrayList<T> domainObjectsList = new ArrayList<T>(this.getRepository().getAll());
		
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
		
		Collection<T> oldDomainObjects = this.getRepository().getAll();
		
		this.getRepository().remove(newDomainObject);
		
		// Before the repo is committed, the state is supposed to be Deleted
		Assert.assertEquals(DomainObject.State.Deleted, newDomainObject.getState());

		uow.commit();

		ArrayList<T> domainObjectsList = new ArrayList<T>(this.getRepository().getAll());
		
		Assert.assertEquals(oldDomainObjects.size() -1, domainObjectsList.size());
		Assert.assertFalse(domainObjectsList.contains(newDomainObject));
		
		// After the repo is committed, the state is supposed to be Detached
		Assert.assertEquals(DomainObject.State.Detached, newDomainObject.getState());
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

		Collection<T> oldDomainObjects = this.getRepository().getAll();
		
		// Add some domain objects
		for (int i=0; i<NUM_NEW_DOS; i++) this.getRepository().add(this.createDomainObject(getEnclosingMethodName(), i));
				
		uow.commit();
		
		ArrayList<T> addedDomainObjectsList = new ArrayList<T>(this.getRepository().getAll());
		
		Assert.assertEquals(oldDomainObjects.size() + NUM_NEW_DOS, addedDomainObjectsList.size());

		for (T domainObject : addedDomainObjectsList) Assert.assertNotEquals(0, domainObject.getId()); 
		
		
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
			Assert.assertEquals(DomainObject.State.Deleted, domainObjectToRemove.getState());
		}
		
		uow.commit();

		ArrayList<T> actualDomainObjectsList = new ArrayList<T>(this.getRepository().getAll());
		
		for (int i=0; i<toRemoveDomainObjectsList.size(); i++)
		{
			T domainObjectToRemove = toRemoveDomainObjectsList.get(i);

			// After the repo is committed, the state is supposed to be Detached
			Assert.assertEquals(DomainObject.State.Detached, domainObjectToRemove.getState());
			
			Assert.assertFalse(actualDomainObjectsList.contains(domainObjectToRemove));
		}
		
		Assert.assertEquals(oldDomainObjects.size() +NUM_NEW_DOS -NUM_REM_DOS, actualDomainObjectsList.size());
		
		for (int i=0; i<addedDomainObjectsList.size(); i++)
		{
			T existingDomainObject = addedDomainObjectsList.get(i);

			Assert.assertTrue(actualDomainObjectsList.contains(existingDomainObject));

			// After the repo is committed, the state is supposed to be Unchanged
			Assert.assertEquals(DomainObject.State.Unchanged, existingDomainObject.getState());			
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
		
		int objectId = newDomainObject.getId();
		
		((GenericRepositoryImpl<?,?>)this.getRepository()).reset();
		
		T hopefullyLoadedDomainObject = this.getRepository().get(objectId);
		
		Assert.assertNotNull(hopefullyLoadedDomainObject);
		Assert.assertEquals(objectId, hopefullyLoadedDomainObject.getId());
	}
}
