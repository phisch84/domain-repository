package com.schoste.ddd.domain.v1.services.standard;

import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.LazyRepositoryLoader;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v2.services.ExampleDAO;

/**
 * Example implementation of the DomainObjectRepository interface
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class DomainObjectRepositoryImpl extends GenericRepositoryImpl<ExampleDomainObject, ExampleDO> implements DomainObjectRepository
{
	@Autowired
	private ExampleDAO dao;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExampleDAO getDataAccessObject() { return this.dao; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void afterAutoConversation(ExampleDO dataObject, ExampleDomainObject domainObject) throws Exception
	{
		// DO NOT SET THE ID OF THE DOMAIN-OBJECT
		domainObject.setProperty3(dataObject.getProperty3());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void afterAutoConversation(ExampleDomainObject domainObject, ExampleDO dataObject) throws Exception
	{
		// DO NOT SET THE ID OF THE DOMAIN-OBJECT
		dataObject.setProperty3(domainObject.getProperty3());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected LazyRepositoryLoader<ExampleDO, ExampleDomainObject> createLazyLoader() throws Exception 
	{
		Stream<ExampleDO> dataSource = this.getDataAccessObject().getAll(null);
		Function<ExampleDO, ExampleDomainObject> dataObjToDomainObjFn = dataObj -> this.safeDataObjectToDomainObject(dataObj);
		LazyRepositoryLoader<ExampleDO, ExampleDomainObject> lrl = this.applicationContext.getBean(LazyRepositoryLoader.class, dataObjToDomainObjFn, dataSource);

		return lrl;
	}

	protected ExampleDomainObject safeDataObjectToDomainObject(ExampleDO dataObj)
	{
		try
		{
			return super.dataObjectToDomainObject(dataObj, null);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return null;
		}
	}
}
