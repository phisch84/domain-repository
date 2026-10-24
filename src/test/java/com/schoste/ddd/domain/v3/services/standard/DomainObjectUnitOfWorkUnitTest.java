package com.schoste.ddd.domain.v3.services.standard;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit of Work tests for the DomainObjectRepository on the mocked DAO
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class DomainObjectUnitOfWorkUnitTest extends DomainObjectUnitOfWorkTestSuite
{

}
