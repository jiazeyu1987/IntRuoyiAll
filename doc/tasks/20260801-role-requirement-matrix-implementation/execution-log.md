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

## M6 - Migration Gate Report Sync Verification

- IMPLEMENTING: 同步 `task-state.json`、`task.md`、`test-report.md` 和 `verification-report.md` 当前迁移预检结论：M6 migration static/policy gate 为 PARTIAL_GREEN，真实运行库预检执行仍待后续 M6 验收。
- GREEN: JSON parse check -> PASS，`task-state.json`、`package.json`、`m6-migration-policy-gate.json` 和 `result.json` 均可 UTF-8 JSON 解析；policy gate 输出 `status=passed`，真实 E2E 结果仍为结构化 `BLOCKED`。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: scoped `git diff --check` on M6 touched files -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。
- Decision: 本切片不执行 `git push`，M6 继续保持 `in_progress`。

## M6 - Migration Runtime Preflight Formal Batch-record Scope Fix

- BDD: M6 migration preflight must separate formal batch records from form slots -> Given 路线工序同时存在正式批记录 MAIN/BATCH_RECORD 和 INTERNAL_RECORD 表单槽位 When M6 迁移预检检查批记录绑定 Then 只允许把 MAIN/BATCH_RECORD 的空 `batch_record_report_id` 作为正式批记录缺口，不能把 LOSS_REPORT / PROCESS_INSPECTION 内部记录槽位当成正式批记录缺失。
- TEST_ADDED: `IntRuoyiFronted/tests/e2e/role-matrix-migration-preflight-static.spec.cjs` -> 增加 MAIN/BATCH_RECORD-only 正向断言和禁止全表空 `batch_record_report_id` 口径的负向断言。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` -> FAIL，expected reason: 临时撤掉 SQL 的 `form_slot_type='MAIN'` / `record_category='BATCH_RECORD'` 过滤后，静态合同拒绝过宽批记录绑定预检口径。
- RUNTIME_RED: authorized local DB execution of `20260802_role_requirement_matrix_m6_migration_preflight.sql` -> FAIL，expected reason: 真实运行库有 528 行空 `batch_record_report_id`，但只读诊断证明它们全部是 `LOSS_REPORT` / `PROCESS_INSPECTION` + `INTERNAL_RECORD` 表单槽位，不是正式 MAIN/BATCH_RECORD 批记录绑定缺失。
- IMPLEMENTING: `20260802_role_requirement_matrix_m6_migration_preflight.sql` 将空 `batch_record_report_id` 检查收窄到 `form_slot_type='MAIN'` 且 `record_category='BATCH_RECORD'`；重复绑定检查原本已是正式口径，保持不变。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-migration-preflight:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: authorized local DB SQL preflight via `int-ruoyi-mysql` -> PASS，`leftover_procedure_count=0`，临时 `/tmp/rrm_m6_preflight.sql` 已清理。
- GREEN: `run-release-migration-policy-gate.py` on the 14-file M6 migration chain -> PASS，`migrationCount=14`，`20260802_role_requirement_matrix_m6_migration_preflight` sha256 refreshed to `a4b225a7ef96e4281c63b90d344cb0ea1989ce6c9112a1f591a4d453d48f65bc`。
- Decision: M6 迁移预检从 PARTIAL_GREEN 升级为 runtime GREEN；M6 仍保持 `in_progress`，因为 PQC personnel、activeOrder cleanup、并发、性能、全量真实 E2E 和 launch gate 尚未完成；本轮仍不执行 `git push`。

## M6 - PQC Employee Scope Fixture And Actual Employee PASS

- BDD: PQC actual employee source uses formal EMPLOYEE scope -> Given PQC 检验员使用共享账号 `shangmengying` 登录芋道源码 tenant 1 When `/pqc/personnel` 读取 PQC 员工/组长来源并在页面切换实际检验人 Then 候选必须来自正式 `mes_pro_process_pool_team_leader_scope`，`actualEmployeeId` 必须可切换到不同于登录人的正式候选，不能默认使用登录用户。
- RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` before fixture -> STRUCTURED_BLOCKED，expected reason: `pqcActualEmployeeSelected=BLOCKED/E2E_PQC_PERSONNEL`，`/pqc/personnel` 返回业务码 `1040760117`，PQC 员工和 PQC 组长来源为空。
- SCHEMA_EVIDENCE: `mes_pro_process_pool_team_leader_scope` confirmed fields `leader_user_id / leader_type / scope_type / employee_user_id / enabled / tenant_id / deleted`; `MesFrontlinePqcContextServiceImpl#listPqcEmployeeCandidates` reads `leader_type=PQC` scopes and includes both leader user ids and `EMPLOYEE` employee user ids.
- IMPLEMENTING: 新增并执行 `m6-pqc-employee-scope-local-seed.sql`，只在授权本地 tenant 1 写入 task-owned scope：`leaderUserId=512 (huzonggang)`、`scopeType=EMPLOYEE`、`employeeUserId=659 (shangmengying)`，remark 标记 `RRM M6 local E2E fixture`。
- GREEN: local DB seed execution -> PASS，最终行 `id=980013 / tenant_id=1 / leader_type=PQC / scope_type=EMPLOYEE / employee_user_id=659 / enabled=1`，临时容器 SQL 文件已清理，`leftover_seed_procedure_count=0`。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME blocker after setting required QA regulation version env and restoring 48081 health.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`pqcActualEmployeeSelected=PASS`，`loginUserId=659`、`actualEmployeeId=512`、`candidateCount=2`、`employeeLabel=胡宗港`，overall blockers reduced from 64 to 63 and only cleanup/coverage blockers remain.
- RUNTIME_NOTE: latest `backend-runtime-control-20260802-205036.jar` was not used because startup failed on unrelated corrupted `MesProProcessPoolTimelineReadMapper.xml` package resource (`前言中不允许有内容`); 48081 verification used previously M6-verified `backend-runtime-control-20260802-170535.jar`, and no backend code changed in this slice.
- Decision: D25 `E2E_PQC_PERSONNEL` blocker is resolved; M6 remains `in_progress` because `activeOrderCleanupDeferred` plus full AC coverage, concurrency, performance and launch gates remain open;本轮仍不执行 `git push`。

## M6 - Concurrency And Performance Gate Ledger

- BDD: M6 concurrency and performance gates must be explicit -> Given M6 still has CONC/PERF acceptance rows after AC-M04 partial proof When full real E2E cannot yet complete every concurrent terminal state or pagination/index proof Then the script must derive CONC/PERF rows from the 62 AC matrix and emit explicit gate blockers, not hide them inside generic coverage blockers or mark them accepted.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> requires `buildM6ConcurrencyPerformanceGateEvidence`, `buildGateBlockers`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, `E2E_CONCURRENCY`, `E2E_PERFORMANCE`, and `gateEvidence`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` lacked `buildM6ConcurrencyPerformanceGateEvidence`.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now derives CONC/PERF acceptance IDs from `test-plan.md`, writes `gateEvidence`, maps gate blockers separately from action blockers, and records `m6ConcurrencyGateDeferred` / `m6PerformanceGateDeferred`; PQC cross-role read-only verification was stabilized to use the real logged-in page context plus formal read-only endpoints after two transient network wait REDs.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- RED_RUNTIME: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` first retry -> STRUCTURED_BLOCKED，expected reason: current shell initially lacked `RRM_*` env and then full run hit transient login/list wait timeouts; direct backend health, frontend login page, and releaseOwner API login diagnostics passed, so this was not recorded as production-code completion evidence.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` with task-owned env -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after concurrency/performance gate ledger -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=7`、`gateEvidence=2`、`actionObserved=8`、`surfaceObserved=33`、`uncovered=21`、`pending=62`、`blockers=65`；`m6ConcurrencyGateDeferred` covers 12 CONC AC and `m6PerformanceGateDeferred` covers 4 PERF AC.
- Decision: M6 gate blockers are now explicit and auditable; M6 remains `in_progress` because cleanup, all 62 AC full real actions/failure paths, the 12 CONC AC and 4 PERF AC still require formal proof;本轮仍不执行 `git push`。

## M6 - AC-D27 PQC Piece Detail Quantity Evidence

- BDD: AC-D27 PQC piece detail quantity from planned task -> Given PQC 页面已按同一 activeOrderId 读取正式 QA 规程和 PQC task plannedInspectionQuantity When 检验员打开逐件检验弹窗 Then 页面必须准备与正式计划检验数量一致的逐件行，不能使用默认 30、空行或整批结果替代逐件明细。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyPqcPieceDetailQuantityPrepared` 和 `pqcPieceDetailQuantityPrepared`，防止 AC-D27 只停留在 PQC 页面入口观察。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyPqcPieceDetailQuantityPrepared`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 新增只读页面动作 `verifyPqcPieceDetailQuantityPrepared`；该动作复用 `pqcRegulationItemsRendered` 的计划数量证据，打开真实 PQC 逐件弹窗，断言检验数量输入值来自 plannedInspectionQuantity，弹窗标题和 `.frontline-pqc-piece-row` 行数均等于该数量，不提交 PQC 数据。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after AC-D27 piece detail action -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=8`、`gateEvidence=2`、`actionObserved=9`、`surfaceObserved=32`、`uncovered=21`、`pending=62`、`blockers=65`；`pqcPieceDetailQuantityPrepared=PASS`，`plannedQuantities=[15]`，`uiQuantity=15`，`pieceRowCount=15`，`m6PerformanceGateDeferred` 已记录 observed PERF AC=`AC-D27`。
- Decision: AC-D27 已新增真实页面逐件数量动作证据，但仍未 `ACCEPTED`；还缺逐件提交后的只读明细还原、失败路径、签名/复核、完整 N+1/查询计数证明和 M6 清理/上线门禁。本轮仍不执行 `git push`。

## M6 - AC-D32 PQC Leader Submission Filter And Pagination

- BDD: AC-D32 PQC leader submission filtering and pagination consistency -> Given PQC 组长在芋道源码 tenant 1 打开球囊扩张压力泵 V21 提交看板 When 按订单、产品、工序、检验类型、轮次、人员、日期和复核状态筛选并翻页 Then 列表只返回匹配行，分页 total 与 page 明细使用同一主提交事件口径，不能因一对多 PQC task/review JOIN 出现重复行、总数漂移或越权数据。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyPqcLeaderSubmissionFilterPaginationConsistency` 和 `pqcLeaderSubmissionFilterPaginationConsistent`，防止 AC-D32 只停留在看板入口观察。
- TEST_ADDED: `process-pool-timeline-mapper-static.spec.cjs` -> 要求提交看板 mapper 读取产品和 PQC task 正式字段，且使用 `pqcTaskId` 精确关联，避免粗粒度一对多 JOIN 放大分页。
- TEST_ADDED: `ProcessPoolTimelineFilterTest` -> 新增 AC-D32 过滤分页单测，使用 pageSize=1 验证 total 与第 1/2 页明细一致。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason: `role-requirement-matrix-real-flow.e2e.js` 缺少 `verifyPqcLeaderSubmissionFilterPaginationConsistency`。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL，expected reason: `MesProProcessPoolTimelineReadMapper.xml` 缺少 `work_order.product_id AS productId`、PQC task 精确关联和 AC-D32 筛选字段。
- RED_PREREQ_BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before target test execution；`MesWmBatchServiceImplTest` testCompile 读取现有 `target\classes\...\MesWmItemConsumeDetailDO$MesWmItemConsumeDetailDOBuilder.class` 时发生 `NoSuchFileException`。该结果只记录为 Maven 增量输出前置 blocker，不作为 AC-D32 业务 RED；目标 JUnit 必须在正式实现后通过隔离/恢复的 Maven 输出重新执行。
- IMPLEMENTING: `ProcessPoolTimelinePageReqVO`、`ProcessPoolTimelineEventReadDO`、`ProcessPoolTimelineEventRespVO`、`ProcessPoolTimelineServiceImpl`、`MesProProcessPoolTimelineReadMapper.xml` 和 `ProcessPoolTimelineTestSupport` 增加产品、检验类型、轮次、复核状态和 `pqcTaskId` 精确关联筛选；count/page 共用同一正式提交事件口径，避免按工单/工序粗粒度 JOIN 放大。
- IMPLEMENTING: `TeamLeaderWorkbenchPage.vue` 与 `src/api/mes/pro/processpool/index.ts` 增加 PQC 组长提交看板筛选项和结果列；真实 E2E 增加 `verifyPqcLeaderSubmissionFilterPaginationConsistency`，先操作 PQC 组长真实页面，再用登录态只读 API 校验 page/count 一致性。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> current-task Maven process hung in `WinNTFileSystem.delete0` / `IncrementalBuildHelper.beforeRebuildExecution`; only task-owned Maven chain PIDs were stopped. No AC-D32 target JUnit GREEN is claimed until Maven output prerequisites are restored.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=9`、`gateEvidence=2`、`blockers=66`；`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA`，原因是当前本机租户在 `submitDate=2026-08-03`、`workOrderId=980008` 下没有至少两笔正式 PQC submitted 事件，无法证明 PQC 组长筛选分页 total 稳定。
- Decision: AC-D32 代码、静态合同、类型检查和真实前置已推进到可执行门禁；真实 E2E 因正式 PQC 提交样本缺失保持结构化 blocker，AC-D32 不标记为 `ACCEPTED`。M6 继续保持 `in_progress`，本轮仍不执行 `git push`。

## M6 - AC-D32 Report Sync And Structural Verification

- IMPLEMENTING: 同步 `task-state.json`、`task.md`、`test-report.md` 和 `verification-report.md` 当前 D32 结论：代码/静态/类型/read-model gate 为 GREEN，target Maven JUnit 为前置 blocker，真实 E2E 为 `pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA`，M6 仍为 `in_progress`。
- GREEN: JSON parse check -> PASS，`task-state.json` 为 `in_progress` 且 7 个 milestone 可解析，`result.json` 为 `BLOCKED` 且 actionEvidence=9。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- GREEN: scoped `git diff --check` on D32 docs and touched frontend/backend contract files -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。

## M6 - AC-D17 PQC Visible Method Metadata

- BDD: AC-D17 PQC visible QA method metadata -> Given PQC 检验员打开球囊扩张压力泵 V21 的正式 PQC 填写页面 When 页面按已发布 QA 规程渲染检验项目 Then 每个项目必须可见展示检验方法、标准和判定类型，不能只显示项目名称或只在 API 证据里存在。
- TEST_ADDED: `role-matrix-qa-regulation-static.spec.cjs` -> 要求 `FrontlineFixedTemplatePanel.vue` 存在 `data-pqc-inspection-meta`、`formatPqcInspectionMeta`，且 `PqcInspectionItem` 保留 `inspectionMethod`、`standardText`、`resultType`。
- RED: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` -> FAIL，expected reason: PQC 页面未可见渲染 method/standard/result metadata。
- IMPLEMENTING: `FrontlineFixedTemplatePanel.vue` 扩展 `PqcInspectionItem` 正式规程字段，在数值项和选择项列表中渲染 `方法 / 标准 / 判定` 元信息；不改变提交 payload、任务来源或默认数量逻辑。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` 增强 `pqcRegulationItemsRendered`，从页面 `data-pqc-inspection-meta` 读取可见文本，并与正式 QA 规程快照中的 `inspectionMethod`、`standardText`、`resultType` 对齐。
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS。
- RUNTIME: int_main backend 48081 was not listening; after reading local runtime/worktree rules, restarted the previously M6-verified isolated jar `backend-runtime-control-20260802-170535.jar` on 48081 without rebuilding from the dirty workspace. Health returned `UP`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=9`、`gateEvidence=2`、`blockers=66`；`pqcRegulationItemsRendered=PASS` now records `visibleMetadataCount=1` plus sample method/standard/result from the formal QA regulation snapshot, while D32 submitted-data, cleanup, coverage, concurrency, and performance blockers remain.
- Decision: AC-D17 now has visible page metadata proof for method/standard/result on top of the prior formal API snapshot proof, but AC-D17 is still not `ACCEPTED` because failure paths, submission/signature/review, cleanup, and full M6 gates remain open. 本轮仍不执行 `git push`。

