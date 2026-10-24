package com.schoste.ddd.domain.v3.models;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.schoste.ddd.domain.v3.entities.BasicEntity;
import com.schoste.ddd.domain.v3.entities.Entity;

/**
 * Asserts how entities are compared, for ExampleDomainObject explicitly and BasicEntity implicitly:
 * equals uses identity, and {@link Entity#isSameEntityAs(Entity)} compares class and id.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class ExampleDomainObjectTest
{
	/**
	 * Another entity class, to compare entities of different classes with the same id
	 */
	protected static class OtherEntity extends BasicEntity<Long>
	{
	}

	/**
	 * Creates an example entity with the given id
	 *
	 * @param id the id of the entity
	 * @return the new entity
	 */
	protected static ExampleDomainObject entityWithId(Long id)
	{
		ExampleDomainObject entity = new ExampleDomainObject();

		entity.setId(id);

		return entity;
	}

	/**
	 * E1: an entity is equal to itself.
	 */
	@Test
	public void equals_sameInstance_isTrue()
	{
		ExampleDomainObject entity = entityWithId(1L);

		Assert.assertTrue(entity.equals(entity));
	}

	/**
	 * E2: two instances with the same id aren't equal; equals uses identity.
	 */
	@Test
	public void equals_otherInstanceWithSameId_isFalse()
	{
		Assert.assertFalse(entityWithId(1L).equals(entityWithId(1L)));
	}

	/**
	 * E3: an entity stays in a hash set when its id changes, e.g. from the virtual to the actual id on commit.
	 */
	@Test
	public void hashCode_idChanges_entityStaysInHashSet()
	{
		ExampleDomainObject entity = entityWithId(-1L);
		Set<ExampleDomainObject> entities = new HashSet<>();

		entities.add(entity);
		entity.setId(1L);

		Assert.assertTrue(entities.contains(entity));
		Assert.assertTrue(entities.remove(entity));
	}

	/**
	 * E4: two instances of the same class with the same id are the same entity.
	 */
	@Test
	public void isSameEntityAs_otherInstanceWithSameId_isTrue()
	{
		Assert.assertTrue(entityWithId(1L).isSameEntityAs(entityWithId(1L)));
	}

	/**
	 * E5: entities with different ids aren't the same entity.
	 */
	@Test
	public void isSameEntityAs_differentId_isFalse()
	{
		Assert.assertFalse(entityWithId(1L).isSameEntityAs(entityWithId(2L)));
	}

	/**
	 * E6: entities of different classes aren't the same entity, even with the same id.
	 */
	@Test
	public void isSameEntityAs_otherClassWithSameId_isFalse()
	{
		OtherEntity other = new OtherEntity();

		other.setId(1L);

		Assert.assertFalse(entityWithId(1L).isSameEntityAs(other));
	}

	/**
	 * E7: null isn't the same entity as any entity.
	 */
	@Test
	public void isSameEntityAs_null_isFalse()
	{
		Assert.assertFalse(entityWithId(1L).isSameEntityAs(null));
	}

	/**
	 * E8: entities without an id aren't the same entity, not even the same instance.
	 */
	@Test
	public void isSameEntityAs_noId_isFalse()
	{
		ExampleDomainObject entity = entityWithId(null);

		Assert.assertFalse(entity.isSameEntityAs(entity));
		Assert.assertFalse(entity.isSameEntityAs(entityWithId(null)));
	}
}
