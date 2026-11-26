# Changelog

What changed between the versions of Domain Repository 3, for users of the artifact. Versions without a release date aren't
released yet. Release versions end with `.RELEASE`.

## 3.0.0 (2026-11-26)

The first version of Domain Repository 3: a new artifact (`domain-repository3`) and package (`com.schoste.ddd.domain.v3`),
which can be used side by side with `domain-repository` 1.x and 2.x. It is built on DAO 4 (`dao4`) instead of DAO 2.x/3.x.

**Changed compared to 1.x and 2.x** (details and a mapping of the names: [Migrating from Domain Repository 1.x and 2.x](README.md#migrating-from-domain-repository-1x-and-2x))

- Entities are only equal to themselves; `isSameEntityAs(other)` compares by class and id.
- Every new entity gets a unique virtual id from the DAO.
- `get(Collection)` and `getAll(boolean)` return lazy streams which must be closed.
- `getAll(true)`: each repository tracks the changes of its DAO on its own.
- `refresh()` / `refresh(entity)` update cached entities from the storage; a rollback makes the repositories forget the
  changed entities.
- No Spring needed: entities and converters are created with their no-argument constructors by default; override
  `doCreateEntity` and `getAutoObjectConverter` to create them by Spring.

**Added**

- Transactions: a `DomainTransactionManager` (e.g. `DomainTransactionManagerImpl` around a `DAOTransactionManager` of
  `dao4`) makes a commit of a Unit of Work atomic, and lets several commits form one transaction. After a rollback, the
  repositories forget the entities committed in the transaction.

**Tested with** `dao4` 4.1.0.RELEASE; compiled for Java 8, built with JDK 21.
