# Verification Report

## Passed

- `IntRuoyiFronted/tests/dcc-dual-version-upload-download-contract.test.cjs`: 4/4 passed.
- `git diff --check`: passed; only line-ending normalization warnings were reported.
- Targeted frontend ESLint for DCC upload/browser/API files and the contract test: passed.
- `scripts\preflight\branch-runtime-port-guard.ps1`: passed for `codex/20260918-001/int_main`, frontend `8082`, backend `48082`.
- High-memory `pnpm exec vue-tsc --noEmit`: no remaining DCC-related errors after fixes; full command still fails on unrelated Form/MES/Workorder pre-existing errors.
- Portable toolchain check: Java 17.0.20.1 and Maven 3.9.11 under `C:\IntRuoyi\.tool-cache` are available for local verification.
- `mvn -pl yudao-module-dcc -am -DskipTests compile`: passed after adding dual download flags to `DccControlledFileVersionHistoryRespVO`.
- Task closeout cleanup preview/apply: passed; no task-local intermediate files required deletion.
- Post-merge `int_main` worktree verification: branch runtime port guard passed, the dual-version contract test passed 4/4, and DCC Maven compile passed.

## Not Run

- Real browser E2E and database migration execution were not run; both remain outside the requested scope.

## Review Notes

- The required read-only upload and optional editable upload are persisted separately.
- Online browsing resolves the read-only artifact only.
- `/download/read-only` and `/download/editable` use independent permissions.
- The legacy `/download` route is restricted to the read-only download permission, so it cannot bypass the split authorization model.
- `GET /dcc/controlled-files/{id}` remains on the query permission, keeping browse/query independent from download permissions.