## M6 - AC-D17 Report Sync Verification

- IMPLEMENTING: 同步 `test-report.md` 和 `verification-report.md`，补充 AC-D17 页面可见 `方法 / 标准 / 判定` 元信息、`visibleMetadataCount=1`、正式 QA 规程样例，以及 D17 仍不 `ACCEPTED` 的 M6 后续门禁口径。
- GREEN: JSON parse check -> PASS，`task-state.json` 与 `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` 均可 UTF-8 JSON 解析。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-static.spec.cjs` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: scoped `git diff --check` on D17 touched files and task reports -> PASS，仅输出 LF/CRLF 工作区提示，无 whitespace error。
- BLOCKED_UNRELATED: `pnpm --dir IntRuoyiFronted ts:check` -> FAIL，current workspace has unrelated DCC controlled-print type errors in `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` (`controlledPrintAllowed`、`controlledPrintPermissionHintVisible`、`controlledPrintResultDialog` 等未定义)。这些文件属于其它 DCC 任务脏改动，不属于本轮 PQC/D17 变更；本轮不回滚、不修复无关 DCC 改动，也不把当前全量 `ts:check` 记录为 D17 GREEN。

## M6 - AC-D29 PQC Formal Submission Event Runtime Reload Gate

- BDD: AC-D29 PQC formal submission creates process-pool event -> Given PQC 检验员已按球囊扩张压力泵 V21 的发布 QA 规程完成逐件明细和实际检验人选择 When 通过真实 PQC 页面执行正式提交 Then 后端必须创建可追溯的工序池 PQC inspection event，并让 PQC 组长提交看板按同一 `pqcTaskId` 可见；不能只把 PQC task 改成 `SUBMITTED` 而不生成待办事件。
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` -> 要求真实 E2E 脚本包含 `verifyPqcFormalSubmissionCreatesEvent`、`pqcFormalSubmissionCreated` 和 `/mes/pro/feedback/frontline/device-account/pqc/submit`，防止 AC-D29 只停留在后端任务状态更新。
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource` -> 断言 `submitPqcInspection(...)` 读取同一 active order/source event/source pool identity，保存逐件明细后调用 `createPqcInspectionEvent(...)`，并把 `activeOrderId`、`pqcTaskId`、`regulationVersionId`、实际人员、签名和 raw payload 写入事件 DTO。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason：真实 E2E 脚本缺少 `verifyPqcFormalSubmissionCreatesEvent` / `pqcFormalSubmissionCreated` 正式提交事件门禁。
- IMPLEMENTING: `MesFrontlinePqcContextServiceImpl#submitPqcInspection` 在校验命令、任务、员工、签名和逐件明细后，读取 active pool 的最新 source event、校验 source identity，更新 PQC task 为 `SUBMITTED`，插入逐件明细，并调用 `MesProcessPoolEventService#createPqcInspectionEvent(...)` 生成工序池 PQC inspection event；真实 E2E 新增正式提交动作、PQC 检验员签名 ID 读取、提交接口等待、PQC 组长新上下文看板核验和 `E2E_PQC_SUBMISSION_DATA` blocker。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests，0 failures/errors，BUILD SUCCESS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` from `IntRuoyiBackend` -> PASS，reactor BUILD SUCCESS，`yudao-server-exec.jar` SHA256 `200527D05A5C9CC2F1E12A303EF9835BDB90E9775BC75B1D3EC94863693D6D25`。
- RUNTIME_BLOCKED: 新构建 M6 runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` 两次启动都未能成为 48081 listener；48081 被无关 DCC patched runtime 自动占用（观测 PID `47592` / `58452`，Jar `backend-runtime-control-20260803-dcc-print-ux-patched.jar`），M6 Jar 日志均显示 `Web server failed to start. Port 48081 was already in use.`。
- CLEANUP: 仅停止本轮任务自有的失败启动残留 PID `5980` / `33980`；未停止无关 DCC runtime，也未改端口或换 profile。
- Decision: AC-D29 后端生成 PQC event 的代码、单测、静态门禁和可运行 Jar 构建已 GREEN，但新 Jar 未加载到 48081，因此未运行 `real:check` / full real E2E，不标记 AC-D29、AC-D32 或任一 AC 为 `ACCEPTED`。M6 继续保持 `in_progress`，新增 runtime blocker 等待 48081 回到本任务 M6 Jar 或用户明确授权处理 DCC runtime 冲突；本轮仍不执行 `git push`。

## M6 - AC-D32 Target JUnit Restored

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests，0 failures/errors，BUILD SUCCESS。
- Decision: 关闭此前 `ProcessPoolTimelineFilterTest` 被 Windows Maven 增量删除 hang 阻塞的前置 blocker；AC-D32 现在具备 mapper static、preflight static、frontend syntax/type/read-model target JUnit GREEN。AC-D32 仍不 `ACCEPTED`，因为真实 E2E 仍缺至少两笔正式 PQC submitted 事件，且新 M6 Jar 尚未加载到 48081。

## M6 - AC-D29 Authorized Runtime E2E Rerun

- AUTHORIZATION: 用户明确授权处理 48081 runtime 冲突后，已停止无关 DCC patched runtime PID `58452`，并让本任务 M6 runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar` 成为 48081 listener。
- GREEN: runtime probe -> PASS，48081 listener PID `28744`，命令行为 `backend-runtime-control-20260803-023450-rrm-m6-pqc-submit.jar`，`http://127.0.0.1:48081/actuator/health` 返回 `UP`，`http://127.0.0.1:8081/` 返回 HTTP `200`。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on the M6 runtime jar -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=10`、`gateEvidence=2`、`actionObserved=9`、`surfaceObserved=32`、`uncovered=21`、`pending=62`、`blockers=67`。
- BLOCKED: `pqcFormalSubmissionCreated=BLOCKED/E2E_PQC_SUBMISSION_DATA`，原因是固定签名 ID `25` 已被工序池事件占用；`pqcLeaderSubmissionFilterPaginationConsistent=BLOCKED/E2E_PQC_SUBMISSION_DATA` 仍因缺少至少两笔正式 PQC submitted 事件无法证明筛选分页 total 一致性。
- Decision: AC-D29 runtime reload blocker 曾关闭，但正式提交仍因签名池数据保持 blocker；本轮后续已新增签名池选择逻辑并保持后端唯一性不变。AC-D29、AC-D32 和 62 个 AC 均不得标记为 `ACCEPTED`。M6 继续保持 `in_progress`，本轮仍不执行 `git push`。

## M6 - AC-D29 Signature Pool And Runtime Ownership Gate

- BDD: AC-D29 must not reuse consumed process-pool signatures -> Given `mes_pro_process_pool_event.signature_id` has a tenant-level unique key and one PQC submission already consumed signature ID `25` When the real E2E prepares another PQC formal submission Then it must choose an unused task-provided formal signature ID, and if all configured IDs are consumed it must emit `E2E_PQC_SIGNATURE_POOL` instead of relaxing backend uniqueness.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `resolveUnusedPqcSignatureId`, `collectConfiguredSignatureIds`, and `E2E_PQC_SIGNATURE_POOL`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，expected reason：真实 E2E 脚本缺少 `resolveUnusedPqcSignatureId`。
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now builds a configured signature ID pool from `RRM_SIGNATURE_IDS_JSON`, logs into a PQC leader read-only context, reads submitted-event signature IDs from the formal submission page, and fills the first unused configured signature ID before clicking PQC submit. It does not change backend `validateUniqueSignature` or create arbitrary fallback IDs.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- RUNTIME_BLOCKED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` could not prove the signature-pool GREEN path because current 48081 listener PID `5852` is unrelated `backend-runtime-control-20260803-121411-dcc-product-onboarding.jar`; the run failed before D29 at PQC process list with `1040506107 当前工序缺少待执行 PQC 检验任务`, consistent with the wrong runtime not loading the later `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar` behavior.
- CLEANUP/OWNERSHIP: Did not stop PID `5852`, did not switch ports, and did not claim full E2E PASS because the listener belongs to an unrelated DCC runtime on the shared int_main port.
- Decision: AC-D29 signature reuse blocker is addressed at the E2E script gate, but full real verification is currently blocked by shared runtime ownership. Need explicit authorization to stop or replace the DCC product-onboarding runtime with the M6 RRM jar before rerunning full real E2E. M6 remains `in_progress`; no `git push` per user instruction.

## M6 - AC-D29 And AC-D32 Real-page PASS Evidence Sync

- BDD: AC-D29/D32 submitted PQC evidence must be real-page sourced -> Given PQC 检验员通过球囊扩张压力泵 V21 正式页面提交过程检验 When PQC 组长使用同一订单、产品、工序、人员、检验类型、轮次、提交日期和复核状态筛选 Then 应能看到同筛选条件下稳定的提交事件分页 total，并且正式提交必须使用未占用签名生成过程池事件。
- TEST_ADDED: `role-matrix-pqc-d32-fixture-static.spec.cjs` -> 锁定 D32 local fixture 只能准备 formal PENDING PQC task，禁止插入 `mes_pro_process_pool_event` 或直接标记 submitted，防止 API/SQL 伪造真实页面提交。
- RED: first runtime execution of `m6-pqc-d32-same-filter-local-seed.sql` -> FAIL，expected reason：MySQL `ERROR 1267 Illegal mix of collations`，临时字符串比较未显式匹配目标列排序规则。
- GREEN: rerun fixed `m6-pqc-d32-same-filter-local-seed.sql` -> PASS，explicit `utf8mb4_unicode_ci` collation applied；local fixture inserted/reused formal PQC task `31` for routeProcessId `928609` / processId `922985` without inserting process-pool event or marking submitted.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-d32-fixture:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: runtime probe -> PASS，48081 listener PID `43876` is `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`; backend health `UP`; frontend 8081 HTTP `200`。
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME。
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED，exit 2；`phaseEvidence=6`、`actionEvidence=10`、`gateEvidence=2`、`blockers=65`。
- GREEN: `pqcFormalSubmissionCreated` action evidence -> PASS，`submittedTaskId=31`、`eventId=26`、`signatureId=23`、`candidateSignatureIds=[25,22,23,24,26,27]`、`usedSignatureIds=[25,22]`。
- GREEN: `pqcLeaderSubmissionFilterPaginationConsistent` action evidence -> PASS，same-filter `total=2`，filters are `submitDate=2026-08-03`、`workOrderCode=RRM-20260801-PP-MO-001`、`employeeUserId=512`、`processId=922985`、`productKeyword=AW.107.02.01.2010`、`inspectionType=PATROL`、`roundNo=1`、`submissionReviewStatus=PENDING`；page 1 event `24` and page 2 event `26`。
- TASK_DOCS: `task-state.json`、`task.md`、`test-report.md`、`verification-report.md` -> UPDATED，关闭旧 runtime ownership、AC-D29 signature/data blocker 和 AC-D32 submitted-data blocker 叙述；M6 仍 `in_progress`，剩余 blocker 为 `activeOrderCleanupDeferred`、`m6ConcurrencyGateDeferred`、`m6PerformanceGateDeferred` 和 62 个 `E2E_COVERAGE`。
- Decision: AC-D29 与 AC-D32 已达到真实动作 PASS，但均不得标记为 `ACCEPTED`；仍缺失败路径、权限/只读核验、并发/性能、清理和全量 M6 coverage gate。本轮仍不执行 `git push`。

