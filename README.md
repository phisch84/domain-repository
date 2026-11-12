# Domain Repository 3 – repositories and Units of Work for Java

A small library for the domain layer: **repositories** create, provide and track the **entities** of an application, and a
**Unit of Work** writes their changes to a storage. The storage is accessed through a DAO (Data Access Object) of the
[DAO 4](https://github.com/phisch84/dao) library, which stores each entity as a **data object**.

> **Status:** version 3.0.0 is under development and not released yet. This page describes the contract of 3.0.0.
> The Javadoc of each type is the complete reference; this page is the entry point.

## Dependencies

The artifact is published to GitHub Packages (`https://maven.pkg.github.com/phisch84/domain-repository`), together with a
Javadoc jar (`javadoc` classifier). It needs `dao4` and one of its storages:

```xml
<dependency>
    <groupId>com.schoste.ddd.domain</groupId>
    <artifactId>domain-repository3</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- a storage of the DAO 4 library, e.g. serialized files (or dao4-hibernate for a database) -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4-fs-serialization</artifactId>
    <version>4.0.0</version>
</dependency>
```

The major version is part of both the artifact id and the package name (`com.schoste.ddd.domain.v3`), so version 3 can be used
side by side with the older versions (`domain-repository` 1.x and 2.x, which share the package `….domain.v1`) while a project
migrates; see [Migrating from Domain Repository 1.x and 2.x](#migrating-from-domain-repository-1x-and-2x).

The library is compiled for Java 8 and doesn't need Spring: every class can be created with `new`. Spring can still create and
inject repositories, entities and converters (override the methods listed under [Writing a repository](#writing-a-repository)).

## Terms

| Term | Meaning |
|---|---|
| **Entity** | An object of the domain, e.g. a customer. Extends `BasicEntity<ID>`; has an id and a state. |
| **Data object** | How an entity is stored. Extends `DataObject<ID>` of `dao4`; loaded and saved by a DAO. |
| **Repository** | Manages the entities of one class: `GenericRepository<ID, E, DO>`, implemented by extending `GenericRepositoryImpl`. |
| **Unit of Work** | Writes the changes recorded by its repositories to their DAOs on `commit()`, or discards them on `rollback()`: `UnitOfWorkImpl`. |
| **Virtual id** | The id of a new entity which hasn't been saved yet. It comes from the DAO; saving replaces it with the **actual id**. |

## Core rules

- **Repositories never write.** `add`, `setModified` and `remove` only record a change; a Unit of Work writes it on `commit()`.
- **An entity's state** tells what happens on commit: `Detached` (not managed) → `add` → `Added` → commit → `Unchanged`
  → `setModified` → `Modified` → commit → `Unchanged` → `remove` → `Deleted` → commit → `Detached`.
- **One instance per entity.** As long as an entity is referenced somewhere, the get-methods return that same instance (the
  repository keeps it in a cache of weak references). The cache takes precedence over the storage: a cached entity isn't
  reloaded by itself. `refresh()` and `refresh(entity)` update cached entities from the storage.
- **Entities are only equal to themselves**, since their id changes on commit. Compare them by id with `isSameEntityAs(other)`.
- **Deleted = flagged or missing.** An entity whose data object is flagged deleted or doesn't exist anymore isn't provided.
- **Streams must be closed.** `get(Collection)` and `getAll(boolean)` return lazy streams; close them, e.g. with try-with-resources.
- **Change tracking:** `getAll(true)` provides the known entities and the ones whose data objects changed since its last call.
  Each repository tracks this on its own, so repositories sharing a DAO don't affect each other.
- **A commit isn't a transaction.** If it fails part-way, the changes written so far stay written.
- **Closing a Unit of Work rolls back** what hasn't been committed; a rollback makes the repositories forget the changed
  entities, so the next get loads them from the storage again.
- **Null arguments** are rejected with `IllegalArgumentException`.

## Writing a repository

The only method a repository has to implement is `getDataAccessObject()`. The entity's setters annotated with `@AutoSet` are
set from the data object, and the data object's setters annotated with `@AutoSet` (of `dao4`) are set from the entity:

```java
public class Customer extends BasicEntity<Long>
{
    private String name;
    private String email;

    public String getName() { return this.name; }

    @AutoSet                                    // set from CustomerDO.getName()
    public void setName(String name) { this.name = name; }

    public String getEmail() { return this.email; }

    @AutoSet(methodName = "getEmailAddress")    // set from CustomerDO.getEmailAddress()
    public void setEmail(String email) { this.email = email; }
}

public class CustomerDO extends DataObject<Long>   // DataObject and AutoSet of dao4
{
    private static final long serialVersionUID = 1L;

    private String name;
    private String emailAddress;

    public String getName() { return this.name; }

    @AutoSet                                    // set from Customer.getName()
    public void setName(String name) { this.name = name; }

    public String getEmailAddress() { return this.emailAddress; }

    @AutoSet(methodName = "getEmail")           // set from Customer.getEmail()
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
}

public class CustomerRepository extends GenericRepositoryImpl<Long, Customer, CustomerDO>
{
    private final DAO<Long, CustomerDO> dao;

    public CustomerRepository(DAO<Long, CustomerDO> dao) { this.dao = dao; }

    @Override
    protected DAO<Long, CustomerDO> getDataAccessObject() { return this.dao; }

    // optional: a typed factory method
    public Customer createCustomer(String name) throws DomainException
    {
        Customer customer = this.createEntity();

        customer.setName(name);

        return customer;
    }
}
```

Everything else has a default which a repository may override:

| Method | Default | Override e.g. to |
|---|---|---|
| `doCreateEntity(Class, Object...)` | the entity's no-argument constructor | create entities by Spring or with constructor arguments (passed by `createEntity(Object...)`) |
| `getAutoObjectConverter()` | an `AutoObjectConverterImpl` | get the classes named by `@AutoSet(clazz = …)` from Spring |
| `doCompleteEntity(DO, E)`, `doCompleteDataObject(E, DO)` | nothing | copy values which aren't annotated with `@AutoSet` |
| `createEntityLoader(…)` | a `GenericEntityLoaderImpl` | (rarely needed) |

The DAO comes from `dao4`; the `CustomerDAO` used below is the serialization DAO from the package documentation of
`com.schoste.ddd.infrastructure.dao.v4.serialization`.

## Using a repository

```java
CustomerRepository customers = new CustomerRepository(new CustomerDAO("/var/data/customers"));
Long id;

try (UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(customers))
{
    Customer customer = customers.createCustomer("Example Ltd.");   // Detached, virtual id
    customer.setEmail("info@example.com");

    customers.add(customer);                                          // Added
    unitOfWork.commit();                                              // saved: actual id, Unchanged

    id = customer.getId();

    customer.setName("Example Inc.");
    customers.setModified(customer);                                  // Modified
    unitOfWork.commit();                                              // saved: Unchanged
}

Customer loaded = customers.get(id);           // the same instance while it is referenced, else loaded from the DAO

try (Stream<Customer> all = customers.getAll(false))                  // streams must be closed
{
    all.forEach(System.out::println);
}

try (UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(customers))
{
    customers.remove(loaded);                                         // Deleted
    unitOfWork.commit();                                              // data object deleted: Detached
}
```

A Unit of Work can also serve several repositories: `new UnitOfWorkImpl(Arrays.asList(customers, orders))` commits them in
this order.

## Migrating from Domain Repository 1.x and 2.x

The old versions (artifact `domain-repository`, package `com.schoste.ddd.domain.v1`, on DAO 2.x/3.x) and version 3 (on DAO 4)
can be on the class path at the same time. Migrate the DAOs as well; the [DAO 4 README](https://github.com/phisch84/dao)
has its own migration section.

**Behavior that changed without a compile error** – check these first:

- **Entities are only equal to themselves.** The old `BasicDomainObject` compared ids in `equals`/`hashCode`. Now, where the
  code means "the same entity", use `isSameEntityAs(other)` (same class and id). Remove own `equals`/`hashCode` overrides
  which use the id: the id of a new entity changes when it is committed, and the repository keeps entities in hash-based
  collections. `equals` may still be overridden for a logical equality, but only on fields which never change.
- **Every new entity gets a unique virtual id from the DAO** (e.g. `-1`, `-2`, …) instead of an id of the repository;
  `isVirtualId(id)` tells whether it has been committed.
- **`getAll` returns a stream which must be closed**, instead of a collection: `try (Stream<E> all = repository.getAll(false)) { … }`.
- **Only registered changes are committed**, as before: tell the repository with `add`, `remove` and `setModified`; setting the
  state of an entity yourself registers nothing.
- **The repository doesn't need Spring anymore.** The old one got the application context and the converter injected; to
  keep creating entities and converters as Spring beans, override `doCreateEntity` and `getAutoObjectConverter` (see
  [Writing a repository](#writing-a-repository)).

**Mapping:**

| Old (`domain.v1`) | New (`domain.v3`) |
|---|---|
| `models.DomainObject`, `models.BasicDomainObject` | `entities.Entity<ID>`, `entities.BasicEntity<ID>` |
| `DomainObject.State` | `Entity.State` |
| `annotations.AutoSet` | `annotations.AutoSet` (same attributes) |
| `services.GenericRepository<T, DO>` | `services.GenericRepository<ID, E, DO>` |
| `services.standard.GenericRepositoryImpl<T, DO>` | `services.standard.GenericRepositoryImpl<ID, E, DO>` |
| `createObject()` | `createEntity()`; customize creation by overriding `doCreateEntity(Class, Object...)` |
| `get(int)` | `get(ID)`; several at once: `get(Collection<ID>)` returning a `Stream` |
| `getAll()` returning a `Collection`, `getAll(Predicate)` | `getAll(boolean recentOnly)` returning a `Stream` (filter it yourself) |
| `reload()` | `refresh()` updates the cached entities from the storage (Unchanged ones in place, Modified ones are skipped, ones whose data object is flagged deleted are forgotten); `reset()` forgets all entities and removes all listeners, including Units of Work (see its Javadoc) |
| `afterAutoConversation(DO, T)` / `afterAutoConversation(T, DO)` | `doCompleteEntity(DO, E)` / `doCompleteDataObject(E, DO)` |
| `getDomainObjectClass()` | `getEntityClass()` |
| `DALException` (from DAO 2.x/3.x) | `DAOException` (from DAO 4); conversion failures in streams: `DataObjectConversionException` |

## Building

```
mvn clean verify
```

Builds with JDK 21 (or newer), targets Java 8, runs all tests and creates the jar, test jar and Javadoc jars.
The build fails on any Javadoc problem, so the documentation stays complete.

## License

MIT, see [LICENSE](LICENSE).
