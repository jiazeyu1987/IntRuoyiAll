# Execution Log

## User Intent

用户要求继续进行主端口 E2E 验证；此前结果显示 `int_main` 运行态健康，但 eDHR 单元格链接自动落库 E2E 未通过。

## BDD

- `BDD: Frontend uses persisted cell values only -> Given` 后端自动落库负责把单元格链接值写入执行详情，`When` 执行页 hydrate 草稿状态，`Then` 页面只读取已保存 `detail.cellValues`，不得再调用 `/batch-record-cell-link/prefill` 注入本地草稿值。
- `BDD: Main runtime E2E must use real openable batch task -> Given` 本地数据库存在授权租户、账号、启用 batchCode 链接规则和可打开正式批记录任务，`When` Playwright 从批次详情点击打开填写，`Then` `task/open` 返回 `cellLinkAutoPersist` 且执行详情和页面目标格显示相同已保存值。

## RED/GREEN Evidence

- `GREEN: experience-preflight -> PASS`，已读取 `frontend-development.md`、`backend-development.md`、`e2e-rules.md`、`login-access.md`、`local-runtime.md`、`database-rules.md`、`worktree-restrictions.md`、`powershell-memory.md` 和 `powershell-encoding.md`，并命中单元格链接预填落库、静态合同同步、数据库夹具和聚焦静态契约门禁。
- `RED: node tests/e2e/edhr-cell-link-auto-persist-static.spec.js -> FAIL`，执行页仍包含 `BatchRecordCellLinkApi.getPrefill`、`normalizeCellLinkPrefillDraftValue` 和 `hydrateDraftState(... prefills ...)`。
- `BLOCKED: node tests/e2e/edhr-batch-execution-real-flow.e2e.js -> FAIL before browser`，缺少 `LOCAL_DATABASE_FIXTURE`；只读诊断确认启用 batchCode 规则 `1` 条、活动未阻塞批次 `3` 个，但最终可打开候选 `0`。

## Current Evidence

- 主端口运行态：前端 `8081` HTTP `200`，后端 `48081` health `UP`。
- 当前目标源码：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- 当前目标测试：`IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js` 与 `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`。

## Blockers

- 真实 E2E 仍需要正式、可打开、任务自有或授权可清理的本地数据库夹具；不得用 mock、API-only 或直接 SQL 造数替代。
