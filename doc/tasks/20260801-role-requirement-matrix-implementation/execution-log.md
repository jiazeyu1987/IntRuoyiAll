# 岗位需求分解矩阵实现任务执行日志

## Task Start

- User intent: 按规划包顺序实现并验证“岗位需求分解矩阵”从 M0 到 M6 的完整开发；必须从 M0 开始，未通过当前里程碑真实 E2E 或前置检查不得进入下一里程碑。
- Planning package: `doc/tasks/20260801-role-requirement-matrix-excel/`。
- New implementation task directory: `doc/tasks/20260801-role-requirement-matrix-implementation/`。
- User scope override: 本次如需提交，只做本地提交，不执行 `git push`，除非用户另行明确要求。

## Preflight Evidence

- Read: `C:\Users\BJB110\.codex\attachments\4a1c24fc-eb00-4fe7-a724-016c045d1eff\pasted-text-1.txt`。
- Read: `E:\IntRuoyi\AGENTS.md`。
- Read: `docs\task-closeout-rules.md`。
- Read: `docs\powershell-encoding.md`。
- Read: `docs\powershell-memory.md`。
- Read skill: `development-plan-delivery`。
- Read skill: `milestone-tdd-delivery`。
- Baseline dirty workspace commit: `baf59590f` (`chore: baseline dirty workspace before role matrix implementation`)。
- Baseline scope: 13 pre-existing dirty files from frontend Runner and parallel task docs; no current implementation task files were included.

## M0 - 契约、术语、权威来源和 E2E 前置冻结

- BDD: M0 source map and E2E preflight -> Given 规划包要求所有正式来源、角色、测试租户、签名、数据库、Redis、浏览器和运行服务在 M0 冻结 When 执行 M0 前置检查 Then 明确每个来源为 CONFIRMED 或 BLOCKED，任一真实 E2E 前置缺失时不得进入 M1。
- M0_PRECHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- M0_PRECHECK: `python -X utf8 C:\Users\BJB110\.codex\skills\roadmap-node-dev-plan\scripts\validate_node_dev_plan.py --task-dir E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- M0_SOURCE: `MesProcessPoolActiveOrderDO` + `20260731_mes_process_pool_team_leader_p1_runtime_config.sql` -> CONFIRMED_INADEQUATE，当前活跃订单以 `leader_user_id + work_order_id` 为核心，不是跨角色统一 activeOrderId。
- M0_SOURCE: `MesFrontlinePqcContextServiceImpl.listActiveOrders()` -> BLOCKED，PQC 仍通过 `processPoolMapper.selectActiveList()` 读取 `mes_pro_process_pool` 活跃行。
- M0_SOURCE: `MesProRouteFlowProcessBatchRecordDO` -> CONFIRMED，正式逐工序批记录绑定来源存在；后续必须继续禁止 `formBindings`、`MAIN` 默认或“工序开始”替代。
- M0_SOURCE: `MesProEdhrReleaseServiceImpl.buildCheckItems()` -> BLOCKED，检验、偏差、返工、报废、库存仍生成来源未接入 blocker。
- M0_PREFLIGHT: local runtime -> PASS，前端 `8081`、后端 `48081`、后端 health、登录页、本机浏览器、Node/pnpm/Maven 均可用；日志和本文档不记录运行命令中的凭据。
- M0_PREFLIGHT: role requirement matrix E2E scripts -> INITIAL_BLOCKED，`e2e:role-requirement-matrix:real:check`、`e2e:role-requirement-matrix:real` 和 `tests/e2e/role-requirement-matrix-real-flow.e2e.js` 初始缺失；后续已通过 TDD 补齐入口和真实 Playwright 预检脚本。
- M0_PREFLIGHT: milestone static scripts -> INITIAL_BLOCKED，`e2e:role-matrix-qa-regulation:static`、`e2e:role-matrix-pqc-dynamic-form:static`、`e2e:role-matrix-transfer-start-check:static`、`e2e:role-matrix-daily-close-scope:static` 初始缺失；后续已补齐并转为业务 RED。
- M0_PREFLIGHT: real data -> BLOCKED，测试租户、六角色账号、权限、电子签名、ERP/调拨/路线/规程/批记录绑定任务样本未确认。
- BLOCKER: M0 未达到进入 M1 的前置条件；不得开始 M1 生产代码或测试 RED。

## M0 - 真实 E2E 前置脚本 TDD

- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: `package.json` 未暴露 `role-requirement-matrix` 预检/真实 E2E 脚本且真实 E2E 文件缺失。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS，已新增 `e2e:role-requirement-matrix:preflight:static`、`e2e:role-requirement-matrix:real:check`、`e2e:role-requirement-matrix:real` 和真实 Playwright 预检脚本。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> BLOCKED，早期 fail-fast 已识别 `RRM_*` 真实测试环境变量、统一 activeOrderId schema/source 缺口、PQC 仍读取 `mes_pro_process_pool`、eDHR 放行来源仍未接入；当时 SOURCE gate 扩展后的口径为 56 个 ENV/SOURCE 前置缺口。
- Evidence: `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`。
- Evidence: `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`。
- Decision: M0 从“真实 E2E 脚本缺失”推进为“真实 E2E 前置脚本存在且能 fail-fast”；M0 仍因正式来源、真实账号/签名/样本数据保持 `blocked`，M3/M4/M5 静态脚本已在后续步骤补齐并转为业务 RED。

## M0 - ERP/QA/PQC Source Gate TDD

- BDD: M0 ERP/QA/PQC source gate -> Given 规划包要求冻结调拨、发货、补料、退料、批次、QA 规程归属和 PQC 任务模型 When 执行真实 E2E 前置检查 Then 这些来源必须被明确标记为 CONFIRMED 或 BLOCKED，不能只检查活跃订单和放行占位项。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: 真实 E2E 预检脚本缺少 `collectErpRelationBlockers`，未覆盖 ERP/QA/PQC source gate。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> BLOCKED，当时 script fail-fast 报告 56 个前置缺口；新增 SOURCE blockers：`activeOrderTransferRelation`、`activeOrderShipmentSource`、`activeOrderReplenishmentReturnSource`、`activeOrderBatchTraceSource`、`qaRegulationOwnership`、`qaRegulationVersionModel`、`pqcTaskModel`、`pqcPieceDetailModel`、`selectActiveByWorkOrderRouteProcess`、`hardcodedPqcInspectionItems`、`defaultPqcInspectionType`、`defaultPqcInspectionQuantity`、`defaultPqcScrapQuantity`。
- Evidence: `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`。
- Evidence: `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`。
- Decision: M0 source map 预检覆盖面已扩大；M0 仍保持 `blocked`，不得进入 M1。

## M0 - Planned Static Script Entry TDD

- BDD: M0 planned static script entries -> Given 规划包要求 M3/M4/M5 后续 RED 不能失败在缺 package script 或缺测试文件 When 执行 M0 预检静态合同 Then `role-matrix-qa-regulation`、`role-matrix-pqc-dynamic-form`、`role-matrix-transfer-start-check`、`role-matrix-daily-close-scope` 四个脚本必须存在且可执行。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: `package.json` 未暴露 `e2e:role-matrix-qa-regulation:static` 等规划静态脚本入口。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- RED: `node tests/e2e/role-matrix-qa-regulation-static.spec.cjs` -> FAIL，expected reason: QA 规程正式 schema/版本模型缺失，PQC 页面仍存在硬编码规程项和默认数量。
- RED: `node tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs` -> FAIL，expected reason: PQC 上下文未暴露规程快照任务身份，页面仍依赖硬编码项目。
- RED: `node tests/e2e/role-matrix-transfer-start-check-static.spec.cjs` -> FAIL，expected reason: activeOrderId 与调拨/批次/库存关系源缺失，放行仍使用 `buildSourceNotIntegratedItem`。
- RED: `node tests/e2e/role-matrix-daily-close-scope-static.spec.cjs` -> FAIL，expected reason: 缺少日结可见入口，责任范围模型缺工作站、产线、设备、订单。
- Decision: M3/M4/M5 静态脚本缺失 blocker 已解除，替换为可执行业务 RED；M0 仍因真实环境、账号/签名/样本和正式来源缺口保持 `blocked`。

## M0 - 本轮本地验证与经验沉淀

- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 返回非零，当时真实脚本报告 56 个 ENV/SOURCE 前置缺口。
- RED: `pnpm e2e:role-matrix-qa-regulation:static` -> FAIL，expected reason: QA regulation formal owned schema/version model 缺失。
- RED: `pnpm e2e:role-matrix-pqc-dynamic-form:static` -> FAIL，expected reason: PQC context 缺 regulation snapshot task identity。
- RED: `pnpm e2e:role-matrix-transfer-start-check:static` -> FAIL，expected reason: activeOrderId 到 transfer/batch/stock 的正式关系源缺失。
- RED: `pnpm e2e:role-matrix-daily-close-scope:static` -> FAIL，expected reason: 日结可见入口和扩展责任范围模型缺失。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: stale M0 evidence scan for old blocker count and removed script-missing conclusions -> NO_MATCH。
- EXPERIENCE: `docs/e2e-rules.md` 已追加“规划型 E2E 前置与业务 RED 分离门禁”，用于后续防止把脚本/环境缺失误记为业务 RED。
- Decision: 本轮按用户明确要求不执行 `git push`；M0 仍保持 `blocked`，不得进入 M1。

## M0 - 独立测试报告补齐

- TEST_REPORT: `doc/tasks/20260801-role-requirement-matrix-implementation/test-report.md` -> CREATED，当时独立记录 M0 入口合同 PASS、真实 E2E 前置 56 项 blocker、M3/M4/M5 业务 RED 和禁止进入 M1 的 gate decision。
- TASK_STATE: `task-state.json` -> UPDATED，新增 `artifacts.testReport`，保持 `status=blocked`、`currentMilestone=M0`。
- Decision: 当前 M0 不满足准出；不得开发 M1 生产代码。

## M0 - 2026-08-02 续跑验证

- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation docs/e2e-rules.md IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-*.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 非零，当时脚本报告 56 个 ENV/SOURCE 前置缺口。
- Decision: M0 blocker 未解除；继续禁止进入 M1，不执行 `git push`。

## M0 - Source Map 深化证据

- M0_SOURCE_DEEPENING: `MesKingdeeProductionOrderSyncServiceImpl` -> CONFIRMED_PARTIAL，Kingdee 生产订单可同步到 `mes_pro_work_order` 并写入 ERP 数量/状态快照，但仍不是跨生产、PQC、调拨、批记录、放行的统一 activeOrderId。
- M0_SOURCE_DEEPENING: `MesProcessPoolActiveOrderDO` + `20260731_mes_process_pool_team_leader_p1_runtime_config.sql` -> CONFIRMED_INADEQUATE，当前字段只有 `leaderUserId/workOrderId/activeStatus/joinedAt/removedAt`，唯一键仍包含 `leader_user_id`，缺正式路线、路线版本、ERP 固定数量快照、业务状态和版本号。
- M0_SOURCE_DEEPENING: `MesFrontlinePqcContextServiceImpl` -> BLOCKED，`listActiveOrders()` 仍读取 `processPoolMapper.selectActiveList()`，`submitPqcInspection()` 仍要求 `selectActiveByWorkOrderRouteProcess(...)` 命中最新生产事件。
- M0_SOURCE_DEEPENING: WMS data objects/services -> CONFIRMED_PARTIAL，`MesWmTransfer*`、`MesWmMaterialStockDO`、`MesWmBatchDO` 存在，但未发现统一 `activeOrderId` 关系；部分模型只关联 `workOrderId` 或 `batchId`。
- M0_SOURCE_DEEPENING: QC/PQC models -> CONFIRMED_INADEQUATE，QC 模板、IPQC/OQC/RQC 和指标结果存在，但未证明本需求所需 QA 规程唯一所有权、发布版本、PQC 任务身份、规程快照和逐件明细模型具备。
- M0_SOURCE_DEEPENING: `FrontlineFixedTemplatePanel.vue` -> CONFIRMED_INADEQUATE，PQC 页面仍硬编码 `length/appearance/seal/pressure`、默认 `PATROL` 和默认检验数量 `30`。
- M0_SOURCE_DEEPENING: `MesProEdhrReleaseServiceImpl` -> BLOCKED，放行检查中的检验、偏差、返工、报废、库存五类来源仍调用 `buildSourceNotIntegratedItem(...)`。
- Evidence updated: `doc/tasks/20260801-role-requirement-matrix-implementation/source-map.md`。
- Decision: 深化扫描只收敛 M0 source map，不进入 M1，不写 M1-M6 生产代码；本轮仍按用户要求不执行 `git push`。

## M0 - Source Map 深化后本地验证

- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation docs/e2e-rules.md IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-*.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 非零，当时脚本报告 56 个 ENV/SOURCE 前置缺口。
- BDD: M0 PQC source gate expansion -> Given M0 必须冻结 PQC 正式来源、任务身份和前端规程驱动渲染 When 真实 E2E 前置检查扫描 PQC 服务和前端页面 Then `selectActiveByWorkOrderRouteProcess`、硬编码项目、默认 `PATROL`、默认数量 `30`、默认损耗 `1` 都必须作为 SOURCE blocker，而不是留给 M3 才发现。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: 真实 E2E 预检脚本缺少 `collectPqcSubmissionBlockers` 和 `collectPqcFrontendBlockers`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 非零，当时脚本报告 56 个 ENV/SOURCE 前置缺口；新增 blockers：`selectActiveByWorkOrderRouteProcess`、`hardcodedPqcInspectionItems`、`defaultPqcInspectionType`、`defaultPqcInspectionQuantity`、`defaultPqcScrapQuantity`。
- RED: `pnpm e2e:role-matrix-pqc-dynamic-form:static` -> FAIL，expected reason: PQC context 缺 regulation snapshot task identity；该 RED 与新增 M0 source gate 一致。
- RED: `pnpm e2e:role-matrix-qa-regulation:static` -> FAIL，expected reason: QA regulation formal owned schema/version model 缺失。
- RED: `pnpm e2e:role-matrix-pqc-dynamic-form:static` -> FAIL，expected reason: PQC context 缺 regulation snapshot task identity。
- RED: `pnpm e2e:role-matrix-transfer-start-check:static` -> FAIL，expected reason: activeOrderId 到 transfer/batch/stock 的正式关系源缺失。
- RED: `pnpm e2e:role-matrix-daily-close-scope:static` -> FAIL，expected reason: 日结可见入口和扩展责任范围模型缺失。
- Decision: M0 仍为 `blocked`，不能进入 M1；本轮不执行 `git push`，当前 blocker 未解除前也不做实现提交。

## M0 - 生产系数与正式批记录绑定 Source Gate TDD

- BDD: M0 production coefficient and batch record binding source gate -> Given M0 必须冻结生产系数、计划数量快照和正式逐工序批记录绑定来源 When 真实 E2E 前置检查扫描路线配置、排产快照、activeOrderId、工艺路线前端和 eDHR 运行态 Then activeOrderId 缺系数/计划数量快照、默认生产系数 `1`、缺失槽位默认 `MAIN`、`batchRecordFormNames` 与 `formBindings` 未证明分离都必须作为 SOURCE blocker。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: 真实 E2E 预检脚本缺少 `collectProductionCoefficientBlockers`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 非零，脚本当前报告 62 个 ENV/SOURCE 前置缺口；新增 blockers：`activeOrderProductionQuantityFactorSnapshot`、`activeOrderPlannedQuantitySnapshot`、`defaultProductionQuantityFactorInAutoSchedule`、`normalizeRecordBindingSlotTypeDefaultMain`、`batchRecordFormNamesFormBindingsSeparation`、`edhrRuntimeDefaultMainSlot`。
- Evidence updated: `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json`。
- Evidence updated: `doc/tasks/20260801-role-requirement-matrix-implementation/role-requirement-matrix-real-e2e-evidence.md`。
- Decision: M0 仍为 `blocked`，不能进入 M1；本轮不执行 `git push`。

## M0 - 生产系数与批记录绑定 Gate 后验证

- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，pnpm lifecycle 非零，脚本报告 62 个 ENV/SOURCE 前置缺口。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation docs/e2e-rules.md` -> PASS。
- RED: `pnpm e2e:role-matrix-qa-regulation:static` -> FAIL，expected reason: QA regulation formal owned schema/version model 缺失。
- RED: `pnpm e2e:role-matrix-pqc-dynamic-form:static` -> FAIL，expected reason: PQC context 缺 regulation snapshot task identity。
- RED: `pnpm e2e:role-matrix-transfer-start-check:static` -> FAIL，expected reason: activeOrderId 到 transfer/batch/stock 的正式关系源缺失。
- RED: `pnpm e2e:role-matrix-daily-close-scope:static` -> FAIL，expected reason: 日结可见入口和扩展责任范围模型缺失。
- GIT_STATUS: `git status --short --branch` -> `int_main...origin/int_main [ahead 1]`；本轮按用户要求不执行 `git push`，且未处理无关脏文件/未跟踪目录。
- EXPERIENCE: `docs/e2e-rules.md` -> UPDATED，将规划型 E2E 门禁中的固定旧缺口数量示例改为“当前缺口数量”，避免后续 source gate 扩展后保留过期 blocker 口径。
- Decision: M0 仍为 `blocked`，M1-M6 继续保持 blocked，不提交、不推送。

