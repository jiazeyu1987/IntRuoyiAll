# 隐藏工艺路线 MES 工序页签执行日志

## User Intent

- 用户基于截图指出黄框里的 `MES 工序` tab 不显示，按当前上下文理解为该页签需要从工艺路线页面隐藏。

## Preflight

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取技能 `bug-regression-fix-loop`、`frontend-feature-delivery` 及其 evidence contract。
- 已读取 `docs/experience-index.md` 并命中前端权限页签、共享分支并发基线、同文件选择性暂存门禁。
- 已建立独立基线提交 `46a63287c`：`chore: baseline dirty workspace before mes tab fix`，保存本任务开始前的脏工作区。
- 并发任务随后继续修改任务文档和 `IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`；这些不是本任务目标文件，本任务后续仅选择性暂存自身改动。

## BDD

- BDD: hide MES process tab -> Given 工艺路线已存在并打开详情/编辑内容页 When 页面渲染工艺路线 tab Then 只显示 `基础信息`、`流转关系图`、`关联产品`，不显示 `MES 工序`。
- BDD: reject legacy mesProcess query -> Given 用户通过旧链接携带 `?tab=mesProcess` 进入工艺路线编辑页 When 前端解析初始 tab Then 页面回到正式允许的 `flow` tab，不挂载 `RouteMesProcessList`。

## Milestone Log

- in_progress: 建立任务记录并准备新增聚焦静态合同。
- RED: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> FAIL, expected reason: 旧实现仍包含 `RouteMesProcessList` 懒加载和 `MES 工序` tab。
- completed: 更新 `mes-route-mes-process-tab-static.spec.js`、`mes-route-basic-info-tab-static.spec.js`、`mes-route-edit-default-flow-tab-static.spec.js`、`mes-route-flow-entry-readonly-static.spec.js` 的页签集合期望。
- completed: 修改 `RouteFormContent.vue`，移除 `MES 工序` 页签、`RouteMesProcessList` 挂载和 `mesProcess` 初始 tab 类型。
- completed: 修改 `RouteEditPage.vue` 和 `index.vue`，将合法 tab/query 集合收敛为 `basic`、`flow`、`product`。
- GREEN: `node tests/e2e/mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-basic-info-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-edit-default-flow-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-route-flow-entry-readonly-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。
- GREEN: `rg` 精确复查目标源码 -> PASS，`RouteFormContent.vue`、`RouteEditPage.vue`、`index.vue` 中无 `MES 工序` tab、`RouteMesProcessList` 挂载或 `mesProcess` 页签白名单残留。
- BLOCKER: `node tests/e2e/mes-route-product-standard-list-static.spec.js` -> FAIL，断言旧 `@request-submit="submitForm"`，当前非本任务实现已使用 `handleSubmitRequest`。
- BLOCKER: `node tests/e2e/mes-route-resource-tab-static.spec.js` -> FAIL，测试引用不存在的 `src/views/mes/pro/route/RouteProcessList.vue`。
- completed: 实现与目标验证完成，任务状态更新为 `ready_for_closeout`。
- GREEN: `project-experience-consolidation` -> PASS，已有 `docs/frontend-development.md#前端权限页签正向授权门禁` 覆盖本次经验，不新增长期经验文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-hide-route-mes-process-tab --mode preview` -> PASS，keep 仅包含 `task.md`、`execution-log.md`、`verification-report.md`，无 delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260803-hide-route-mes-process-tab --mode apply` -> PASS，linked=False，未删除文件。

## Changed Files

- `IntRuoyiFronted/src/views/mes/pro/route/RouteFormContent.vue`
- `IntRuoyiFronted/src/views/mes/pro/route/RouteEditPage.vue`
- `IntRuoyiFronted/src/views/mes/pro/route/index.vue`
- `IntRuoyiFronted/tests/e2e/mes-route-basic-info-tab-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-edit-default-flow-tab-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-flow-entry-readonly-static.spec.js`
- `IntRuoyiFronted/tests/e2e/mes-route-mes-process-tab-static.spec.js`
