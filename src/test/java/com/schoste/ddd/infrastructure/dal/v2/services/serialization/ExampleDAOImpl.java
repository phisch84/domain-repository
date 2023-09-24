package com.schoste.ddd.infrastructure.dal.v2.services.serialization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.services.ExampleDAO;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDAOImpl extends GenericSerializationDAO<ExampleDO> implements ExampleDAO
{
	@Autowired
	protected ApplicationContext applicationContext;

	/**
	 * {@inheritDoc}
	 */
	public ExampleDAOImpl(String storagePath) throws IllegalArgumentException, IllegalStateException, Exception 
	{
		super(storagePath);
	}
	
	@Override
	public ExampleDO createDataObject() throws DALException
	{
		return (ExampleDO) this.applicationContext.getBean(ExampleDO.class);
	}

}
