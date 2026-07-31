# Execution Log

## 2026-07-31

- User intent: MES 工序来源为 `C:\Users\BJB110\Desktop\文档\压力泵工序.xlsx`，系统 MES 工序列表必须与文档完全一致，不多不少。
- Rules read: `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/experience-index.md`、`bug-regression-fix-loop`、`spreadsheets`。
- Initial git status: `int_main...origin/int_main [ahead 17]`，存在大量已修改和未跟踪文件；本任务先定位并避免覆盖无关改动。
- BDD: MES 工序列表与 Excel 源完全一致 -> Given Excel 源文件包含压力泵 MES 工序清单 When 系统加载 MES 工序列表 Then 列表名称、数量和顺序必须与 Excel 完全一致且无额外项。
- Planned RED: 对比 Excel 解析结果与系统数据源/接口输出，修复前应报告差异。
- Source baseline: `node doc\tasks\mes-process-xlsx-sync-20260731\tools\parse-pressure-pump-xlsx.mjs` -> PASS；工作簿包含 `二代压力泵`、`一代压力泵`、`Sheet2`、`Sheet3`，本任务使用 `二代压力泵`；表头为 12 个 Excel 原始列；有效 MES 工序为 Excel 行 2-33 共 32 条；Excel 行 34-36 仅有孤立产能值 `588`、`7481`、`10225`，不得导入。
- RED: `node IntRuoyiFronted\tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> FAIL，预期原因：旧 MES 工序 API 仍复用 `ProRouteResourceApi.getResourcePage` / `/mes/pro/route-resource/page`，页面与数据源不能保证与 Excel 行 2-33 完全一致。
- Implementation: 新增独立后端只读链路 `/mes/pro/mes-process/page`，包括 Controller、VO、DO、Mapper、Service、目标 JUnit；新增 SQL `20260731_mes_process_catalog_from_pressure_pump_xlsx.sql` 创建并填充 `mes_pro_mes_process_catalog` / `mes_pro_mes_process_catalog_machinery`；前端 API 和页面改为独立只读目录，展示 Excel 12 个原始列。
- Implementation correction: 核对租户拦截器后发现目录 SQL 使用 `tenant_id=0`，而新 DO 若不显式忽略租户过滤会导致业务租户查询不到源基线；已在主表和设备明细 DO 添加 `@TenantIgnore`，并将该要求写入静态合同。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS；合同已覆盖页面只读能力、独立 API、菜单权限、租户忽略、SQL 种子恰好 32 条、Excel 12 个原始列逐行全字段一致、行 34-36 不导入、斜杠设备编码拆分明细。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProMesProcessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS；`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION: `pnpm ts:check` from `IntRuoyiFronted` -> PASS。
- REGRESSION: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260730_mes_process_readonly_catalog_menu.sql --sql-file sql\mysql\20260731_mes_process_catalog_from_pressure_pump_xlsx.sql --output ..\doc\tasks\mes-process-xlsx-sync-20260731\migration-policy-gate.json` -> FAIL，预期原因：专项子集未包含上游 `20260709_mes_route_flow_config_unification` 依赖上下文，脚本正确拒绝缺失依赖。
- REGRESSION: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\mes-process-xlsx-sync-20260731\migration-policy-gate.json` -> PASS，`status=passed`，`migrationCount=403`，包含 `20260731_mes_process_catalog_from_pressure_pump_xlsx`。
- Experience consolidation: 已按 `project-experience-consolidation` 规则将“全局只读 Excel 种子 + `tenant_id=0` + 租户过滤”经验合并到 `docs/database-rules.md#全局只读-excel-种子租户边界门禁`，并在 `docs/experience-index.md` 增加触发关键词。
- Status update: implementation and required targeted verification are complete; `task.md` set to `ready_for_closeout` because cleanup/commit/push remain pending in a shared dirty workspace.
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id mes-process-xlsx-sync-20260731 --mode preview` -> status ready, but preview delete list included `C:\Users\BJB110\.cache\codex-runtimes\...node_modules` outside workspace plus task helper files. Apply was not run because deleting outside-workspace runtime dependencies is unsafe for this task scope.
- CONCURRENT COMMIT NOTICE: while this task was in closeout, branch advanced from `ahead 18` to `ahead 19`; latest commit `c8d5db607 fix: align DCC project associated file types with taxonomy` includes `docs/experience-index.md`, which also contains this task's new experience-index keyword. History was not amended or reset.
- Current git boundary: task-owned uncommitted changes are `MesProMesProcessCatalogDO.java`、`MesProMesProcessCatalogMachineryDO.java`、`mes-pro-mes-process-readonly-static.spec.js`、`doc/tasks/mes-process-xlsx-sync-20260731/*`; unrelated dirty files in DCC/team-leader/other task areas remain untouched.

## 2026-08-01

- User intent: 运行态访问 `admin-api/mes/pro/mes-process/page` 报“请求地址不存在”，需要从根因修复，不能用 fallback、空数据或替代接口掩盖。
- Rules read: `bug-regression-fix-loop`、`bug-contract.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/local-runtime.md`、`docs/powershell-encoding.md`。
- Status update: 新增运行态路由缺失回归后，任务状态由 `ready_for_closeout` 改回 `in_progress`。
- BDD: MES 工序分页接口必须在运行后端注册 -> Given 前端 MES 工序页请求 `/admin-api/mes/pro/mes-process/page` When 后端服务加载 MES 模块 Then 请求不应返回“请求地址不存在”，应进入正式 Controller 并按权限/业务规则返回响应。
- Planned RED: 用静态/构建级契约或本机运行态探针证明当前路由未被当前服务注册或未被当前运行 Jar 加载。
