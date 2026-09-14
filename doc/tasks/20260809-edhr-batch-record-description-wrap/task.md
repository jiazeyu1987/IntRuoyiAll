# Task: 批记录测试描述列超宽换行

## Task Goal

让“批记录测试”页面三张职责/任务表格的“描述”列在内容超过列宽时自动换行并完整显示，不影响其它列、操作行为或数据契约。

## Milestones

- [x] M1 定位目标页面、三个描述列和现有溢出行为。
- [x] M2 先新增描述列换行静态合同并取得 RED 证据。
- [x] M3 实施三个描述列的局部换行样式并取得 GREEN 证据。
- [x] M4 收敛较窄桌面描述列默认宽度，避免固定操作列遮挡，并完成真实页面复验。
- [x] M5 完成相关回归和任务收尾。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-description-wrap-static.spec.cjs`
- `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
- `pnpm ts:check`
- Playwright 真实页面检查常用桌面和较窄桌面视口下描述内容换行、无水平越界。
- `git diff --check`（仅任务相关文件）。
- `frontend-feature-delivery` evidence validator。
- `task-closeout-cleanup` preview/apply。

## Experience Gate

- `docs\experience-index.md` 存在。
- 已读取 `docs\e2e-rules.md#静态合同与真实 E2E 同步门禁`：窄范围页面缺陷使用聚焦静态合同，避免宽合同中的无关失败影响本任务证据。
- 已读取 `docs\e2e-rules.md#Element Plus 选择框显示门禁` 与 `#顶部固定信息栏真实视口边界门禁` 的通用显示原则：用户可见长文本不能依赖省略或 tooltip，应通过真实视口和 DOM 样式证据确认完整显示。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接修正目标描述列的 Element Plus 溢出配置和局部样式，不改变数据或交互链路。
- 是否存在临时补丁或绕过：否。

## Current Status

completed - 描述列换行、较窄桌面边界、聚焦/相邻合同、类型/样式检查、真实页面验证、经验沉淀和 cleanup 均已通过。

## Cleanup Candidates

- doc/tasks/20260809-edhr-batch-record-description-wrap/frontend-feature-evidence.md
- doc/tasks/20260809-edhr-batch-record-description-wrap/verify-description-wrap.cjs
- output/playwright/20260809-edhr-batch-record-description-wrap/