## M0 - 用户授权本机租户与夹具补齐验证

- BDD: M0 authorized local tenant fixture setup -> Given 用户明确授权本轮使用本机租户 `芋道源码`、六个随机账号和压力泵路线最新版本 When 准备 M0 真实 E2E 前置数据 Then 租户、账号、权限、电子签名、压力泵路线、工单/调拨和 QC/IPQC 夹具可用于真实预检，但不得把本地夹具等同于正式 SOURCE 实现。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: 真实 E2E 预检脚本缺少显式本机基准租户授权门禁 `RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION`。
- RED: `pnpm e2e:role-requirement-matrix:real:check` -> FAIL，expected reason: 用户授权的本机租户 `芋道源码` 仍被旧 ENV 禁止口径拦截。
- GREEN: local fixture database verification -> PASS，六角色账号、`super_admin` 本地测试权限、电子签名授权与签名图片、压力泵路线 V21、工单 `RRM-20260801-PP-MO-001`、两条调拨和 QC/IPQC 模板/指标均存在；密码明文未写入文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation` -> PASS；仅提示 `package.json` LF/CRLF 工作区警告。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE 前置缺口，无 ENV/RUNTIME blocker。
- TASK_DOCS: `task.md`、`task-state.json`、`verification-report.md` -> UPDATED，删除过期的 62 个 ENV/SOURCE 和“测试租户/账号/签名未确认”口径，改为本地数据前置已补齐、正式 SOURCE 缺口仍阻塞。
- EXPERIENCE: `project-experience-consolidation` -> REVIEWED，已有 `docs/e2e-rules.md` 的“规划型 E2E 前置与业务 RED 分离门禁”可承载本轮经验，无需新建长期经验文档。
- Decision: M0 仍为 `blocked`，原因已从环境/账号/样本缺失收敛为 31 个正式 SOURCE/model/code 缺口；不得进入 M1。本轮按用户明确要求不执行 `git push`。

## M0 - 派生 QA 临时规程夹具

- BDD: M0 derived QA regulation fixture -> Given 用户要求使用球囊扩张压力泵路线 V21、逐工序 MAIN 批记录绑定和 `PROCESS_INSPECTION / 过程检验记录 V3.0` When 从源 Word 表格逆推 QA/PQC 规程 Then 本地临时 QC 模板应保存源检验方法、源数量或临时首检数量、临时巡检系数，并继续明确不代表正式 QA 规程版本模型。
- RED: derived QA regulation data validation query -> FAIL，expected reason: `RRM-20260801-QA-REG-PP-V21` 模板不存在，无法覆盖 `过程检验记录 V3.0` 的 49 条源检验方法。
- GREEN: derived QA regulation fixture write -> PASS，新增/更新 `mes_qc_template` 模板 `6 / RRM-20260801-QA-REG-PP-V21`，关联产品 `902149 / 球囊扩张压力泵`，写入 49 条 `mes_qc_indicator` 与 `mes_qc_template_indicator` 派生方法。
- GREEN: derived QA regulation database verification -> PASS，模板 `6`、产品 `902149`、49 条派生检验方法、临时首检数量和临时巡检系数均存在；`组装Ⅲ` 10 条源方法未匹配 V21 同名路线工序并保留为 unmatched，不静默映射到包装工序。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- TASK_DOCS: `m0-derived-qa-regulation.md` -> CREATED，记录模板 `6`、49 条方法、临时数量/系数、路线覆盖和 unmatched source 限制。
- TASK_DOCS: `m0-test-data.md`、`database-schema-evidence.md`、`m0-preflight.md`、`test-report.md`、`verification-report.md`、`task.md`、`task-state.json` -> UPDATED，加入派生 QA 临时规程证据。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with `RRM_QA_REGULATION_VERSION_ID=6` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE 前置缺口，无 ENV/RUNTIME blocker。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: 本轮只补齐用户授权的临时 M0 测试数据；正式 QA 规程所有权、不可变版本、PQC 任务身份、规程快照和逐件明细仍是 SOURCE blocker，M0 仍 `blocked`，不得进入 M1。

## M0 - Gate Realignment and Blocker Inventory Rerun

- BDD: M0 blocker inventory gate -> Given `task.md` 明确 M0 未 accepted 前不得进入 M1 When 主线程复核当前任务状态和 `real:check` 输出 Then 只能补齐 M0 blocker 证据、保持 `currentMilestone=M0`、禁止新增 M1-M6 生产代码。
- BLOCKER: RRM-BLK-001..031 -> 31 个 SOURCE blocker 已结构化记录在 `blocker-inventory.md`；该清单只作为 M0 证据和后续计划依据，不授权主线程进入 M1。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE blocker；无 ENV/RUNTIME blocker。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M0 仍为 `blocked`；M1-M6 继续 blocked by M0；本轮未新增生产代码、未提交、未执行 `git push`。

## M0 - Independent Gate Audit

- BDD: M0 independent advancement gate -> Given M0 准出要求 `real:check` 无 SOURCE/ENV/RUNTIME blocker 且所有 M0 证据同步 When 独立复核任务状态、规划包、测试计划、blocker inventory 和 `result.json` Then 只有全部准出条件有直接证据时才能进入 M1。
- AUDIT: `m0-gate-audit.md` -> CREATED，逐项列出 M0 requirement、required proof、current evidence 和 audit result。
- BLOCKER: M0 gate -> `real:check` 当前仍有 31 个 SOURCE blocker；activeOrderId、ERP 调拨/批次、QA/PQC 正式模型、生产系数、三类路线配置分离和 eDHR 放行来源均未满足 M0 准出。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE blocker；无 ENV/RUNTIME blocker。
- GREEN: `git diff --check -- IntRuoyiFronted/package.json IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-pqc-dynamic-form-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-transfer-start-check-static.spec.cjs IntRuoyiFronted/tests/e2e/role-matrix-daily-close-scope-static.spec.cjs doc/tasks/20260801-role-requirement-matrix-implementation` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M0 gate audit 结论为 `FAIL_BLOCKED`；主线程停止在 M0，只允许继续 M0 证据、blocker 和验证复核；未新增生产代码，未提交，未执行 `git push`。

## M0 - Blocker Inventory Required Field Completion

- BDD: M0 blocker inventory required fields -> Given 目标提示词要求每条 blocker 包含 current status 和 created/updated date When 复核 `blocker-inventory.md` Then 31 个 RRM-BLK 记录必须能通过 ID 关联到明确状态和创建/更新时间。
- TASK_DOCS: `blocker-inventory.md` -> UPDATED，新增 `Status Register`，为 RRM-BLK-001..031 补齐 `Current status=OPEN_BLOCKED` 和 `Created/updated date=2026-08-02`。
- GREEN: blocker inventory status register validation -> PASS，`Blocker Summary` 与 `Status Register` 均为 31 条且 ID 集合完全一致。
- Decision: M0 blocker 结构化字段已补齐；31 个 SOURCE blocker 未减少，M0 仍 `blocked`，不得进入 M1。

## M0 - Milestone Boundary Correction After User Challenge

- BDD: M0 milestone boundary correction -> Given 用户指出 M0 尚未完成却出现主线程进入 M1 的风险 When 复核 `task-state.json`、`result.json`、`blocker-inventory.md` 和 M0 审计文档 Then 当前里程碑必须保持 `M0`，M1 只能作为 blocker 归属/后续解决方向，不能作为主线程启动许可。
- HISTORICAL_BLOCKER: `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> 当时为 31 个 SOURCE blocker 和 RUNTIME blocker `backendHealth`；后续 `M0 - Runtime Blocker Recheck` 已验证关闭 runtime blocker。
- TASK_DOCS: `task-state.json`、`task.md`、`m0-preflight.md`、`m0-gate-audit.md`、`test-report.md`、`verification-report.md`、`blocker-inventory.md` -> UPDATED，纠正“无 ENV/RUNTIME blocker”的过期口径，并新增 `RRM-BLK-032`。
- Decision: M0 仍为 `blocked`；主线程停止在 M0；M1-M6 继续 blocked by M0；本轮未新增生产代码、未提交、未执行 `git push`。

