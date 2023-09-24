package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dal.v2.services.mocked.GenericMockedDAO;

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

}
