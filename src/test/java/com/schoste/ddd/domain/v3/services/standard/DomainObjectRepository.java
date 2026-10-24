package com.schoste.ddd.domain.v3.services.standard;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.domain.v3.models.ExampleDomainObject;
import com.schoste.ddd.domain.v3.services.GenericRepository;

/**
 * Example interface to a repository
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DomainObjectRepository extends GenericRepository<Long, ExampleDomainObject, ExampleDO>
{

}
