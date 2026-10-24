package com.schoste.ddd.domain.v2.services.standard;

import java.util.Objects;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v2.dal.services.TestDataAccessObject;
import com.schoste.ddd.domain.v2.models.ExampleDomainObject;

/**
 * Contract tests of the DomainObjectRepository on the mocked DAO
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainObjectRepositoryContractTest extends GenericRepositoryContractTest<Long, ExampleDomainObject, ExampleDO, DomainObjectRepository>
{
	@Autowired
	private DomainObjectRepository repository;

	@Autowired
	private ExampleDAO dao;

	@Override
	protected DomainObjectRepository getRepository() { return this.repository; }

	@SuppressWarnings("unchecked")
	@Override
	protected TestDataAccessObject<Long, ExampleDO> getDataAccessObject() { return (TestDataAccessObject<Long, ExampleDO>) this.dao; }

	@Override
	protected void changeValues(ExampleDO dataObject)
	{
		dataObject.setProperty1("changed1");
		dataObject.setProperty2("changed2");
		dataObject.setProperty3("changed3");
	}

	@Override
	protected boolean hasValuesOf(ExampleDomainObject entity, ExampleDO dataObject)
	{
		return Objects.equals(entity.getProperty1(), dataObject.getProperty1())
			&& Objects.equals(entity.getProperty2(), dataObject.getProperty2())
			&& Objects.equals(entity.getProperty3(), dataObject.getProperty3());
	}

	@Override
	protected void failNextConversion(RuntimeException failure)
	{
		((DomainObjectRepositoryImpl) this.repository).failNextConversion(failure);
	}
}
