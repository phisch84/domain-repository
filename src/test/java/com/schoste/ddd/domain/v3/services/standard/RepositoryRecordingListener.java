package com.schoste.ddd.domain.v3.services.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.schoste.ddd.domain.v3.services.GenericRepository;
import com.schoste.ddd.domain.v3.services.RepositoryChangeListener;

/**
 * Repository change listener which records the notifications it receives
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class RepositoryRecordingListener implements RepositoryChangeListener
{
	private final List<Object> added = new ArrayList<>();
	private final List<Object> removed = new ArrayList<>();
	private final List<Object> modified = new ArrayList<>();
	private final List<Object> refreshed = new ArrayList<>();
	private final List<Object> forgotten = new ArrayList<>();
	private int refreshNotifications = 0;

	/**
	 * Gets the refreshed objects of the received {@link RepositoryChangeListener#onRefreshed(GenericRepository, Collection, Collection)} notifications
	 *
	 * @return the refreshed objects in the order of the notifications
	 */
	public List<Object> getRefreshed() { return this.refreshed; }

	/**
	 * Gets the forgotten objects of the received {@link RepositoryChangeListener#onRefreshed(GenericRepository, Collection, Collection)} notifications
	 *
	 * @return the forgotten objects in the order of the notifications
	 */
	public List<Object> getForgotten() { return this.forgotten; }

	/**
	 * Gets the number of received {@link RepositoryChangeListener#onRefreshed(GenericRepository, Collection, Collection)} notifications
	 *
	 * @return the number of refresh notifications
	 */
	public int getRefreshNotifications() { return this.refreshNotifications; }

	/**
	 * Gets the objects of the received {@link RepositoryChangeListener#onObjectAdded(GenericRepository, Object)} notifications
	 *
	 * @return the added objects in the order of the notifications
	 */
	public List<Object> getAdded() { return this.added; }

	/**
	 * Gets the objects of the received {@link RepositoryChangeListener#onObjectRemoved(GenericRepository, Object)} notifications
	 *
	 * @return the removed objects in the order of the notifications
	 */
	public List<Object> getRemoved() { return this.removed; }

	/**
	 * Gets the objects of the received {@link RepositoryChangeListener#onObjectModified(GenericRepository, Object)} notifications
	 *
	 * @return the modified objects in the order of the notifications
	 */
	public List<Object> getModified() { return this.modified; }

	/**
	 * Checks if no notification was received
	 *
	 * @return true if no notification was received
	 */
	public boolean isEmpty() { return this.added.isEmpty() && this.removed.isEmpty() && this.modified.isEmpty() && (this.refreshNotifications == 0); }

	@Override
	public void onObjectAdded(GenericRepository<?,?,?> repository, Object obj) { this.added.add(obj); }

	@Override
	public void onObjectRemoved(GenericRepository<?,?,?> repository, Object obj) { this.removed.add(obj); }

	@Override
	public void onObjectModified(GenericRepository<?,?,?> repository, Object obj) { this.modified.add(obj); }

	@Override
	public void onRefreshed(GenericRepository<?,?,?> repository, Collection<Object> refreshedObjects, Collection<Object> forgottenObjects)
	{
		this.refreshNotifications++;
		this.refreshed.addAll(refreshedObjects);
		this.forgotten.addAll(forgottenObjects);
	}

	@Override
	public void close() { }
}
