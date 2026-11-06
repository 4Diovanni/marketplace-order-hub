# CLAUDE.md · marketplace-order-hub

Rules for this repository. They apply to every person or agent working here.

## Branches

Every branch name says what is being done. No generic or auto-generated names (`claude/...`, `patch-1`, `test`, `my-branch`).

Format: `<type>/<issue number>-<short-description>`

| Type | When to use |
|---|---|
| `feat` | new feature |
| `fix` | bug fix |
| `refactor` | code change with no behavior change |
| `test` | tests only |
| `docs` | documentation only |
| `chore` | setup, build, CI, dependencies |

- Description in Portuguese, lowercase, no accents, words separated by hyphens, about 5 words at most.
- One branch per issue (or per small group of related issues). When grouping, use the first issue number.
- Examples: `chore/1-setup-projeto`, `feat/5-crud-produtos`, `feat/9-adapters-marketplaces`, `fix/23-estoque-negativo`.

## Pull requests

- One PR per branch, small and complete: code, tests, and README updates when relevant.
- The PR body closes its issues (`Closes #n`) and says how it was tested.
- `main` only receives code through a PR with green CI.

### Maintenance PRs (`fix`, production bugs, hotfixes)

Maintenance PRs must be **minimal and designed for the lowest possible impact on production**.

- Change only what the fix needs. No refactoring, renaming, formatting, or "while I'm here" improvements in the same PR; open a separate `refactor` issue for those.
- Never change public API contracts (routes, request/response fields, status codes) or the database schema unless the fix requires it. If it does, say so explicitly in the PR.
- Prefer the fix that touches the fewest files and the smallest blast radius, even if a broader fix looks cleaner.
- Add a test that reproduces the bug and fails without the fix.
- The PR body states: the root cause, what changed, the risk, and how to roll back.

## Project

- Java 21 · Spring Boot 3 · Maven · MySQL 8 · Flyway.
- Language: code and this file in English; README, issues, PRs, and user-facing messages in Portuguese.
- Packages are organized **by layer** under `com.giovanni.orderhub`. Every new class goes into its layer's folder:

| Package | What goes there |
|---|---|
| `controller` | `@RestController`: receives the request, validates the DTO, calls the service |
| `service` | business rules and transactions |
| `repository` | Spring Data JPA interfaces |
| `model` | JPA entities and domain models |
| `enums` | every enum (`Channel`, `OrderStatus`, `ErrorCode`, ...) |
| `dto` | API input/output records, one subpackage per resource (`dto/product`, `dto/order`, `dto/error`) |
| `exception` | `ApiException` (base) + subfolders by subject: `handler`, `product`, `order`, `integration` |
| `integration` | marketplace adapters, one subpackage per channel (`integration/alfashop`, ...) |
| `config` | `@Configuration` and `@ConfigurationProperties` classes |

- Tests mirror the package of the class under test (`service/OrderServiceTest`, `controller/OrderApiIntegrationTest`).
- Schema changes only through Flyway migrations (`ddl-auto: validate`).

## Coding practices

### Design and organization
- **Thin controllers:** receive the request, validate the DTO, call the service. Business rules live in the service (or in the entity, when the rule belongs to the entity).
- **Entities never leave through the API.** Input and output use DTOs (`record`), with explicit conversion.
- **Single responsibility:** a class or method does one thing. A method over ~20 lines, or with `if` nested more than 2 levels, is a signal to extract.
- **Constructor injection** with `final` fields. No field `@Autowired`.
- **No magic numbers or strings:** use constants or enums with names that explain them.
- **Strategy instead of `if/switch` per channel:** a new marketplace is a new class, without touching what already works.

### Readability and formatting
Code is read far more often than it is written. If a person has to stop and decode it, it is wrong, even if it works.

