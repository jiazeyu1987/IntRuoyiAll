# Active Order Production Submission Device Detail

## Goal
在活跃订单工序提交详情的“生产提交”主 tab 中显示每条一线生产提交对应的正式设备信息。

## Milestones
- [x] 确认生产提交详情数据源是否已有正式设备字段。
- [x] 写 RED 静态合同锁定后端 VO/API/前端列展示。
- [x] 实现后端正式设备字段透传和前端展示。
- [x] 跑定向静态合同、类型检查、后端编译和证据校验。

## Expected Verification
- 生产提交表格包含“设备”列。
- 设备信息来自正式生产提交/事件读模型字段，不从 PQC、物料、工序名或前端缓存推断。
- 后端模型、VO、Controller、前端 API 类型均包含设备字段。
- 现有 PQC 主 tab 自有工序分组不回退。

## Current Status
completed - 实现提交 `5c969a5ff` 已进入 `int_main`，cleanup apply 已删除本任务临时 evidence，保留 task/execution/verification 记录。

## Cleanup Candidates
- doc/tasks/20260905-active-order-production-submission-device-detail/backend-api-evidence.md
- doc/tasks/20260905-active-order-production-submission-device-detail/frontend-feature-evidence.md

## Design Constraints Check
- 不使用 fallback、mock 或默认成功。
- 不按工序名称推断设备。
- 只处理活跃订单详情生产提交设备展示，不改变正式提交写入链路。
