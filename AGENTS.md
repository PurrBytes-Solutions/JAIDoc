# Guiding coding agents

Guidelines for AI agents working in this repository. This file is for **rules and conventions** — what the code should
do, how it should be structured, and what to avoid. How something works (architecture decisions, design choices,
step-by-step procedures) belongs in the `documentation/` directory, not here.

These rules apply to every task in this project unless explicitly overridden. Bias: caution over speed on non-trivial
work. Use judgment on trivial tasks.

---

## Behavioral Rules

### Rule 1 — Think Before Coding

State assumptions explicitly. If uncertain, ask rather than guess. Push back when a simpler approach exists. Stop when
confused.

**Read before writing.** Before adding code, read exports, immediate callers, and shared utilities. Read each file only
once — do not re-open a file whose content you already have in context unless it was modified after you read it. If
unsure why code is structured a certain way, ask.

**Stop when stuck.** After two rounds of investigation (reading, searching, verifying), stop and proceed with planning
or implementation. Do not add a third round of "just one more check." Stop as soon as the goal is met — do not keep
exploring "just in case." Do not retry failing tool calls in a loop. If a tool fails after 2 attempts, fall back to a
shell command or ask for clarification. Ask when stuck rather than looping over the same files indefinitely.

### Rule 2 — Simplicity First

Minimum code that solves the problem. Nothing speculative. No features beyond what was asked. No abstractions for
single-use code.

### Rule 3 — Surgical Changes

Touch only what you must. Clean up only your own mess. Don't "improve" adjacent code, comments, or formatting. Match
existing style.

### Rule 4 — Goal-Driven Execution

Define success criteria. Loop until verified. Don't follow steps. Define success and iterate independently.

### Rule 5 — Know Your Constraints

Respect token and time limits. If approaching a limit, summarize and start fresh. Surface the breach. Do not repeat the
same failed approach — try a different angle or ask for clarification.

### Rule 6 — Prefer MCP Tools Over Shell Commands

When an MCP server provides a tool for a task, use it first. If an MCP tool or shell command fails after 2 attempts,
fall back to asking for clarification. Do not retry a failing tool indefinitely.

### Rule 7 — Checkpoint After Every Significant Step

Summarize what was done, what's verified, what's left. Don't continue from a state you can't describe back. Stop and
restate.

### Rule 8 — Fail Loud

"Completed" is wrong if anything was skipped silently.
"Tests pass" is wrong if any fail. Default to surfacing uncertainty, not hiding it. Report findings even when they
contradict your hypothesis. If a verification step returns an empty or unexpected result, surface it explicitly rather
than assuming success.

---

## Language Rules

- **Code is always written in English.** All identifiers (class names, method names, variable names, function names,
  constants, package names, etc.), as well as inline comments and commit messages, must be in English.
- **Documentation is always written in English.** All documentation, including Javadoc/KDoc, README files, design docs,
  and any other written documentation, must be in English.

---

## Code Style Rules

- **Do not use Fully Qualified Class Names in code.** Always use `import` statements and refer to classes by their
  simple name. For example, use `ObjectMapper` instead of `com.fasterxml.jackson.databind.ObjectMapper` in the code
  body. **Exception:** when two classes share the same simple name (e.g., `java.util.Date` and `java.sql.Date`), import
  the one that is used more frequently and use the Fully Qualified Class Name only for the less frequently used one.
- **Follow Java naming conventions.** Use `PascalCase` for classes, `camelCase` for methods and variables, and
  `UPPER_SNAKE_CASE` for constants.
- **Do not leave commented-out code.** If code is not used, remove it. Git history preserves it. Stop after one pass —
  do not re-check files for commented-out code after the initial cleanup.
- **Do not use `@SuppressWarnings` without justification.** If a warning needs to be suppressed, add a comment
  explaining why.
- **Prefer `Optional` over `null`.** Use `Optional` as a return type when a value may be absent, instead of returning
  `null`. Exception: null is acceptable for third-party API contracts, collection types, or when the API explicitly
  requires it. Document the reason in a comment.
- **Prefer unchecked exceptions for programming errors.** Do not throw checked exceptions unless required by a framework
  contract. Use `IllegalArgumentException`, `IllegalStateException`, or custom runtime exceptions. Document the
  precondition with a comment when throwing.

---

## Configuration / Spring Boot Rules

- **Do not hardcode configuration values.** Use `application.yaml` or environment variables. Do not put URLs, ports,
  credentials, or configuration values directly in Java code. **Exception:** API base URLs (e.g., `ADOPTIUM_BASE`) are
  acceptable constants — they are not secrets and do not change per deployment. Define such constants in a dedicated
  configuration class (e.g., `@ConfigurationProperties` or a `@Configuration` class with `@Value` fields), never as
  standalone constants in service or controller classes.
- **Use constructor injection for production code.** Field injection with `@Autowired` is acceptable in test classes
  that do not load a Spring context (pure unit tests). All integration tests should use constructor injection like
  production code.
- **Use Lombok `@Slf4j` for logging.** Do not use `System.out.println`, `java.util.logging`, or manual `Logger` fields.

---

## Security Rules

- **Never commit secrets or credentials.** Do not include API keys, passwords, tokens, or certificates in the
  repository. Use environment variables or a secrets manager.
- **Do not log sensitive data.** Avoid printing personal information, tokens, or credentials in logs (via `@Slf4j`).
- **Validate all external input.** Sanitize and validate user inputs, request parameters, file uploads, and any data
  originating from outside the application boundary before processing.
- **Use parameterized queries.** Never concatenate user input into SQL strings. Always use prepared statements or
  JPA/Hibernate parameter binding.
- **Prevent XSS.** Encode output when rendering user-controlled data in HTML contexts. Rely on Spring's default encoding
  and template engine escaping; do not bypass it.
