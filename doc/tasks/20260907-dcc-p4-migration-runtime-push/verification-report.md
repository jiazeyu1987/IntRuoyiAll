# DCC P4 迁移、运行态重启与提交核验

## Verdict

- Result: PASS
- Scope: 测试库 P4 数据迁移、本地 `int_main` 48081 重启、DCC 生命周期验证和提交同步。

## Migration

- P4 migration first run: exit `0`。
- P4 migration repeat run: exit `0`。
- P4 static contract: `3 passed`。
- Complete dependency closure: `status=passed, migrationCount=10`。
- Post-migration read-only result: tenant 1 has one `DIRECT/PUBLISHED/DCC_PUBLISH` DCC publish policy; prior BPM policy is disabled.
- Protected lifecycle data unchanged: A/1 `SUPERSEDED`, A/2 `WORKING`, B/1 `ACTIVE`, Master points to B/1, signatures `10/10 VALID`。

## Restart

- Standard command: `script/deploy/restart-int-ruoyi-local.ps1 -Component backend`。
- Build result: Maven Reactor `30/30 SUCCESS`, `BUILD SUCCESS`, `yudao-server-exec.jar` repackaged。
- Runtime: PID `30832`, Jar `output/runtime/int_main/backend-runtime-control-20260907-085110.jar`, listener `48081`。
- Health: `http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`。

## DCC Verification

- Backend lifecycle and signature suites after restart: 331 tests, 0 failures/errors; signature subset 56 tests, 0 failures/errors。
- Frontend: 9 DCC static contracts pass; relaxed `vue-tsc` pass。
- Real page: B/1 ACTIVE, A/1 SUPERSEDED, A/2 WORKING and A/1 successor B/1 match read-only database facts。

## Git

- Existing implementation commit `94d1f6b7f` already contains the DCC lifecycle implementation and P4 migration.
- This task's evidence commit `9c7de643b` was pushed to `origin/int_main`; final `HEAD` and `origin/int_main` are synchronized.
- Concurrent unrelated changes in `docs/worktree-memory.md` and registration acceptance files were excluded from staging and remain untouched.

## Residual Boundary

- Full SQL-root policy gate remains blocked by the unrelated migration metadata defect in `20260903_dcc_controlled_file_related_file.sql`; the complete P4 dependency closure is green.
- No remote test/production server was accessed or modified.

## Closeout

- Cleanup preview and apply passed with no blocked paths or warnings.
- Only this task's intermediate database evidence file was removed after its validator result and migration evidence were copied into this report and execution log.
- Final task status is `completed`.
