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
- [ ] 校验技能证据并更新收尾状态

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-duplicate-inline-error-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-management-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-personnel-duplicate-inline-error-static.spec.js doc\tasks\20260805-production-personnel-duplicate-inline-error`

## Current Status

in_progress

- 只修改新增人员弹窗的错误展示和任务专用静态合同。
- 不修改 API、后端校验、错误码、数据库、权限、菜单或真实数据来源。
- 当前分支存在非本任务 ahead 基线提交，提交和推送需单独复核。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；后端错误仍原样展示在弹窗内。
- `是否从根因和长期维护角度解决`：是；将表单提交错误归属到发起操作的弹窗，而不是全局消息层。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用合同验证弹窗错误归属、自动清理和非全局展示。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：锁定新增人员弹窗 header 与红色错误样式，避免误改其它错误提示。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：不得宽泛提交或推送现有非本任务 ahead 基线。
