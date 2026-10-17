package com.schoste.ddd.domain.v1.services.standard;

import com.schoste.ddd.domain.v1.dal.models.ExampleDO;
import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.GenericRepository;

/**
 * Example interface to a repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface DomainObjectRepository extends GenericRepository<Long, ExampleDomainObject, ExampleDO>
{

}
