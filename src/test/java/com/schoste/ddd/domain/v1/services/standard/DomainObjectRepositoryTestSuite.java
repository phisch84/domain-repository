package com.schoste.ddd.domain.v1.services.standard;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.GenericRepositoryTest;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;

/**
 * Collection of tests for the DomainObjectRepository class.
 * Extending classes will do the actual tests.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public abstract class DomainObjectRepositoryTestSuite extends GenericRepositoryTest<ExampleDomainObject, ExampleDO, DomainObjectRepository>
{
	/**
	 * {@inheritDoc}
	 * 
	 * If the id of one object is smaller 1, then the id is not regarded
	 */
	@Override
	protected boolean compareDomainObjects(ExampleDomainObject one, ExampleDomainObject another) throws Exception
	{
		if ((one.getId() > 0) && (another.getId() > 0)) return (one.getId() == another.getId());
		
		return true;
	}
}