## M0 - Runtime Blocker Recheck

- BDD: M0 runtime blocker recheck -> Given M0 准出要求 `real:check` 无 SOURCE/ENV/RUNTIME blocker When 本机 `48081` health 恢复并重跑 M0 最小验证 Then runtime blocker 应关闭，但 31 个 SOURCE blocker 仍保持 M0 blocked。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，返回 `status=UP`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE blocker；无 ENV/RUNTIME blocker。
- BLOCKER: RRM-BLK-032 -> RESOLVED_VERIFIED，后端 health runtime blocker 已通过 health 和 `real:check` 验证关闭。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析且任务 Markdown 可 UTF-8 读取。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M0 仍为 `blocked`；主线程停止在 M0；M1-M6 继续 blocked by M0；本轮未新增生产代码、未提交、未执行 `git push`。

## M0 - Current State Rerun

- BDD: M0 current state rerun -> Given 目标文件要求 M0 未完成前不得进入 M1 When 复核当前任务状态、规划包、测试计划和 `real:check` Then 只更新 M0 证据，保持 `currentMilestone=M0`，并确认 31 个 SOURCE blocker 仍阻塞准出。
- GREEN: planning package read -> PASS，`development-plan.md` 532 行、`test-plan.md` 423 行；测试计划当前映射 62 个 AC、62 个 TC、16 个 BDD 场景。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，返回 `status=UP`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED，脚本报告 31 个 SOURCE blocker；无 ENV/RUNTIME blocker。
- GREEN: `python -X utf8 -c "import json, pathlib; ..."` -> PASS，`task-state.json` 可解析，任务 Markdown 可 UTF-8 读取，且 `currentMilestone=M0`、`M1` 仍 blocked。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M0 仍为 `blocked`；M1-M6 继续 blocked by M0；本轮未新增生产代码、未提交、未执行 `git push`。

## M0 - Planning Package Validator Rerun

- BDD: M0 planning package validator rerun -> Given M0 只能推进规划、source map、前置和 blocker 证据 When 复跑规划包 BDD/TDD 与 roadmap 校验器 Then 规划结构必须仍证明 62 AC / 62 TC / 16 BDD 覆盖，但不得作为进入 M1 的依据。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\roadmap-node-dev-plan\scripts\validate_node_dev_plan.py --task-dir E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- Decision: 规划包结构继续通过；M0 仍因 31 个 SOURCE blocker 不准出，主线程不得进入 M1。

## M0 - Blocker Key Consistency Audit

- BDD: M0 blocker key consistency audit -> Given `real:check` 输出 31 个 SOURCE blocker When 复核 `result.json` 和 `blocker-inventory.md` Then 每个 SOURCE blocker 的精确 key 必须能在结构化清单中定位，且 M0/M1 边界不变。
- RED: blocker key consistency script -> FAIL，expected reason: `hardcodedPqcInspectionItems`、`defaultPqcInspectionType`、`defaultPqcInspectionQuantity`、`defaultPqcScrapQuantity`、`defaultProductionQuantityFactorInAutoSchedule` 未以精确 key 形式落入 `blocker-inventory.md`。
- TASK_DOCS: `blocker-inventory.md` -> UPDATED，为 RRM-BLK-022、RRM-BLK-023、RRM-BLK-024、RRM-BLK-025、RRM-BLK-028 补齐 `result.json` 中的精确 SOURCE key。
- GREEN: blocker key consistency script -> PASS，`resultStatus=BLOCKED`、`sourceCount=31`、`envCount=0`、`runtimeCount=0`、`openBlockedCount=31`、`resolvedVerifiedCount=1`、`missingSourceKeysInInventory=[]`。
- Decision: M0 仍为 `blocked`；本轮只修正 M0 blocker 证据一致性，未新增生产代码、未提交、未执行 `git push`。

## M0 - M1 Boundary Guard Documentation

- BDD: M0 boundary guard after user challenge -> Given 用户再次确认 M0 未完成不应启动 M1 When 复核 `task-state.json`、`result.json`、`task.md` 和 `blocker-inventory.md` Then 主线程必须明确停在 M0，所有 M1-M6 表述只能作为 blocker 归属、后续解决方向或未来验证入口。
- TASK_DOCS: `task.md` -> UPDATED，新增 `Milestone Boundary Guard`，明确 M0 未准出前不得新增 M1-M6 生产代码、不得运行 M1 实现闭环、不得把后续 AC 标记为 GREEN/ACCEPTED。
- TASK_DOCS: `blocker-inventory.md` -> UPDATED，`Verification Methods` 增加当前门禁说明，并将 M1/M3/M4/M5 命令标注为 future gate，避免误读为当前主线程已启动后续里程碑。
- RED: `pnpm e2e:role-requirement-matrix:real:check` without M0 fixture environment -> FAIL，expected reason: 未注入本机授权租户、六角色账号、签名、工单、路线和规程夹具环境变量；该结果不作为 M0 当前 SOURCE 准出口径。
- GREEN: structural boundary guard validation -> PASS，`task-state.json.status=blocked`、`currentMilestone=M0`、`M1.blockedBy=M0`，且 `task.md` / `blocker-inventory.md` / `execution-log.md` / `test-report.md` / `verification-report.md` 均包含 M0/M1 边界保护证据。
- GREEN: blocker key consistency validation -> PASS，当前 `result.json` 为 `source=31`、`env=0`、`runtime=0`、`OPEN_BLOCKED=31`、`RESOLVED_VERIFIED=1`，31 个 SOURCE key 均可在 `blocker-inventory.md` 定位。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with authorized M0 fixture env -> EXPECTED_BLOCKED，31 个 SOURCE blocker；0 ENV；0 RUNTIME。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS，仅 LF/CRLF 工作区警告。
- BLOCKER: M0 gate -> 当前 `result.json` 仍为 31 个 SOURCE blocker、0 ENV、0 RUNTIME；M0 继续 `blocked`，M1-M6 继续 blocked by upstream。
- Decision: 本轮只收紧 M0/M1 边界文档；未新增生产代码、未提交、未执行 `git push`。

