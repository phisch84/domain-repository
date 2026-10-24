package com.schoste.ddd.domain.v3.dal.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Test-side extension of a DAO which lets tests arrange the storage and observe how the repository uses the DAO.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the data type of the identifiers of the data objects
 * @param <DO> the data object class
 */
public interface TestDataAccessObject<ID extends Serializable, DO extends DataObject<ID>> extends DAO<ID, DO>
{
	/**
	 * A query the DAO has received
	 *
	 * @param <ID> the data type of the identifiers of the data objects
	 */
	public static class Query<ID>
	{
		private final String method;
		private final List<ID> ids;

		/**
		 * Creates a new instance
		 *
		 * @param method the name of the queried method, e.g. "get(id)", "get(ids)", "getAll" or "getRecent"
		 * @param ids the requested ids, if any
		 */
		@SafeVarargs
		public Query(String method, ID... ids)
		{
			this.method = method;
			this.ids = (ids == null) ? Collections.<ID>emptyList() : Collections.unmodifiableList(Arrays.asList(ids));
		}

		/**
		 * Gets the name of the queried method
		 *
		 * @return the name of the queried method, e.g. "get(id)", "get(ids)", "getAll" or "getRecent"
		 */
		public String getMethod() { return this.method; }

		/**
		 * Gets the requested ids
		 *
		 * @return the requested ids, empty if the method doesn't take ids
		 */
		public List<ID> getIds() { return this.ids; }

		@Override
		public String toString() { return String.format("%s%s", this.method, this.ids.isEmpty() ? "" : this.ids); }
	}

	/**
	 * Puts a data object into the storage as it is, e.g. with its deleted flag and time stamps.
	 * A data object with the id of a stored one replaces it.
	 * If the data object's id is virtual, it gets a new actual id first.
	 *
	 * @param dataObject the data object to put into the storage
	 */
	public void seed(DO dataObject);

	/**
	 * Gets the queries received since the last call of {@link TestDataAccessObject#resetStatistics()}
	 *
	 * @return the received queries in the order of their arrival
	 */
	public List<Query<ID>> getQueries();

	/**
	 * Gets the number of streams returned since the last call of {@link TestDataAccessObject#resetStatistics()}
	 *
	 * @return the number of returned streams
	 */
	public int getOpenedStreams();

	/**
	 * Gets the number of returned streams which have been closed since the last call of {@link TestDataAccessObject#resetStatistics()}
	 *
	 * @return the number of closed streams
	 */
	public int getClosedStreams();

	/**
	 * Lets the next query fail before it returns
	 *
	 * @param failure the exception the next query throws
	 */
	public void failNextQuery(DAOException failure);

	/**
	 * Lets the next returned stream fail when its first data object is consumed
	 *
	 * @param failure the exception the stream throws
	 */
	public void failNextStream(RuntimeException failure);

	/**
	 * Clears the recorded queries, the stream counters and the pending failures
	 */
	public void resetStatistics();
}
