# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 用户反馈“报工管理里没有数据”。本机已有 5 条 `CODX-RPT-20260806` 一线生产格式报工数据，但截图对应的默认 `admin` 会话进入生产组长页后表格为空。
- Expected: `芋道源码/admin` 在本机生产组长页点击“报工管理”时，必须按正式 `PRODUCTION` 组长员工范围加载当天报工列表；不得用前端假数据、空列表兜底或 API-only 成功替代真实页面。

## Reproduction

- SQL RED: `admin_production_scope_count=0`、`admin_visible_marker_count=0`，证明默认 `admin` 无生产负责员工范围，看不到任务报工事件。
- Frontend RED: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> FAIL，新增断言要求 `watch(activeProductionModuleTab)` 在生产“报工管理”页签选中时按 `PRODUCTION` 加载列表，当前代码缺失该 watcher。

## Root Cause

- Data root cause: `mes_pro_process_pool_team_leader_scope` 中 `leader_user_id=1` 只有 `PQC + EMPLOYEE` 范围，没有 `PRODUCTION + EMPLOYEE` 范围；后端 `MesTeamLeaderWorkbenchServiceImpl.getSubmissionPage` 只按 `scopeService.listResponsibleEmployeeIds(...)` 返回的员工集合过滤报工列表。
- Frontend root cause: 生产组长独立页默认展示“人员管理”，切换到“报工管理”时原代码没有监听 `activeProductionModuleTab` 并调用 `getSubmissionList()`；PQC 页签已有相同模式的 watcher。

## Regression Test

- Updated: `IntRuoyiFronted/tests/e2e/production-leader-function-tabs-static.spec.js`。
- Contract: 生产“报工管理”页签必须存在 `watch(activeProductionModuleTab, async (tab) => ...)`，在 `tab === 'report'` 且当前组长类型为 `PRODUCTION` 时设置 `queryParams.leaderType='PRODUCTION'`、`queryParams.pageNo=1`、调用 `ensureSubmissionDateCondition()` 和 `getSubmissionList()`。

## Verification

- Verification: 前端静态合同、相邻工作台合同、`pnpm ts:check`、本机 SQL、admin 登录态接口和真实 Playwright 页面只读路径均已执行。
- Verification: 默认本机 admin 点击生产“报工管理”后列表请求返回 `total=25` 且页面可见行数 `10`，证明用户截图中的空表路径已恢复。

## RED Evidence

- RED: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> FAIL，断言“生产组长切换到报工管理 tab 时必须按 PRODUCTION 组长类型自动加载当天报工列表”未命中。
- RED: SQL 只读复验 -> `admin_production_scope_count=0`、`admin_visible_marker_count=0`。

## GREEN Evidence

- GREEN: `node tests/e2e/production-leader-function-tabs-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: SQL 写入 `mes_pro_process_pool_team_leader_scope.id=980044` 后，`admin_visible_marker_count=5`。
- GREEN: `admin` 登录态接口 pageSize=50 -> 业务码 `0`、总数 `25`、任务事件 ID `161-165` 命中 `5` 条。
- GREEN: Playwright 真实页面只读路径 -> 登录 `芋道源码/admin`，进入 `/mes/pro/process-pool/production-leader` 后点击“报工管理”，列表请求 `leaderType=PRODUCTION&submitDate=2026-08-06&pageNo=1&pageSize=10`，接口页行数 `10`、页面可见行数 `10`、组长写请求数 `0`、`pageErrors=0`。

## Risk And Regression Scope

- Scope: 本机 tenant `1`、默认 `admin` 用户 `1`、员工 `964`、任务自有报工标识 `CODX-RPT-20260806`。
- Frontend risk: 仅增加生产页签切换加载，与既有 PQC 页签 watcher 保持同类模式；不改变提交、复核或分配写入逻辑。
- Data risk: admin 范围为本机测试可见性补齐；如需撤回，按 `id=980044` 或备注 `CODX-RPT-20260806 admin production report visibility` 清理。

## Blockers And Follow-Up Actions

- No functional blocker remains for the reported empty report table in local `int_main`.
- Project closeout blocker remains: 当前 `int_main` 工作区存在大量本任务外既有脏改动，cleanup/commit/push 需单独确认策略后执行。
