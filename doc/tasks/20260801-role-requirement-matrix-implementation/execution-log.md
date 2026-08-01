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
