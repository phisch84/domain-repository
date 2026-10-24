package com.schoste.ddd.domain.v3.dal.services.mocked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v3.dal.services.TestDataAccessObject;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.inmemory.AbstractInMemoryDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository.
 * Keeps the data objects in memory (see {@link AbstractInMemoryDAO}) and records how it is used (see {@link TestDataAccessObject}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class ExampleDAOImpl extends AbstractInMemoryDAO<ExampleDO> implements ExampleDAO, TestDataAccessObject<Long, ExampleDO>
{
	protected long lastVirtualId = 0;

	protected List<Query<Long>> queries = new ArrayList<>();
	protected int openedStreams = 0;
	protected int closedStreams = 0;
	protected DAOException nextQueryFailure = null;
	protected RuntimeException nextStreamFailure = null;

	@Override
	protected ExampleDO doCreateDataObject() throws Exception
	{
		return new ExampleDO();
	}

	@Override
	protected boolean doIsVirtualId(Long id)
	{
		return (id < 0);
	}

	@Override
	protected synchronized Long doCreateVirtualId() throws Exception
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}

	/**
	 * Records a query and throws the failure set by {@link #failNextQuery(DAOException)}, if any
	 *
	 * @param method the name of the queried method
	 * @param ids the requested ids, if any
	 * @throws DAOException the failure set by {@link #failNextQuery(DAOException)}
	 */
	protected synchronized void beforeQuery(String method, Long... ids) throws DAOException
	{
		this.queries.add(new Query<>(method, ids));

		DAOException failure = this.nextQueryFailure;

		this.nextQueryFailure = null;

		if (failure != null) throw failure;
	}

	/**
	 * Counts a returned stream, records its closing and lets it fail as set by {@link #failNextStream(RuntimeException)}
	 *
	 * @param dataObjStream the stream to return
	 * @return the tracked stream
	 */
	protected synchronized Stream<ExampleDO> track(Stream<ExampleDO> dataObjStream)
	{
		RuntimeException failure = this.nextStreamFailure;

		this.nextStreamFailure = null;
		this.openedStreams++;

		Stream<ExampleDO> trackedStream = dataObjStream.onClose(() -> { synchronized (this) { this.closedStreams++; } });

		if (failure != null) trackedStream = trackedStream.<ExampleDO>map(dataObj -> { throw failure; });

		return trackedStream;
	}

	@Override
	public ExampleDO get(Long id) throws DAOException
	{
		this.beforeQuery("get(id)", id);

		return super.get(id);
	}

	@Override
	public Stream<ExampleDO> get(Collection<Long> ids) throws IllegalArgumentException, IllegalStateException, DAOException
	{
		if (ids == null) return super.get(ids);

		this.beforeQuery("get(ids)", ids.toArray(new Long[0]));

		return this.track(super.get(ids));
	}

	/**
	 * Records the query as "getAll" if it was made by {@link #getAll()} or {@link #getAll(DeletedDataObjects)}
	 * (they call this method with {@link Long#MIN_VALUE}), as "getModifiedSince" otherwise.
	 * The other query methods aren't overridden, so every query is recorded once.
	 */
	@Override
	public Stream<ExampleDO> getModifiedSince(long timestamp, DeletedDataObjects deleted) throws IllegalArgumentException, DAOException
	{
		if (deleted == null) return super.getModifiedSince(timestamp, deleted);

		this.beforeQuery((timestamp == Long.MIN_VALUE) ? "getAll" : "getModifiedSince");

		return this.track(super.getModifiedSince(timestamp, deleted));
	}

	@Override
	public synchronized void seed(ExampleDO dataObject)
	{
		if ((dataObject.getId() == null) || this.isVirtualId(dataObject.getId())) dataObject.setId(++this.lastDataObjectId);
		else this.lastDataObjectId = Math.max(this.lastDataObjectId, dataObject.getId());

		this.dataObjects.put(dataObject.getId(), dataObject);
	}

	@Override
	public synchronized List<Query<Long>> getQueries() { return new ArrayList<>(this.queries); }

	@Override
	public synchronized int getOpenedStreams() { return this.openedStreams; }

	@Override
	public synchronized int getClosedStreams() { return this.closedStreams; }

	@Override
	public synchronized void failNextQuery(DAOException failure) { this.nextQueryFailure = failure; }

	@Override
	public synchronized void failNextStream(RuntimeException failure) { this.nextStreamFailure = failure; }

	@Override
	public synchronized void resetStatistics()
	{
		this.queries.clear();
		this.openedStreams = 0;
		this.closedStreams = 0;
		this.nextQueryFailure = null;
		this.nextStreamFailure = null;
	}
}
