package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericLazyLoader;

/**
 * Mock implementation of {@link LazyLoader} for unit testing
 */
public class ExampleLazyLoaderImpl extends GenericLazyLoader<Integer, ExampleDO>
{
    protected Collection<Integer> mockedDataSource;
    protected Iterator<Integer> mdsIterator;

    public ExampleLazyLoaderImpl(Function<Integer, ExampleDO> sourceRecordTodataObjConversionFn, Collection<Integer> mockedDataSource)
    {
        super(sourceRecordTodataObjConversionFn);

        if (mockedDataSource == null) throw new IllegalArgumentException();

        this.mockedDataSource = mockedDataSource;
        this.mdsIterator = this.mockedDataSource.iterator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super ExampleDO> action) 
    {
        if (!this.mdsIterator.hasNext()) return false;

        Integer nextDataObjId = this.mdsIterator.next();
        ExampleDO nextDataObj = this.sourceRecordToDataObjConversionFn.apply(nextDataObjId);

        action.accept(nextDataObj);

        return true;
    }

    @Override
    public Spliterator<ExampleDO> trySplit() 
    {
        return null;
    }

    @Override
    public long estimateSize() 
    {
        return this.mockedDataSource.size();
    }

    @Override
    public int characteristics() 
    {
        return Spliterator.IMMUTABLE;
    }

    @Override
    public void close()
    {
        return;
    }
}