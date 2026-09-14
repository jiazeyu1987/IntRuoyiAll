# Execution Log

- Task ID: task-6586818a22-20260814T121328
- Workspace: E:\IntRuoyi
- User intent: 用户同意“导入只升级工序节点，旧工艺路线绑定关系应保留”的思路，要求基于当前代码分析并写成 PRD、开发计划、测试计划。

## BDD Scenarios

BDD: 未勾选工艺流程不重建路线 -> Given 用户上传批记录表单 Word 且未勾选“工艺流程” / When 用户提交导入 / Then 系统只导入或升版批记录表单，不按 Word 工序顺序重建工序节点、普通流程边或 START/END 边界；如因批记录表单绑定生成候选，仅更新绑定快照且 flowGraph 沿用原 ACTIVE。

BDD: 无现有路线时新建路线 -> Given 用户勾选“工艺流程”且所选 DCC 项目代码没有现有工艺路线 / When Word 工序识别成功 / Then 系统新建工艺路线、工序、流程关系和 DCC 项目代码绑定，并生成初始 ACTIVE 版本。

BDD: 已有路线时生成候选版本 -> Given 用户勾选“工艺流程”且所选 DCC 项目代码已有 ACTIVE 工艺路线 / When 用户确认升版导入 / Then 系统生成或更新 DRAFT 路线候选版本，当前 ACTIVE 路线在发布前不变。

BDD: 升版保留旧工序配置 -> Given 旧路线每个工序已有正式批记录表单绑定、表单槽位 formBindings 或工序开始配置 / When Word 导入只升级工序节点和顺序 / Then 可唯一映射的旧配置迁移到候选新工序节点，工序结束不产生绑定关系。

BDD: 映射失败必须阻断 -> Given 旧配置所在工序在 Word 中缺失、重复或无法唯一映射 / When 导入尝试生成候选版本 / Then 系统 fail fast 并提示具体原因，不静默丢失绑定关系。

## Evidence And Analysis

- 读取技能：spec-driven-delivery、product-requirements-docs、bdd-tdd-acceptance-planner。
- 读取项目规则：docs/task-closeout-rules.md、docs/powershell-encoding.md、docs/experience-index.md 命中项。
- 读取当前代码：
  - 前端 Word 导入页已处理 DCC 项目、预检、候选版本提示和提交参数。
  - 前端 API 已支持 dccProjectCodeId、rebuildBatchRecord、routeUpgradeConfirmed、expectedRouteId、expectedRouteVersionId、expectedRouteCandidateVersionId。
  - 后端导入服务已有 DCC 和路线升版治理字段。
  - 后端已有路线分支已进入 createOrUpdateCandidateRouteVersion，不直接覆盖 ACTIVE。
  - 当前 loadPreservedData 只保留旧工序基础属性和流程边，未覆盖逐工序正式批记录表单绑定、formBindings 和工序开始配置。
  - 候选发布投影以 routeSnapshotJson.configSnapshots 为准，因此候选快照阶段必须补齐旧绑定关系。

## Document Outputs

- prd.md：已写入完整 PRD，覆盖目标、范围、非目标、前置条件、影响面、阶段计划、验收标准和阻塞条件。
- development-plan.md：已写入开发计划，覆盖现状分析、设计原则、里程碑、实现顺序和后续变更文件。
- test-plan.md：已写入测试计划，覆盖环境、账号数据、命令、测试用例、覆盖矩阵和 pass/fail 标准。

## Verifier Correction Pass

- 2026-08-14：根据独立 verifier 的 PASS_WITH_NOTES 结论修正文档。
- 修正 1：将“未勾选即禁止候选版本”的绝对口径收窄为“未勾选工艺流程不得按 Word 重建工序节点、流程边或 START/END 边界”；若仅因批记录表单绑定生成候选，flowGraph 必须沿用原 ACTIVE。
- 修正 2：明确后续改造目标是 createOrUpdateCandidateRouteVersion 候选生成快照主链路，loadPreservedData 只是现有保留逻辑的读取入口之一。
- 修正 3：显式点名需要迁移的配置快照字段：batchUseConfigs.batchRecordReports、batchUseConfigs.formBindings、routeStartProductionLeaders、batchRecordAttachmentOwners。
- 修正 4：测试计划新增 T13，覆盖“只导入批记录表单绑定时不重排 flowGraph”的负向断言。
- 经验沉淀检查：已读取 project-experience-consolidation 技能并检索现有长期门禁；docs/backend-development.md 与 docs/frontend-development.md 已覆盖 Word 工艺路线候选、DCC 项目绑定、候选快照和三类配置不可替代规则，本次不新增长期经验文档。

