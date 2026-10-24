/**
 * Entities: the objects of the domain which a repository manages
 * ({@link com.schoste.ddd.domain.v3.entities.Entity}, usually extending {@link com.schoste.ddd.domain.v3.entities.BasicEntity}).
 *
 * An entity has an id and a state. The id is virtual while the entity hasn't been persisted, and the id of its data object afterwards.
 * The state tells what the Unit of Work does with the entity on commit (see {@link com.schoste.ddd.domain.v3.entities.Entity.State});
 * the repository manages it.
 *
 * Entities are only equal to themselves, since their id changes on commit. Compare them by id with
 * {@link com.schoste.ddd.domain.v3.entities.Entity#isSameEntityAs(com.schoste.ddd.domain.v3.entities.Entity)}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.domain.v3.entities;
