package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.entities.Entity.State;
import com.schoste.ddd.domain.v2.exceptions.DataObjectNullException;
import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.domain.v2.services.AutoObjectConverter;
import com.schoste.ddd.domain.v2.services.GenericRepository;
import com.schoste.ddd.domain.v2.services.GenericEntityLoader;
import com.schoste.ddd.domain.v2.services.RepositoryChangeListener;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Implementation of the generic repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the identifiers of the domain models and data objects
 * @param <E> the entity class
 * @param <DO> the data object class
 */
abstract public class GenericRepositoryImpl<ID extends Serializable, E extends Entity<ID>, DO extends GenericDataObject<ID>> implements GenericRepository<ID, E, DO>
{	
	protected Set<RepositoryChangeListener> changeListener = new HashSet<RepositoryChangeListener>();
	protected Map<ID, WeakReference<E>> loadedEntities = new HashMap<ID, WeakReference<E>>();

	private void notifyObjectAdded(E entity)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectAdded(this, entity);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	private void notifyObjectRemoved(E entity)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectRemoved(this, entity);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	private void notifyObjectModified(E entity)
	{
		try
		{
			synchronized (this.changeListener)
			{
				for (RepositoryChangeListener listener : this.changeListener) listener.onObjectModified(this, entity);
			}
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Creates a new, unique virtual identifier for an entity (see {@link Entity#getId()}) which also identifies it
	 * as not (yet) persisted
	 * @return a new, unique virtual identifier
	 */
	abstract protected ID getNextVirtualId();

	/**
	 * Returns an instance to the implementation of an {@link AutoObjectConverter} which will be used in
	 * data object to entity conversion methods (and visa versa)
	 * 
	 * @return an instance to the implementation of an {@link AutoObjectConverter}
	 */
	abstract protected AutoObjectConverter getAutoObjectConverter();

	/**
	 * The method which creates the actual instance of a new entity. This instance will then be used by
	 * {@link GenericRepositoryImpl#createEntity(Object...)}.
	 * To be implemented by the deriving class.
	 * 
	 * @param entityClass the class type of the entity to create
	 * @param args optional arguments. Their usage depends on the implementation of the method.
	 * @return a new instance of a repository's entity.
	 * @throws Exception re-throws every exception
	 */
	abstract protected E doCreateEntity(Class<E> entityClass, Object... args) throws Exception;

	/**
	 * Gets the data access object (DAO) which is used to actually persist
	 * the domain models
	 *
	 * @return a data access object (DAO)
	 */
	abstract protected GenericDataAccessObject<ID, DO> getDataAccessObject();

	/**
	 * Called by {@link GenericRepositoryImpl#dataObjectToDomainObject(GenericDataObject, DomainObject)}
	 * after the entity was enriched by the auto converter
	 * 
	 * @param dataObject the data object with source information
	 * @param entity the entity to enrich with information
	 * @throws Exception re-throws every exception
	 */
	abstract protected void afterAutoConversation(DO dataObject, E entity) throws Exception;

	/**
	 * Called by {@link GenericRepositoryImpl#domainObjectToDataObject(DomainObject)}
	 * after the data object was enriched by the auto converter
	 * 
	 * @param entity the entity to enrich with information with source information
	 * @param dataObject the data object to enrich with information
	 * @throws Exception re-throws every exception
	 */
	abstract protected void afterAutoConversation(E entity, DO dataObject) throws Exception;

	/**
	 * Returns an iterable object that will poll the next available data object from the data source
	 * 
	 * To be overwritten by the extending class.
	 * 
	 * @param <ID> the class of the data object's and entity's unique identifier
	 * @param <DO> the class of the data object to load
	 * @param <E> the class of the entity to load
	 * @param args optional arguments for the creation of the entity loader. Their usage depends on the actual implementation
	 * @return an iterable object that will poll the next available data object from the data source
	 * @throws Exception re-throws every exception
	 */
	abstract protected GenericEntityLoader<ID, DO, E> createEntityLoader(Object... args) throws Exception;

	abstract protected GenericEntityLoader<ID, DO, E> createEntityLoader(Collection<E> cachedEntities, Stream<DO> dataObjStream) throws Exception;

	/**
	 * Calls an operation and returns its result.
	 * A {@link DALException} and a {@link DomainException} thrown by the operation is re-thrown as it is,
	 * every other exception is wrapped in a {@link DomainException}.
	 *
	 * @param <T> the class of the operation's result
	 * @param operation the operation to call
	 * @return the result of the operation
	 * @throws DALException re-thrown if the operation catches a {@link DALException}
	 * @throws DomainException thrown if the operation throws any exception
	 */
	static protected <T> T call(Callable<T> operation) throws DALException, DomainException
	{
		try
		{
			return operation.call();
		}
		catch (DALException e)
		{
			throw e;
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

		if (types.length < 2) throw new IllegalStateException();

		return (Class) types[1];
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

		if (types.length < 3) throw new IllegalStateException();

		return (Class) types[2];
	}

	/**
	 * Checks if an entity was already persisted, i.e. it has an id which is not virtual
	 * (see {@link GenericDataAccessObject#isVirtualId(Serializable)}).
	 *
	 * @param entity the entity to check
	 * @return true if the entity was persisted already, false otherwise
	 */
	protected boolean isPersisted(E entity)
	{
		ID id = entity.getId();

		return (id != null) && !this.getDataAccessObject().isVirtualId(id);
	}

	/**
	 * Checks if an object is an entity of this repository which was already persisted
	 * by calling the typed method isPersisted().
	 *
	 * @param entity the object to check
	 * @return true if the object is an entity of this repository and was persisted already, false otherwise
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected boolean isPersisted(Object entity) throws Exception
	{
		if ((entity == null) || (!this.getDomainObjectClass().isInstance(entity))) return false;

		return this.isPersisted((E) entity);
	}

	/**
	 * Creates a new entity from a data object.
	 * The returned entity is supposed to have the state Unchanged on return and must be cached by this repository
	 * so that the contracts for the repository's get() methods are still sound.
	 * 
	 * @param dataObject the data object a source
	 * @param entity an existing entity to update with the provided data object. If null a new one will be created
	 * @return the new or updated entity
	 * @throws IllegalArgumentException thrown if the dataObject is null
	 * @throws Exception re-throws every exception
	 */
	protected synchronized E dataObjectToDomainObject(DO dataObject, E entity) throws Exception
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");
		if (entity == null) entity = this.createEntity();

		this.getAutoObjectConverter().convert(dataObject, entity);
		this.afterAutoConversation(dataObject, entity);

		// Check if the DO exists in the repo already and if so, update it
		boolean existed = (this.loadedEntities.remove(entity.getId()) != null);
		
		entity.setId(dataObject.getId());
		entity.setState(State.Unchanged);
		
		if (existed) this.loadedEntities.put(entity.getId(), new WeakReference<E>(entity));
		
		return entity;
	}

	/**
	 * Creates a new entity from a data object by calling the typed method dataObjectToDomainObject()
	 * 
	 * @param dataObject the data object a source
	 * @param entity an existing entity to update with the provided data object. If null a new one will be created
	 * @return the new or updated entity
	 * @throws IllegalArgumentException if the classes of the dataObject or the entity do not match the classes of the repository or the data access object
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected E dataObjectToDomainObject(Object dataObject, Object entity) throws Exception
	{
		if ((dataObject != null) && (!this.getDataObjectClass().isInstance(dataObject))) throw new IllegalArgumentException("dataObject");
		if ((entity != null) && (!this.getDomainObjectClass().isInstance(entity))) throw new IllegalArgumentException("entity");
		
		return this.dataObjectToDomainObject((DO) dataObject, (E) entity);
	}

	/**
	 * Creates a data object from a given entity (so it can be persisted).
	 * When overwriting this method <b>make sure to call the method of the super class.</b>
	 * When calling the method of the super class, the repository will check if there
	 * already exists a data object with the given id that you need to update.
	 * 
	 * @param entity the entity to create the data object from
	 * @return a data object or null if no data object exists yet for the given entity's id
	 * @throws IllegalArgumentException thrown when any of the parameters is null
	 * @throw DataObjectNullException thrown if no data object could be created
	 * @throws Exception re-throws every exception
	 */
	protected DO domainObjectToDataObject(E entity) throws Exception
	{
		if (entity == null) throw new IllegalArgumentException("entity");

		DO dataObject = this.isPersisted(entity) ? this.getDataAccessObject().get(entity.getId()) : this.getDataAccessObject().createDataObject();

		if (dataObject == null) throw new DataObjectNullException(this.getDataAccessObject().getClass());

		dataObject.setId(entity.getId());
		dataObject.setIsDeleted(false);

		this.getAutoObjectConverter().convert(entity, dataObject);
		this.afterAutoConversation(entity, dataObject);

		return dataObject;
	}
	
	/**
	 * Creates a data object from a given entity (so it can be persisted)
	 * 
	 * @param entity the entity to create the data object from
	 * @return a data object
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected DO domainObjectToDataObject(Object entity) throws Exception
	{
		if ((entity != null) && (!this.getDomainObjectClass().isInstance(entity))) throw new IllegalArgumentException("entity");
		
		return this.domainObjectToDataObject((E) entity);
	}
	
	/**
	 * Tries to get an entity from the cache.
	 * If it isn't found in the cache and the id is an actual id then it is reloaded from DAL.
	 * If it isn't found in the cache and the id is a virtual id then null is returned.
	 * 
	 * @param id the id of the entity to get/load
	 * @return the entity with the given id or null if it does not exist
	 * @throws Exception re-throws every exception
	 */
	protected synchronized E loadDomainObject(ID id) throws Exception
	{
		E entity = null;
		
		if (this.loadedEntities.containsKey(id))
		{
			// The object was loaded once; get it from cache
			WeakReference<E> refToDomainObject = this.loadedEntities.get(id);
			
			entity = refToDomainObject.get();
		}
		
		if (entity == null)
		{
			// virtual ids will not exist in the DAO's storage -> return null
			if (this.isVirtualId(id)) return null;

			// The object was not loaded yet or was removed from cache; reload it
			DO dataObject = this.getDataAccessObject().get(id);
			
			if (dataObject != null)
			{
				// An object with the given id exists in the DAL; load it
				entity = this.createEntity();
				entity = this.dataObjectToDomainObject(dataObject, entity);
				
				WeakReference<E> refToDomainObject = new WeakReference<E>(entity);
				
				this.loadedEntities.put(entity.getId(), refToDomainObject);
			}
		}
		
		return entity;
	}

	/**
	 * {@inheritDoc}
	 */
	abstract public boolean isVirtualId(ID id) throws IllegalArgumentException;

	/**
	 * {@inheritDoc}
	 */
	@Override
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
	@Override
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
	@Override
	public E createEntity(Object... args) throws DomainException
	{
		try
		{
			@SuppressWarnings("unchecked")
			E obj = (E) this.doCreateEntity(this.getDomainObjectClass(), args);

			// the DAO assigns a new virtual id to every data object it creates
			obj.setId(this.getNextVirtualId());
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
	@Override
	public E get(ID id) throws DALException, DomainException
	{
		try
		{
			E entity = this.loadDomainObject(id);
			
			return entity;
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
	@Override
	public Stream<E> get(ID[] ids) throws IllegalArgumentException, DALException, DomainException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		E entity = null;
		WeakReference<E> refToDomainObject = null;
		Collection<E> cachedEntities = new ArrayList<>(ids.length);
		Set<ID> missingIds = new HashSet<>(ids.length);

		try
		{
			for (int i=0; i<ids.length; i++)
			{		
				if (!this.loadedEntities.containsKey(ids[i]))
				{
					// only add actual ids for a reload from the DAO
					if (!this.isVirtualId(ids[i])) missingIds.add(ids[i]);

					continue;
				}

				refToDomainObject = this.loadedEntities.get(ids[i]);
				entity = refToDomainObject.get();

				// the entity exists in the cache, so somewhere its reference is held -> do not reload / overwrite ...
				if (entity != null)
				{
					// but make sure to store its hard reference
					cachedEntities.add(entity);

					continue;
				}

				// only add actual ids for a reload from the DAO
				if (!this.isVirtualId(ids[i])) missingIds.add(ids[i]);

				// remove the ids with dead references. If it was a virtual id, the entity is abandoned.
				// If it was an actual id, the entity will be reloaded from the DAO and added back to the cache if it still exists
				this.loadedEntities.remove(ids[i]);
			}

			Stream<DO> dataObjStream = this.getDataAccessObject().get(missingIds);
			GenericEntityLoader<ID, DO, E> el = this.createEntityLoader(cachedEntities, dataObjStream);
			Stream<E> entityStream = StreamSupport.stream(el, false);
					  entityStream.onClose(el);

			return entityStream;
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
	@Override
	@SuppressWarnings("unchecked")
	public Stream<E> get(Collection<ID> ids) throws IllegalArgumentException, DALException, DomainException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		return this.get(call(() -> ids.toArray((ID[]) Array.newInstance(this.getDomainObjectClass(), ids.size()))));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<E> getAll(boolean recentOnly) throws DALException, DomainException
	{
		E entity = null;
		Collection<E> cachedEntities = new ArrayList<>(this.loadedEntities.size());
		Set<ID> missingIds = new HashSet<>(this.loadedEntities.size());

		try
		{
			Set<Entry<ID, WeakReference<E>>> loadedEntries = new HashSet<>(this.loadedEntities.entrySet());

			for (Entry<ID, WeakReference<E>> entityEntry : loadedEntries)
			{
				ID entityId = entityEntry.getKey();
				WeakReference<E> entityRef = entityEntry.getValue();

				// virtual ids will not exist in the DAO, so if they have been unloaded, remove their entries from the cache
				if (this.isVirtualId(entityId))
				{
					if (entityRef == null)
					{
						this.loadedEntities.remove(entityId);

						continue;
					}

					entity = entityRef.get();

					if (entity == null)
					{
						this.loadedEntities.remove(entityId);

						continue;
					}
				}

				// the entity for an actual id has been unloaded. Add it to the set of ids for reloading from DAO
				if (entityRef == null)
				{
					missingIds.add(entityId);

					continue;
				}

				entity = entityRef.get();

				if (entity == null)
				{
					missingIds.add(entityId);

					continue;
				}

				cachedEntities.add(entity);
			}

			Stream<DO> dataObjStream = (recentOnly) ? this.getDataAccessObject().getRecent()
													: this.getDataAccessObject().getAll();
			GenericEntityLoader<ID, DO, E> el = this.createEntityLoader(cachedEntities, dataObjStream);
			Stream<E> entityStream = StreamSupport.stream(el, false);
					  entityStream.onClose(el);

			return entityStream;
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
	@Override
	public synchronized void add(E entity) throws DomainException
	{
		try
		{
			if (entity == null) throw new IllegalArgumentException("entity");
			if ((entity.getState() != State.Detached) && (entity.getState() != State.Added)) throw new IllegalStateException();
	
			if (!this.loadedEntities.containsKey(entity.getId()))
			{
				this.loadedEntities.put(entity.getId(), new WeakReference<E>(entity));
				
				entity.setState(State.Added);
				
				this.notifyObjectAdded(entity);
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
	@Override
	@SuppressWarnings("unchecked")
	public void add(Object object) throws DomainException
	{
		try
		{
			E entity = (E) object;
	
			this.add(entity);
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
	@Override
	public synchronized void remove(E entity) throws DomainException
	{
		try
		{
			if (entity == null) throw new IllegalArgumentException("entity");
	
			if (this.loadedEntities.containsKey(entity.getId()))
			{
				this.loadedEntities.remove(entity.getId());
				
				if (entity.getState() == State.Unchanged)	entity.setState(State.Deleted);
				if (entity.getState() == State.Modified)	entity.setState(State.Deleted);
				if (entity.getState() == State.Added) entity.setState(State.Detached);
				
				this.notifyObjectRemoved(entity);
			}
			else
			{
				// The object doesn't exist in the repo anyways, so detach it.
				entity.setState(State.Detached);
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
	@Override
	@SuppressWarnings("unchecked")
	public void remove(Object object) throws DomainException
	{
		try
		{
			E entity = (E) object;
			
			this.remove(entity);
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
	@Override
	public void setModified(E entity) throws DomainException
	{
		try
		{
			if (entity == null) throw new IllegalArgumentException("entity");
			if (entity.getState() != State.Unchanged) return;
			
			entity.setState(State.Modified);
			this.notifyObjectModified(entity);
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
	@SuppressWarnings("unchecked")
	public void setModified(Object object) throws DomainException
	{
		try
		{
			E entity = (E) object;
			
			this.setModified(entity);
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
	@Override
	public synchronized void reset() throws DomainException
	{
		try
		{
			synchronized (this.changeListener)
			{
				this.changeListener.clear();
				this.loadedEntities.clear();
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
}