## Verification

- GREEN: python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_artifacts.py --cwd E:\IntRuoyi --task-id task-6586818a22-20260814T121328 -> PASS，PRD 和测试计划结构有效。
- GREEN: python C:\Users\BJB110\.codex\skills\spec-driven-delivery\scripts\validate_test_report.py --cwd E:\IntRuoyi --task-id task-6586818a22-20260814T121328 --expected-outcome pending -> PASS，test-report 明确为文档审查完成、功能测试待执行。
- GREEN: python -X utf8 readback -> PASS，8 个任务文档均可按 UTF-8 读取。
- GREEN: git diff --check -- doc/tasks/task-6586818a22-20260814T121328 -> PASS。
- GREEN: rg 旧绝对口径扫描 -> PASS，未检出“未勾选即禁止候选版本”的残留要求。
- INFO: 本任务为文档交付，未运行生产代码 Maven/pnpm/Playwright 测试；这些命令已写入 test-plan.md 作为后续实现验证门禁。

## Development Resume

- 2026-08-14：用户要求继续完成文档内的开发验证工作，任务由文档交付阶段切换为实现阶段。
- 当前阶段：P1 明确导入入口和用户确认边界。
- 工作区基线：相关前端页面和静态合同已有未提交改动；后续只基于当前内容增量修改，不回滚并行任务改动。
- 子 Agent：重启前两次执行请求均因上游 502 失败，未形成代码改动或验证结论；恢复后将重新调度 P1 executor 和独立 tester。

## P1 Execution Evidence

