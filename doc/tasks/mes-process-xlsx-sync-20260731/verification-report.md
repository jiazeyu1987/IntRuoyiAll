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

## Commands

- `node doc\tasks\mes-process-xlsx-sync-20260731\tools\parse-pressure-pump-xlsx.mjs` -> PASS，32 valid rows, 3 ignored rows.
- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS，静态合同校验 Excel 12 列全字段 SQL 基线、接口、页面、租户忽略和菜单搜索。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS，1 test passed。
- `pnpm ts:check` -> PASS。
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\mes-process-xlsx-sync-20260731\migration-policy-gate.json` -> PASS，migrationCount=403。

## Remaining Closeout

- Cleanup/commit/push not performed yet because the shared `int_main` workspace is already `ahead 19` and contains multiple unrelated dirty files from other task areas.
- Cleanup apply was not run because preview included outside-workspace runtime dependencies under `C:\Users\BJB110\.cache\codex-runtimes\...node_modules` in the delete list.
- Current task status is `ready_for_closeout`; task-owned changes are isolated and verified, but final Git closeout must avoid staging unrelated DCC/team-leader/other task edits.