## M6 - D29/D32 Evidence Sync Structural Verification

- NOTE: first parallel verification attempt triggered a PowerShell `System.OutOfMemoryException`; it did not write files. The same checks were rerun serially and are recorded below.
- GREEN: `python -X utf8 -c "import json, pathlib; json.loads(...task-state.json...)"` -> PASS，`task-state.json` 可解析。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-matrix-pqc-d32-fixture:static` -> PASS。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- GREEN: scoped `git diff --check` on D29/D32 docs, D32 fixture SQL, package script and static contracts -> PASS。

## M6 - AC-D29 PQC Duplicate Submit Status Guard

- BDD: AC-D29 duplicate PQC task submit must fail fast -> Given a PQC inspection task is already `SUBMITTED` and has generated or may have generated a process-pool PQC event When the frontline PQC submit endpoint is called again with the same `pqcTaskId` Then the service must reject before updating the task, inserting piece details, or creating another process-pool event; only `PENDING` tasks may enter formal submission.
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldRejectAlreadySubmittedPqcInspectionTask` -> discovered in source and asserts `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID`, no `pqcTaskMapper.updateById`, no `pqcPieceDetailMapper.insertBatch`, and no `processPoolEventService.createPqcInspectionEvent`.
- RED_BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED, expected business RED could not be reached because the current-task Maven chain stalled before target compile while unrelated DCC/MES Maven processes were also active; current-task PIDs `66460/44116/39272` were stopped only after stack inspection showed no target result.
- RED_BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BLOCKED, expected business RED could not be reached because Maven stalled in `IncrementalBuildHelper.beforeRebuildExecution` / `WinNTFileSystem.delete0`; current-task PIDs `5980/32400/41968` were stopped.
- RED_BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> BLOCKED, expected business RED could not be reached because non-incremental MES compile stalled in javac file-attribute scanning under concurrent Maven load; current-task PIDs `50844/51936/34020` were stopped.
- IMPLEMENTING: Added `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID` error code, introduced `PQC_TASK_STATUS_PENDING`, required `PENDING` in `requirePqcTaskIdentity(...)`, and replaced the submit write literal with `PQC_TASK_STATUS_SUBMITTED`.
- GREEN_BLOCKED: Standard `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` stalled again in compiler stale-source scanning; current-task PIDs `19476/32184/50924` were stopped after stack inspection.
- GREEN_BLOCKED: Non-incremental `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL before target tests, expected reason not reached: shared `target\classes` is missing `MesProScheduleCalendarDayDetailRespVO$LineDetailItem$LineDetailItemBuilder.class` although source `LineDetailItem` is annotated with `@Builder`; another non-current MES Maven PID `57820` is still active in the same backend root, so this task did not clean or rewrite shared `target`.
- AUTHORIZED_CLEANUP: After user authorization, stopped the blocking same-root MES Maven chain `57820/7728/20224`. A Maven module clean `mvn -pl yudao-module-mes clean` started deleting `yudao-module-mes\target` but stalled in `maven-clean-plugin Cleaner.delete` / `WinNTFileSystem.delete0`; current-task clean PIDs `28552/42884/65452` were stopped, and `target` still had 1081 entries.
- AUTHORIZED_RETRY: Retried non-incremental target test after partial clean. Additional same-root MES Maven chains auto-appeared and were stopped under the same authorization: `59876/36624/61668`, `57028/29908/51964`, and `31416/31660/21148`. The current target test `47312/66196/41880` then remained in javac `ClassWriter.writeClass` / file close for more than 20 minutes without reaching current surefire reports, so it was stopped to avoid further `target` contention.
- TARGET_ISOLATION: Moved the remaining partial output directory from `IntRuoyiBackend\yudao-module-mes\target` to `IntRuoyiBackend\yudao-module-mes\target\rrm_m6_blocked_20260803_151631`, allowing Maven to regenerate a clean `target` without deleting the locked residual files.
- TEST_FIX: Fixed `MesFrontlinePqcContextServiceTest#shouldRejectAlreadySubmittedPqcInspectionTask` Mockito overload ambiguity by changing `updateById(any())` to `updateById(any(MesPqcInspectionTaskDO.class))`; this keeps the same no-write assertion and only makes the test compile.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 10 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Decision: AC-D29 duplicate-submit status guard has targeted backend GREEN evidence, but AC-D29 and M6 are not accepted because full M6 failure paths, permissions/read-only evidence, concurrency/performance gates, cleanup, and 62 AC coverage remain open. 本轮仍不执行 `git push`。

## M6 - AC-D29 PQC Concurrent Duplicate Submit Guard In Independent Worktree

- WORKTREE: Created and registered `D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803` on branch `codex/rrm-m0-m6-verification-20260803`; fast-forwarded it to current `int_main` commit `c52f5ddba`; slot `17`, frontend `8098`, backend `48098`; branch runtime port guard passed during fast-forward merge.
- BDD: AC-D29 concurrent PQC task submit must fail fast -> Given two submit requests can read the same PQC task while it is still `PENDING` When one request consumes the task before the second request writes Then the second request must fail before inserting piece details or creating another process-pool PQC event; the state transition must be atomic `PENDING -> SUBMITTED`.
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldRejectPqcInspectionWhenPendingTaskWasConsumedConcurrently` -> discovered in the independent worktree baseline after fast-forwarding `int_main`; the test simulates a stale `PENDING` read followed by a zero-row status update and asserts no piece-detail or event writes.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `shouldRejectPqcInspectionWhenPendingTaskWasConsumedConcurrently` did not throw `ServiceException`; tests run `11`, failures `1`, errors `0`.
- IMPLEMENTING: Added `MesPqcInspectionTaskMapper#updateSubmittedIfPending(...)` using `WHERE id = ? AND task_status = 'PENDING'`; `submitPqcInspection(...)` now requires that conditional update to affect exactly one row before inserting piece details or creating the process-pool PQC event.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `11`, failures `0`, errors `0`, BUILD SUCCESS.
- Decision: AC-D29 concurrent duplicate-submit backend race is now target-JUnit GREEN in the independent worktree, but AC-D29 and M6 are still not `ACCEPTED` because real failure paths, permissions/read-only evidence, cleanup, broader concurrency/performance gates, and 62 AC coverage remain open; no `git push` performed.

## M6 - AC-D29 Concurrent Guard Report Sync

- TASK_DOCS: `task.md`, `test-report.md`, and `verification-report.md` -> UPDATED to include independent-worktree AC-D29 concurrent duplicate-submit RED/GREEN evidence and keep M6 `in_progress`.
- GREEN: UTF-8 / JSON / Markdown structural read check -> PASS, `task-state.json`, `task.md`, `execution-log.md`, `test-report.md`, and `verification-report.md` are readable.
- GREEN: scoped `git diff --check` on AC-D29 Java/test files and task reports -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- Decision: Report sync is complete for the AC-D29 concurrent backend guard; no AC is marked `ACCEPTED`, and no `git push` performed per user instruction.

## M6 - AC-D34 PQC Duplicate Terminal Review Guard

- BDD: AC-D34 PQC review terminal state must be single-writer -> Given a PQC submission event already has an `APPROVED` or `REJECTED` review record When another leader request attempts to confirm or reject the same event again Then the service must fail fast before inserting another terminal review; concurrent requests must serialize on the same process-pool event.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldRejectDuplicateTerminalReviewForSameSubmission` -> asserts an existing terminal review blocks a second review and no new review row is inserted.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `shouldRejectDuplicateTerminalReviewForSameSubmission` did not throw `ServiceException`; tests run `3`, failures `1`, errors `0`.
- IMPLEMENTING: `reviewSubmission(...)` now runs in a transaction, locks the source process-pool event with `selectByIdForUpdate(...)`, reads the latest existing review with `LIMIT 1 FOR UPDATE`, and throws `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS` before inserting a duplicate terminal record.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `3`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `14`, failures `0`, errors `0`, BUILD SUCCESS.
- Decision: AC-D34 duplicate terminal review backend guard is now target-JUnit GREEN in the independent worktree, but AC-D34 and M6 are still not `ACCEPTED` because real page review actions, process-inspection aggregation, cleanup, broader concurrency/performance gates, and 62 AC coverage remain open; no `git push` performed.

## M6 - AC-D35 PQC Self-review Isolation Guard

- BDD: AC-D35 PQC self-review must fail fast on the backend -> Given a PQC submission event records `actualEmployeeId` from the real inspector When a team leader review request uses the same user as `leaderUserId` Then the service must reject before inserting a review record, even if the same account has leader scope.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldRejectSelfReviewWhenLeaderIsActualInspector` -> sets `event.actualEmployeeId=leaderUserId=3001`, expects `ServiceException`, and asserts no review row is inserted.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `shouldRejectSelfReviewWhenLeaderIsActualInspector` did not throw `ServiceException`; tests run `4`, failures `1`, errors `0`.
- IMPLEMENTING: Added `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`; `reviewSubmission(...)` now checks `Objects.equals(reqBO.getLeaderUserId(), event.getActualEmployeeId())` after scope validation and before latest-review lookup/insert.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `4`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `15`, failures `0`, errors `0`, BUILD SUCCESS.
- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, `verification-report.md`, and `backend-api-evidence.md` -> UPDATED to include AC-D35 backend RED/GREEN evidence and keep M6 `in_progress`.
- Decision: AC-D35 self-review backend guard is now target-JUnit GREEN in the independent worktree, but AC-D35 and M6 are still not `ACCEPTED` because real-page self-confirm action, permissions/read-only proof, cleanup, broader concurrency/performance gates, and 62 AC coverage remain open; no `git push` performed.

## M6 - AC-M21 / AC-D37 PQC Process-inspection Aggregation Backend Slice

- BDD: approved PQC review aggregates process-inspection evidence -> Given a formal PQC submission event has a `PENDING` process-inspection aggregation status When a team leader approves the submission Then the backend must atomically mark that PQC record as `AGGREGATED` with the review id and aggregation timestamp; rejected, missing, already aggregated, or concurrently consumed records must not count as process-inspection completion.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest` now asserts `APPROVED` calls `MesPqcProcessInspectionAggregationService#aggregateApprovedPqcSubmission(eventId, reviewId)` and `REJECTED` never aggregates.
- TEST_ADDED: `MesPqcProcessInspectionAggregationServiceTest` covers pending-to-aggregated update, missing PQC record fail-fast, already aggregated fail-fast, and concurrent zero-row update fail-fast.
- TEST_ADDED: `MesProcessPoolSchemaTest` covers `processInspectionAggregationStatus`, `processInspectionReviewId`, `processInspectionAggregatedAt`, and migration `20260803_mes_process_pool_pqc_process_inspection_aggregation.sql`.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: `MesPqcProcessInspectionAggregationService` did not exist after the behavior test required formal aggregation.
- TEST_FIX: `MesProcessPoolSchemaTest` initially failed on the escaped SQL default string because the idempotent migration passes column DDL through a stored procedure string; assertion now matches `DEFAULT ''PENDING''` without changing the effective MySQL default.
- IMPLEMENTING: Added process-inspection aggregation columns to `MesProProcessPoolPqcRecordDO`, H2 schema, and MySQL migration; `MesProcessPoolEventServiceImpl#createPqcInspectionEvent` now explicitly creates PQC records with `PENDING` aggregation status.
- IMPLEMENTING: Added `MesPqcProcessInspectionAggregationServiceImpl`, `MesProProcessPoolPqcRecordMapper#updateProcessInspectionAggregatedIfPending(...)`, `PRO_PROCESS_POOL_PQC_RECORD_REQUIRED`, and `PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED`.
- IMPLEMENTING: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` now calls aggregation only after inserting an `APPROVED` review; `REJECTED` reviews remain recorded but excluded from process-inspection completion.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `1`, failures `0`, errors `0`, BUILD SUCCESS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `9`, failures `0`, errors `0`, BUILD SUCCESS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `4`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `24`, failures `0`, errors `0`, BUILD SUCCESS.
- Decision: AC-M21/AC-D37 process-inspection aggregation has backend schema/service/review-trigger GREEN evidence, but AC-M21/AC-D37 and M6 are still not `ACCEPTED` because real-page confirmation visibility, read-only verification, cleanup, broader concurrency/performance gates, and 62 AC coverage remain open; no `git push` performed.

## M6 - AC-M21 / AC-D37 Migration Policy Refresh

- RED_RETRY: `run-release-migration-policy-gate.py` with existing gate `file` paths -> FAIL，expected reason：`--sql-file` paths like `sql/mysql/...` were resolved from workspace root and did not exist.
- RED_RETRY: `run-release-migration-policy-gate.py` with sql-root-relative basenames -> FAIL，expected reason：the policy script validates `--sql-file` path existence before applying `--sql-root`, so bare basenames were not valid workspace paths.
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file <16 workspace-relative M6 SQL paths> --output doc\tasks\20260801-role-requirement-matrix-implementation\m6-migration-policy-gate.json` -> PASS，`migrationCount=16`; `20260803_mes_process_pool_pqc_event_source` and `20260803_mes_process_pool_pqc_process_inspection_aggregation` are now included.
- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, and `verification-report.md` -> UPDATED to reflect the current 16-file release migration policy gate.

