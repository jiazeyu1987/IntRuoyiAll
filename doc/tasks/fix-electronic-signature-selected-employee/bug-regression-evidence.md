# Bug Regression Evidence：一线提交电子签名按选择员工校验

## Bug Summary

一线生产正式提交时，设备端登录账号选择实际填写员工后输入电子签名密码，页面提示“当前登录账号必须是实际填写员工，无法完成电子签名”。该逻辑错误地把签名主体绑定到当前登录账号。

## Expected Behavior

电子签名主体应为页面选择的实际填写员工。只要 `signatureEmployeeId == actualEmployeeId`，且输入密码匹配该选择员工的电子签名授权数据，就允许正式提交；如果签名员工与实际填写员工不一致或密码不匹配，必须拒绝并不写入正式报工链路。

## Reproduction

- `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> RED 时失败，证明前端仍存在登录账号拦截。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED 时失败，证明后端签名服务缺少选择员工 actor 入口。

## Root Cause

- 前端正式提交前置断言要求 `signatureEmployeeId === currentLoginUserId`。
- 后端提交服务要求实际员工/签名员工等于登录用户。
- 生产提交签名服务只按 `SecurityFrameworkUtils.getLoginUserId()` 记录签名，无法代表选择员工落签名快照。

## Regression Tests

- `IntRuoyiFronted/tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` 覆盖前端禁止当前登录账号拦截。
- `MesProFrontlineFeedbackSubmitServiceTest.shouldSignAsSelectedEmployeeWhenDeviceAccountIsLoggedIn` 覆盖设备端登录账号与选择员工不同但签名通过。
- `MesProBatchRecordExecutionSignatureServiceTest.recordProductionSubmitSignature_usesSelectedEmployeeActorInsteadOfLoginUser` 覆盖签名记录使用选择员工 actor 和快照。
- `MesProFrontlineFeedbackSubmitServiceTest.shouldRejectClientSuppliedSignatureIdBeforeWritingAnyRecord` 覆盖客户端不得预传签名 ID。

## RED

- RED: `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> FAIL, expected reason: 前端仍包含登录账号拦截文案和 `signatureEmployeeId/currentLoginUserId` guard。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `recordProductionSubmitSignature(Long, String, String)` 尚不存在。

## GREEN

- GREEN: `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS。
- GREEN: `pnpm e2e:frontline-formal-submit:static` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 22 tests, 0 failures, 0 errors。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 31 tests, 0 failures, 0 errors。
- GREEN: `git diff --check` -> PASS。

## Verification

最终验证包括新增前端静态回归、既有一线正式提交静态契约、后端定向 22 个测试、后端相邻 31 个回归测试、残留源码扫描和 `git diff --check`。全部通过，旧登录账号拦截仅保留在回归测试的禁止文案断言中。

## Risk And Regression Scope

验证范围覆盖前端静态契约、后端一线正式提交服务、生产提交签名服务和相邻正式提交回归。未运行真实 Playwright 写入型 E2E，因为本次未启动本地运行态或创建测试租户数据。

## Blockers And Follow-Up

无当前阻塞。后续若需要发布前验收，可在确认测试租户、账号、签名密码和运行态后补跑一线生产真实提交 Playwright 写入链路。
