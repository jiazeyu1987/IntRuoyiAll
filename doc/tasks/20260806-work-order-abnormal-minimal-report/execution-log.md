# Execution Log

## User Intent

- 用户要求“上报异常不需要工序ID和异常原因，只需要订单号和异常说明”。

## BDD

- BDD: 订单异常上报只填订单号和异常说明 -> Given 生产组长进入“异常”页签 / When 选择订单号并填写异常说明后提交 / Then 页面不展示工序ID和异常原因，提交 payload 只包含订单号对应的 `workOrderId` 与 `abnormalDescription`。
- BDD: 后端异常上报接口不要求工序和原因 -> Given 请求体只有 `workOrderId` 与 `abnormalDescription` / When 调用 `work-order/abnormal/report` / Then 后端按登录用户标记并上报，工序和异常原因字段保持空值。

## Command Intent

- 读取前端、后端、E2E、PowerShell、任务收尾规则与技能合同。
- 定位页面：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 定位 API：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`、`MesWorkOrderAbnormalReportReqVO`、`MesWorkOrderAbnormalReportServiceImpl`。

## Milestone Updates

- in_progress: 已创建任务目录并记录 BDD、预期验证和适用门禁。

## Verification Evidence

- RED: pending。
- GREEN: pending。

## Blockers

- 当前主工作区已有大量非本任务 tracked、untracked 与 staged 改动，其中 `TeamLeaderWorkbenchPage.vue` 已有 staged 变更；本任务只做精确补丁，不执行宽泛暂存、提交或回滚。
