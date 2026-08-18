package com.schoste.ddd.infrastructure.dal.v2.services.serialization;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dal.v2.services.LazyLoader;

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

	@SuppressWarnings("unchecked")
	@Override
	protected LazyLoader<Integer, ExampleDO> createLazyLoader() throws Exception 
	{
		Function<Integer, GenericDataObject> cv = id -> this.safeDoGet(id);
		LazyLoader<Integer, ExampleDO> ll = this.applicationContext.getBean(LazyLoader.class, cv, this.storagePath);

		return ll;
	}

	/**
	 * Calls the {@link GenericSerializationDAO#doGet(int)} method inside a try-catch block
	 * 
	 * @param id the id of the data object to load
	 * @return the data object with the given id, or null if none was found or on error 
	 */
	protected ExampleDO safeDoGet(Integer id)
	{
		try
		{
			return this.doGet(id);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return null;
		}
	}

}
