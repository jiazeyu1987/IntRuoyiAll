# 生产组长报工明细物料真实名称显示

## Task Goal
只修复生产组长报工管理展开明细中物料标题显示为“物料 1 / 物料 2”或“物料名称未记录”的问题，改为显示一线生产提交数据或正式报工物料表中的真实物料名称。

## Milestones
- [x] 定位展开明细物料标题来源与提交快照字段
- [x] 先补最小静态合同复现“物料 1”占位问题
- [x] 修复新提交快照，保存正式物料名称字段
- [x] 修复时间线接口，旧记录从正式报工物料表补齐物料名称
- [x] 运行定向静态、后端和类型验证并记录结果

## Expected Verification
- 定向静态合同 RED/GREEN：报工明细标题优先显示真实物料名，不显示“物料 N”占位。
- 后端单测证明保存报工事件时 `materialDetails` 快照带真实 `materialName`。
- 后端单测证明旧 raw_payload 缺名时，时间线接口从 `mes_pro_feedback_material` 补齐 `materialName`。
- 前端类型检查通过。
- 本轮文件 `git diff --check` 通过。

## Design Constraints Check
- 仅处理本轮截图红框标题显示问题，不处理设备参数、提交链路或其他历史问题。
- 显示字段使用正式响应、提交快照或正式报工物料表中的物料名称；内部编码、ID 只作为身份，不作为可见名称回退。
- 不添加 mock 或默认成功路径。

## Current Status
completed - 实现提交 `5c969a5ff` 已进入 `int_main`，cleanup apply 已删除本任务临时 evidence，保留 task/execution/verification 记录。

## Cleanup Candidates
- doc/tasks/20260905-team-leader-material-real-name-display/backend-api-evidence.md
- doc/tasks/20260905-team-leader-material-real-name-display/frontend-feature-evidence.md
