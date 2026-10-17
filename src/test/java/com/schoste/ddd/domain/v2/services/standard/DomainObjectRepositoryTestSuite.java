package com.schoste.ddd.domain.v2.services.standard;

import java.util.Objects;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.models.ExampleDomainObject;
import com.schoste.ddd.domain.v2.services.GenericRepositoryTest;

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
