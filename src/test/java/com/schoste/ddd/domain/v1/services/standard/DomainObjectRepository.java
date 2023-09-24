package com.schoste.ddd.domain.v1.services.standard;

import com.schoste.ddd.domain.v1.models.ExampleDomainObject;
import com.schoste.ddd.domain.v1.services.GenericRepository;
import com.schoste.ddd.infrastructure.dal.v2.models.ExampleDO;

/**
 * Example interface to a repository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public interface DomainObjectRepository extends GenericRepository<ExampleDomainObject, ExampleDO>
{

}
