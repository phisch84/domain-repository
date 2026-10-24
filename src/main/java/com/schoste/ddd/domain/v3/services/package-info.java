/**
 * The interfaces of repositories and Units of Work.
 *
 * A repository ({@link com.schoste.ddd.domain.v3.services.GenericRepository}) creates entities, provides them from its cache or
 * loads them from a DAO (Data Access Object), and records which entities are added, modified or removed.
 * A Unit of Work ({@link com.schoste.ddd.domain.v3.services.UnitOfWork}) writes these changes to the DAOs on commit, or discards
 * them on rollback. Their standard implementations are in the package {@code com.schoste.ddd.domain.v3.services.standard}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.domain.v3.services;
