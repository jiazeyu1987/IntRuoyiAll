# Backend API Evidence

## Scope

- Endpoint scope: `GET/PUT /mes/pro/batch-record-report/cell-rules`、`GET/POST /mes/pro/edhr-process-form-permission-rule/get-by-report|save-by-report`、eDHR work task open and field audit save.
- Service scope: batch record cell rule support, process form permission rule service, work task responsibility snapshot, execution snapshot assist rows, and field audit authorization.

## API And Data Contract

- `cell-rules` 请求/响应携带 `assistRows`，并保存辅助行 `rowKey/description/sort/fields`。
- `save-by-report` 支持 `fillAssignments[]`，每个辅助行用 `scopeKey` 绑定个人或角色候选来源。
- 旧报表级填写规则通过迁移显式落到 `scopeKey=ALL` 和 `schemaVersion=2` 精确单元格范围。
- 工作任务冻结 `responsibilityScopeJson`；执行快照冻结 `assistRows`，运行态不从当前模板 JSON 回退。

## Auth Validation And Errors

- 后端按 `sourceTableIndex + rowIndex + columnIndex` 执行精确写入授权。
- 缺少辅助行、重复坐标、空描述、无效下拉选项、签名标记缺失、未知 `scopeKey`、混用旧 `fillRule` 与新 `fillAssignments` 均 fail fast。
- 责任快照缺失或损坏时拒绝任务打开/字段保存，不吞异常、不默认成功、不扩大权限。

## BDD Scenarios

- BDD: assist rows persist -> Given 可填写单元格存在 / When 保存辅助行 / Then 后端保存读回并拒绝重复或未覆盖坐标。
- BDD: fill assignments persist -> Given 报表已有辅助行 / When 保存 `fillAssignments` / Then 服务端生成精确 `fillableScopeJson`。
- BDD: responsibility snapshot -> Given 创建填写任务 / When 任务生成 / Then 一个表单仍只有一个任务，并冻结所有辅助行责任范围。
- BDD: field audit authorization -> Given 同一行不同列分配给不同员工 / When 越权写入 / Then 后端拒绝且不产生字段审计。

## RED And GREEN

- RED: targeted Maven tests in `execution-log.md` failed for missing `assistRows`, missing `FillAssignment`, missing explicit migration scope, missing responsibility snapshot, missing column-level authorization, and missing execution snapshot `assistRows`.
- GREEN: targeted Maven tests in `execution-log.md` passed after implementing cell rules, permission rules, migrations, work task snapshots, execution snapshots, and field audit authorization.

## Verification

- `MesProBatchRecordCellRuleSupportTest` -> PASS。
- `MesProEdhrProcessFormPermissionRuleControllerContractTest` and `MesProEdhrProcessFormPermissionRuleServiceImplTest` targeted cases -> PASS。
- `MesProEdhrVisualFillConfigScopeMigrationContractTest` -> PASS。
- `MesProEdhrWorkTaskServiceImplTest` and ownership transfer regression -> PASS。
- `MesProBatchRecordExecutionFieldAuditServiceTest` -> PASS。
- `MesProBatchRecordExecutionServiceImplTest` and `MesProEdhrBatchExecutionServiceTest` -> PASS。

## Observability

- Validation errors include row keys or cell coordinates where applicable.
- Runtime health for the current worktree backend is `UP` on `http://127.0.0.1:48083/actuator/health`.

## Blockers

- No backend blocker remains from T01-T07.
- Full real E2E remains blocked by missing three-user test credentials, explicit write authorization, and a task-owned `CODX-VFC-*` report fixture.
