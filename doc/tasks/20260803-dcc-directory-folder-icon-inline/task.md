# 文档目录文件夹图标同行展示

## Task Goal

将“文控中心 > 文档目录”表格中代表文件夹的默认三角展开图标调整为文件夹图标，确保图标与目录名称同行展示，并支持单击目录名称列红框范围内任意位置展开/折叠；不修改后端、API、权限、菜单、目录数据或既有操作行为。

## Milestones

- [x] 创建聚焦静态契约，先证明当前实现缺少文件夹图标与同行布局。
- [x] 实现文档目录文件夹图标、同行布局和默认三角视觉隐藏。
- [x] 运行聚焦契约、相邻目录契约和 TypeScript 检查，记录验证证据。
- [x] 完成任务记录与验证证据更新。

## Expected Verification

- `pnpm e2e:dcc:directory-folder-icon-inline:static`
- `pnpm e2e:dcc:directory-summary:static`
- `pnpm e2e:dcc:directory-lazy-loading:static`
- `node tests/e2e/dcc-directory-expand-actions-toolbar-static.spec.js`
- `pnpm ts:check`

## Current Status

blocked - 实现与验证已完成；最终 completed 收尾被共享分支提交边界阻塞，任务文件已被并发基线提交 `03646727b` 与大量非本任务文件混入同一提交，不能在不改写历史的前提下形成任务独立实现提交。

## Blockers

- 最新基线提交 `03646727b chore: baseline main worktree before form center merge` 已包含本任务源码、测试和任务文档，同时包含大量非本任务文件；为避免伪装成任务独立提交，未继续创建实现提交或推送。
- 当前分支仍领先 `origin`；在确认是否允许推送该混合基线提交前，不标记 `completed`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整目录表格展示结构和可验证静态契约。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：本任务新增最小静态契约覆盖文件夹图标、同行布局和默认三角隐藏；若全量检查失败于无关历史问题，只记录首个无关 blocker，不用无关失败替代当前需求验证。
- Int 统一前端样式门禁：目录表格保持蓝/中性色、紧凑表格密度和任务型操作台风格，不引入装饰性重设计。
