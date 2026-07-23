# Security Policy

Thank you for helping keep Reitera and its users safe. This document explains
which versions receive security fixes and how to report a vulnerability
responsibly.

## Supported Versions

Reitera follows [Semantic Versioning](https://semver.org/). Security fixes are
applied to the latest released version only. Older releases are not patched —
please upgrade before reporting an issue against an outdated version.

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |
| < 1.0   | :x:                |

The production branch is `main`; active development happens on `develop`. The
publicly deployed instance always tracks the latest `1.x` release.

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
pull requests, or discussions.** A public report exposes the flaw to everyone
before a fix is available.

Instead, use one of these private channels:

1. **GitHub Private Vulnerability Reporting (preferred).** Go to the
   [**Security** tab](https://github.com/theHELDERscrolls/Reitera/security) of
   this repository and click **"Report a vulnerability"**. This keeps the
   discussion private and tracked in one place.
2. **Email.** Write to **manuhelderruiz@gmail.com** with the subject line
   `[SECURITY] Reitera`.

You will receive an acknowledgement that the report was received. From there,
the report will be triaged, a fix prepared, and you will be kept informed of the
progress.

### What to include

A good report helps confirm and fix the issue quickly. Where possible, include:

- The type of issue (e.g. authentication bypass, IDOR, XSS, SQL injection,
  sensitive data exposure).
- The affected component — frontend (Angular), backend (Spring Boot API),
  database, or deployment.
- The affected version, commit, or endpoint (e.g. `POST /api/v1/...`).
- Step-by-step instructions to reproduce the issue.
- A proof-of-concept, request/response sample, or minimal payload if available.
- The potential impact (what an attacker could achieve).

The more detail you provide, the faster the issue can be validated and resolved.

## What to Expect

Reitera is maintained by a single developer, so response times are best-effort
rather than a formal SLA:

- **Acknowledgement:** within **5 business days** of your report.
- **Assessment:** an initial evaluation (confirmed / needs more info / not a
  vulnerability) as soon as the report has been reproduced.
- **Resolution:** valid vulnerabilities are prioritised over other work; the
  timeline depends on severity and complexity.
- **Updates:** you will be kept informed at each stage, and notified once a fix
  is released.

If you do not receive an acknowledgement within the stated window, please send a
polite follow-up in case the original message was missed.

## Scope

**In scope** — issues in the code maintained in this repository:

- The Angular frontend (`frontend/`).
- The Spring Boot backend and its REST API (`backend/reitera-backend/`).
- The database schema and access control logic (`init.sql`, ownership checks).
- Authentication and session handling (JWT access/refresh tokens, rate
  limiting).

**Out of scope:**

- Vulnerabilities in third-party hosting platforms (Vercel, Render, Supabase) —
  report those to the respective provider.
- Vulnerabilities in third-party dependencies that already have a public
  advisory and a pending Dependabot update (though you are welcome to flag them).
- Reports generated solely by automated scanners without a demonstrable,
  exploitable impact.
- Denial-of-service, social engineering, physical attacks, or spam.

## Disclosure Policy

Please practise **coordinated disclosure**: give a reasonable amount of time for
a fix to be prepared and released before disclosing the issue publicly. Once a
fix is available, credit will gladly be given to reporters who wish to be
acknowledged.

## Safe Harbor

Any good-faith security research conducted in accordance with this policy is
considered authorised. Actions taken in good faith to identify and report a
vulnerability — without violating user privacy, destroying data, or degrading
the service for others — will not lead to legal action. If in doubt about
whether a specific test is acceptable, ask first via one of the private channels
above.
