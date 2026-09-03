# Contributing to popn.gg

Thank you for contributing. Please keep changes focused and discuss large API,
database, or architecture changes in an issue before implementation.

## Development flow

1. Create a branch from `develop` using `feature/**`, `hotfix/**`, or `chore/**`.
2. Implement the change and add tests appropriate to its risk.
3. Run `./gradlew test` locally. MySQL-specific tests require Docker.
4. Open a pull request into `develop` and complete the pull request checklist.
5. Address review feedback. Maintainers promote a deployed and verified
   `develop` revision to `main` through a separate release pull request.
   Release pull requests from `develop` to `main` must use **Create a merge commit**;
   the `main` ruleset rejects squash and rebase merges so the long-lived branch history
   remains connected. Feature and hotfix pull requests into `develop` may still be squashed.

Do not open ordinary feature pull requests directly against `main`.

## Repository safety

- Never commit passwords, tokens, private keys, production data, or `.env` files.
- Use test-only values in examples and automated tests.
- Keep database migrations forward-only and compatible with the current `main`
  application so a candidate deployment can safely roll back.
- Do not include copyrighted jacket artwork, game assets, or data that you do
  not have permission to redistribute.

## Code and tests

- Follow the existing module boundaries and surrounding Java style.
- Add regression tests for bug fixes and behavior tests for new functionality.
- Keep the changed-line coverage at or above the CI threshold.
- Document changes to public APIs, environment variables, migrations, and
  operational procedures.

By submitting a contribution, you agree that it is licensed under the Apache
License 2.0 and that you have the right to submit it.
