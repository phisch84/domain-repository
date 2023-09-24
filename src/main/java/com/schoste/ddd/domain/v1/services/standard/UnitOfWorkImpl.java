package com.schoste.ddd.domain.v1.services.standard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.schoste.ddd.domain.v1.exceptions.DomainException;
import com.schoste.ddd.domain.v1.models.DomainObject;
import com.schoste.ddd.domain.v1.models.DomainObject.State;
import com.schoste.ddd.domain.v1.services.GenericRepository;
import com.schoste.ddd.domain.v1.services.UnitOfWork;
import com.schoste.ddd.domain.v1.services.UnitOfWorkListener;
import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Implementation of the UOW interface
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class UnitOfWorkImpl implements UnitOfWork
{
	private Map<GenericRepositoryImpl<?,?>, RepositoryChangeLog> changeLogs;
	private Set<UnitOfWorkListener> listners = new HashSet<UnitOfWorkListener>();

	private void notifyListenersAfterDelete(Collection<Object> objs)
	{
		for (UnitOfWorkListener listener : this.listners)
		{
			try
			{
				listener.afterDelete(objs);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	private void deleteDomainObjects(GenericRepositoryImpl<?,?> repository) throws Exception
	{
		GenericDataAccessObject<?> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToRemove = changeLog.getObjectsToRemove();

		synchronized (objectsToRemove)
		{
			GenericDataObject[] dataObjects = new GenericDataObject[objectsToRemove.size()];
			Map<Integer, Object> dataObjectsToObjectsToRemove = new HashMap<>(objectsToRemove.size());
			int index=0;

			for (Object objToRemove : objectsToRemove)
			{
				GenericDataObject dataObject = repository.domainObjectToDataObject(objToRemove);
				
				dataObjects[index++] = dataObject;
				dataObjectsToObjectsToRemove.put(System.identityHashCode(dataObject), objToRemove);
			}

			dao.delete(dataObjects);

			for (Object objToRemove : objectsToRemove)
			{
				if (objToRemove instanceof DomainObject) ((DomainObject) objToRemove).setState(State.Detached);
			}

			this.notifyListenersAfterDelete(objectsToRemove);

			objectsToRemove.clear();
		}
	}

	private void notifyListenersAfterPersistNew(Collection<Object> objs)
	{
		for (UnitOfWorkListener listener : this.listners)
		{
			try
			{
				listener.afterPersistNew(objs);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	private void persistNewDomainObjects(GenericRepositoryImpl<?,?> repository) throws Exception
	{
		GenericDataAccessObject<?> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToPersist = changeLog.getObjectsToAdd();

		synchronized (objectsToPersist)
		{
			GenericDataObject[] dataObjects = new GenericDataObject[objectsToPersist.size()];
			Map<Integer, Object> dataObjectsToObjectsToPersist = new HashMap<>(objectsToPersist.size());
			int index=0;

			for (Object objToPersist : objectsToPersist)
			{
				GenericDataObject dataObject = repository.domainObjectToDataObject(objToPersist);
				
				dataObjects[index++] = dataObject;
				dataObjectsToObjectsToPersist.put(System.identityHashCode(dataObject), objToPersist);
			}

			dao.save(dataObjects);

			// update the domain object in case the data object changed
			for (GenericDataObject dataObject : dataObjects) repository.dataObjectToDomainObject(dataObject, dataObjectsToObjectsToPersist.get(System.identityHashCode(dataObject)));

			this.notifyListenersAfterPersistNew(objectsToPersist);

			objectsToPersist.clear();
		}
	}

	private void notifyListenersAfterPersistExisting(Collection<Object> objs)
	{
		for (UnitOfWorkListener listener : this.listners)
		{
			try
			{
				listener.afterPersistExisting(objs);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	private void persistExistingDomainObjects(GenericRepositoryImpl<?,?> repository) throws Exception
	{
		GenericDataAccessObject<?> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToPersist = changeLog.getObjectsToUpdate();

		synchronized (objectsToPersist)
		{
			GenericDataObject[] dataObjects = new GenericDataObject[objectsToPersist.size()];
			Map<Integer, Object> dataObjectsToObjectsToPersist = new HashMap<>(objectsToPersist.size());
			int index=0;

			for (Object objToPersist : objectsToPersist)
			{
				GenericDataObject dataObject = repository.domainObjectToDataObject(objToPersist);
				
				dataObjects[index++] = dataObject;
				dataObjectsToObjectsToPersist.put(System.identityHashCode(dataObject), objToPersist);
			}

			dao.save(dataObjects);

			// update the domain object in case the data object changed
			for (GenericDataObject dataObject : dataObjects) repository.dataObjectToDomainObject(dataObject, dataObjectsToObjectsToPersist.get(System.identityHashCode(dataObject)));

			this.notifyListenersAfterPersistExisting(objectsToPersist);

			objectsToPersist.clear();
		}
	}

	/**
	 * Creates a new instance of the Unit of Work (UoW) for a given repository.
	 * 
	 * @param repository the repository the UoW is created for.
	 */
	public UnitOfWorkImpl(GenericRepositoryImpl<?,?> repository)
	{
		this(Arrays.asList(new GenericRepositoryImpl<?,?>[] { repository }));
	}
	
	/**
	 * Creates a new instance of the Unit of Work (UoW) for given repositories.
	 * 
	 * @param repositories the repositories the UoW is created for. Pass the repositories in the order in which they should be committed / rolled back
	 */
	public UnitOfWorkImpl(List<GenericRepositoryImpl<?,?>> repositories)
	{
		if (repositories == null) return;
		
		// LinkedHashMap is used here to ensure the sort order when enumerating repositories
		this.changeLogs = new LinkedHashMap<GenericRepositoryImpl<?,?>, RepositoryChangeLog>(repositories.size());
		
		for (GenericRepositoryImpl<?,?> repository : repositories)
		{
			this.changeLogs.put(repository, new RepositoryChangeLog());
			
			repository.addChangeListener(this);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void commit() throws DALException, DomainException 
	{
		try
		{
			//TODO: encapsulate in transaction
			for (GenericRepositoryImpl<?,?> repository : this.changeLogs.keySet())
			{					
				this.deleteDomainObjects(repository);
				this.persistNewDomainObjects(repository);
				this.persistExistingDomainObjects(repository);
				
				repository.resetVirtualDomainObjectId();
			}
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	private void notifyListenersAfterRollback(Collection<Object> objs)
	{
		for (UnitOfWorkListener listener : this.listners)
		{
			try
			{
				listener.afterRollback(objs);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void rollback() throws DomainException
	{
		try
		{
			Collection<Object> objectsToRollBack = new ArrayList<>();

			for (GenericRepositoryImpl<?,?> repository : this.changeLogs.keySet())
			{
				RepositoryChangeLog changeLog = this.changeLogs.get(repository);
	
				synchronized (changeLog.getObjectsToRemove())
				{
					for (Object objToRemove : changeLog.getObjectsToRemove()) repository.add(objToRemove);

					objectsToRollBack.addAll(changeLog.getObjectsToRemove());

					changeLog.getObjectsToRemove().clear();
				}
				
				synchronized (changeLog.getObjectsToAdd())
				{
					for (Object objToAdd : changeLog.getObjectsToAdd()) repository.remove(objToAdd);
					
					objectsToRollBack.addAll(changeLog.getObjectsToAdd());

					changeLog.getObjectsToAdd().clear();
				}
			}

			this.notifyListenersAfterRollback(objectsToRollBack);
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	private void notifyListenersAfterReload(Collection<Object> objs)
	{
		for (UnitOfWorkListener listener : this.listners)
		{
			try
			{
				listener.afterReload(objs);
			}
			catch (Exception e)
			{
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onReload(GenericRepository<?,?> repository) throws DomainException
	{
		try
		{
			if (!this.changeLogs.containsKey(repository)) return;
			
			RepositoryChangeLog changeLog = this.changeLogs.get(repository);
			Collection<Object> detachedObjects = new ArrayList<>();
			
			synchronized (changeLog.getObjectsToRemove())
			{
				for (Object objToRemove : changeLog.getObjectsToRemove())
				{
					if (objToRemove instanceof DomainObject) ((DomainObject) objToRemove).setState(State.Detached);
				}

				detachedObjects.addAll(changeLog.getObjectsToRemove());
				changeLog.getObjectsToRemove().clear();
			}
			
			synchronized (changeLog.getObjectsToAdd())
			{
				for (Object objToAdd : changeLog.getObjectsToAdd())
				{
					if (objToAdd instanceof DomainObject) ((DomainObject) objToAdd).setState(State.Detached);
				}

				detachedObjects.addAll(changeLog.getObjectsToAdd());
				changeLog.getObjectsToAdd().clear();
			}
			
			synchronized (changeLog.getObjectsToUpdate())
			{
				changeLog.getObjectsToUpdate().clear();
			}

			this.notifyListenersAfterReload(detachedObjects);
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onObjectAdded(GenericRepository<?,?> repository, Object obj) throws DomainException 
	{
		try
		{
			if (!this.changeLogs.containsKey(repository)) return;
			
			RepositoryChangeLog changeLog = this.changeLogs.get(repository);
			
			synchronized (changeLog.getObjectsToAdd())
			{
				changeLog.getObjectsToAdd().add(obj);
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onObjectRemoved(GenericRepository<?,?> repository, Object obj) throws DomainException 
	{
		try
		{
			if (!this.changeLogs.containsKey(repository)) return;
			
			RepositoryChangeLog changeLog = this.changeLogs.get(repository);
			
			synchronized (changeLog.getObjectsToUpdate()) { changeLog.getObjectsToUpdate().remove(obj); }
			synchronized (changeLog.getObjectsToAdd()) { changeLog.getObjectsToAdd().remove(obj); }
			
			// Only mark objects for deletion if they were persisted already
			if ((obj instanceof DomainObject) && (((DomainObject) obj).getId() > 0))
			{
				synchronized (changeLog.getObjectsToRemove()) { changeLog.getObjectsToRemove().add(obj); }
			}
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onObjectModified(GenericRepository<?, ?> repository, Object obj) throws DomainException
	{
		try
		{
			if (!this.changeLogs.containsKey(repository)) return;
			
			RepositoryChangeLog changeLog = this.changeLogs.get(repository);
			
			synchronized (changeLog.getObjectsToUpdate()) { changeLog.getObjectsToUpdate().add(obj); }
		}
		catch (Exception e)
		{
			 throw new DomainException(e);
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws IOException 
	{
		try
		{
			this.rollback();
			
			for (GenericRepository<?,?> repository : this.changeLogs.keySet()) repository.removeChangeListener(this);
			
			this.changeLogs.clear();
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addListener(UnitOfWorkListener listener)
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.listners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeListener(UnitOfWorkListener listener)
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.listners.remove(listener);
	}
}
