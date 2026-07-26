# Bug Regression Evidence

## Bug Summary

当前填写人 `zhangkeying` 从个人控制台点击 eDHR 待办 `进入处理` 时，后端返回“当前 eDHR 批次状态不允许该操作”。

## Expected Behavior

批次未关闭、未归档、未驳回、未作废时，当前填写/返工工作任务责任人应能通过个人控制台正式打开动态路线表单；关闭、归档、驳回、作废批次继续 fail-fast 阻断。

## Reproduction

- 真实用户路径：`zhangkeying` 登录个人控制台，点击 eDHR 待办 `进入处理`，页面提示“当前 eDHR 批次状态不允许该操作”。
- 后端 RED：`openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller` 在修复前复现同一错误码。

## Root Cause

`openTask` 对 `TASK_STATUS_APPROVED` 的任务只放行传统 execution 已提交表单；动态路线表单使用 Form Center 上下文，通常没有传统 `executionId`。因此已提交动态路线表单在批次关闭前由当前填写人再次打开时，被旧逻辑错误归类为批次状态非法。

## Regression Test

- Added: `MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller`
- Coverage: 动态路线表单、完整 Form Center 上下文、当前填写 work task、无传统 execution 创建、返回 `formCenterInstanceId/formTemplateId/workTaskId`。

## RED:

`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: FAIL，抛出 `ServiceException: 当前 eDHR 批次状态不允许该操作`。

## GREEN:

`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: PASS，`Tests run: 1, Failures: 0, Errors: 0`。

## Verification

`mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_allowsApprovedDynamicRouteFormBeforeCloseForCurrentFiller+openTask_allowsApprovedOrdinaryFillCompletedBeforeReleaseForHistoricalFiller+openTask_rejectsClosedBatch" "-Dsurefire.failIfNoSpecifiedTests=false" test`

Result: PASS，`Tests run: 3, Failures: 0, Errors: 0`。

## Risk

修复不引入 fallback；动态路线表单关闭前再打开仍要求完整 Form Center 上下文、真实 `workTaskId` 归属、填写/返工任务类型和责任人权限校验。

## Blockers

真实 Playwright E2E 尚未完成：本地 `zhangkeying` 账号没有可用密码来源。默认密码来源在 `芋道源码/zhangkeying` 与 `测试租户/zhangkeying` 下均登录失败。需要用户提供测试账号密码，或授权临时重置并恢复本地测试账号密码。
