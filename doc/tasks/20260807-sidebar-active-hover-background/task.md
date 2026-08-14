# 侧边栏选中菜单悬停背景修复

## Task Goal

修复侧边栏已选中菜单在鼠标移到文字上时，文字区域单独变白的问题；选中项悬停时保持整行原有选中背景色不变。

## Milestones

- [x] 确认截图现象与侧边栏样式根因
- [x] 记录 BDD 并完成 RED 验证
- [x] 实施最小样式修复并完成 GREEN/回归验证
- [x] 完成任务证据、清理和收尾

## Expected Verification

- 聚焦静态契约证明菜单标题子元素不再单独设置 hover 背景。
- 聚焦静态契约证明已选中菜单行 hover 时继续使用选中背景色。
- `pnpm ts:check` 通过。
- 目标 Vue/SCSS 文件的 Stylelint 检查通过。
- `git diff --check` 对本任务文件通过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，从共享侧边栏菜单样式源移除标题子元素的独立 hover 背景，避免所有菜单复现同类问题。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 前端截图样式块静态契约

- Trigger: 用户基于截图要求调整局部颜色、选中态和 hover 背景。
- Preflight check: 静态契约分别锁定普通菜单行 hover、已选中菜单行 hover 和标题子元素，避免跨样式块误命中。
- Blocker: 契约无法证明标题子元素不再覆盖父级选中背景，或无法证明选中行 hover 仍使用选中背景色时停止。
- Verification: 聚焦静态契约、`pnpm ts:check`、`git diff --check`。
- Forbidden action: 禁止用全局白色/蓝色覆盖、改路由或改业务状态冒充局部样式修复。
- Evidence: `docs/frontend-development.md#前端截图样式块静态契约门禁` 与本任务执行记录。
