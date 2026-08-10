# Verification Report

## Summary

- 时间：2026-08-08 22:52 +08:00。
- 账号：`芋道源码/admin`。
- 页面路径：`http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-production-fill`。
- 目标写接口：`POST /admin-api/mes/pro/feedback/frontline/submit`。
- 结果：HTTP 200，业务码 `1040760111`，业务失败符合现场报错类型。

## Page Evidence

- 页面工序：`1. 粗洗工序`。
- 页面员工：`112`，`actualEmployeeId=980023`，`signatureEmployeeId=980023`。
- 页面设备卡片：`超声波清洗机`。
- 填写产出数量：`1`。
- 页面错误提示：`提交设备/工作站上下文与授权工序不一致，submittedDeviceId=980009, submittedWorkstationId=980010, expectedDeviceId=41, expectedWorkstationId=980010`。

## Request And Response Evidence

- 提交载荷 `processPoolContext`：`routeId=922119`，`routeProcessId=980661`，`processId=922985`，`workstationId=980010`，`deviceId=980009`，`templateType=PRODUCTION_SIMPLIFIED`。
- 提交载荷 `feedbackPayload.selectedDevice`：`deviceId=980009`，`deviceCode=B09393`，`deviceName=超声波清洗机`。
- 后端授权候选：`routeId=922119`，`routeProcessId=980661`，`processId=922985`，`workstationId=980010`，`deviceId=41`，`deviceCode=A03190`，`deviceName=球囊成型机`，`processName=粗洗工序`。
- 运行态 `runtimeConfig.devices`：只返回 `deviceId=980009`，`deviceCode=B09393`，`deviceName=超声波清洗机`。

## Root Cause

- 本次失败不是工位不一致：提交和授权候选的 `workstationId` 都是 `980010`。
- 失败点是设备 ID 域不一致：前端提交的 `deviceId=980009` 来自一线运行态返回的班组设备卡片；后端授权候选期望的 `deviceId=41` 来自 route-start 生产组长候选的工作站正式机台。
- 当前后端提交授权在 `MesFrontlineSubmitAuthorizationServiceImpl.authorize(...)` 中直接比较 `command.deviceId()` 与 `process.deviceId()`，没有区分 `mes_process_pool_team_device.id` 与 `mes_dv_machinery.id` 两个来源，导致同一工序/工位下的合法页面选择被判为上下文不一致。

## Verification Commands

- `node doc\tasks\20260808-frontline-production-admin-submit-diagnosis\frontline-admin-submit-diagnosis.mjs` -> PASS，真实页面路径发出一次目标正式提交请求并复现业务失败。
- 结果文件：`doc/tasks/20260808-frontline-production-admin-submit-diagnosis/frontline-admin-submit-diagnosis-result.json`。

## Fix Direction

- 修复时应统一一线正式提交授权与运行态设备卡片使用的设备身份来源，至少保证 `authorizedCandidate.deviceId`、`runtimeConfig.devices[].deviceId`、`processPoolContext.deviceId`、`selectedDevice.deviceId` 属于同一 ID 域。
- 不能用前端隐藏设备、空设备成功、默认设备或跳过授权来绕过；应在后端 route-start 生产组长链路中正式建模工作站机台与班组设备的关系，或让授权候选使用与运行态提交一致的班组设备 ID。
