package com.schoste.ddd.domain.v2.dal.services.mocked;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v2.dal.services.TestDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.services.mocked.GenericMockedDAO;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository.
 * Keeps the data objects in memory (see {@link GenericMockedDAO}) and records how it is used (see {@link TestDataAccessObject}).
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDAOImpl extends GenericMockedDAO<ExampleDO> implements ExampleDAO, TestDataAccessObject<Long, ExampleDO>
{
	protected long lastVirtualId = 0;

	protected List<Query<Long>> queries = new ArrayList<>();
	protected int openedStreams = 0;
	protected int closedStreams = 0;
	protected DALException nextQueryFailure = null;
	protected RuntimeException nextStreamFailure = null;

	@Override
	protected ExampleDO doCreateDataObject(Object... args) throws Exception
	{
		return new ExampleDO();
	}

	@Override
	public boolean isVirtualId(Long id)
	{
		if (id == null) return false;
		if (id >= 0) return false;

		return true;
	}

	@Override
	protected Long getNextVirtualId(Object... args) throws Exception
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}

	@Override
	protected Long getActualId(Object... args) throws Exception
	{
		return System.currentTimeMillis();
	}

	/**
	 * Records a query and throws the failure set by {@link #failNextQuery(DALException)}, if any
	 *
	 * @param method the name of the queried method
	 * @param ids the requested ids, if any
	 * @throws DALException the failure set by {@link #failNextQuery(DALException)}
	 */
	protected synchronized void beforeQuery(String method, Long... ids) throws DALException
	{
		this.queries.add(new Query<>(method, ids));

		DALException failure = this.nextQueryFailure;

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
	public ExampleDO get(Long id) throws DALException
	{
		this.beforeQuery("get(id)", id);

		return super.get(id);
	}

	@Override
	public Stream<ExampleDO> get(Long[] ids) throws IllegalArgumentException, DALException
	{
		if (ids == null) return super.get(ids);

		this.beforeQuery("get(ids)", ids);

		return this.track(super.get(ids));
	}

	@Override
	public Stream<ExampleDO> getAll() throws DALException
	{
		this.beforeQuery("getAll");

		return this.track(super.getAll());
	}

	@Override
	public Stream<ExampleDO> getRecent(long lastModTS, boolean returnDeleted) throws DALException
	{
		this.beforeQuery("getRecent");

		return this.track(super.getRecent(lastModTS, returnDeleted));
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
	public synchronized void failNextQuery(DALException failure) { this.nextQueryFailure = failure; }

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
