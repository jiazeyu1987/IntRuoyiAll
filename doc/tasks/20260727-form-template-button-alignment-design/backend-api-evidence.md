# Backend API Evidence

## Scope

- Endpoint scope: `/form-center/template-pool` 的模板列表响应。
- Service scope: `FormCenterRuntimeServiceImpl#toTemplateResp`。
- Persistence scope: `FormTemplateVersionDO` 对应 `bpm_form_template_version`。

## API And Data Contract

- `FormCenterTemplateRespVO` 新增显式批记录绑定摘要字段。
- `FormTemplateVersionDO` 新增同名 String 字段，由 MyBatis Plus 映射新增 snake_case 列。
- `toTemplateResp` 仅从 BPM 自身持久化版本读取字段，不引用 MES 类、不查 MES 表、不做名称匹配。

## Auth, Validation, And Error Behavior

- 模板池既有租户过滤与权限边界不变。
- 后端本次不新增隐式查询或降级路径。
- 缺少绑定由前端基于响应字段 fail fast；后续如新增绑定写入接口，应继续保持显式校验。

## Required Config, Services, Fixtures, And Migrations

- Required migration: `IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql`。
- No new service dependency.
- No BPM -> MES module dependency.

## BDD Scenarios

- `BDD: 模板池暴露显式批记录绑定摘要 -> Given 模板版本持久化绑定 reportId / When 查询模板池 / Then 响应包含同一 reportId 与绑定摘要。`
- `BDD: BPM runtime 不耦合 MES -> Given 需要展示表单模板按钮 / When 组装模板池响应 / Then 不查询 MES 表、不按名称猜测批记录报表。`

## RED And GREEN

- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, DO/VO 字段和 runtime 映射缺失。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 3 tests。`

## Observability

- 本次未新增日志；字段缺失不会被后端默认成功掩盖，前端会以明确提示暴露不可操作状态。

## Verification

- `mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，3 tests。
- `git diff --check -- <task-owned files>`：PASS。

## Blockers

- 绑定字段的数据写入来源需由导入链路或正式绑定流程填充；本次不做名称匹配回填。
