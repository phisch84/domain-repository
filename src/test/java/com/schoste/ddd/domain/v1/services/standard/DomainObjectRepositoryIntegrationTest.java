package com.schoste.ddd.domain.v1.services.standard;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test configuration / implementation of the test suite for the DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/integrationtest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainObjectRepositoryIntegrationTest extends DomainObjectRepositoryTestSuite
{
	@Autowired
	private DomainObjectRepository repository;
	
	@Override
	protected DomainObjectRepository getRepository() { return this.repository; }

	@Override
	protected void createDataObjects(String callingMethodName, int callId) throws Exception
	{
		
	}

}