- **Guard clauses instead of nested `if`:** handle the exit case first and return early. No `if` inside `if` inside `if`.

  ```java
  // bad
  if (product != null) {
      if (product.hasStockFor(qty)) {
          if (qty > 0) { ... }
      }
  }

  // good
  if (product == null) {
      throw new ProductNotFoundException(sku);
  }
  if (qty <= 0) {
      throw new InvalidQuantityException(qty);
  }
  if (!product.hasStockFor(qty)) {
      return OrderStatus.PENDING_STOCK;
  }
  ...
  ```
- **No `else` after `return` or `throw`.**
- **The public method tells the story; private methods hold the details.** A reader should understand the flow from the public method alone (e.g. `OrderService.receive`: sum quantities → find products → reserve stock → save).
- **One level of detail per method**, ~20 lines at most. Beyond that, extract a method whose name explains it.
- **Streams only when they make it clearer.** Simple `filter`/`map`/`toList` is fine. If it needs `Collectors.toMap` with a merge function, `reduce`, or a multi-line lambda, use a `for` loop.
- **No nested ternaries** and no "clever" one-liners.
- **Variables named after what they hold:** `quantityBySku`, `productBySku`, not `map`, `m2`, `aux`.
- **Formatting:** 4 spaces, lines up to 120 characters, braces always (even on one-line `if`), one blank line between logical blocks, no wildcard imports.

### Naming
- Names describe intent: `reserveStock`, `findPendingOrders`, not `process`, `handle2`, `data`.
- Classes are nouns, methods are verbs, booleans start with `is`/`has`/`can`.
- No abbreviations that only the author understands.

### Data and rules
- Money is always `BigDecimal`, never `double`.
- Dates are `Instant` (UTC) in the database and the API.
- `Optional` only as a return type; never as a field or parameter. Never call `Optional.get()` without checking.
- `@Transactional` on the service method that must be atomic, never on the controller.
- Validate input at the edge (Bean Validation on the DTO) and business rules in the service.
- Time-based logic receives a `Clock` (injected), so tests can control time without `Thread.sleep`.

### Errors and API messages
- Every business exception extends `ApiException` and carries an `ErrorCode` from `enums/ErrorCode`. New error = new `ErrorCode` entry + exception in the right `exception/<subject>` folder.
- Every error response is RFC 7807 (`ProblemDetail`) with:
  - `code`: stable `ErrorCode` name, for the front end to decide what to do;
  - `message`: seller-facing text in Portuguese, ready to display;
  - `detail`: technical detail for support and logs.
- Unexpected errors return `500` with `INTERNAL_ERROR` and the standard message. Never leak stack traces or internal details in the response.
- Never swallow exceptions (empty `catch` or just `printStackTrace`). Handle it or rethrow with context.
- Correct HTTP status codes: `201` created, `200` ok, `204` no body, `400` invalid input, `404` not found, `409` conflict, `422` business rule violated, `429` rate limit (with `Retry-After`).

### Tests
- Every PR with a new rule includes a test for that rule.
- Unit tests (JUnit 5 + Mockito) for service rules and mappers; integration tests (Testcontainers + MySQL) for end-to-end flows.
- Test names state the scenario and the expected result: `shouldSetPendingStockWhenAnyItemIsMissing`.
- Arrange / Act / Assert structure, one behavior per test.
- Never disable or delete a test to make CI pass.

### Logging
- SLF4J (`private static final Logger log = LoggerFactory.getLogger(...)`), never `System.out`.
- Log relevant business events (order received, duplicate order, insufficient stock, rate limit hit) with the order id and channel.
- Never log full personal data or secrets.

### Commits
- Imperative mood, in Portuguese, saying what changes: `Adiciona baixa de estoque tudo-ou-nada`.
- Small, meaningful commits; no `wip`, `ajustes`, `fix2` in the final PR history.

## Fixed rule

Portfolio project with a domain inspired by marketplace integration. **No code, payload, rule, or data from any real company** (Magis5, Allcare, or any other). Marketplaces, JSON formats, and data are made up.
