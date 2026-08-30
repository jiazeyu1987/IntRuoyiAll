# 20260830 NAS 原路径同步未受控文件 Verification Report

## Summary

已完成 NAS 未受控文件按原 NAS 路径同步到 DCC 系统的设计、开发和真实单文件 E2E 验证。

实现结果：
- 服务端新增“原路径同步”任务，支持 `FIRST_UNSYNCED`、`EXPLICIT_SELECTED_FILES`、`ALL_UNSYNCED` 三种范围。
- 同步前校验统计快照与 NAS 当前文件大小/修改时间签名一致；签名不一致会失败，不写入旧文件。
- 同步成功后写入 DCC 文件存储目录 `dcc/nas-original-path-sync/<原 NAS 父路径>`，并登记 ACTIVE 同步记录。
- 新一轮 NAS 统计会把 ACTIVE 原路径同步记录视为系统内已有，因此未受控数量减少。
- 移除同步记录会删除对应文件存储记录，并把 ACTIVE 记录置为 DELETED，下次统计会重新计入未受控数量。
- 前端统计明细补充分页、待识别文件勾选、同步 1 个验证、同步选中、同步全部、移除同步记录和状态列。
- 真实 E2E 首轮发现删除后数据库同步文件编号未清空，根因是 MyBatis Plus 默认更新策略跳过 `NULL` 字段；已改为 Mapper 显式清空并复验通过。

## Verification Commands

- `python -m pytest script\tests\test_dcc_nas_original_path_sync_sql.py -q` -> PASS, 2 passed.
- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileNasTransferServiceTest,DccNasControlAuditServiceImplTest" test` -> PASS, 59 tests, 0 failures, 0 errors.
- `pnpm e2e:dcc:nas-original-path-sync:static` -> PASS.
- `pnpm install --frozen-lockfile` -> PASS, frontend dependencies installed from lockfile.
- `pnpm ts:check` -> PASS.
- `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS.
- `node --check tests\e2e\dcc-nas-original-path-sync-real.e2e.js` -> PASS.
- `pnpm e2e:dcc:nas-original-path-sync:real` -> PASS, auditTaskId=5, auditFileId=7, syncTaskId=27.
- `docker exec ... mysql readonly cleanup counts` -> PASS, `ACTIVE_SYNC_ROWS=0`, `OPEN_FIXTURE_TASKS=0`, `OPEN_FIXTURE_FILES=0`.
- `git diff --check` -> PASS, no whitespace errors; only line-ending normalization warnings.
- Task-owned runtime cleanup -> PASS, stopped local frontend `8310` and backend `48310`; no remaining listeners on those ports.
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260830-nas-original-path-sync --mode preview` -> BLOCKED: closeout apply requires Git commit/merge/delete authorization and a clean main workspace; project policy does not allow those Git operations without explicit user request.
- `git merge --ff-only codex/nas-original-path-sync` in `E:\IntRuoyi` -> PASS, `int_main` fast-forwarded to `733bcddb1`.
- `node --check tests\e2e\dcc-nas-original-path-sync-real.e2e.js` after merge -> PASS.
- `pnpm e2e:dcc:nas-original-path-sync:static` after merge -> PASS.
- `scripts\preflight\branch-runtime-port-guard.ps1` after merge -> PASS, `int_main` ports remain frontend `8081`, backend `48081`.
- `git diff --check` after merge -> PASS, no whitespace errors; only line-ending normalization warnings from unrelated dirty files.
- `task-closeout-cleanup --mode preview` after merge -> PASS, no task-owned temporary paths to delete and no blockers.
- `task-closeout-cleanup --mode apply` after merge -> PASS, no files deleted; preserved task records.

## Real E2E Evidence

- Evidence JSON: `doc/tasks/20260830-nas-original-path-sync/artifacts/nas-original-path-sync-single-e2e-evidence.json`
- Screenshots: `doc/tasks/20260830-nas-original-path-sync/artifacts/nas-original-path-sync-single-before.png`, `nas-original-path-sync-single-after-sync.png`, `nas-original-path-sync-single-after-delete.png`
- Sample file: `1. QMS documents/0 QM/2025年质量方针与目标.pdf`
- Observed business result: before sync candidate count `1`; after sync active sync count `1` and candidate count `0`; after remove active sync count `0` and candidate count `1`.

## Result

completed

## Integration Completion

- Main baseline commit before integration: `61ba07435`.
- Rebased implementation commit: `6b5f29a76`.
- Task branch integration tip: `733bcddb1`.
- `E:\IntRuoyi` / `int_main` completed a fast-forward merge from `codex/nas-original-path-sync`.
- No push was performed; `int_main` remains local ahead of `origin/int_main`.
- Remaining main workspace dirty files are unrelated post-baseline changes and were not staged, committed, cleaned, reset, or merged by this task.
