package com.schoste.ddd.domain.v2.services.standard;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.exceptions.DataObjectConversionException;
import com.schoste.ddd.domain.v2.services.GenericEntityLoader;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Standard implementation of the {@link GenericEntityLoader} interface.
 *
 * @param <ID> the data type of the unique identifier of the {@link Entity} and its corresponding {@link GenericDataObject}
 * @param <DO> the data type of data objects loaded from the underlying DAO
 * @param <E> the actual data type of the entities that are loaded
 */
public class GenericEntityLoaderImpl<ID extends Serializable, DO extends GenericDataObject<ID>, E extends Entity<ID>> implements GenericEntityLoader<ID, DO, E>
{
    protected Queue<E> cachedEntities;
    protected Set<ID> providedIds;
    protected Function<DO, E> dataObjToDomainObjFunction;
    protected Stream<DO> dataObjStream;
    protected Iterator<DO> dataObjStreamIterator;

    /**
     * Creates a new instance
     * 
     * @param cachedEntities the entities which are already loaded and cached by the repository. They take precedence over data objects with the same id from the data object stream.
     * @param dataObjStream a stream (usually to a DAO) that provides the data objects of the entities
     * @param dataObjToDomainObjFunction a function that will convert a data object from the data object stream into an entity
     * @throws IllegalArgumentException thrown if cachedEntities, dataObjStream or dataObjToDomainObjFunction is null
     */
    public GenericEntityLoaderImpl(Collection<E> cachedEntities, Stream<DO> dataObjStream, Function<DO, E> dataObjToDomainObjFunction) throws IllegalArgumentException
    {
        if (cachedEntities == null) throw new IllegalArgumentException();
        if (dataObjStream == null) throw new IllegalArgumentException();
        if (dataObjToDomainObjFunction == null) throw new IllegalArgumentException();

        this.cachedEntities = new ArrayDeque<>(cachedEntities.size());
        this.providedIds = new HashSet<>(cachedEntities.size());

        for (E cachedEntity : cachedEntities)
        {
            if (cachedEntity == null) continue;

            this.cachedEntities.add(cachedEntity);
            this.providedIds.add(cachedEntity.getId());
        }

        this.dataObjToDomainObjFunction = dataObjToDomainObjFunction;
        this.dataObjStream = dataObjStream;
        this.dataObjStreamIterator = this.dataObjStream.iterator();
    }

    /**
     * Implements {@link Spliterator#tryAdvance(Consumer)}.
     * Called by the encapsulating {@link Stream} class when the next record is requested.
     * 
     * @param action the consumer of the stream that will take the next data object when {@link Consumer#accept(Object)} is called by this method.
     * @return true if a data object was provided, false if there are no more data objects
     */
    @Override
    public boolean tryAdvance(Consumer<? super E> action) 
    {
        if (this.dataObjStreamIterator == null) return false;

        if (!this.cachedEntities.isEmpty())
        {
            E domainObj = this.cachedEntities.poll();

            action.accept(domainObj);

            return true;
        }

        DO dataObj;

        while ((dataObj = this.nextDataObject()) != null)
        {
            if (dataObj.getIsDeleted()) continue;
            if (!this.providedIds.add(dataObj.getId())) continue; // the id belongs to a cached entity or was provided already

            E domainObj = this.convert(dataObj);

            action.accept(domainObj);

            return true;
        }

        return false;
    }

    /**
     * Gets the next data object from the data object stream.
     *
     * @return the next data object, or null if there are no more data objects
     * @throws DALRuntimeException thrown if the data object stream fails. A {@link DALRuntimeException} is passed on as it is,
     * every other exception becomes the cause of a new one
     */
    protected DO nextDataObject() throws DALRuntimeException
    {
        try
        {
            while (this.dataObjStreamIterator.hasNext())
            {
                DO dataObj = this.dataObjStreamIterator.next();

                if (dataObj != null) return dataObj;
            }

            return null;
        }
        catch (DALRuntimeException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DALRuntimeException(e);
        }
    }

    /**
     * Converts a data object into an entity by calling the conversion function.
     *
     * @param dataObj the data object to convert
     * @return the entity, never null
     * @throws DataObjectConversionException thrown if the conversion function throws or returns null
     */
    protected E convert(DO dataObj) throws DataObjectConversionException
    {
        E domainObj;

        try
        {
            domainObj = this.dataObjToDomainObjFunction.apply(dataObj);
        }
        catch (DataObjectConversionException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DataObjectConversionException(dataObj.getId(), e);
        }

        if (domainObj == null) throw new DataObjectConversionException(dataObj.getId(), null);

        return domainObj;
    }

    /**
     * Implements {@link Spliterator#trySplit()}.
     * May be implemented by the deriving class.
     * Unused in this implementation, but can be overwritten and implemented if needed.
     * 
     * @return an instance of a {@link Spliterator} with a subset of elements
     */
    @Override
    public Spliterator<E> trySplit() 
    {
        return null;
    }

    /**
     * Implements {@link Spliterator#estimateSize()}.
     * Needs to be implemented by the deriving class.
     * 
     * @return an estimated amount of elements available. {@link Long#MAX_VALUE} in case no estimate can be given.
     */
    @Override
    public long estimateSize() 
    {
        return Long.MAX_VALUE;
    }

    /**
     * Implements {@link Spliterator#characteristics()}.
     * May be implemented by the deriving class.
     * In this implementation it simply reports an immutable collection
     * 
     * @return a value indicating the characteristics of the collection
     */
    @Override
    public int characteristics() 
    {
        return Spliterator.IMMUTABLE;
    }

    /**
     * Implements {@link Runnable#run()}.
     * Callback method for {@link java.util.stream.Stream#onClose(Runnable)} which will be called by the encapsulating {@link Stream} class
     * when it is closed.
     */
    @Override
    public void run() 
    {
        try
        {
            this.close();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Implements {@link AutoCloseable#close()}.
     * Called by {@link #run()} which will be called by the encapsulating {@link Stream} class
     * when it is closed.
     * Closes the data stream of the underlying DAO
     */
    @Override
    public void close() throws Exception 
    {
        if (this.dataObjStream != null) this.dataObjStream.close();

        this.dataObjStreamIterator = null;
        this.dataObjStream = null;
    }
}