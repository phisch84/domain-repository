package com.schoste.ddd.domain.v2.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Generic interface to repositories.
 * 
 * Repositories manage access to entities ({@link Entity}) of an application, in particular
 * creating new entities via {@link GenericRepository#createEntity(Object...)}, which is supposed to work like a
 * factory method for each entity, and
 * looking these entities up and returning them via the get-methods of the repository, like {@link GenericRepository#get(Serializable)}.
 * 
 * An implementation of a repository must guarantee that calls to {@link GenericRepository#get(Collection)},
 * {@link GenericRepository#get(Serializable)}, {@link GenericRepository#get(Serializable[])} and
 * {@link GenericRepository#getAll(boolean)} will return the same instances of entities in subsequent calls
 * as long as a reference to an entity is held somewhere. Once the garbage collector has claimed an
 * entity, these methods will return a new instance.
 *
 * Repositories must not add, modify or delete data objects ({@link GenericDataObject}) of entities directly.
 * This is supposed to be done by the implementations of the Unit of Work ({@link UnitOfWork}) when
 * {@link UnitOfWork#commit()} is called.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the data type of the identifiers of the domain models and data objects
 * @param <E> the entity class
 * @param <DO> the data object class
 */
public interface GenericRepository<ID extends Serializable, E extends Entity<ID>, DO extends GenericDataObject<ID>>
{
	/**
	 * Creates a new entity ({@link Entity}), but does not add it to the repository.
	 * The new entity will have a new, unique virtual id and a detached state
	 * ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) detached}).
	 * 
	 * @param args optional arguments. Their actual meaning and use depend on the actual implementation
	 * @return the instance of a new entity
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E createEntity(Object... args) throws DomainException;

	/**
	 * Determines if a given id is virtual or an actual id.
	 * Virtual ids are ids created by the repository to uniquely identify unpersisted entities.
	 * Once an entity (to be precise: its underlying data object) has been persisted, the id of the entity
	 * is supposed to be overwritten by the id of its persisted data object - the actual id.
	 * 
	 * @param id the id to check
	 * @return true if the provided id is virtual, false if not.
	 * @throws IllegalArgumentException thrown if parameter id is null
	 */
	public boolean isVirtualId(ID id) throws IllegalArgumentException;

	/**
	 * Gets the entity with a given id. If no entity with the given id exists in the repository,
	 * or if the entity's id exists in the repository, but no actual entity has been loaded,
	 * then it will try to load it from the underlying DAO.
	 * If the DAO returns a data object which is flagged deleted, the repository will treat it
	 * as if it didn't exist / hadn't been returned by the DAO.
	 * 
	 * @param id the id of the entity to get
	 * @return the entity, or null if none was found or the entity's data object is flagged deleted
	 * 
	 * @throws IllegalArgumentException thrown if parameter id is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E get(ID id) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Tries to get all entities with an id contained in the provided ids.
	 * If no entity with a given id exists in the repository, it will try to load it from the underlying DAO.
	 * If no data object with a requested id exists in the DAO, then it will simply be omitted.
	 * If the data object is flagged deleted ({@link GenericDataObject#getIsDeleted()} is true),
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted}),
	 * remove the entity from its cache and not report it back in the stream.
	 *
	 * The returned stream holds resources of the DAO and is supposed to be closed after use (see {@link GenericEntityLoader}).
	 *
	 * @param ids the ids of the entities to get
	 * @return a stream which provides the found entities
	 *
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> get(ID[] ids) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Tries to get all entities with an id contained in the provided ids.
	 * If no entity with a given id exists in the repository, it will try to load it from the underlying DAO.
	 * If no data object with a requested id exists in the DAO, then it will simply be omitted.
	 * If the data object is flagged deleted ({@link GenericDataObject#getIsDeleted()} is true),
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted}),
	 * remove the entity from its cache and not report it back in the stream.
	 *
	 * The returned stream holds resources of the DAO and is supposed to be closed after use (see {@link GenericEntityLoader}).
	 *
	 * @param ids the ids of the entities to get
	 * @return a stream which provides the found entities
	 * 
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> get(Collection<ID> ids) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Gets entities which are either already present in the repository, or for which there exists
	 * a data object (see {@link GenericDataObject}) with a corresponding id
	 * ({@link GenericDataObject#getId()} equals {@link Entity#getId()}) and which isn't flagged deleted
	 * ({@link GenericDataObject#getIsDeleted()} is false).
	 * 
	 * The returned entities must include those entities whose ids exist in the repository's cache and which are already loaded.
	 * Since these entities are most likely referenced elsewhere, they must not be changed by a reload.
	 *
	 * For all entity ids in the repository's own cache for which no entity is loaded (anymore) and which haven't been reported by
	 * {@link GenericDataAccessObject#getRecent()} (parameter recentOnly is true),
	 * it is supposed to call {@link GenericDataAccessObject#get(Serializable[])} and update the entities with the data objects returned.
	 * Since the returned stream is consumed lazily, these ids are only known once the data objects of
	 * {@link GenericDataAccessObject#getRecent()} have been consumed; they are therefore requested at the end of the returned stream.
	 * If no data object was returned for a given id, or if the data object was flagged deleted,
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted}),
	 * remove the entity from its cache and not report it back in the stream.
	 * 
	 * In order to get potential new entities from the storage, or to update existing ones that are not in the cache (anymore,
	 * i.e. their id isn't registered in the repository's cache):
	 * <ul>
	 * <li>If parameter recentOnly is true, then the repository is supposed to call {@link GenericDataAccessObject#getRecent()};
	 * thus only new or modified data objects of entities are loaded from the DAO.</li>
	 * <li>If parameter recentOnly is false, then the repository is supposed to call {@link GenericDataAccessObject#getAll()};
	 * thus also data objects of entities that haven't been loaded yet (for whatever reason) are loaded.</li>
	 * </ul>
	 *
	 * Note that {@link GenericDataAccessObject#getRecent()} advances the DAO's most recent modification time stamp
	 * ({@link GenericDataAccessObject#getLastModifiedTimeStamp()}) once its stream was consumed to its end.
	 * If the DAO instance is shared with other repositories or services which also call {@link GenericDataAccessObject#getRecent()},
	 * then data objects reported to them won't be reported to this repository again (and vice versa).
	 *
	 * The returned stream holds resources of the DAO and is supposed to be closed after use (see {@link GenericEntityLoader}).
	 *
	 * @param recentOnly if true, only new or modified data objects are loaded from the DAO (see {@link GenericDataAccessObject#getRecent()}),
	 * if false, all data objects are loaded (see {@link GenericDataAccessObject#getAll()})
	 * @return a stream which provides the found entities
	 *
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> getAll(boolean recentOnly) throws DALException, DomainException;

	/**
	 * Adds an entity to the repository if its state is either Detached or Added (multiple calls with the same entity have no effect).
	 * The state of the entity is set to Added ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) added}),
	 * the entity is added to the repository's internal cache, and {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)}
	 * is called for the repository's listeners.
	 * If an entity with the same id already exists in the repository's cache, but is a different instance than the provided id, then an exception is thrown.
	 * If an entity with the same id already exists in the repository's cache, then this method does nothing - regardless of the state of the provided entity.
	 * If the entity doesn't exist in the repository's cache yet and the state of the provided entity is anything other than Detached or Added, an exception is thrown.
	 * The actual persistence of the entity is done by the Unit of Work ({@link UnitOfWork}) when
     * {@link UnitOfWork#commit()} is called.
	 * 
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null, or if the provided entity has the same id as a cached entity, but is a different instance
	 * @throws IllegalStateException thrown if the state of the entity is anything other than Detached or Added and the entity is unknown to the repository.
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void add(E entity) throws DomainException;
	
	/**
	 * Adds an entity to the repository if its state is either Detached or Added (multiple calls with the same entity have no effect).
	 * The state of the entity is set to Added ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) added}),
	 * the entity is added to the repository's internal cache, and {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)}
	 * is called for the repository's listeners.
	 * If an entity with the same id already exists in the repository's cache, but is a different instance than the provided id, then an exception is thrown.
	 * If an entity with the same id already exists in the repository's cache, then this method does nothing - regardless of the state of the provided entity.
	 * If the entity doesn't exist in the repository's cache yet and the state of the provided entity is anything other than Detached or Added, an exception is thrown.
	 * The actual persistence of the entity is done by the Unit of Work ({@link UnitOfWork}) when
     * {@link UnitOfWork#commit()} is called.
	 * 
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null, or if the provided entity has the same id as a cached entity, but is a different instance
	 * @throws IllegalStateException thrown if the state of the entity is anything other than Detached or Added and the entity is unknown to the repository.
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void add(Object entity) throws DomainException;
	
	/**
	 * Flags an entity which is known to the repository (its id exists in the cache) for removal from the repository:
	 * For entities with the state Unchanged or Modified, the state of the entity is set to Deleted
	 * ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted}).
	 * For entities with the state Added, the state of the entity is set to Detached.
	 * The entity is only flagged for removal if there is an entity with the same id in the repository's cache.
	 * If no entity with the same id exists, this method just sets the entity's state to Detached.
	 * The actual removal of the entity is done by the Unit of Work ({@link UnitOfWork}) when
     * {@link UnitOfWork#commit()} is called. After this call, the entity is evicted from the repository's cache.
	 * 
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void remove(E entity) throws DomainException;
	
	/**
	 * Flags an entity which is known to the repository (its id exists in the cache) for removal from the repository:
	 * For entities with the state Unchanged or Modified, the state of the entity is set to Deleted
	 * ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted}).
	 * For entities with the state Added, the state of the entity is set to Detached.
	 * The entity is only flagged for removal if there is an entity with the same id in the repository's cache.
	 * If no entity with the same id exists, this method just sets the entity's state to Detached.
	 * The actual removal of the entity is done by the Unit of Work ({@link UnitOfWork}) when
     * {@link UnitOfWork#commit()} is called. After this call, the entity is evicted from the repository's cache.
	 * 
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void remove(Object entity) throws DomainException;

	/**
	 * Marks an entity as modified so it will be persisted by the Unit of Work ({@link UnitOfWork}).
	 * If the entity's state is anything other than Unchanged, the state won't change.
	 * 
	 * @param entity the entity to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void setModified(E entity) throws DomainException;
	
	/**
	 * Marks an entity as modified so it will be persisted by the Unit of Work ({@link UnitOfWork}).
	 * If the entity's state is anything other than Unchanged, the state won't change.
	 * 
	 * @param entity the entity to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void setModified(Object entity) throws DomainException;

	/**
	 * Adds a listener which will be notified when the repository changes
	 *
	 * @param listener the listener to add
	 */
	public void addChangeListener(RepositoryChangeListener listener);

	/**
	 * Removes a listener which would otherwise be notified when the repository changes
	 * 
	 * @param listener the listener to remove
	 */
	public void removeChangeListener(RepositoryChangeListener listener);

	/**
	 * Resets the repository by removing all listeners and clearing all loaded objects.
	 *
	 * Entities keep their states, and changes which have already been registered with a Unit of Work ({@link UnitOfWork})
	 * are not discarded, since the Unit of Work is only removed as a listener.
	 * Call {@link UnitOfWork#commit()} or {@link UnitOfWork#rollback()} before resetting the repository.
	 * After the reset, the repository doesn't know any entity anymore, e.g. {@link GenericRepository#remove(Entity)}
	 * will only detach a previously loaded entity, but won't flag it for removal from the storage.
	 *
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void reset() throws DomainException;
}
