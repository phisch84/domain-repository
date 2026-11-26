# Domain Repository 3 – repositories and Units of Work for Java

A small library for the domain layer: **repositories** create, provide and track the **entities** of an application, and a
**Unit of Work** writes their changes to a storage. The storage is accessed through a DAO (Data Access Object) of the
[DAO 4](https://github.com/phisch84/dao) library, which stores each entity as a **data object**.

> **Status:** version 3.0.0 is under development and not released yet. This page describes the contract of 3.0.0;
> what changed between the versions is in the [changelog](CHANGELOG.md).
> The Javadoc of each type is the complete reference; this page is the entry point.

## Dependencies

The artifact is published to GitHub Packages (`https://maven.pkg.github.com/phisch84/domain-repository`), together with a
Javadoc jar (`javadoc` classifier). It needs `dao4` (brought along) and one of its storages:

```xml
<dependency>
    <groupId>com.schoste.ddd.domain</groupId>
    <artifactId>domain-repository3</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>

<!-- a storage of the DAO 4 library, e.g. serialized files, or a database via dao4-hibernate (Hibernate 5.6)
     or dao4-hibernate-jakarta (Hibernate 6.6 and later) -->
<dependency>
    <groupId>com.schoste.ddd.infrastructure</groupId>
    <artifactId>dao4-fs-serialization</artifactId>
    <version>4.1.0.RELEASE</version>
</dependency>
```

Release versions end with `.RELEASE` (e.g. `3.0.0.RELEASE`); they never change. Development versions (`-SNAPSHOT`) aren't
published. Version 3.0.0 is built and tested with `dao4` 4.1.0.

### Getting the artifacts

GitHub Packages needs a login also for public packages. This library and DAO 4 come from two registries, which one token
can read:

1. Create a GitHub personal access token (classic) with the scope `read:packages`.
2. Add it to your Maven settings (`~/.m2/settings.xml`), once for each registry:

   ```xml
   <servers>
       <server>
           <id>github-domain-repository</id>
           <username>YOUR_GITHUB_USER</username>
           <password>YOUR_TOKEN</password>
       </server>
       <server>
           <id>github-dao</id>
           <username>YOUR_GITHUB_USER</username>
           <password>YOUR_TOKEN</password>
       </server>
   </servers>
   ```

3. Add the registries to your project's `pom.xml`, with the same ids:

   ```xml
   <repositories>
       <repository>
           <id>github-domain-repository</id>
           <url>https://maven.pkg.github.com/phisch84/domain-repository</url>
       </repository>
       <repository>
           <id>github-dao</id>
           <url>https://maven.pkg.github.com/phisch84/dao</url>
       </repository>
   </repositories>
   ```

The Javadoc jar is the reference documentation: IDEs show it when they download it (e.g. Eclipse and VS Code with
"download Javadoc" enabled, or `mvn dependency:resolve -Dclassifier=javadoc`). Start with its overview.

The major version is part of both the artifact id and the package name (`com.schoste.ddd.domain.v3`), so version 3 can be used
side by side with the older versions (`domain-repository` 1.x and 2.x, which share the package `….domain.v1`) while a project
migrates; see [Migrating from Domain Repository 1.x and 2.x](#migrating-from-domain-repository-1x-and-2x).

The library is compiled for Java 8, runs on any later Java version and doesn't need Spring: every class can be created with
`new`, so it fits applications with any Spring version (or none). Spring can still create and
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
- **A commit is only a transaction with a transaction manager.** Without one, a commit which fails part-way leaves the changes
  written so far written. See [Transactions](#transactions).
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

## Transactions

A commit writes its changes by several DAO calls, so without a transaction it isn't atomic. A `DomainTransactionManager` wraps
the transactions of the storage (`DAOTransactionManager` of `dao4`, e.g. `HibernateDAOTransactionManager`) and keeps the
repositories consistent with the storage when a transaction is rolled back:

```java
DomainTransactionManager transactions = new DomainTransactionManagerImpl(new HibernateDAOTransactionManager(sessionFactory));
```

**One atomic commit:** a Unit of Work with a transaction manager stores all changes of a commit or none. If the commit fails,
it acts like `rollback()` for all its changes, also for the ones it had written already.

```java
UnitOfWorkImpl unitOfWork = new UnitOfWorkImpl(Arrays.asList(customers, orders), transactions);
// or: unitOfWork.setTransactionManager(transactions), e.g. as a property of a Spring bean
```

**Several commits in one transaction:** e.g. when a child needs the actual id of its parent, which it only gets by a commit.

```java
try (DomainTransaction transaction = transactions.begin())
{
    customers.add(customer);
    unitOfWork.commit();                    // customer gets its actual id; the storage keeps the change open

    order.setCustomerId(customer.getId());
    orders.add(order);
    unitOfWork.commit();

    transaction.commit();                   // stores both commits; without it, closing rolls both back
}
```

- A transaction belongs to the thread which began it; all commits of Units of Work in that thread join it, also of Units of
  Work without a transaction manager. Reads of the repositories see the changes made in it.
- If the transaction is rolled back, or a commit in it fails, the storage discards all changes of the transaction, and the
  repositories forget the entities committed in it, as `rollback()` does: they become `Detached`, and getting them again provides
  new instances with the state of the storage (none for entities which had been added).
- `begin()` on a thread which has a transaction already joins it (an *inner* transaction): only the outermost one commits;
  rolling back an inner one forgets its entities and marks the whole transaction for rollback. So a method can use a
  transaction regardless of whether its caller has one.
- Listeners of the Units of Work are notified on each commit, also if the transaction is rolled back later.
- The storage must support transactions: the Hibernate DAOs of `dao4` do, the file system and serialization DAOs don't.

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

Contributors (people and AI agents) start with [AGENTS.md](AGENTS.md): layout, conventions and how to test; the reasons
behind the design are in [docs/decisions.md](docs/decisions.md).

```
mvn clean verify
```

Builds with JDK 21 (or newer), targets Java 8, runs all tests and creates the jar, test jar and Javadoc jars.
The build fails on any Javadoc problem, so the documentation stays complete.

## License

MIT, see [LICENSE](LICENSE).
