/**
 * The exceptions of repositories and Units of Work.
 *
 * Methods throw {@link com.schoste.ddd.domain.v3.exceptions.DomainException} (or a subclass) for failures of the domain layer,
 * re-throw {@code DAOException} of the {@code dao4} library for failures of a DAO, and throw {@link IllegalArgumentException}
 * and {@link IllegalStateException} as documented by each method.
 * Streams returned by repositories can't throw checked exceptions; they throw
 * {@link com.schoste.ddd.domain.v3.exceptions.DataObjectConversionException} and {@code DAORuntimeException} instead.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.domain.v3.exceptions;
