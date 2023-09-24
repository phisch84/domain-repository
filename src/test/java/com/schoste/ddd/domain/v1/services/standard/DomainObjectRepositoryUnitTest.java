package com.schoste.ddd.domain.v1.services.standard;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;

/**
 * Unit test configuration / implementation of the test suite for the DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainObjectRepositoryUnitTest extends DomainObjectRepositoryTestSuite
{
	@Autowired
	private DomainObjectRepository repository;
	
	@Override
	protected DomainObjectRepository getRepository() { return this.repository; }

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createDataObjects(String callingMethodName, int callId) throws Exception
	{
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExampleDomainObject createDomainObject(String callingMethodName, int callId) throws Exception
	{
		ExampleDomainObject domainObject = this.getRepository().createObject();
		
		domainObject.setProperty1(String.format("%s_%s", callingMethodName, callId));
		domainObject.setProperty2(String.format("%s_%s_%s", callingMethodName, callId, callId));
		domainObject.setProperty3(String.format("%s_%s_%s_%s", callingMethodName, callId, callId, callId));
		
		return domainObject;
	}
	
}
