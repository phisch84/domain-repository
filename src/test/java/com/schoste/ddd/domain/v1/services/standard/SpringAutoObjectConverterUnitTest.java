package com.schoste.ddd.domain.v1.services.standard;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit test configuration / implementation of the test suite for the SpringAutoObjectConverter
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringAutoObjectConverterUnitTest extends SpringAutoObjectConverterTestSuite
{

}