## M0 - Future RED Plan Register

- BDD: M0 blocker-to-RED planning -> Given M0 仍有 31 个 SOURCE blocker When 将 blocker 转换为后续 TDD 入口 Then 必须只记录 future RED 计划和依赖关系，不得执行 M1-M6 生产实现或把后续命令当作当前 GREEN。
- TASK_DOCS: `blocker-inventory.md` -> UPDATED，新增 `Future RED Plan Register`，按 activeOrderId、生产系数、QA/PQC、PQC 前端、调拨/放行、三类路线配置分离六组登记 covered blocker IDs、earliest allowed milestone、required precondition、future RED command、expected RED reason 和当前 M0 blocked 下是否允许执行。
- GREEN: future RED plan coverage validation -> PASS，`Future RED Plan Register` 覆盖 RRM-BLK-001..031 全部 31 个 SOURCE blocker，未包含已关闭的 RUNTIME blocker RRM-BLK-032。
- GREEN: UTF-8/docs structure validation -> PASS，`blocker-inventory.md`、`execution-log.md`、`test-report.md`、`verification-report.md` 均可读取并包含 future RED plan 证据。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS，仅 LF/CRLF 工作区警告。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with authorized M0 fixture env -> EXPECTED_BLOCKED，31 个 SOURCE blocker；0 ENV；0 RUNTIME。
- BLOCKER: M0 gate -> future RED 计划不解除任何 SOURCE blocker；当前 `result.json` 仍需保持 31 SOURCE / 0 ENV / 0 RUNTIME 的 blocked 口径。
- Decision: 本轮只补齐后续 RED 计划登记；未新增生产代码、未提交、未执行 `git push`。

## M0 - Design Constraint and Execution Permission Clarification

- BDD: M0 no-fallback design constraint clarification -> Given M0 使用本机临时 QA 夹具和任务数据 When 记录设计约束检查 Then 必须明确这些夹具只用于预检，不是 fallback、临时补丁、绕过或正式 source model。
- TASK_DOCS: `task.md` -> UPDATED，`设计约束检查` 明确 `real:check` 对缺正式来源保持 fail-fast / BLOCKED，且 `m0-derived-qa-regulation.md`、工单、调拨和签名数据只作为 M0 预检夹具。
- TASK_DOCS: `blocker-inventory.md` -> UPDATED，新增 `Current M0 Execution Permission`，逐项列明当前允许的 M0 证据/预检工作与禁止的 M1-M6 实现闭环。
- GREEN: design constraint / execution permission docs validation -> PASS，`task-state.json.status=blocked`、`currentMilestone=M0`，且 `task.md`、`blocker-inventory.md`、`execution-log.md`、`test-report.md`、`verification-report.md` 均包含本轮无 fallback / 无绕过边界证据。
- GREEN: result/inventory consistency validation -> PASS，当前 `result.json` 仍为 `status=BLOCKED`、31 SOURCE、0 ENV、0 RUNTIME，且 31 个 SOURCE key 均可在 `blocker-inventory.md` 定位。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS，仅 LF/CRLF 工作区警告。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with authorized M0 fixture env -> EXPECTED_BLOCKED，31 个 SOURCE blocker；0 ENV；0 RUNTIME。
- BLOCKER: M0 gate -> 临时夹具不解除 activeOrderId、QA 规程版本、PQC task 或 ERP 关系正式模型 blocker。
- Decision: 本轮只澄清无 fallback / 无绕过边界；未新增生产代码、未提交、未执行 `git push`。

## M0 - Planning Package Supervisor Script Precheck

- BDD: M0 supervisor script precheck -> Given 目标要求按规划包顺序推进 When 使用 development-plan supervisor 的状态脚本复核规划包 Then 若脚本无法识别当前规划包结构，只能记录工具适配缺口，不能据此推进 M1。
- RED: `python C:\Users\BJB110\.codex\skills\development-plan-delivery\scripts\init_or_resume_task.py --cwd E:\IntRuoyi --task-dir doc/tasks/20260801-role-requirement-matrix-excel` -> FAIL，expected reason: `development-plan.md does not contain any milestone headings`；当前规划包使用 `## Milestones` 表格结构，不符合该脚本的标题式 milestone parser。
- RED: `python C:\Users\BJB110\.codex\skills\development-plan-delivery\scripts\render_plan_status.py --cwd E:\IntRuoyi --task-dir doc/tasks/20260801-role-requirement-matrix-excel` -> FAIL，expected reason: `task-state.json` 缺少该脚本期望的 `task_id` 字段。
- GREEN: planning package direct read -> PASS，`development-plan.md`、`prd.md`、`test-plan.md`、`task-state.json` 均存在且可 UTF-8 读取；规划包 `task-state.json.currentMilestone=M0`，所有 M1-M6 acceptance 仍为 pending。
- BLOCKER: supervisor tooling contract -> 该脚本预检失败不解除 M0 SOURCE blocker，不授权进入 M1；当前实施任务仍以 `doc/tasks/20260801-role-requirement-matrix-implementation/task-state.json`、`real:check` 和 M0 gate audit 为准。
- Decision: 本轮只记录 supervisor 工具适配证据；未修改规划包、未新增生产代码、未提交、未执行 `git push`。

## M0 - User Challenge Continuation Recheck

- BDD: M0 no-M1 continuation recheck -> Given 用户指出 M0 未完成时主线程不应进入 M1 When 重新读取目标提示词、规划包、任务状态、验证报告和真实前置结果 Then 只能继续 M0 证据复核，保持 `currentMilestone=M0`，并确认 M1 仍 blocked by M0。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，返回 `status=UP`。
- GREEN: task/result/inventory consistency validation -> PASS，`task-state.json.status=blocked`、`currentMilestone=M0`、`M1.blockedBy=M0`，`result.json` 为 31 SOURCE / 0 ENV / 0 RUNTIME，且 31 个 SOURCE key 均可在 `blocker-inventory.md` 定位。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with authorized M0 fixture env -> EXPECTED_BLOCKED，31 个 SOURCE blocker；0 ENV；0 RUNTIME。
- BLOCKER: M0 gate -> 31 个 SOURCE blocker 未减少；当前不允许进入 M1 schema/service/frontend/backend 实现，不允许把 M1-M6 AC 标记为 GREEN/ACCEPTED。
- Decision: 本轮只复核并同步 M0 边界证据；未新增生产代码、未提交、未执行 `git push`。

## M0 - Gate Redefinition Accepted

- CHANGE_REQUEST: `docs/changes/20260802-role-requirement-matrix-m0-gate-redefinition.md` -> CREATED，记录用户明确调整 M0 门禁口径：M0 只负责识别并结构化冻结 SOURCE blocker，不要求在 M0 清零这些需要 M1-M5 正式实现的 blocker。
- BDD: M0 revised gate -> Given `real:check` 当前只有 SOURCE blocker 且无 ENV/RUNTIME blocker When 31 个 SOURCE blocker 已结构化记录并归属到 M1-M5 Then M0 可以标记 accepted，当前里程碑切换到 M1，但 M2-M6 仍不得越级。
- TASK_DOCS: `doc/tasks/20260801-role-requirement-matrix-excel/development-plan.md` -> UPDATED，M0 Gate 增加“SOURCE blocker 只需结构化冻结并归属到 M1-M5”的准出口径。
- TASK_DOCS: `task-state.json` -> UPDATED，`status=in_progress`、`currentMilestone=M1`、`M0.status=accepted`、`M1.status=in_progress`，M1 active slice 为 `M1 activeOrderId authority`。
- TASK_DOCS: `task.md`、`m0-gate-audit.md`、`blocker-inventory.md`、`test-report.md`、`verification-report.md`、`m0-preflight.md`、`m0-test-data.md`、`database-schema-evidence.md`、`role-requirement-matrix-real-e2e-evidence.md` -> UPDATED，同步 revised M0 gate 和 M1 启动边界。
- M0_GATE: `pnpm e2e:role-requirement-matrix:real:check` latest evidence -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，31 个 SOURCE blocker；0 ENV；0 RUNTIME；按新口径不再阻塞 M0 exit。
- GREEN: revised M0 gate consistency script -> PASS，`task-state.json.status=in_progress`、`currentMilestone=M1`、`M0.status=accepted`、31 SOURCE / 0 ENV / 0 RUNTIME，且 31 个 SOURCE key 均可在 `blocker-inventory.md` 定位。
- GREEN: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs/changes/20260802-role-requirement-matrix-m0-gate-redefinition.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\roadmap-node-dev-plan\scripts\validate_node_dev_plan.py --task-dir E:\IntRuoyi\doc\tasks\20260801-role-requirement-matrix-excel` -> PASS。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- M0_PREFLIGHT: `pnpm e2e:role-requirement-matrix:real:check` with authorized M0 fixture env -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，exit code 2，31 SOURCE / 0 ENV / 0 RUNTIME。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: `git diff --check -- doc/tasks/20260801-role-requirement-matrix-implementation doc/tasks/20260801-role-requirement-matrix-excel docs/changes/20260802-role-requirement-matrix-m0-gate-redefinition.md IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M0 accepted by revised gate；当前允许进入 M1 activeOrderId 权威来源切片 RRM-BLK-001..007；M2-M6 仍 blocked by dependencies；本轮未新增生产代码、未提交、未执行 `git push`。

## M1 - ActiveOrder Authority Source Switch

