package com.schoste.ddd.domain.v2.services;

import java.io.Serializable;
import java.util.Spliterator;

import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to the implementation of an entity loader which is used in {@link GenericRepository#get(Serializable[])},
 * {@link GenericRepository#get(java.util.Collection)} and {@link GenericRepository#getAll(boolean)}
 * 
 * @param <ID> the data type of the unique identifier of the {@link Entity} and its corresponding {@link GenericDataObject}
 * @param <DO> the data type of data objects loaded from the underlying DAO
 * @param <E> the actual data type of the entities that are loaded
 */
public interface GenericEntityLoader<ID extends Serializable, DO extends GenericDataObject<ID>, E extends Entity<ID>> extends Spliterator<E>, Runnable, AutoCloseable
{

}
