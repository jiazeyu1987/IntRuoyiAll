# 生产组长报工默认全量查询修正

## Task Goal

按用户确认口径修正生产组长“报工管理”和“报工历史”：默认展示当前生产组长权限内的全部报工，不再只看当天；用户主动选择提交日期时仍按提交日期过滤。

## Milestones

- [x] M1：建立任务记录、读取触发规则和适用经验门禁。
- [ ] M2：补充 RED 静态合同，证明当前生产组长默认查询仍带隐藏 submitDate。
- [ ] M3：实施最小前端修复，移除生产组长默认隐藏日期限制。
- [ ] M4：运行定向回归、类型检查和差异检查。
- [ ] M5：完成验证报告、经验沉淀和任务收尾。

## Expected Verification

- RED/GREEN：`node tests/e2e/team-leader-report-all-history-default-static.spec.cjs`。
- 相邻回归：`node tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs`。
- 相邻回归：`node tests/e2e/team-leader-report-shared-allocation-static.spec.cjs`。
- 类型检查：`pnpm ts:check`。
- 差异检查：`git diff --check -- <task-owned files>`。

## Current Status

in_progress：任务记录已创建，准备补充 RED 合同并实施修复。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；修正默认查询参数口径，保留用户显式日期筛选。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 命中 `docs/frontend-development.md#统一列表复合工具栏布局门禁`：用户明确“默认展示所有历史/所有报工，不是只看当天”，因此正式请求默认必须省略 `submitDate`；禁止用隐藏内部日期伪装成无筛选。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本次使用任务专用最小静态合同先 RED 后 GREEN，并保留相邻报工管理合同回归。

## Cleanup Candidates

## Cleanup Keep

- doc/tasks/20260810-production-report-all-history-default/task.md
- doc/tasks/20260810-production-report-all-history-default/execution-log.md
- doc/tasks/20260810-production-report-all-history-default/verification-report.md
