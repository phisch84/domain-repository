package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import java.util.function.Function;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dal.v2.services.LazyLoader;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDAOImpl extends GenericMockedDAO<ExampleDO> implements ExampleDAO
{
	@Override
	public ExampleDO createDataObject() throws DALException
	{
		return new ExampleDO();
	}

	@Override
	protected LazyLoader<Integer, ExampleDO> createLazyLoader() throws Exception 
	{
		Function<Integer, ExampleDO> cf = id -> this.safeGet(id);
		LazyLoader<Integer, ExampleDO> mlli = new ExampleLazyLoaderImpl(cf, this.dataObjects.keySet());

		return mlli;
	}

	protected ExampleDO safeGet(int id)
	{
		try
		{
			return this.dataObjects.get(id);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return null;
		}
	}
}
