# 工艺路线新增表单点击后数量刷新

## Task Goal

修复工艺路线流转关系图“表单槽位”节点数量徽标：用户点击右侧“新增表单”后，即使新行尚未选择模板，节点右上角数量也应立即按右侧动态表单行数从 `1` 变为 `2`。

## Milestones

- [x] 建立任务记录并复查现有新增表单计数链路。
- [x] 写入聚焦回归合同，复现新增空行仍被数量 helper 排除的问题。
- [x] 修改节点数量 helper，使非 `MAIN` 动态槽位行立即计数。
- [x] 运行目标静态合同、相邻边框回归和类型检查。
- [ ] 完成任务证据与收尾验证。

## Expected Verification

- `node tests/e2e/mes-route-flow-form-slot-count-badge-static.spec.js`
- `node tests/e2e/mes-route-flow-binding-border-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## 经验门禁

- 前端聚合新增默认分类门禁：新增子项默认分类必须进入当前聚合字段的可统计范围。
- 前端静态契约隔离门禁：使用当前最小静态合同锁定点击新增后的数量口径。
- 静态合同与真实 E2E 同步门禁：更新静态合同后必须重跑目标合同。
- 同文件并行改动选择性暂存门禁：当前分支有未推送/并行改动，提交前需只暂存本任务 hunks。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修正节点数量 helper 的计数口径，使其与右侧动态槽位行一致。
- `是否存在临时补丁或绕过`：否。

## Final Verification Result

- PASS：目标静态合同、边框回归、布局回归、`pnpm ts:check`、bug evidence 校验、经验门禁更新、cleanup preview/apply 均通过。
- BLOCKED：当前 `int_main` 已有 20 个非本任务 ahead 提交，直接 `git push origin int_main` 会同时发布并行任务提交；本任务未执行推送。

## Cleanup Keep

- doc/tasks/20260726-route-flow-add-form-click-count/task.md
- doc/tasks/20260726-route-flow-add-form-click-count/execution-log.md
- doc/tasks/20260726-route-flow-add-form-click-count/bug-regression-evidence.md
- doc/tasks/20260726-route-flow-add-form-click-count/verification-report.md
