# Contributing Guide

Thank you for considering contributing to the `api-standard-spring-boot-starter`. This library is a core infrastructure component, meaning stability and backward compatibility are our highest priorities.

---

## 1. Local Setup

### Prerequisites
*   **Java 21** (LTS) or higher.
*   **Maven 3.8+**

### Architecture
The project is divided into three main modules:
*   `api-standard-core`: Contains the interfaces (`ProblemDetailEnricher`), `ErrorCode`, and the `ProblemDetailService`. (No Web/MVC dependencies).
*   `api-standard-web`: Contains the `@RestControllerAdvice`, `GlobalExceptionHandler`, and filters. (Depends on Spring Web).
*   `api-standard-starter`: The auto-configuration module. Contains `spring.factories` / `AutoConfiguration.imports` that wire everything together when the dependency is added.

### Build and Test
To compile the project and run all unit tests:
```bash
mvn clean install
```

---

## 2. Testing and Coverage

Due to its role as a cross-cutting concern, we require extremely high reliability.

*   **100% Core Coverage**: Any new `Enricher` or modification to `ProblemDetailService` must be accompanied by unit tests covering positive cases, negative cases, and null/edge bounds.
*   **No Flaky Tests**: Tests must not rely on `Thread.sleep()` or external network calls.

---

## 3. Release Flow and Standards

This repository strictly adheres to automated semantic versioning.

### Conventional Commits
All pull requests and commits must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This allows our CI/CD pipeline to automatically generate changelogs and bump versions.

**Valid Examples:**
*   `feat: add support for WebFlux exception handling` (Will trigger a MINOR bump: 2.x.0)
*   `fix: resolve NullPointerException in TraceIdEnricher` (Will trigger a PATCH bump: 2.0.x)
*   `feat!: completely redesign the ErrorCode enum` (The `!` indicates a BREAKING CHANGE, triggering a MAJOR bump: 3.0.0)

### Pull Request Process
1.  Create a feature branch from `main`.
2.  Write your code and add necessary tests.
3.  Ensure `mvn clean verify` passes locally.
4.  Submit a Pull Request.
5.  Wait for the CI pipeline (GitHub Actions) to pass the build and SonarQube quality gates.
6.  Once approved by a maintainer, it will be Squash-Merged into `main`.
