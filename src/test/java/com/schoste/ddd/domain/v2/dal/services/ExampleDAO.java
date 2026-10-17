package com.schoste.ddd.domain.v2.dal.services;

import com.schoste.ddd.domain.v2.dal.models.ExampleDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Interface to the example data access object used in the DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface ExampleDAO extends GenericDataAccessObject<Long, ExampleDO>
{

}
