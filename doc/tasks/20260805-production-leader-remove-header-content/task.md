# 20260805 生产组长移除冗余头部内容

## Task Goal

按用户截图删除生产组长页面黄框内的三处内容：

- 页面标题“生产组长”及说明文字。
- 人员管理中的“生产人员档案”标题及说明文字。
- “刷新人员档案”按钮。

保留生产组长功能 Tab、“新增人员”、状态筛选、人员列表和现有业务逻辑。

## Milestones

- [x] 创建任务记录并确认截图范围
- [ ] 编写聚焦静态合同并取得 RED
- [ ] 删除黄框内容
- [ ] 运行 GREEN、相邻回归和 TypeScript 检查
- [ ] 更新验证报告和收尾状态

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-tabs-flat-style-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-function-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-leader-remove-header-content-static.spec.js doc\tasks\20260805-production-leader-remove-header-content`

## Current Status

in_progress

- 只改生产组长页面展示结构和任务专用静态合同。
- 不改 API、后端、数据库、权限、菜单、路由或真实数据来源。
- 共享页面存在其它并发任务改动，提交和推送必须保持任务边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，直接移除冗余展示节点，不使用 CSS 隐藏或条件绕过。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用静态合同锁定本次删除范围，不修改无关大合同绕过并发失败。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：按生产模块 ContentWrap 分块断言目标内容已移除，避免跨块误命中 PQC 标题。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享分支存在非本任务 ahead 提交，不宽泛提交或推送。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：同一 Vue 文件存在并发功能 hunks，提交前必须验证任务边界。
