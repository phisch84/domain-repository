package com.schoste.ddd.domain.v1.services;

import java.util.Spliterator;

import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Interface to the implementation of a lazy loader that is used in {@link GenericRepository#getAll(java.util.function.Predicate)}
 * 
 * @param <SR> the data type of data objects loaded from the underlying DAO
 * @param <E> the actual data type of the domain objects that are lazily loaded
 */
public interface LazyRepositoryLoader<DO extends GenericDataObject, E extends DomainObject> extends Spliterator<E>, Runnable, AutoCloseable
{

}
