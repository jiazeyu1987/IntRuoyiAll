# 20260805 生产组长移除冗余头部内容

## Task Goal

按用户截图删除生产组长页面黄框内的三处内容：

- 页面标题“生产组长”及说明文字。
- 人员管理中的“生产人员档案”标题及说明文字。
- “刷新人员档案”按钮。

保留生产组长功能 Tab、“新增人员”、状态筛选、人员列表和现有业务逻辑。

## Milestones

- [x] 创建任务记录并确认截图范围
- [x] 编写聚焦静态合同并取得 RED
- [x] 删除黄框内容
- [x] 运行 GREEN、相邻回归和 TypeScript 检查
- [x] 完成 frontend feature evidence 校验
- [ ] 独立提交、推送和 cleanup

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-tabs-flat-style-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-function-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-leader-remove-header-content-static.spec.js doc\tasks\20260805-production-leader-remove-header-content`

## Current Status

blocked

- 黄框内容已删除，任务专用合同、相邻 Tab/新增人员合同、TypeScript 检查和 evidence validator 均通过。
- 并发任务生成基线提交 `f6ea8f545 chore: preserve dirty worktree baseline`，将本任务 Vue 改动、静态合同、`task.md` 和 `execution-log.md` 与大量非本任务前后端改动一起提交。
- 当前 `int_main` 领先 `origin/int_main` 1 个混合基线提交；禁止将该提交冒充成本任务实现提交或未经边界复核直接推送。
- 因实现已被混合基线提交吞入，无法形成严格独立的任务实现提交；未运行 cleanup apply，临时 evidence 保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，直接移除冗余展示节点，不使用 CSS 隐藏或条件绕过。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用静态合同锁定本次删除范围，不修改无关大合同绕过并发失败。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：按生产模块 ContentWrap 分块断言目标内容已移除，避免跨块误命中 PQC 标题。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享分支存在非本任务 ahead 提交，不宽泛提交或推送。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：同一 Vue 文件存在并发功能 hunks，提交前必须验证任务边界。
