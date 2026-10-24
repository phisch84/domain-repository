/**
 * The standard implementations of repositories and Units of Work.
 *
 * To create a repository, extend {@link com.schoste.ddd.domain.v3.services.standard.GenericRepositoryImpl} and implement its only
 * abstract method, {@code getDataAccessObject()}. Use {@link com.schoste.ddd.domain.v3.services.standard.UnitOfWorkImpl} to commit
 * the changes of one or more repositories. Spring is optional: all classes can be created with {@code new}.
 *
 * Example for the entity {@code Customer}, its data object {@code CustomerDO} and a DAO of the {@code dao4} library:
 * <pre>
 * public class CustomerRepository extends GenericRepositoryImpl&lt;Long, Customer, CustomerDO&gt;
 * {
 *     private final DAO&lt;Long, CustomerDO&gt; dao;
 *
 *     public CustomerRepository(DAO&lt;Long, CustomerDO&gt; dao) { this.dao = dao; }
 *
 *     protected DAO&lt;Long, CustomerDO&gt; getDataAccessObject() { return this.dao; }
 * }
 *
 * CustomerRepository customers = new CustomerRepository(customerDAO);
 *
 * try (UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(customers))
 * {
 *     Customer customer = customers.createEntity();
 *
 *     customer.setName("Jane");
 *     customers.add(customer);
 *     unitOfWork.commit(); // saves the customer; it gets its actual id
 * }
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.domain.v3.services.standard;
