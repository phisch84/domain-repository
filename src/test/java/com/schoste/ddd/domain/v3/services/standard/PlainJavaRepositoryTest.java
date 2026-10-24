package com.schoste.ddd.domain.v3.services.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.mocked.ExampleDAOImpl;
import com.schoste.ddd.domain.v3.entities.Entity.State;
import com.schoste.ddd.domain.v3.exceptions.DomainException;
import com.schoste.ddd.domain.v3.models.ExampleDomainObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Asserts that a repository works without Spring and with the least possible code: the implementing class only provides its DAO,
 * and {@link GenericRepositoryImpl} provides the rest (virtual ids from the DAO, entity creation by the no-argument constructor,
 * the {@link AutoObjectConverterImpl} and the {@link GenericEntityLoaderImpl}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class PlainJavaRepositoryTest
{
	/**
	 * The smallest possible repository: it only provides its DAO
	 */
	protected static class PlainRepository extends GenericRepositoryImpl<Long, ExampleDomainObject, ExampleDO>
	{
		protected final DAO<Long, ExampleDO> dao;

		/**
		 * Creates a new instance
		 *
		 * @param dao the DAO of the repository
		 */
		public PlainRepository(DAO<Long, ExampleDO> dao) { this.dao = dao; }

		@Override
		protected DAO<Long, ExampleDO> getDataAccessObject() { return this.dao; }
	}

	/**
	 * A repository whose entities are created with arguments: it offers a typed factory method and receives the arguments
	 * in {@link GenericRepositoryImpl#doCreateEntity(Class, Object...)}
	 */
	protected static class FactoryRepository extends PlainRepository
	{
		protected final List<Object[]> receivedArgs = new ArrayList<>();

		/**
		 * Creates a new instance
		 *
		 * @param dao the DAO of the repository
		 */
		public FactoryRepository(DAO<Long, ExampleDO> dao) { super(dao); }

		/**
		 * The typed factory method
		 *
		 * @param property1 the value of property 1 of the new entity
		 * @return the new entity
		 * @throws DomainException re-throws every exception
		 */
		public ExampleDomainObject createWithProperty1(String property1) throws DomainException
		{
			return this.createEntity(property1);
		}

		@Override
		protected ExampleDomainObject doCreateEntity(Class<ExampleDomainObject> entityClass, Object... args) throws Exception
		{
			ExampleDomainObject entity = new ExampleDomainObject();

			this.receivedArgs.add(args);

			if (args.length > 0) entity.setProperty1((String) args[0]);

			return entity;
		}
	}

	protected ExampleDAOImpl dao;
	protected PlainRepository repository;

	/**
	 * Creates a new DAO and repository for each test
	 */
	@Before
	public void setUp()
	{
		this.dao = new ExampleDAOImpl();
		this.repository = new PlainRepository(this.dao);
	}

	/**
	 * P1: a new entity is Detached and has a virtual id of the DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_plainRepository_hasVirtualIdOfDataAccessObject() throws Exception
	{
		ExampleDomainObject entity = this.repository.createEntity();

		Assert.assertEquals(State.Detached, entity.getState());
		Assert.assertNotNull(entity.getId());
		Assert.assertTrue(this.dao.isVirtualId(entity.getId()));
	}

	/**
	 * P2: new entities get different virtual ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_twice_givesDifferentIds() throws Exception
	{
		Assert.assertNotEquals(this.repository.createEntity().getId(), this.repository.createEntity().getId());
	}

	/**
	 * P3: the repository decides virtual ids like its DAO.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void isVirtualId_plainRepository_decidesLikeDataAccessObject() throws Exception
	{
		for (Long id : Arrays.asList(-2L, -1L, 0L, 1L, 2L))
		{
			Assert.assertEquals(String.valueOf(id), this.dao.isVirtualId(id), this.repository.isVirtualId(id));
		}
	}

	/**
	 * P4: null isn't accepted as id.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void isVirtualId_null_throwsIllegalArgumentException()
	{
		this.repository.isVirtualId(null);
	}

	/**
	 * P5: an entity is committed and loaded again without Spring; the properties annotated with AutoSet are converted
	 * by the default converter, the others aren't (the repository doesn't complete them).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void commitAndGet_plainRepository_convertsAutoSetProperties() throws Exception
	{
		ExampleDomainObject entity = this.repository.createEntity();

		entity.setProperty1("value1");
		entity.setProperty2("value2");
		entity.setProperty3("value3");

		try (UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(this.repository))
		{
			this.repository.add(entity);
			unitOfWork.commit();
		}

		Long actualId = entity.getId();

		Assert.assertFalse(this.repository.isVirtualId(actualId));

		this.repository.reset();

		ExampleDomainObject loaded = this.repository.get(actualId);

		Assert.assertNotSame(entity, loaded);
		Assert.assertEquals(State.Unchanged, loaded.getState());
		Assert.assertEquals("value1", loaded.getProperty1());
		Assert.assertEquals("value2", loaded.getProperty2());
		Assert.assertNull(loaded.getProperty3());
	}

	/**
	 * P6: the arguments of a typed factory method are passed to doCreateEntity; the entity gets a virtual id and is Detached.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_withArguments_passesThemToDoCreateEntity() throws Exception
	{
		FactoryRepository factoryRepository = new FactoryRepository(this.dao);

		ExampleDomainObject entity = factoryRepository.createWithProperty1("value1");

		Assert.assertEquals("value1", entity.getProperty1());
		Assert.assertArrayEquals(new Object[] { "value1" }, factoryRepository.receivedArgs.get(0));
		Assert.assertTrue(this.dao.isVirtualId(entity.getId()));
		Assert.assertEquals(State.Detached, entity.getState());
	}

	/**
	 * P7: entities loaded from the DAO are created without arguments.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_loadsEntity_createsItWithoutArguments() throws Exception
	{
		FactoryRepository factoryRepository = new FactoryRepository(this.dao);
		ExampleDO dataObject = new ExampleDO();

		this.dao.seed(dataObject);

		Assert.assertNotNull(factoryRepository.get(dataObject.getId()));
		Assert.assertArrayEquals(new Object[0], factoryRepository.receivedArgs.get(0));
	}

	/**
	 * P9: a Unit of Work doesn't accept null as list of repositories.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void unitOfWork_nullRepositories_throwsIllegalArgumentException()
	{
		new UnitOfWorkImpl((List<GenericRepositoryImpl<?,?,?>>) null);
	}

	/**
	 * P10: a Unit of Work doesn't accept null as repository.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void unitOfWork_nullRepository_throwsIllegalArgumentException()
	{
		new UnitOfWorkImpl((GenericRepositoryImpl<?,?,?>) null);
	}

	/**
	 * P11: null isn't accepted as listener to add.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addChangeListener_null_throwsIllegalArgumentException()
	{
		this.repository.addChangeListener(null);
	}

	/**
	 * P12: null isn't accepted as listener to remove.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void removeChangeListener_null_throwsIllegalArgumentException()
	{
		this.repository.removeChangeListener(null);
	}

	/**
	 * P8: the default entity creation doesn't accept arguments, since it uses the no-argument constructor.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createEntity_argumentsWithDefaultCreation_throwsDomainException() throws Exception
	{
		try
		{
			this.repository.createEntity("value1");

			Assert.fail();
		}
		catch (DomainException e)
		{
			Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
	}
}
