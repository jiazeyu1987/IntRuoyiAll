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

## Verification Evidence

- RED target-preflight-file-contract: 58 planned target preflight SQL files were missing.
- GREEN app-preflight-regression: 61 passed across target preflight file contracts, manifest metadata, migration policy gate, release preflight plan and manifest validator.
- Scope: no server write, NAS upload, Docker build, Maven package, MinIO operation or production action was performed.
