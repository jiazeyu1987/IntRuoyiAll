# 文档目录文件夹图标同行展示

## Task Goal

将“文控中心 > 文档目录”表格中代表文件夹的默认三角展开图标调整为文件夹图标，并确保图标与目录名称同行展示；不修改后端、API、权限、菜单、目录数据或既有操作行为。

## Milestones

- [x] 创建聚焦静态契约，先证明当前实现缺少文件夹图标与同行布局。
- [x] 实现文档目录文件夹图标、同行布局和默认三角视觉隐藏。
- [x] 运行聚焦契约、相邻目录契约和 TypeScript 检查，记录验证证据。
- [ ] 完成任务记录、清理与收尾状态更新。

## Expected Verification

- `pnpm e2e:dcc:directory-folder-icon-inline:static`
- `pnpm e2e:dcc:directory-summary:static`
- `pnpm e2e:dcc:directory-lazy-loading:static`
- `node tests/e2e/dcc-directory-expand-actions-toolbar-static.spec.js`
- `pnpm ts:check`

## Current Status

blocked - 实现与聚焦验证已完成；最终提交、推送和 completed 收尾被共享工作区并发脏改、既有相邻契约失败与既有 TypeScript 失败阻塞。

## Blockers

- `node tests/e2e/dcc-directory-expand-actions-toolbar-static.spec.js` 当前失败在既有 `useTreeTableExpand(true)` 断言；本任务未修改该行，目标源码仍为既有 `useTreeTableExpand(false)`。
- `pnpm ts:check` 当前失败在 `src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue` 的产品立项方法缺失，和本任务目录图标改动无文件重叠。
- 共享分支 `int_main` 已领先 `origin` 且存在大量并发脏改与最近提交移动；本任务未执行基线/实现/收尾提交，避免混入非任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整目录表格展示结构和可验证静态契约。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：本任务新增最小静态契约覆盖文件夹图标、同行布局和默认三角隐藏；若全量检查失败于无关历史问题，只记录首个无关 blocker，不用无关失败替代当前需求验证。
- Int 统一前端样式门禁：目录表格保持蓝/中性色、紧凑表格密度和任务型操作台风格，不引入装饰性重设计。
