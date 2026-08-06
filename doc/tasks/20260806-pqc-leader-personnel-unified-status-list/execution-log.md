# Execution Log

## User Intent

- 用户要求：PQC 组长的人员管理里，删除禁用分组；禁用的名字是红色；禁用的和没有禁用的显示在一个列表里。

## BDD Scenarios

- BDD: PQC 人员启停状态统一列表 -> Given PQC 人员中同时存在启用与禁用人员 / When PQC 组长打开人员管理列表 / Then 页面不再提供启用或禁用分组筛选，并在同一列表加载全部人员。
- BDD: 禁用 PQC 人员姓名红色提示 -> Given 某个 PQC 人员为禁用状态 / When 该人员展示在人员管理列表 / Then 人员姓名以红色显示，且状态列仍显示“已禁用”。

## Milestone Updates

- in_progress: 已创建任务目录，并读取前端功能交付、前端开发、任务收尾和 PowerShell/编码规则。
- completed: 新增专用静态合同 `pqc-personnel-unified-status-list-static.spec.cjs`，先验证旧行为 RED。
- completed: 修改 `TeamLeaderWorkbenchPage.vue`，删除 PQC 人员启用状态筛选，移除 `pqcPersonnelQuery.enabled`，将 `refreshPqcPersonnel` 改为 `getPqcPersonnelList()` 全量请求，并为禁用 PQC 姓名增加红色样式类。
- completed: 更新 `pqc-leader-personnel-tab-static.spec.js`，将旧“必须有 enabled filter”断言替换为“不得按 enabled 分组”的新合同。
- blocked: 并发基线提交 `c4675d197 chore: baseline pre-existing dirty worktree` 已把本任务实现与非本任务文件一起提交，无法安全形成独立实现提交或推送。

## TDD Evidence

- RED: `node tests\e2e\pqc-personnel-unified-status-list-static.spec.cjs` -> FAIL, expected because the old PQC personnel actions still rendered `v-model="pqcPersonnelQuery.enabled"` and split enabled/disabled inspectors.
- GREEN: `node tests\e2e\pqc-personnel-unified-status-list-static.spec.cjs` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-personnel-tab-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-module-tabs-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\pqc-leader-standard-list-template-static.spec.js` -> PASS.
- REGRESSION: `node tests\e2e\production-personnel-unified-status-list-static.spec.cjs` -> initially FAIL after shared selector merge; fixed by splitting PQC and production disabled-name CSS rules, then PASS.
- REGRESSION: `pnpm ts:check` -> PASS.
- POST-CONCURRENCY RECHECK: after unrelated working-tree changes appeared in `TeamLeaderWorkbenchPage.vue`, `node tests\e2e\pqc-personnel-unified-status-list-static.spec.cjs` -> PASS and `node tests\e2e\pqc-leader-personnel-tab-static.spec.js` -> PASS.
- E2E: Not executed; no task-specific real Playwright path and confirmed disabled PQC personnel fixture were identified for this narrow list-display change. Static contracts cover the rendered controls, request parameter contract, status text, and red disabled-name class without claiming real E2E PASS.
- EXPERIENCE: no new long-term experience document created; the observed closeout risk is already covered by `docs\powershell-memory.md` sections `共享分支并发基线提交门禁` and `同文件并行改动选择性暂存门禁`.

## Blockers

- Git closeout blocker: HEAD `c4675d197` is a dirty-worktree baseline commit containing this task's implementation plus unrelated backend/frontend/docs changes. Per shared-branch concurrency gate, do not push or claim an independent task implementation commit without user direction.
