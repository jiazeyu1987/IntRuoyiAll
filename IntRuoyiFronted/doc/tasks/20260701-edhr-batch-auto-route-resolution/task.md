# 任务：eDHR 批次执行自动识别工艺路线（前端）

- Task ID: `20260701-edhr-batch-auto-route-resolution`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

移除 eDHR 批次执行创建弹窗中的路线ID输入与相关误导文案，前端只让用户选择工单和批次号，把路线解析职责完全交给后端。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\task.md`
- 状态：`blocked`
- 处理说明：上一前端任务已显式阻塞，不影响本轮 eDHR 前端修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 本次保持现有弹窗布局，只移除路线输入和相关说明，不做额外视觉重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。前端不再要求用户理解内部 routeId，也不保留“可选，后端解析”的错误合同文案。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 创建弹窗不再显示路线ID -> Given 用户打开 eDHR 批次执行创建弹窗 / When 页面渲染 / Then 只显示工单、批次号和备注，不显示路线ID输入及其提示。`

## Milestones

1. M1：确认当前弹窗中路线相关输入、文案和依赖逻辑。`completed`
2. M2：移除路线输入与相关前端依赖。`completed`
3. M3：补 frontend evidence。`completed`

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260701-edhr-batch-auto-route-resolution\frontend-feature-evidence.md`

## Current Blockers

- 无。前端 required verification 当前已通过；前端单仓提交仅受整任务后端门禁阻塞。

## Final Verification Result

- 已移除创建弹窗中的路线ID输入、相关提示和前端提交字段。
- 创建弹窗现在只保留工单、批次号和备注，路线解析职责完全交给后端；同时保留 `EdhrBatchExecutionOpenOrCreateReqVO.routeId?: number` 以兼容 `FeedbackForm` 等既有内部任务上下文入口。
- `validate_frontend_feature.py` 已通过，closeout preview 已确认仅 `frontend-feature-evidence.md` 为默认可清理候选。
- 已修复 `BatchExecutionTemplateSimulatePage.vue` 本地模板模拟态的非法 `recordCategory: 'TEMPLATE'`，`pnpm ts:check` 已在 8GB 堆内存下通过。
