package com.schoste.ddd.domain.v3.services.standard;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.ExampleDAO;
import com.schoste.ddd.domain.v3.models.ExampleDomainObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Unit of Work tests for the DomainObjectRepository.
 * Extending classes provide the configuration (i.e. the DAO) to run them on.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public abstract class DomainObjectUnitOfWorkTestSuite extends GenericUnitOfWorkTest<Long, ExampleDomainObject, ExampleDO, DomainObjectRepository>
{
	@Autowired
	private DomainObjectRepository repository;

	@Autowired
	private ExampleDAO dao;

	@Override
	protected DomainObjectRepository getRepository() { return this.repository; }

	@Override
	protected DAO<Long, ExampleDO> getDataAccessObject() { return this.dao; }

	@Override
	protected void changeValues(ExampleDomainObject entity)
	{
		entity.setProperty1("changed1");
		entity.setProperty2("changed2");
		entity.setProperty3("changed3");
	}

	@Override
	protected boolean hasValuesOf(ExampleDomainObject entity, ExampleDO dataObject)
	{
		return Objects.equals(entity.getProperty1(), dataObject.getProperty1())
			&& Objects.equals(entity.getProperty2(), dataObject.getProperty2())
			&& Objects.equals(entity.getProperty3(), dataObject.getProperty3());
	}
}
