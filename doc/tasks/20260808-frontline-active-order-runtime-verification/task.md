# 一线生产 activeOrder 运行态限制验证

## Task Goal

验证报错 `一线提交身份上下文缺少必填字段：productionSubmitContext.activeOrder routeId=922119` 是否仍来自当前源码或本地运行态，并确认一线生产提交不再强制依赖工艺路线、工单、生产任务、物料、记录本或 activeOrder 上下文。

## Milestones

- [x] 建立需求到验证项清单，记录适用门禁。
- [x] 静态核对当前源码不再包含一线生产 `productionSubmitContext.activeOrder` / `requireSingleActiveOrder` 前置限制。
- [x] 运行 MES 一线运行态相关后端回归测试。
- [x] 检查本机 `48081` 运行态归属、健康状态与是否存在旧 Jar / 旧编译产物风险。
- [x] 输出验证结论、剩余阻塞和后续处理建议。

## Expected Verification

- `rg "productionSubmitContext\\.activeOrder|activeOrder routeId|requireSingleActiveOrder" IntRuoyiBackend\yudao-module-mes\src\main IntRuoyiBackend\yudao-module-mes\src\test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineDeviceParameterValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `Get-NetTCPConnection -LocalPort 48081 -State Listen` 并核对 owning process 命令行。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`

## Applicable Gate Summary

### 一线生产正式提交 activeOrder 限制移除门禁

- Trigger: 一线生产正式提交、`productionSubmitContext.activeOrder`、`routeId=922119`、无工单生产提交、`requireSingleActiveOrder`。
- Preflight check: 一线生产运行态和正式提交不得解析、匹配或要求 `productionSubmitContext.activeOrder`、生产工单、生产任务、产品物料或开启记录本；运行态只应使用路线、路线工序、MES 工序、工作站、实际员工和生产组长审批人上下文。
- Blocker: 当前源码或运行态仍要求 activeOrder / 工单 / 任务 / 物料 / 记录本，或本地 `48081` 运行的不是当前项目可信 Jar。
- Verification: 后端回归覆盖无 activeOrder / 无工单 / 无任务 / 无记录本仍返回生产提交上下文；静态扫描确认禁止恢复 activeOrder 匹配；本机运行态确认健康且进程归属清晰。
- Forbidden action: 禁止用旧运行 Jar、API-only、默认工单、默认任务、默认物料、默认记录本、空成功或前端隐藏错误冒充正式修复。
- Evidence: `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本次只验证是否仍存在旧 activeOrder 前置限制，不新增绕过。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260808-frontline-active-order-runtime-verification/task.md
- doc/tasks/20260808-frontline-active-order-runtime-verification/execution-log.md
- doc/tasks/20260808-frontline-active-order-runtime-verification/verification-report.md

## Final Verification

- cleanup preview/apply: PASS; no delete candidates, no blocked paths.
- task-owned detached build worktree removed: `D:\IntRuoyiWorktree\20260808-frontline-active-order-runtime-verification-backend` -> `Test-Path=False`。
- final runtime: `48081` PID `62116`, health `UP`, runtime Jar `backend-latest-20260808-1802-frontline-active-order.jar`。
