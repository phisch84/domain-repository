package com.schoste.ddd.domain.v2.services.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.entities.Entity;
import com.schoste.ddd.domain.v2.exceptions.DataObjectConversionException;
import com.schoste.ddd.domain.v2.models.ExampleDomainObject;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;

/**
 * Tests of {@link GenericEntityLoaderImpl} without a repository or DAO (layer L of the test grid).
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class GenericEntityLoaderImplTest
{
	/**
	 * Ids of the data objects passed to the conversion function, in the order of the calls
	 */
	protected List<Long> convertedIds;

	/**
	 * Conversion function which records its calls and creates an Unchanged entity with the data object's id
	 */
	protected Function<ExampleDO, ExampleDomainObject> recordingConversion;

	protected static ExampleDO createDataObject(long id, boolean isDeleted)
	{
		ExampleDO dataObject = new ExampleDO();

		dataObject.setId(id);
		dataObject.setIsDeleted(isDeleted);

		return dataObject;
	}

	protected static ExampleDO createDataObject(long id)
	{
		return createDataObject(id, false);
	}

	protected static ExampleDomainObject createEntity(long id)
	{
		ExampleDomainObject entity = new ExampleDomainObject();

		entity.setId(id);
		entity.setState(Entity.State.Unchanged);

		return entity;
	}

	protected static List<Long> getIds(Collection<ExampleDomainObject> entities)
	{
		return entities.stream().map(ExampleDomainObject::getId).sorted().collect(Collectors.toList());
	}

	protected GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> createLoader(Collection<ExampleDomainObject> cachedEntities, Stream<ExampleDO> dataObjStream)
	{
		return new GenericEntityLoaderImpl<>(cachedEntities, dataObjStream, this.recordingConversion);
	}

	protected static List<ExampleDomainObject> consume(GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> loader)
	{
		try (Stream<ExampleDomainObject> entityStream = StreamSupport.stream(loader, false).onClose(loader))
		{
			return entityStream.collect(Collectors.toList());
		}
	}

	@Before
	public void setUp()
	{
		this.convertedIds = new ArrayList<>();
		this.recordingConversion = dataObj ->
		{
			this.convertedIds.add(dataObj.getId());

			return createEntity(dataObj.getId());
		};
	}

	/**
	 * L1: cached entities are provided as the same instances; data objects of other ids are converted into entities.
	 */
	@Test
	public void tryAdvance_cachedEntitiesAndDataObjects_providesCachedInstancesAndConvertedEntities()
	{
		ExampleDomainObject cached1 = createEntity(1L);
		ExampleDomainObject cached2 = createEntity(2L);

		List<ExampleDomainObject> entities = consume(this.createLoader(Arrays.asList(cached1, cached2), Stream.of(createDataObject(3L), createDataObject(4L))));

		Assert.assertEquals(Arrays.asList(1L, 2L, 3L, 4L), getIds(entities));
		Assert.assertTrue(entities.stream().anyMatch(entity -> entity == cached1));
		Assert.assertTrue(entities.stream().anyMatch(entity -> entity == cached2));
		Assert.assertEquals(Arrays.asList(3L, 4L), this.convertedIds);
	}

	/**
	 * L2: a data object whose id belongs to a cached entity is skipped without calling the conversion function.
	 */
	@Test
	public void tryAdvance_dataObjectOfCachedId_skipsWithoutConversion()
	{
		ExampleDomainObject cached = createEntity(1L);

		List<ExampleDomainObject> entities = consume(this.createLoader(Collections.singletonList(cached), Stream.of(createDataObject(1L), createDataObject(2L))));

		Assert.assertEquals(Arrays.asList(1L, 2L), getIds(entities));
		Assert.assertTrue(entities.contains(cached));
		Assert.assertEquals(Collections.singletonList(2L), this.convertedIds);
	}

	/**
	 * L3: a data object flagged deleted is skipped without calling the conversion function.
	 */
	@Test
	public void tryAdvance_dataObjectFlaggedDeleted_skipsWithoutConversion()
	{
		List<ExampleDomainObject> entities = consume(this.createLoader(Collections.emptyList(), Stream.of(createDataObject(1L, true), createDataObject(2L))));

		Assert.assertEquals(Collections.singletonList(2L), getIds(entities));
		Assert.assertEquals(Collections.singletonList(2L), this.convertedIds);
	}

	/**
	 * L4: an id contained twice in the data object stream is provided once.
	 */
	@Test
	public void tryAdvance_duplicateIdInDataObjects_providesOnce()
	{
		List<ExampleDomainObject> entities = consume(this.createLoader(Collections.emptyList(), Stream.of(createDataObject(1L), createDataObject(1L), createDataObject(2L))));

		Assert.assertEquals(Arrays.asList(1L, 2L), getIds(entities));
		Assert.assertEquals(Arrays.asList(1L, 2L), this.convertedIds);
	}

	/**
	 * L5: if the conversion function returns null, a {@link DataObjectConversionException} with the data object's id is thrown.
	 */
	@Test
	public void tryAdvance_conversionReturnsNull_throwsDataObjectConversionException()
	{
		GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> loader = new GenericEntityLoaderImpl<>(Collections.emptyList(), Stream.of(createDataObject(1L)), dataObj -> null);

		try
		{
			consume(loader);

			Assert.fail();
		}
		catch (DataObjectConversionException e)
		{
			Assert.assertEquals(1L, e.getDataObjectId());
		}
	}

	/**
	 * L6: data objects are pulled lazily, only as far as the entity stream is consumed.
	 */
	@Test
	public void tryAdvance_partialConsumption_pullsOnlyNeededDataObjects()
	{
		List<Long> pulledIds = new ArrayList<>();
		Stream<ExampleDO> dataObjStream = Stream.of(createDataObject(1L), createDataObject(2L), createDataObject(3L)).peek(dataObj -> pulledIds.add(dataObj.getId()));
		GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> loader = this.createLoader(Collections.emptyList(), dataObjStream);

		try (Stream<ExampleDomainObject> entityStream = StreamSupport.stream(loader, false).onClose(loader))
		{
			Assert.assertTrue(entityStream.findFirst().isPresent());
		}

		Assert.assertEquals(Collections.singletonList(1L), pulledIds);
		Assert.assertEquals(Collections.singletonList(1L), this.convertedIds);
	}

	/**
	 * L7: closing the loader closes the data object stream; afterwards, no more entities are provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void close_openDataObjectStream_closesItAndEnds() throws Exception
	{
		boolean[] closed = { false };
		Stream<ExampleDO> dataObjStream = Stream.of(createDataObject(1L)).onClose(() -> closed[0] = true);
		GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> loader = this.createLoader(Collections.singletonList(createEntity(2L)), dataObjStream);

		loader.close();

		Assert.assertTrue(closed[0]);
		Assert.assertFalse(loader.tryAdvance(entity -> Assert.fail()));
	}

	/**
	 * L7: {@link GenericEntityLoaderImpl#run()} (the close handler of the entity stream) closes the data object stream.
	 */
	@Test
	public void run_openDataObjectStream_closesIt()
	{
		boolean[] closed = { false };
		Stream<ExampleDO> dataObjStream = Stream.of(createDataObject(1L)).onClose(() -> closed[0] = true);

		consume(this.createLoader(Collections.emptyList(), dataObjStream));

		Assert.assertTrue(closed[0]);
	}

	/**
	 * L8: a {@link DALRuntimeException} thrown by the data object stream is passed on as it is.
	 */
	@Test
	public void tryAdvance_dataObjectStreamThrowsDALRuntimeException_passesItOn()
	{
		DALRuntimeException failure = new DALRuntimeException(new DALException());
		Stream<ExampleDO> dataObjStream = Stream.of(createDataObject(1L)).map(dataObj -> { if (dataObj != null) throw failure; return dataObj; });

		try
		{
			consume(this.createLoader(Collections.emptyList(), dataObjStream));

			Assert.fail();
		}
		catch (DALRuntimeException e)
		{
			Assert.assertSame(failure, e);
		}
	}

	/**
	 * L8: any other unchecked exception thrown by the data object stream is thrown as {@link DALRuntimeException}.
	 */
	@Test
	public void tryAdvance_dataObjectStreamThrowsOtherException_throwsDALRuntimeException()
	{
		IllegalStateException failure = new IllegalStateException("test");
		Stream<ExampleDO> dataObjStream = Stream.of(createDataObject(1L)).map(dataObj -> { if (dataObj != null) throw failure; return dataObj; });

		try
		{
			consume(this.createLoader(Collections.emptyList(), dataObjStream));

			Assert.fail();
		}
		catch (DALRuntimeException e)
		{
			Assert.assertSame(failure, e.getCause().getCause());
		}
	}

	/**
	 * L9: if the conversion function throws, a {@link DataObjectConversionException} with the data object's id and the
	 * original exception as cause is thrown.
	 */
	@Test
	public void tryAdvance_conversionThrows_throwsDataObjectConversionExceptionWithIdAndCause()
	{
		IllegalStateException failure = new IllegalStateException("test");
		GenericEntityLoaderImpl<Long, ExampleDO, ExampleDomainObject> loader = new GenericEntityLoaderImpl<>(Collections.emptyList(), Stream.of(createDataObject(1L)), dataObj -> { throw failure; });

		try
		{
			consume(loader);

			Assert.fail();
		}
		catch (DataObjectConversionException e)
		{
			Assert.assertEquals(1L, e.getDataObjectId());
			Assert.assertSame(failure, e.getCause());
		}
	}
}
