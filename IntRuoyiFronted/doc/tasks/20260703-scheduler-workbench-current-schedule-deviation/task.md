# 任务：同步排产工作台报工偏差新口径

- Task ID: 20260703-scheduler-workbench-current-schedule-deviation
- Created: 2026-07-03
- Current Status: completed

## Task Goal

前端工作台接入后端新的“当次排产偏差”口径与工序明细契约，使卡片和弹窗不再使用旧的任务段口径或瓶颈数据伪装偏差。

## Milestones

1. 建立任务文档并记录经验门禁。completed
2. 补充前端静态 RED 回归，覆盖新契约字段与明细展示。completed
3. 实现工作台卡片/弹窗接入新口径。completed
4. 运行静态回归和类型校验。completed
5. 提交本次前端直接改动。completed

## Expected Verification

- 静态测试覆盖 `reportedDeviationDetails` 契约与“当次排产”文案说明。
- `pnpm ts:check` 通过。

## 经验门禁

- 已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\\ProjectPackage\\Int\\IntPP\\FRONTEND_STYLE.md`。
- 已读取 `frontend-feature-delivery`，前端只消费正式后端契约，不继续使用瓶颈数据代替偏差明细。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，前端与后端统一切换到新的偏差口径。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 等待后端 summary 契约变更落地后联调。

## 完成记录

- 状态：completed。
- 弹窗摘要已切换为“实际报工数量 / 当次排产数量 / 总偏差”。
- 前端已接入 `currentSchedulePlannedQuantity`、`currentScheduleReportedQuantity`、`reportedDeviationDetails`，不再从 `bottlenecks` 推导偏差明细。
- 验证：`node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` PASS；`node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` PASS；`pnpm ts:check` PASS。
