# Execution Log

## Initial Intent

用户要求继续验证 PQC 正式提交成功写入链路，并明确授权缺少的数据可由 Codex 自行补充。

## BDD Scenarios

- BDD: PQC formal submit succeeds with task-owned formal prerequisites -> Given an active order, a pending PQC task, published QA regulation and inspection items, a formal production-submit event, and an authorized current-user signature account, When the user submits from the real frontline PQC page and signs with the current-user password, Then the backend writes the PQC result, process event, PQC record, piece details, and `PQC_SUBMIT` signature transactionally and the frontend shows a receipt.
- BDD: duplicate formal submit is idempotent -> Given the same PQC task has already been formally submitted, When the user attempts to submit again from the frontend path, Then the UI is locked and does not create a second formal write.
- BDD: fixture data remains task-owned and traceable -> Given this verification creates missing formal prerequisites, When data is written, Then every created record uses the task marker `PQC-FS-WRITE-E2E-20260807` or a recorded task-owned identity and has a documented recovery path.
- BDD: PQC frontend handles backend timestamp shapes -> Given the backend serializes `LocalDateTime` as numeric timestamps in the current runtime, When the page renders production-submit candidate and receipt times, Then it must not throw `serverSubmitTime.replace is not a function`.
- BDD: PQC draft result calculation does not preempt bulk fill -> Given the user has selected a pending PQC task and clicks “全部合格”, When draft field values recompute, Then draft result calculation must not call submit-only sample-count assertions before the bulk values are fully established.

## Rules And Skills

- Trigger docs read: `docs/task-closeout-rules.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/database-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/powershell-encoding.md`, `docs/frontend-development.md`.
- Skills read: `playwright`, `database-schema-delivery`, `frontend-feature-delivery`, `independent-verification-gate`, `task-closeout-cleanup`, `project-experience-consolidation`.
- Experience index read: `docs/experience-index.md`.

## RED / GREEN Evidence

- RED: `SELECT COUNT(*) ... active_order_id=41 ... round_no=2` -> FAIL, expected task-owned pending round-2 fixture but actual count was `0`.
- GREEN: `prepare-pqc-write-e2e-fixture.ps1 -RoundNo 2` -> PASS, inserted task `230`; later real page submitted it but pageerror evidence required a clean rerun.
- RED: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> FAIL, expected dedicated `formatPqcServerSubmitTime` to support numeric timestamps; source still used `candidate.serverSubmitTime.replace(...)`.
- GREEN: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS after adding `formatPqcServerSubmitTime` and widening `serverSubmitTime` type to `string | number`.
- RED: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> FAIL, expected draft result calculation not to call `getPqcExactPieceValuesForSubmit`.
- GREEN: `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS after changing draft result calculation to use `getPqcCurrentChoiceValues`, keeping submit-time `assertPqcSubmissionSampleQuantities`.
- GREEN: `prepare-pqc-write-e2e-fixture.ps1 -RoundNo 3` -> PASS, inserted clean task `231` for final real-page rerun.
- GREEN: `run-pqc-write-e2e-with-temp-password.ps1` with `PQC_WRITE_E2E_TASK_ID=231` and `PQC_WRITE_E2E_ROUND_NO=3` -> PASS;真实页面登录 `shangmengying`，选择目标工单，全部合格，电子签名，正式提交。

## Fixture Snapshot

- Existing task-owned formal source: work orders `980028`-`980032`, active orders `41`-`45`, route `980094`, route version `628`, route process `980675`, process `922985`.
- Existing published QA source: regulation/version `38`, inspection type `FINAL`, item `CODX-PQC-20260807-SP-FINAL`, equipment `41 / A03190`.
- Existing formal production-submit source for target order: process-pool event `171`, with device account `659`, device `41`, workstation `980010`.
- Created fixture `230`: round `2`, later submitted as event `188`, record `109`, signature `3390`; used to expose pageerror and confirm write path.
- Created fixture `231`: round `3`, final clean verification target; submitted as event `189`, record `110`, signature `3391`.
- Tenant boundary: tenant `1` only.

## Account Recovery Evidence

- Temporary E2E password wrapper uses current-user account `659 / shangmengying` only, never logs plaintext password.
- A failed prior restore left user `659` at a temporary hash; `recover-pqc-user-659-from-binlog.ps1` decoded local MySQL binlog and restored the exact before-image.
- Final account assertion after successful E2E: user `659`, updater `NULL`, update_time `2026-08-06 03:21:25`, enabled tenant `1`, not deleted, password bcrypt length `60`.
- Wrapper restore logic was hardened to restore by the temporary password hash even if updater was already reverted, while still failing fast if final snapshot does not match.

## Verification Commands

- `pnpm e2e:frontline-formal-submit:static` -> PASS.
- `node tests/e2e/mes-frontline-pqc-submit-to-leader-chain-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `run-pqc-write-e2e-with-temp-password.ps1` with task `231` / round `3` -> PASS.
- DB read-only assertions: task `231` is `SUBMITTED`, actual quantity `3`; event `189`, record `110`, signature `3391`; piece detail count `3`; duplicate event count `1`.

## Project Experience Consolidation

- Updated `docs/frontend-development.md#前端-localdatetime-响应契约门禁` to cover direct display paths such as `serverSubmitTime` numeric timestamps and formatter/static-contract requirements.
- Added `docs/frontend-development.md#前端提交前严格验证与草稿态计算隔离门禁` for separating draft/watch/computed result calculation from submit-only strict assertions.
- Updated `docs/experience-index.md` keywords and verified routing with `rg -n "serverSubmitTime\\.replace|submit-only|前端提交前严格验证与草稿态计算隔离门禁" docs/experience-index.md docs/frontend-development.md`.
- `git diff --check -- docs/frontend-development.md docs/experience-index.md` -> PASS, with only LF-to-CRLF working-copy warnings.

## Closeout Cleanup Evidence

- `task_closeout.py --task-id 20260807-frontline-pqc-formal-submit-write-e2e --mode preview` -> READY, keep `task.md` / `execution-log.md` / `verification-report.md`, blocked `<none>`, warnings `<none>`.
- `task_closeout.py --task-id 20260807-frontline-pqc-formal-submit-write-e2e --mode apply` -> APPLIED, deleted only current task-owned artifacts, helper scripts, and temporary skill evidence files; remaining files are the three core task records.

## Blockers

- None remaining.
