package com.schoste.ddd.domain.v3.dal.services.serialization;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.dal.services.ExampleDAO;
import com.schoste.ddd.infrastructure.dao.v4.serialization.AbstractSerializationDAO;

/**
 * Implementation of the ExampleDAO interface used in DomainObjectRepository.
 * Stores the data objects as serialized files (see {@link AbstractSerializationDAO}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class ExampleDAOImpl extends AbstractSerializationDAO<Long, ExampleDO> implements ExampleDAO
{
	@Autowired
	protected ApplicationContext applicationContext;

	protected long lastVirtualId = 0;

	/**
	 * Creates a new DAO which stores the data objects in the given folder
	 *
	 * @param storagePath the path to the folder of the data objects
	 * @throws IllegalArgumentException thrown if storagePath is null
	 * @throws IllegalStateException thrown if the folder can't be used
	 * @throws Exception re-throws every exception
	 */
	public ExampleDAOImpl(String storagePath) throws IllegalArgumentException, IllegalStateException, Exception
	{
		super(storagePath);
	}

	@Override
	protected ExampleDO doCreateDataObject() throws Exception
	{
		return (ExampleDO) this.applicationContext.getBean(ExampleDO.class);
	}

	@Override
	protected boolean doIsVirtualId(Long id)
	{
		return (id <= 0);
	}

	@Override
	protected synchronized Long doCreateVirtualId() throws Exception
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
	protected synchronized Long doCreateActualId(Long virtualId) throws Exception
	{
		long now = System.currentTimeMillis();
		Path fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(now));

		while (Files.exists(fileToCheck)) fileToCheck = Paths.get(this.storagePath.toString(), String.valueOf(++now));

		return now;
	}

	@Override
	protected Long toId(String value)
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
