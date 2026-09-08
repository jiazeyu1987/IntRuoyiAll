# Verification Report

## Result

通过：`int_main` 真实前端 E2E 已完成临时角色授权最小闭环 5/5。

## Commands

- `mvn.cmd -pl yudao-module-system '-Dtest=TemporaryRoleGrantServiceImplTest,PermissionServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，35 tests。
- `mvn.cmd -pl yudao-module-system -DskipTests compile` -> PASS。
- `python -X utf8 -m pytest script\tests\test_system_temporary_role_grant_sql.py -q` -> PASS，3 tests。
- `node tests\e2e\system-temporary-role-grant-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `pnpm exec eslint src/api/system/temporaryRoleGrant/index.ts src/views/system/temporary-role-grant/index.vue` -> PASS。
- `pnpm exec stylelint "src/views/system/temporary-role-grant/index.vue" --cache --cache-location node_modules/.cache/stylelint/` -> PASS。
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260908-temporary-role-grant-minimal\database-schema-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-temporary-role-grant-minimal\backend-api-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260908-temporary-role-grant-minimal\frontend-feature-evidence.md` -> PASS。
- `git diff --check` -> PASS。
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，当前分支端口 frontend 8164 / backend 48164。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260908-temporary-role-grant-minimal --mode preview` -> BLOCKED，当前任务分支已干净，但本机 `int_main` 含本地提交 `738235973`，当前分支不是它的后代，不能按规则 ff-only 合并。
- `python -X utf8 -c "from pathlib import Path; ..."` -> PASS，统一电子签名 T1 SQL delimiter 合同通过。
- `Get-Content IntRuoyiBackend\sql\mysql\20260908_system_temporary_role_grant.sql -Raw | docker exec -i int-ruoyi-mysql mysql ...` -> PASS，本机临时授权表、审计表、菜单和过期扫描任务已迁移。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\output\playwright\start-temporary-role-grant-int-main-backend.ps1` -> PASS，48081 重启到 `backend-runtime-control-20260908-171951.jar`，健康检查 `UP`。
- `node ..\output\playwright\temporary-role-grant-real-e2e.cjs` -> PASS，5 PASS / 0 FAIL / 0 BLOCKED；页面可达、新建、审批、撤销、审计闭环均通过真实前端操作完成。

## Notes

- admin 未修改，仍可用于测试。
- 已通过 cherry-pick 将临时角色授权任务提交融合进 `int_main`，未融合早先备份任务提交。
- cleanup preview/apply 在 `int_main` 上通过，无删除项。
- E2E 前置发现 `20260908_system_signature_password_t1.sql` 缺少 delimiter，已修复；该脚本完整唯一索引迁移仍因本机重复账号 `zhangsan`、`lisi` fail-fast，未自动合并账号。
