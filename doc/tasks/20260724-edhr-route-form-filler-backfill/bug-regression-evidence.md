# Bug Regression Evidence

## Bug Summary and Expected Behavior

- Bug: 工艺路线工序绑定的损耗单已配置填写人，但批次执行详情返回的损耗单任务 `fillableUsers` 为空，导致右侧单据卡片显示未配置。
- Expected: 动态表单任务应返回路线绑定配置的填写人。

## Reproduction Command or Path

- Runtime read-only API query confirmed `EDHRB-1784855561493` route config has `张可莹（zhangkeying）` while task `fillableUsers=[]`。

## Root Cause

- 批次详情组装只解析有效填写工作任务、过程表单规则和工序任务分配规则，没有把动态表单任务的 `routeBindingId` 映射回 `MesProRouteFlowProcessBatchRecordDO.candidateSourceType/candidateSourceIds`。冻结路线任务创建时也未完整保留该候选人来源。

## Regression Test Added or Updated

- `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`
- 场景只配置损耗单路线绑定填写人 `USERS/152`，断言批次详情返回用户 ID `152` 和姓名 `张可莹（zhangkeying）`。

## RED Command and Expected Failure

- RED: 运行时复现显示路线绑定配置 `USERS/152`，但批次详情对应损耗单任务 `fillableUsers=[]`。
- RED: 新增回归测试覆盖该失败行为，修复前预期为 `expected: <[152]> but was: <[]>`。

## GREEN Command and Passing Result

- GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromActiveFillWorkTask+detailTask_includesFillableUsersFromAssignmentRuleWhenWorkTaskNotCreated+detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> PASS。

## Verification

- `mvn -pl yudao-module-mes -DskipTests compile` -> PASS。
- 目标 Maven 测试 -> PASS。
- 相邻优先级回归测试 -> PASS。
- `git diff --check`（本次触达服务与测试文件）-> PASS。

## Risk and Regression Scope

- Must not alter existing main batch-record task fillable-user priority.
- Must not infer filler from current login user, creator, updater, or generic owner role.

## Blockers and Follow-up Actions

- None.
