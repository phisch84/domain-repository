package com.schoste.ddd.domain.v2.services;

import java.io.Serializable;
import java.util.Spliterator;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to the implementation of an entity loader which is used in {@link GenericRepository#get(Serializable[])},
 * {@link GenericRepository#get(java.util.Collection)} and {@link GenericRepository#getAll(boolean)}.
 *
 * An entity loader is the {@link Spliterator} behind the stream returned by these methods. It combines two sources:
 * <ol>
 * <li>the entities which are already loaded and cached by the repository (the cached entities), and</li>
 * <li>a stream of data objects from the repository's DAO, which are converted into entities while the stream is consumed.</li>
 * </ol>
 *
 * An entity loader is supposed to guarantee that:
 * <ul>
 * <li>the cached entities are provided as they are (the same instances, neither updated nor replaced);</li>
 * <li>no entity is provided twice, i.e. no new entity is created for a data object whose id belongs to a cached entity;</li>
 * <li>no entity is provided for a data object which is flagged deleted ({@link GenericDataObject#getIsDeleted()} is true);</li>
 * <li>no entity is provided for an id whose removal is pending (see {@link GenericRepository}); the repository removes
 * the data objects of these ids from the data object stream before it passes the stream to the loader;</li>
 * <li>entities created from data objects are cached by the repository, so that subsequent calls of the repository's
 * get-methods return the same instances (see {@link GenericRepository});</li>
 * <li>no null elements are provided.</li>
 * </ul>
 * The order in which the entities are provided is undefined. Regardless of the order, the cache takes precedence over the DAO:
 * for an id with a cached entity, the cached entity is provided - even if the DAO reports a newer data object for it.
 * Only data objects of ids without a cached entity are converted into entities.
 *
 * Data objects are loaded lazily: the DAO is only queried as far as the stream is consumed.
 * Failures while the stream is consumed happen after the repository's method has returned, and
 * {@link Spliterator#tryAdvance(java.util.function.Consumer)} can't throw checked exceptions. Therefore they are thrown as unchecked exceptions:
 * <ul>
 * <li>If loading data objects from the DAO fails, a {@link com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException} is thrown.
 * A {@link com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException} thrown by the DAO's stream is passed on as it is;
 * a {@link com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException} becomes its cause.</li>
 * <li>If a data object can't be converted into an entity, a {@code DataObjectConversionException} is thrown. It is unchecked,
 * carries the id of the data object which couldn't be converted, and has the original exception as its cause.</li>
 * </ul>
 *
 * An entity loader holds resources of the DAO (e.g. open files or database cursors), which are released by
 * {@link AutoCloseable#close()}. The repository registers the loader as close handler of the returned stream
 * ({@link java.util.stream.Stream#onClose(Runnable)}, which calls {@link Runnable#run()}), so callers are supposed to close
 * the returned stream, e.g. by using a try-with-resources statement.
 *
 * @param <ID> the data type of the unique identifier of the {@link Entity} and its corresponding {@link GenericDataObject}
 * @param <DO> the data type of data objects loaded from the underlying DAO
 * @param <E> the actual data type of the entities that are loaded
 */
public interface GenericEntityLoader<ID extends Serializable, DO extends GenericDataObject<ID>, E extends Entity<ID>> extends Spliterator<E>, Runnable, AutoCloseable
{

}
