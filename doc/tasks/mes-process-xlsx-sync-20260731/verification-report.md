# Verification Report

## Scope

- User request: MES 工序列表必须与 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx` 完全一致，不多不少。
- Source baseline: `二代压力泵` 工作表，Excel 行 2-33 共 32 条有效 MES 工序，行 34-36 仅有孤立产能值并排除。
- Display contract: 前端只读展示 Excel 12 个原始列，保留 `/` 与空白，不做布尔转换、不展示路线资源派生列。

## Result

- PASS: 新增独立后端只读目录 `/mes/pro/mes-process/page`，不再复用 `/mes/pro/route-resource/page` 聚合读模型。
- PASS: SQL 种子 `20260731_mes_process_catalog_from_pressure_pump_xlsx.sql` 写入 32 条 `PUMP2-MES-*` 目录行，逐行对应 Excel 行 2-33。
- PASS: 斜杠设备编码写入设备明细表，例如 `B09032/G01160`、`A05199/A05203`、`A05048/A03274` 均拆分。
- PASS: 两张 Excel 只读目录 DO 均标记 `@TenantIgnore`，避免 `tenant_id=0` 源基线在业务租户下不可见。
- PASS: 前端页面展示 Excel 12 列并调用独立 API，无新增、编辑、删除、导入、启用、停用维护入口。
- PASS: 2026-08-01 运行态 404 已修复；48081 已切换到包含 `MesProMesProcessController` 的新稳定 runtime Jar，actuator mappings 包含 `/mes/pro/mes-process/page`。
- PASS: 本机数据库已应用 MES 工序目录 SQL；登录态分页返回 `code=0`、`total=32`，首条 `粗洗`，末条 `W包装打包`。

## Commands

- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\parse-pressure-pump-xlsx.mjs` -> PASS，32 valid rows, 3 ignored rows.
- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS，静态合同校验 Excel 12 列全字段 SQL 基线、接口、页面、租户忽略和菜单搜索。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed。
- `pnpm ts:check` -> PASS。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\mes-process-xlsx-sync-20260731\migration-policy-gate.json` -> PASS，migrationCount=403。
- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> RED before reload, FAIL missing MVC mapping `/mes/pro/mes-process/page`。
- `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，runtime Jar SHA256 `D44F5BE85C16F116959C3F259A62B4D48DEB7BA45A144CED43137ABEC61FAE5A`。
- Backend runtime reload -> PASS，新 PID `54564` 使用 `output\runtime\int_main\backend-runtime-control-20260801-002326.jar`，health `UP`。
- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\check-mes-process-runtime-route.mjs` -> GREEN after reload, PASS found MVC mapping `/mes/pro/mes-process/page`。
- Local SQL apply -> PASS，执行 `20260731_mes_process_catalog_from_pressure_pump_xlsx.sql` 后目录表 32 行、源行号 2-33、排序 1-32、设备明细 19 行。
- Logged-in page API probe -> PASS，`/admin-api/mes/pro/mes-process/page?pageNo=1&pageSize=50` 返回 `code=0`、`total=32`、首尾工序 `粗洗` / `W包装打包`。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\mes-process-xlsx-sync-20260731\bug-regression-evidence.md` -> PASS。
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED，当前 testCompile 被无关历史 MES 测试缺失符号阻塞；不是本轮 404/500 修复代码路径失败。

## Remaining Closeout

- Cleanup/commit/push not performed yet because the shared `int_main` workspace has concurrent unrelated third-party feedback progress edits and latest baseline commit `ec52d8dc8` already mixed several task areas.
- Cleanup apply was not run because preview included outside-workspace runtime dependencies under `C:\Users\BJB110\.cache\codex-runtimes\...node_modules` in the delete list.
- Current task status is `ready_for_closeout`; runtime route/data are fixed, but final Git closeout must avoid staging unrelated active task edits.
