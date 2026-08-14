# 新建生产人员负责范围误报回归证据

## Bug

生产组长在“新增人员”相关操作中看到 `班组长不在该员工或工序的负责范围内`，该文案把“员工负责范围”和“工序负责范围”混在一起，容易让用户理解成新增员工前必须已经负责该员工。

## Expected

新增正式工或临时工生产人员档案时，只建立当前生产组长名下人员档案，不校验该员工是否已经在负责员工范围内；保存工序员工关系、报工列表/详情/复核/确认仍按员工或工序负责范围拦截越权。

## Reproduction

通过 `MesTeamLeaderScopeServiceTest` 和 `MesTeamLeaderRuntimeConfigServiceTest` 复现职责边界：新增人员路径断言不调用 `assertCanAccessEmployee` / `assertCanMaintainProcess`，范围拒绝路径断言返回目标化错误信息。

## Root Cause

后端范围拒绝统一使用 `PRO_PROCESS_POOL_TEAM_SCOPE_DENIED`，文案为“员工或工序”，导致独立的工序绑定越权错误被误解为新增员工阶段要求员工已在负责范围；测试也未锁定新增正式工/临时工创建路径不得触发范围校验。

## RED

RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `PRO_PROCESS_POOL_TEAM_TARGET_SCOPE_DENIED` 尚未定义，目标化错误信息测试先失败。

## GREEN

GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。

## Verification

- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamEmployeeBindingServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 35, Failures: 0, Errors: 0, Skipped: 0。
- 代码核对：`/employee-profile/temporary/create`、`/employee-profile/formal/link` 不保存工序关系；`/process-employee-binding/save` 继续调用 `assertCanMaintainProcess`。

## Blockers

当前代码和验证无阻塞；仓库存在大量非本任务脏改动，提交/推送收尾需避免混入无关任务文件。
