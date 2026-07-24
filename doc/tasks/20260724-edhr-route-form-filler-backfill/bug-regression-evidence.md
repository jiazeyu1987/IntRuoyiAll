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

- `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test`
- 当前未到达断言：测试上下文被无关 `NoClassDefFoundError: MesDvRepairPageReqVO` 阻断。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated test` -> BLOCKED, 测试上下文未能启动。

## GREEN Command and Passing Result

- `mvn -pl yudao-module-mes -DskipTests compile` 在路线绑定回填逻辑写入时通过。
- 目标回归测试尚未获得 GREEN；后续同文件并发修改覆盖了本次实现，且其他未归属改动引入编译错误。
- GREEN: Pending, 不将编译通过替代目标回归测试通过。

## Verification

- `mvn -pl yudao-module-mes -DskipTests compile` -> PASS（并发覆盖发生前）。
- 目标 Maven 测试 -> BLOCKED（无关测试上下文/编译问题）。
- `git diff --check`（本次触达服务与测试文件）-> PASS。

## Risk and Regression Scope

- Must not alter existing main batch-record task fillable-user priority.
- Must not infer filler from current login user, creator, updater, or generic owner role.

## Blockers and Follow-up Actions

- 等待用户确认是否基于当前最新 `MesProEdhrBatchExecutionServiceImpl.java` 重新合并本次路线绑定填写人回填。
- `MesProBatchRecordExecutionFieldAuditServiceImpl.java` 的未归属编译错误需要其所属任务完成或用户授权处理后，才能重跑目标 Maven 测试。
