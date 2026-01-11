# Architectural Decisions

## Development and Build Tools

**Docker Compose** - For local development
- Provides isolated, reproducible Postgres instances without requiring local installation, ensuring consistent development environments across all team members.

**Testcontainers** - For tests
- Enables automatic setup and teardown of real Postgres instances during test execution, providing true integration testing without manual infrastructure management.

**Gradle** - For Java build system
- Offers flexible dependency management and build configuration with a concise Groovy/Kotlin DSL, widely adopted in modern Java projects.

**npm** - For React frontend
- Industry-standard package manager for JavaScript/TypeScript projects with the largest ecosystem of frontend libraries and tools.

## Testing Frameworks

**Cucumber-JVM** - For Java backend Gherkin tests
- Mature and widely-adopted BDD framework that integrates seamlessly with JUnit 5 and Testcontainers for executable specifications of API behavior.

**Cucumber.js with Playwright** - For React frontend Gherkin tests
- Combines Gherkin's readable specifications with Playwright's modern browser automation capabilities for reliable end-to-end testing of UI behavior.
