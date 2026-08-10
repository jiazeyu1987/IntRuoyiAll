# Execution Log

## Intent

用户截图显示生产组长「报工管理」多条件筛选条仍显示“暂无筛选条件”。上一轮修复了默认日期空数据自动发现，但默认提交日期没有稳定作为可见筛选 Tab 呈现。

## BDD

BDD: 报工管理默认提交日期筛选可见 -> Given 生产组长进入报工管理；When 页面初始化或切换到报工管理页签；Then 多条件筛选条应显示 `提交日期: <日期>`，不应显示“暂无筛选条件”。

BDD: 默认日期发现后筛选仍可见 -> Given 默认今天没有记录但最近日期有记录；When 页面自动切换到最近有记录日期；Then 筛选 Tab 应同步显示被应用的提交日期。

## TDD

- RED: `node tests/e2e/team-leader-report-default-submit-date-visible-static.spec.cjs` -> FAIL, 缺少 `ensureSubmissionDateCondition(markApplied = false)` 和首屏 `ensureSubmissionDateCondition(true)` 初始化，默认日期没有被标记为可见已应用筛选。
- GREEN: `node tests/e2e/team-leader-report-default-submit-date-visible-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-nearest-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/team-leader-report-default-submit-date-visible-static.spec.cjs doc/tasks/20260808-team-leader-report-filter-visible` -> PASS，仅 Git 提示该 Vue 文件下次由 Git 触碰时 LF 会替换为 CRLF。

## Root Cause

`queryParams.submitDate` 初始化为默认日期，但 `submissionMultiFilterState.conditions/appliedConditions` 首屏仍为空；`TableMultiFilter` 只按 `state.conditions` 渲染筛选 Tab，因此显示“暂无筛选条件”。

## Completed Work

- 将 `ensureSubmissionDateCondition` 扩展为 `markApplied = false`，默认保留草稿筛选与已应用筛选边界。
- 首屏 setup 和重置筛选时调用 `ensureSubmissionDateCondition(true)`，让默认提交日期同时进入可见条件和已应用条件。
- 保留默认日期最近报工日期发现逻辑，不改接口、不造报工列表数据。
- 清理预览/应用均通过，仅删除临时 `bug-regression-evidence.md` 和 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 已将经验合并到 `docs/frontend-development.md#统一列表复合工具栏布局门禁`，并在 `docs/experience-index.md` 增加 `报工管理筛选默认无`、`暂无筛选条件`、`appliedConditions` 检索关键词。

## Blockers

- None.
