package com.schoste.ddd.domain.v1.services.standard;

import java.util.Objects;

import com.schoste.ddd.domain.v1.dal.models.ExampleDO;
import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.GenericRepositoryTest;

/**
 * Collection of tests for the DomainObjectRepository class.
 * Extending classes will do the actual tests.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public abstract class DomainObjectRepositoryTestSuite extends GenericRepositoryTest<Long, ExampleDomainObject, ExampleDO, DomainObjectRepository>
{
	/**
	 * {@inheritDoc}
	 *
	 * If one of the objects wasn't persisted yet, then the id is not regarded
	 */
	@Override
	protected boolean compareDomainObjects(ExampleDomainObject one, ExampleDomainObject another) throws Exception
	{
		if (this.isPersisted(one) && this.isPersisted(another)) return Objects.equals(one.getId(), another.getId());
		
		return true;
	}
}