## M6 - AC-M21 / AC-D37 PQC Aggregation Event-Type Isolation

- BDD: AC-M21/AC-D37 process-inspection aggregation must only consume PQC inspection reviews -> Given the shared `submission/review` backend can review production and PQC process-pool events When an `APPROVED` review is inserted for a `PRODUCTION_SUBMIT` event Then the service must keep the review record but must not call PQC process-inspection aggregation; only `PQC_INSPECTION` approved reviews may mark PQC records as aggregated.
- TEST_ADDED: `MesTeamLeaderSubmissionReviewServiceTest#shouldNotAggregateApprovedProductionSubmission` -> uses a `PRODUCTION_SUBMIT` event and asserts `MesPqcProcessInspectionAggregationService#aggregateApprovedPqcSubmission(...)` is never called.
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `shouldNotAggregateApprovedProductionSubmission` observed `aggregateApprovedPqcSubmission(1001, 7003)` for a production event; tests run `6`, failures `1`, errors `0`.
- IMPLEMENTING: `MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission(...)` now calls PQC aggregation only when review status is `APPROVED` and locked event type is `PQC_INSPECTION`; approved production reviews continue to insert the review but do not require or mutate a PQC record.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `6`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `25`, failures `0`, errors `0`, BUILD SUCCESS.
- Decision: The backend now isolates process-inspection aggregation to approved PQC inspection events and no longer fails production reviews on missing PQC records; AC-M21/AC-D37 still remain not `ACCEPTED` because real-page confirmation visibility, read-only verification, cleanup, broader concurrency/performance gates, and 62 AC coverage remain open; no `git push` performed.

## M6 - AC-M21 / AC-D37 PQC Aggregation Read-model And Real-page Visibility Contract

- BDD: AC-M21/AC-D37 aggregated PQC review must be visible in the team-leader read model -> Given a PQC submission has been approved by the PQC leader and its process-inspection record is marked `AGGREGATED` When the PQC leader submission page and timeline read model are reloaded Then the same event must expose `processInspectionAggregationStatus`, `processInspectionReviewId`, and `processInspectionAggregatedAt`, and the page must visibly show the aggregation state from the formal read model.
- TEST_ADDED: `ProcessPoolTimelineQueryTest#shouldExposePqcProcessInspectionAggregationStatus` asserts the timeline service copies aggregation status, review id, and aggregation time from `ProcessPoolTimelineEventReadDO` into `ProcessPoolTimelineEventRespVO`.
- TEST_ADDED: `process-pool-timeline-mapper-static.spec.cjs` now requires `pqc.process_inspection_aggregation_status`, `pqc.process_inspection_review_id`, and `pqc.process_inspection_aggregated_at` from `mes_pro_process_pool_pqc_record`.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires process-inspection aggregation API fields, stable `data-team-leader-review-event-id`, visible `data-pqc-process-inspection-aggregation`, and real-flow action `pqcLeaderReviewApprovedAndAggregated`.
- RED: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: missing `setProcessInspectionAggregationStatus(...)` on `ProcessPoolTimelineEventReadDO` and missing response getters on `ProcessPoolTimelineEventRespVO`.
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL, expected reason: mapper did not select `pqc.process_inspection_aggregation_status AS processInspectionAggregationStatus`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real E2E script lacked `verifyPqcLeaderReviewApprovalAggregatesProcessInspection`.
- IMPLEMENTING: Added aggregation fields to `ProcessPoolTimelineEventReadDO`, `ProcessPoolTimelineEventRespVO`, `ProcessPoolTimelineServiceImpl`, and `MesProProcessPoolTimelineReadMapper.xml`; added frontend API type fields; added a PQC-only aggregation status column and stable review event selector to `TeamLeaderWorkbenchPage.vue`; extended real-flow E2E to run PQC inspector submission before PQC leader review, click the real page review button by event id, call `/mes/pro/process-pool/team-leader/submission/review`, and verify the approved row returns `AGGREGATED` with matching review id.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `2`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderControllerTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,MesProcessPoolPqcEventTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `28`, failures `0`, errors `0`, BUILD SUCCESS.
- GREEN: `pnpm --dir IntRuoyiFronted install --frozen-lockfile` -> PASS, installed the independent worktree frontend dependencies from the existing lockfile so `ts:check` could run in this worktree.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- GREEN: `git diff --check` -> PASS, only LF/CRLF working-copy warnings, no whitespace errors.
- Decision: AC-M21/AC-D37 now have backend status, event-type isolation, read-model field propagation, and real-page visibility contract GREEN. They remain not `ACCEPTED` because the new code has not yet been loaded into the task runtime for a full real E2E approval/aggregation run, and M6 still requires read-only proof, cleanup, broader concurrency/performance gates, and full 62 AC coverage; no `git push` performed.

## M6 - Independent Worktree Runtime Real E2E Approval Aggregation

- BDD: AC-M21/AC-D37 live PQC approval aggregation must use a review-safe actual employee -> Given the PQC leader reviewer is a distinct accountable user When the PQC inspector selects an actual employee and submits a formal PQC inspection Then the E2E must not select the same user as the later PQC leader reviewer, and an approved real-page review must aggregate the process-inspection record.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real-flow `readText(filePath)` did not normalize CRLF to LF, causing a Windows-only false SOURCE blocker for batchRecordFormNames/formBindings separation.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static`, `pnpm --dir IntRuoyiFronted e2e:role-matrix-route-config-separation:static`, and `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS after `readText(...)` normalizes `\r\n` to `\n`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on worktree frontend/backend `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- RED_RUNTIME: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> FAIL before structured blockers because PQC leader submission page returned business 500; read-only schema probe showed local runtime DB lacked `process_inspection_aggregation_status`, `process_inspection_review_id`, and `process_inspection_aggregated_at` on `mes_pro_process_pool_pqc_record`.
- IMPLEMENTING_RUNTIME: Applied task-owned M6 migrations `20260803_mes_process_pool_pqc_event_source.sql` and `20260803_mes_process_pool_pqc_process_inspection_aggregation.sql` to the authorized local Docker MySQL test DB; read-only probes confirmed the three aggregation columns, `idx_mes_pp_pqc_process_inspection`, and PQC_TASK nullable device/workstation dependencies.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after runtime migration -> PASS.
- RED_RUNTIME: authorized full real E2E then returned structured blocker `E2E_PQC_REVIEW_BUSINESS:pqcLeaderReviewApprovedAndAggregated` because actualEmployeeId `512` equaled the PQC leader reviewer and backend correctly rejected self-review.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `resolveRoleUserId`, `reviewerUserId`, `excludedReviewerUserIds`, and `E2E_PQC_PERSONNEL_REVIEWER` so the real E2E must prove reviewer-safe actual employee selection.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real-flow lacked `resolveRoleUserId` before the reviewer-safe selection implementation.
- IMPLEMENTING: `verifyPqcActualEmployeeSwitch(...)` now logs in the configured `pqcLeader`, resolves reviewerUserId, excludes reviewer user IDs from actual employee candidates, records `nonLoginCandidateAvailable`, and asserts the switched actual employee is not the later reviewer.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` and `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- RUNTIME: frontend 8098 had stopped and `real:check` reported `RUNTIME:frontendLogin`; restarted only the task-owned worktree frontend with `scripts\runtime\start-branch-frontend.ps1`, then confirmed frontend HTTP `200` and backend health `UP` on `48098`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` after frontend restart -> PASS.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=11`, `gateEvidence=2`, `blockers=65`; `pqcActualEmployeeSelected=PASS` selected actualEmployeeId `659` while reviewerUserId was `512`, `pqcFormalSubmissionCreated=PASS` created submittedTaskId `19` / eventId `28` / signatureId `26`, and `pqcLeaderReviewApprovedAndAggregated=PASS` returned reviewId `6` with `processInspectionAggregationStatus=AGGREGATED` and `processInspectionReviewId=6`.
- Decision: The previous live approval/aggregation blocker is resolved at real-page action level in the independent worktree. M6 remains `in_progress` because `activeOrderCleanupDeferred`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` blockers remain open; no `git push` performed.
## M6 - AC-D33 PQC Submission Detail Traceability And Permission Isolation

- BDD: AC-D33 PQC submission detail must preserve piece-level payload and reject unauthorized readers -> Given a formal PQC submission was created from the real PQC inspector page When the PQC leader opens the submitted event detail Then the page and detail API must show the same event/task/signature/actual employee, original payload, and all piece values; When an unauthorized actor tries the same detail Then the real page must expose no detail entry and the backend detail endpoint must not return business success.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyPqcLeaderSubmissionDetailTraceability`, `pqcLeaderSubmissionDetailTraceable`, stable detail selectors, visible signature/original-payload markers, `verifyPqcLeaderSubmissionDetailUnauthorizedBlocked`, `pqcLeaderSubmissionDetailUnauthorizedBlocked`, `E2E_PQC_DETAIL_PERMISSION`, and the `全部合格` bulk-button click in `completePqcPieceDetailsForSubmission(...)`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real E2E script lacked `verifyPqcLeaderSubmissionDetailUnauthorizedBlocked` for AC-D33 detail permission isolation.
- IMPLEMENTING: Added `verifyPqcLeaderSubmissionDetailUnauthorizedBlocked(...)`, using dedicated `unauthorizedActor` browser context to prove the real page has no `[data-team-leader-detail-event-id]` for the submitted event and `/mes/pro/process-pool/team-leader/submission/detail` returns non-success (`403 没有该操作权限`).
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: the real E2E still clicked `合格`, but the Element Plus page only exposes the visible bulk button `全部合格`; choice inspection items therefore kept blank `pqcPieceValues`.
- IMPLEMENTING: `completePqcPieceDetailsForSubmission(...)` now clicks `getByRole('button', { name: /^全部合格$/ })`, so every choice inspection item receives non-empty values before formal PQC submit.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on worktree frontend/backend `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with task-owned temporary signature id `98003302` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=13`, `gateEvidence=2`, `blockers=65`; `pqcFormalSubmissionCreated=PASS` created `submittedTaskId=21` / `eventId=30` / `signatureId=98003302`, `pqcLeaderSubmissionDetailTraceable=PASS` returned `pieceDetailCount=105` and `pieceValueGroupCount=14`, `pqcLeaderSubmissionDetailUnauthorizedBlocked=PASS` returned business `403 没有该操作权限` for `aoteman`, and `pqcLeaderReviewApprovedAndAggregated=PASS` returned `reviewId=8` with `processInspectionAggregationStatus=AGGREGATED`.
- Decision: AC-D33 now has real-page positive detail traceability and unauthorized detail-read rejection evidence. AC-D33 and M6 still remain not `ACCEPTED` because full failure paths, read-only proof, cleanup, broader concurrency/performance gates, and the 62 AC coverage ledger remain open; no `git push` performed.
## M6 - AC-D35 PQC Self-review Real-action Blocker Proof

- BDD: AC-D35 PQC self-review must be blocked through the real leader review path -> Given a submitted PQC inspection event whose actual inspector is also the reviewing leader When the PQC leader tries to submit a terminal review Then the backend must reject with `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, no terminal review is written, and the event remains pending for another eligible reviewer.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyPqcLeaderSelfReviewBlocked`, `pqcLeaderSelfReviewBlocked`, `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and `E2E_PQC_REVIEW_SELF` in the real E2E script.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: the real E2E script lacked `verifyPqcLeaderSelfReviewBlocked`.
- IMPLEMENTING: `verifyPqcLeaderSelfReviewBlocked(...)` now resolves the real `pqcLeader` user id, finds a pending PQC leader submission whose `actualEmployeeUserId` equals that reviewer, calls the formal `/mes/pro/process-pool/team-leader/submission/review` endpoint, expects `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and re-queries the same submission page to prove it is still pending.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on worktree frontend/backend `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with task-owned temporary signature id `98003304` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=15`, `gateEvidence=2`, `blockers=65`; `pqcFormalSubmissionCreated=PASS` created `submittedTaskId=23` / `eventId=32` / `signatureId=98003304`, `pqcLeaderReviewApprovedAndAggregated=PASS` returned `reviewId=10` and `processInspectionAggregationStatus=AGGREGATED`, `pqcLeaderDuplicateTerminalReviewBlocked=PASS` rejected duplicate terminal review with `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`, and `pqcLeaderSelfReviewBlocked=PASS` rejected eventId `24` with reviewerUserId `512`, actualEmployeeId `512`, `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and `stillPending=true`.
- Decision: AC-D34 and AC-D35 now have real negative review action evidence in addition to backend RED/GREEN guards, but neither AC is `ACCEPTED`; cleanup, broader concurrency/performance gates, read-only proof, and the 62 AC coverage ledger remain open. No `git push` performed.
## M6 - AC-D35 Evidence Sync Verification

- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, `verification-report.md`, and `execution-log.md` -> UPDATED to reflect D34 duplicate-terminal and D35 self-review real-action PASS evidence while keeping M6 `in_progress`.
- GREEN: UTF-8 JSON parse for `task-state.json` -> PASS，status=`in_progress`，currentMilestone=`M6`，activeSlice=`M6 AC-D35 self-review real-action PASS on independent worktree runtime; cleanup, broader concurrency, performance and coverage gates remain`.
- GREEN: UTF-8 JSON parse for `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS，status=`BLOCKED`，phaseEvidence=6，actionEvidence=15，gateEvidence=2，blockers=65，failedActions=0.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- BLOCKED_ENV: first `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` in the fresh shell -> BLOCKED with 34 missing `RRM_*` env blockers; expected reason: this shell did not inherit the authorized local RRM test environment.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` with one-time task env injection on worktree frontend/backend `8098/48098` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME.
- GREEN: `git diff --check` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- E2E_RESTORE: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` rerun after check-only `result.json` overwrite -> STRUCTURED_BLOCKED，exit 2，with `phaseEvidence=6`，`actionEvidence=15`，`gateEvidence=2`，`blockers=65`，`failedActions=0`；the canonical `result.json` is again full-real evidence, not check-only evidence.
- E2E: latest full real rerun used task-owned temporary signature id `98003305` and created `submittedTaskId=24` / `eventId=33`; `pqcLeaderReviewApprovedAndAggregated=PASS` returned `reviewId=11` / `AGGREGATED`, `pqcLeaderDuplicateTerminalReviewBlocked=PASS` rejected the duplicate terminal review with `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`, and `pqcLeaderSelfReviewBlocked=PASS` still rejects eventId `24` with reviewerUserId `512`, actualEmployeeId `512`, `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and `stillPending=true`.
## M6 - AC-M21 / AC-D37 Process-inspection Aggregation Read-only Proof

- BDD: AC-D37 process-inspection aggregation read-only proof -> Given one PQC submission has been approved and one self-review attempt remains pending When the PQC leader reloads the submission read model through the real page session Then only the approved event is `AGGREGATED` with its review id, and the pending self-review-blocked event is not aggregated.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyPqcProcessInspectionAggregationReadOnly`, approved/pending read-model checks, `processInspectionAggregationStatus`, `AGGREGATED`, negative `notEqual`, and `E2E_PQC_AGGREGATION_READONLY`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: the real E2E script lacked `verifyPqcProcessInspectionAggregationReadOnly`.
- IMPLEMENTING: `verifyPqcProcessInspectionAggregationReadOnly(...)` now reloads the formal `/mes/pro/process-pool/team-leader/submission/page` read model after real approval and self-review rejection; it asserts the approved event is `AGGREGATED` with the same `reviewId`, and asserts the pending self-review-blocked event is not `AGGREGATED` and has no `processInspectionReviewId`.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with task-owned temporary signature id `98003306` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=16`, `gateEvidence=2`, `blockers=65`, and `failedActions=0`; `pqcFormalSubmissionCreated=PASS` created `submittedTaskId=25` / `eventId=34` / `signatureId=98003306`, `pqcLeaderReviewApprovedAndAggregated=PASS` returned `reviewId=12` and `processInspectionAggregationStatus=AGGREGATED`, and `pqcProcessInspectionAggregationReadOnly=PASS` proved approvedEventId `34` is `AGGREGATED` while pendingEventId `24` remains `PENDING`.
- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, and `verification-report.md` -> UPDATED to reflect AC-D37 read-only PASS evidence while keeping M6 `in_progress` and preserving cleanup/concurrency/performance/coverage blockers.
- Decision: AC-M21/AC-D37 now have backend status/type-isolation/read-model/page-visibility plus real read-only aggregation evidence, but they remain not `ACCEPTED`; cleanup, broader concurrency/performance gates, and full 62 AC coverage remain open. No `git push` performed.

## M6 - AC-D30 PQC Rejected Correction Revision Chain

- BDD: AC-D30 PQC rejected correction requires latest rejected review -> Given a submitted PQC inspection has an original payload, a terminal rejection reason, and no accepted correction yet When the inspector submits a correction with changed payload, field diff, correction reason, and a new signature Then the backend must allow the revision only if the latest submission review is `REJECTED`, preserve the original payload as `beforePayload`, record the corrected payload as `afterPayload`, keep the rejection reason in the review chain, and reject correction attempts when the latest review is missing or already `APPROVED`.
- TEST_ADDED: `MesProcessPoolEventRevisionServiceTest#rejectsCorrectionWhenLatestSubmissionReviewIsMissing` and `#rejectsCorrectionWhenLatestSubmissionReviewIsApproved` -> assert revision creation requires the latest locked submission review to be `REJECTED` and writes no revision when the review is missing or already approved.
- TEST_ADDED: `MesProcessPoolEventRevisionServiceTest` positive path and `MesProcessPoolEventRevisionFifoLockTest` now stub `MesProcessPoolSubmissionReviewMapper#selectLatestByEventIdForUpdate(...)` with a latest `REJECTED` review so existing revision and FIFO tests still prove their original behavior after the new AC-D30 gate.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL at testCompile, expected reason: AC-D30 behavior test required a new constructor dependency, locked latest-review mapper method, and formal error code before production code supported the rejected-review gate.
- IMPLEMENTING: `MesProcessPoolEventRevisionServiceImpl#updateOriginalRecord(...)` now locks the event, loads the latest submission review with `LIMIT 1 FOR UPDATE`, requires `reviewStatus=REJECTED`, and throws `PRO_PROCESS_POOL_REVISION_REJECTED_REVIEW_REQUIRED` for missing or non-rejected latest reviews before inserting revision or diff rows.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `13`, failures `0`, errors `0`, BUILD SUCCESS.
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionFifoLockTest,MesTeamLeaderSubmissionReviewServiceTest,ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineQueryTest,MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, tests run `27`, failures `0`, errors `0`, BUILD SUCCESS.
- Decision: AC-D30 rejected-correction revision-chain backend gate is target/regression GREEN in the independent worktree, but AC-D30 and M6 remain not `ACCEPTED` until the real page rejection/correction action, read-only proof, cleanup, broader concurrency/performance gates, and the 62 AC coverage ledger close; no `git push` performed.

## M6 - AC-D30 PQC Rejected Correction Real-page Action

- BDD: AC-D30 rejected correction must be proven through real pages -> Given a PQC submission is pending for a leader who is not the actual inspector When the PQC leader rejects it and opens the visible correction action Then the system must submit `/event-revision/update-original`, write a revision id with a new configured signature id, preserve the rejection reason, and expose `modificationHistorySummary` in the read model.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `preparePqcRejectedCorrectionCandidate`, real PQC inspector candidate preparation, real leader correction selector, configured unused signature-pool usage for `revisionSignatureId`, and forbids `Date.now()` as a generated revision signature id.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: AC-D30 real-flow still used `Date.now()` for `revisionSignatureId` and lacked `preparePqcRejectedCorrectionCandidate`.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now uses `resolveUnusedPqcSignatureId(...)` for AC-D30 revision signatures and, when the current leader page has no eligible pending submission, prepares a candidate through the real PQC inspector page path (`verifyPqcActiveOrderReadOnly` -> `verifyPqcRegulationItemsRendered` -> `verifyPqcPieceDetailQuantityPrepared` -> `verifyPqcActualEmployeeSwitch` -> `verifyPqcFormalSubmissionCreatesEvent`) before the leader rejects and corrects it.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree frontend/backend `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E_RED: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after initial AC-D30 real action -> STRUCTURED_BLOCKED with `pqcLeaderRejectedCorrectionChain=BLOCKED/E2E_PQC_REJECT_CORRECTION`, expected reason: after the new PQC submission was approved for aggregation there was no remaining eligible pending event for rejected correction.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` after candidate-preparation fix -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=17`, `gateEvidence=2`, `blockers=65`, `failedActions=0`; `pqcLeaderRejectedCorrectionChain=PASS` rejected and corrected `eventId=37`, returned `reviewId=15`, `revisionId=2`, `preparedSubmittedTaskId=28`, `preparedSignatureId=98003314`, `revisionSignatureId=98003315`, and read-model `modificationHistorySummary=原始记录已修改 1 次`.
- Decision: AC-D30 now has backend target/regression GREEN plus real-page rejected-correction PASS action evidence. It is still not `ACCEPTED` because cleanup, broader concurrency/performance, read-only/failure-path breadth, and the 62 AC coverage ledger remain open. No `git push` performed.

## M6 - D30 Evidence Sync And Gate Recheck

- TASK_DOCS: `task-state.json`, `task.md`, and `verification-report.md` -> UPDATED to replace stale AC-D30 backend-only wording with latest real-page `pqcLeaderRejectedCorrectionChain=PASS` evidence while keeping M6 `in_progress`.
- GREEN: UTF-8 JSON parse for `task-state.json` and latest `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `git diff --check` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- Decision: Evidence is now consistent that current M6 has `phaseEvidence=6`, `actionEvidence=17`, `gateEvidence=2`, `blockers=65`, and no failed action/gate; remaining blockers are still `activeOrderCleanupDeferred`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - Concurrency Gate Evidence Ledger Correction

- BDD: M6 concurrency gate must report all observed CONC AC evidence -> Given later M6 slices have real actions for PQC submit, review, and process-inspection aggregation When the full-real evidence builds `m6ConcurrencyGateDeferred` Then the blocker must remain open but list all observed CONC acceptance ids instead of saying only AC-M04 has evidence.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `observedConcurrencyAcceptanceIds` and verifies `buildM6ConcurrencyPerformanceGateEvidence` writes observed concurrency acceptance ids into gate evidence.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: `role-requirement-matrix-real-flow.e2e.js` lacked `observedConcurrencyAcceptanceIds` and still described the concurrency gate as AC-M04-only.
- IMPLEMENTING: `buildM6ConcurrencyPerformanceGateEvidence(...)` now derives CONC action keys and `observedConcurrencyAcceptanceIds` from all PASS action evidence while preserving `m6ConcurrencyGateDeferred` as `BLOCKED`.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Decision: This corrects the evidence ledger only; it does not clear the concurrency gate because the full 12 CONC AC still need complete concurrent terminal-state proof. No `git push` performed.

## M6 - AC-D35 Self-review Candidate Preparation Recovery

