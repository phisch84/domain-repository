package com.schoste.ddd.domain.v3.dal.services;

import com.schoste.ddd.domain.v3.dal.models.ExampleDO;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Interface to the example data access object used in the DomainObjectRepository
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public interface ExampleDAO extends DAO<Long, ExampleDO>
{

}
