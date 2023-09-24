package com.schoste.ddd.domain.v1.services;

import java.util.Collection;

import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Generic interface to repositories
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <T> the domain model class
 * @param <DO> the data object class
 */
public interface GenericRepository<T extends DomainObject, DO extends GenericDataObject>
{
	/**
	 * Creates a new domain model but does not add it to the repository.
	 * The new model will have a new, unique virtual id.
	 * 
	 * @return the instance to a new domain model
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public T createObject() throws DomainException;
	
	/**
	 * Reloads all domain objects from the underlying data source.
	 * Existing objects with state Added or Deleted will become detached.
	 * Existing objects with state Modified will become Unchanged.
	 * Existing objects with state Detached will become Unchanged if they exist in the underlying data source.
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void reload() throws DALException, DomainException;
	
	/**
	 * Gets a domain object with a given id.
	 * If no domain object with the given id exists in the repository it will
	 * try to load it from the underlying DAO.
	 * 
	 * @param id the id of the domain object to get
	 * @return the domain object or null if none was found
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public T get(int id) throws DALException, DomainException;
	
	/**
	 * Gets all available domain models
	 * 
	 * @return a collection of domain models
	 * 
	 * @throws DALException re-throws exceptions from the data access layer
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public Collection<T> getAll() throws DALException, DomainException;
	
	/**
	 * Adds a domain model to the repository. The model is only added
	 * if there is no other model with the same id.
	 * 
	 * @param domainObject the model to add
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws IllegalStateException thrown if the state of the domainObject is anything else than Detached or Added
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void add(T domainObject) throws DomainException;
	
	/**
	 * Adds a domain model to the repository. The model is only added
	 * if there is no other model with the same id.
	 * 
	 * @param domainObject the model to add
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws IllegalStateException thrown if the state of the domainObject is anything else than Detached or Added
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void add(Object object) throws DomainException;
	
	/**
	 * Removes a domain model from the repository. The model is only removed
	 * if there is a model with the same id.
	 * 
	 * @param domainObject the model to remove
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void remove(T domainObject) throws DomainException;
	
	/**
	 * Removes a domain model from the repository. The model is only removed
	 * if there is a model with the same id.
	 * 
	 * @param domainObject the model to remove
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void remove(Object object) throws DomainException;

	/**
	 * Marks a domain model as modified so it will be persisted by the UoW.
	 * If the models state anything else but Unchanged the state won't change.
	 * 
	 * @param domainObject the model to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void setModified(T domainObject) throws DomainException;
	
	/**
	 * Marks a domain model as modified so it will be persisted by the UoW.
	 * If the models state anything else but Unchanged the state won't change.
	 * 
	 * @param domainObject the model to mark as modified
	 * @throws IllegalArgumentException thrown if the parameter domainObject is null
	 * @throws DomainException re-throws every exception as {@see DomainException}
	 */
	public void setModified(Object object) throws DomainException;

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
	 * Resets the repository by removing all listeners,
	 * clearing all loaded objects and reseting the virtual object id.
	 * 
	 * @throws Exception re-throws every exception
	 */
	public void reset() throws DomainException;
}