- BDD: AC-D35 self-review blocker must prepare missing real data through the PQC inspector page -> Given the PQC leader page has no pending event whose `actualEmployeeUserId` equals the PQC leader reviewer When the real E2E reaches the self-review negative path Then the script must use the PQC inspector page to select the reviewer as actual inspector, submit a formal PQC event, and then prove the leader review endpoint rejects it with `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: missing `preparePqcSelfReviewCandidate(...)` and no call from `verifyPqcLeaderSelfReviewBlocked(...)` to prepare the candidate through the real PQC inspector path.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now adds `preparePqcSelfReviewCandidate(...)`, reuses the real PQC inspector page chain (`verifyPqcActiveOrderReadOnly` -> `verifyPqcRegulationItemsRendered` -> `verifyPqcPieceDetailQuantityPrepared` -> `switchPqcActualEmployeeToUser` -> `verifyPqcFormalSubmissionCreatesEvent`), and retries the PQC leader pending read model before returning `E2E_PQC_REVIEW_SELF`.
- IMPLEMENTING: `verifyPqcFormalSubmissionCreatesEvent(...)` now selects the latest matching employee evidence and accepts the explicit `pqcSelfReviewActualEmployeeSelected` action evidence, so the prepared event uses `actualEmployeeId=reviewerUserId` rather than the normal non-reviewer PQC inspector.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: runtime health probe on independent worktree `8098/48098` -> PASS, frontend HTTP `200`, backend health `UP`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with the base six-ID signature pool -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=17`, `gateEvidence=2`, `blockers=66`, and `failedActions=0`; `pqcFormalSubmissionCreated=PASS` created `submittedTaskId=35` / `eventId=46` / `signatureId=98003394` for normal approval, `pqcLeaderReviewApprovedAndAggregated=PASS` returned `reviewId=24`, `pqcLeaderSelfReviewBlocked=PASS` proved self-review eventId `47` with `actualEmployeeId=512` remains pending after `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and `pqcProcessInspectionAggregationReadOnly=PASS` proved only the approved event is aggregated while the self-review-blocked event is not.
- BLOCKED: `pqcLeaderRejectedCorrectionChain=BLOCKED/E2E_PQC_SIGNATURE_POOL`, expected reason: the latest full-real command used only the base six configured signature IDs and all were already consumed by process-pool events; the script correctly refused to invent or reuse a signature ID and reported that more formal configured signature IDs are required for another AC-D30 candidate.
- Decision: AC-D35 and AC-M21/AC-D37 are recovered to real-action PASS in the latest canonical `result.json`. AC-D30 remains target/backend GREEN historically, but the current canonical full-real result is blocked by configured signature-pool exhaustion until additional formal task-owned signature IDs are provided or prepared. M6 remains `in_progress`; no `git push` performed.

## M6 - Extended Signature Pool Full-real Canonical Evidence Sync

- BDD: AC-D30 rejected correction must remain real-page proven when the task-owned formal signature pool has enough unused IDs -> Given the prior base-six rerun correctly blocked instead of reusing a consumed signature ID When the authorized full-real E2E reruns with the extended configured signature pool Then the rejected-correction chain must complete through real pages, and remaining blockers must be limited to cleanup, concurrency, performance, and full AC coverage.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with extended signature pool on independent worktree runtime `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=17`, `gateEvidence=2`, `blockers=65`, `failedActions=0`, `actionObserved=18`, `surfaceObserved=24`, `uncovered=20`, and `pending=62`.
- E2E: `pqcLeaderRejectedCorrectionChain=PASS` in the latest canonical `result.json`; eventId `49` was rejected and corrected through the real page path, returning `reviewId=26`, `revisionId=7`, `preparedSubmittedTaskId=37`, `preparedSignatureId=98003401`, `revisionSignatureId=98003402`, and `modificationHistorySummary=原始记录已修改 1 次`.
- E2E: `pqcLeaderSelfReviewBlocked=PASS` remains valid for eventId `47` with reviewerUserId `512`, actualEmployeeId `512`, expected error `PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN`, and `stillPending=true`; `pqcProcessInspectionAggregationReadOnly=PASS` proves approvedEventId `48` is `AGGREGATED` while pendingEventId `47` remains `PENDING`.
- BLOCKERS: latest canonical blocker set is now exactly `activeOrderCleanupDeferred`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. There is no current `E2E_PQC_SIGNATURE_POOL` blocker and no failed action.
- Decision: This supersedes the immediately prior base-six signature-pool result for current-state reporting only; the base-six run remains historical evidence that the script fails fast when the configured formal signature pool is exhausted. M6 remains `in_progress` because cleanup, concurrency, performance, and full 62 AC coverage are still open. No `git push` performed.

## M6 - Extended Signature Evidence Report Sync Verification

- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, `verification-report.md`, and `execution-log.md` -> UPDATED to reflect the latest extended-signature canonical result and remove `E2E_PQC_SIGNATURE_POOL` from the current blocker set.
- GREEN: UTF-8 JSON parse for `task-state.json` and `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS; state remains `in_progress` / `M6`, latest result is `BLOCKED`, `phaseEvidence=6`, `actionEvidence=17`, `gateEvidence=2`, `blockers=65`, `failedActions=0`, `pqcLeaderRejectedCorrectionChain=PASS`, and no current signature-pool blocker is present.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `git diff --check` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- Decision: Current M6 has no clearly failing action; it remains blocked only by `activeOrderCleanupDeferred`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - QA Regulation Page Action Structured Blocker

- BDD: QA regulation maintenance page must not be proven by the old QC template shell -> Given BDD-07 requires product / route version / process QA regulation lifecycle proof When the QA role enters the current QA regulation entry Then the real E2E must either observe formal published-version page selectors or emit `E2E_QA_REGULATION_PAGE` instead of treating `/mes/qc/template` as accepted proof.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `qaRegulationEntry` to call `verifyQaRegulationPublishedVersionReadOnly`, checks stable `qaRegulationPublishedVersionReadOnly` action evidence, full BDD-07 acceptance ids, formal selector evidence, and `E2E_QA_REGULATION_PAGE`.
- RED: `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: the real E2E script did not include `verifyQaRegulationPublishedVersionReadOnly`.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now wires `qaRegulationEntry.actionKey = verifyQaRegulationPublishedVersionReadOnly`; the action checks formal QA regulation published-version selectors and returns structured `E2E_QA_REGULATION_PAGE` evidence when the current page only proves shell load.
- GREEN: `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: runtime health probe on independent worktree `8098/48098` -> PASS, frontend HTTP `200`, backend health `UP`.
- E2E: authorized `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=18`, `gateEvidence=2`, `blockers=66`, and `failedActions=0`; `qaRegulationPublishedVersionReadOnly=BLOCKED/E2E_QA_REGULATION_PAGE` because no formal published QA regulation page selector evidence was visible on the current QA entry.
- GREEN: `git diff --check -- IntRuoyiFronted/tests/e2e/role-requirement-matrix-preflight-static.spec.cjs IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, `verification-report.md`, and `execution-log.md` -> UPDATED to reflect latest canonical 18-action / 66-blocker result and the new QA regulation page blocker.
- Decision: M6 remains `in_progress`; current blockers are `activeOrderCleanupDeferred`, `qaRegulationPublishedVersionReadOnly`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - QA PASS And Latest Result Evidence Sync

- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, and `verification-report.md` -> UPDATED to supersede the previous QA-page blocker wording with latest canonical full-real evidence.
- GREEN: UTF-8 JSON parse for `task-state.json` and `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS; state remains `in_progress` / `M6`, latest result is `BLOCKED`, `phaseEvidence=6`, `actionEvidence=18`, `gateEvidence=2`, `blockers=65`, `failedActions=0`, `qaRegulationPublishedVersionReadOnly=PASS`, and `pqcLeaderRejectedCorrectionChain=PASS` for `eventId=58`, `reviewId=36`, `revisionId=12`, `revisionSignatureId=98003413`.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `git diff --check` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- Decision: M6 remains `in_progress`; current blockers are `activeOrderCleanupDeferred`, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No clearly failing action is present, and no `git push` was performed.

## M6 - AC-D12 / AC-D38 Daily-close Performance Read-only Evidence

- BDD: AC-D12/AC-D38 daily-close performance evidence must read the real team-leader daily-close board -> Given the production leader has joined the task-owned active order When the M6 real E2E evaluates performance evidence Then it must read the visible `data-role-matrix-daily-close` cards, prove the formal daily-close status and card keys, and map the action to AC-D12 and AC-D38 without clearing the full performance gate.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyDailyClosePerformanceReadOnly`, `data-role-matrix-daily-close-card`, AC-D12, AC-D38, stable `dailyClosePerformanceReadOnly` action key, and production leader `joinActiveOrder` phase wiring.
- RED: `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: the partially added static contract had invalid `)assert.match` syntax and could not yet evaluate the missing daily-close action contract.
- IMPLEMENTING: fixed the static contract syntax, added `verifyDailyClosePerformanceReadOnly(...)`, read the real daily-close status and four `data-role-matrix-daily-close-card` cards, asserted stable card keys and numeric values, and wired the action after `joinActiveOrder`, conflict-route rejection, and cleanup traceability.
- CONTRACT_FIX: first GREEN attempt still failed because the static regex depended on token order instead of function behavior; the contract now extracts the daily-close action function block and asserts the required selector, AC ids, and action key independently.
- GREEN: `node --check D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: authorized `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=65`, and `failedActions=0`; `dailyClosePerformanceReadOnly=PASS`, `dailyCloseStatusText=可日结`, `cardCount=4`, `observedCardKeys=pending-review/rejected-review/active-orders/load-blocker`, and `active-orders=1`.
- TASK_DOCS: `task-state.json`, `task.md`, `test-report.md`, `verification-report.md`, and `execution-log.md` -> UPDATED to reflect latest canonical 19-action result and AC-D12/AC-D38 daily-close evidence while keeping M6 `in_progress`.
- GREEN: UTF-8 JSON parse for `task-state.json` and `IntRuoyiFronted/test-results/role-requirement-matrix-real-flow/result.json` -> PASS; state remains `in_progress` / `M6`, latest result is `BLOCKED`, `actionEvidence=19`, `blockers=65`, `failedActions=0`, and `dailyClosePerformanceReadOnly=PASS`.
- GREEN: `git diff --check` -> PASS; only LF/CRLF working-copy warnings were emitted, with no whitespace errors.
- Decision: This closes only the daily-close read-only evidence gap for AC-D12/AC-D38 observation. `m6PerformanceGateDeferred` remains BLOCKED because full N+1, paging drift, index, and query-count evidence is still missing; no `git push` performed.

## M6 - AC-D32 Timeline Performance Index Evidence

- BDD: AC-D32 PQC submission pagination must avoid row-level JSON task extraction -> Given the PQC leader submission board filters by product, process, employee, inspection type, round, review status, and submitted date When the backend reads page 1/page 2 with the same filters Then the query must use a persisted/indexable PQC task identity and supporting timeline/review indexes instead of per-row `JSON_EXTRACT` joins, while still keeping the full M6 performance gate open until N+1/query-count proof is complete.
- TEST_ADDED: `process-pool-timeline-mapper-static.spec.cjs` now requires `20260804_mes_process_pool_timeline_performance_indexes.sql`, generated `pqc_task_id`, `idx_mes_pp_event_timeline_acd32`, `idx_mes_pqc_task_timeline_acd32`, `idx_mes_pp_review_latest_event`, and mapper join `pqc_task.id = pool_event.pqc_task_id`; it also rejects `JSON_EXTRACT(pool_event.raw_payload, '$.pqcTaskId')` in the pagination join.
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> FAIL, expected reason: missing `20260804_mes_process_pool_timeline_performance_indexes.sql`.
- IMPLEMENTING: added `20260804_mes_process_pool_timeline_performance_indexes.sql` with release metadata, generated `mes_pro_process_pool_event.pqc_task_id`, AC-D32 event/task/latest-review indexes, and changed `MesProProcessPoolTimelineReadMapper.xml` to join PQC task through `pool_event.pqc_task_id`.
- GREEN_RETRY: first mapper static reruns failed on over-specific static string checks for procedure-composed `ADD COLUMN` / `ADD KEY` and SQL string escaping; the static contract was corrected to validate actual column/index definitions without weakening the behavior gate.
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-timeline-mapper-static.spec.cjs` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `run-release-migration-policy-gate.py` on the 17-file M6 migration chain -> PASS, `migrationCount=17`, new migration sha256 `8a339b19e596216861228e325bae84c9c91e24bb32d0bb573d4004ef35c70dcc`.
- RUNTIME_GREEN: authorized local DB execution of `20260804_mes_process_pool_timeline_performance_indexes.sql` against `int-ruoyi-mysql` -> PASS; schema verification shows generated column `pqc_task_id` and indexes `idx_mes_pp_event_timeline_acd32`, `idx_mes_pqc_task_timeline_acd32`, and `idx_mes_pp_review_latest_event`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS，0 SOURCE / 0 ENV / 0 RUNTIME.
- Decision: AC-D32 now has formal index/query-shape proof and local runtime schema proof, but this is still only partial M6 performance evidence. `m6PerformanceGateDeferred` remains BLOCKED until full N+1, paging drift, query-count, and broader daily-close/PQC-list/piece-detail performance proof is complete. No `git push` performed.

## M6 - AC-D32 Timeline Query-count Proof

- BDD: AC-D32 PQC submission pagination must avoid per-row detail lookups -> Given the PQC leader submission board reads page 1 and page 2 with the same formal filters When `ProcessPoolTimelineServiceImpl` builds each page Then each page must add exactly one count query and one page query, with zero per-row detail queries.
- TEST_ADDED: `ProcessPoolTimelineFilterTest#shouldUseCountAndPageQueriesWithoutPerRowDetailLookupsForPqcPagination` asserts page 1 produces `count=1/page=1/detail=0` and page 2 produces `count=2/page=2/detail=0`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `InMemoryTimelineReadMapper` did not expose `getCountQueryCalls`, `getPageQueryCalls`, or `getDetailQueryCalls` for the new AC-D32 query-count contract.
- IMPLEMENTING: `ProcessPoolTimelineTestSupport.InMemoryTimelineReadMapper` now tracks count/page/detail query calls separately while preserving the existing timeline read behavior.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `ProcessPoolTimelineFilterTest` 3 tests, 0 failures, 0 errors, BUILD SUCCESS.
- Decision: AC-D32 now has index/query-shape/runtime schema proof plus focused query-count proof for the timeline submission pagination path. `m6PerformanceGateDeferred` remains BLOCKED because daily-close, PQC list, and piece-detail paths still need complete N+1 / paging drift / query-count evidence, and AC-D32 still needs its broader failure-path/permission/cleanup coverage before `ACCEPTED`.

## M6 - AC-D12 / AC-D38 Daily-close Request-budget Evidence

