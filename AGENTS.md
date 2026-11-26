# Working on Domain Repository 3

Instructions for AI agents and contributors. What the library does: [README.md](README.md). Why it is the way it is:
[docs/decisions.md](docs/decisions.md) – read it before changing behavior. What changed: [CHANGELOG.md](CHANGELOG.md).
The storage side is the DAO 4 library ([phisch84/dao](https://github.com/phisch84/dao)), which has its own `AGENTS.md`.

## The documentation is part of the product

Users get the artifact from Maven and usually never see the source code. What they read: the **Javadoc jar** (in their IDE),
the **POM** (name, description) and, at most, the README on GitHub. Therefore:

- Every public or protected element has Javadoc; the package documentation (`package-info.java`) and
  `src/main/javadoc/overview.html` are the entry points. The build fails on any Javadoc problem (doclint), also in the test
  jar (except missing comments).
- A change of behavior updates, in the same step: the Javadoc where users read the rule, the README if it mentions it,
  `CHANGELOG.md` (under the version in development), and `docs/decisions.md` if a decision changes.

## Layout

One Maven module, artifact `domain-repository3`, package `com.schoste.ddd.domain.v3`, compiled for Java 8.

| Package | Content |
|---|---|
| `entities` | `Entity`, `BasicEntity`, the entity states |
| `services` | `GenericRepository`, `UnitOfWork`, transactions, listeners, converters (interfaces) |
| `services.standard` | the implementations: `GenericRepositoryImpl`, `UnitOfWorkImpl`, `DomainTransactionManagerImpl`, … |
| `annotations`, `exceptions` | `@AutoSet` (for entity setters), the exceptions |

Tests (`src/test/java`, same package root):

- `services.standard.GenericRepositoryContractTest` checks the repository rules (grid ids such as `G2` in each test's
  Javadoc) on a DAO which records how it is used (`dal.services.TestDataAccessObject`); the unit tests use the mocked DAO
  of `dal.services.mocked`; `DomainTransactionTest` uses a mocked transactional DAO.
- `DomainObjectUnitOfWorkIntegrationTest` runs the Unit of Work end to end with the serialization DAO of `dao4-fs-serialization`.
- `PlainJavaRepositoryTest` proves that everything works without Spring.

## Build and test

- JDK 21, Maven 3.9. **Always build with `clean`**: IDEs (e.g. VS Code) write into `target/classes`, so an incremental build can
  report success for code which doesn't compile.
- `mvn clean install` runs all tests and builds the jar, test jar and Javadoc jars.
- `dao4` comes from GitHub Packages (`https://maven.pkg.github.com/phisch84/dao`, repository id `github`): your
  `~/.m2/settings.xml` needs a `<server>` with the id `github` and a token with `read:packages`.
- Tests go through the public interface. Test names: `method_situation_expectation`, e.g. `add_twin_throwsIllegalArgument`;
  a new rule gets its test first.

## Conventions

- **Line endings are mixed and must be kept per file**: most files are CRLF (Java sources, POM, README and the other
  Markdown files); the workflows and a few Java files are LF. New files take the endings of their neighbors. Git doesn't
  convert them; `sed -i` in Git Bash turns CRLF into LF, so check endings after shell edits.
- Java style: tabs and braces on their own line, `this.` for members; keep the style of the file.
- Dependencies: the artifact depends on nothing but `dao4`. Spring and the storages of `dao4` are test dependencies only.
- Versions: change the version only with `mvn versions:set -DnewVersion=X.Y.Z.RELEASE -DgenerateBackupPoms=false`.
  Release versions end with `.RELEASE`, development versions with `-SNAPSHOT` (see decision 17). The `dao4` version is the
  property `dao4.version`; only released `dao4` versions may be used for a release.

## Releasing

1. On the branch `X.Y.Z`: set the release version (see above), date the version in `CHANGELOG.md`, update the README's
   status line. Build with `mvn clean install`; push; CI must be green.
2. Fast-forward `master` to the branch (`git merge --ff-only X.Y.Z`), so the release is exactly the commit CI tested.
3. On GitHub, create the release with the tag `X.Y.Z.RELEASE` on `master` and the version's changelog section as notes, and
   publish it (not as draft): the workflow `maven-publish.yml` builds, tests and deploys the artifact to GitHub Packages.
4. GitHub Packages refuses to overwrite a version: if the deploy fails after publishing, delete the package version before
   publishing again. The version stays at the release afterwards (no new `-SNAPSHOT`).

## Working with the owner

- Don't stage or commit; the owner reviews the diff and commits. Use plain `mv`/`rm`, not `git mv`/`git rm`.
- Work in small steps and summarize each one for review.
- Other projects depend on `domain-repository3`: breaking changes need a new major version (decision 15).