- BDD: M1 activeOrder authority source switch -> Given M0 已按新口径 accepted 且 M1 只允许处理 RRM-BLK-001..007 When PQC 查询活跃订单和真实 source blocker 检查运行 Then PQC 订单列表必须只读取统一 `mes_pro_process_pool_active_order`，`real:check` 必须识别 M1 新迁移已替换旧 `leader_user_id` 唯一键，不得继续用旧 P1 SQL 误报 RRM-BLK-006。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: `NoSuchFieldException: routeId`。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，13 tests，0 failures/errors。
- RED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，expected reason: `real E2E script must include ACTIVE_ORDER_AUTHORITY_SQL`。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: `MesFrontlinePqcContextServiceImpl` 构造器缺 `MesProcessPoolActiveOrderMapper`，且 `MesProcessPoolActiveOrderMapper` 缺 `selectActiveList` / `selectActiveByWorkOrderAndRoute`。
- GREEN: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests，0 failures/errors。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，19 tests，0 failures/errors。
- REGRESSION: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- E2E: authorized `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，24 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-001..007 不再出现在 `result.json`。
- TASK_DOCS: `task-state.json`、`task.md`、`blocker-inventory.md`、`backend-api-evidence.md`、`database-schema-evidence.md` -> UPDATED，M1 标记为 accepted，当前里程碑切换到 M2，M2 仅允许先做 RRM-BLK-026..028 的 BDD/RED。
- Decision: M1 activeOrderId authority source gate accepted；当前可以进入 M2，但不得越级实现 M3-M6；本轮仍不执行 `git push`。

## M1 - Evidence Sync Verification

- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: UTF-8 / JSON structural read -> PASS，实施包和规划包 `task-state.json`、`result.json` 可解析，任务文档可 UTF-8 读取。
- GREEN: `pnpm e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，19 tests，0 failures/errors。
- E2E: authorized `pnpm e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，24 SOURCE / 0 ENV / 0 RUNTIME。
- GREEN: result/inventory consistency script -> PASS，24 个当前 SOURCE key 均在 `blocker-inventory.md`，RRM-BLK-001..007 均为 `RESOLVED_VERIFIED`。
- GREEN: `git diff --check -- <M1 owned files and task docs>` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M2 已设为当前里程碑；下一步必须先为 RRM-BLK-026..028 写 M2 BDD/RED，不得直接实现 M3-M6。

## M2 - Production Coefficient And Process Target Snapshots

- BDD: M2 production coefficient snapshots -> Given M1 activeOrderId authority 已 accepted 且 M2 只允许处理 RRM-BLK-026..028 When 活跃订单加入、FIFO 分配、手工分配、报工确认和工序完成运行 Then 每个订单工序必须冻结 ERP 数量、生产系数和计划数量，分配/完成必须读取该快照，自动排产缺系数必须 fail-fast 而不是默认 `1`。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: missing `MesProcessPoolActiveOrderProcessSnapshotDO`、`MesProcessPoolActiveOrderProcessSnapshotMapper`、`MesTeamLeaderOrderProcessTargetService`、`MesTeamLeaderOrderProcessTarget`。
- IMPLEMENTING: 新增 `mes_pro_process_pool_active_order_process_snapshot` migration、DO、Mapper 和 `MesTeamLeaderOrderProcessTargetService`；`MesTeamLeaderActiveOrderServiceImpl` 在加入活跃订单时冻结逐工序系数/计划数量；FIFO、手工分配、报工确认和工序完成改读目标数量服务；`MesProAutoScheduleServiceImpl` 移除默认生产系数路径。
- GREEN: UTF-8 BOM repair -> PASS，`MesTeamLeaderActiveOrderServiceTest.java`、`MesTeamLeaderFifoAllocationServiceTest.java`、`MesTeamLeaderOrderProcessCompletionServiceTest.java` 均确认 `BOM=False`。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProAutoScheduleContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，25 tests，0 failures/errors。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- REGRESSION: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，21 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-026..028 不再出现在 `result.json`。
- TASK_DOCS: `task-state.json`、`task.md`、`blocker-inventory.md`、`source-map.md`、`backend-api-evidence.md`、`database-schema-evidence.md`、`test-report.md`、`verification-report.md` -> UPDATED，M2 标记为 accepted，当前里程碑切换到 M3，M3 仅允许先做 RRM-BLK-017..025 的 BDD/RED。
- Decision: M2 production coefficient snapshots source gate accepted；当前可以进入 M3，但不得越级实现 M4-M6；本轮仍不执行 `git push`。

## M3 - QA Regulation And PQC Source Model

- BDD: M3 QA regulation and PQC source model -> Given M1/M2 已 accepted 且 M3 只允许处理 RRM-BLK-017..025 When PQC 上下文、提交和前端表单运行 Then PQC 必须从已发布 QA 规程版本和 PQC task 身份读取检验项目、检验类型、日期、班次、轮次、计划数量和逐件明细，不能依赖最新生产事件、前端硬编码项目或默认数量。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M2 -> EXPECTED_BLOCKED_FOR_M3，21 SOURCE / 0 ENV / 0 RUNTIME；expected reason: RRM-BLK-017..025 仍包含 QA 规程所有权、规程版本、PQC task、逐件明细、提交来源和前端默认/硬编码检验项缺口。
- IMPLEMENTING: 新增 QA 规程正式 schema / DO / Mapper，新增 PQC task 和逐件明细 schema / DO / Mapper；`MesFrontlinePqcContextServiceImpl` 改为读取已发布规程和待提交 PQC task，并按 `activeOrderId + pqcTaskId + regulationVersionId + task identity` 提交；`FrontlineFixedTemplatePanel.vue` 改为从 `inspectionItems` 和任务快照动态渲染/提交。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，8 tests，0 failures/errors。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-qa-regulation:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-dynamic-form:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after M3 implementation -> EXPECTED_BLOCKED_WITH_M3_FALSE_POSITIVE，13 SOURCE / 0 ENV / 0 RUNTIME；expected reason: `hardcodedPqcInspectionItems` 仍由 real-flow 源码扫描把动态 `const pqcInspectionItems = computed(...)` 误判为硬编码项目。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 的 `hardcodedItemPattern` 收窄为硬编码 union / `PQC_INSPECTION_ITEMS` / 四项对象字面量，不再把动态 computed 变量名当作硬编码。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-dynamic-form:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_DOWNSTREAM，12 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-017..025 不再出现在 `result.json`，剩余 SOURCE 全部归属 M4/M5。
- TASK_DOCS: `task-state.json`、`task.md`、`blocker-inventory.md`、`source-map.md`、`backend-api-evidence.md`、`database-schema-evidence.md`、`test-report.md`、`verification-report.md` -> UPDATED，M3 标记为 accepted，当前里程碑切换到 M4；本轮未启动 M4 生产代码、未提交、未执行 `git push`。
- Decision: M3 QA/PQC source gate accepted；当前可以进入 M4，但不得越级实现 M5-M6；本轮仍不执行 `git push`。

## M3 - Evidence Sync Verification

- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260801-role-requirement-matrix-implementation/database-schema-evidence.md` -> PASS。
- GREEN: state/result/inventory consistency script -> PASS，`currentMilestone=M4`、`M3.status=accepted`、`result.json` 为 12 SOURCE / 0 ENV / 0 RUNTIME，且 RRM-BLK-017..025 均为 `RESOLVED_VERIFIED`。
- GREEN: stale M3 blocker phrase scan -> PASS，任务文档不再包含旧的“当前进入 M3 / 21 个 SOURCE / M3 当前 blocker”口径。
- GREEN: UTF-8 / JSON structural read -> PASS。
- GREEN: `git diff --check -- <M3 owned files and task docs>` -> PASS；仅 LF/CRLF 工作区警告。
- Decision: M3 evidence sync complete；当前任务保持 `in_progress` / `currentMilestone=M4`，不执行 `git push`。

## M4 - Transfer Trace And Release Source Model

- BDD: M4 transfer trace and start check -> Given M1-M3 已 accepted 且 activeOrderId 是统一订单身份 When 一个活跃订单存在发货、补料、退料和多批次调拨事实 Then 系统必须用正式 activeOrderId 关系表追溯 transfer/shipment/replenishment/return/batch/material stock 来源，缺来源时保持阻塞而不是按单号或默认库存通过。
- BDD: M4 release completeness source checks -> Given eDHR 放行预检需要检验、偏差、返工、报废和库存五类来源 When 执行放行预检 Then 五项必须通过正式 source adapter 生成 PASS/BLOCKER/NOT_APPLICABLE，不得继续由 `buildSourceNotIntegratedItem` 占位，也不得把来源缺失改为默认 PASS。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-transfer-start-check:static` -> FAIL，expected reason: 缺少 activeOrderId 到调拨/发货/补料/退料/批次库存的正式关系源，M4 静态合同仍能识别 RRM-BLK-013..016。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL/TIMEOUT，expected reason: M4 schema/source adapter 尚未实现，且一次 generated `target\classes` 诊断输出损坏导致标准 Maven 不能完成；该诊断不作为 GREEN。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> FAIL，expected reason: 既有 `service/pro/batchrecord/MesProEdhrReleaseServiceImplTest` 缺少新引入的 `MesOrderReleaseCompletenessService` bean / mock。
- IMPLEMENTING: 新增 `mes_pro_process_pool_active_order_transfer_trace` migration、DO、Mapper 和 `MesActiveOrderTransferTraceService`；新增 `MesOrderReleaseCompletenessService` 五类正式来源适配方法；`MesProEdhrReleaseServiceImpl` 放行预检改为调用正式 adapter；既有放行服务测试显式 mock 新依赖。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` from `IntRuoyiBackend` -> PASS，21 tests，0 failures/errors。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceSchemaTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS，21 tests，0 failures/errors。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-transfer-start-check:static` -> PASS。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> EXPECTED_BLOCKED_FOR_M5，3 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-008..016 不再出现在 `result.json`，剩余 SOURCE 为 RRM-BLK-029..031。
- TASK_DOCS: `task-state.json`、`task.md`、`blocker-inventory.md`、`source-map.md`、`backend-api-evidence.md`、`database-schema-evidence.md`、`role-requirement-matrix-real-e2e-evidence.md` -> UPDATED，M4 标记为 accepted，当前里程碑切换到 M5。
- TOOLING_NOTE: `development-plan-delivery` 通用脚本要求 `development-plan.md/prd.md/test-plan.md` 与当前 task-dir 同目录；本任务历史结构将规划包放在 `doc/tasks/20260801-role-requirement-matrix-excel`，实现包用 `planningPackage` 字段引用，因此脚本预检失败只记录为工具适配证据，不解除或阻塞 M5 业务门禁。
- Decision: M4 transfer/release source gate accepted；当前可以进入 M5 route batch-record/formBindings separation 切片，但不得越级实现 M6；本轮仍不执行 `git push`。

## M5 - Route Batch-record And FormBindings Separation

- BDD: M5 route configuration separation -> Given 工艺路线同一工序同时存在“工序开始”、正式逐工序批记录绑定和 `formBindings` 表单槽位 When 前端字段明细、节点状态、保存/复制和 eDHR 运行态创建任务 Then `batchRecordFormNames` 只能来自正式批记录绑定，`formBindings` 只能作为表单槽位，缺少 `formSlotType` 必须 fail-fast，不得默认 `MAIN` 或用工序开始配置替代。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static` -> FAIL，expected reason: `normalizeRecordBindingSlotType` 对缺失槽位默认 `MAIN`，且运行态存在缺失槽位被当作正式批记录的风险。
- IMPLEMENTING: `RouteFlowGraphDesigner.vue` 移除缺槽位默认 `MAIN`，新增/使用显式 `resolveRecordBindingSlotType` / `requireRecordBindingSlotType`；共享 key、附加表单数量、保存和复制链路不再伪造 `MAIN`。`MesProEdhrBatchExecutionServiceImpl.resolveRouteFormSlotType` 对空值或非法槽位 fail-fast，不再 `blankToDefault(..., FORM_SLOT_MAIN)`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 将粗粒度存在性检查收敛为 value/link/border 三类分离检查，证明 `batchRecordFormNames` 与 `formBindings` 同屏存在但互不替代。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-matrix-route-config-separation-static.spec.cjs` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` from `IntRuoyiBackend` -> PASS，reactor BUILD SUCCESS，`yudao-module-mes` 编译通过。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME；RRM-BLK-029..031 不再出现在 `result.json`。
- TOOLING_NOTE: targeted `MesProEdhrBatchExecutionServiceTest` Maven run exceeded the earlier timeout while still in Java compile; PID 33048/32784 was confirmed task-owned by command line, diagnosed with `jcmd`, then stopped. No test PASS is claimed from that timed-out command; M5 backend acceptance uses the focused static contract plus standard backend compile and real:check PASS.
- TASK_DOCS: `task-state.json`、`task.md`、`blocker-inventory.md`、`source-map.md`、`test-report.md`、`verification-report.md`、`role-requirement-matrix-real-e2e-evidence.md` -> UPDATED，M5 标记为 accepted，当前里程碑切换到 M6。
- Decision: M5 route configuration separation source gate accepted；当前可以进入 M6 migration/concurrency/performance/full-real-E2E gate；本轮仍不执行 `git push`。

