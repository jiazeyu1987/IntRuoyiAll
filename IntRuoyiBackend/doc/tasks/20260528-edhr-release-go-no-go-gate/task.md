# 20260528-edhr-release-go-no-go-gate

## Task Goal

实现一个只读的 eDHR 生产发布 Go/No-Go 机器门禁，聚合当前发布镜像、受保护存储、即时备份、恢复演练、G8/G9、G10/G11 与 CI 证据。门禁必须 fail-closed：缺任何真实证据、出现占位值、证据失败或证据不一致时输出 JSON `decision=NO-GO` 或 `decision=BLOCKED`，并以非 0 退出；只有所有证据真实且通过时才允许 `decision=GO`。

门禁不得发送 webhook、不得执行 rollback/restore/backup、不得修改目标环境或输入证据文件。

## Milestones

- M1: 建立任务文档、BDD 场景与 RED 合同测试。
- M2: 实现 `script/release-readiness/validate-edhr-production-go-no-go.ps1` 只读聚合 validator 与必要示例模板。
- M3: 跑通本脚本合同测试和既有 G8/G9、G10/G11 回归测试。
- M4: 更新任务文档和 execution log，记录最终证据、剩余生产阻塞与改动范围。

## Expected Verification

- `python -X utf8 -m pytest script/tests/test_edhr_release_ops_acceptance_contract.py -q`
- `python -X utf8 -m pytest script/tests/test_release_readiness_g8_g9_contracts.py script/tests/test_release_readiness_g10_g11_contracts.py -q`
- 只读安全检查：脚本文本不得包含执行 webhook、rollback、restore、backup 的真实动作调用；validator 不得修改输入文件。

## Current Status

- status: completed for machine-gate code and tests; real production release remains `NO-GO` until real evidence is supplied
- owner: backend worker + main reviewer
- allowed write scope:
  - `doc/tasks/20260528-edhr-release-go-no-go-gate/task.md`
  - `doc/tasks/20260528-edhr-release-go-no-go-gate/execution-log.md`
  - `script/release-readiness/validate-edhr-production-go-no-go.ps1`
  - `script/release-readiness/templates/edhr-production-go-no-go.example.json`
  - `script/tests/test_edhr_release_ops_acceptance_contract.py`

## 2026-05-28 Reviewer Fail Repair Scope

- Reviewer fail: production Go/No-Go validator accepted non-empty evidence path strings without proving that protected storage, backup manifest/checksum, CI report, rehearsal archive/hash/restore evidence, and confirmation evidence files exist and are readable.
- Reviewer fail: backup manifest/checksum and CI reports lacked content consistency checks.
- Reviewer fail: rehearsal archive/hash/restore validation lacked strict archiveId and SHA-256 checks.
- Reviewer fail: top-level `releaseId`, `currentImageTag`, and `backupId` were not bound to G8/G9 and G10/G11 confirmation files.
- Repair expected result: every reviewer blocker above is covered by failing contract tests before implementation, then by passing GREEN verification.

## Verification Evidence

- RED: `python -X utf8 -m pytest script/tests/test_edhr_release_ops_acceptance_contract.py -q` failed before implementation because the production Go/No-Go validator and example evidence template were absent.
- GREEN: `python -X utf8 -m pytest script/tests/test_edhr_release_ops_acceptance_contract.py -q` passed, 7 tests.
- GREEN: `python -X utf8 -m pytest script/tests/test_release_readiness_g8_g9_contracts.py script/tests/test_release_readiness_g10_g11_contracts.py -q` passed, 18 tests.
- CHECK: `git diff --check` passed.
- REVIEWER_FAIL: independent reviewer and main reviewer found that the initial validator allowed synthetic path strings, stale confirmation files, unrelated checksums, mismatched archive evidence, and invalid hash evidence to produce `GO`.
- RED: reviewer adversarial checksum check -> FAIL, a checksums file with a valid-looking SHA-256 digest for `unrelated.bin` still produced `decision=GO`.
- GREEN: `python -X utf8 -m pytest script/tests/test_edhr_release_ops_acceptance_contract.py -q` passed, 17 tests after repair.
- GREEN: `python -X utf8 -m pytest script/tests/test_release_readiness_g8_g9_contracts.py script/tests/test_release_readiness_g10_g11_contracts.py -q` passed, 18 tests after repair.
- CHECK: `git diff --check` passed after repair.
- CHECK: `python -X utf8 tool\verify_tdd_compliance.py --all-changed --task-dir doc\tasks\20260528-edhr-release-go-no-go-gate` passed after repair.
- FINAL GREEN: `python -X utf8 -m pytest script/tests/test_edhr_release_ops_acceptance_contract.py script/tests/test_release_readiness_g8_g9_contracts.py script/tests/test_release_readiness_g10_g11_contracts.py -q` passed, 35 tests.
- FINAL CHECK: example template validation returned `decision=NO-GO`, 43 blockers, `readOnly=True`, `sendsWebhook=False`.
- FINAL REVIEW NOTE: independent final reviewer sub-agent attempts returned service `503`; those failed attempts were not used as release evidence. The earlier independent reviewer failure was repaired, and main reviewer reran adversarial checks and the final verification commands above.
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-release-go-no-go-gate --mode preview` -> `blocked`, no delete candidates. Blockers are linked-worktree fast-forward/main-worktree state only; all five current task deliverables are classified as keep.

## Remaining Blockers

- 未提供真实生产环境 eDHR 发布证据包；本轮只实现机器门禁与合成合同测试，不能据此判定真实生产发布为 GO。
- 真实发布前仍必须提供并通过验证：protected storage verifier PASS、backup-now success/INTBK-0000、同 backupId 的 rehearsal PASSED/success、G8/G9 确认、G10/G11 确认，以及未跳过测试的 backend/frontend/E2E CI 证据。

## Cleanup Keep

- `doc/tasks/20260528-edhr-release-go-no-go-gate/task.md`
- `doc/tasks/20260528-edhr-release-go-no-go-gate/execution-log.md`
- `script/release-readiness/validate-edhr-production-go-no-go.ps1`
- `script/release-readiness/templates/edhr-production-go-no-go.example.json`
- `script/tests/test_edhr_release_ops_acceptance_contract.py`
