# Execution Log

## Intent

用户截图显示报工管理筛选条仍默认选中 `提交日期: 2026-08-07`。本轮口径修正为：筛选默认应为空/无，不默认显示提交日期条件。

## BDD

BDD: 报工管理默认筛选为空 -> Given 生产组长进入报工管理；When 页面首屏初始化；Then 多条件筛选条不应默认创建提交日期 Tab，字段下拉不应默认选中提交日期。

BDD: 用户主动筛选提交日期 -> Given 报工管理默认无筛选；When 用户新增并选择提交日期后查询；Then 列表按用户选择的提交日期查询。

BDD: 默认无筛选仍可查询 -> Given 报工管理默认无可见提交日期条件；When 用户添加员工、工序等非日期条件并点击查询；Then 前端使用内部 `submitDate` 满足后端必填，不提示必须先添加可见提交日期。

## TDD

- RED: `node tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs` -> FAIL, 当前代码缺少 `ensureSubmissionQueryDate`，并且会通过 `ensureSubmissionDateCondition(true)` 创建可见提交日期筛选 Tab。
- RED: `node IntRuoyiFronted\tests\e2e\team-leader-report-default-filter-empty-static.spec.cjs` -> FAIL, 合同新增初始化清理和查询 handler 断言后，当前实现缺少 `clearSubmissionVisibleFilterState`，仍无法覆盖用户截图中旧默认日期可见状态。
- GREEN: `node tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-standard-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `pnpm ts:check` -> PASS, 二次复跑通过；首次运行期间曾命中同页旧状态类型检查噪声，重跑以最终退出码 0 作为有效证据。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs IntRuoyiFronted/tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-leader-standard-list-template-static.spec.js IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js IntRuoyiFronted/tests/e2e/production-leader-function-tabs-static.spec.js doc/tasks/20260808-team-leader-report-filter-empty-default` -> PASS，仅 Git 提示部分文件下次由 Git 触碰时 LF 会替换为 CRLF。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-team-leader-report-filter-empty-default\bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260808-team-leader-report-filter-empty-default\frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-report-default-filter-empty-static.spec.cjs IntRuoyiFronted/tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs IntRuoyiFronted/tests/e2e/pqc-leader-standard-list-template-static.spec.js IntRuoyiFronted/tests/e2e/mes-process-pool-team-leader-static.spec.js IntRuoyiFronted/tests/e2e/production-leader-function-tabs-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js docs/frontend-development.md docs/experience-index.md doc/tasks/20260808-team-leader-report-filter-empty-default` -> PASS，仅 Git 提示部分文件下次由 Git 触碰时 LF 会替换为 CRLF。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-team-leader-report-filter-empty-default --mode preview` -> PASS，仅删除临时 evidence 文件。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-team-leader-report-filter-empty-default --mode apply` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`。

## Root Cause

上一轮把后端必填的 `submitDate` 误同步成用户可见筛选条件：`ensureSubmissionDateCondition(true)` 会初始化 `conditions/appliedConditions`，因此页面首屏显示 `提交日期: 2026-08-07`。

## Completed Work

- 拆分 `ensureSubmissionQueryDate()`：只保证列表请求有后端必填提交日期，不写入 `TableMultiFilter` 可见 state。
- 移除首屏、页签切换、重置和最近日期发现中的自动可见日期筛选创建。
- 新增初始化清理：若热更新或旧状态只留下 `submitDate` 默认可见条件且尚未应用，进入报工列表前会清空可见筛选。
- 移除 `hasSubmissionDateCondition` 可见日期门禁；默认无筛选时，点击查询直接走标准多条件 apply，由内部 `submitDate` 满足后端必填。
- 最近日期发现仍更新内部查询日期，但不生成 `提交日期` Tab。
- 更新相邻静态合同，统一为“默认筛选 UI 为空，内部查询日期满足后端必填”。
- 修正长期经验门禁，避免再次把“默认无筛选”误做成“默认日期可见”。

## Blockers

- None.
