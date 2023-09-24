package com.schoste.ddd.domain.v1.services.standard;

import org.springframework.beans.factory.annotation.Autowired;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.standard.GenericRepositoryImpl;
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
}
