# 任务：调整排产工作台报工偏差卡片

- Task ID: 20260703-scheduler-workbench-feedback-deviation-dialog
- Created: 2026-07-03
- Current Status: completed

## Task Goal

按截图要求调整排产工作台顶部指标：删除“今日可用产能”卡片；“报工偏差”卡片改为整数显示；点击“报工偏差”后展示总偏差和各工序偏差信息。

## Milestones

1. 建立任务文档并记录经验门禁。completed
2. 补充前端静态 RED 回归，覆盖删除产能卡、整数偏差、偏差弹窗。completed
3. 实现工作台卡片和偏差弹窗最小改造。completed
4. 运行静态回归和类型/语法验证。completed
5. 记录最终验证结果并按门禁收尾。completed

## Expected Verification

- `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` 通过。
- `node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` 通过。
- 新增静态测试证明“今日可用产能”卡片不再渲染，报工偏差显示整数，点击打开明细弹窗。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：PowerShell 中文读写必须显式 UTF-8；本任务写文件使用 `apply_patch`，命令输出设置 UTF-8。
- 已读取 `docs/experience-index.md`：本任务命中 PowerShell 与前端页面/样式门禁。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持蓝灰操作台风格，不引入大改版。
- 后端工序级偏差明细需要修改 `ruoyi-vue-pro`，但后端最近任务 `20260703-showroom-product-import-target-market-overflow` 仍为 `in_progress`，本轮前端先不打开后端新任务，避免违反任务顺序门禁。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：部分；前端交互先落地，后端工序级偏差明细受未完成后端任务阻塞，已记录为阻塞。
- 是否存在临时补丁或绕过：否；前端弹窗只使用当前 summary 与 bottlenecks 已有契约字段，不伪造后端明细。

## Current Blockers

- 后端工序级偏差明细契约受前一个后端任务未完成阻塞；当前弹窗无法展示真实“按工序报工-计划偏差”明细，只能展示当前接口已有的总偏差与瓶颈/工序相关信息。

## 完成记录

- 状态：completed。
- 已删除顶部指标中的“今日可用产能”卡片。
- “报工偏差”卡片主值改为 `formatIntegerNumber(summary.value.reportedDeviationQuantity)`，按整数显示。
- 点击“报工偏差”打开 `报工偏差明细` 弹窗，展示总偏差、今日报工、已排任务和当前接口可用的工序偏差信息。
- 验证：`node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` PASS；`node tests/e2e/mes-scheduler-workbench-interaction-static.spec.js` PASS；`pnpm ts:check` PASS；task-closeout preview PASS。
- 阻塞说明：真实后端“按工序报工-计划偏差”明细仍受后端未完成任务门禁阻塞，未在本次前端提交中伪造数据。
