# Verification Report

## Scope

本次在既有设计基础上完成开发验证：补齐 BPM 模板池批记录绑定摘要字段、SQL 增量迁移、前端 API 类型和表单模板红框三按钮路由对齐。

## Checks

- `documentation-structure`：已生成前端、后端 API、数据模型、配置安全部署四份设计文档。
- `no-fallback-design`：设计明确禁止名称匹配、空值兜底、静默回退旧弹窗。
- `mapping-blocker`：设计明确将缺少稳定 `reportId` 映射列为实现前 blocker。
- `python -X utf8 section-check`：PASS，任务级设计文档 UTF-8 可读且必备章节完整。
- `git diff --check -- doc\tasks\20260727-form-template-button-alignment-design`：PASS，无空白错误。
- `python -m pytest script\tests\test_form_template_batch_record_binding_sql.py`：PASS，3 项 SQL 迁移契约通过。
- `node tests\e2e\form-template-batch-record-button-alignment-static.spec.js`：PASS，表单模板三按钮不再引用旧弹窗/本页编辑/本页模拟填写入口。
- `mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，BPM 合同测试 3 项通过。
- `pnpm ts:check`：PASS，前端 relaxed TypeScript 检查通过。
- `docker information_schema.columns`：PASS，本地 Docker MySQL 的 `bpm_form_template_version` 已包含 7 个 `batch_record_*` 新增字段。
- `docker information_schema.statistics`：PASS，本地 Docker MySQL 已存在 `idx_bpm_form_template_batch_record_report(tenant_id,batch_record_report_id,deleted)`。
- `real-login template-pool probe`：FAIL/BLOCKED，页面 `/mdm/form-center/template` 可访问，模板池接口 code=0 且 rowCount=5，但当前 48081 原始 jar 首行不含新增绑定字段且 boundRowCount=0。
- `mvn.cmd -pl yudao-server -am "-DskipTests" package` on clean HEAD snapshot：FAIL，`yudao-module-mes` 编译错误阻止生成完整 server jar。
- `runtime jar patch attempts`：FAIL，BPM-only patch 与旧 CRM 不兼容，clean-built-module patch 与旧 MES 不兼容；已恢复原始后端 jar 且 health=UP。
- `mvn.cmd -pl yudao-server -am "-DskipTests" package` on clean snapshot + MES compile fix：PASS，生成完整 `yudao-server-exec.jar`。
- `runtime jar startup`：PASS，48081 运行 `yudao-server-exec-clean-snapshot-plus-mes-compile-fix.jar`，SHA256=`3F1A0FF05DF94A1D2E67C9A3F2B5F038BADD484F8EBB37FC00F063BC08C73487`，health=UP。
- `real-login template-pool probe`：PASS，页面 `/mdm/form-center/template` 可访问，模板池接口返回新增 `batchRecord*` 字段。
- `real E2E form template 3 buttons`：PASS，临时绑定模板行 `id=29` 到 reportId `45144f68db034fb9bbd01179c7cee59b` 后，真实点击 `打开 / 编辑 / 填写` 分别进入批记录 preview designer、edit designer、template-simulate 路由。
- `fixture restore`：PASS，`bpm_form_template_version.id=29` 的 `batch_record_report_id` 与 `batch_record_binding_status` 已恢复为 NULL。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence ...\frontend-feature-evidence.md`：PASS。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ...\backend-api-evidence.md`：PASS。
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ...\database-schema-evidence.md`：PASS。
- `python -X utf8 docs read`：PASS，任务目录 Markdown 均可 UTF-8 读取。
- `task-owned trailing whitespace scan`：PASS，本任务新增/修改文件均无尾随空白。
- `temporary artifact cleanup`：PASS，本任务 `.runtime` 目录和临时构建快照均已删除，PID 10456 未运行。
- `task-closeout-cleanup preview/apply`：PASS，10 份任务设计/证据文档全部保留，无 delete/blocked/warnings。
- `current worktree button alignment contract`：FAIL，聚焦断言报告 `打开,编辑,填写` 均未进入批记录同源路径。

## Implemented Files

- `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/dal/dataobject/formcenter/FormTemplateVersionDO.java`
- `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/controller/admin/formcenter/vo/FormCenterTemplateRespVO.java`
- `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/formcenter/runtime/FormCenterRuntimeServiceImpl.java`
- `IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql`
- `IntRuoyiFronted/src/api/form-center/template.ts`
- `IntRuoyiFronted/src/views/form-center/template/index.vue`

## Required Follow-Up Verification

- 目标环境迁移：在目标环境执行 release migration 后，用 `information_schema.columns/statistics` 只读核对新增字段和索引。
- 目标环境 E2E：使用正式绑定数据或授权测试数据复验三按钮真实点击路径。

## Blockers

- 并行任务已在同一源码和静态合同中实施与本任务相反的行为，其中编辑反向改动已进入提交，打开/填写反向改动当前已暂存。
- 当前工作区不再满足“表单模板三个按钮按批记录表单行为对齐”，需先明确两项互斥需求的最终优先级，再恢复实现和重跑真实 E2E。
