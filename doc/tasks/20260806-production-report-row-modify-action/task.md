# 生产组长报工行操作改为修改

## Task Goal

将生产组长“报工管理”列表行操作中的“标记异常”改为“修改”，点击后进入正式原始记录修改弹窗，用于修改写错的报工单；保留独立“异常”模块的异常上报能力。

## Milestones

- [ ] M1 任务文档与适用门禁建立
- [ ] M2 RED 静态合同覆盖按钮文案与点击行为
- [ ] M3 前端实现最小正式修改入口
- [ ] M4 GREEN 与回归验证
- [ ] M5 收尾、经验沉淀、提交与推送

## Expected Verification

- `node tests/e2e/production-leader-report-row-modify-action-static.spec.cjs`
- `node tests/e2e/team-leader-pqc-review-gate-static.spec.js`
- `node tests/e2e/production-leader-function-tabs-static.spec.js`
- `pnpm ts:check`
- `git diff --check`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，行级按钮直接绑定正式原始记录修改入口，不再用异常上报预填替代修改。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端按钮文案与行为一致性门禁：静态合同必须同时锁定按钮可见文案、稳定 `data-*` 锚点、`@click` 新正式动作，并禁止旧点击方法继续绑定。
- 严格无 fallback：不得用 toast、文案替换或异常预填模拟修改入口。

## Baseline

- Dirty worktree baseline commit before this task: `175ddfda1`.
- After baseline, unrelated concurrent changes remained in `IntRuoyiFronted/src/views/mes/pro/route/RouteFlowGraphDesigner.vue`, `IntRuoyiFronted/tests/e2e/mes-route-start-production-leaders-static.spec.js`, `doc/tasks/20260806-route-start-production-leader-top-save/execution-log.md`, and `doc/tasks/20260806-hide-review-copy-columns/`; this task will not stage or modify those files.