## M5 - Daily Close And Scope Coverage Recheck

- BDD: M5 daily-close unresolved item surface -> Given M5 不仅包含路线三类配置分离，还包含日结、范围、权限、审计与快照 When 班组长工作台加载当前提交列表、活跃订单和错误状态 Then 页面必须提供可见“日结”待处理面，按真实页面状态暴露待复核、异常和加载阻塞，不能用默认成功或隐藏空白替代。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` -> FAIL，expected reason: `TeamLeaderWorkbenchPage.vue` 缺少 `data-role-matrix-daily-close` / `dailyClose` / `日结` 可见面，M5 不能作为整体 accepted。
- TASK_DOCS: `task-state.json`、`task.md` -> UPDATED，当前里程碑从 M6 退回 M5，M6 改为 pending / blocked by M5 daily-close/scope gate。
- BDD: M5 extended scope authority -> Given 班组长负责范围不只包含员工、工序和工位 When 日结、维护和范围授权判断读取 scope model Then 系统必须显式表达生产线、设备和订单 scope，并对缺失授权 fail-fast，不能默认扩大到全局范围。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` -> FAIL，expected reason: `SCOPE_TYPE_PRODUCTION_LINE` 缺失，scope model 只覆盖员工、工序、工位。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: 新增范围测试先失败于缺 `assertCanMaintainProductionLine` / `assertCanMaintainEquipment` / `assertCanMaintainOrder` 和 `SCOPE_TYPE_PRODUCTION_LINE` / `SCOPE_TYPE_EQUIPMENT` / `SCOPE_TYPE_ORDER`。
- IMPLEMENTING: `TeamLeaderWorkbenchPage.vue` 新增 `data-role-matrix-daily-close` 日结待处理看板，按真实 `submissionList`、`activeOrderOptions` 和 `loadError` 展示待复核、复核不正确、活跃订单和加载阻塞；不返回默认成功。
- IMPLEMENTING: `MesProcessPoolTeamLeaderScopeDO` / `MesTeamLeaderScopeService` / `MesTeamLeaderScopeServiceImpl` / `20260730_mes_process_pool_team_leader.sql` / `20260802_mes_process_pool_team_leader_scope_extended.sql` 增加 `PRODUCTION_LINE`、`EQUIPMENT`、`ORDER` 三类 scope 字段、索引和显式断言入口。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-daily-close-scope:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- BLOCKED: standard targeted Maven rerun remained task-owned but timed out in `WinNTFileSystem.delete0` / `IncrementalBuildHelper.beforeRebuildExecution`; PID 616 / 34648 were confirmed task-owned and stopped. Non-incremental verification then failed before target tests on unrelated existing sources: `ProcessPoolReviewCopyGenerateSubmitReqVO.FieldMapping`, `ProcessPoolTimelineDetailRespVO.ReadonlyActions`, `MesWmMiscReceiptStatusEnum` Lombok constructor/getter errors. No Maven GREEN is claimed for the backend scope slice.
- TASK_DOCS: `task-state.json`、`task.md`、`test-report.md`、`verification-report.md` -> UPDATED，M5 remains `in_progress` / M6 remains `pending` until backend verification blocker is cleared；本轮仍不执行 `git push`。

## M5 - Backend Verification Recovered And Accepted

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesProcessPoolTeamLeaderSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests，0 failures/errors，BUILD SUCCESS。
- TASK_DOCS: `task-state.json`、`task.md`、`test-report.md`、`verification-report.md` -> UPDATED，M5 标记为 accepted，当前里程碑切换到 M6。
- Decision: M5 daily-close/scope/backend target gates accepted；当前可以进入 M6 migration/concurrency/performance/full-real-E2E gate；本轮仍不执行 `git push`。

## M6 - Full Validation Entry Gate

- BDD: M6 full validation gate -> Given M0-M5 已 accepted 且 `real:check` 前置齐全 When 执行迁移、并发、性能、release 覆盖和真实 Playwright 全链路验证 Then M6 必须暴露所有剩余 blocker，不能把静态合同、API-only 或登录预检冒充 62 AC 全链路通过。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` with authorized local fixture env -> PASS，真实前置齐全且凭据未写入证据。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> BLOCKED，expected reason: 脚本仍 fail-fast 报告 “M6 全链路真实 E2E 尚未实现；完成 M1-M5 ACCEPTED 后必须扩展本脚本覆盖 62 个 AC。”
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:team-leader-workbench:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:frontline-formal-submit:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:frontline-team-config:static` -> PASS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:team-leader-report-allocation:static` -> PASS。
- RED: `pnpm --dir IntRuoyiFronted e2e:edhr:release:check` -> FAIL，expected reason: `src/api/mes/pro/edhr/releaseDossierRequirementSetting.ts` was not covered by the release coverage matrix.
- IMPLEMENTING: `package.json` 新增 `e2e:edhr:release-dossier-requirement:check` / `e2e:edhr:release-dossier-requirement`；`edhr-release-e2e-coverage-gate.mjs` 新增 `release-dossier-requirement/setting` feature，绑定既有 API、Profile 配置页、release check presentation、真实 E2E 和根任务证据。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:edhr:release-dossier-requirement:check` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:edhr:release:check` -> PASS，features=15，checkScripts=12，syntaxFiles=12。
- REGRESSION: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，58 tests，0 failures/errors，BUILD SUCCESS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- BLOCKER: M6 full real E2E -> 当前脚本只证明前置和六角色登录导航能力，尚未实现 62 AC 全链路页面动作、失败路径、权限隔离、并发/性能数据、清理和 launch/readiness 证据；M6 保持 `in_progress`。

## M6 - Real E2E Coverage Ledger Slice

- BDD: M6 structured AC coverage ledger -> Given M6 必须证明 62 个 AC 的真实页面动作、失败路径和只读核验 When 真实 E2E 仍无法一次性完成全部业务动作 Then 脚本必须从测试计划加载 62 AC，记录已观察页面阶段，并把未达到 ACCEPTED 的 AC 逐项结构化为 blocker，不能再用泛化“未实现”占位。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 扩展静态合同，要求真实 E2E 脚本包含 `loadAcceptanceMatrix`、`M6_REAL_FLOW_PHASES`、`buildAcceptanceCoverage`、`assertAcceptanceCoverage`、`acceptanceCoverage`、`phaseEvidence`，并禁止保留泛化 “M6 全链路真实 E2E 尚未实现” 占位。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `loadAcceptanceMatrix` 和 M6 AC coverage ledger。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增从 `test-plan.md` 解析 62 AC、六角色 M6 页面阶段表面检查、coverage ledger、结构化 `E2E_COVERAGE` blocker 和 evidence 输出；删除末尾泛化未实现 fail-fast。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- REGRESSION: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` first rerun -> FAIL，expected reason: 入口级阶段错误地等待隐藏复核弹窗控件 `data-team-leader-fifo-allocation`。
- IMPLEMENTING: 将当前切片的真实阶段判据收敛为稳定入口级可见区域；PQC 组长阶段先点击 `PQC 组长` 页签，再等待工作台和日结入口，后续写动作另行作为独立 M6 RED/GREEN。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；六角色入口/页面阶段均执行，`phaseEvidence=6`，`surfaceObserved=40`，`uncovered=22`，`pending=62`，`blockers=62`，结果已写入 `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` 和 `role-requirement-matrix-real-e2e-evidence.md`。
- Decision: M6 仍保持 `in_progress`；本轮只完成真实 E2E coverage ledger 和入口阶段证据，未把任何 AC 标记为 full ACCEPTED，后续继续按 AC 切片补真实动作、失败路径、并发/性能和清理证据。

## M6 - AC-M04 Active Order Join Idempotency And Action Evidence

