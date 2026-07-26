# Verification Report

## Summary

- Result: PARTIAL PASS; `int_main` 前端真实页面中 `芋道源码/admin` 已能在源选择框看到 `生产工单`。
- Date: 2026-07-26
- Frontend entry: `http://127.0.0.1:8081`
- Identity label: `芋道源码/admin`
- Runtime blocker: full field-selection E2E is blocked because backend port `48081` is currently served by another worktree, not `E:\IntRuoyi`.

## Passed Checks

- `node tests\e2e\mes\batch-record-cell-link-static.spec.js` -> PASS
- `node --check tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs` -> PASS
- `pnpm ts:check` -> PASS
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
- `int-main source selector visibility E2E` -> PASS, visible option `生产工单`, `mesWriteRequests=0`
- `git diff --check -- task-owned paths` -> PASS
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS

## Blocked Check

- `node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs` against `http://127.0.0.1:8081` -> FAIL before field matrix assertion because `workbench-context` does not return `sourceFields`.
- Root cause: `48081` is listening from `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726`, whose backend code does not include the production-work-order source field contract.
- Next action: stop or replace that conflicting `48081` process with an `E:\IntRuoyi` backend build, then rerun the full readonly E2E.
