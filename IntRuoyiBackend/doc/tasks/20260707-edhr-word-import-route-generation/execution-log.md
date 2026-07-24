# Execution Log: eDHR Word 导入自动生成工艺路线

BDD: 成功生成路线 -> Given 用户上传 Word 且第一个解析表单为“产品信息”，When 填写批记录名称并执行导入，Then 系统生成批记录表单、启用工艺路线、去除产品信息后的路线工序和工艺批记录路线绑定。

BDD: 缺少产品信息中止 -> Given 用户上传 Word 但解析表单中没有产品信息，When 执行导入，Then 系统报错并回滚批记录表单、路线、路线工序和用途绑定。

BDD: 产品信息不在首位中止 -> Given 用户上传 Word 且产品信息不是第一个解析表单，When 执行导入，Then 系统报错并回滚所有导入落库结果。

BDD: 自动补齐工序主数据 -> Given Word 中包含新的工序名称，When 导入成功，Then 系统自动创建缺失工序主数据并复用已有同名工序。

## Evidence

- RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest test` -> FAIL, expected reason: missing route generation service, new rollback error codes, and route summary fields on import result.
- GREEN: `mvn.cmd -pl yudao-module-mes clean test "-Dtest=MesProBatchRecordReportServiceImplDbTest"` -> PASS, 43 tests / 0 failures / 0 errors.
- REGRESSION: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProRouteUseConfigControllerPermissionTest,MesProEdhrBatchExecutionServiceTest,MesProWorkOrderMapperTest,MesProWorkOrderServiceImplTest" test` -> PASS, 75 tests / 0 failures / 0 errors.
- FRONTEND: `pnpm.cmd ts:check` -> FAIL, Node heap out of memory at default 4GB.
- FRONTEND: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-word-import-route-generation --mode preview` -> READY, keep `task.md` and `execution-log.md`, delete `backend-api-evidence.md`.
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-word-import-route-generation --mode apply` -> APPLIED, deleted `backend-api-evidence.md`.

## Real E2E Follow-up

BDD: 真实 Word 导入生成路线 -> Given 测试租户 `aoteman` 通过前端批记录工序页面上传真实 Word `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`，When 填写唯一批记录名称并确认导入，Then 系统通过真实上传接口生成批记录表单、启用工艺路线、去除产品信息后的路线工序，并生成 `BATCH_RECORD` / `CONTROLLED_BATCH` / `SEQUENTIAL` 的工艺批记录路线绑定。

- PREFLIGHT: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template --target-text 批记录名称` -> FAIL, bundled Chromium headless shell exited at ICU data initialization before opening login page.
- GREEN: experience-preflight -> PASS, `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/batch-record-template --target-text 批记录名称` -> PASS, 真实登录已进入目标页。

- BLOCKER: real-e2e-schema-preflight -> FAIL, 本机 48081 已加载当前 jar，但本机 Docker MySQL 缺少 `mes_pro_batch_record_report.form_slot_type`，`/batch-record-report/exists` 因 `Unknown column 'form_slot_type'` 中止。
- GREEN: local-schema-preflight -> PASS, 已仅对本机 Docker MySQL 应用既有迁移 `sql/mysql/20260707_mes_batch_record_extra_form_slots.sql`，并复核 `mes_pro_batch_record_report.form_slot_type`、`mes_pro_route_use_process_batch_record.form_slot_type` 与唯一索引存在。
- BLOCKER: real-e2e-autocode-preflight -> FAIL, 真实导入已进入当前后端路线生成逻辑，但测试租户缺少 `PRO_ROUTE_CODE` 自动编码规则，接口返回“编码规则不存在”。
- GREEN: local-autocode-preflight -> PASS, 已仅对本机测试租户 `tenant_id=122` 补齐 `PRO_ROUTE_CODE` 自动编码规则，格式为固定前缀 `RT` + 6 位流水号。
- GREEN: local-autocode-preflight -> PASS, 已仅对本机测试租户 `tenant_id=122` 用显式 `utf8mb4_unicode_ci` 补齐 `PRO_ROUTE_CODE` 自动编码规则，格式为固定前缀 `RT` + 6 位流水号，并复核规则与分段存在。
- BLOCKER: real-e2e-route-use-schema-preflight -> FAIL, 真实导入已成功创建路线与绑定，但最终调用 `/admin-api/mes/pro/route-use-config/process-config-list` 校验用途配置时返回“系统异常”；本机 Docker MySQL 的 `mes_pro_route_use_process_batch_record` 缺少当前代码读取的 `required_policy`、`required_condition_json`、`owner_role_key`、`archive_visibility`、`slot_config_snapshot_hash` 字段。
- GREEN: local-route-use-schema-preflight -> PASS, 已仅对本机 Docker MySQL 应用既有迁移 `sql/mysql/20260707_mes_batch_record_extra_form_slots.sql` 的幂等建字段逻辑，并复核 `mes_pro_route_use_process_batch_record` 与 `mes_pro_edhr_batch_execution_task` 相关字段存在；迁移尾部历史数据回填命中既有排序规则冲突，但本次真实 Word 导入与路线用途查询所需字段已补齐。
- GREEN: real-e2e-word-import-route -> PASS, `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe node tests\e2e\edhr-word-template-import-real-flow.e2e.js` 使用真实 Word `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`、测试租户 `aoteman` 前端真实导入通过：`batchRecordName=E2E-WORD-ROUTE-20260707175158`、`routeCode=RT000002`、`reports=15`、`routeProcesses=14`、`batchBindings=14`。
