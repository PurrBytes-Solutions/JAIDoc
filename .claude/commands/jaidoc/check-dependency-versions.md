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
- The **Status Legend** — the exact, deterministic criteria for `✅ Up to date`, `📈 Update`, and `⚠️ Review` (including
  the concrete triggers that justify a `⚠️ Review` warning).
- The **Release Notes & Migration References** mapping — which source repository, tag format, and extra reference
  (wiki / changelog / migration guide) to link for each declared dependency.

**Do not duplicate or guess these — reuse them exactly as documented.** If `DEPENDENCIES.md` and these instructions ever
disagree, `DEPENDENCIES.md` wins.

### Step 1 — Collect versioned items strictly from `pom.xml`

Read `pom.xml` and collect **only** what is actually declared there. Do **not** infer, assume, or add any dependency
that is not explicitly present in the file:

1. **The parent POM** (`<parent>` block, e.g. `spring-boot-starter-parent`) — groupId, artifactId, version. Its
   `<version>` is **always** a literal value and must **never** be flagged as "missing a property" — Maven resolves the
   parent before the local `<properties>` are available, so a `${property}` reference cannot be used there. Still
   include it in Step 3/4 to compare it against Sonatype Central, just skip it entirely in Step 2.
2. **Every property in `<properties>`** that holds a dependency version (e.g. `onnxruntime.version`,
   `openapi.version`, `commons-compress.version`, `hibernate-search.version`, `spring-ai.version`,
   `spring-cloud.version`) — map each one back to the `groupId:artifactId` it is used for (via `${property}`
   references in `<dependencies>` or `<dependencyManagement>`).
3. **Every `<dependency>` (in `<dependencies>` and `<dependencyManagement>`) that has an explicit `<version>` tag**,
   whether it references a property (`${...}`) or contains a literal version string.

Do not include dependencies without a `<version>` tag (those are managed by `spring-boot-starter-parent` and are out of
scope for this check, per `DEPENDENCIES.md`).

### Step 2 — Detect hardcoded versions missing a property

For every dependency found in Step 1 — **excluding the `<parent>` block** (see the note in Step 1, item 1: its literal
version is a Maven requirement, not an oversight) — check whether its `<version>` is a literal value (e.g.
`<version>1.2.3</version>`) instead of a property reference (`<version>${xxx.version}</version>`).

- If a **literal version** is found, flag it and suggest extracting it into a new property in `<properties>`, proposing
  a name following the existing convention (e.g. `<artifactId>.version`) and showing the exact before/after XML snippet.
- Do not modify `pom.xml` — only report the finding and the suggestion.

### Step 3 — Query the latest version for each item

For each groupId:artifactId collected in Step 1, run the `curl` command documented in `documentation/DEPENDENCIES.md`
to fetch the latest version from Sonatype Central. Reuse the command verbatim, substituting `<groupId>` and
`<artifactId>`.

### Step 4 — Assign a status to each item

For every item collected in Step 1, assign **exactly one** status by comparing the *Current* (`pom.xml`) version with
the *Latest* (Sonatype Central) version, applying the **Status Legend** from `documentation/DEPENDENCIES.md` verbatim:

- `✅ Up to date` — *Current* equals *Latest*.
- `📈 Update` — *Current* is older than *Latest* (a newer stable version exists).
- `⚠️ Review` — the result is not a clean, comparable stable upgrade. This is the **warning** state. Never emit it
  without a reason: it fires **only** for one of the documented triggers (query failed / no result, latest is a
  pre-release or milestone, the version is managed by a BOM or parent, or the bump is a **major** version change).
  Always append a short note naming *which* trigger fired, so the warning is never unexplained.

Do not invent statuses or thresholds beyond the legend — if the legend and these instructions disagree, the legend wins.

### Step 5 — Attach release notes / migration references

For every item flagged `📈 Update` — and for every `⚠️ Review` caused by a major version bump — build a reference link
using the **Release Notes & Migration References** mapping in `documentation/DEPENDENCIES.md`. Do **not** invent URLs:

1. Look up the dependency's source repository and tag format in the mapping table.
2. Build the release-notes link for the *Latest* version (e.g.
   `https://github.com/<owner>/<repo>/releases/tag/v<X.Y.Z>`), respecting the per-dependency tag format (Apache
   `rel/...`, Hibernate `.Final` without `v`, etc.).
3. Add the extra reference (wiki / changelog / migration guide) when the mapping lists one — this is what helps assess
   the migration effort. For Spring Boot, include the wiki release-notes entry (e.g.
   `https://github.com/spring-projects/spring-boot/wiki`) in addition to the tag link.
4. **Optionally** (best-effort only) pull a one-line highlight from the GitHub Releases API as documented in
   `DEPENDENCIES.md`. If the call fails, is rate-limited, or the tag format differs, **fall back to printing just the
   link** — never block the report on it.

Items that are `✅ Up to date` do not need a reference link.

### Step 6 — Report results

Produce the comparison table using the **exact format** from `documentation/DEPENDENCIES.md` (note the
`Release notes / migration` column):

| Dependency     | Current (pom.xml) | Latest on Sonatype Central | Status                                | Release notes / migration |
|----------------|-------------------|----------------------------|---------------------------------------|---------------------------|
| **artifactId** | X.Y.Z             | A.B.C                      | ✅ Up to date / 📈 Update / ⚠️ Review | link(s) or —              |

Include one row per item collected in Step 1. Fill the `Release notes / migration` column with the link (s) from Step 5
for `📈 Update` / major `⚠️ Review` rows, and `—` otherwise. For any `⚠️ Review` row, put the trigger note either in the
Status cell or in a footnote directly under the table.

After the table, add a **"Versions without property"** section listing any literal `<version>` found in Step 2, each
with the suggested property name and the before/after XML snippet. If none were found, state that explicitly.

### Rules & guardrails

- Work exclusively with what is declared in `pom.xml` — never assume transitive or undeclared dependencies.
- Do not edit `pom.xml`; this command is read-only/reporting.
- If a Sonatype Central query fails or returns no result, report it as ⚠️ Review with a short note instead of guessing a
  version.
- Never emit a `⚠️ Review` warning without stating which documented trigger caused it — an unexplained warning is a bug.
- Never fabricate a release-notes or migration URL. Build it strictly from the mapping in `DEPENDENCIES.md`; if the tag
  is uncertain, link the repository's `/releases` page or its wiki/changelog instead.
