package com.schoste.ddd.domain.v1.services.standard;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the change log used by the Unit of Work (UoW)
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
class RepositoryChangeLog implements Serializable
{
	private static final long serialVersionUID = 3753863234753947084L;
	
	// TODO: Existing objects when the UoW was created for rolling back	
	private Set<Object> objectsToUpdate = new HashSet<Object>();
	private Set<Object> objectsToAdd = new HashSet<Object>();
	private Set<Object> objectsToRemove = new HashSet<Object>();
	
	public Set<Object> getObjectsToRemove() 
	{
		return objectsToRemove;
	}
	
	public void setObjectsToRemove(Set<Object> objectsToRemove) 
	{
		this.objectsToRemove = objectsToRemove;
	}
	
	public Set<Object> getObjectsToAdd() 
	{
		return objectsToAdd;
	}
	
	public void setObjectsToAdd(Set<Object> objectsToAdd) 
	{
		this.objectsToAdd = objectsToAdd;
	}

	public Set<Object> getObjectsToUpdate() 
	{
		return objectsToUpdate;
	}
	
	public void setObjectsToUpdate(Set<Object> objectsToUpdate) 
	{
		this.objectsToUpdate = objectsToUpdate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((objectsToAdd == null) ? 0 : objectsToAdd.hashCode());
		result = prime * result + ((objectsToRemove == null) ? 0 : objectsToRemove.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		RepositoryChangeLog other = (RepositoryChangeLog) obj;
		
		if (objectsToAdd == null) 
		{
			if (other.objectsToAdd != null) return false;
		} else if (!objectsToAdd.equals(other.objectsToAdd))
			return false;
		if (objectsToRemove == null) {
			if (other.objectsToRemove != null)
				return false;
		} else if (!objectsToRemove.equals(other.objectsToRemove))
			return false;
		return true;
	}
	
}
