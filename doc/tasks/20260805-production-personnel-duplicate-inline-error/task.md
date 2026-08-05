# 20260805 生产人员同名错误弹窗内提示

## Task Goal

将新增临时工时的同名错误从全局 `ElMessage.error` 改为“新增人员”弹窗标题栏内的红色提示，并提供：

- 自动消失。
- 手动关闭。
- 修改显示名后清除。
- 退出弹窗后清理。

保留后端错误原文、表单数据和现有新增人员业务链路。

## Milestones

- [x] 创建任务记录并定位错误处理链路
- [x] 新增聚焦回归合同并取得 RED
- [x] 实现弹窗内错误提示和退出机制
- [x] 运行 GREEN、相邻回归和 TypeScript 检查
- [x] 校验 bug regression 和 frontend feature 证据
- [x] 确认实现与回归测试已进入共享基线并推送
- [ ] cleanup 与最终收尾记录

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-management-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-personnel-duplicate-inline-error-static.spec.js doc\tasks\20260805-production-personnel-duplicate-inline-error`

## Current Status

ready_for_closeout

- 弹窗内红色错误、6 秒自动消失、手动关闭、修改显示名清除、关闭弹窗清理和组件卸载清理均已实现并通过验证。
- 2026-08-05 复验：聚焦合同、三个相邻合同、`pnpm ts:check`、bug regression validator 和 frontend feature validator 均通过。
- 本任务实现、测试和初始证据已包含在共享基线提交 `3db8a7030` 中；该提交同时包含其它任务文件，任务记录明确保留此共享提交事实，不将其伪装为独立实现提交。
- `3db8a7030` 已是 `origin/int_main` 的祖先，本任务生产代码和回归测试已推送。
- 当前 Vue 工作区仅剩并发任务新增的 `single-line-toolbar` 变更，本任务不暂存、不提交、不回滚该变更。
- 现已满足 cleanup preview/apply 前置，临时 evidence 将按默认规则清理，三个核心任务文档保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；后端错误仍原样展示在弹窗内。
- `是否从根因和长期维护角度解决`：是；将表单提交错误归属到发起操作的弹窗，而不是全局消息层。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用合同验证弹窗错误归属、自动清理和非全局展示。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：锁定新增人员弹窗 header 与红色错误样式，避免误改其它错误提示。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：不得宽泛提交或推送现有非本任务 ahead 基线。