- BDD: AC-M04 activeOrderId duplicate join idempotency -> Given 球囊扩张压力泵路线 v21 的授权工单已经存在统一 activeOrderId When 生产组长再次通过真实页面加入同一 `workOrderId + routeId + routeVersionId` Then 系统必须返回同一个 activeOrderId，重复加入和并发唯一键竞争不得报“系统异常”，且不得重复插入快照或审计。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> FAIL，expected reason: `joinActiveOrder` 真实页面动作命中 `uk_mes_pp_active_order` 重复活跃订单唯一键后返回“系统异常”，AC-M04 重复加入失败路径未被幂等处理。
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest` -> 新增同一 `workOrderId/routeId/routeVersionId` 已活跃时直接返回既有 activeOrderId 的测试，并新增 `DuplicateKeyException` 并发竞争后重新读取既有 activeOrderId 的测试。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: `MesTeamLeaderActiveOrderServiceImpl.addActiveOrder(...)` 尚未先查既有活跃订单，也未在唯一键竞争后重新读取同一 activeOrderId。
- IMPLEMENTING: `MesProcessPoolActiveOrderMapper` 新增 `selectActiveByWorkOrderRouteVersion(...)`；`MesTeamLeaderActiveOrderServiceImpl.addActiveOrder(...)` 改为先读既有 activeOrderId，插入遇到 `DuplicateKeyException` 时重新读取同一业务键，仍然 fail-fast 抛出真实异常；既有记录路径不写重复快照和审计。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 将活跃订单列表刷新失败记录为结构化 `activeOrderListResponseError` blocker，避免真实页面动作后的列表等待失败被吞成非结构化异常。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests，0 failures/errors，BUILD SUCCESS。
- REGRESSION: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- REGRESSION: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- REGRESSION: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，60 tests，0 failures/errors，BUILD SUCCESS。
- RUNTIME: int_main backend restored on `backend-runtime-control-20260802-170535.jar`; `/actuator/health` -> `UP`；authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`joinActiveOrder` action evidence PASS，`activeOrderId=12`、`workOrderId=980008`、`routeId=922119`、`routeVersionId=448`；coverage ledger 为 `total=62`、`accepted=0`、`actionObserved=1`、`surfaceObserved=39`、`uncovered=22`、`pending=62`、`blockers=62`。
- Decision: AC-M04 现在具备真实页面动作证据和后端幂等/并发唯一键 GREEN，但仍不是 `ACCEPTED`；还缺 AC-M04 的冲突路线失败路径、跨角色只读核验、权限隔离和完整清理证据。M6 保持 `in_progress`，62 个 AC 仍按 `E2E_COVERAGE` blocker 逐项推进。

## M6 - AC-M04 Conflicting Route Fail-fast Slice

- BDD: AC-M04 conflicting route fail-fast -> Given 一个生产工单已经由正式排产绑定到路线和路线版本 When 生产组长请求把同一工单加入另一个 `routeId/routeVersionId` Then 系统必须在插入活跃订单前 fail-fast，不能先写入 active order 后再依赖事务回滚或后续快照失败掩盖冲突。
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest#shouldRejectConflictingRouteBeforeInsertingActiveOrder` -> 覆盖同一工单请求错误路线版本时抛出 `PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED`，并断言不调用 active order insert、process snapshot insert 和 audit insert。
- RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: 服务在发现排产路线不匹配前已经调用 `activeOrderMapper.insert(...)`。
- IMPLEMENTING: `MesTeamLeaderActiveOrderServiceImpl.addActiveOrder(...)` 将正式排产路线/路线版本校验前移到 active order insert 之前；同路线重复加入仍直接返回既有 activeOrderId，并发唯一键竞争仍重新读取既有 activeOrderId，不新增 fallback。
- GREEN_RETRY: same Maven command first rerun -> TIMEOUT/FAIL evidence found in surefire report，expected reason: 新前置校验要求并发唯一键测试提供匹配排产路线夹具；未宣称 GREEN。
- TEST_FIXTURE: `MesTeamLeaderActiveOrderServiceTest#shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey` -> 补齐匹配 `MesProScheduleOrderDO` 夹具，保持并发唯一键语义不变。
- GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests，0 failures/errors，BUILD SUCCESS。
- REGRESSION: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，61 tests，0 failures/errors，BUILD SUCCESS。
- Decision: AC-M04 现在具备后端重复加入、并发唯一键和冲突路线前置拒绝证据，但仍缺真实页面冲突路线失败路径、跨角色只读核验、权限隔离、清理和 M6 性能/上线门禁；M6 继续保持 `in_progress`，不执行 `git push`。

## M6 - AC-M04 Real Page Conflict Route And Cross-role Evidence

- BDD: AC-M04 real-page conflict route and cross-role read-only -> Given 生产组长已通过真实页面加入球囊扩张压力泵路线 v21 的活跃订单 When 同一页面再提交错误路线并由 PQC 检验员打开只读 PQC 页面 Then 错误路线必须以真实业务错误 fail-fast 且不新增 activeOrder，PQC 必须只读看到同一 activeOrderId。
- TEST_ADDED: `IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyActiveOrderConflictRouteFailure` 和 `activeOrderConflictRouteRejected`，防止只保留后端单测而缺真实页面失败路径。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyActiveOrderConflictRouteFailure`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增生产组长页面冲突路线提交、业务失败响应断言、页面错误提示断言和登录态只读列表复核；生产组长阶段现在记录 `joinActiveOrder` 与 `activeOrderConflictRouteRejected` 两个 action evidence。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` first rerun -> FAILED，expected reason: 冲突路线失败后脚本依赖 `page.reload()` 捕获活跃订单列表刷新，真实页面未稳定触发该列表响应，导致未结构化 timeout。
- IMPLEMENTING: 将冲突路线后的复核改为使用当前页面登录态令牌发起只读 `/admin-api/mes/pro/process-pool/team-leader/active-order/list`，仅用于最终状态核验，不替代真实页面提交动作。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，真实前置仍为 0 SOURCE / 0 ENV / 0 RUNTIME blocker。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=3`、`actionKeys=joinActiveOrder, activeOrderConflictRouteRejected, activeOrderCrossRoleReadOnly`、`actionObserved=3`、`surfaceObserved=38`、`uncovered=21`、`pending=62`、`blockers=62`。
- Decision: AC-M04 现在已有真实页面成功加入、真实页面冲突路线失败路径、PQC 跨角色只读核验和后端重复/并发/冲突路线单测证据；仍未 `ACCEPTED`，因为权限隔离、清理闭环以及 M6 migration/performance/launch gates 尚未完成；本轮仍不执行 `git push`。

## M6 - AC-M04 Permission Isolation Fixture Gate

- BDD: AC-M04 unauthorized active-order mutation isolation -> Given 生产组长已通过真实页面加入球囊扩张压力泵路线 v21 的活跃订单 When 放行负责人等错误角色尝试进入活跃订单维护或调用活跃订单写入 Then 页面不得暴露维护表单，后端必须拒绝写入；若测试账号仍具备通配或维护权限，必须结构化阻塞且不得执行破坏性写入探测。
- TEST_ADDED: `IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyActiveOrderUnauthorizedMutationBlocked`、`activeOrderUnauthorizedMutationBlocked` 和 `/system/auth/get-permission-info`，防止权限隔离只停留在口头 blocker。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyActiveOrderUnauthorizedMutationBlocked`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增当前页面会话权限读取、错误角色权限隔离动作和 `E2E_PERMISSION` action blocker；若账号含 `*:*:*` 或 `mes:pro-process-pool-team-leader:maintain`，直接记录 `BLOCKED`，不发起活跃订单写请求，避免污染共享夹具。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，真实前置仍为 0 SOURCE / 0 ENV / 0 RUNTIME blocker。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED；pnpm lifecycle non-zero wrapping script exit 2；`phaseEvidence=6`、`actionEvidence=4`、`actionKeys=joinActiveOrder, activeOrderConflictRouteRejected, activeOrderCrossRoleReadOnly, activeOrderUnauthorizedMutationBlocked`、`activeOrderUnauthorizedMutationBlocked=BLOCKED/E2E_PERMISSION`、`blockedPermission=mes:pro-process-pool-team-leader:maintain`、`actionObserved=3`、`surfaceObserved=38`、`uncovered=21`、`pending=62`、`blockers=63`。
- Decision: AC-M04 权限隔离现在具备可执行门禁和结构化 blocker；当前 `releaseOwner` 账号仍具备活跃订单维护权限，不能证明错误角色会被后端拒绝。需要调整本机测试角色夹具为不含 `mes:pro-process-pool-team-leader:maintain` / `*:*:*` 的正式错误角色后，才能继续执行写入拒绝核验；本轮仍不执行 `git push`。

## M6 - AC-M04 Cleanup Traceability And Structured Runtime Blockers

