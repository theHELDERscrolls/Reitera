# Contributing to Reitera

Thank you for your interest in Reitera. This document explains how the project is structured and how to contribute.

## Branching model

- `main` — production branch. Only receives merges from `develop` when a new version is ready to deploy.
- `develop` — integration branch. All feature work targets this branch.
- Feature branches — named `feature/<issue-number>-<short-slug>` (e.g. `feature/42-keyboard-shortcuts`).
- Chore / fix branches — named `chore/<slug>` or `fix/<issue-number>-<slug>`.

```
feature/42-keyboard-shortcuts ──► develop ──► main
```

## Commit style

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add keyboard shortcut for card flip
fix: correct FSRS stability calculation on relearn cards
docs: add API endpoint docs for /study
chore: update Angular to 21.1
refactor: extract card rating logic into service
```

Use lowercase, imperative mood, no trailing period.

## Pull request flow

1. Create a branch from `develop`.
2. Open a PR targeting `develop`.
3. Link the issue it closes (`Closes #42`) in the PR description.
4. Keep PRs focused — one feature or fix per PR.

## Local development

See the [README](README.md#local-development) for the three-step setup.

## Running tests and lint

```bash
# Frontend
cd frontend
ng test    # unit tests (Vitest + jsdom)
ng lint    # ESLint

# Backend
cd backend/reitera-backend
mvn test   # JUnit tests
```

## Questions or issues

Open a [GitHub issue](../../issues) or contact [manuhelderruiz@gmail.com](mailto:manuhelderruiz@gmail.com).
