package com.schoste.ddd.domain.v2.services.standard;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.domain.v2.models.ExampleDomainObject;
import com.schoste.ddd.domain.v2.services.GenericRepository;

/**
 * Example interface to a repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface DomainObjectRepository extends GenericRepository<Long, ExampleDomainObject, ExampleDO>
{

}
