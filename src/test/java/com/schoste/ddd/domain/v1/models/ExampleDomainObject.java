package com.schoste.ddd.domain.v1.models;

import com.schoste.ddd.domain.v1.annotations.AutoSet;

/**
 * Example implementation of the domain object interface
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDomainObject extends BasicDomainObject implements DomainObject
{
	private String property1;
	private String property2;
	private String property3;
	
	/**
	 * Default constructor of the example domain object
	 */
	public ExampleDomainObject() { super(); }

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
