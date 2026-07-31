# Bug Regression Evidence

## Bug Summary

测试管理中的闭环测试会反复使用固定名称创建和删除临时测试项。当前删除使用逻辑删除；同名测试项第二次删除时，两条记录都会占用 `(tenant_id, name, deleted=1)`，导致唯一键冲突。

## Expected Behavior

在没有运行中执行记录时，测试项及其检查点应被删除；之后可以继续使用同一名称完成新建、修改、查询和删除闭环。

## Reproduction

`mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest#deleteCase_allowsRepeatedCreateAndDeleteWithSameName" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`CodexTestCaseServiceImpl.deleteCase()` 调用了 MyBatis Plus `deleteById`，对带逻辑删除字段的测试项执行 `UPDATE deleted=1`。测试项唯一键包含 `deleted`，因此第二条同名记录逻辑删除时与第一条已删除记录冲突。

## Regression Test

`CodexTestCaseServiceImplTest#deleteCase_allowsRepeatedCreateAndDeleteWithSameName`

## RED

RED: 目标测试失败，第二次删除抛出 `DuplicateKeyException`。

## GREEN

GREEN: 测试项 Mapper 增加显式物理删除，服务删除流程改用该方法。目标回归测试通过。

## Verification

`CodexTestCaseServiceImplTest`、`CodexTestExecutionServiceImplTest` 和 `CodexTestRunnerServiceImplTest` 共 30 个测试全部通过。

## Risk And Regression Scope

修复仅改变测试项配置删除的持久化语义。运行中执行保护保持不变；执行历史继续保存测试项快照；检查点仍在删除测试项前物理删除。

## Blockers And Follow-up

无。
