# Execution Log

## User Intent

用户反馈：审批中心“待办”页签里，左侧徽标显示待办数量 128，但列表区域显示为空/数量为 0。

## Baseline

- Branch: `int_main`
- Existing dirty workspace baseline commit: `6f9ed0e83 chore: baseline existing workspace changes`
- Baseline file list recorded from `git show --name-status --oneline -1`.
- Current task files start after the baseline commit.

## BDD Scenarios

- BDD: 待办徽标与待办列表一致 -> Given 当前用户存在全局待办任务总数大于 0, When 用户打开 `/approval-center/todo` 且未设置模块或关键词过滤, Then 列表接口必须返回非空当前页数据并且 total 与徽标统计口径一致。
- BDD: 待办查询状态可见且可恢复 -> Given 待办页签 URL 或快速过滤中存在模块/关键词过滤, When 过滤导致列表为空, Then 页面查询控件必须展示对应过滤状态，不能在控件空白时偷偷按过滤条件查询。

## RED / GREEN Evidence

- RED: 待运行。
- GREEN: 待运行。

## Notes

- 已读取 `bug-regression-fix-loop` 技能和 `bug-contract.md`。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
