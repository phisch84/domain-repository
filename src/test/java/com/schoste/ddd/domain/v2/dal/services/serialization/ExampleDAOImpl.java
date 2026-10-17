package com.schoste.ddd.domain.v2.dal.services.serialization;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.dal.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dal.v3.services.serialization.GenericSerializationDAO;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository.
 * Stores the data objects as serialized files (see {@link GenericSerializationDAO}).
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDAOImpl extends GenericSerializationDAO<Long, ExampleDO> implements ExampleDAO
{
	@Autowired
	protected ApplicationContext applicationContext;

	protected long lastVirtualId = 0;

	/**
	 * {@inheritDoc}
	 */
	public ExampleDAOImpl(String storagePath) throws IllegalArgumentException, IllegalStateException, Exception
	{
		super(storagePath);
	}

	@Override
	protected ExampleDO doCreateDataObject(Object... args) throws Exception
	{
		return (ExampleDO) this.applicationContext.getBean(ExampleDO.class);
	}

	@Override
	public boolean isVirtualId(Long id)
	{
		if (id == null) return false;
		if (id > 0) return false;

		return true;
	}

	@Override
	protected Long getNextVirtualId(Object... args) throws Exception
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}

	/**
	 * Returns the current time as id of a new file. If a file with that id exists already, the next free id is returned.
	 */
	@Override
	protected synchronized Long getActualId(Object... args) throws Exception
	{
		long now = System.currentTimeMillis();
		Path fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(now));

		while (Files.exists(fileToCheck)) fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(++now));

		return now;
	}

	@Override
	protected Long toID(String value)
	{
		if (value == null) return 0L;

		try
		{
			return Long.valueOf(value);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return 0L;
		}
	}
}
