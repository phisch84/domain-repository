package com.schoste.ddd.domain.v3.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v3.entities.Entity;
import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

/**
 * Generic interface to repositories.
 * 
 * Repositories manage access to entities ({@link Entity}) of an application, in particular
 * creating new entities via {@link GenericRepository#createEntity()}, which is supposed to work like a
 * factory method for each entity, and
 * looking these entities up and returning them via the get-methods of the repository, like {@link GenericRepository#get(Serializable)}.
 * 
 * An implementation of a repository must guarantee that calls to {@link GenericRepository#get(Collection)},
 * {@link GenericRepository#get(Serializable)} and
 * {@link GenericRepository#getAll(boolean)} will return the same instances of entities in subsequent calls
 * as long as a reference to an entity is held somewhere. Once the garbage collector has claimed an
 * entity, these methods will return a new instance.
 *
 * Terms used in this documentation:
 * <ul>
 * <li><b>cache</b>: the repository keeps weak references to the entities it has loaded, or which have been added to it, by their ids.</li>
 * <li><b>known entity</b>: an entity is known to the repository if it is the very instance which is cached for its id.
 * An id whose entity has been claimed by the garbage collector doesn't make an entity known.</li>
 * <li><b>pending removal</b>: an entity which has been flagged for removal by {@link GenericRepository#remove(Entity)},
 * but whose removal hasn't been committed yet ({@link UnitOfWork#commit()}). The get-methods treat it as non-existent.</li>
 * </ul>
 *
 * The cache takes precedence over the DAO: for a known entity, the get-methods return the cached instance without querying the DAO
 * for it. A cached entity therefore reflects the storage at the time it was loaded; if others modify or delete its data object,
 * this isn't reflected as long as the entity is held somewhere. The repository never refreshes such an entity by itself:
 * refreshing is the caller's decision, either by {@link GenericRepository#refresh()} or {@link GenericRepository#refresh(Entity)},
 * which update the held instances, or by {@link UnitOfWork#rollback()} or {@link GenericRepository#reset()}, after which
 * the get-methods load new instances from the storage. The storage remains the gold source.
 *
 * An {@link IllegalArgumentException} or {@link IllegalStateException} documented by a method is thrown as it is, also by the
 * overloads which take an {@link Object}. Every other exception is re-thrown as {@link DomainException}, or as
 * {@link DAOException} where documented.
 *
 * Repositories must not add, modify or delete data objects ({@link DataObject}) of entities directly.
 * This is supposed to be done by the implementations of the Unit of Work ({@link UnitOfWork}) when
 * {@link UnitOfWork#commit()} is called.
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the data type of the identifiers of the domain models and data objects
 * @param <E> the entity class
 * @param <DO> the data object class
 */
