# Execution Log

## Intent

用户反馈：在表单模板页签点击“链接”按钮后先提示“批记录单元格链接引用的表单不存在：PRODUCTION_WORK_ORDER”；修复入口初始化后，又提示“批记录表单布局 JSON 无效：FORMTPL:32”。需要解释含义并修复表单模板链接工作台的前后端阻塞。

## BDD

BDD: 表单模板链接入口默认生产工单来源 -> Given 表单模板链接入口返回默认来源 `PRODUCTION_WORK_ORDER`，When 工作台初始化加载源区域，Then 前端必须把来源类型同步为 `PRODUCTION_WORK_ORDER` 并渲染生产工单字段矩阵，不得请求 `/form-cells?reportId=PRODUCTION_WORK_ORDER`。

BDD: 表单模板根布局 JSON 可作为链接目标 -> Given Form Center 模板版本 `FORMTPL:32` 的 `jimuSchemaJson` 根对象直接包含 `rows` 和 `cellRules`，When 链接工作台加载该模板目标单元格，Then 后端必须按正式模板布局解析可链接目标单元格，不得只接受字符串型 `sheetLayoutJson`。

BDD: 表单模板识别字段可作为链接目标 -> Given Form Center 模板版本 `FORMTPL:32` 的 `jimu_schema_json` 为空但 `recognized_schema_json` 是正式字段数组，When 链接工作台加载该模板目标单元格，Then 后端必须按识别字段生成可链接目标单元格，不得报布局 JSON 无效。

## Evidence

- ROOT_CAUSE: `loadWorkbenchContext` 固定先设置 `sourceType = BATCH_RECORD_CELL`，随后把 `sourceReportId` 设为后端返回的 `PRODUCTION_WORK_ORDER` 并立即调用 `loadSourceCells()`；因此前端按普通批记录表单调用 `getFormCells(PRODUCTION_WORK_ORDER)`，后端按正式 fail-fast 报“引用的表单不存在”。
- ROOT_CAUSE: `getFormTemplateCells()` 只读取 `schema.getString("sheetLayoutJson")`；部分 Form Center 模板把已保存布局作为根对象 `rows`，或作为对象型 `sheetLayoutJson` / `layout` 保存，导致布局字符串为空并触发“批记录表单布局 JSON 无效：FORMTPL:32”。
- ROOT_CAUSE: 本地真实数据 `bpm_form_template_version.id=32` 中 `jimu_schema_json` 为 `NULL`，`recognized_schema_json` 为 JSON array 字段列表；旧实现没有把识别字段形态纳入单元格链接目标解析契约，因此继续在 `parseTemplateSchema()` 处报“批记录表单布局 JSON 无效：FORMTPL:32”。
- RED: `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js` -> FAIL, 工作台初始化没有归一化默认来源，仍会先固定为普通批记录来源。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 修复前根布局 JSON 模板会触发 `批记录表单布局 JSON 无效：FORMTPL:32`。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 修复前 `jimu_schema_json=NULL + recognized_schema_json=字段数组` 会触发 `批记录表单布局 JSON 无效：FORMTPL:32`。
- GREEN: `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `node tests/e2e/mes/batch-record-cell-link-static.spec.js` -> PASS
- REGRESSION: `node tests/e2e/form-template-button-interaction-parity-static.spec.js` -> PASS
- REGRESSION: `node tests/e2e/form-template-independent-button-actions-static.spec.js` -> PASS
- REGRESSION: `node tests/e2e/form-center-static.spec.js` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260728-form-template-cell-link-work-order-init/bug-regression-evidence.md` -> PASS
- GREEN: `git -C E:\IntRuoyi diff --check -- <task-owned-files>` -> PASS
- GREEN: project-experience-consolidation -> PASS, 已确认现有 `docs/frontend-development.md` 表单模板按钮领域边界门禁、`docs/e2e-rules.md` schema-backed E2E 门禁，以及 `docs/e2e-rules.md` 中 FormCenter 预览布局读取 `jimuSchemaJson.sheetLayoutJson/layout/rows` 的规则覆盖本类问题；相关全局文档已有并行脏改动，本次只在任务文档摘录命中门禁，不新增长期经验文档。
- RUNTIME: `48081` health -> UP, but listener PID 归属旧运行 Jar `backend-switch-filler-20260728-131920.jar`，该 Jar 未包含本次 `recognized_schema_json` 修复。
- BLOCKER: runtime reload -> 已生成补丁 Jar `output/runtime/int_main/backend-formtpl32-20260728-1328.jar`（仅替换本任务服务 class），但自动停止/启动 `48081` 的命令被环境安全策略拦截；未强杀旧进程，避免绕过本地运行态规则。
- BLOCKER: commit/push -> 当前工作区存在非本任务脏改动，未进行本任务提交推送，避免混入并行任务改动。
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS, 生成完整 `yudao-server-exec.jar`，不再使用手工补丁 Jar。
- RUNTIME: copied full jar -> `output/runtime/int_main/backend-formtpl32-full-20260728-1416.jar`, SHA256 `624282A33DFE21E7C5DD5C5EF2D443314DFA1A306BA6467BF74779FB16B52BD6`。
- RUNTIME: direct Stop-Process/Start-Process reload -> BLOCKED by environment policy; switched to standard local restart script instead of bypassing process policy.
- GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS.
- RUNTIME: `48081` listener -> PID `56272`, Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar`, SHA256 `073AFE1D63B0D1C8F99847F68AB7E2916FCB090CA1DF720C63B58952D0B68903`, first health check `UP`.
- ROUTE_CHECK: unauthenticated `GET /admin-api/mes/pro/batch-record-cell-link/form-cells?reportId=FORMTPL%3A32` -> HTTP 200 body code `401`, proving the new backend exposes the target route behind security.
- BLOCKER: logged-in target API verification -> local login/API path is currently blocked by runtime dependency timeouts. Evidence: login request aborted, `/actuator/health` timed out on 3 attempts with 20s timeout, backend log shows JDBC/Redis connection acquisition timeouts, and Docker read-only checks timed out. No unrelated DB/Redis/Docker process was stopped.
- GREEN: project-experience-consolidation runtime follow-up -> PASS, existing `docs/local-runtime.md` gates already cover standard backend restart, stable runtime Jar, health/API distinction, and local dependency timeout handling; no new long-term experience document was created.
- USER_REPORT: `http://localhost:8081/mdm/form-center/template` 报错 -> reproduced runtime layer. `8081` returns the Vite HTML shell with HTTP 200, `48081` is still PID `56272` running the new Jar, and unauthenticated `/admin-api/system/auth/get-permission-info` returns code `401`.
- ROOT_CAUSE_RUNTIME: logged-in page requests are failing after token/DB access, not because the Vue route is missing. Backend log shows `/admin-api/system/dict-data/simple-list` and `/admin-api/system/auth/get-permission-info` failing with `CannotGetJdbcConnectionException` / `GetConnectionTimeoutException`, blocked behind a long DCC query.
- BLOCKER: safe backend restart -> `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` failed before stopping Java because Docker Desktop Linux engine returned HTTP 500 while inspecting `int-ruoyi-mysql`; current backend remained listening on `48081`. Docker/local dependency restart requires explicit authorization because it affects shared local containers.
