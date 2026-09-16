# one-button-app-release-20260911

## Task Goal

Complete the application-side release workflow requirements for the one-button app-release task: standard release packages are `app-release` / without data, and every migration that depends on target data must have an executable read-only target preflight before Maven, Docker, NAS upload, or deployment.

## Milestones

1. [completed] P1 application release contract and source role convergence.
2. [completed] P2 ReleaseWorkflow runtime orchestration.
3. [in_progress] P3 target read-only migration preflight coverage and release package gates: local contract coverage complete; real target execution still pending.
4. [pending] P4 product UI verification.
5. [pending] P5 regression, release evidence, integration, and closeout.

## Expected Verification

- Target-preflight file set matches every non-schema or target-bound planned migration.
- Every target-preflight SQL file is one read-only `SELECT` or `WITH` statement with an exact success marker.
- Migration metadata, preflight files, and release package gates reject missing or unsafe checks before build or deploy side effects.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；把历史 target data 依赖变成构建前可执行只读门禁。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

R79 已完成 build-release、manifest validation 和 NAS READY，但测试服 publish-test 在版本切换前失败：route-snapshot backfill 报告为 `status=READY`、`blockerCount=0`，SSH 进程仍返回 1。根因是 `MesProRouteVersionSnapshotMigrationRunner` 使用 `SpringApplication.exit(...)` 聚合无关的 `ExitCodeGenerator`，覆盖了命令本身的 READY 退出码。应用仓已完成最小修复和 6 个清洁 Maven 定向测试；下一步提交应用修复与任务证据，使用全新 `release-20260916-one-button-app-r80` 重建并继续测试服 publish-test，R79 不复用。

## Verification Evidence

- RED target-preflight-file-contract: 58 planned target preflight SQL files were missing.
- GREEN app-preflight-regression: 61 passed across target preflight file contracts, manifest metadata, migration policy gate, release preflight plan and manifest validator.
- GREEN app-infra-c07-regression: after aligning runtime-control test fixtures to the `manifest.json`/`packageId` and tested-operation evidence contract, and mechanically refreshing stale codegen snapshots from the existing generator, `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` passed with 546 tests, 0 failures, 0 errors, 10 skipped.
- GREEN r28-balloon-cleanup-contract: 11 local cleanup/preflight tests and 40 release migration regression tests passed; testing the revised preflight against the real test database returned `TARGET_PREFLIGHT_PASS` for the already-normalized 49-process baseline without applying business-data changes.
- GREEN scheduler-heartbeat-and-jimu-preflight: backend workflow scheduler now reconciles terminal low-level operations without user polling, refreshes workflow heartbeat from fresh operation-log mtime during long running builds, and only performs fail-closed recovery when both workflow heartbeat and operation log are stale. Target preflight for `20260829_mes_old_form_template_binding_switch` now covers Jimu layout semantics and idempotency. Targeted Python tests passed 12/12; release workflow Maven tests passed 19/19.
- GREEN r73-application-tooling-contract-sync: application publish script now accepts and preserves extended migration metadata (`requiresTargetPreflight/applyOrder/sessionProfile/approvedHook/resultAssertion`), excludes rollback/evidence-only SQL from required migrations, and matches the current backend Docker runtime args contract; targeted 5 passed, R73 discovered-test group 135 passed, adjacent release regression 58 passed, PowerShell AST PASS.
- R79 route-snapshot runner exit-code contract: real publish evidence recorded a READY route report with a non-zero SSH exit because `SpringApplication.exit(...)` aggregated an unrelated `ExitCodeGenerator`; the runner now propagates the command result directly and keeps non-READY results fail-fast. Clean targeted Maven verification passed 6 tests with 0 failures/errors.
- BLOCKER r61-interrupted-package: local `release-20260915-one-button-app-r61` has required-sql and preflight evidence only; it lacks `manifest.json` and image tar, with no active R61 build/release process observed. It is not reusable and must be replaced by a new releaseTag after committing this fix.
- Scope: no server write, NAS upload, Docker build, Maven package, MinIO operation or production action was performed.
