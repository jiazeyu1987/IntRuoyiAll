# Backend API Evidence

## Endpoint, Service, Job, or Handler Scope

- Endpoint: `GET /admin-api/mes/pro/edhr-batch-execution/get?id={id}`。
- Service: `MesProEdhrBatchExecutionServiceImpl`。

## API Contract and Data Contract

- Existing response field: `EdhrBatchExecutionTaskRespVO.fillableUsers`。
- Expected data contract: 动态表单任务应使用发布路线同步出的表单填表权限规则回填 `fillableUsers`，不新增接口字段。
- Filler source priority: active fill/rework work task -> process form fill permission rule -> route-process fill assignment rule.

## Auth, Permissions, Validation, and Error Behavior

- 不改变认证、权限、校验或错误映射。
- 不吞异常、不返回默认成功、不引入 fallback。

## Required Config, Services, Fixtures, and Migrations

- Required fixture: 后端测试构造冻结路线动态表单绑定、`formBindingKey`、`MesProEdhrProcessFormPermissionRuleDO(FILL/USERS/152)` 和启用用户。
- Migrations: 无。

## BDD Scenarios

- BDD: 动态表单任务显示工艺路线绑定填写人 -> Given 工艺路线工序绑定的损耗单配置了填写人, When 用户打开批次执行详情, Then 对应损耗单任务 `fillableUsers` 必须返回该配置人员，供右侧单据卡片显示。
- BDD: 主生产表任务填写人逻辑不被破坏 -> Given 主生产表或已有工作任务已有填写人来源, When 批次执行详情组装任务列表, Then 仍优先使用既有工作任务或任务分配规则解析填写人，不被动态表单绑定回填覆盖。

## RED Command and Expected Failure

- `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' surefire:test`
- Expected failure before fix: `expected: <[152]> but was: <[]>`.

## GREEN Command and Passing Result

- `mvn '-Dmaven.compiler.useIncrementalCompilation=false' -DskipTests compile` -> PASS.
- `javac @target\javac-edhr.args` -> PASS for isolated target test-class compilation.
- `mvn '-Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated' '-Dsurefire.useManifestOnlyJar=false' surefire:test` -> PASS, 3 tests.

## Contract or Integration Verification

- Verified `fillableUsers` includes route dynamic form filler `152 / 张可莹（zhangkeying）`.
- Verified active work task candidates still take priority.
- Verified existing route-process assignment rule fallback remains intact when no dynamic form rule exists.

## Observability Touchpoints

- No new logging required; this is response data mapping.

## Blockers and Downstream Skill Needs

- Existing backend files were already modified before this task; this task will preserve unrelated changes.
- Full module `testCompile` is blocked by unrelated legacy WM/MD tests referencing missing classes; not task-owned.
