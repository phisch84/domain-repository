package com.schoste.ddd.domain.v3.services.standard;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.schoste.ddd.domain.v3.entities.Entity;
import com.schoste.ddd.domain.v3.entities.Entity.State;
import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.services.DomainTransaction;
import com.schoste.ddd.domain.v3.services.DomainTransactionManager;
import com.schoste.ddd.domain.v3.services.GenericRepository;
import com.schoste.ddd.domain.v3.services.UnitOfWork;
import com.schoste.ddd.domain.v3.services.UnitOfWorkListener;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Standard implementation of the {@link UnitOfWork} for repositories which extend {@link GenericRepositoryImpl}.
 *
 * On {@link UnitOfWorkImpl#commit()}, the repositories are processed in the order passed to the constructor. For each repository,
 * the Unit of Work first deletes the data objects of the removed entities, then saves the added entities, then the modified ones,
 * each by one call of the repository's DAO. The listeners ({@link UnitOfWorkListener}) are notified after each of these steps.
 * Exceptions thrown by listeners are printed to {@code System.err} and don't stop the commit.
 *
 * {@link UnitOfWorkImpl#close()} rolls back the changes which haven't been committed and unregisters the Unit of Work from its
 * repositories. Use it with a try-with-resources statement:
 * <pre>
 * try (UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(customerRepository))
 * {
 *     customerRepository.add(customer);
 *     unitOfWork.commit();
 * }
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class UnitOfWorkImpl implements UnitOfWork
{
	private Map<GenericRepositoryImpl<?,?,?>, RepositoryChangeLog> changeLogs;
	private Set<UnitOfWorkListener> listners = new HashSet<UnitOfWorkListener>();
	private DomainTransactionManager transactionManager = null;

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

	private <ID extends Serializable, DO extends DataObject<ID>> void deleteEntities(GenericRepositoryImpl<ID,?,DO> repository) throws Exception
	{
		DAO<ID, DO> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToRemove = changeLog.getObjectsToRemove();

		synchronized (objectsToRemove)
		{
			Collection<DO> dataObjects = new ArrayList<>(objectsToRemove.size());
			Map<Integer, Object> dataObjectsToObjectsToRemove = new HashMap<>(objectsToRemove.size());

			for (Object objToRemove : objectsToRemove)
			{
				DO dataObject = repository.entityToDataObject(objToRemove);

				dataObjects.add(dataObject);
				dataObjectsToObjectsToRemove.put(System.identityHashCode(dataObject), objToRemove);
			}

			dao.delete(dataObjects);

			for (Object objToRemove : objectsToRemove)
			{
				if (objToRemove instanceof Entity) ((Entity<?>) objToRemove).setState(State.Detached);
			}

			DomainTransactionManagerImpl.registerWritten(repository, objectsToRemove);

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

	private <ID extends Serializable, DO extends DataObject<ID>> void persistNewEntities(GenericRepositoryImpl<ID,?,DO> repository) throws Exception
	{
		DAO<ID, DO> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToPersist = changeLog.getObjectsToAdd();

		synchronized (objectsToPersist)
		{
			Collection<DO> dataObjects = new ArrayList<>(objectsToPersist.size());
			Map<Integer, Object> dataObjectsToObjectsToPersist = new HashMap<>(objectsToPersist.size());

			for (Object objToPersist : objectsToPersist)
			{
				DO dataObject = repository.entityToDataObject(objToPersist);

				dataObjects.add(dataObject);
				dataObjectsToObjectsToPersist.put(System.identityHashCode(dataObject), objToPersist);
			}

			dao.save(dataObjects);

			// update the domain object in case the data object changed
			for (DO dataObject : dataObjects) repository.dataObjectToEntity(dataObject, dataObjectsToObjectsToPersist.get(System.identityHashCode(dataObject)));

			DomainTransactionManagerImpl.registerWritten(repository, objectsToPersist);

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

	private <ID extends Serializable, DO extends DataObject<ID>> void persistExistingEntities(GenericRepositoryImpl<ID,?,DO> repository) throws Exception
	{
		DAO<ID, DO> dao = repository.getDataAccessObject();
		RepositoryChangeLog changeLog = this.changeLogs.get(repository);
		Collection<Object> objectsToPersist = changeLog.getObjectsToUpdate();

		synchronized (objectsToPersist)
		{
			Collection<DO> dataObjects = new ArrayList<>(objectsToPersist.size());
			Map<Integer, Object> dataObjectsToObjectsToPersist = new HashMap<>(objectsToPersist.size());

			for (Object objToPersist : objectsToPersist)
			{
				DO dataObject = repository.entityToDataObject(objToPersist);

				dataObjects.add(dataObject);
				dataObjectsToObjectsToPersist.put(System.identityHashCode(dataObject), objToPersist);
			}

			dao.save(dataObjects);

			// update the domain object in case the data object changed
			for (DO dataObject : dataObjects) repository.dataObjectToEntity(dataObject, dataObjectsToObjectsToPersist.get(System.identityHashCode(dataObject)));

			DomainTransactionManagerImpl.registerWritten(repository, objectsToPersist);

			this.notifyListenersAfterPersistExisting(objectsToPersist);

			objectsToPersist.clear();
		}
	}

	/**
	 * Creates a new Unit of Work for a repository and registers it as the repository's change listener.
	 *
	 * @param repository the repository whose changes the Unit of Work records
	 * @throws IllegalArgumentException thrown if repository is null
	 */
	public UnitOfWorkImpl(GenericRepositoryImpl<?,?,?> repository) throws IllegalArgumentException
	{
		this(Arrays.asList(new GenericRepositoryImpl<?,?,?>[] { repository }));
	}

	/**
	 * Creates a new Unit of Work for several repositories and registers it as their change listener.
	 *
	 * @param repositories the repositories whose changes the Unit of Work records, in the order in which they are committed and rolled back
	 * @throws IllegalArgumentException thrown if repositories is null or contains null
	 */
	public UnitOfWorkImpl(List<GenericRepositoryImpl<?,?,?>> repositories) throws IllegalArgumentException
	{
		if (repositories == null) throw new IllegalArgumentException("repositories");
		if (repositories.contains(null)) throw new IllegalArgumentException("repositories");

		// LinkedHashMap is used here to ensure the sort order when enumerating repositories
		this.changeLogs = new LinkedHashMap<GenericRepositoryImpl<?,?,?>, RepositoryChangeLog>(repositories.size());
		
		for (GenericRepositoryImpl<?,?,?> repository : repositories)
		{
			this.changeLogs.put(repository, new RepositoryChangeLog());
			
			repository.addChangeListener(this);
		}
	}

	/**
	 * Creates a new Unit of Work for several repositories, which commits in transactions of a transaction manager
	 * (see {@link #setTransactionManager(DomainTransactionManager)}), and registers it as their change listener.
	 *
	 * @param repositories the repositories whose changes the Unit of Work records, in the order in which they are committed and rolled back
	 * @param transactionManager the transaction manager to commit with, or null to commit without an own transaction
	 * @throws IllegalArgumentException thrown if repositories is null or contains null
	 */
	public UnitOfWorkImpl(List<GenericRepositoryImpl<?,?,?>> repositories, DomainTransactionManager transactionManager) throws IllegalArgumentException
	{
		this(repositories);

		this.transactionManager = transactionManager;
	}

	/**
	 * Sets the transaction manager the Unit of Work commits with. With a transaction manager, each {@link #commit()} runs in a
	 * transaction of it (an inner one if the thread has a transaction already), so it stores all its changes or none: if it fails,
	 * nothing is stored, and it acts like {@link #rollback()} for all its changes, also the ones it had written already.
	 * Without a transaction manager, a commit isn't atomic (see {@link UnitOfWork#commit()}), unless the thread has a
	 * transaction (see {@link DomainTransactionManager}), which it joins.
	 *
	 * @param transactionManager the transaction manager to commit with, or null to commit without an own transaction
	 */
	public void setTransactionManager(DomainTransactionManager transactionManager)
	{
		this.transactionManager = transactionManager;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The commit is atomic if the Unit of Work has a transaction manager ({@link #setTransactionManager(DomainTransactionManager)}).
	 * The changes of a commit join the transaction which the thread has (see {@link DomainTransactionManager}); if the commit fails,
	 * that transaction is marked for rollback.
	 */
	public void commit() throws DAOException, DomainException
	{
		DomainTransaction transaction = null;

		try
		{
			if (this.transactionManager != null) transaction = this.transactionManager.begin();

			for (GenericRepositoryImpl<?,?,?> repository : this.changeLogs.keySet())
			{
				this.deleteEntities(repository);
				this.persistNewEntities(repository);
				this.persistExistingEntities(repository);
			}

			if (transaction != null) transaction.commit();
		}
		catch (DAOException e)
		{
			this.abortCommit(transaction, e);

			throw e;
		}
		catch (Exception e)
		{
			DomainException failure = (e instanceof DomainException) ? (DomainException) e : new DomainException(e);

			this.abortCommit(transaction, failure);

			throw failure;
		}
	}

	/**
	 * Cleans up after a failed commit. Without an own transaction, the transaction which the thread has (if any) is marked for
	 * rollback, and the changes which haven't been written stay recorded. With an own transaction, it is rolled back, which lets the
	 * repositories forget the entities written by the commit, and the changes which haven't been written are rolled back as well.
	 *
	 * @param transaction the own transaction of the commit, or null
	 * @param failure the failure of the commit, to which further failures are added as suppressed exceptions
	 */
	private void abortCommit(DomainTransaction transaction, Exception failure)
	{
		if (transaction == null)
		{
			DomainTransactionManagerImpl.markForRollback();

			return;
		}

		try
		{
			transaction.close();
		}
		catch (Exception e)
		{
			failure.addSuppressed(e);
		}

		try
		{
			this.rollback();
		}
		catch (Exception e)
		{
			failure.addSuppressed(e);
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

			for (GenericRepositoryImpl<?,?,?> repository : this.changeLogs.keySet())
			{
				RepositoryChangeLog changeLog = this.changeLogs.get(repository);

				// the storage is the gold source: the repository forgets the changed entities instead of restoring them in memory
				for (Collection<Object> changedObjects : Arrays.asList(changeLog.getObjectsToRemove(), changeLog.getObjectsToAdd(), changeLog.getObjectsToUpdate()))
				{
					synchronized (changedObjects)
					{
						for (Object changedObject : changedObjects) repository.forget(changedObject);

						objectsToRollBack.addAll(changedObjects);

						changedObjects.clear();
					}
				}
			}

			this.notifyListenersAfterRollback(objectsToRollBack);
		}
		catch (Exception e)
		{
			throw new DomainException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * A refresh only affects entities with the state Unchanged, which aren't recorded by a Unit of Work.
	 * Therefore, the Unit of Work ignores this notification.
	 */
	@Override
	public void onRefreshed(GenericRepository<?,?,?> repository, Collection<Object> refreshedObjects, Collection<Object> forgottenObjects) throws DomainException
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public void onObjectAdded(GenericRepository<?,?,?> repository, Object obj) throws DomainException 
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
	public void onObjectRemoved(GenericRepository<?,?,?> repository, Object obj) throws DomainException 
	{
		try
		{
			if (!this.changeLogs.containsKey(repository)) return;
			
			RepositoryChangeLog changeLog = this.changeLogs.get(repository);
			
			synchronized (changeLog.getObjectsToUpdate()) { changeLog.getObjectsToUpdate().remove(obj); }
			synchronized (changeLog.getObjectsToAdd()) { changeLog.getObjectsToAdd().remove(obj); }
			
			// Only mark objects for deletion if they were persisted already (repository is one of the keys of the change logs)
			if (((GenericRepositoryImpl<?,?,?>) repository).isPersisted(obj))
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
	public void onObjectModified(GenericRepository<?,?,?> repository, Object obj) throws DomainException
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
			
			for (GenericRepository<?,?,?> repository : this.changeLogs.keySet()) repository.removeChangeListener(this);
			
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
