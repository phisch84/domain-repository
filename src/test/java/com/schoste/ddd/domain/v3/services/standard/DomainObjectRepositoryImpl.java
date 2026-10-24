package com.schoste.ddd.domain.v3.services.standard;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v3.models.ExampleDomainObject;
import com.schoste.ddd.domain.v3.services.AutoObjectConverter;
import com.schoste.ddd.domain.v3.services.GenericEntityLoader;

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

	protected RuntimeException nextConversionFailure = null;

	/**
	 * Lets the next conversion of a data object into an entity fail
	 *
	 * @param failure the exception the next conversion throws
	 */
	public synchronized void failNextConversion(RuntimeException failure) { this.nextConversionFailure = failure; }

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
		RuntimeException failure = this.nextConversionFailure;

		this.nextConversionFailure = null;

		if (failure != null) throw failure;

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

	@Override
	public boolean isVirtualId(Long id) throws IllegalArgumentException
	{
		if (id == null) throw new IllegalArgumentException("id");
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
	protected GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> createEntityLoader(Collection<ExampleDomainObject> cachedEntities, Stream<ExampleDO> dataObjStream, Function<ExampleDO, ExampleDomainObject> dataObjToEntityFunction) throws Exception
	{
		GenericEntityLoader<Long, ExampleDO, ExampleDomainObject> el = (GenericEntityLoader<Long, ExampleDO, ExampleDomainObject>) this.applicationContext.getBean(GenericEntityLoader.class, cachedEntities, dataObjStream, dataObjToEntityFunction);

		return el;
	}
}
