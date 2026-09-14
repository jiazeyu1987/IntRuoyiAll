# 一线生产提交成功弹框

## 任务目标

- 一线生产正式提交明确成功后，使用页面内弹框显示成功结果，不再依赖全局 toast。
- 成功弹框必须渲染在一线生产最大化根节点内部，普通状态和浏览器最大化状态均不可被页面或全屏层覆盖。
- 用户关闭成功弹框后可继续选择员工、工序并开始下一次独立报工；失败或响应不确定时不得显示成功弹框。

## 里程碑

- [x] M1：核对现有提交、复位、最大化弹框和经验门禁。
- [x] M2：补充成功弹框 BDD 与失败的聚焦静态合同。
- [x] M3：实现全屏根节点内成功弹框及继续报工交互。
- [x] M4：完成聚焦回归、类型检查、真实 Playwright 最大化验证和独立复核。
- [x] M5：完成经验沉淀与任务清理。

## 预期验证

- `node tests/e2e/frontline-production-submit-success-dialog-static.spec.cjs` 先 RED 后 GREEN。
- `node tests/e2e/frontline-production-repeat-submit-static.spec.cjs`、`node tests/e2e/frontline-production-fullscreen-submit-confirm-static.spec.cjs`、`node tests/e2e/frontline-formal-submit-static.spec.cjs` 通过。
- `pnpm ts:check` 通过。
- frontend feature evidence validator 与 self-test 通过。
- Playwright 真实页面在最大化后正式提交：目标 POST 成功，成功弹框可见，`fullscreenElement` 包含弹框，弹框中心 hit-test 命中弹框，关闭后数量为空且正式提交入口恢复。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue IntRuoyiFronted/tests/e2e/frontline-production-submit-success-dialog-static.spec.cjs doc/tasks/20260809-frontline-submit-success-dialog docs/frontend-development.md docs/experience-index.md` 通过。

## 经验门禁

- 命中 `docs/frontend-development.md#Element Plus 全屏弹框挂载门禁`：最大化后必须可见的弹框要位于 fullscreen 元素子树，不能依靠 body overlay 或随机提高全局 z-index。
- 命中 `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`：只有正式 POST 明确成功并完成下一会话复位后才显示成功弹框；失败保留原草稿且不得伪造成功。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：新增聚焦合同稳定锁定模板位置、状态、提交时序和样式层级。
- Blocker：若成功弹框只能挂到 body、需要退出全屏才能显示，或失败路径会打开弹框，则停止实现。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；弹框与现有确认弹框同属最大化根节点内的页面状态，不依赖浏览器全屏层外的全局消息组件。
- `是否存在临时补丁或绕过`：否。
- `测试数据变更`：真实 E2E 仅在已确认的本机测试租户使用任务可追踪账号完成一次正式提交；正式报工、事件和签名作为审计事实保留。

## Current Status

completed：提交成功弹框、最大化层级、继续报工复位和失败隔离均已实现并通过真实 Playwright；经验沉淀和任务清理 preview/apply 无 blocked 或 warning。
