# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: PQC组长“PQC管理”列表显示最近提交记录优先。
- Non-goals: 不新增前端表头交互排序、不新增 mock 数据、不改变 PQC 人员范围、租户权限或提交日期筛选语义。

## Requirements And Acceptance IDs

- AC-1: PQC管理列表按正式服务端提交时间倒序展示。
- AC-2: 前端不得只对当前页数组排序，避免跨页顺序失真。
- AC-3: 空筛选仍保持不传隐藏 submitDate。

## UI Entry Points, Routes, Components, And Owned Files

- Route/component: `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue` 的 PQC管理 tab。
- Owned tests: `IntRuoyiFronted/tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs`。

## API Contracts And Data States

- API: `GET /admin-api/mes/pro/process-pool/team-leader/submission/page` 继续返回 `PageResult<ProcessPoolTimelineEventVO[]>`。
- Data state: 前端直接使用 `data.list || []` 的正式分页返回顺序；排序由后端 mapper 的 `server_submit_time DESC, id DESC` 保证。

## BDD Scenarios

- BDD: PQC管理列表最近提交优先 -> Given PQC组长打开“PQC管理”列表且存在多条不同提交时间的 PQC 提交记录 / When 列表通过正式分页接口加载 / Then 第一页按服务端提交时间倒序返回，提交时间相同按事件 ID 倒序稳定排列，最近提交记录排在最前面。
- BDD: 排序不在前端当前页伪造 -> Given PQC管理列表通过服务端分页加载 / When 用户切换页码或筛选条件 / Then 前端直接使用正式分页返回顺序，后端按提交时间倒序和事件 ID 倒序提供稳定跨页排序。

## RED Command And Expected Failure

- RED: `node tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs` -> FAIL，缺少后端正式倒序合同，mapper 仍按 `server_submit_time ASC, id ASC`。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- UI 结构未改动；表格加载、空态、错误提示、人员范围权限仍沿用现有 `UnifiedListTemplate` 和 `getSubmissionList`。
- 权限边界不变：PQC管理仍按后端当前登录组长人员范围读取。

## E2E Or Component Verification Path

- 使用静态合同验证前端未当前页排序、仍直接使用正式分页返回顺序；未启动真实 E2E，因为本次无 UI 结构和写入路径变化。

## Blockers And Follow-Up Skills

- Blockers: 无。
- Follow-up skills: 无。
