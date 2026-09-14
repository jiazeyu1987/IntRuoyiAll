# Execution Log

## 2026-08-08

- User intent: 修复排产工单 3 个已复现问题：组合筛选删除条件、反向承诺交期、优先级排序/可访问性。
- Command intent: 读取 `bug-regression-fix-loop`、`frontend-feature-delivery`、`playwright` 技能说明及引用合同。
- Command intent: 读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`，确认前端修复、真实页面验证和 UTF-8 文档规则。
- BDD: 组合筛选删除单个条件 -> Given 排产工单已应用多个筛选条件 When 用户删除其中一个条件 Then 页面只移除该条件、保留其他条件并发起新查询。
- BDD: 反向承诺交期筛选 -> Given 用户在反向承诺交期筛选中输入日期范围 When 用户点击查询 Then 日期范围保留并进入排产工单列表请求，不能静默恢复全量列表。
- BDD: 优先级排序与可访问性 -> Given 用户点击排产工单优先级表头 When 排序方向改变 Then 请求携带正式排序参数、表头暴露 `aria-sort`，且优先级输入有可访问名称。
- Verification boundary: 修复任务必须先 RED 后 GREEN；真实页面回归不得执行排产写操作。
- Root cause: `TableMultiFilter.removeActiveConditionTab()` 在 `emitState()` 之后再读取 computed `activeConditionId`，会把新活动 Tab 当成被删除条件；页面直接绑定 `scheduleOrderMultiFilter.removeCondition`，删除后不重写正式 query 参数、不刷新列表。
- Root cause: `useTableMultiFilter.validate()` 只拦截半填范围；当 Element Plus 日期范围被反向输入清空为无值时，条件 Tab 仍存在但校验通过，查询链路清空参数并恢复全量数据。
- Root cause: 排产工单主列表未向包装组件传入受控 sort state，也未绑定标准 `sort-change`；后端分页 VO/Mapper 没有排序字段和白名单，因此优先级表头只变 class、不发正式排序请求，`th[aria-sort]` 也未同步。
- RED: `node tests\e2e\schedule-order-filter-sort-fix-static.spec.js` -> FAIL, expected first failure `删除当前筛选条件时必须先捕获被删除 condition id`。
- Implementation: `TableMultiFilter` 删除前捕获 removed condition id；`useTableMultiFilter` 新增空条件阻断和 `removeConditionAndApply()`；排产工单页面删除条件后立即应用剩余条件。
- Implementation: 排产工单主列表透传 `sortState` / `sortChange`；页面新增 `scheduleOrderSortState`、`sortField=priorityNo`、`sortOrder=asc|desc`、优先级 `th[aria-sort]` 同步和优先级输入 `aria-label`。
- Implementation: 后端排产工单分页 VO 新增 `sortField` / `sortOrder` 校验，Mapper 以 `priorityNo` 白名单排序；不支持字段或字段/方向不成对时 fail fast。
- GREEN: `node tests\e2e\schedule-order-filter-sort-fix-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\schedule-order-main-multi-filter-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\unified-list-template-multi-filter-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\unified-list-template-sort-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check:schedule` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS, `yudao-module-mes` compiled successfully after 05:19 min.
- E2E: `node doc\tasks\20260808-schedule-order-filter-sort-fix\verify-schedule-order-filter-sort-fix.cjs` -> PASS; target statuses `multiFilterRemoval=NOT_REPRODUCED`, `reversePromiseDate=NOT_REPRODUCED`, `prioritySortA11y=NOT_REPRODUCED`, `mesWriteRequests=0`, `pageErrors=0`, `consoleErrors=0`。
- Residual non-target check: `node tests\e2e\unified-list-template-all-headers-sortable-static.spec.js` -> FAIL on existing unrelated pages such as `dcc/controlled-file/basic-data/file-type-taxonomy` and `mes/dv/*`; not used as current task gate.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-schedule-order-filter-sort-fix/bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-schedule-order-filter-sort-fix/frontend-feature-evidence.md` -> PASS。
- Verification: `git diff --check -- <current task files>` -> PASS；仅 LF/CRLF 提示，无 whitespace error。
- Closeout: `task_closeout.py --task-id 20260808-schedule-order-filter-sort-fix --mode preview` -> PASS；delete 仅包含临时 skill evidence 文件。
- Closeout: `task_closeout.py --task-id 20260808-schedule-order-filter-sort-fix --mode apply` -> PASS；临时 skill evidence 文件已清理，任务记录和复验产物已保留。
- Experience consolidation: `docs/experience-index.md` 已有匹配的复合筛选、服务端分页排序、cleanup keep 和 evidence 清理门禁；本次无新增长期经验文档，已回填适用门禁到 `task.md`。
