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
 * Repositories manage access to entities ({@link Entity}) of an application. In particular
 * creating new entities via {@link GenericRepository#createEntity(Object...)} - which is supposed to work like a
 * factory method for each entity, and
 * get-methods of the repository, like {@link GenericRepository#get(Serializable)} to look these entities up and
 * return them.
 * 
 * An implementation of a repository must guarantee that calls to {@link GenericRepository#get(Collection)},
 * {@link GenericRepository#get(Serializable)}, {@link GenericRepository#get(Serializable[])} and
 * {@link GenericRepository#getAll(boolean)} will return the same instances of entities in subsequent calls
 * as long as the reference to an entity is held somewhere. Once the garbage collector claims the reference to an
 * entity, will return a new reference.
 * 
 * Repositories must not add, modify or delete data objects ({@link GenericDataObject}) of entities directly.
 * This is supposed to be done by the implementations of the Unit of Work ({@link UnitOfWork}) when
 * ({@link UnitOfWork#commit()}) is called. 
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
	 * Creates a new entity (entity - {@link Entity}) but does not add it to the repository.
	 * The new entity will have a new, unique virtual id and a detached state
	 * ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) detached).
	 * 
	 * @param args optional arguments. Their actual meaning and use depends on the actual implementation
	 * @return the instance to a new entity
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E createEntity(Object... args) throws DomainException;

	/**
	 * Determines if a given id is virtual or an actual id.
	 * Virtual ids are ids created by the repository to unqiuely identify unpersisted entities.
	 * Once an entity (to be precise: its underlying data object) has been persisted, the id of the entity
	 * is supposed to be overwritten by the id from its persited data object - the actual id.
	 * 
	 * @param id the id to check
	 * @return true if the provided id is virtual, false if not.
	 * @throws IllegalArgumentException thrown if parameter id is null
	 */
	public boolean isVirtualId(ID id) throws IllegalArgumentException;

	/**
	 * Gets an entity with a given id. If no entity with the given id exists in the repository,
	 * or if the entity's id exists in the repository, but no actual entity has been loaded,
	 * then it will try to load it from the underlying DAO.
	 * 
	 * @param id the id of the entity to get
	 * @return the entity or null if none was found
	 * 
	 * @throws IllegalArgumentException thrown if parameter id is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E get(ID id) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Tries to get all entities with an id contained in the provided ids.
	 * If no entity with the given id exists in the repository it will try to load it from the underlying DAO.
	 * If no data object with the requested id exists in the DAO, then it will be simply omitted.
	 * If the data object is flagged deleted ({@link GenericDataObject#getIsDeleted()} is true)
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted),
	 * remove the entity from its cache and not report it back in the stream.
	 * 
	 * @param ids the ids of the entities to get
	 * @return a stream which provides found entities
	 * 
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> get(ID[] ids) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Tries to get all entities with an id contained in the provided ids.
	 * If no entity with the given id exists in the repository it will try to load it from the underlying DAO.
	 * If no data object with the requested id exists in the DAO, then it will be simply omitted.
	 * If the data object is flagged deleted ({@link GenericDataObject#getIsDeleted()} is true)
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted),
	 * remove the entity from its cache and not report it back in the stream.
	 * 
	 * @param ids the ids of the entities to get
	 * @return a stream which provides found entities
	 * 
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> get(Collection<ID> ids) throws IllegalArgumentException, DALException, DomainException;

	/**
	 * Gets all entities which are either already present in the repository, or for which there exists
	 * a data object (see {@link GenericDataObject}) with a corresponding id
	 * ({@link GenericDataObject#getId()} equals {@link Entity#getId()}) which isn't flagged deleted
	 * ({@link GenericDataObject#getIsDeleted()} is false).
	 * 
	 * The returned entities must contain those entities for which ids exist in its cache and where an entity is already loaded.
	 * Since these entities are most likely held outside, they must not be changed by a reload.
	 * 
	 * For all entity ids in the repository's own cache, where no entity isn't loaded (anymore) that haven't been reported by {@link GenericDataAccessObject#getRecent()},
	 * it is supposed to call {@link GenericDataAccessObject#get(Serializable[])} and update the entities with the
	 * data objects returned. If no data object was returned for a given id, or if the data object was flagged deleted,
	 * the repository is supposed to set the entity deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted),
	 * remove the entity from its cache and not report it back in the stream.
	 * 
	 * In order to get potential new entities from the storage, or to update existing ones that do not exist (their id isn't registered in the repository's cache)
	 * in the cache (anymore):
	 * <ul>
	 * <li>If parameter recentOnly is true, then the repository is supposed to call {@link GenericDataAccessObject#getRecent()};
	 * thus only new or modified data objects of entities are loaded from the DAO.</li>
	 * <li>If parameter recentOnly is false, then the repository is supposed to call {@link GenericDataAccessObject#getAll()};
	 * thus also data objects of entities that haven't been loaded yet (for whatever reason) are loaded.</li>
	 * </ul>
	 * 
	 * @return a stream which provides found entities
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> getAll(boolean recentOnly) throws DALException, DomainException;

	/**
	 * Adds an entity to the repository if its state is either Detached or Added (multiple calls with the same entity have no effect).
	 * The state of the entity is set to Added ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) added),
	 * and the entity is added to the repository's internal cache and for its listeners {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)}
	 * is called.
	 * If an entity with the same id already exists in the repository's cache, then this method does nothing - regardless of the state of the provided entity.
	 * If the entity doesn't exist in the repositories cache yet and state of the provided entity is anything else than Detached or Added an exception is thrown.
	 * The actual persistence of the entity id done by the Unit of Work ({@link UnitOfWork}) when
     * ({@link UnitOfWork#commit()}) is called. 
	 * 
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws IllegalStateException thrown if the state of the entity is anything else than Detached or Added and the entity is unknown to the repository.
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void add(E entity) throws DomainException;
	
	/**
	 * Adds an entity to the repository if its state is either Detached or Added (multiple calls with the same entity have no effect).
	 * The state of the entity is set to Added ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) added),
	 * and the entity is added to the repository's internal cache and for its listeners {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)}
	 * is called.
	 * If an entity with the same id already exists in the repository's cache, then this method does nothing - regardless of the state of the provided entity.
	 * If the entity doesn't exist in the repositories cache yet and state of the provided entity is anything else than Detached or Added an exception is thrown.
	 * The actual persistence of the entity id done by the Unit of Work ({@link UnitOfWork}) when
     * ({@link UnitOfWork#commit()}) is called. 
	 * 
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws IllegalStateException thrown if the state of the entity is anything else than Detached or Added and the entity is unknown to the repository.
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void add(Object entity) throws DomainException;
	
	/**
	 * Flags an entity for removal from the repository:
	 * The state of the entity is set deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted).
	 * The entity is only flagged for removal if there is an entity with the same id in the repositories cache.
	 * If no entity with the same id already exists, this method just sets the entity's state to detached.
	 * The actual removal of the entity id done by the Unit of Work ({@link UnitOfWork}) when
     * ({@link UnitOfWork#commit()}) is called. After this call, the entity is evicted from the repositories cache.
	 * 
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void remove(E entity) throws DomainException;
	
	/**
	 * Flags an entity for removal from the repository:
	 * The state of the entity is set deleted ({@link Entity#setState(com.schoste.ddd.domain.v2.entities.Entity.State) deleted).
	 * The entity is only flagged for removal if there is an entity with the same id in the repositories cache.
	 * If no entity with the same id already exists, this method just sets the entity's state to detached.
	 * The actual removal of the entity id done by the Unit of Work ({@link UnitOfWork}) when
     * ({@link UnitOfWork#commit()}) is called. After this call, the entity is evicted from the repositories cache.
	 * 
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void remove(Object entity) throws DomainException;

	/**
	 * Marks a entity as modified so it will be persisted by the Unit of Work ({@link UnitOfWork}).
	 * Entities marked as modified will not be overwritten when the repository reloads data objects from the DAO.
	 * If the models state is anything else but Unchanged the state won't change.
	 * 
	 * @param entity the entity to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void setModified(E entity) throws DomainException;
	
	/**
	 * Marks a entity as modified so it will be persisted by the Unit of Work ({@link UnitOfWork}).
	 * Entities marked as modified will not be overwritten when the repository reloads data objects from the DAO.
	 * If the models state is anything else but Unchanged the state won't change.
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
	 * Removes a listener which would be notified when the repository changes
	 * 
	 * @param listener the listener to remove
	 */
	public void removeChangeListener(RepositoryChangeListener listener);

	/**
	 * Resets the repository by removing all listeners and clearing all loaded objects.
	 * 
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void reset() throws DomainException;
}