- 2026-08-14：完成 P1“明确导入入口和用户确认边界”的执行核对；本阶段只补充测试覆盖，没有修改后端生产代码，也没有回滚重启前已有的前端改动。
- BDD: 仅导入批记录表单绑定时沿用 ACTIVE 流程图 -> Given 已有 ACTIVE 工艺路线且用户未勾选“工艺流程”、只导入批记录表单 / When 系统生成批记录表单绑定候选 / Then 候选的 flowGraph.nodes、edges、boundaryEdges 与 ACTIVE 完全一致，只更新逐工序正式批记录表单绑定快照。
- BDD: 勾选工艺流程才进入路线重建确认 -> Given 已有 ACTIVE 工艺路线 / When 用户勾选“工艺流程”并提交 Word 导入 / Then 前端才按 Word 工序顺序表达路线候选重建，并冻结 expectedRouteId、expectedRouteVersionId、expectedRouteCandidateVersionId；锁定候选状态阻断选择和提交。
- RED: 未产生业务行为 RED -> NOT APPLICABLE；恢复执行时，前端分流和后端绑定候选沿用 ACTIVE 快照的生产行为已存在。新增数据库级合同测试首次运行即通过，未通过篡改断言或回滚现有实现伪造失败。
- 新增测试：MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph，覆盖 ACTIVE 的 nodes、edges、boundaryEdges 原样进入 DRAFT 绑定候选，并验证候选只写入两个工序的正式批记录报表绑定。
- BLOCKER: `mvn --% -pl yudao-module-mes -am -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph test` -> FAIL，在上游 yudao-module-dcc 的 testCompile 阶段被当前工作区大量缺失 DCC 测试依赖类阻断，尚未进入 MES 测试；该失败与本次 P1 测试源码无关，未记作 RED。
- GREEN: `mvn --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph test` -> PASS；Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js` -> PASS，验证工艺流程重建与批记录表单绑定候选分流、候选锁定和用户提示。
- GREEN: `node tests/e2e/batch-record-word-import-production-upgrade-dedupe-static.spec.js` -> PASS，验证确认参数只由明确分流结果生成。
- GREEN: `node tests/e2e/batch-record-word-import-dcc-identity-static.spec.cjs` -> PASS，3/3，验证 DCC 项目记录 ID 和正式路线绑定身份链路。
- BLOCKER: `mvn --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenRouteCandidatePendingApproval_blocksBeforeCreatingNextVersion+recognizeUploadedRoute_whenRouteCandidateReadyToPublish_blocksBeforeCreatingNextVersion test` -> ERROR；共享工作区并发 Maven 编译期间，Spring 启动时缺少刚被并发构建影响的 `MesProDccProjectGovernanceServiceImpl.class`，2 个测试均未进入业务断言。该结果按验证环境竞争记录，不能作为候选锁定行为失败或通过证据；独立 tester 需在无并发 Maven 写同一 target 目录时重跑。
- P1 验收映射：P1-AC1 由新增后端数据库测试与 route-candidate 静态合同覆盖；P1-AC2 由候选版本提示、冻结路线/版本 ID 静态合同覆盖；P1-AC3 由 PENDING_APPROVAL、READY_TO_PUBLISH 禁用/阻断静态合同及现有后端候选状态测试覆盖。

## P1 Independent Verification

- GREEN: 独立 tester 首轮仅读取 prd.md 与 test-plan.md，随后核对当前代码和测试；P1-AC1、P1-AC2、P1-AC3 全部通过。
- GREEN: 独立 tester 运行 3 个前端静态合同、pnpm ts:check 和后端定向 DB 测试，均通过；详细命令和结果见 test-report.md。
- INFO: 本阶段不执行写入型真实浏览器 E2E，因为当前任务没有已确认的测试租户、账号和任务自有 Word fixture；未使用 mock、API-only 或直接 SQL 替代。
- STATE: P1 -> completed；P2 -> in_progress。

## P2 Execution Evidence

- BDD: 新 DCC 项目创建完整路线 -> Given 所选启用 DCC 项目代码没有正式路线 / When 用户勾选工艺流程并导入 Word / Then 系统创建路线、正式工序、START/END 边界、DCC 正式绑定和初始 ACTIVE V1。
- BDD: 已有路线只更新候选 -> Given 所选 DCC 项目已有 ACTIVE 路线和同源 DRAFT / When 用户携带预检冻结 ID 确认重建 / Then 更新原 DRAFT，不创建 V3，ACTIVE 版本保持不变。
- BDD: DCC 或冻结 ID 缺失/漂移阻断 -> Given dccProjectCodeId 缺失、已有路线的 expectedRouteId/expectedRouteVersionId 缺失，或 expectedRouteCandidateVersionId 已漂移 / When 提交导入 / Then 在解析 Word 和写入路线前 fail fast。
- BDD: 锁定候选阻断 -> Given 候选为 PENDING_APPROVAL 或 READY_TO_PUBLISH / When 提交导入 / Then 不更新候选、不新建下一版本。
- RED: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordRouteGovernanceContractTest,MesProBatchRecordRouteCandidateGovernanceTest test` -> FAIL；旧静态合同仍绑定已移除的 `selectListByItemIds(dccProductItemIds)` 方法名，未验证当前“正式 DCC 绑定优先、唯一 DCC 物料路线绑定补建”的业务规则。
- GREEN: 将过期合同改为验证 `selectCurrentListByDccProjectCodeId` 正式绑定优先、`resolveRoutesByDccProjectBinding` 与受控 `resolveRoutesByDccProjectProductBinding` 分支；未修改生产代码。
- GREEN: 后端 P2 定向 DB 测试 -> PASS；Tests run: 8, Failures: 0, Errors: 0, Skipped: 0。覆盖新路线完整创建、DCC 缺失、冻结路线 ID 缺失、候选 ID 漂移、DRAFT 原位更新、PENDING_APPROVAL/READY_TO_PUBLISH 阻断和重复路线阻断。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordRouteGovernanceContractTest,MesProBatchRecordRouteCandidateGovernanceTest test` -> PASS；Tests run: 5, Failures: 0, Errors: 0, Skipped: 0。
- INFO: P2 生产治理逻辑在恢复时已存在；本阶段补齐 DB 负向证据、新路线完整性断言，并修复过期测试合同，没有引入 fallback 或直接覆盖 ACTIVE 的代码。

## P3 Execution Evidence

- BDD: 升版候选按 Word 顺序迁移旧工序配置 -> Given ACTIVE 路线存在重复 processId 的多个工序实例，且每个实例分别配置正式 batchRecordReports 和 formBindings / When 用户勾选“工艺流程”并按新的 Word 工序顺序生成 DRAFT 候选 / Then 候选节点严格按 Word 顺序生成，并按 processId 加同工序出现次序唯一复用旧 routeProcessId；正式批记录表单和表单槽位分别迁移，互不替代。
- BDD: 升版候选保留工序开始配置且不创建结束绑定 -> Given ACTIVE 快照存在 routeStartProductionLeaders、batchRecordAttachmentOwners 和 START/END 边界 / When 重建 DRAFT 候选 / Then 两类 START 配置原样保留，候选只生成 START/END 流程边界，不生成 END 业务绑定。
- BDD: 已配置旧工序无法映射时阻断 -> Given ACTIVE 路线某个带 batchRecordReports 或 formBindings 的旧工序实例在 Word 中缺失 / When 生成升版候选 / Then 后端 fail fast 并提示具体 processId、routeProcessId 和出现次序，ACTIVE 版本与正式路线均不改变。
- RED: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateRouteOnlyForUploadedWord_whenConfiguredOccurrenceIsMissing_failsFast test` -> FAIL；Tests run: 2, Failures: 2。重复 processId 的第二个 Word 工序错误复用第一个旧 routeProcessId（期望旧第二实例，实际仍为旧第一实例）；带正式 batchRecordReports 的旧工序从 Word 缺失时未抛出 ServiceException，证明当前候选生成会错映射并静默丢配置。
- RED: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingStartConfigurations test` -> FAIL；Tests run: 1, Errors: 1。当前完整 ACTIVE 快照只沿用 batchRecordAttachmentOwners，routeStartProductionLeaders 读取结果为 null，证明候选迁移的正式源快照缺少该 START 配置。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateRouteOnlyForUploadedWord_whenConfiguredOccurrenceIsMissing_failsFast,MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingStartConfigurations test` -> PASS；Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKER: P3 相邻回归命令在 testCompile 阶段被任务范围外的并行改动阻断：`MesProBatchRecordCellLinkServiceImplTest.java:130` 调用了当前 mapper 不存在的 `selectListByBatchRecordReportIds(Object)`；本次未修改或回滚该并行文件，失败发生在 P3 测试执行前，不属于 P3 行为回归。
- RESOLVED: 上述并行 testCompile 阻断随后由对应并行改动补齐 mapper 方法；P3 未修改该范围外文件，后续 P3 定向命令已重新完成编译并通过。
- 实现：重建候选先读取 ACTIVE 完整快照并严格校验 flowGraph.nodes、batchUseConfigs、batchRecordReports 与 formBindings 的独立正式来源；候选节点按 Word 顺序生成，旧节点按 processId 加 occurrence 映射并复用冻结 routeProcessId，新工序使用 clientRouteProcessId。
- 实现：映射到旧节点时复制完整 batchUseConfig，分别保留 nested batchRecordReports 和 formBindings；正式批记录绑定重写 routeProcessId、permissionScopeId 与两类 snapshotHash，表单槽位只重写节点身份。已有旧配置时不使用 Word 新报表、formBindings 或默认 MAIN 覆盖正式批记录来源；无旧配置的新工序才使用本次 Word 正式报表建立新绑定。
- 实现：ACTIVE 完整快照补齐 routeStartProductionLeaders，并与 batchRecordAttachmentOwners 一起原样迁移到 DRAFT；候选仅生成 START/END boundaryEdges，未新增任何 END 业务绑定字段或配置。
- 实现：任何 ACTIVE batchUseConfig 所属旧工序未被 Word 的 processId+occurrence 唯一映射时，在候选写入前以 processId、routeProcessId、occurrence 上下文 fail fast；候选发布前不更新 ACTIVE 版本和正式 routeProcess 行。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateRouteOnlyForUploadedWord_whenConfiguredOccurrenceIsMissing_failsFast,MesProRouteServiceImplTest#buildCurrentRouteSnapshotJson_shouldPreserveExistingStartConfigurations test` -> PASS；最终复跑 Tests run: 3, Failures: 0, Errors: 0, Skipped: 0。覆盖 Word 重排、重复 processId 出现次序、正式批记录保留、只有 formBindings 时不补正式批记录、两类 START 配置、无 END 绑定、无 Fastjson `$ref`、ACTIVE 不变和缺失映射失败。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenOnlyGovernedRouteIsDisabled_restoresRouteBeforeCreatingDraftCandidate+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3 test` -> PASS；Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。相邻回归确认禁用路线恢复后仍只写候选，以及同源 DRAFT 原位更新不创建 V3。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteCandidatePendingApproval_blocksBeforeCreatingNextVersion+recognizeUploadedRoute_whenRouteCandidateReadyToPublish_blocksBeforeCreatingNextVersion+recognizeUploadedRoute_whenCandidateIdChangedAfterPreflight_failsBeforeParsingWord test` -> PASS；Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。回归确认未勾选工艺流程仍沿用 ACTIVE flowGraph，锁定候选和候选 ID 漂移继续阻断。
- INFO: 扩展的 11 条相邻回归曾得到 Tests run: 11, Errors: 4；其中上述 2 条空 ACTIVE 快照夹具已按正式前置修正并独立 GREEN，另 2 条旧用例 `recognizeUploadedRoute_whenOnlyRouteRebuildUsesExistingRouteProductWithoutBatchRecordVersion_allowsUpgrade`、`recognizeUploadedRoute_whenUpgradingRoute_keepsStableProcessConnectionInfoOnActiveRoute` 因未携带当前生产代码已必填的 DCC 项目 ID 在进入 P3 候选逻辑前失败，属于既有 P2/DCC 测试夹具缺口，本阶段未放宽生产校验或扩大范围修复。
- P3 验收映射：P3-AC1 由 Word 顺序、正式 processId、routeProcessId/clientRouteProcessId 断言覆盖；P3-AC2 由 nested batchRecordReports 保留和身份/hash 重写覆盖；P3-AC3 由 formBindings 独立保留且 form-only 工序的 batchRecordReports 仍为空覆盖；P3-AC4 由 routeStartProductionLeaders、batchRecordAttachmentOwners 原样迁移覆盖；P3-AC5 由 processId+occurrence 缺失映射 fail fast 覆盖；附加验收“无 END 绑定”和“发布前 ACTIVE 不变”分别由 boundary/config key 与 ACTIVE snapshot/routeProcess DB 断言覆盖。