- BDD: Daily-close card reading must not create hidden list/detail N+1 requests -> Given the production leader page has already loaded the formal daily-close cards When the M6 real E2E reads the daily-close cards Then the read-only action must record a request budget and prove zero submission page, active-order list, and submission detail requests during card evaluation.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyDailyClosePerformanceReadOnly(...)` to start `createDailyCloseRequestBudgetTracker`, assert `submissionDetailRequests=0`, and write `requestBudget` into action evidence.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: daily-close action did not start a request budget tracker.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now tracks GET requests to `/team-leader/submission/page`, `/team-leader/active-order/list`, and `/team-leader/submission/detail` while reading the daily-close cards, asserts all three counters are zero, and returns `requestBudget` with the action evidence.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized health + `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, backend `UP`, frontend HTTP `200`, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=66`, and `failedActions=0`; `dailyClosePerformanceReadOnly=PASS` with `dailyCloseStatusText=可日结`, `cardCount=4`, and `requestBudget={submissionPageRequests:0, activeOrderListRequests:0, submissionDetailRequests:0}`.
- BLOCKED: `pqcLeaderRejectedCorrectionChain=BLOCKED/E2E_PQC_SIGNATURE_POOL`, expected reason: all configured task-owned electronic signature IDs are now consumed by process-pool events; the script correctly refused to invent or reuse signatures and requires additional formal signature IDs before another AC-D30 full-real action can pass.
- Decision: AC-D12/AC-D38 now have real-page card evidence plus request-budget evidence for the daily-close card read path. M6 remains `in_progress`; current blockers are `activeOrderCleanupDeferred`, `pqcLeaderRejectedCorrectionChain` signature-pool exhaustion, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - AC-D27 Piece-detail Request-budget Evidence

- BDD: AC-D27 piece-detail modal must not hide per-item N+1 requests -> Given the PQC inspector page has loaded the formal QA regulation task and planned inspection quantity When the M6 real E2E opens and completes the piece-detail modal Then the action must record request-budget evidence and prove zero per-item piece-detail GET requests, zero active-order process snapshot reloads, and zero personnel reloads during modal completion.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyPqcPieceDetailQuantityPrepared(...)` to start `createPqcPieceDetailRequestBudgetTracker`, assert `pieceDetailRequests=0`, and write `requestBudget` into the AC-D27 action result.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: `verifyPqcPieceDetailQuantityPrepared(...)` did not start a piece-detail request-budget tracker.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now tracks GET requests to PQC piece-detail, active-order process snapshot, and PQC personnel endpoints while opening/filling piece-detail modals; the action asserts all tracked counts remain zero and returns `requestBudget`.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=72`, and `failedActions=0`; `pqcPieceDetailQuantityPrepared=PASS` with `uiQuantity=15`, `pieceRowCount=15`, `completedPieceValueCount=30`, `completedChoiceItemCount=6`, and `requestBudget={pieceDetailRequests:0, processSnapshotRequests:0, pqcPersonnelRequests:0}`.
- BLOCKED: current full-real run now blocks `pqcFormalSubmissionCreated`, D33/D34/D37 dependent leader actions, and `pqcLeaderRejectedCorrectionChain` because every configured task-owned signature ID is already consumed by process-pool events. This is expected fail-fast behavior; no signature was reused or invented.
- Decision: AC-D27 now has real-page quantity plus request-budget evidence, but M6 remains `in_progress`; current blockers are `activeOrderCleanupDeferred`, signature-pool exhaustion for new PQC submissions/corrections, `m6ConcurrencyGateDeferred`, `m6PerformanceGateDeferred`, and 62 `E2E_COVERAGE` items. No `git push` performed.

- GREEN: post-documentation structural verification -> PASS; `task-state.json` and latest `result.json` parse as UTF-8 JSON, `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` PASS, `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` PASS, and scoped `git diff --check` PASS with only LF/CRLF warnings.

## M6 - AC-D32 PQC Leader List Request-budget Evidence

- BDD: AC-D32 PQC leader list paging must not hide per-row detail or active-order reload requests -> Given the PQC leader page has a stable same-filter pagination candidate When the real E2E applies the filter and reads page 1 and page 2 Then the action must record a request budget, prove exactly bounded submission page requests, and prove zero submission detail or active-order list requests during the list evidence path.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `verifyPqcLeaderSubmissionFilterPaginationConsistency(...)` to start `createPqcLeaderSubmissionListRequestBudgetTracker`, assert `submissionDetailRequests=0`, and write `requestBudget` into the AC-D32 action result.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: PQC leader submission pagination action did not start a request budget tracker.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now tracks GET requests to `/team-leader/submission/page`, `/team-leader/submission/detail`, and `/team-leader/active-order/list` around the real PQC leader filter plus page 1/page 2 reads; it asserts `submissionDetailRequests=0`, `activeOrderListRequests=0`, and bounded `submissionPageRequests=3..4`, then returns the budget in action evidence.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=72`, and `failedActions=0`; `pqcLeaderSubmissionFilterPaginationConsistent=PASS`, `total=3`, `firstEventId=39`, `secondEventId=45`, and `requestBudget={submissionPageRequests:3, submissionDetailRequests:0, activeOrderListRequests:0}`.
- Decision: AC-D32 now has generated-column/index proof, count+page/zero-detail service proof, same-filter page 1/page 2 real evidence, and real-page request-budget evidence. `m6PerformanceGateDeferred` remains BLOCKED because daily-close and piece-detail still need backend query-count or paging-drift proof, and AC-D32 still needs broader failure-path, permission/read-only, cleanup, and full 62 AC acceptance before it can be marked `ACCEPTED`. No `git push` performed.
- GREEN: post-documentation structural verification -> PASS; `task-state.json` and latest `result.json` parse as UTF-8 JSON, `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` PASS, `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` PASS, and scoped `git diff --check` PASS with only LF/CRLF warnings.

## M6 - AC-D12/D38 and AC-D27 Backend Query-count Proof

- BDD: daily-close and piece-detail performance paths must avoid hidden N+1 queries -> Given the M6 performance gate already has real-page request-budget evidence for daily-close cards and PQC piece-detail modal completion When backend services prepare the matching submission summary, active-order list, and PQC task context Then the target tests must prove bounded count/page or bulk reads, with no per-row detail lookup and no per-process pending-task query.
- TEST_ADDED: `ProcessPoolTimelineFilterTest#shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary` asserts daily-close submission summary reads run count+page only and zero detail queries.
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance` asserts daily-close active-order card reads use one active-order query and never load/rebuild per-process snapshots.
- TEST_ADDED: `MesFrontlinePqcContextServiceTest#shouldPreparePqcPieceDetailContextWithBulkQueriesOnly` asserts PQC piece-detail context preparation bulk-loads route processes, active-order PQC tasks, and regulation items, and never calls `selectPendingByActiveOrderProcess` or `selectById`.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: daily-close backend query-count proof was not recognized by the static gate.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest,MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `MesFrontlinePqcContextServiceTest#shouldPreparePqcPieceDetailContextWithBulkQueriesOnly` hit `PRO_FRONTLINE_PQC_TASK_REQUIRED` because `MesFrontlinePqcContextServiceImpl` still looked up pending tasks with per-process `selectPendingByActiveOrderProcess`.
- IMPLEMENTING: `MesFrontlinePqcContextServiceImpl#resolvePqcTaskContext(...)` now resolves the pending task from the already bulk-loaded active-order task list using the same `businessDate / inspectionType / roundNo / id` ordering as `MesPqcInspectionTaskMapper#selectPendingByActiveOrderProcess`; submitted tasks are still skipped for list context, and `submitPqcInspection(...)` keeps the stale submitted-task `PRO_FRONTLINE_PQC_TASK_STATUS_INVALID` guard before active-process resolution.
- TEST_FIX: `MesFrontlinePqcContextServiceTest` now uses a shared `selectListByActiveOrderId` fixture list for route-process context tests and removes old per-process pending-task stubs; the static gate now matches the actual JUnit `assertEquals(1, mapper.getCountQueryCalls())` style without weakening the count/page/detail proof.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineFilterTest,MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 23 tests, 0 failures, 0 errors, BUILD SUCCESS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- Decision: Daily-close and piece-detail backend query-count proof is now GREEN for the current performance slice. M6 remains `in_progress`; `m6PerformanceGateDeferred` is not closed until full real failure paths, permission/read-only breadth, cleanup, runtime/paging-drift evidence, and 62 AC coverage are complete. No `git push` performed.


## M6 - Performance Gate Closure

- BDD: M6 performance gate closes only with complete request-budget and backend proof -> Given AC-D12/AC-D38, AC-D27, and AC-D32 already have real-page request-budget or pagination evidence When the full real E2E builds gate evidence Then the performance gate must collect backend query-count/index proof and emit `m6PerformanceGateVerified=PASS` only when all four PERF AC and all proof flags are complete.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `collectM6PerformanceProofs`, `hasCompleteM6PerformanceGateEvidence`, backend query-count/index proof tokens, and conditional `m6PerformanceGateVerified` instead of a hard-coded `m6PerformanceGateDeferred` result.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real-flow did not collect backend performance proofs and still hard-coded the performance gate as blocked.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` now reads the authoritative backend test/migration/mapper proof files, verifies daily-close submission count/page with zero detail, active-order single-query card reads, AC-D27 bulk PQC context preparation, AC-D32 generated-column/indexed mapper join, and AC-D32 count/page with zero detail; the gate remains BLOCKED if any action or proof is missing.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=64`, and `failedActions=0`; `m6PerformanceGateVerified=PASS` with all six `performanceProofs` true, and current blocker categories are `E2E_CLEANUP=1`, `E2E_CONCURRENCY=1`, `E2E_COVERAGE=62`.
- Decision: M6 performance gate is closed at gate level, but no PERF AC is individually ACCEPTED until its complete failure-path, permission/read-only, cleanup, and full 62-AC coverage gates are complete. No `git push` performed.

## M6 - AC-M18 Order-process Concurrency Proof Recognition

- BDD: AC-M18 over-target concurrent progress must be recognized by the M6 gate -> Given `MesTeamLeaderOrderProcessCompletionServiceTest` proves locked progress reads, duplicate backfill suppression, and over-target concurrent progress rejection When full real E2E builds the M6 concurrency gate Then AC-M18 must appear in `provedConcurrencyAcceptanceIds` and must not remain in the missing proof list.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now asserts the AC-M18 service test contains `shouldPreventOverTargetProgressWhenConcurrentAllocationAlreadyConsumedRemainingQuantity` and `verify(..., never()).backfillCompletedProcess`, and that real-flow recognizes Mockito `never()).backfillCompletedProcess` syntax.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: M6 AC-M18 concurrency proof did not recognize Mockito `verify(..., never()).backfillCompletedProcess` syntax, so canonical full real E2E still listed missing proof `AC-M18, AC-M19, AC-M23`.
- IMPLEMENTING: `role-requirement-matrix-real-flow.e2e.js` AC-M18 proof regex now matches `never()).backfillCompletedProcess`, aligning the gate with the already GREEN service-level test.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized runtime health and `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, backend `UP`, frontend HTTP `200`, 0 SOURCE / 0 ENV / 0 RUNTIME.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests, 0 failures, 0 errors, BUILD SUCCESS.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=64`, and `failedActions=0`; `m6ConcurrencyGateDeferred` now reports proved `AC-M04, AC-M07, AC-M16, AC-M17, AC-M18, AC-M20, AC-M21, AC-D29, AC-D34, AC-D37` and missing only `AC-M19, AC-M23`.
- Decision: AC-M18 is no longer a concurrency proof gap. M6 remains `in_progress` because active-order cleanup, AC-M19 batch-record backfill concurrency, AC-M23 release terminal concurrency, and 62 `E2E_COVERAGE` blockers remain open. No `git push` performed.
- GREEN: post-documentation structural verification -> PASS; `task-state.json` and latest `result.json` parse as UTF-8 JSON, latest result remains `BLOCKED` with 64 blockers and 0 failed actions, `provedConcurrencyAcceptanceIds` includes `AC-M18`, `missingConcurrencyAcceptanceIds=AC-M19,AC-M23`, `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` PASS, `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` PASS, and `git diff --check` PASS with LF/CRLF warnings only.

## M6 - AC-M23 Release Terminal Concurrency Proof and Documentation Sync

- BDD: AC-M23 release terminal transition must lock-reread the transaction before side effects -> Given a release transaction precheck is consumed by another terminal action When submit/approve/reject/withdraw continues from stale state Then the service must reread the transaction `FOR UPDATE`, reject the second terminal transition, and avoid creating a second terminal signature.
- TEST_ADDED: `MesProEdhrReleaseServiceImplTest#shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock` covers stale precheck consumption and verifies the terminal transition cannot produce a second signature after lock reread.
- RED: target release-service JUnit -> FAIL, expected reason: terminal release transitions did not lock-reread the release transaction before terminal side effects.
- IMPLEMENTING: `MesProEdhrReleaseTransactionMapper#selectByIdForUpdate(Long id)` added; `MesProEdhrReleaseServiceImpl#submit`, `approve`, `reject`, and `withdraw` now call `requireTransactionForUpdate(...)` before terminal status validation and side effects.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest#shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test, 0 failures/errors.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests, 0 failures/errors.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=63`, and `failedActions=0`; `m6ConcurrencyGateVerified=PASS`, `m6PerformanceGateVerified=PASS`, blocker categories are `E2E_CLEANUP=1` and `E2E_COVERAGE=62`.
- TASK_DOCS: synchronized `test-report.md` and `verification-report.md` so current-state summaries no longer report stale `64 blockers`, `m6ConcurrencyGateDeferred`, or missing `AC-M19/AC-M23`; historical rows remain marked as historical/superseded.
- GREEN: documentation sync structural verification -> PASS; `test-report.md`, `verification-report.md`, and `execution-log.md` read as UTF-8; `task-state.json` and latest `result.json` parse as UTF-8 JSON; latest `result.json` remains `BLOCKED` with 63 blockers, 0 failed actions, `E2E_CLEANUP=1`, `E2E_COVERAGE=62`, `m6ConcurrencyGateVerified=PASS`, and `m6PerformanceGateVerified=PASS`.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS after documentation sync.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS after documentation sync.
- GREEN: scoped `git diff --check` on synced task reports -> PASS with LF/CRLF warnings only and no whitespace errors.
- Decision: M6 remains `in_progress`; no currently observed failed action remains. The only current blockers are `activeOrderCleanupDeferred` and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - Current Real E2E Rerun and Release Traceability Blocker Sync

