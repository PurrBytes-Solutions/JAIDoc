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
  "com.microsoft.onnxruntime:onnxruntime" \
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

| Dependency     | Current (pom.xml) | Latest on Sonatype Central | Status                               |
|----------------|-------------------|----------------------------|--------------------------------------|
| **artifactId** | X.Y.Z             | A.B.C                      | ✅ Up to date / 📈 Update / ⚠️ Review |

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
