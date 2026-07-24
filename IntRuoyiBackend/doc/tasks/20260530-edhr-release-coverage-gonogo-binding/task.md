# 20260530-edhr-release-coverage-gonogo-binding

## Task Goal

将前端 eDHR release E2E coverage report 接入后端生产 Go/No-Go 门禁。后端 `validate-edhr-production-go-no-go.ps1` 必须 fail-closed：缺少 report、report 不可读、schema/status/feature 列表不完整、`checkedScripts` 或 `checkedE2eFiles` 缺失/重复/不完整/包含未知项、存在 failures、feature 未 passed、命令不是 eDHR release coverage gate、或 report 声称 check-only 为 full real PASS 时，生产发布判定必须 `NO-GO`。

本任务只读验证发布证据；不得执行 webhook、backup、restore、rollback、生产发布或任何环境修改。

## Scope

- `script/release-readiness/validate-edhr-production-go-no-go.ps1`
- `script/release-readiness/templates/edhr-production-go-no-go.example.json`
- `script/tests/test_edhr_release_ops_acceptance_contract.py`
- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/`

## BDD Scenarios

- BDD: GoNoGo 读取 eDHR coverage report -> Given `ciEvidence.e2eGates.reportPath` 指向前端 release coverage JSON / When validator 运行 / Then report 必须 schema 合法、status=passed、mode=check、featureCount 与 features 数量一致、所有 features status=passed。
- BDD: 泛化 E2E passed 不足以放行 -> Given `ciEvidence.e2eGates.status=passed` 但 report 不是 eDHR release coverage report / When validator 运行 / Then 输出 `decision=NO-GO`。
- BDD: check-only 不伪装 real PASS -> Given report 为 check mode / When validator 运行 / Then 必须接受 `realGateClaimed=false`，但如果 report 声称 full real PASS 则阻塞。
- BDD: release command 必须 canonical -> Given outer command 或 report command 使用 substring、echo、尾随文本、未知参数、option-like `--report`、quoted option-like `--report` 或 shell 元字符 / When validator 校验 / Then 输出 `decision=NO-GO` 并点名 canonical command blocker。
- BDD: report matrix 必须精确绑定 -> Given report 缺失或错误填写 `checkedScripts` / `checkedE2eFiles` / When validator 读取 report / Then 必须校验字段存在、非空、无重复、只包含并完整包含当前 required matrix 的 7 个 check scripts 与 7 个 E2E files。

## Strict TDD Plan

1. RED: 新增后端合同测试，构造泛化 E2E report、缺 feature、错误 command、缺 matrix 字段，预期旧 validator 错误放行。
2. GREEN: 扩展 validator 解析 eDHR coverage report，绑定 outer/report command 和 required matrix。
3. REVIEWER RED/GREEN: Jason、Carver、Darwin、Maxwell 逐轮指出 command spoof 和 matrix 覆盖缺口后，补充对应负例测试，再最小化收紧 validator。
4. MAIN REVIEW RED/GREEN: 主 reviewer 发现 `--report path;echo` 等 shell 元字符可被吞进 path，先补充负例测试确认 8 项失败，再收紧 report path 为可审计安全字符集合。
5. REGRESSION: 运行 eDHR Go/No-Go 合同测试、G8/G9 与 G10/G11 回归、示例模板 NO-GO、actual frontend report integration、`git diff --check`。

## Current Status

- status: completed
- frontend paired task: `yudao-ui-admin-vue3/doc/tasks/20260530-edhr-release-coverage-gonogo-binding/`
- completed work:
  - Required `ciEvidence.e2eGates.reportPath` to be a frontend eDHR release coverage JSON report.
  - Required outer and report `command` to be canonical eDHR release coverage check commands.
  - Required report `schemaVersion=1`, `status=passed`, `mode=check`, `realGateClaimed=false`, exact required feature IDs, all feature statuses passed, exact `checkedScripts`, exact `checkedE2eFiles`, and empty `failures`.
  - Kept validator read-only; no webhook, backup, restore, rollback, publish, or environment mutation is invoked.

## Verification Evidence

- RED: eDHR command metacharacter hardening -> `python -X utf8 -m pytest script\tests\test_edhr_release_ops_acceptance_contract.py -q` failed 8 cases before final hardening because `--report ...json;echo`, `&&echo`, and `|echo` were accepted as GO.
- GREEN: `python -X utf8 -m pytest script\tests\test_edhr_release_ops_acceptance_contract.py -q` -> PASS, 67 tests.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_readiness_g8_g9_contracts.py script\tests\test_release_readiness_g10_g11_contracts.py -q` -> PASS, 18 tests.
- GREEN: placeholder template validator run -> `NO-GO` as expected, with `readOnly=true` and `sendsWebhook=false`.
- GREEN: temporary valid Go/No-Go evidence using the actual frontend `test-results\edhr-release-coverage\report.json` -> `decision=GO`, `readOnly=true`, `sendsWebhook=false`.
- GREEN: temporary evidence with metacharacter outer/report command `node scripts/edhr-release-e2e-coverage-gate.mjs --check --report test-results/edhr-release-coverage/report.json;echo` -> `decision=NO-GO`, outer/report command both blocked as non-canonical.
- GREEN: `git diff --check` -> PASS; Git emitted only Windows LF/CRLF normalization warnings for touched files.
- REVIEW PASS: final independent reviewer `Ohm` -> PASS, no blocking issues, no required changes.
- CLOSEOUT PREVIEW: task-closeout-cleanup preview -> BLOCKED for automatic cleanup/merge; no delete candidates. Blockers: branch cannot fast-forward merge into `int_main`, main backend worktree is dirty, and cleanup does not own pending code/test changes.
- POST-COMMIT CLOSEOUT PREVIEW: task-closeout-cleanup preview -> BLOCKED for automatic cleanup/merge; no delete candidates. Current worktree is clean; remaining blockers are that branch `codex/20260527-edhr-prod-doc-code-subagent-review` cannot fast-forward merge into `int_main`, and main backend worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` has unrelated dirty changes.

## Subagent-Driven Review

- Worker `Popper` added the first backend RED tests and initial validator binding.
- Independent reviewers `Jason`, `Carver`, `Darwin`, and `Maxwell` each failed an integrated diff on command/report binding gaps.
- Workers and main reviewer repaired each blocker with new RED/GREEN coverage.
- Main reviewer recovered the backend worktree after it disappeared from the paired worktree folder, recreated the worktree on `codex/20260527-edhr-prod-doc-code-subagent-review`, restored the reviewed changes, and reran validation.
- Final independent reviewer `Ohm` passed the restored integrated diff.

## Cleanup Keep

- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/task.md`
- `doc/tasks/20260530-edhr-release-coverage-gonogo-binding/execution-log.md`
