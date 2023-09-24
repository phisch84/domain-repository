package com.schoste.ddd.infrastructure.dal.v2.models;

import com.schoste.ddd.infrastructure.dal.v2.annotations.AutoSet;

/**
 * Example data object used in the DomainObjectRepository
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDO extends GenericDataObject
{
	private String property1;
	private String property2;
	private String property3;

	private static final long serialVersionUID = 7411239831182467495L;

	/**
	 * Gets the example property
	 * 
	 * @return a String
	 */
	public String getProperty1() 
	{
		return property1;
	}

	/**
	 * Sets the example property
	 * 
	 * @param property1 a String
	 */
	@AutoSet
	public void setProperty1(String property1) 
	{
		this.property1 = property1;
	}
	
	/**
	 * Gets the example property
	 * 
	 * @return a String
	 */
	public String getProperty2() 
	{
		return property2;
	}

	/**
	 * Sets the example property
	 * 
	 * @param property2 a String
	 */
	@AutoSet
	public void setProperty2(String property2) 
	{
		this.property2 = property2;
	}

	/**
	 * Gets the example property
	 * 
	 * @return a String
	 */
	public String getProperty3() 
	{
		return property3;
	}

	/**
	 * Sets the example property
	 * 
	 * @param property3 a String
	 */
	public void setProperty3(String property3) 
	{
		this.property3 = property3;
	}
}
