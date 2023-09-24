package com.schoste.ddd.domain.v1.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.domain.v1.models.DomainObject.State;
import com.schoste.ddd.domain.v1.services.standard.DomainObjectRepository;
import com.schoste.ddd.testing.v1.GenericObjectTest;

/**
 * Asserts that equality is correctly determined for ExampleDomainObject explicitly
 * and BasicDomainObject implicitly.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class ExampleDomainObjectTest extends GenericObjectTest<ExampleDomainObject>
{
	@Autowired
	private DomainObjectRepository repository;

	@Override
	protected Map<String, Collection<ExampleDomainObject>> getEqualObjects()
	{
		Map<String, Collection<ExampleDomainObject>> equalObjectsSet = new HashMap<String, Collection<ExampleDomainObject>>();
		
		try
		{
			Collection<ExampleDomainObject> equalObjects = new ArrayList<ExampleDomainObject>();
	
			ExampleDomainObject equalObject1 = this.repository.createObject();
			ExampleDomainObject equalObject2 = this.repository.createObject();
			
			equalObject1.setId(1);
			equalObject1.setState(State.Added);
			equalObject2.setId(1);
			equalObject2.setState(State.Detached);
			
			equalObjects.add(equalObject1);
			equalObjects.add(equalObject2);
			
			equalObjectsSet.put("equalObjects", equalObjects);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
		
		return equalObjectsSet;
	}

	@Override
	protected Map<String, Collection<ExampleDomainObject>> getNotEqualObjects()
	{
		Map<String, Collection<ExampleDomainObject>> notEqualObjectsSet = new HashMap<String, Collection<ExampleDomainObject>>();

		try
		{
			Collection<ExampleDomainObject> notEqualObjects = new ArrayList<ExampleDomainObject>();
	
			ExampleDomainObject notEqualObject1 = this.repository.createObject();
			ExampleDomainObject notEqualObject2 = this.repository.createObject();
			
			notEqualObject1.setId(1);
			notEqualObject1.setState(State.Detached);
			notEqualObject2.setId(2);
			notEqualObject2.setState(State.Detached);
			
			notEqualObjects.add(notEqualObject1);
			notEqualObjects.add(notEqualObject2);
			
			notEqualObjectsSet.put("notEqualObjects", notEqualObjects);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}

		return notEqualObjectsSet;
	}

}