- **Enforce CSRF protection.** Do not disable Spring Security's CSRF protection without a documented reason and an
  alternative mitigation (e.g., same-site cookies + custom token).

---

## Documentation Rules

- **Javadoc must be in English.** All Javadoc comments must be written in English, matching the English-only policy for
  code and documentation.
- **Document public classes and methods with Javadoc.** All public APIs and methods called from outside the package must
  have Javadoc explaining their purpose, parameters, and return values.
- **Keep the `README.md` up to date.** If a change affects how the project is configured, installed, or used, update the
  README. Do not leave it as a historical record.

---

## Documentation Maintenance Rules

- **Keep `documentation/STRUCTURE.md` in sync with the code layout.** Whenever you add, remove, or move a package,
  class, or configuration file, update STRUCTURE.md to reflect the change. Do not leave the structure map stale.
- **Keep all files in `documentation/` current.** The documentation directory contains: STRUCTURE.md (project layout),
  DEPENDENCIES.md (version management), AI-MODELS.md (local models and benchmarks), DATABASE.md (JPA entities and
  ingestion flows), DOCLET.md (JSON doclet architecture), JACKSON.md (customizer pattern), JDK-DATA.md (ZIP and JSON
  pipeline), JDK-DISTRIBUTION.md (Adoptium downloader), MCP.md (MCP server setup), SECURITY.md (actuator and logging),
  TEST.md (test conventions), TRANSFORMER.md (ONNX model), and FEATURES.md (feature workspaces). If a change affects the
  architecture, CLI options, output format, configuration, security setup, testing approach, or any other detail
  described in any of these files, update the relevant file in the same session — before moving on. Stale documentation
  is worse than no documentation.
- **Do not duplicate documentation.** If a detail is already well-documented in `documentation/`, do not repeat it in
  code comments or CLAUDE.md. Reference the documentation file instead.

---

## File Reading & Loop Prevention Rules

*This section has been merged into Rule 1 (Think Before Coding). See above.*

---

## Testing Rules

- **Mark test classes with `@Tag`.** Use `BaseTest.TAG_UNIT` for unit tests and `BaseTest.TAG_INTEGRATION` for
  integration tests so the CI pipeline can run only the appropriate type. Never use raw strings — reference the
  constants. See [TEST.md](documentation/TEST.md) for the full test conventions.
- **Follow the given/when/then structure.** Organize each test method in three clear sections: setup (given), the call
  under test (when), and the assertions (then). Use comments or blank lines to separate sections.
- **Use AssertJ for assertions.** Prefer AssertJ over Hamcrest or JUnit assertions. Use descriptive messages with
  `.as("...")` to make failures easy to diagnose.
- **Mock only external dependencies.** Do not mock objects you own. Mock HTTP clients, external services, file I/O, and
  other boundaries — not collaborators within the system under test.
- **Test behavior, not implementation.** Verify that the system produces the correct observable outcome, not that it
  uses a specific internal method or data structure.

---

## Git & Branching Rules

- **Branch from `main`.** Create feature branches with the pattern `feature/<name>` or `fix/<issue>`. Never push
  directly to `main`.
- **Keep commits atomic.** Each commit should represent a single logical change.
  Use [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.
- **Sync before merging.** Rebase or merge `main` into your branch before opening a PR to ensure you're working with the
  latest changes.
- **Resolve conflicts locally.** Don't force-push to resolve conflicts — merge or rebase cleanly, then push.

---

## Dependency Management Rules

- **Check [DEPENDENCIES.md](documentation/DEPENDENCIES.md) before adding new dependencies.** Understand the version
  management methodology and current status.
- **Prefer existing dependencies.** Before adding a new library, check if an existing dependency can fulfill the need.
- **Pin versions explicitly.** Do not rely on dependency management plugins for version resolution without explicit
  version declarations.
- **Review security advisories.** Before upgrading a dependency, check for known security vulnerabilities.

---

## Project References

Deep-dive documentation lives in the `documentation/` directory; feature workspaces live in `features/`. Consult these
before working in the areas they cover.

### Architecture & Structure

- **[Project Structure](documentation/STRUCTURE.md)** — High-level layout, config hierarchy, build output, tech stack
- **[Dependency Versions](documentation/DEPENDENCIES.md)** — Version management methodology, current status, and update
  commands
- **[Feature Workspaces](features/FEATURES.md)** — Per-feature context bundles that inform implementation planning
- **[Jackson Config](documentation/JACKSON.md)** — Customizer pattern, YAML mapper convention
- **[Security Config](documentation/SECURITY.md)** — Actuator restrictions, logging paths

### Data & Distribution

- **[JDK Distribution](documentation/JDK-DISTRIBUTION.md)** — Adoptium distribution downloader, source selection,
  archive handling
- **[JDK Data](documentation/JDK-DATA.md)** — JDK source ZIP and JSON Javadoc data pipeline
- **[Database](documentation/DATABASE.md)** — JPA entities, Hibernate Search, kNN mapping, ingestion/search flows
- **[Doclet](documentation/DOCLET.md)** — JSON doclet architecture, CLI options, output format, chunking

### AI & Models

- **[Transformer Model](onnx/TRANSFORMER.md)** — ONNX embedding model, URI scheme requirements, model selection
- **[AI Models](documentation/AI-MODELS.md)** — Local AI models, quantization, and performance benchmarks

### Tooling & Tests

- **[MCP Server](documentation/MCP.md)** — MCP server setup and JetBrains adapter
- **[Test Architecture](documentation/TEST.md)** — Test class hierarchy, tags, JsonMapper setup

### History & Decisions

- **[Black Book](blackbook/BLACKBOOK.md)** — AI dev log: thoughts, decisions, gotchas