public interface GenericRepository<ID extends Serializable, E extends Entity<ID>, DO extends DataObject<ID>>
{
	/**
	 * Creates a new entity ({@link Entity}), but does not add it to the repository.
	 * The new entity will have a new, unique virtual id and a detached state
	 * ({@link Entity#setState(com.schoste.ddd.domain.v3.entities.Entity.State) detached}).
	 * Repositories whose entities need data for their creation offer typed factory methods in addition
	 * (e.g. {@code createCustomer(String name)}).
	 *
	 * @return the instance of a new entity
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E createEntity() throws DomainException;

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
	 * Gets the entity with a given id.
	 * If the entity with the given id is known to the repository, the cached instance is returned without querying the DAO.
	 * If the entity's removal is pending, null is returned.
	 * Otherwise (the id isn't cached, or its entity has been claimed by the garbage collector), it will try to load the entity
	 * from the underlying DAO; a loaded entity has the state Unchanged and is cached.
	 * Virtual ids are never looked up in the DAO, since their entities haven't been persisted.
	 * If the DAO returns a data object which is flagged deleted, the repository will treat it
	 * as if it didn't exist / hadn't been returned by the DAO.
	 *
	 * @param id the id of the entity to get
	 * @return the entity, or null if none was found, the entity's data object is flagged deleted, or the entity's removal is pending
	 * 
	 * @throws IllegalArgumentException thrown if parameter id is null
	 * @throws DAOException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public E get(ID id) throws IllegalArgumentException, DAOException, DomainException;

	/**
	 * Tries to get all entities with an id contained in the provided ids.
	 * Each entity is provided once, even if its id is contained multiple times in the provided ids.
	 * For each id:
	 * <ul>
	 * <li>if its entity is known to the repository, the cached instance is provided without querying the DAO for it;</li>
	 * <li>if its entity's removal is pending, the entity is omitted;</li>
	 * <li>if it is a virtual id without a known entity, the entity is omitted (virtual ids are never looked up in the DAO);</li>
	 * <li>otherwise, the entity is loaded from the data object returned by the underlying DAO, gets the state Unchanged and is cached.
	 * If no data object with the id exists in the DAO, or if the data object is flagged deleted
	 * ({@link DataObject#isDeleted()} is true), the entity is omitted and the id is removed from the cache.</li>
	 * </ul>
	 * The DAO is only queried if at least one id is left to be loaded from it.
	 * The order in which the entities are provided is undefined.
	 *
	 * The returned stream holds resources of the DAO and is supposed to be closed after use.
	 * Failures which occur while the stream is consumed are thrown as unchecked exceptions (see {@link GenericEntityLoader}).
	 *
	 * @param ids the ids of the entities to get
	 * @return a stream which provides the found entities
	 *
	 * @throws IllegalArgumentException thrown if parameter ids is null or contains null
	 * @throws DAOException re-throws exceptions from the data access layer which occur before the stream is returned
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> get(Collection<ID> ids) throws IllegalArgumentException, DAOException, DomainException;

	/**
	 * Gets the entities of the repository: the entities known to the repository (including added entities which haven't been
	 * persisted yet), and the entities of data objects (see {@link DataObject}) which aren't flagged deleted
	 * ({@link DataObject#isDeleted()} is false). Each entity is provided once.
	 * Entities whose removal is pending are omitted.
	 *
	 * Known entities are provided as they are. Since they are most likely referenced elsewhere, they must not be changed by a reload,
	 * even if the DAO reports a newer data object for them (the cache takes precedence over the DAO).
	 * Data objects of ids without a known entity are converted into new entities, which get the state Unchanged and are cached.
	 *
	 * The data objects are loaded from the DAO as follows:
	 * <ul>
	 * <li>If parameter recentOnly is false, the repository calls {@link DAO#getAll()};
	 * thus all data objects of the storage are loaded.</li>
	 * <li>If parameter recentOnly is true, the repository keeps its own change-tracking time stamp: the highest modification
	 * time stamp of the data objects consumed from such a call so far (the beginning before the first call and after
	 * {@link GenericRepository#reset()}). It calls {@link DAO#getModifiedSince(long)} with this time stamp;
	 * thus only data objects which were added or modified at or after it are loaded.
	 * Additionally, the cached ids whose entities have been claimed by the garbage collector and which haven't been reported by
	 * {@link DAO#getModifiedSince(long)} are requested by {@link DAO#get(java.util.Collection)}.
	 * Since the returned stream is consumed lazily, these ids are only known once the reported data objects
	 * have been consumed; they are therefore requested at the end of the returned stream, and only if there are any.</li>
	 * </ul>
	 * If no data object is returned for a cached id whose entity has been claimed by the garbage collector, or if its data object is
	 * flagged deleted, the entity is omitted and the id is removed from the cache.
	 *
	 * Consequences if parameter recentOnly is true:
	 * <ul>
	 * <li>Data objects which haven't changed since the repository's time stamp and whose ids aren't cached are not provided.</li>
	 * <li>The time stamp advances while the returned stream is consumed: data objects which have been consumed aren't reported again
	 * by the next call, even if the stream wasn't consumed to its end (e.g. by {@link Stream#findFirst()}). Data objects with exactly
	 * the time stamp may be reported again (at least once).</li>
	 * <li>Each repository tracks the changes on its own, so repositories and services which share the DAO instance don't affect
	 * each other.</li>
	 * </ul>
	 * Use recentOnly = false for a complete result.
	 *
	 * The order in which the entities are provided is undefined.
	 * The returned stream holds resources of the DAO and is supposed to be closed after use.
	 * Failures which occur while the stream is consumed are thrown as unchecked exceptions (see {@link GenericEntityLoader}).
	 *
	 * @param recentOnly if true, only new or modified data objects are loaded from the DAO (see {@link DAO#getModifiedSince(long)}),
	 * if false, all data objects are loaded (see {@link DAO#getAll()})
	 * @return a stream which provides the found entities
	 *
	 * @throws DAOException re-throws exceptions from the data access layer which occur before the stream is returned
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public Stream<E> getAll(boolean recentOnly) throws DAOException, DomainException;

	/**
	 * Adds an entity to the repository. The actual persistence of the entity is done by the Unit of Work ({@link UnitOfWork}) when
	 * {@link UnitOfWork#commit()} is called.
	 * <ul>
	 * <li>If the entity is already known to the repository, this method does nothing - regardless of the entity's state.
	 * Thus, multiple calls with the same entity have no effect.</li>
	 * <li>If a different instance with the same id is known to the repository, an {@link IllegalArgumentException} is thrown.</li>
	 * <li>Otherwise (the id isn't cached, or its entity has been claimed by the garbage collector), the entity must have the state
	 * Detached or Added and a virtual id (see {@link GenericRepository#isVirtualId(Serializable)}), or an {@link IllegalStateException}
	 * is thrown. An entity with an actual id has been persisted before (e.g. its removal has already been committed);
	 * create a new entity instead. The state of the entity is set to Added
	 * ({@link Entity#setState(com.schoste.ddd.domain.v3.entities.Entity.State) added}), the entity is added to the repository's cache,
	 * and {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)} is called for the repository's listeners.</li>
	 * </ul>
	 *
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null, or if a different instance with the same id is known to the repository
	 * @throws IllegalStateException thrown if the entity is unknown to the repository and its state is anything other than Detached or Added, or its id isn't virtual
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void add(E entity) throws DomainException;
	
	/**
	 * Adds an entity to the repository. Behaves exactly like {@link GenericRepository#add(Entity)}.
	 *
	 * @param entity the entity to add
	 * @throws IllegalArgumentException thrown if the parameter entity is null or not an entity of this repository,
	 * or if a different instance with the same id is known to the repository
	 * @throws IllegalStateException thrown if the entity is unknown to the repository and its state is anything other than Detached or Added, or its id isn't virtual
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void add(Object entity) throws DomainException;
	
	/**
	 * Flags an entity for removal from the repository. The actual removal from the storage is done by the Unit of Work
	 * ({@link UnitOfWork}) when {@link UnitOfWork#commit()} is called.
	 * <ul>
	 * <li>If the entity's state is Deleted (its removal is already pending), this method does nothing.</li>
	 * <li>If the entity is known to the repository and its state is Unchanged or Modified, its state is set to Deleted
	 * ({@link Entity#setState(com.schoste.ddd.domain.v3.entities.Entity.State) deleted}) and its removal is pending:
	 * the entity is evicted from the cache, and the get-methods treat it as non-existent from now on.</li>
	 * <li>If the entity is known to the repository and its state is Added (it hasn't been persisted yet), its state is set to Detached
	 * and the entity is evicted from the cache. Nothing is removed from the storage.</li>
	 * <li>If a different instance with the same id is known to the repository, an {@link IllegalArgumentException} is thrown.</li>
	 * <li>If the entity is unknown to the repository, this method just sets the entity's state to Detached.
	 * Nothing is removed from the storage.</li>
	 * </ul>
	 * If the entity was known to the repository, {@link RepositoryChangeListener#onObjectRemoved(GenericRepository, Object)}
	 * is called for the repository's listeners.
	 *
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null, or if a different instance with the same id is known to the repository
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void remove(E entity) throws DomainException;
	
	/**
	 * Flags an entity for removal from the repository. Behaves exactly like {@link GenericRepository#remove(Entity)}.
	 *
	 * @param entity the entity to remove
	 * @throws IllegalArgumentException thrown if the parameter entity is null or not an entity of this repository,
	 * or if a different instance with the same id is known to the repository
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void remove(Object entity) throws DomainException;

	/**
	 * Marks an entity as modified so it will be persisted by the Unit of Work ({@link UnitOfWork}) when
	 * {@link UnitOfWork#commit()} is called.
	 * <ul>
	 * <li>If the entity is known to the repository and its state is Unchanged, its state is set to Modified
	 * and {@link RepositoryChangeListener#onObjectModified(GenericRepository, Object)} is called for the repository's listeners.</li>
	 * <li>If the entity's state is anything other than Unchanged, the state won't change and the listeners aren't notified.</li>
	 * <li>If the entity's state is Unchanged, but the entity is unknown to the repository, an {@link IllegalStateException} is thrown.</li>
	 * </ul>
	 *
	 * @param entity the entity to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter entity is null
	 * @throws IllegalStateException thrown if the entity's state is Unchanged, but the entity is unknown to the repository
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void setModified(E entity) throws DomainException;

	/**
	 * Marks an entity as modified. Behaves exactly like {@link GenericRepository#setModified(Entity)}.
	 *
	 * @param entity the entity to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter entity is null or not an entity of this repository
	 * @throws IllegalStateException thrown if the entity's state is Unchanged, but the entity is unknown to the repository
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void setModified(Object entity) throws DomainException;

	/**
	 * Refreshes the known entities from the storage: entities whose data objects have been modified in the storage after they
	 * were loaded (or last refreshed) get the values of the modified data objects. The instances stay the same, so references
	 * held elsewhere see the new values.
	 * <ul>
	 * <li>Known entities with the state Unchanged are refreshed: their values are updated in place and their state stays Unchanged.</li>
	 * <li>If the data object of a known Unchanged entity is flagged deleted ({@link DataObject#isDeleted()} is true),
	 * the entity is forgotten: it is evicted from the cache and its state is set to Detached.</li>
	 * <li>Modified entities are skipped: their local changes take precedence until they are committed ({@link UnitOfWork#commit()})
	 * or rolled back ({@link UnitOfWork#rollback()}).</li>
	 * <li>Added entities haven't been persisted yet, and entities whose removal is pending are treated as non-existent;
	 * neither is refreshed.</li>
	 * <li>Data objects without a known entity are ignored; the get-methods load them on demand.</li>
	 * </ul>
	 *
	 * The modified data objects are requested by {@link DAO#getModifiedSince(long, DeletedDataObjects)}, including the data
	 * objects flagged deleted, with the oldest modification time stamp of the data objects the known Unchanged entities were loaded
	 * or last refreshed from. An entity is only refreshed if its data object is newer than the one it was loaded or last refreshed from.
	 * All time stamps come from the storage, so the result doesn't depend on the clock of the repository's host.
	 * The change-tracking time stamp of {@link GenericRepository#getAll(boolean)} with recentOnly = true isn't changed.
	 *
	 * Data objects which are removed from the storage physically, instead of being flagged deleted, can't be reported by
	 * {@link DAO#getModifiedSince(long, DeletedDataObjects)}; their entities aren't forgotten by this method.
	 * Use {@link GenericRepository#refresh(Entity)} for them.
	 * If there is no known Unchanged entity, the DAO isn't queried.
	 *
	 * This method is meant for fire-and-forget calls: entities which can't be refreshed (e.g. Modified ones) are skipped silently.
	 * Callers which need to pay attention to the state of each entity use {@link GenericRepository#refresh(Entity)} instead
	 * (e.g. in a loop), which throws if an entity can't be refreshed.
	 *
	 * After the refresh, {@link RepositoryChangeListener#onRefreshed(GenericRepository, Collection, Collection)} is called for the
	 * repository's listeners, even if nothing was refreshed.
	 *
	 * If the refresh fails part-way, the entities refreshed so far stay refreshed. If an entity's data object can't be converted,
	 * the entity may have been updated partially, so it is forgotten (state Detached). The exception is thrown, and the listeners
	 * aren't notified. Since the time stamps of the entities which haven't been refreshed are unchanged, a later call of this method
	 * picks them up.
	 *
	 * @return the affected entities: the refreshed ones, which have the state Unchanged, and the forgotten ones, which have the
	 * state Detached. The collection is empty if nothing was refreshed; callers which don't need it can ignore it.
	 * @throws DAOException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public Collection<E> refresh() throws DAOException, DomainException;

	/**
	 * Refreshes a known entity from the storage, regardless of time stamps: its data object is loaded by
	 * {@link DAO#get(Serializable)}, and the entity gets its values in place (the instance stays the same)
	 * and keeps the state Unchanged.
	 * If no data object exists for the entity's id anymore, or if it is flagged deleted ({@link DataObject#isDeleted()} is true),
	 * the entity is forgotten: it is evicted from the cache and its state is set to Detached.
	 * The caller tells the outcome by the entity's state afterwards: Unchanged if it was refreshed, Detached if it was forgotten.
	 *
	 * Unlike {@link GenericRepository#refresh()}, this method is meant for callers which pay attention to the state of each entity:
	 * it throws instead of skipping an entity which can't be refreshed.
	 *
	 * After the refresh, {@link RepositoryChangeListener#onRefreshed(GenericRepository, Collection, Collection)} is called for the
	 * repository's listeners.
	 *
	 * If the data object can't be converted, the entity may have been updated partially, so it is forgotten (state Detached),
	 * a {@link DomainException} is thrown, and the listeners aren't notified.
	 *
	 * @param entity the entity to refresh
	 * @throws IllegalArgumentException thrown if the parameter entity is null, or if a different instance with the same id is known to the repository
	 * @throws IllegalStateException thrown if the entity is unknown to the repository, or if its state isn't Unchanged
	 * (local changes would be lost; commit or roll them back first)
	 * @throws DAOException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every other exception as {@link DomainException}
	 */
	public void refresh(E entity) throws IllegalArgumentException, IllegalStateException, DAOException, DomainException;

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
	 * Resets the repository by removing all listeners and forgetting all entities.
	 *
	 * Every known entity is evicted from the cache and its state is set to Detached; pending removals are forgotten as well.
	 * This is the same as {@link UnitOfWork#rollback()} does for the entities registered with a Unit of Work, applied to all
	 * entities of the repository. After the reset, the repository doesn't know any entity anymore: the get-methods load
	 * entities from the DAO again and provide new instances.
	 * The change tracking of {@link GenericRepository#getAll(boolean)} with recentOnly = true starts at the beginning again:
	 * its next call provides all data objects which aren't flagged deleted.
	 *
	 * Units of Work ({@link UnitOfWork}) are only removed as listeners; changes which have already been registered with them
	 * are not discarded. Since the affected entities are Detached now, committing these changes afterwards leads to undefined results.
	 * Call {@link UnitOfWork#commit()} or {@link UnitOfWork#rollback()} before resetting the repository.
	 *
	 * @throws DomainException re-throws every exception as {@link DomainException}
	 */
	public void reset() throws DomainException;
}
