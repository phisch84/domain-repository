package com.schoste.ddd.domain.v1.services.standard;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.AutoObjectConverter;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;

abstract public class SpringAutoObjectConverterTestSuite
{
	@Autowired
	protected AutoObjectConverter converter;

	@Test
	public void testConvertSrcNull() throws Exception
	{
		ExampleDO src = new ExampleDO();
		ExampleDomainObject dst = new ExampleDomainObject();
		
		src.setProperty1("setProperty1");
		src.setProperty2("setProperty2");
		src.setProperty3("setProperty3");
		
		this.converter.convert(null, dst);
		
		Assert.assertEquals("setProperty1", src.getProperty1());
		Assert.assertEquals("setProperty2", src.getProperty2());
		Assert.assertEquals("setProperty3", src.getProperty3());
		Assert.assertNull(dst.getProperty1());
		Assert.assertNull(dst.getProperty2());
		Assert.assertNull(dst.getProperty3());
	}

	@Test
	public void testConvertDstNull() throws Exception
	{
		ExampleDO src = new ExampleDO();
		ExampleDomainObject dst = new ExampleDomainObject();
		
		src.setProperty1("setProperty1");
		src.setProperty2("setProperty2");
		src.setProperty3("setProperty3");
		
		this.converter.convert(src, null);
		
		Assert.assertEquals("setProperty1", src.getProperty1());
		Assert.assertEquals("setProperty2", src.getProperty2());
		Assert.assertEquals("setProperty3", src.getProperty3());
		Assert.assertNull(dst.getProperty1());
		Assert.assertNull(dst.getProperty2());
		Assert.assertNull(dst.getProperty3());
	}

	@Test
	public void testConvertDataObjToDomainObj() throws Exception
	{
		ExampleDO src = new ExampleDO();
		ExampleDomainObject dst = new ExampleDomainObject();
		
		src.setProperty1("setProperty1");
		src.setProperty2("setProperty2");
		src.setProperty3("setProperty3");
		
		this.converter.convert(src, dst);

		Assert.assertEquals("setProperty1", src.getProperty1());
		Assert.assertEquals("setProperty2", src.getProperty2());
		Assert.assertEquals("setProperty3", src.getProperty3());
		Assert.assertEquals("setProperty1", dst.getProperty1());
		Assert.assertEquals("setProperty2", dst.getProperty2());
		Assert.assertNotEquals("setProperty3", dst.getProperty3());
	}

	@Test
	public void testConvertDomainObjToDataObj() throws Exception
	{
		ExampleDomainObject src = new ExampleDomainObject();
		ExampleDO dst = new ExampleDO();
		
		src.setProperty1("setProperty1");
		src.setProperty2("setProperty2");
		src.setProperty3("setProperty3");
		
		this.converter.convert(src, dst);

		Assert.assertEquals("setProperty1", src.getProperty1());
		Assert.assertEquals("setProperty2", src.getProperty2());
		Assert.assertEquals("setProperty3", src.getProperty3());
		Assert.assertEquals("setProperty1", dst.getProperty1());
		Assert.assertEquals("setProperty2", dst.getProperty2());
		Assert.assertNotEquals("setProperty3", dst.getProperty3());
	}
}