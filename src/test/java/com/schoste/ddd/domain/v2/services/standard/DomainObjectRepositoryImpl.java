package com.schoste.ddd.domain.v2.services.standard;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v2.models.ExampleDomainObject;
import com.schoste.ddd.domain.v2.services.AutoObjectConverter;
import com.schoste.ddd.domain.v2.services.GenericEntityLoader;

/**
 * Example implementation of the DomainObjectRepository interface
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class DomainObjectRepositoryImpl extends GenericRepositoryImpl<Long, ExampleDomainObject, ExampleDO> implements DomainObjectRepository
{
	@Autowired 
	private ApplicationContext applicationContext;

	@Autowired
	private ExampleDAO dao;

	protected Long lastVirtualId = -1L;

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
	protected Long getNextVirtualId()
	{
		return this.lastVirtualId--;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AutoObjectConverter getAutoObjectConverter()
	{
		AutoObjectConverter converter = this.applicationContext.getBean(AutoObjectConverter.class);

		return converter;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExampleDomainObject doCreateEntity(Class<ExampleDomainObject> entityClass, Object... args) throws Exception
	{
		ExampleDomainObject entity = this.applicationContext.getBean(entityClass);

		return entity;
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

	@Override
	public boolean isVirtualId(Long id) throws IllegalArgumentException
	{
		if (id == null) return false;
		if (id > -1) return false;

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> createEntityLoader(Object... args) throws Exception
	{
		GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> el = (GenericEntityLoader<Long, ExampleDO, ExampleDomainObject>) this.applicationContext.getBean(GenericEntityLoader.class, args);

		return el;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> createEntityLoader(Collection<ExampleDomainObject> cachedEntities, Stream<ExampleDO> dataObjStream) throws Exception
	{
		Function<ExampleDO, ExampleDomainObject> dataObjToDomainObjFunction = dataObj -> this.safeDataObjectToDomainObject(dataObj);
		GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> el = (GenericEntityLoader<Long, ExampleDO, ExampleDomainObject>) this.applicationContext.getBean(GenericEntityLoader.class, cachedEntities, dataObjStream, dataObjToDomainObjFunction);

		return el;
	}
}
