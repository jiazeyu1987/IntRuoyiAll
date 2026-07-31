# Bug Regression Evidence

## Bug Summary

创建 eDHR 批次执行时，普通批记录填写人规则保存为 `scopeKey=ALL` 且未显式保存 `fillableScopeJson`，工作任务创建链路在生成责任范围快照时直接报错：`eDHR 工作任务责任范围快照无效：scopeKey=ALL`。

## Expected Behavior

当批次工序任务绑定正式批记录表单，且填写人规则为整表 `ALL` 时，系统应从正式批记录报表成员元数据生成可追溯整表 `ranges` 责任范围快照；如果缺少正式定义、版本或报表成员，必须 fail fast，不能用空范围、默认 MAIN、表单槽位或工序开始配置兜底。

## Reproduction

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`MesProEdhrWorkTaskServiceImpl#parseRequiredFillableScope` 对普通批记录规则只读取 `MesProEdhrProcessFormPermissionRuleDO.fillableScopeJson`。但 `scopeKey=ALL` 的正式整表规则可以合法保存为空范围，导致创建工作任务时无法生成责任范围快照并抛出 `PRO_EDHR_WORK_TASK_RESPONSIBILITY_SCOPE_INVALID`。

## Regression Test

- Added `MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank`
- Added `MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_failsFastWhenAllScopeReportMembersMissing`

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `ServiceException: eDHR 工作任务责任范围快照无效：scopeKey=ALL`

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest#createInitialFillTask_buildsAllScopeSnapshotFromReportMembersWhenRuleScopeIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrWorkTaskServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 71 tests, 0 failures, 0 errors

## Risk And Regression Scope

修复范围限定在 `EDHR_PROCESS_FORM_FILLER` 工作任务责任范围快照生成。动态表单槽位仍读取批次任务冻结的 `fillableScopeJson`；普通非 `ALL` 规则和缺少正式报表成员的 `ALL` 规则继续 fail fast。

## Verification

目标单用例和完整 `MesProEdhrWorkTaskServiceImplTest` 均已通过；完整类回归结果为 71 tests、0 failures、0 errors。

## Blockers And Follow-Up

当前分支存在非本任务本地提交领先 `origin/int_main`，最终 closeout/push 需要避免混入无关任务风险。
