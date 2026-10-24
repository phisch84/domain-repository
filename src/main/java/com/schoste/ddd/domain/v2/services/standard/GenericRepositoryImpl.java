package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.entities.Entity.State;
import com.schoste.ddd.domain.v2.exceptions.DataObjectConversionException;
import com.schoste.ddd.domain.v2.exceptions.DataObjectNullException;
import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.domain.v2.services.AutoObjectConverter;
import com.schoste.ddd.domain.v2.services.GenericRepository;
import com.schoste.ddd.domain.v2.services.GenericEntityLoader;
import com.schoste.ddd.domain.v2.services.RepositoryChangeListener;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
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
	protected Map<ID, WeakReference<E>> pendingRemovals = new HashMap<ID, WeakReference<E>>();

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
	 * as not (yet) persisted.
	 *
	 * The returned identifier must be recognized as virtual by both {@link GenericRepositoryImpl#isVirtualId(Serializable)}
	 * and {@link GenericDataAccessObject#isVirtualId(Serializable)} of the repository's DAO:
	 * {@link GenericRepositoryImpl#domainObjectToDataObject(Entity)} sets the entity's virtual id on a new data object,
	 * and the DAO is supposed to treat it as a new data object when it is saved.
	 *
	 * @return a new, unique virtual identifier
	 */
	abstract protected ID getNextVirtualId();

	/**
	 * Returns an instance of the implementation of an {@link AutoObjectConverter} which will be used in
	 * data-object-to-entity conversion methods (and vice versa)
	 *
	 * @return an instance of the implementation of an {@link AutoObjectConverter}
	 */
	abstract protected AutoObjectConverter getAutoObjectConverter();

	/**
	 * The method which creates the actual instance of a new entity. This instance will then be used by
	 * {@link GenericRepositoryImpl#createEntity(Object...)}.
	 * To be implemented by the deriving class.
	 * 
	 * @param entityClass the class type of the entity to create
	 * @param args optional arguments. Their usage depends on the implementation of the method.
	 * @return a new instance of the repository's entity.
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
	 * Called by {@link GenericRepositoryImpl#dataObjectToDomainObject(GenericDataObject, Entity)}
	 * after the entity was enriched by the auto converter
	 * 
	 * @param dataObject the data object with source information
	 * @param entity the entity to enrich with information
	 * @throws Exception re-throws every exception
	 */
	abstract protected void afterAutoConversation(DO dataObject, E entity) throws Exception;

	/**
	 * Called by {@link GenericRepositoryImpl#domainObjectToDataObject(Entity)}
	 * after the data object was enriched by the auto converter
	 *
	 * @param entity the entity with source information
	 * @param dataObject the data object to enrich with information
	 * @throws Exception re-throws every exception
	 */
	abstract protected void afterAutoConversation(E entity, DO dataObject) throws Exception;

	/**
	 * Creates an entity loader ({@link GenericEntityLoader}) from implementation-specific arguments.
	 * It is not used by this class; the get-methods use {@link GenericRepositoryImpl#createEntityLoader(Collection, Stream, Function)}.
	 *
	 * To be implemented by the extending class.
	 *
	 * @param args optional arguments for the creation of the entity loader. Their usage depends on the actual implementation
	 * @return a new entity loader
	 * @throws Exception re-throws every exception
	 */
	abstract protected GenericEntityLoader<ID, DO, E> createEntityLoader(Object... args) throws Exception;

	/**
	 * Creates the entity loader ({@link GenericEntityLoader}) which provides the entities of the streams returned by
	 * {@link GenericRepositoryImpl#get(Serializable[])}, {@link GenericRepositoryImpl#get(Collection)} and
	 * {@link GenericRepositoryImpl#getAll(boolean)}.
	 * The returned loader must fulfill the contract of {@link GenericEntityLoader} and convert data objects with the given
	 * conversion function, which caches the entities it creates and throws unchecked exceptions only.
	 * The data object stream doesn't contain data objects of pending removals.
	 *
	 * To be implemented by the extending class.
	 *
	 * @param cachedEntities the entities which are already loaded and cached by the repository. They are provided as they are and take precedence over data objects with the same id.
	 * @param dataObjStream the stream of data objects from the repository's DAO, which are converted into entities
	 * @param dataObjToEntityFunction the function which converts a data object into an entity and caches it (see {@link GenericRepositoryImpl#toCachedEntity(GenericDataObject)})
	 * @return a new entity loader
	 * @throws Exception re-throws every exception
	 */
	abstract protected GenericEntityLoader<ID, DO, E> createEntityLoader(Collection<E> cachedEntities, Stream<DO> dataObjStream, Function<DO, E> dataObjToEntityFunction) throws Exception;

	/**
	 * Calls an operation and returns its result.
	 * A {@link DALException} or a {@link DomainException} thrown by the operation is re-thrown as is;
	 * every other exception is wrapped in a {@link DomainException}.
	 *
	 * @param <T> the class of the operation's result
	 * @param operation the operation to call
	 * @return the result of the operation
	 * @throws DALException re-thrown if the operation throws a {@link DALException}
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
	 * Checks if an entity, passed in as an {@link Object}, can be considered persisted by checking if its
	 * id is virtual (see {@link GenericRepositoryImpl#isVirtualId(Serializable)}).
	 *
	 * @param obj the entity passed in as {@link Object}
	 * @return false if its id is virtual, true otherwise
	 * @throws IllegalArgumentException thrown if parameter obj is either null or not an {@link Entity}
	 * @throws ClassCastException thrown if obj is an {@link Entity} whose id isn't of the repository's id type
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected boolean isPersisted(Object obj) throws IllegalArgumentException, Exception
	{
		if (obj == null) throw new IllegalArgumentException("obj");
		if (!(obj instanceof Entity<?>)) throw new IllegalArgumentException("obj");

		Entity<?> entity = (Entity<?>) obj;

		return !this.isVirtualId((ID)entity.getId());
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
	 * Creates a new entity from a data object.
	 * The returned entity is supposed to have the state Unchanged on return and must be cached by this repository
	 * so that the contracts of the repository's get() methods still hold.
	 *
	 * @param dataObject the data object as source
	 * @param entity an existing entity to update with the provided data object. If null, a new one will be created
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
	 * @param dataObject the data object as source
	 * @param entity an existing entity to update with the provided data object. If null, a new one will be created
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
	 * When overriding this method, <b>make sure to call the method of the super class.</b>
	 * When calling the method of the super class, the repository will check whether
	 * a data object with the given id already exists that needs to be updated.
	 *
	 * @param entity the entity to create the data object from
	 * @return a data object or null if no data object exists yet for the given entity's id
	 * @throws IllegalArgumentException thrown when any of the parameters is null
	 * @throws DataObjectNullException thrown if no data object could be created
	 * @throws Exception re-throws every exception
	 */
	protected DO domainObjectToDataObject(E entity) throws Exception
	{
		if (entity == null) throw new IllegalArgumentException("entity");

		DO dataObject = this.isVirtualId(entity.getId()) ? this.getDataAccessObject().createDataObject()
														 : this.getDataAccessObject().get(entity.getId());

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
	 * Checks if the removal of the entity with a given id is pending, i.e. the entity has been flagged for removal by
	 * {@link GenericRepositoryImpl#remove(Entity)}, but the removal hasn't been committed yet.
	 * A removal is no longer pending once its entity has left the state Deleted (e.g. the Unit of Work has committed it and
	 * set the entity Detached); its entry is removed then.
	 *
	 * @param id the id of the entity
	 * @return true if the removal of the entity is pending, false otherwise
	 */
	protected synchronized boolean isPendingRemoval(ID id)
	{
		WeakReference<E> entityRef = this.pendingRemovals.get(id);

		if (entityRef == null) return false;

		E entity = entityRef.get();

		if ((entity != null) && (entity.getState() == State.Deleted)) return true;

		this.pendingRemovals.remove(id);

		return false;
	}

	/**
	 * Tries to get an entity from the cache.
	 * If it isn't found in the cache and the id is an actual id, then it is reloaded from the DAL.
	 * If it isn't found in the cache and the id is a virtual id, then null is returned.
	 * 
	 * @param id the id of the entity to get/load
	 * @return the entity with the given id, or null if it does not exist
	 * @throws Exception re-throws every exception
	 */
	protected synchronized E loadDomainObject(ID id) throws Exception
	{
		if (this.isPendingRemoval(id)) return null;

		// The object was loaded once and is still referenced somewhere; get it from cache
		E entity = this.getCachedEntity(id);

		if (entity != null) return entity;

		// virtual ids will not exist in the DAO's storage -> return null
		if (this.isVirtualId(id)) return null;

		// The object was not loaded yet or was removed from cache; reload it
		DO dataObject = this.getDataAccessObject().get(id);

		if (dataObject == null) return null; // not found in DAO
		if (dataObject.getIsDeleted()) return null; // found in DAO but flagged deleted

		// An object with the given id exists in the DAL; load it
		return this.createCachedEntity(dataObject);
	}

	/**
	 * Gets the entity cached for an id
	 *
	 * @param id the id of the entity
	 * @return the cached entity, or null if the id isn't cached or its entity has been claimed by the garbage collector
	 */
	protected synchronized E getCachedEntity(ID id)
	{
		WeakReference<E> entityRef = this.loadedEntities.get(id);

		return (entityRef == null) ? null : entityRef.get();
	}

	/**
	 * Creates a new entity with the state Unchanged from a data object and caches it
	 *
	 * @param dataObject the data object to create the entity from
	 * @return the new entity
	 * @throws Exception re-throws every exception
	 */
	protected synchronized E createCachedEntity(DO dataObject) throws Exception
	{
		E entity = this.dataObjectToDomainObject(dataObject, this.createEntity());

		this.loadedEntities.put(entity.getId(), new WeakReference<E>(entity));

		return entity;
	}

	/**
	 * Converts a data object into an entity and caches it. This is the conversion function of the entity loaders
	 * (see {@link GenericRepositoryImpl#createEntityLoader(Collection, Stream, Function)}).
	 * If an entity has been cached for the data object's id in the meantime, the cached entity is returned, since the cache
	 * takes precedence over the DAO.
	 *
	 * @param dataObject the data object to convert
	 * @return the entity, never null
	 * @throws DataObjectConversionException thrown if the data object couldn't be converted
	 */
	protected synchronized E toCachedEntity(DO dataObject) throws DataObjectConversionException
	{
		try
		{
			E entity = this.getCachedEntity(dataObject.getId());

			return (entity != null) ? entity : this.createCachedEntity(dataObject);
		}
		catch (DataObjectConversionException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DataObjectConversionException(dataObject.getId(), e);
		}
	}

	/**
	 * Creates the stream which is returned by the get-methods which return streams.
	 * The data objects of pending removals are removed from the data object stream before it is passed to the entity loader.
	 *
	 * @param cachedEntities the cached entities to provide
	 * @param dataObjStream the stream of data objects to convert into entities. It is closed when the returned stream is closed.
	 * @return the stream of entities
	 * @throws Exception re-throws every exception
	 */
	protected Stream<E> streamEntities(Collection<E> cachedEntities, Stream<DO> dataObjStream) throws Exception
	{
		try
		{
			Stream<DO> filteredStream = dataObjStream.filter(dataObj -> (dataObj == null) || !this.isPendingRemoval(dataObj.getId()));
			GenericEntityLoader<ID, DO, E> el = this.createEntityLoader(cachedEntities, filteredStream, this::toCachedEntity);

			return StreamSupport.stream(el, false).onClose(el);
		}
		catch (Exception e)
		{
			dataObjStream.close();

			throw e;
		}
	}

	/**
	 * Gets the data objects reported by {@link GenericDataAccessObject#getRecent()}, followed by the data objects of the
	 * given ids which haven't been reported by {@link GenericDataAccessObject#getRecent()}.
	 * The latter are requested by {@link GenericDataAccessObject#get(Collection)} once the data objects of
	 * {@link GenericDataAccessObject#getRecent()} have been consumed, and only if there are any.
	 *
	 * @param missingIds the ids to request if they aren't reported by {@link GenericDataAccessObject#getRecent()}
	 * @return the stream of data objects
	 * @throws DALException re-throws exceptions from the data access layer
	 */
	protected Stream<DO> streamRecentDataObjects(Set<ID> missingIds) throws DALException
	{
		Set<ID> reportedIds = new HashSet<>();
		Stream<DO> recentStream = this.getDataAccessObject().getRecent().peek(dataObj -> { if (dataObj != null) reportedIds.add(dataObj.getId()); });

		if (missingIds.isEmpty()) return recentStream;

		Stream<DO> missingStream = Stream.of(missingIds).flatMap(ids ->
		{
			Set<ID> unreportedIds = new HashSet<>(ids);

			unreportedIds.removeAll(reportedIds);

			if (unreportedIds.isEmpty()) return Stream.<DO>empty();

			try
			{
				return this.getDataAccessObject().get(unreportedIds);
			}
			catch (DALException e)
			{
				throw new DALRuntimeException(e);
			}
		});

		return Stream.concat(recentStream, missingStream);
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
	public E get(ID id) throws IllegalArgumentException, DALException, DomainException
	{
		if (id == null) throw new IllegalArgumentException("id");

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

		return this.get(Arrays.asList(ids));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<E> get(Collection<ID> ids) throws IllegalArgumentException, DALException, DomainException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		Collection<E> cachedEntities = new ArrayList<>(ids.size());
		Set<ID> missingIds = new HashSet<>(ids.size());

		try
		{
			synchronized (this)
			{
				// each id is regarded once, so each entity is provided once
				for (ID id : new LinkedHashSet<>(ids))
				{
					if (id == null) continue;
					if (this.isPendingRemoval(id)) continue;

					E entity = this.getCachedEntity(id);

					// the entity exists in the cache, so somewhere its reference is held -> do not reload / overwrite it
					if (entity != null)
					{
						cachedEntities.add(entity);

						continue;
					}

					// remove the ids with dead references. If it was a virtual id, the entity is abandoned.
					// If it was an actual id, the entity will be reloaded from the DAO and added back to the cache if it still exists
					this.loadedEntities.remove(id);

					// only add actual ids for a reload from the DAO
					if (!this.isVirtualId(id)) missingIds.add(id);
				}
			}

			// the DAO is only queried if at least one id is left to be loaded from it
			Stream<DO> dataObjStream = missingIds.isEmpty() ? Stream.<DO>empty() : this.getDataAccessObject().get(missingIds);

			return this.streamEntities(cachedEntities, dataObjStream);
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
	public Stream<E> getAll(boolean recentOnly) throws DALException, DomainException
	{
		Collection<E> cachedEntities = new ArrayList<>();
		Set<ID> missingIds = new HashSet<>();

		try
		{
			synchronized (this)
			{
				for (Entry<ID, WeakReference<E>> entityEntry : new ArrayList<>(this.loadedEntities.entrySet()))
				{
					ID entityId = entityEntry.getKey();
					E entity = (entityEntry.getValue() == null) ? null : entityEntry.getValue().get();

					if (entity != null)
					{
						cachedEntities.add(entity);

						continue;
					}

					// remove the ids with dead references. Data objects which still exist are cached again when they are converted
					this.loadedEntities.remove(entityId);

					// virtual ids will not exist in the DAO; actual ids are requested if getRecent() doesn't report them
					if (!this.isVirtualId(entityId)) missingIds.add(entityId);
				}
			}

			Stream<DO> dataObjStream = (recentOnly) ? this.streamRecentDataObjects(missingIds)
													: this.getDataAccessObject().getAll();

			return this.streamEntities(cachedEntities, dataObjStream);
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
	public synchronized void add(E entity) throws IllegalArgumentException, IllegalStateException, DomainException
	{
		if (entity == null) throw new IllegalArgumentException("entity");

		try
		{
			if (this.loadedEntities.containsKey(entity.getId()))
			{
				WeakReference<E> cachedEntityRef = this.loadedEntities.get(entity.getId());
				E cachedEntity = (cachedEntityRef == null) ? (E) null : cachedEntityRef.get();

				if (cachedEntity == null)
				{
					// the repo knows the id, but the entity was unloaded -> add it back if states are okay
				}
				else
				{
					if (entity == cachedEntity) return; // the entity's id is already known; do nothing.
					else throw new IllegalArgumentException("entity");
				}				
			}

			if ((entity.getState() != State.Detached) && (entity.getState() != State.Added)) throw new IllegalStateException();
	
			this.loadedEntities.put(entity.getId(), new WeakReference<E>(entity));
			
			entity.setState(State.Added);
			
			this.notifyObjectAdded(entity);
		}
		catch (IllegalArgumentException e)
		{
			throw e;
		}
		catch (IllegalStateException e)
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
				
				if (entity.getState() == State.Unchanged) entity.setState(State.Deleted);
				if (entity.getState() == State.Modified) entity.setState(State.Deleted);
				if (entity.getState() == State.Added) entity.setState(State.Detached);

				if (entity.getState() == State.Deleted) this.pendingRemovals.put(entity.getId(), new WeakReference<E>(entity));

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
				this.pendingRemovals.clear();
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}
}
