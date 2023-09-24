package com.schoste.ddd.domain.v1.services.standard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.domain.v1.services.AutoObjectConverter;

/**
 * Extension of the {@see AutoObjectConverterImpl} class which uses Spring to instantiate
 * objects.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class SpringAutoObjectConverterImpl extends AutoObjectConverterImpl implements AutoObjectConverter
{
	@Autowired
	protected ApplicationContext applicationContext;

	@Override
	protected Object getInstance(Class<?> clazz) 
	{
		return this.applicationContext.getBean(clazz);
	}

	@Override
	protected Object getInstance(String className)
	{
		return this.applicationContext.getBean(className);
	}
}
