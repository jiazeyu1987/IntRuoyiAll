# CI/CD Environment Evidence

## Environment

- Target runtime profile: local `int_main`.
- Target backend port: `48081`.
- Runtime workspace: `E:\IntRuoyi`.
- Build source: clean worktree created from latest `origin/int_main`.

## Build And Deploy Contract

- Build only from a clean, recorded remote commit.
- Run the focused MES regression before packaging.
- Package with the standard Maven reactor path.
- Record build and deployed Jar SHA-256 values and require exact equality.
- Stop only the listener whose command line is confirmed as the current `E:\IntRuoyi` backend.
- Do not change `application-local.yaml`, database credentials, service port, or data source.

## Rollback

- Preserve the previous runtime Jar with its SHA-256 before replacement.
- No automatic fallback is performed. If the new backend cannot become healthy, fail fast and report the preserved rollback artifact and exact failure.

## Secrets

- No secret values are added or recorded.
- Existing local profile configuration is reused without printing credentials.

## Current Status

in_progress
