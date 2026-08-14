# 20260804-production-fill-fullscreen-toggle

## Task Goal

将 eDHR 批次“生产填写”固定填写页右上角按钮从默认“主页”改为“最大化”；点击后进入浏览器全屏最大化，按钮文案切换为“主页”，再次点击“主页”退出全屏恢复普通页面。

## Milestones

- [x] M1: 记录 BDD 和当前静态合同 RED 证据。
- [x] M2: 修改生产填写固定面板最大化按钮、全屏状态同步与样式。
- [x] M3: 更新相邻静态合同并完成 GREEN/REGRESSION 验证。
- [x] M4: 补齐验证报告和收尾状态。
- [x] M5: 给非超管账号授予生产组长角色，并完成真实 Playwright E2E。

## Expected Verification

- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-real.e2e.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`（当前因既有页签“历史批记录”缺失先失败，非本任务行为；详见 verification-report）
- `git diff --check -- src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs doc/tasks/20260804-production-fill-fullscreen-toggle/task.md doc/tasks/20260804-production-fill-fullscreen-toggle/execution-log.md doc/tasks/20260804-production-fill-fullscreen-toggle/verification-report.md`

## Current Status

ready_for_closeout

- 实现、定向静态验证和真实生产组长页面 E2E 已完成。
- 已给 `芋道源码/limin` 授予 `mes_team_leader` 角色，账号仍保留既有其它角色；未使用 admin/default 账号替代生产组长路径。
- 真实 E2E 使用 `芋道源码/limin` 打开 `/mes/pro/feedback/edhr-batch-production-fill`，验证默认 `最大化`、全屏后 `主页`、点击恢复后再次 `最大化`。
- E2E PASS 证据：`IntRuoyiFronted/test-results/20260804-production-fill-fullscreen-toggle/result.json`，写请求数 0，目标接口失败 0，pageErrors 0。
- 当前工作区存在大量本任务开始前已有脏改动，且分支已领先 `origin/int_main`；未执行基线提交、任务提交、push 或 cleanup apply，因此不标记 `completed`。

## Applicable Gates

- Frontend static contract isolation: if broader tests fail on unrelated history, use a focused static contract for this production-fill fullscreen behavior and record the unrelated blocker separately.
- Screenshot button/static contract gate: lock the target button scope in `FrontlineFixedTemplatePanel.vue`; do not rewrite routing, save, submit, or permission behavior.
- Element Plus fullscreen gate: if a fullscreen area owns dialogs, dialogs must remain inside the fullscreen subtree and must not rely on body teleport; this task does not move submit/result dialogs.
- PowerShell/encoding gate: all task docs and Chinese text must be written as UTF-8 and commands must not use `&&`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；全屏 API 缺失或调用失败时必须显示明确错误，不做静默降级。
- `是否从根因和长期维护角度解决`：是；在固定生产填写面板上建立正式全屏状态和按钮语义。
- `是否存在临时补丁或绕过`：否。
