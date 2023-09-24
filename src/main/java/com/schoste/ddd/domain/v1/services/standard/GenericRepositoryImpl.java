package com.schoste.ddd.domain.v1.services.standard;

import java.lang.ref.SoftReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import com.schoste.ddd.domain.v1.exceptions.DataObjectNullException;
import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.domain.v1.models.DomainObject.State;
import com.schoste.ddd.domain.v1.services.AutoObjectConverter;
import com.schoste.ddd.domain.v1.services.GenericRepository;
import com.schoste.ddd.domain.v1.services.RepositoryChangeListener;
import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Implementation of the generic repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <T> the domain model class
 * @param <DO> the data object class
 */
@Repository
public abstract class GenericRepositoryImpl<T extends DomainObject, DO extends GenericDataObject> implements GenericRepository<T, DO> 
{
	@Autowired
	protected ApplicationContext applicationContext;

	@Autowired
	protected AutoObjectConverter converter;
	
	protected Set<RepositoryChangeListener> changeListener = new HashSet<RepositoryChangeListener>();
	protected Map<Integer, SoftReference<T>> loadedObjects = new HashMap<Integer, SoftReference<T>>();
	protected int lastVirtualId = -1;
	
	private void notifyObjectAdded(T domainObject)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectAdded(this, domainObject);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private void notifyObjectRemoved(T domainObject)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectRemoved(this, domainObject);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private void notifyObjectModified(T domainObject)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectModified(this, domainObject);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private void notifyReload()
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onReload(this);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Obtains the actual class of domain objects
	 * 
	 * @return the actual domain class
	 * @throws IllegalStateException thrown if the actual domain class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDomainObjectClass() throws IllegalStateException, Exception
	{
		Type[] types = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments();

		if (types.length < 1) throw new IllegalStateException();

		return (Class) types[0];
	}
	
	/**
	 * Obtains the actual class of data objects
	 * 
	 * @return the actual data class
	 * @throws IllegalStateException thrown if the actual data class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDataObjectClass() throws IllegalStateException, Exception
	{
		Type[] types = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments();

		if (types.length < 2) throw new IllegalStateException();

		return (Class) types[1];
	}
	
	/**
	 * Gets the data access object (DAO) which is used to actually persist
	 * the domain models
	 * 
	 * @return a data access object (DAO)
	 */
	protected abstract GenericDataAccessObject<?> getDataAccessObject();

	/**
	 * Called by {@see GenericRepositoryImpl#dataObjectToDomainObject(GenericDataObject, DomainObject)}
	 * after the domain object was enriched by the auto converter
	 * 
	 * @param dataObject the data object with source information
	 * @param domainObject the domain object to enrich with information
	 * @throws Exception re-throws every exception
	 */
	protected abstract void afterAutoConversation(DO dataObject, T domainObject) throws Exception;

	/**
	 * Creates a new domain model or updates an existing from a data object.
	 * When overwriting this method <b>DO NOT</b> set the id of the domain object.
	 * <b>Rather call the method of the super class.</b> The method of the super class will
	 * update the id and <b>it will make sure the object in the repository is updated too!</b>
	 * The returned domain model is supposed to have the state Unchanged on return.
	 * 
	 * @param dataObject the data object a source
	 * @param domainObject an existing domain model to update with the provided data object. If null a new one will be created
	 * @return the new or updated domain model
	 * @throws IllegalArgumentException thrown if the dataObject is null
	 * @throws Exception re-throws every exception
	 */
	protected synchronized T dataObjectToDomainObject(DO dataObject, T domainObject) throws Exception
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");
		if (domainObject == null) domainObject = this.createObject();

		this.converter.convert(dataObject, domainObject);
		this.afterAutoConversation(dataObject, domainObject);

		// Check if the DO exists in the repo already and if so, update it
		boolean existed = (this.loadedObjects.remove(domainObject.getId()) != null);
		
		domainObject.setId(dataObject.getId());
		domainObject.setState(State.Unchanged);
		
		if (existed) this.loadedObjects.put(domainObject.getId(), new SoftReference<T>(domainObject));
		
