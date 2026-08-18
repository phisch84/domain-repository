package com.schoste.ddd.domain.v1.services.standard;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.domain.v1.services.LazyRepositoryLoader;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericLazyLoader;

/**
 * Standart implementation of the {@link LazyRepositoryLoader} interface.
 *
 * @param <DO> the data type of the data objects that will be converted to domain objects
 * @param <E> the data type of the domain objects to return
 */
public class LazyRepositoryLoaderImpl<DO extends GenericDataObject, E extends DomainObject> implements LazyRepositoryLoader<DO, E>
{
    protected Function<DO, E> dataObjToDomainObjFunction;
    protected Stream<DO> dataObjStream;
    protected Iterator<DO> dataObjStreamIterator;

    /**
     * Creates a new instance
     * 
     * @param dataObjToDomainObjFunction a function that will convert a data object from the data object stream into a domain object
     * @param dataObjStream a stream (usually to a DAO) that provides the data objects of the domain objects
     * @throws IllegalArgumentException thrown if either dataObjToDomainObjFunction or dataObjStream is null
     */
    public LazyRepositoryLoaderImpl(Function<DO, E> dataObjToDomainObjFunction, Stream<DO> dataObjStream) throws IllegalArgumentException
    {
        if (dataObjToDomainObjFunction == null) throw new IllegalArgumentException();
        if (dataObjStream == null) throw new IllegalArgumentException();

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
        if (!this.dataObjStreamIterator.hasNext()) return false;

        DO dataObj = this.dataObjStreamIterator.next();
        E domainObj = this.dataObjToDomainObjFunction.apply(dataObj);

        action.accept(domainObj);

        return true;
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
     * Called by {@link GenericLazyLoader#run()} which will be called by the encapsulating {@link Stream} class
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
