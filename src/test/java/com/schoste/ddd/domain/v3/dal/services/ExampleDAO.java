package com.schoste.ddd.domain.v3.dal.services;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Interface to the example data access object used in the DomainObjectRepository
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface ExampleDAO extends DAO<Long, ExampleDO>
{

}