- BDD: AC-M04 active-order cleanup traceability -> Given 生产组长已通过真实页面加入球囊扩张压力泵路线 v21 的任务活跃订单 When M6 真实 E2E 进入清理判断 Then 脚本必须先重新定位同一个 `activeOrderId + workOrderId + routeId + routeVersionId`，并在无法安全清理共享夹具时记录结构化 cleanup blocker，不能静默删除或遗漏清理风险。
- TEST_ADDED: `IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyActiveOrderCleanupTraceability` 和 `activeOrderCleanupDeferred`，防止 AC-M04 只记录加入/冲突/权限而缺少任务数据清理门禁。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyActiveOrderCleanupTraceability`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增 activeOrder 清理追溯检查，先通过登录态只读列表重新定位本轮 activeOrder，再将共享 M6 夹具清理窗口缺失记录为 `activeOrderCleanupDeferred / E2E_CLEANUP`；同时将登录接口响应等待超时归入 `loginResponseTimeout` fail-fast 详情，避免非结构化 FAILED。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED；pnpm lifecycle non-zero wrapping script exit 2；`phaseEvidence=6`、`actionEvidence=5`、`actionKeys=joinActiveOrder, activeOrderConflictRouteRejected, activeOrderCleanupDeferred, activeOrderCrossRoleReadOnly, activeOrderUnauthorizedMutationBlocked`、`activeOrderCleanupDeferred=BLOCKED/E2E_CLEANUP`、`activeOrderUnauthorizedMutationBlocked=BLOCKED/E2E_PERMISSION`、`actionObserved=3`、`surfaceObserved=38`、`uncovered=21`、`pending=62`、`blockers=64`。
- Decision: AC-M04 清理风险已变成可追溯结构化 blocker；当前 activeOrderId 仍是 PQC、放行、日结和后续 M6 验证共享夹具，不能直接删除。AC-M04 仍未 `ACCEPTED`，还缺非维护权限错误角色写入拒绝、清理窗口/可重建夹具，以及 M6 migration/performance/launch gates；本轮仍不执行 `git push`。

## M6 - AC-M04 Dedicated Unauthorized Actor PASS

- BDD: AC-M04 dedicated unauthorized actor -> Given 六个业务角色账号需要继续保留其业务入口能力 When 校验错误角色活跃订单写入隔离 Then 真实 E2E 必须使用独立 `unauthorizedActor` 登录态执行后端写入拒绝探测，不能把放行负责人账号降权来换取权限测试通过。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `RRM_UNAUTHORIZED_USERNAME`、`RRM_UNAUTHORIZED_PASSWORD` 和 `unauthorizedActor`，防止继续复用 releaseOwner 作为错误角色。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `RRM_UNAUTHORIZED_USERNAME`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增专用错误角色环境变量、配置脱敏和复用业务角色的 fail-fast；`verifyActiveOrderUnauthorizedMutationBlocked` 改为使用独立浏览器上下文登录 `unauthorizedActor` 后读取权限、验证页面不暴露维护表单并调用后端写入接口确认拒绝。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` first run with `aoteman` -> FAILED，expected reason: 在 releaseOwner 已登录页面内二次登录会被已有 token 自动跳转，脚本等待 login form 超时。
- IMPLEMENTING: 错误角色验证改为 `page.context().browser().newContext(...)` 创建独立浏览器上下文，避免同页已有登录态干扰。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` with `RRM_UNAUTHORIZED_USERNAME=aoteman` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with `RRM_UNAUTHORIZED_USERNAME=aoteman` -> STRUCTURED_BLOCKED；pnpm lifecycle non-zero wrapping script exit 2；`phaseEvidence=6`、`actionEvidence=5`、`activeOrderUnauthorizedMutationBlocked=PASS`、`activeOrderCleanupDeferred=BLOCKED/E2E_CLEANUP`、`actionObserved=4`、`surfaceObserved=37`、`uncovered=21`、`pending=62`、`blockers=63`。
- Decision: AC-M04 权限隔离动作已由专用非维护权限账号 `aoteman` 真实验证 PASS；`E2E_PERMISSION` blocker 已清除。AC-M04 仍未 `ACCEPTED`，因为 activeOrder 清理仍 deferred，且 62 AC 仍需完整真实动作、失败路径、只读核验、迁移、性能和上线门禁；本轮仍不执行 `git push`。

## M6 - Report Sync And Structural Verification

- IMPLEMENTING: 同步 `test-report.md` 与 `verification-report.md` 当前结论：`activeOrderUnauthorizedMutationBlocked=PASS`、`activeOrderCleanupDeferred=BLOCKED/E2E_CLEANUP`、`actionObserved=4`、`surfaceObserved=37`、`pending=62`、`blockers=63`。
- GREEN: `python -X utf8 -c "...json.loads(...task-state.json/result.json)..."` -> PASS，`task-state.json` 与 `result.json` 均可 UTF-8 JSON 解析。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `git diff --check -- <M6 touched files>` -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。
- BLOCKER_RECORDED: `development-plan-delivery` 脚本不适配当前实现任务目录；该目录没有 `development-plan.md/prd.md/test-plan.md`，当前任务继续以 `task.md/task-state.json/execution-log.md` 为 M0-M6 执行来源，不跳过 M6 gate。

## M6 - PQC Regulation Items Rendered Action Evidence

- BDD: PQC regulation items rendered from published QA regulation -> Given 生产组长已加入球囊扩张压力泵 V21 活跃订单且正式 QA 规程版本和 PQC 任务已发布 When PQC 检验员通过真实页面打开 PQC 填写入口 Then 页面必须按 activeOrderId、路线工序、PQC task 和发布规程版本返回检验项目、方法、标准和计划检验数量，不能使用硬编码项目、默认合格或固定 PATROL/30 成功。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyPqcRegulationItemsRendered` 和 `pqcRegulationItemsRendered`，防止只观察 PQC 页面入口而未证明规程项目动态渲染。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyPqcRegulationItemsRendered`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增 `qaRegulationVersionId` 脱敏配置、`loadPqcProcessesViaAuth(...)` 和 `verifyPqcRegulationItemsRendered(...)`；PQC 阶段现在先验证同一 activeOrderId 只读列表，再用当前 PQC 页面登录态读取 `/pqc/active-order/processes`，断言 `regulationVersionId`、`pqcTaskId`、`inspectionItems`、项目编码、名称、方法、标准、结果类型和计划检验数量。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` first PQC regulation run -> STRUCTURED_BLOCKED，expected reason: 旧 `RRM_QA_REGULATION_VERSION_ID=6` 是单版本锚点，但当前 V21 14 个路线工序实际读取发布版本 ID 16..29。
- IMPLEMENTING: 将 `RRM_QA_REGULATION_VERSION_ID` 调整为观测字段；正式验收以当前 activeOrderId 返回的 14 个工序 `regulationVersionId/pqcTaskId/inspectionItems` 为来源，不用单一旧版本号阻塞多工序规程集。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC regulation render slice -> STRUCTURED_BLOCKED；pnpm lifecycle non-zero wrapping script exit 2；`phaseEvidence=6`、`actionEvidence=6`、`pqcRegulationItemsRendered=PASS`、`activeOrderCleanupDeferred=BLOCKED/E2E_CLEANUP`、`actionObserved=7`、`surfaceObserved=34`、`uncovered=21`、`pending=62`、`blockers=63`；PQC 返回 14 个工序、32 个正式 QA 规程项目、发布版本 ID 16..29、计划巡检数量均为 15。
- Decision: PQC 动态规程项目渲染已有真实动作证据并覆盖 AC-D17/D19/D24/D31 的正向观察，但仍不是 `ACCEPTED`；仍缺失败路径、签名/逐件提交、复核、清理、并发/性能和上线门禁。本轮仍不执行 `git push`。
- GREEN: final structural check -> `task-state.json` and `result.json` parse as UTF-8 JSON; `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` PASS; `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` PASS; scoped `git diff --check` PASS。

## M6 - PQC Actual Employee Switch Gate

- BDD: PQC actual employee selection under shared login account -> Given PQC 检验员使用共享登录账号进入球囊扩张压力泵 V21 的 PQC 填写路径 When 选择实际检验员工并调用正式 `pqc/switch-employee` Then 运行态必须写入独立 `actualEmployeeId`，不得默认使用登录人，也不得在人员来源为空时返回默认成功。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyPqcActualEmployeeSwitch`、`pqcActualEmployeeSelected`、`/pqc/personnel` 和 `/pqc/switch-employee`，防止共享账号实际检验人只停留在页面入口观察。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyPqcActualEmployeeSwitch` 和 `pqcActualEmployeeSelected`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增 PQC 人员范围读取、候选人筛选、正式 switch-employee 调用和 `E2E_PQC_PERSONNEL` 结构化 blocker；若 PQC 员工/组长来源为空，脚本记录 blocker 并继续输出后续 coverage ledger，不把登录人当作实际检验员工。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after PQC actual employee switch gate -> STRUCTURED_BLOCKED；pnpm lifecycle non-zero wrapping script exit 2；`phaseEvidence=6`、`actionEvidence=7`、`pqcActualEmployeeSelected=BLOCKED/E2E_PQC_PERSONNEL`、`activeOrderCleanupDeferred=BLOCKED/E2E_CLEANUP`、`actionObserved=7`、`surfaceObserved=34`、`uncovered=21`、`pending=62`、`blockers=64`。
- BLOCKER: `pqcActualEmployeeSelected` -> PQC 人员范围返回业务码 `1040760117`，原因是“PQC 员工和 PQC 组长来源为空，无法切换填写员工”；需要在本机测试租户补齐正式 PQC 组长/员工 `EMPLOYEE` scope 后，才能证明 `actualEmployeeId` 不默认登录人。
- Decision: D25 现在具备可执行真实 E2E 门禁和结构化数据 blocker；按用户要求先记录 blocker 并继续 M6 后续切片，不执行 `git push`。

## M6 - D25 Report Sync And Structural Verification

- IMPLEMENTING: 同步 `task-state.json`、`task.md`、`test-report.md` 和 `verification-report.md` 当前 D25 结论：`pqcActualEmployeeSelected=BLOCKED/E2E_PQC_PERSONNEL`、`actionEvidence=7`、`actionObserved=7`、`surfaceObserved=34`、`pending=62`、`blockers=64`。
- GREEN: `python -X utf8 -c "...json.loads(...task-state.json/result.json)..."` -> PASS，`task-state.json` 与 `result.json` 均可 UTF-8 JSON 解析；`result.json` 当前为 `BLOCKED` 且 blocker 数量为 64。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: scoped `git diff --check` on M6 touched files -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。

## M6 - Migration Preflight Static Gate

- BDD: M6 migration preflight before full acceptance -> Given M1-M5 已引入 activeOrder、生产系数快照、QA/PQC 规程、调拨追溯和范围迁移 When 进入 M6 全量真实验收前 Then 必须有可重复执行的迁移预检，明确阻塞双活跃来源冲突、开放订单缺路线版本或系数、开放 PQC 缺任务身份或规程版本、正式批记录绑定缺失或冲突，且不能通过默认成功或人工 checklist 替代。
- TEST_ADDED: `role-matrix-migration-preflight-static.spec.cjs` -> 新增 M6 迁移预检静态合同，要求 `20260802_role_requirement_matrix_m6_migration_preflight.sql` 存在、包含 release-migration 元数据、四个 fail-fast procedure、`SIGNAL SQLSTATE` 和正式来源缺口中文错误。
- RED: `node IntRuoyiFronted\tests\e2e\role-matrix-migration-preflight-static.spec.cjs` -> FAIL，expected reason: 缺少 `20260802_role_requirement_matrix_m6_migration_preflight.sql`。
- IMPLEMENTING: 新增只读迁移预检 SQL `IntRuoyiBackend\sql\mysql\20260802_role_requirement_matrix_m6_migration_preflight.sql`；通过临时 stored procedure 检查双活跃来源冲突、开放 activeOrder 权威字段、开放 PQC task 权威字段和正式批记录绑定冲突，失败时 `SIGNAL SQLSTATE '45000'`；不执行业务数据 UPDATE/DELETE/INSERT。
- IMPLEMENTING: `package.json` 新增 `e2e:role-matrix-migration-preflight:static`；总 preflight 静态合同将该脚本纳入 planned static scripts。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-matrix-migration-preflight-static.spec.cjs` -> PASS。
- GREEN: `node -e "JSON.parse(...package.json...)"` -> PASS。
- RED_RETRY: `run-release-migration-policy-gate.py` first targeted run -> FAIL，expected reason: migration metadata used unsupported `type=preflight`。
- IMPLEMENTING: release-migration metadata 改为允许的 `type=config`，并显式依赖 `20260802_mes_pqc_inspection_task`、`20260802_mes_process_pool_active_order_transfer_trace`、`20260802_mes_process_pool_team_leader_scope_extended`，覆盖 M3/M4/M5 后置边界。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file <14-file M6 chain> --output doc\tasks\20260801-role-requirement-matrix-implementation\m6-migration-policy-gate.json` -> PASS，`migrationCount=14`，最后迁移为 `20260802_role_requirement_matrix_m6_migration_preflight`。
- Decision: M6 迁移预检静态和 release policy gate 已 GREEN；真实数据库执行该预检仍属于 M6 后续验收，不在本切片标记为完成。
