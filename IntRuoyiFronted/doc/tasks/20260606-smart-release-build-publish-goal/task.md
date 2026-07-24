# 任务：Smart Release 前端构建回归修复

## Goal

修复 `build-release` 过程中前端发布构建失败的问题，确保发布包构建链路可以继续执行。

## Scope

- 复现并记录 `HallListTable.vue` 的 ESLint 构建失败。
- 最小化修复 Vue 模板标签写法。
- 运行前端发布构建验证。

## Non-Scope

- 不调整展厅管理页面交互、样式或业务逻辑。
- 不修改未跟本次构建失败相关的 MES 未跟踪文件。
- 不使用关闭 ESLint、跳过前端构建等绕过方式。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；构建失败必须显式失败，修复规则违规本身。
- `是否从根因和长期维护角度解决`：是；按 Vue lint 规则修复模板标签闭合方式。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 前端发布构建通过 -> Given 展厅管理表格包含音频预览列 / When 运行 Vite test 模式发布构建 / Then ESLint 不因 HTML 自闭合规则失败，构建继续执行。

## Milestones

- [x] M1：复现构建失败并定位 ESLint 规则。
- [x] M2：修复模板标签写法。
- [x] M3：运行前端构建回归验证。

## Expected Verification

- `node node_modules/vite/bin/vite.js build --mode test` 通过。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260606-smart-release-build-publish-goal/bug-regression-evidence.md`
