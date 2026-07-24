# 20260530-edhr-release-coverage-gonogo-binding

## Task Goal

让前端 eDHR release E2E coverage gate 输出机器可读报告，供后端生产 Go/No-Go 门禁读取。发布链不能只依赖泛化的 `ciEvidence.e2eGates.status=passed`；必须证明当前 eDHR release coverage matrix 已完整通过，并且 full real E2E 未运行时不得被报告成真实全量 PASS。

本前端任务只负责生成和验证 coverage report，不写真实密码、不执行生产发布、不创建 mock 数据、不修改业务页面。

## Scope

- `scripts/edhr-release-e2e-coverage-gate.mjs`
- `scripts/edhr-release-e2e-coverage-contract.test.mjs`
- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/`

## BDD Scenarios

- BDD: release check 输出机器报告 -> Given 开发者运行 `node scripts/edhr-release-e2e-coverage-gate.mjs --check --report <file>` / When 覆盖矩阵、脚本和语法检查通过 / Then report JSON 必须包含 schemaVersion、status=passed、mode=check、featureCount、features、checkedScripts、checkedE2eFiles 和 realGateClaimed=false。
- BDD: report 不伪造 full real PASS -> Given 只运行 check gate / When report 写入 / Then report 必须明确 `realGateClaimed=false`，不得把 check-only 证据标成 full real E2E PASS。
- BDD: 失败不写成功 report -> Given 覆盖矩阵缺真实 E2E token / When check gate 失败 / Then report JSON 必须写入 status=failed 与 failures，进程退出非 0。
- BDD: report 参数歧义必须 fail fast -> Given 开发者运行 `--report --unknown` 或 `--report=-x` / When report path 形似 CLI option / Then CLI 必须在参数解析阶段失败，不运行 coverage gate，不写成功 report，不做 fallback。

## Strict TDD Plan

1. RED: 在 report 支持实现前运行 frontend contract test，预期失败于缺少 `--report` 机器报告能力。
2. GREEN: 实现 report 写入与 report schema contract，使 `--check --report <file>` 通过并生成可读 JSON。
3. REVIEWER RED/GREEN: reviewer 指出 `--report` 参数歧义后，补充 `--report --unknown` / `--report=-x` 合同测试并收紧参数解析为 fail-fast。
4. REGRESSION: 运行 release coverage contract、release check、node syntax 和 `git diff --check`。

## Current Status

- status: completed
- backend paired task: `ruoyi-vue-pro/doc/tasks/20260530-edhr-release-coverage-gonogo-binding/`
- completed work:
  - Added `--report <path>` and `--report=<path>` parsing for the eDHR release coverage gate.
  - Added UTF-8 JSON report writing with `schemaVersion`, `generatedAt`, `mode`, `status`, `command`, `featureCount`, `features`, `checkedScripts`, `checkedE2eFiles`, `failures`, and `realGateClaimed`.
  - Kept check-mode reports explicitly `mode=check`, `status=passed` only when every feature status is `passed`, and `realGateClaimed=false`.
  - Required report path values to be non-empty and not start with `-`.

## Verification Evidence

- GREEN: `node --test scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS, 11 tests.
- GREEN: `node scripts\edhr-release-e2e-coverage-gate.mjs --check --report test-results\edhr-release-coverage\report.json` -> PASS, report regenerated with `schemaVersion=1`, `mode=check`, `status=passed`, `featureCount=10`, `failures=[]`, and `realGateClaimed=false`.
- GREEN: `node --check scripts\edhr-release-e2e-coverage-gate.mjs` -> PASS.
- GREEN: `git diff --check` -> PASS; Git emitted only Windows LF/CRLF normalization warnings for touched files.
- REVIEW PASS: final independent reviewer `Ohm` -> PASS, no blocking issues, no required changes.
- CLOSEOUT PREVIEW: task-closeout-cleanup preview -> BLOCKED for automatic cleanup/merge; no delete candidates. Blockers: branch cannot fast-forward merge into `int_main`, and cleanup does not own pending gate script changes.
- POST-COMMIT CLOSEOUT PREVIEW: task-closeout-cleanup preview -> BLOCKED for automatic cleanup/merge; no delete candidates. Current worktree is clean; remaining blocker is that branch `codex/20260527-edhr-prod-doc-code-subagent-review` cannot fast-forward merge into `int_main`.

## Subagent-Driven Review

- Worker `Pascal` implemented the frontend report contract and report writer in the allowed frontend scope.
- Independent reviewer `Darwin` failed the integrated diff because `--report` could consume option-looking values; worker `Dewey` repaired the frontend CLI parser and added a fail-fast contract test.
- Main reviewer recovered the frontend worktree after it disappeared from the paired worktree folder, recreated the worktree on `codex/20260527-edhr-prod-doc-code-subagent-review`, and restored the reviewed changes.
- Final independent reviewer `Ohm` passed the restored integrated diff.

## Cleanup Keep

- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/task.md`
- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/execution-log.md`
