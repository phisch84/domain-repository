package com.schoste.ddd.domain.v1.dal.services.mocked;

import com.schoste.ddd.domain.v1.dal.models.ExampleDO;
import com.schoste.ddd.domain.v1.dal.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dal.v3.services.mocked.GenericMockedDAO;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository.
 * Keeps the data objects in memory (see {@link GenericMockedDAO}).
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDAOImpl extends GenericMockedDAO<ExampleDO> implements ExampleDAO
{
	protected long lastVirtualId = 0;

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
}