		return domainObject;
	}

	/**
	 * Creates a new domain model or updates an existing from a data object by calling the
	 * typed method dataObjectToDomainObject().
	 * 
	 * @param dataObject the data object a source
	 * @param domainObject an existing domain model to update with the provided data object. If null a new one will be created
	 * @return the new or updated domain model
	 * @throws IllegalArgumentException if the classes of the dataObject or the domainObject do not match the classes of the repository or the data access object
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected T dataObjectToDomainObject(Object dataObject, Object domainObject) throws Exception
	{
		if ((dataObject != null) && (!this.getDataObjectClass().isInstance(dataObject))) throw new IllegalArgumentException("dataObject");
		if ((domainObject != null) && (!this.getDomainObjectClass().isInstance(domainObject))) throw new IllegalArgumentException("domainObject");
		
		return this.dataObjectToDomainObject((DO) dataObject, (T) domainObject);
	}

	/**
	 * Called by {@see GenericRepositoryImpl#domainObjectToDataObject(DomainObject)}
	 * after the data object was enriched by the auto converter
	 * 
	 * @param domainObject the domain object to enrich with information with source information
	 * @param dataObject the data object to enrich with information
	 * @throws Exception re-throws every exception
	 */
	protected abstract void afterAutoConversation(T domainObject, DO dataObject) throws Exception;

	/**
	 * Creates a data object from a given domain model (so it can be persisted).
	 * When overwriting this method <b>make sure to call the method of the super class.</b>
	 * When calling the method of the super class, the repository will check if there
	 * already exists a data object with the given id that you need to update.
	 * 
	 * @param domainObject the domain model to create the data object from
	 * @return a data object or null if no data object exists yet for the given domain model's id
	 * @throws IllegalArgumentException thrown when any of the parameters is null
	 * @throw DataObjectNullException thrown if no data object could be created
	 * @throws Exception re-throws every exception
	 */
	protected DO domainObjectToDataObject(T domainObject) throws Exception
	{
		if (domainObject == null) throw new IllegalArgumentException("domainObject");

		@SuppressWarnings("unchecked")
		DO dataObject = (domainObject.getId() > 0) ? (DO) this.getDataAccessObject().get(domainObject.getId()) : (DO) this.getDataAccessObject().createDataObject();

		if (dataObject == null) throw new DataObjectNullException(this.getDataAccessObject().getClass());

		dataObject.setId(domainObject.getId());
		dataObject.setIsDeleted(false);

		this.converter.convert(domainObject, dataObject);
		this.afterAutoConversation(domainObject, dataObject);

		return dataObject;
	}
	
	/**
	 * Creates a data object from a given domain model (so it can be persisted)
	 * 
	 * @param domainObject the domain model to create the data object from
	 * @return a data object
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected DO domainObjectToDataObject(Object domainObject) throws Exception
	{
		if ((domainObject != null) && (!this.getDomainObjectClass().isInstance(domainObject))) throw new IllegalArgumentException("domainObject");
		
		return this.domainObjectToDataObject((T) domainObject);
	}
	
	/**
	 * Resets the virtual id for not persisted domain objects to the next lowest value.
	 * If multiple UoWs track a repository and one of them commits it might still be
	 * the case that some domain objects are neither persisted nor removed and therefore
	 * still exist with virtual Ids.
	 */
	protected synchronized void resetVirtualDomainObjectId()
	{
		int minVirtualId = 0;
		
		for (int domainObjectId : this.loadedObjects.keySet()) minVirtualId = Math.min(minVirtualId, domainObjectId);
		
		this.lastVirtualId = --minVirtualId;
	}
	
	/**
	 * Tries to get a domain object from the cache. If it isn't found in cache
	 * (either because it expired or was not loaded yet) it is reloaded from DAL
	 * 
	 * @param id the id of the domain object to get/load
	 * @return the domain object with the given id or null if it does not exist at all
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected synchronized T loadDomainObject(int id) throws Exception
	{
		T domainObject = null;
		
		if (this.loadedObjects.containsKey(id))
		{
			// The object was loaded once; get it from cache
			SoftReference<T> refToDomainObject = this.loadedObjects.get(id);
			
			domainObject = refToDomainObject.get();
		}
		
		if (domainObject == null)
		{
			// The object was not loaded yet or was removed from cache; reload it
			DO dataObject = (DO) this.getDataAccessObject().get(id);
			
			if (dataObject != null)
			{
				// An object with the given id exists in the DAL; load it
				domainObject = this.createObject();
				domainObject = this.dataObjectToDomainObject(dataObject, domainObject);
				
				SoftReference<T> refToDomainObject = new SoftReference<T>(domainObject);
				
				this.loadedObjects.put(domainObject.getId(), refToDomainObject);
			}
		}
		
		return domainObject;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void addChangeListener(RepositoryChangeListener listener)
	{
		synchronized (this.changeListener)
		{
			if (listener != null) this.changeListener.add(listener);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void removeChangeListener(RepositoryChangeListener listener)
	{
		synchronized (this.changeListener)
		{
			if (listener != null) this.changeListener.remove(listener);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public T createObject() throws DomainException
	{
		try
		{
			@SuppressWarnings("unchecked")
			T obj = (T) this.applicationContext.getBean(this.getDomainObjectClass());
			
			obj.setId(this.lastVirtualId--);
			obj.setState(State.Detached);
			
			return obj;
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public synchronized void reload() throws DALException, DomainException
	{
		try
		{
			this.notifyReload();
			
			for (SoftReference<T> loadedObjectRef : this.loadedObjects.values())
			{
				T existingObject = loadedObjectRef.get();
				
				if (existingObject != null) existingObject.setState(State.Detached);
			}
	
			Collection<?> newDataObjects = (Collection<?>) this.getDataAccessObject().reloadAll();
	
			for (Object newObject : newDataObjects) 
			{
				DO newDataObject = (DO) newObject;
				T newDomainObject = this.dataObjectToDomainObject(newDataObject, this.get(newDataObject.getId()));
	
				this.loadedObjects.put(newDomainObject.getId(), new SoftReference<T>(newDomainObject));
			}
			
			Collection<Integer> existingObjectIds = new ArrayList<>(this.loadedObjects.keySet());
			
			for (int existingObjectId : existingObjectIds)
			{
				SoftReference<T> existingObjectRef = this.loadedObjects.get(existingObjectId);
				T existingObject = existingObjectRef.get();
				
				if (existingObject == null) this.loadedObjects.remove(existingObjectId);
				if (existingObject.getState() == State.Detached) this.loadedObjects.remove(existingObject.getId());
			}
		}
		catch (DALException e)
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
	public T get(int id) throws DALException, DomainException
	{
		try
		{
			T domainObject = this.loadDomainObject(id);
			
			return domainObject;
		}
		catch (DALException e)
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
	@SuppressWarnings("unchecked")
	public synchronized Collection<T> getAll() throws DALException, DomainException
	{
		try
		{
			Collection<?> newDataObjects = (Collection<?>) this.getDataAccessObject().getAll();
			
			for (Object newObject : newDataObjects) 
			{
				DO newDataObject = (DO) newObject;
				T newDomainObject = this.dataObjectToDomainObject(newDataObject, this.get(newDataObject.getId()));
				
				this.loadedObjects.put(newDomainObject.getId(), new SoftReference<T>(newDomainObject));
			}
			
			Collection<T> domainObjects = new ArrayList<T>(this.loadedObjects.size());
	
			for (int domainObjectId : this.loadedObjects.keySet())
			{
				T domainObject = this.loadDomainObject(domainObjectId);
				
				domainObjects.add(domainObject);
			}
			
			return domainObjects;
		}
		catch (DALException e)
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
	public synchronized void add(T domainObject) throws DomainException
	{
		try
		{
			if (domainObject == null) throw new IllegalArgumentException("domainObject");
			if ((domainObject.getState() != State.Detached) && (domainObject.getState() != State.Added)) throw new IllegalStateException();
	
			if (!this.loadedObjects.containsKey(domainObject.getId()))
			{
				this.loadedObjects.put(domainObject.getId(), new SoftReference<T>(domainObject));
				
				domainObject.setState(State.Added);
				
				this.notifyObjectAdded(domainObject);
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized void remove(T domainObject) throws DomainException
	{
		try
		{
			if (domainObject == null) throw new IllegalArgumentException("domainObject");
	
			if (this.loadedObjects.containsKey(domainObject.getId()))
			{
				this.loadedObjects.remove(domainObject.getId());
				
				if (domainObject.getState() == State.Unchanged)	domainObject.setState(State.Deleted);
				if (domainObject.getState() == State.Modified)	domainObject.setState(State.Deleted);
				if (domainObject.getState() == State.Added) domainObject.setState(State.Detached);
				
				this.notifyObjectRemoved(domainObject);
			}
			else
			{
				// The object doesn't exist in the repo anyways, so detach it.
				domainObject.setState(State.Detached);
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setModified(T domainObject) throws DomainException
	{
		try
		{
			if (domainObject == null) throw new IllegalArgumentException("domainObject");
			if (domainObject.getState() != State.Unchanged) return;
			
			domainObject.setState(State.Modified);
			this.notifyObjectModified(domainObject);
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void add(Object object) throws DomainException
	{
		try
		{
			T domainObject = (T) object;
	
			this.add(domainObject);
		}
		catch (DomainException e)
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
	@SuppressWarnings("unchecked")
	public void remove(Object object) throws DomainException
	{
		try
		{
			T domainObject = (T) object;
			
			this.remove(domainObject);
		}
		catch (DomainException e)
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
	@SuppressWarnings("unchecked")
	public void setModified(Object object) throws DomainException
	{
		try
		{
			T domainObject = (T) object;
			
			this.setModified(domainObject);
		}
		catch (DomainException e)
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
	public synchronized void reset() throws DomainException
	{
		try
		{
			synchronized (this.changeListener)
			{
				this.changeListener.clear();
				this.loadedObjects.clear();
				this.resetVirtualDomainObjectId();
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
}
