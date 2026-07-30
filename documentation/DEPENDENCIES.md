# Dependency Version Management

## Checking Latest Versions

To verify whether a dependency is up to date, query Sonatype Central's Solr search API. This returns the actual latest
version as published, including pre-releases and milestones that may not yet be on Maven Central's metadata XML.

### Method

```bash
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:<groupId>+AND+a:<artifactId>&sort=v+desc&rows=1" | jq '.response.docs[0].v'
```

Replace `<groupId>` and `<artifactId>` with the Maven coordinates. The `+` between them is URL-encoded space.

### Examples — All Project Dependencies

Run each command to get the latest version from Sonatype Central:

```bash
# spring-boot-parent (parent POM)
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.springframework.boot+AND+a:spring-boot-starter-parent&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# onnxruntime
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:com.microsoft.onnxruntime+AND+a:onnxruntime&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# springdoc-openapi-starter-webflux-ui
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.springdoc+AND+a:springdoc-openapi-starter-webflux-ui&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# commons-compress
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.apache.commons+AND+a:commons-compress&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# hibernate-search-bom
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.hibernate.search+AND+a:hibernate-search-bom&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# spring-ai-bom
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.springframework.ai+AND+a:spring-ai-bom&sort=v+desc&rows=1" | jq '.response.docs[0].v'

# spring-cloud-dependencies
curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:org.springframework.cloud+AND+a:spring-cloud-dependencies&sort=v+desc&rows=1" | jq '.response.docs[0].v'
```

### One-Liner — Check Everything at Once

```bash
for dep in \
  "org.springframework.boot:spring-boot-starter-parent" \
  "com.microsoft.onnxruntime:onnxruntime_gpu" \
  "org.springdoc:springdoc-openapi-starter-webflux-ui" \
  "org.apache.commons:commons-compress" \
  "org.hibernate.search:hibernate-search-bom" \
  "org.springframework.ai:spring-ai-bom" \
  "org.springframework.cloud:spring-cloud-dependencies"; do
  IFS=':' read -r g a <<< "$dep"
  latest=$(curl -s "https://central.sonatype.com/solrsearch/select?wt=json&q=g:${g}+AND+a:${a}&sort=v+desc&rows=1" | jq -r '.response.docs[0].v')
  echo "${a}: ${latest}"
done
```

### Generating a Comparison Table

For each dependency, run the curl command and compare against the value in the `<properties>` section of `pom.xml`.
Format as:

| Dependency     | Current (pom.xml) | Latest on Sonatype Central | Status                                | Release notes / migration |
|----------------|-------------------|----------------------------|---------------------------------------|---------------------------|
| **artifactId** | X.Y.Z             | A.B.C                      | ✅ Up to date / 📈 Update / ⚠️ Review | link(s) — see below       |

### Status Legend

The `Status` column is **derived deterministically** by comparing the version in `pom.xml` (*Current*) with the latest
version returned by Sonatype Central (*Latest*). Compare versions numerically, component by component
(`major.minor.patch`), and treat qualifier suffixes (`.Final`, `-RC1`, `-M1`, `-SNAPSHOT`) as lower precedence than the
matching release. Assign exactly one status per row:

| Status        | When it applies                                                                                                                     |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------|
| ✅ Up to date | *Current* equals *Latest* (same version).                                                                                           |
| 📈 Update     | *Current* is **older** than *Latest* (a newer stable version exists). Always attach the release-notes / migration link (see below). |
| ⚠️ Review     | The result is **not a clean, comparable stable upgrade** and a human must look at it (see the triggers below).                      |

**`⚠️ Review` triggers** — use this status (never guess a version) whenever any of the following is true:

- The Sonatype Central query **failed, timed out, or returned no result** for the coordinate.
- The latest published version is a **pre-release / milestone** (`-M`, `-RC`, `-SNAPSHOT`, `-alpha`, `-beta`) while the
  project uses a stable one — upgrading is optional and must be decided manually.
- The version is **managed by a BOM or the parent** and pinning it locally would fight that management.
- The bump is a **major version change** (the leading `major` component increases, e.g. `4.x → 5.0`) — these are almost
  always breaking and require reading the migration guide before updating.

`⚠️ Review` is therefore **not an error by itself**: it means "this row needs a human decision", and the row must
include a short note explaining *which* trigger fired.

### Release Notes & Migration References

Whenever a dependency is flagged **📈 Update** or **⚠️ Review** (major bump), attach a reference link so the reader can
assess the migration effort instead of upgrading blind. Build the links from the source repository, not from Sonatype.

**General convention** — most projects tag releases as `v<version>`, so the release notes live at:

