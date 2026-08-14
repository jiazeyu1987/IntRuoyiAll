# Verification Report

## Summary

- 已移除一线生产正式提交授权中的 `提交设备/工作站上下文与授权工序不一致` 限制。
- 授权服务不再比较 submittedDeviceId/submittedWorkstationId 与授权候选 expectedDeviceId/expectedWorkstationId。
- 保留正式提交必需校验：请求必填、签名员工一致、授权工序、团队员工和模板一致。
- 提交阶段设备参数校验已保持移除状态。

## Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新测试复现 `PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSubmitAuthorizationTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlineRuntimeConfigProcessScopeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 18, Failures: 0, Errors: 0, Skipped: 0。
- STATIC: service/test 范围内 `PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH` 和错误文案无引用。
- EVIDENCE VALIDATOR: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260808-frontline-submit-remove-context-mismatch-limit\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`

## Result

completed

本次后端行为和任务规则文档均已更新，cleanup preview/apply 通过，临时 backend API evidence 已删除，核心任务记录保留。
