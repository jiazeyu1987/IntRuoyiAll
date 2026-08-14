# 排产工单筛选与优先级排序修复验证报告

## Summary

- 修复组合筛选删除单个条件：删除前捕获真实 condition id，并在排产工单页面使用 `removeConditionAndApply()` 立即应用剩余条件。
- 修复反向承诺交期静默全量查询：已选择字段但值为空的条件 Tab 会提示并阻断查询，不再清空参数后恢复全量数据。
- 修复优先级排序与可访问性：页面接管受控排序状态，发出 `sortField=priorityNo&sortOrder=asc|desc`，同步优先级表头 `aria-sort`，并给“新优先级”输入补可访问名称。
- 后端补齐正式分页排序白名单：仅支持 `priorityNo`，排序字段/方向缺失或不支持时 fail fast，不做静默降级。

## Verification

- `node tests\e2e\schedule-order-filter-sort-fix-static.spec.js` -> PASS。
- `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> PASS。
- `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS。
- `node tests\e2e\unified-list-template-sort-static.spec.js` -> PASS。
- `pnpm ts:check:schedule` -> PASS。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- `node doc\tasks\20260808-schedule-order-filter-sort-fix\verify-schedule-order-filter-sort-fix.cjs` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-schedule-order-filter-sort-fix/bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-schedule-order-filter-sort-fix/frontend-feature-evidence.md` -> PASS。
- `git diff --check -- <current task files>` -> PASS；仅输出既有 LF/CRLF 提示，无 whitespace error。

## Real Page Evidence

- 组合筛选删除：删除前 2 个条件，删除后保留 1 个条件；删除后发出 1 次新排产工单 page 请求，`clearedAll=false`，`retainedOldResultsWithoutReload=false`。
- 反向承诺交期：未发全量 page 请求，显示 `请填写承诺交期筛选条件。`。
- 优先级排序：点击后 page 请求包含 `sortField=priorityNo&sortOrder=asc`；表头 `aria-sort` 从 `none` 变为 `ascending`；优先级输入 `aria-label=新优先级`。
- 只读安全：真实页面回归 `mesWriteRequests=0`，`pageErrors=0`，`consoleErrors=0`。

## Residual Notes

- `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js` 仍失败在大量非本任务页面的历史接线问题，未作为本次排产工单修复的放行门禁。
- Playwright 捕获 24 个 `requestfailed`，均为导航/非目标链路类请求失败；目标排产工单 page 请求、报工对比请求和本次三项修复断言均通过。

## Closeout

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-schedule-order-filter-sort-fix --mode preview` -> PASS；blocked/warnings 均为 none。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-schedule-order-filter-sort-fix --mode apply` -> PASS；已清理临时 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`，保留核心任务记录、复验脚本和真实页面产物。