```
https://github.com/<owner>/<repo>/releases/tag/v<version>
```

Some ecosystems use a different tag format; use the per-dependency mapping below and, when the exact tag is uncertain,
fall back to the repository's `/releases` page or its wiki/changelog. The mapping for this project's declared
dependencies:

| Dependency (groupId:artifactId)                       | Source repository                                                                         | Tag format                       | Extra reference                                                                              |
|-------------------------------------------------------|-------------------------------------------------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------------------|
| `org.springframework.boot:spring-boot-starter-parent` | [spring-projects/spring-boot](https://github.com/spring-projects/spring-boot)             | `v<version>`                     | Per-version release notes in the [wiki](https://github.com/spring-projects/spring-boot/wiki) |
| `org.springframework.ai:spring-ai-bom`                | [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)                 | `v<version>`                     | [Reference docs](https://docs.spring.io/spring-ai/reference/)                                |
| `org.springframework.cloud:spring-cloud-dependencies` | [spring-cloud/spring-cloud-release](https://github.com/spring-cloud/spring-cloud-release) | `v<version>`                     | Release-train notes in the [wiki](https://github.com/spring-cloud/spring-cloud-release/wiki) |
| `org.springdoc:springdoc-openapi-starter-webflux-ui`  | [springdoc/springdoc-openapi](https://github.com/springdoc/springdoc-openapi)             | `v<version>`                     | —                                                                                            |
| `org.hibernate.search:hibernate-search-bom`           | [hibernate/hibernate-search](https://github.com/hibernate/hibernate-search)               | `<version>` (no `v`)             | [Migration guides](https://hibernate.org/search/documentation/migrate/)                      |
| `org.hibernate.orm:hibernate-community-dialects`      | [hibernate/hibernate-orm](https://github.com/hibernate/hibernate-orm)                     | `<version>` (no `v`)             | [Migration guides](https://hibernate.org/orm/documentation/migrate/)                         |
| `com.microsoft.onnxruntime:onnxruntime_gpu`           | [microsoft/onnxruntime](https://github.com/microsoft/onnxruntime)                         | `v<version>`                     | —                                                                                            |
| `org.apache.commons:commons-compress`                 | [apache/commons-compress](https://github.com/apache/commons-compress)                     | `rel/commons-compress-<version>` | [Changelog](https://commons.apache.org/proper/commons-compress/changes.html)                 |
| `org.apache.httpcomponents.client5:httpclient5`       | [apache/httpcomponents-client](https://github.com/apache/httpcomponents-client)           | `rel/v<version>`                 | —                                                                                            |
| `org.xerial:sqlite-jdbc`                              | [xerial/sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)                               | `<version>` (no `v`)             | —                                                                                            |
| `org.projectlombok:lombok`                            | [projectlombok/lombok](https://github.com/projectlombok/lombok)                           | `v<version>`                     | [Changelog](https://projectlombok.org/changelog)                                             |
| `tools.jackson.*` (Jackson 3)                         | [FasterXML/jackson](https://github.com/FasterXML/jackson)                                 | `jackson-<version>`              | [Release notes wiki](https://github.com/FasterXML/jackson/wiki/Jackson-Releases)             |

**Example** — Spring Boot `4.1.0`:

- Release notes (tag): <https://github.com/spring-projects/spring-boot/releases/tag/v4.1.0>
- Migration / release notes (wiki): <https://github.com/spring-projects/spring-boot/wiki> → "Spring Boot 4.1 Release
  Notes"

**Optional (best-effort) — fetch a short highlight** instead of only linking. When network access and the GitHub API are
available, the release body can be pulled with:

```bash
curl -s "https://api.github.com/repos/<owner>/<repo>/releases/tags/v<version>" | jq -r '.body'
```

Treat this as best-effort only: if the call fails, is rate-limited, or the tag format differs, **fall back to printing
the link** (the mapping above) — never block the report on it.

### What to Check

**Always verify:**

- All version properties in `<properties>` (the ones with Sonatype comment links)
- Dependencies with explicit `<version>` tags not managed by a BOM or parent

**Check periodically (monthly or on major releases):**

- Dependencies managed by `spring-boot-parent` but not pinned (e.g., `sqlite-jdbc`, `httpclient5`, `lombok`)
- Plugin versions in `<build><plugins>` (versions are inherited from parent)

**Do NOT need separate checks:**

- Artifacts already managed by a BOM import (`spring-cloud-dependencies`, `spring-ai-bom`, `hibernate-search-bom`) —
  their transitive versions are controlled by the BOM
- Dependencies where the version is inherited from `spring-boot-parent` and the parent is up to date
