# 任务：修正排产工作台报工偏差为当次排产口径

- Task ID: 20260703-scheduler-workbench-current-schedule-deviation
- Created: 2026-07-03
- Current Status: completed

## Task Goal

将排产工作台“报工偏差”从“当天任务段报工数量 - 当天任务段数量”改为用户直觉的“当次排产实际报工数量 - 排产数量”，避免按工序或跨天任务段重复累计；同时为前端弹窗提供各工序偏差明细。

## Milestones

1. 建立任务文档并记录经验门禁。completed
2. 补充后端 RED 回归，证明旧口径错误且缺少工序明细。completed
3. 实现后端工作台 summary 新口径与工序明细契约。completed
4. 运行后端目标测试并记录 RED/GREEN。completed
5. 提交本次后端直接改动。completed

## Expected Verification

- `MesProSchedulerWorkbenchServiceImplTest` 覆盖：
  - 总偏差来自有效排产工单层的实际报工数量与排产数量差值。
  - 工序明细来自有效排产工序快照的 planned/reported/deviation。
- 前端可消费新的 `reportedDeviationDetails` 契约，不再依赖瓶颈数据伪装工序偏差。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：PowerShell 中文/SQL 路径必须显式 UTF-8。
- 已读取 `docs/experience-index.md`：本轮命中 PowerShell 与前后端交付门禁。
- 已读取 `bug-regression-fix-loop`、`backend-api-delivery`：先补 RED 再做最小实现，不得继续沿用错误口径。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，改为排产工单层总偏差 + 工序层明细，消除按任务段和按工序重复累计的口径错误。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 无。

## 完成记录

- 状态：completed。
- 已将工作台总偏差改为“当前有效排产工单（已排产/生产中）的实际报工数量 - 排产数量”。
- 已新增 `currentSchedulePlannedQuantity`、`currentScheduleReportedQuantity`、`reportedDeviationDetails` 契约字段。
- 工序明细改为直接返回有效排产工序快照的 planned/reported/deviation，不再让前端复用瓶颈数据伪装偏差明细。
- 验证：`mvn -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchServiceImplTest" test` PASS。
