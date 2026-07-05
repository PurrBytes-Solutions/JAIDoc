---
allowed-tools: Read, Bash(curl:*), Bash(jq:*)
argument-hint: (no arguments)
description: Check pom.xml dependency versions against Sonatype Central and flag hardcoded versions missing a property
---

## Context

- Dependency version management reference: `documentation/DEPENDENCIES.md`
- Project POM: `pom.xml`

## Task

Verify the versions of the dependencies **declared in `pom.xml`** against the latest versions published on Sonatype
Central, following the method already documented in `documentation/DEPENDENCIES.md`.

### Step 0 — Read the reference first

Read `documentation/DEPENDENCIES.md` in full before doing anything else. It is the source of truth for:

- The `curl` command used to query Sonatype Central's Solr search API.
- The Markdown table format used to report results.

**Do not duplicate or guess these — reuse them exactly as documented.** If `DEPENDENCIES.md` and these instructions
ever disagree, `DEPENDENCIES.md` wins.

### Step 1 — Collect versioned items strictly from `pom.xml`

Read `pom.xml` and collect **only** what is actually declared there. Do **not** infer, assume, or add any dependency
that is not explicitly present in the file:

1. **The parent POM** (`<parent>` block, e.g. `spring-boot-starter-parent`) — groupId, artifactId, version. Its
   `<version>` is **always** a literal value and must **never** be flagged as "missing a property" — Maven resolves
   the parent before the local `<properties>` are available, so a `${property}` reference cannot be used there. Still
   include it in Step 3/4 to compare it against Sonatype Central, just skip it entirely in Step 2.
2. **Every property in `<properties>`** that holds a dependency version (e.g. `onnxruntime.version`,
   `openapi.version`, `commons-compress.version`, `hibernate-search.version`, `spring-ai.version`,
   `spring-cloud.version`) — map each one back to the `groupId:artifactId` it is used for (via `${property}`
   references in `<dependencies>` or `<dependencyManagement>`).
3. **Every `<dependency>` (in `<dependencies>` and `<dependencyManagement>`) that has an explicit `<version>` tag**,
   whether it references a property (`${...}`) or contains a literal version string.

Do not include dependencies without a `<version>` tag (those are managed by `spring-boot-starter-parent` and are out
of scope for this check, per `DEPENDENCIES.md`).

### Step 2 — Detect hardcoded versions missing a property

For every dependency found in Step 1 — **excluding the `<parent>` block** (see the note in Step 1, item 1: its literal
version is a Maven requirement, not an oversight) — check whether its `<version>` is a literal value
(e.g. `<version>1.2.3</version>`) instead of a property reference (`<version>${xxx.version}</version>`).

- If a **literal version** is found, flag it and suggest extracting it into a new property in `<properties>`,
  proposing a name following the existing convention (e.g. `<artifactId>.version`) and showing the exact
  before/after XML snippet.
- Do not modify `pom.xml` — only report the finding and the suggestion.

### Step 3 — Query the latest version for each item

For each groupId:artifactId collected in Step 1, run the `curl` command documented in `documentation/DEPENDENCIES.md`
to fetch the latest version from Sonatype Central. Reuse the command verbatim, substituting `<groupId>` and
`<artifactId>`.

### Step 4 — Report results

Produce the comparison table using the **exact format** from `documentation/DEPENDENCIES.md`:

| Dependency     | Current (pom.xml) | Latest on Sonatype Central | Status                               |
|----------------|-------------------|----------------------------|--------------------------------------|
| **artifactId** | X.Y.Z             | A.B.C                      | ✅ Up to date / 📈 Update / ⚠️ Review |

Include one row per item collected in Step 1. After the table, add a **"Versions without property"** section listing any
literal `<version>` found in Step 2, each with the suggested property name and the before/after XML snippet. If none
were found, state that explicitly.

### Rules & guardrails

- Work exclusively with what is declared in `pom.xml` — never assume transitive or undeclared dependencies.
- Do not edit `pom.xml`; this command is read-only/reporting.
- If a Sonatype Central query fails or returns no result, report it as ⚠️ Review with a short note instead of
  guessing a version.
