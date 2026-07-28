# Execution Log

## User Intent

用户要求继续进行主端口 E2E 验证；此前结果显示 `int_main` 运行态健康，但 eDHR 单元格链接自动落库 E2E 未通过。

## BDD

- `BDD: Frontend uses persisted cell values only -> Given` 后端自动落库负责把单元格链接值写入执行详情，`When` 执行页 hydrate 草稿状态，`Then` 页面只读取已保存 `detail.cellValues`，不得再调用 `/batch-record-cell-link/prefill` 注入本地草稿值。
- `BDD: Main runtime E2E must use real openable batch task -> Given` 本地数据库存在授权租户、账号、启用 batchCode 链接规则和可打开正式批记录任务，`When` Playwright 从批次详情点击打开填写，`Then` `task/open` 返回 `cellLinkAutoPersist` 且执行详情和页面目标格显示相同已保存值。
- `BDD: Rough wash task must use current batch task context -> Given` 用户截图中生产批号 `881M009889` 已链接到“粗洗工序生产记录 / 生产批号”，`When` 创建/打开粗洗工序批记录，`Then` 后端必须按当前 `batchExecutionId + batchTaskId` 创建或打开执行记录并自动落库生产批号，不能传空 `taskId` 导致复用旧执行记录或跳过目标任务上下文。
- `BDD: Dynamic route form opens with production work order prefill -> Given` 损耗单或过程检验记录来自 `formBindings` 动态表单且 `FORM_TEMPLATE_VERSION` 链接规则把生产工单字段映射到目标字段，`When` 用户创建或再次打开该动态表单任务，`Then` 后端必须把链接值写入 FormCenter 实例草稿 `formData`，已有人工值不得被覆盖，不能只让传统批记录执行记录自动落库。

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
- `DIAGNOSIS: screenshot batch 881M009889 -> LOCAL_DB_NOT_FOUND`，本机库只读查询未找到 `mes_pro_work_order.batch_code='881M009889'` 或 `mes_pro_edhr_batch_execution.batch_code='881M009889'`；截图批次不在当前本机库，不能直接用该批次做本机 DB E2E。
- `DIAGNOSIS: code path -> FOUND`，`MesProEdhrBatchExecutionServiceImpl.buildOpenOrCreateExecutionReq(...)` 当前把传统批记录打开请求写成 `.setTaskId(null)`；该行为和 `docs/backend-development.md#切换填写人快照读取边界` 的“必须写入当前批次任务 ID”门禁冲突，可能导致粗洗工序复用旧执行记录或缺少当前任务上下文。
- `RED: node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs -> FAIL`，旧代码命中 `.setTaskId(null)`，证明传统批记录打开链路未把当前批次任务 ID 传入执行记录创建/打开上下文。
- `GREEN: code fix -> PASS`，`MesProEdhrBatchExecutionServiceImpl.buildOpenOrCreateExecutionReq(...)` 已改为 `.setTaskId(task.getId())`，不再传空 taskId。
- `GREEN: node D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime\IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-cell-link-task-id-context-static.spec.cjs -> PASS`，静态合同确认后端打开请求传当前任务 ID，且执行记录服务按 `batchExecutionId + taskId` 查询、生成 active context key 并持久化 taskId。
- `GREEN: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`，2 个聚焦 JUnit 断言传统批记录打开请求携带当前批次任务 ID。
- `GREEN: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`，补跑自动落库响应断言，确认 `task/open` 返回 `cellLinkAutoPersist`。
- `GREEN: isolated runtime build -> PASS`，在 `D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime` 执行 `mvn.cmd -pl yudao-server -am -DskipTests package` 成功，生成修复后后端 Jar 并启动到 `48088`，health 为 `UP`；前端启动到 `8088`，HTTP `200`。
- `BLOCKED then GREEN: slot7 frontend env -> PASS`，第一次 slot 7 E2E 因 worktree 缺 `.env.local` 导致验证码开启而阻塞；补入任务自有 `.env.local` 指向 `48088` 且 `VITE_APP_CAPTCHA_ENABLE=false` 后重新启动前端。
- `GREEN: EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8088 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48088 EDHR_BATCH_E2E_TENANT_LABEL=测试租户 EDHR_BATCH_E2E_USERNAME=codexedhrcell01 EDHR_BATCH_E2E_REQUIRE_NEW_EXECUTION=0 node tests/e2e/edhr-batch-execution-real-flow.e2e.js -> PASS`，slot 7 修复后运行态通过真实批次详情点击“打开填写”，证据写入 `real-e2e-slot7-evidence.md`。
- `GREEN: slot7 cleanup -> PASS`，停止 `48088` 后端和 `8088` 前端任务自有进程；`git worktree remove --force` 先解除 Git 注册但遗留目录，确认无 Git 注册、无监听端口、无目标进程后以不跟随 reparse point 的删除逻辑清理残留目录；临时分支无独有提交并已删除；端口登记项 `20260728-edhr-cell-link-taskid-runtime` 已标记 `active=false`、补入 `deletedAt/cleanupTask`。
- `GREEN: task-closeout-cleanup preview/apply -> PASS`，keep `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`、`frontend-feature-evidence.md`、`real-e2e-evidence.md`、`real-e2e-slot7-evidence.md`；delete `<none>`；blocked `<none>`；warnings `<none>`。
- `GREEN: project-experience-consolidation -> PASS`，已将 worktree slot E2E `.env.local` / 验证码关闭 / 后端端口代理门禁合并到 `docs/worktree-memory.md#Worktree 真实 E2E 运行产物门禁`，并在 `docs/experience-index.md` 增加关键词路由；未新建长期经验文档。
- `BLOCKED: commit/push -> NOT_RUN`，当前主工作区存在大量并行任务脏改，用户当前明确要求只进行 E2E 验证；未进行宽泛 baseline commit，避免混入无关任务改动。
- `DIAGNOSIS: dynamic form batchCode source -> FOUND`，本地库存在 `FORM_TEMPLATE_VERSION` 链接规则 `16/17`，且批次 `900000000894` 的 eDHR 批号为 `123123123`、生产工单 `881MO090935` 的 `mes_pro_work_order.batch_code` 为 `NULL`；对应 FormCenter 实例 `388/389/390` 的 `form_data_json` 目标格仍为空，说明动态表单旧逻辑依赖工单表批号会复现用户反馈，而传统批记录已因执行上下文批号可通过。
- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#buildFormTemplateVersionPrefillData_resolvesProductionBatchCodeFromExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，新增回归测试要求动态表单预填接口接收执行上下文批号，旧接口只有 `templateVersionId + workOrderId + formData` 三参，编译失败符合预期。
- `GREEN: code fix -> PASS`，`MesProBatchRecordCellLinkService.buildFormTemplateVersionPrefillData(...)` 增加 `executionBatchCode`，`MesProEdhrBatchExecutionServiceImpl` 在创建和再次打开动态表单时均传 `batch.getBatchCode()`；`FORM_TEMPLATE_VERSION` 的 `PRODUCTION_WORK_ORDER.batchCode` 现在使用 eDHR 执行上下文批号，不再依赖 `workOrder.batchCode`。
- `GREEN: node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-dynamic-form-cell-link-batch-code-static.spec.cjs -> PASS`，静态合同确认动态表单创建、打开两处均传入 `batch.getBatchCode()`，且 batchCode 分支读取 `executionBatchCode`。
- `GREEN: mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile -> PASS`，主代码编译通过，确认动态表单预填接口签名与生产调用链一致。
- `BLOCKED: mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest#buildFormTemplateVersionPrefillData_resolvesProductionBatchCodeFromExecutionContext" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，当前全量 testCompile 被并行“产品名称下拉”测试阻塞：`MesProBatchRecordReportControllerTest` / `MesProBatchRecordReportServiceImplDbTest` 引用缺失方法 `getProductNameOptions(String, boolean)`；该阻塞不属于本次动态表单批号修复，未按用户要求扩展处理。

## Current Evidence

- 主端口运行态：前端 `8081` HTTP `200`，后端 `48081` health `UP`。
- 隔离验证运行态：worktree `D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime`，slot 7 前端 `8088`，后端 `48088`，修复后后端 Jar `backend-edhr-cell-link-taskid-20260728-170049.jar`。
- 清理结果：slot 7 端口不再监听；worktree 目录不存在；Git worktree 列表不含该路径；端口登记项为 `active=false`。
- 当前目标源码：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- 当前目标测试：`IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js` 与 `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`。
- 当前真实 E2E 证据：`doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-evidence.md`。
- 当前动态表单修复证据：`IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-dynamic-form-cell-link-batch-code-static.spec.cjs`，以及主代码 `mvn -pl yudao-module-mes -am "-DskipTests" compile` 通过。

## Blockers

- 本次用户要求的测试租户真实 E2E 已在主运行态与 slot 7 修复后运行态均通过，无剩余 E2E blocker。
- 仍保留一项非本任务阻塞记录：`node tests/e2e/mes/batch-record-cell-link-static.spec.js` 当前失败在并行表单模板 API 合同断言 `api misses templateId?: number`，不作为本次执行页 `/prefill` 回归或真实 E2E 放行门禁。
- 提交/推送未执行，原因是主工作区有大量并行任务脏改且当前用户范围是 E2E 验证；本次不做会混入无关改动的 baseline commit。
- 动态表单聚焦 JUnit 当前被并行产品名称下拉测试编译错误阻塞，无法作为 GREEN 证据；已用静态合同和主代码编译补充验证，但真实动态表单 Playwright E2E 仍待使用任务自有测试数据复验。
