# FIFO 分配弹框生产信息列

## Task Goal

在生产组长报工分配弹框中增加“要生产数量”和“生产系数”两列，并将弹框宽度扩大约 30%，方便组长在 FIFO 自动分配或手工调整时同时看到目标订单生产信息。

## Milestones

- M1：确认现有弹框、接口类型和测试锚点。`[完成]`
- M2：用 BDD 和静态契约固定新增列与宽度要求。[完成]
- M3：实现前端展示和类型扩展。[完成]
- M4：运行前端定向验证和收尾。[完成]

## Expected Verification

- 静态契约先 RED 后 GREEN，覆盖弹框宽度、两个新增列、显示数据来源和提交载荷不新增字段。
- 前端类型检查通过。
- TypeScript 检查通过或明确记录与本任务无关的既有阻塞。
- 真实 E2E：用户于 2026-08-10 明确授权跳过，改以静态契约、TypeScript 和后端构建作为合并前验证。
- 本任务只改前端展示和类型，不改变分配保存逻辑、FIFO 算法或后端接口行为。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接使用活跃订单正式响应字段展示，不改写业务计算。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout：已按用户授权跳过真实 E2E，完成静态契约、TypeScript 检查和后端构建，准备提交并合并到 int_main。

## Worktree Evidence

- Worktree：`D:/IntRuoyiWorktree/allocation-dialog-production-columns`
- Branch：`codex/allocation-dialog-production-columns`
- Runtime profile：`int_main`
- Slot：`8`
- Frontend port：`8089`
- Backend port：`48089`