## P4 Execution Evidence

- BDD: 发布 Word 候选后保留三类配置 -> Given READY_TO_PUBLISH 的 Word 路线候选包含按 Word 顺序冻结的节点、batchRecordReports、formBindings、routeStartProductionLeaders 和 batchRecordAttachmentOwners / When 发布投影执行并将候选切换为 ACTIVE / Then 正式路线按候选节点和关系投影，正式批记录表单与表单槽位分别落到对应新工序，候选快照中的工序开始配置继续作为新 ACTIVE 的运行态来源。
- BDD: 发布前继续读取原 ACTIVE -> Given Word 路线候选仍为 DRAFT 或 READY_TO_PUBLISH / When 批次执行或路线配置查询当前生效版本 / Then 当前 ACTIVE 版本和正式路线数据保持不变，候选配置不提前进入运行态。
- BDD: Word 候选必要配置快照缺失时阻断发布 -> Given EDHR_WORD_IMPORT 候选缺少 batchUseConfigs 的独立 batchRecordReports/formBindings，或缺少 routeStartProductionLeaders、batchRecordAttachmentOwners 明确数组 / When 尝试发布 / Then 在删除或重建正式路线数据前 fail fast，不用空配置、旧 ACTIVE 或默认值冒充成功。
- BDD: Word 候选生成时显式冻结空 START 配置 -> Given ACTIVE 完整快照没有 routeStartProductionLeaders 或 batchRecordAttachmentOwners 配置 / When 生成 EDHR_WORD_IMPORT 绑定候选或路线重建候选 / Then 候选在生成阶段将缺少的配置键写成显式空数组，发布阶段不从 ACTIVE 或默认值回填。
- RED: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteVersionPublishProjectionServiceImplTest#projectCandidate_whenEdhrBindingArraysAreIncomplete_rejectsBeforeLiveMutation+projectCandidate_whenEdhrStartArraysAreIncomplete_rejectsBeforeLiveMutation,MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3 test` -> FAIL；Tests run: 4, Failures: 2, Errors: 2。发布投影对缺少 formBindings 或 routeStartProductionLeaders 的 EDHR_WORD_IMPORT 候选均未抛出异常；绑定候选和路线重建候选在 ACTIVE 源快照缺少 START 配置键时均生成 null，而不是显式空数组。失败原因与 P4 预期缺口完全一致。
- INTERMEDIATE: 同一 GREEN 命令首次修复后运行 -> FAIL；业务断言 4/4 已通过，但 2 个发布前阻断测试仍保留 RED 阶段为穿透旧实现所需的 routeProcessMapper stubbing，Mockito 严格模式报告 2 个 UnnecessaryStubbingException。删除已不再需要的测试桩后原命令复跑，不改生产逻辑。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteVersionPublishProjectionServiceImplTest#projectCandidate_whenEdhrBindingArraysAreIncomplete_rejectsBeforeLiveMutation+projectCandidate_whenEdhrStartArraysAreIncomplete_rejectsBeforeLiveMutation,MesProBatchRecordReportServiceImplDbTest#generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3 test` -> PASS；Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。Surefire 原始报告时间为 2026-08-14 23:00:38/23:00:39，两份测试集各 2/2 PASS。
- CORRECTION: P3 记录中“正式批记录绑定重写 routeProcessId、permissionScopeId 与两类 snapshotHash”表述不准确，以本条为准：旧正式批记录绑定迁移到候选时仅更新其 routeProcessId 节点引用；permissionScopeId、recordCategorySnapshotHash、slotConfigSnapshotHash 均属于已经冻结的正式绑定关系，必须原样保留。P3 的数据库断言已分别验证旧值 701001/701002 和两组冻结 hash 未变化。
- BDD: Word 新增工序发布时建立正式权限范围 -> Given 候选新增工序仅有 clientRouteProcessId 且批记录绑定仍携带临时候选引用 / When 发布投影先创建新的正式 routeProcess / Then 系统为该正式 routeProcess 创建批记录权限范围并按正式 routeProcessId 生成两类冻结 hash，严禁把 clientRouteProcessId 写入 permissionScopeId。
- RED: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteVersionPublishProjectionServiceImplTest#projectCandidate_whenNewEdhrProcessUsesClientReference_createsFormalPermissionScope test` -> FAIL；Tests run: 1, Failures: 1, Errors: 0, Skipped: 0；期望正式 permissionScopeId=7301，实际为 clientRouteProcessId=-1，证明原发布投影会错误发布临时候选身份。
- GREEN: 同一新增工序发布测试 -> PASS；Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。发布投影现在只对 clientRouteProcessId 新工序创建正式权限范围，objectId 使用新正式 routeProcessId；旧 routeProcessId 工序继续原样保留既有 permissionScopeId 和两类冻结 hash。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3,MesProRouteVersionPublishProjectionServiceImplTest#projectCandidate_shouldRestoreMainBatchRecordAndKeepLossFormInIndependentSlot+projectCandidate_whenNewEdhrProcessUsesClientReference_createsFormalPermissionScope+projectCandidate_whenEdhrBindingArraysAreIncomplete_rejectsBeforeLiveMutation+projectCandidate_whenEdhrStartArraysAreIncomplete_rejectsBeforeLiveMutation test` -> PASS；Tests run: 7, Failures: 0, Errors: 0, Skipped: 0。覆盖旧绑定权限范围/hash 原样保留、正式批记录与 formBindings 独立投影、START 配置保留、缺快照发布前阻断、发布前 ACTIVE 不变、同源 DRAFT 原位更新和新增工序正式权限范围创建。
- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProRouteVersionPublishProjectionServiceImplTest test` -> PASS；Tests run: 9, Failures: 0, Errors: 0, Skipped: 0。发布投影测试类全部回归通过。
- P4 验收映射：P4-AC1 由正式批记录、表单槽位、START 配置、旧绑定身份保留和新工序正式权限建立的正向发布测试覆盖；P4-AC2 由绑定候选沿用 ACTIVE flowGraph 和同源 DRAFT 原位更新数据库测试覆盖；P4-AC3 由缺 formBindings/START 显式数组且写运行态前零交互的失败测试覆盖。
- INFO: 本执行阶段未运行写入型真实浏览器 E2E；任务仍缺已确认的测试租户、账号和任务自有 Word fixture，未使用 mock、API-only 或直接 SQL 替代真实页面验收。

## Final Regression After Restart (2026-08-15)

- GREEN: `mvn.cmd --% -pl yudao-module-mes -DskipTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=MesProBatchRecordReportServiceImplDbTest#generateForUploadedWord_whenRebuildingExistingRoute_preservesMappedProcessConfigurations+generateBatchRecordBindingCandidate_whenFlowNotSelected_preservesActiveFlowGraph+recognizeUploadedRoute_whenRouteDraftCandidateExists_updatesDraftWithoutCreatingV3,MesProRouteVersionPublishProjectionServiceImplTest test` -> PASS；Tests run: 12, Failures: 0, Errors: 0, Skipped: 0。覆盖旧工序正式绑定保留、未勾选工艺流程不重建流程图、同源候选原位更新、发布正向投影、缺快照发布前阻断以及新增工序正式权限范围建立。
- GREEN: `node tests/e2e/batch-record-word-import-route-candidate-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-word-import-production-upgrade-dedupe-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/batch-record-word-import-dcc-identity-static.spec.cjs` -> PASS；3/3。
- GREEN: `pnpm ts:check` -> PASS。
- INFO: P4 子 Agent 已完成生产实现与回归，主 Agent 复核差异并独立复跑上述 12 条后端用例和前端合同。真实浏览器写入 E2E 仍缺已确认的测试租户、账号及任务自有 Word fixture，因此按项目规则保留为环境前置阻塞，不以 mock、API-only 或直接 SQL 替代。
- EXPERIENCE: `project-experience-consolidation` -> 已合并到现有 `docs/backend-development.md#Word-升版候选必须保留正式批记录绑定身份`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- CLOSEOUT PREVIEW: `task_closeout.py --task-id task-6586818a22-20260814T121328 --mode preview` -> PASS；8 份正式任务文档全部列入 keep，delete/blocked/warnings 均为空。
- CLOSEOUT APPLY: `task_closeout.py --task-id task-6586818a22-20260814T121328 --mode apply` -> PASS；主工作区 `int_main`，非 linked worktree；deleted_paths 为空，未执行 Git 提交、合并、推送或 worktree 删除。
- STATE: `ready_for_closeout` -> `completed`；收尾完成。
- GREEN: `validate_artifacts.py` -> PASS；PRD、测试计划和 task-state 的正式验收编号一致。
- GREEN: `validate_test_report.py --expected-outcome passed` -> PASS；T1-T13 均有命令、环境和证据引用，覆盖全部 15 个 PRD 验收项。
- GREEN: `check_plan_completion.py --apply` -> PASS；`complete: true`。
- GREEN: 任务目录 UTF-8 严格解码、task-state JSON 解析、相关生产/测试/长期经验文档 `git diff --check`、任务文档尾随空白扫描 -> PASS。
