# 一线 PQC 重复覆盖身份系统异常修复

## Task Goal

修复一线 PQC 加载活跃订单工序时，将跨生产工序或跨轮次的合法 PQC 任务误判为重复覆盖身份并返回系统异常的问题。

## Milestones

- [x] M1：从 int_main 后端日志定位异常栈和业务身份
- [x] M2：新增完整任务身份回归并取得 RED
- [x] M3：修复覆盖匹配身份
- [x] M4：执行定向后端回归与证据校验
- [ ] M5：完成任务收尾

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcTaskOverlayTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 相关一线 PQC 上下文定向回归
- Bug regression evidence validator
- Backend API evidence validator

## Design Constraints Check

- PQC 任务身份必须包含 `activeOrderId + routeProcessId + processId + regulationVersionId + qaProcessId + qaItemCode + inspectionRuleKey + inspectionType + businessDate + shiftCode + roundNo`。
- 不按第一条任务、排序或默认生产工序消除歧义。
- 真正完整身份重复仍需 fail fast，不能静默去重。
- 不修改数据库 schema 或业务数据。
- 不执行真实 E2E；用户本轮未明确要求。
- 不停止或重启 int_main；用户本轮未授权。
- 不提交、不推送；用户本轮未授权 Git 操作。

## Current Status

in_progress

代码修复与 21 项定向回归已通过；当前运行 Jar 尚未重启，等待用户授权重启 int_main 后端后验证运行态。
