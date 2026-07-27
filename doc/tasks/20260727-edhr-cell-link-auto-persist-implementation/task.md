# eDHR 单元格链接自动落库实现

## Task Goal

实现 `doc/tasks/20260727-edhr-cell-link-auto-persist-design/` 中定义的开发验证任务：创建或打开 eDHR 批记录执行记录时，后端按已启用单元格链接规则自动把生产工单批号等来源值落库到执行记录 `cell_values_json`，并保留字段审计链证据；前端不再把未落库的本地预填值当作正式值显示。

## Scope

- 新增或调整 MES 后端自动落库服务、字段审计写入边界、创建/打开执行记录链路和响应摘要。
- 覆盖生产工单来源字段 `batchCode` 到目标批记录单元格的成功、缺源值、目标已有值和重复打开幂等场景。
- 调整 eDHR 执行页前端状态流，移除未落库 `/prefill` 草稿注入语义。
- 补充后端 RED/GREEN 测试、前端静态契约、任务证据和验证报告。
- 不新增数据库迁移，不直接修复历史业务数据，不引入前端兜底或静默降级。

## Milestones

- [x] 建立实现任务证据，记录适用门禁和 RED 场景。
- [x] 后端补 RED 测试，证明创建/打开执行记录未自动落库。
- [x] 实现后端自动落库、字段审计链更新、幂等和错误模型。
- [x] 调整前端执行页，移除未落库预填注入，并补静态契约。
- [x] 运行目标后端、前端和结构校验，更新证据文档。
- [x] 完成经验沉淀、收尾清理、提交并推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" test`
- `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- `git diff --check -- <task-owned files>`
- 若本地运行态、登录或测试租户前置满足，再执行真实 eDHR 批次执行 Playwright 路径；若缺前置，记录明确 blocker，不用 API-only 冒充通过。

## Current Status

completed

## Verification Summary

- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkAutoPersistServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，4 tests。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，138 tests。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext+openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test`，3 tests。
- PASS: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js`。
- PASS: `git diff --check -- <task-owned implementation files and implementation task docs>`，仅 Windows LF-to-CRLF 提示。
- BLOCKED for full class regression only: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 仍有既有无关失败：H2 `bpm_form_template_version.batch_record_report_id` 缺列、批记录附件负责人配置无效、`get_releasePendingApproval_locksNormalTaskActions` 期望差异。
- PASS: 三份交付证据验证器通过，cleanup preview/apply 通过，长期经验已归入现有门禁。
- PASS: 实现提交 `b7dc3380` 和验证记录提交 `6b2575da` 均已存在于 `origin/int_main`。
- E2E scope note: 本机 `8081/48081` 未运行且没有当前任务授权的写入型测试租户/账号，因此未声明真实 Playwright 路径通过，也未用 API-only 兜底。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺生产工单、缺生产批号、目标字段缺失、审计链冲突必须显式失败或显式冲突，不写空值、不默认成功。
- `是否从根因和长期维护角度解决`：是。落库动作收敛到后端创建/打开执行记录写边界，并复用字段审计链，不靠前端本地 draft 展示伪装成功。
- `是否存在临时补丁或绕过`：否。不直接 SQL 修数据，不跳过字段审计，不把只读 GET 详情改成隐式写接口。

## 经验门禁

- `docs/backend-development.md#edhr-详情回填门禁`：配置页有值但详情为空时，必须核对来源字段、执行任务快照、详情组装链路和正式规则来源；禁止只改前端显示。
- `docs/backend-development.md#edhr-批记录版本治理规则运行态门禁`：涉及 `openOrCreateByContext` 时必须保留已发布批记录版本治理校验，不得绕过运行态规则确认。
- `docs/e2e-rules.md#schema-backed-e2e-迁移与字段可选态门禁`：涉及单元格链接真实 E2E 时必须核对 schema、可见态、可选态和写请求证据；禁止 API-only 冒充页面通过。
- `docs/frontend-development.md#前端静态契约隔离门禁`：如果宽范围前端检查存在历史失败，新增本任务专用静态契约覆盖当前行为。
- `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：PowerShell 中 Maven `-D` 参数必须整体加双引号。
- `docs/task-closeout-rules.md#任务验证脚本保留门禁`：若新增任务专用验证脚本需要保留，必须写入 Cleanup Keep 并在提交时强制纳入。

## Cleanup Keep

- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/task.md
- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/execution-log.md
- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/verification-report.md
- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/backend-api-evidence.md
- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/frontend-feature-evidence.md
- doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/bug-regression-evidence.md
