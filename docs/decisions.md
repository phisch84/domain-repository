# Design decisions

Why Domain Repository 3 is the way it is. The rules themselves are documented in the Javadoc (start with `GenericRepository`
and `UnitOfWork`); this page keeps the reasons, so that nobody "fixes" a deliberate choice. Change a decision only on purpose:
update this page, the Javadoc, the README and the tests together. The decisions of the underlying DAO 4 library are in
[its own docs/decisions.md](https://github.com/phisch84/dao/blob/master/docs/decisions.md).

## Entities and the cache

| # | Decision | Why | So don't … |
|---|---|---|---|
| 1 | **Entities are only equal to themselves** (`BasicEntity` doesn't override `equals`/`hashCode`); `isSameEntityAs` compares class and id. Subclasses may define `equals` on fields which never change. | The id of a new entity changes on commit (virtual → actual), and the repository and the Unit of Work keep entities in hash-based collections. Same reason as in DAO 4. | … override `equals`/`hashCode` with the id. |
| 2 | **One instance per entity**: the repository keeps loaded entities in a cache of weak references; the cache takes precedence over the storage. An id whose entity was garbage collected counts as unknown. | Changes made to an entity in one place are seen everywhere; the storage is only read again when nobody holds the entity. | … reload or replace an entity somebody holds; refreshing is explicit (`refresh`, rollback, `reset`). |
| 3 | **A cached entity is provided even if its data object was deleted in the storage meanwhile.** | The storage is the source of truth, but the repository never changes an entity behind its holder's back; refreshing is explicit. | … drop cached entities silently on reads. |
| 4 | **`get(id)` returns null for an entity whose removal is pending** (removed, not committed yet); `add` on such an entity throws `IllegalStateException`, as on an entity whose deletion was committed. | The entity is gone from the domain's point of view; re-adding it would break the Unit of Work (delete, then save of a missing data object) and lose its state before the removal. Relaxing this later stays compatible. | … let `add` cancel a removal. |
| 5 | **Strict state checks**: `add` only for Detached/Added entities; `remove` of a Deleted entity does nothing; `remove` with another instance of a cached id and `setModified` of an unknown entity throw. | Mistakes in the use of the repository show up where they are made, not as wrong data after a commit. | … relax the checks without a reason from a real use case. |

## Change tracking, streams, Units of Work

| # | Decision | Why | So don't … |
|---|---|---|---|
| 6 | **Each repository tracks the changes of its DAO on its own** (`getAll(true)`); its time stamp advances while the stream is consumed; `reset()` sets it back. | DAO 4's change tracking is stateless, so several repositories on one DAO must not affect each other; what was consumed isn't reported again, even if the stream is closed early. | … keep the tracking time stamp in the DAO or share it between repositories. |
| 7 | **Streams are lazy and must be closed**; failures while consuming are unchecked (`DAORuntimeException`, `DataObjectConversionException`). | Large amounts of data never have to fit into memory (as in DAO 4); a stream can't throw checked exceptions. | … collect results eagerly. |
| 8 | **A rollback forgets**: every entity in the Unit of Work's change log (added, removed and modified) is evicted from the cache and becomes Detached; the next get loads it from the storage. | Only forgetting handles modified entities correctly (their changes would otherwise be saved by the next commit); undoing a removal or a modification in place can't restore the previous state. | … try to undo changes in place. |
| 9 | **One Unit of Work per repository at a time**: every Unit of Work registered with a repository records all of its changes. | Scoping the changes per Unit of Work is only worth it when parallel Units of Work are actually needed. | … use several Units of Work on one repository at the same time. |
| 10 | **A commit is atomic only with a transaction manager**; a rollback of a transaction makes the repositories forget the entities committed in it. | The storage decides whether transactions are possible (Hibernate DAOs: yes, file DAOs: no); the repositories must not keep entities the storage discarded. | … promise atomic commits without a transaction manager. |

## API and versions

| # | Decision | Why | So don't … |
|---|---|---|---|
| 11 | **No framework needed**: entities and converters are created with their no-argument constructors by default; `doCreateEntity(Class, Object...)` and `getAutoObjectConverter()` are the hooks for Spring. | The library must work in any application and with any Spring version; Spring is the common case, not a requirement. | … add a compile dependency on Spring. |
| 12 | **`createEntity()` has no arguments in the interface**; repositories whose entities need data offer typed factory methods (e.g. `createCustomer(String name)`). | Typed methods instead of `Object...` arguments; the protected `createEntity(Object...)` stays for constructor arguments, e.g. for Spring's `getBean`. | … add `Object...` parameters to the interface. |
| 13 | **Two `@AutoSet` annotations**: the one of this library on entity setters, the one of `dao4` on data object setters. | The infrastructure layer (`dao4`) must never depend on the domain layer. | … merge them into one annotation. |
| 14 | **Class names stay** (`GenericRepository`, `GenericRepositoryImpl`). | `Repository` would clash with Spring Data. | … rename them to `Repository`. |
| 15 | **The major version is part of the artifact id and the package** (`domain-repository3`, `….domain.v3`). | Versions 1/2 and 3 can be used side by side while a project migrates (as in DAO 4). | … publish a new major version under the old artifact id. |
| 16 | **Java 8**, compiled with `release 8`. | Same baseline as DAO 4: existing Java 8 applications keep working; newer Java versions run it unchanged. | … use Java 9+ APIs. |
| 17 | **Release versions end with `.RELEASE`** and never change; development versions are `-SNAPSHOT`. A release is tagged on `master`. | A `.RELEASE` version exists exactly once; Maven sorts it like the plain version, after the snapshot. | … rebuild or re-publish a release version. |