- BDD: Current-state M6 verification must distinguish data prerequisites from code failures -> Given the PQC signature pool can be extended with task-owned unused IDs and the release owner reads the formal traceability page When the authorized real flow is rerun Then PQC submit/detail/review/correction actions must pass without signature reuse, while missing target eDHR batch/release data must remain a structured release-traceability blocker.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` with extended task-owned PQC signature pool `98003520..98003550` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` with extended task-owned PQC signature pool `98003520..98003550` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=19`, `gateEvidence=2`, `blockers=64`, and `failedActions=0`.
- GREEN: current PQC chain recovered without relaxing backend uniqueness: `pqcFormalSubmissionCreated=PASS` with `submittedTaskId=33`, `eventId=83`, `signatureId=98003520`; `pqcLeaderReviewApprovedAndAggregated=PASS` with `reviewId=60`; `pqcLeaderRejectedCorrectionChain=PASS` with `eventId=84`, `reviewId=61`, `revisionSignatureId=98003522`; `pqcProcessInspectionAggregationReadOnly=PASS`.
- BLOCKED: current release traceability prerequisite is missing target data, not an observed code failure. Read-only DB checks show `mes_pro_edhr_batch_execution` has 0 rows and `mes_pro_edhr_release_transaction` has 0 rows for tenant 1 target `work_order_id=980008` / `work_order_code=RRM-20260801-PP-MO-001`, so the release owner page cannot prove AC-M22/AC-M23 for this task batch yet.
- Decision: M6 remains `in_progress`; current blockers are `activeOrderCleanupDeferred`, `edhrReleaseTraceabilityReadOnly`, and 62 `E2E_COVERAGE` items. No `git push` performed.
## M6 - Release Prefill Dialog Overlay Fix and Current Full Real E2E Sync

- BDD: Release preparation must reuse the real prefill dialog instead of clicking through it -> Given the batch execution page is opened with `prefillWorkOrderCode` and the page auto-opens the `打开或创建 eDHR 批次执行` dialog When the M6 real E2E prepares an eDHR release batch Then it must wait for and use that visible dialog, only clicking the underlying `打开/创建` button if no dialog appears, so Element Plus overlays cannot intercept the action.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now requires `prepareEdhrReleaseBatchExecutionViaRealPage` to wait for `autoOpenedDialog` before clicking the underlying open/create button.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: real E2E did not reuse the prefill auto-open dialog and could click the underlying `打开/创建` button while an Element Plus overlay was already visible.
- IMPLEMENTING: `prepareEdhrReleaseBatchExecutionViaRealPage` now locates the `打开或创建 eDHR 批次执行` dialog immediately after page load, waits up to 5000ms for the auto-opened prefill dialog, and only clicks the page-level `打开/创建` button when the dialog did not auto-open.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=20`, `gateEvidence=2`, `blockers=63`, and `failedActions=0`; blocker categories are `E2E_CLEANUP=1` and `E2E_COVERAGE=62`.
- GREEN: release preparation and read-only traceability recovered through real pages: `edhrReleasePreparedViaBatchExecutionPage=PASS` with `batchExecutionId=900000000926`, `batchExecutionCode=EDHRB-1785810846141`, `releaseTransactionId=104`, `releaseStatus=PRECHECK_FAILED`; `edhrReleaseTraceabilityReadOnly=PASS` with `checkItemCount=10`, `eventCount=1`, `eventTypes=PRECHECK`, and `mutationRequestCount=0`.
- Decision: M6 remains `in_progress`; no currently observed failed action remains. Current blockers are `activeOrderCleanupDeferred` and 62 `E2E_COVERAGE` items. No `git push` performed.

## M6 - Active-order Cleanup Await and Backend Remove Fix

- BDD: Final active-order cleanup must prove the real backend state is cleared -> Given the M6 real flow joined active order `12` for work order `980008` When the final cleanup action runs after all role actions Then the Playwright helper must await cleanup verification and the backend remove API must move the active order to `REMOVED` without a MyBatis optimistic-lock parameter error.
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL, expected reason: `runFinalActiveOrderCleanup(...)` returned `verifyActiveOrderCleanupTraceability(...)` without `await`, so cleanup completion could be reported without awaited verification.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS after adding `return await verifyActiveOrderCleanupTraceability(...)`.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- RED: authorized full real E2E after the await fix -> FAIL at final cleanup, expected reason: backend remove active-order API returned `500 系统异常`; backend log root cause was MyBatis-Plus `BindingException: Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found` on `mes_pro_process_pool_active_order` optimistic-lock `updateById`.
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest` covers successful remove and concurrent/stale zero-row remove, requiring the audit record to be inserted only after the conditional active-order update succeeds.
- RED: target Maven first failed because `MesProcessPoolActiveOrderMapper` did not expose the explicit remove method needed by the service-level guard.
- IMPLEMENTING: `MesProcessPoolActiveOrderMapper#removeActiveOrder(...)` now performs a conditional `ACTIVE -> REMOVED` update with `version = version + 1`; `MesTeamLeaderActiveOrderServiceImpl#removeActiveOrder(...)` uses that mapper method and throws `PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS` on zero updated rows before inserting audit.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 9 tests, 0 failures/errors.
- GREEN: adjacent backend regression `MesTeamLeaderActiveOrderServiceTest,MesFrontlinePqcContextServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderSubmissionReviewServiceTest,ProcessPoolTimelineFilterTest,MesProEdhrReleaseServiceImplTest` -> PASS, 57 tests, 0 failures/errors.
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS; runtime jar copied as `output\runtime\rrm-m6-48098\jars\backend-runtime-control-20260804-115138-rrm-m6-active-order-remove-fix.jar`, SHA256 `3FC8B09BCD605C554E4AB126572A16CFD288D2A9D3E745F1FF619FBE7AF40F85`.
- RUNTIME: stopped confirmed current-worktree backend PID `65300` running old `backend-runtime-control-20260804-110900-rrm-m6-cleanup-reactivation.jar`; started PID `42100` from `backend-runtime-control-20260804-115138-rrm-m6-active-order-remove-fix.jar` on backend `48098`; backend health `UP`; frontend `8098` HTTP `200`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=20`, `gateEvidence=2`, `blockers=62`, and `failedActions=0`; all blocker categories are `E2E_COVERAGE=62`.
- GREEN: `activeOrderCleanupCompleted=PASS` with `activeOrderId=12`, `workOrderId=980008`, `routeId=922119`, `routeVersionId=448`, `cleanupWindow=AFTER_ALL_ROLE_ACTIONS`, `removeResult=true`, and `refreshedActiveOrderCount=0`.
- Decision: Two clear defects are now closed at verification level: the Playwright missing-`await` cleanup lifecycle bug and the backend MyBatis-Plus optimistic-lock remove failure. M6 remains `in_progress` because 62 AC still require full real-page failure paths, permissions/read-only breadth, cleanup-readiness, and AC-level acceptance; no SOURCE/ENV/RUNTIME blockers and no failed action remain. No `git push` performed.

## M6 - Release Precheck Failure Reason Summary Fix

- BDD: Release precheck check-item persistence must fit formal field limits without hiding blockers -> Given an active order has many unconfirmed PQC inspection tasks When `/admin-api/mes/pro/edhr-release/precheck` inserts release check items Then the failure reason must preserve blocker meaning as total count plus representative examples and must not overflow `mes_pro_edhr_release_check_item.failure_reason`.
- RED: authorized full real E2E on independent worktree `8098/48098` -> BLOCKED with `edhrReleasePreparedViaBatchExecutionPage=BLOCKED`; backend log root cause was `MysqlDataTruncation: Data too long for column 'failure_reason'` while inserting release check items.
- TEST_ADDED: `MesOrderReleaseCompletenessServiceTest#shouldSummarizeLargePendingPqcTaskFailureReasonWithinReleaseFieldBudget` covers 120 pending PQC tasks and asserts `BLOCKER`, reason contains `共 120 个`, contains `示例`, and length is `<= 500`.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: existing code concatenated the full pending task id list into `failureReason`.
- IMPLEMENTING: `MesOrderReleaseCompletenessServiceImpl` now formats large source id collections with `summarizeIds(...)` for pending PQC tasks, quality/deviation ids, rework traces, scrap traces, and inventory ids; this is a formal summary, not a silent truncate or schema enlargement.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 22 tests, 0 failures/errors, BUILD SUCCESS.
- GREEN: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS; runtime jar copied as `output\runtime\rrm-m6-48098\jars\backend-runtime-control-20260804-124630-rrm-m6-release-precheck-summary-fix.jar`, SHA256 `93E29FD4BA64FBB9E1654D4B78A27E71D0F9327E1C69731CD81A79297D0F1637`.
- RUNTIME: stopped confirmed current-worktree backend PID `38068` running old `backend-runtime-control-20260804-115138-rrm-m6-active-order-remove-fix.jar`; started PID `70004` from `backend-runtime-control-20260804-124630-rrm-m6-release-precheck-summary-fix.jar` on backend `48098`; backend health `UP`; frontend `8098` HTTP `200`.
- GREEN: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` on independent worktree `8098/48098` -> PASS, 0 SOURCE / 0 ENV / 0 RUNTIME.
- E2E: authorized `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` on independent worktree `8098/48098` -> STRUCTURED_BLOCKED, exit 2, with `phaseEvidence=6`, `actionEvidence=20`, `gateEvidence=2`, `blockers=62`, and `failedActions=0`; all blocker categories are `E2E_COVERAGE=62`.
- GREEN: release preparation and read-only traceability remain recovered after the runtime reload: `edhrReleasePreparedViaBatchExecutionPage=PASS` with `releaseTransactionId=104`, `releaseStatus=PRECHECK_FAILED`, and `precheckSummary=放行前检查失败：3 个阻塞项，3 个失败项`; `edhrReleaseTraceabilityReadOnly=PASS` with `checkItemCount=10`, `eventCount=7`, and `mutationRequestCount=0`.
- Decision: The clear release precheck persistence defect is closed at target regression and real-page verification level. M6 remains `in_progress` because the remaining 62 blockers are AC-level `E2E_COVERAGE` breadth gaps, not currently observed SOURCE/ENV/RUNTIME blockers or failed actions. No `git push` performed.

## M6 - Active-order Transfer Trace Projection And No-extra-E2E Merge Scope

- BDD: active-order join must carry formal transfer trace source ids -> Given the team leader joins the pressure-pump active order from the real workbench and enters transfer document IDs in a visible field When the active order is inserted, reused, restored, or concurrently reused Then the backend must project formal `mes_wm_transfer` / line / detail rows into `mes_pro_process_pool_active_order_transfer_trace` with idempotency keys and source snapshots, and the page must expose the submitted IDs without direct SQL, mock, fallback, or default success.
- TEST_ADDED: `MesActiveOrderTransferTraceServiceTest` now covers projecting transfer header, line, and detail data into active-order transfer traces.
- TEST_ADDED: `MesTeamLeaderActiveOrderServiceTest` now covers forwarding `transferIds` for new, existing, concurrent-existing, and reactivated active-order paths.
- TEST_ADDED: `MesProcessPoolTeamLeaderControllerTest` now covers request VO -> BO propagation of `transferIds`.
- TEST_ADDED: `role-requirement-matrix-preflight-static.spec.cjs` now asserts the real E2E path parses `RRM_TRANSFER_IDS`, fills the visible `调拨单ID列表` form field, sends `transferIds?: number[]`, and validates positive integer parsing.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: missing constructor dependencies, missing `recordTransferTracesForActiveOrder(...)`, and missing active-order `transferIds` request plumbing.
- IMPLEMENTING: `MesActiveOrderTransferTraceServiceImpl` now reads formal transfer header/line/detail mappers, creates one trace per detail with active-order identity, transfer source fields, stock/batch/item/quantity, source status, occurred time, idempotency key, and source snapshot JSON; `MesTeamLeaderActiveOrderServiceImpl` invokes it for all active-order add/reuse/reactivation outcomes.
- IMPLEMENTING: `TeamLeaderWorkbenchPage.vue` now exposes `调拨单ID列表`, validates comma/space/Chinese-comma separated positive IDs, and sends `transferIds` through `addTeamLeaderActiveOrder`; the real-flow script fills the same visible field from `RRM_TRANSFER_IDS`.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 23 tests, 0 failures/errors, BUILD SUCCESS.
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS.
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS.
- GREEN: `pnpm --dir IntRuoyiFronted ts:check` -> PASS.
- SCOPE_CHANGE: User stated on 2026-08-04 that this round must not rerun full real E2E before local merge into `int_main`; retained canonical full real E2E evidence is the prior `STRUCTURED_BLOCKED` run with `phaseEvidence=6`, `actionEvidence=20`, `gateEvidence=2`, `blockers=62`, and `failedActions=0`.
- Decision: The active-order transfer projection slice is GREEN at target/backend/static/type level and can be locally committed/merged without another full real E2E per user scope. M6 remains `in_progress`; 62 `E2E_COVERAGE` blockers remain open and no `git push` is authorized.
