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
- `GREEN: node tests/e2e/edhr-cell-link-auto-persist-static.spec.js -> PASS`，执行页不再调用 `/batch-record-cell-link/prefill`，并只通过 `hydrateDraftState(detail)` 从已保存详情 hydrate。
- `GREEN: node tests/e2e/edhr-pre-release-editable-submit-static.spec.js -> PASS`，预关闭可编辑提交合同已同步为“不调用 draft-only prefill”。
- `GREEN: node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js -> PASS`，填写页权限合同保留 workTask 保存权限 gate，同时确认执行页不再调用 prefill 接口。
- `BLOCKED: node tests/e2e/mes/batch-record-cell-link-static.spec.js -> FAIL`，失败点为并行新增的表单模板参数断言 `api misses templateId?: number`，不属于本次执行页 `/prefill` 移除范围；本任务未修改该 API。
- `GREEN: pnpm ts:check -> PASS`，`vue-tsc --noEmit -p tsconfig.relaxed.json` 完成且退出码 `0`。
- `BLOCKED: EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 node tests/e2e/edhr-batch-execution-real-flow.e2e.js -> FAIL before browser`，证据文件 `real-e2e-evidence.md` 记录缺失 `LOCAL_DATABASE_FIXTURE`。
- `GREEN: read-only route/rule diagnosis -> PASS`，现有启用规则 `12` 指向 target report `1d05410f1d3140c5b8aa6786887ae69c`、scope id `130`；该 scope 对应的历史路线 `922140 / E2E-RVSM-20260718103832` 已删除，不能提供当前正式批次夹具。既有可复用页面建数脚本会在通过后清理任务批次和任务路线，不能作为稳定 E2E 夹具来源。
- `GREEN: runtime precheck -> PASS`，主工作区前端 `http://127.0.0.1:8081/` 返回 HTTP `200`，后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`；端口归属为 `E:\IntRuoyi\IntRuoyiFronted` Vite 和 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-110240.jar`。
- `GREEN: test-tenant fixture repair -> PASS`，经用户授权在 `测试租户` 使用任务自有数据复验；批次任务 `6955` 的 `form_slot_type` 从 `MAIN` 修正为正式报表槽位 `LOSS_REPORT`，并补入 `slot_config_snapshot_hash=0f84775df0c4a14feeedc6f606d4efc17434e2ce387ce93fb666ae91f26f8d52`，解除“打开填写”禁用和 `tasks=[]` 夹具阻塞。
- `GREEN: node tests/e2e/edhr-batch-execution-real-flow.e2e.js -> PASS`，环境为 `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081`、`EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081`、`EDHR_BATCH_E2E_TENANT_LABEL=测试租户`、`EDHR_BATCH_E2E_USERNAME=codexedhrcell01`、`EDHR_BATCH_E2E_REQUIRE_NEW_EXECUTION=0`；脚本通过真实批次详情点击“打开填写”并写入 `real-e2e-evidence.md`。
- `GREEN: auto-persist assertion -> PASS`，批次 `BE-EDHR-CELL-20260728-104808`、任务 `6955`、执行 `1579` 打开后 `task/open` 返回 `cellLinkAutoPersist.status=NO_CHANGE_ALREADY_APPLIED`，目标单元格 `1:5` 的执行详情和原表模式页面输入控件均显示 `EDHR-CELL-20260728-104808`。

## Current Evidence

- 主端口运行态：前端 `8081` HTTP `200`，后端 `48081` health `UP`。
- 当前目标源码：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- 当前目标测试：`IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js` 与 `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`。
- 当前真实 E2E 证据：`doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-evidence.md`。

## Blockers

- 本次用户要求的测试租户真实 E2E 已通过，无剩余 E2E blocker。
- 仍保留一项非本任务阻塞记录：`node tests/e2e/mes/batch-record-cell-link-static.spec.js` 当前失败在并行表单模板 API 合同断言 `api misses templateId?: number`，不作为本次执行页 `/prefill` 回归或真实 E2E 放行门禁。
