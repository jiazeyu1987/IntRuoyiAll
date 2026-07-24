# 执行日志

## BDD

- BDD: 一期重名导入生成新版本快照 -> Given 批记录已有 V1.0 当前版本 When 导入同名主 Word Then 系统生成 V2.0 待审批快照，不修改 V1.0。
- BDD: 一期审批通过切换未来业务 -> Given V2.0 待审批 When 授权审批人通过 Then `definition.current_version_id` 原子切换到 V2.0，新业务使用 V2.0。
- BDD: 一期历史业务绑定旧版 -> Given V1.0 执行任务已创建 When V2.0 生效 Then 历史任务仍通过 `batch_record_version_id` 打开 V1.0。
- BDD: 一期关键规则迁移失败阻断 -> Given V1.0 存在关键单元格规则 When V2.0 无法稳定匹配 Then 生成 `BLOCKER` 并禁止提交审批。
- BDD: 二期授权确认 -> Given 迁移项为 `CONFIRM_REQUIRED` When 有权限用户确认 Then 可以提交审批且保留确认审计。
- BDD: 三期受控回滚 -> Given V2.0 当前生效且 V1.0 曾审批通过 When 回滚审批通过 Then 当前版本指针切回 V1.0，历史引用不被改写。
- BDD: 三期附加表单槽位版本化 -> Given 批记录版本存在生产、设备、质量填写规则 When 查询版本治理影响面 Then 返回槽位类型、负责人角色和槽位配置快照哈希，证明新导入不要求重新设计规则。
- BDD: 三期版本影响面分析 -> Given 某版本被执行任务、路线绑定和单元格规则引用 When 查询影响面 Then 返回执行数、任务数、路线绑定数、规则数和风险等级。
- BDD: 三期批量历史治理巡检 -> Given 版本迁移项存在 `BLOCKER` 或 `CONFIRM_REQUIRED` When 查询巡检 Then 返回 `BLOCKED`、问题数量、摘要和下一步动作。
- BDD: 三期治理看板和运营指标 -> Given 定义下存在多版本、待审批和回滚申请 When 查询 summary/metrics Then 返回当前版本、历史执行、待审批、阻断巡检和运营指标。
- BDD: 三期受控回滚审批入口 -> Given 目标旧版本存在且保留审批关系 When 发起回滚 Then 只创建 `BATCH_RECORD_VERSION / ROLLBACK` 统一变更申请，不直接切换当前版本指针。

## RED / GREEN 计划

- RED: 版本定义和版本快照测试 -> 旧代码缺少定义表、版本表和当前指针，应失败。
- RED: P0 数据契约测试 -> 旧代码缺少执行任务、路线用途绑定和执行快照 `batch_record_version_id`，应失败。
- RED: 审批幂等测试 -> 旧代码缺少审批事件唯一键，重复回调应失败。
- RED: 迁移证据测试 -> 旧代码缺少 `source_logical_key / target_logical_key / match_confidence / match_evidence_json`，应失败。
- GREEN: 完成阶段一后端、前端、E2E、DB 断言和并发测试。
- GREEN: 完成阶段二结构化 diff、授权确认、草稿重传和 E2E。
- GREEN: 完成阶段三附加槽位、回滚、影响面、治理看板和 E2E。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseThreeGovernanceContractTest" test` -> FAIL, 阶段三治理 controller/service/VO、槽位治理字段和统一变更 `BATCH_RECORD_VERSION` 对象类型缺失。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseThreeGovernanceContractTest" test` -> PASS, Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。

## 经验预检

- INFO: experience-index -> matched `docs/powershell-memory.md`, `docs/worktree-memory.md`, `docs/login-access.md`, `docs/experience/batch-record-form-recognition.md`。
- GREEN: worktree-preflight -> PASS，后端 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\ruoyi-vue-pro`，前端 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version\yudao-ui-admin-vue3`，分支 `codex/edhr_version`，端口 `8096/48096` 当前空闲。
- BLOCKER: experience-preflight -> 真实 E2E 尚未启动；启动前必须完成依赖、后端健康检查、前端代理、DB/Redis 目标和官方登录预检。

## 已完成工作

- 创建前后端 `edhr_version` worktree。
- 创建后端任务文档和执行日志。
- 阶段二子 agent 完成代码结构调研：确认当前 Word 导入入口、eDHR 后端包结构、前端页面/API 位置和既有真实 E2E 资产。
- 新增阶段二迁移体验增强设计文档，明确结构化 diff、`CONFIRM_REQUIRED`、草稿重传、迁移证据展示 API 与真实 E2E 路径。
- 新增阶段二后端待启用契约测试清单，显式标记 `TODO(PHASE2_WAIT_PHASE1)`，等待阶段一版本模型落地后转为真实测试，未创建任何替代接口或 mock 成功路径。
- 阶段三新增最小治理 API：summary、impact、inspection、metrics、rollback/request。
- 阶段三扩展现有版本与迁移契约，补齐附加表单槽位版本化字段 `form_slot_type`、`owner_role_key`、`slot_config_snapshot_hash`。
- 阶段三受控回滚仅创建统一变更审批申请，未直接切换 `current_version_id`，保留旧版本引用和审批关系。
- 阶段三治理指标复用现有版本、执行、路线、规则、统一变更表，不新增独立巡检任务调度，避免过度设计。

## 验证证据

- 待执行：阶段一 RED 测试。
- 待执行：阶段一 GREEN 测试。
- GREEN: phase3-backend-contract -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseThreeGovernanceContractTest" test` -> PASS, Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKER: phase3-real-e2e -> 本子任务未启动本地 `48096/8096` 运行态，且缺少真实版本 ID、回滚目标版本 ID 与签核证据哈希；不能声明测试租户写入和 admin 只读复验通过。

## 剩余阻塞

- 待确认当前本机数据库、Redis、文件服务配置是否可用于 `48096/8096` 独立运行态。
- 待确认测试租户中可复用或可创建的 eDHR 批记录 Word 样本。
- 待确认 `芋道源码/admin` 最终只读验证账号当前可登录且具备目标页面只读权限。
- BLOCKER: phase2-wait-phase1 -> 阶段一 `MesProBatchRecordDefinitionDO`、`MesProBatchRecordVersionDO`、`MesProBatchRecordVersionMigrationItemDO`、`MesProBatchRecordApprovalEventDO` 等版本契约类尚未在本 worktree 出现；阶段二不得自行造替代模型。
- TODO(PHASE2_WAIT_PHASE1): 阶段一版本契约合入前，阶段二只保留迁移体验增强设计和待启用测试清单。
- BLOCKER: phase2-real-e2e -> 本子任务未启动 `48096/8096` 本地运行态，阶段二真实 E2E 只能准备路径，不能声明通过。
- BLOCKER: phase3-real-e2e -> 需要主控提供或创建本地测试租户真实批记录定义、当前版本、可回滚旧版本、审批签核证据哈希，并启动本地前后端运行态后执行 Playwright；本子 agent 未用 mock 成功替代。
- REVIEW: phase3-rollback-scope -> 当前实现是“发起回滚审批”而非“审批通过后切换版本指针”；是否把审批完成后的切换动作纳入三期，需主控结合一期/二期审批流统一判定。
- REVIEW: phase3-json-snapshot -> 回滚 diff 快照当前为最小字符串 JSON，主控可评估是否改成统一 JSON 序列化工具以增强转义鲁棒性。

## 当前状态

- blocked: 阶段三后端契约测试已通过；真实 E2E 因本地运行态和真实测试数据参数缺失阻塞。

## 阶段一 RED / GREEN 证据（2026-07-08）

- BDD: 导入后提交升版审批 -> Given 用户在顶部“导入 Word”导入同名主批记录 When 后端返回新版本快照 Then 页面展示版本号、版本ID、状态，并只能对 PRECHECK_PASSED 版本提交升版审批。
- BDD: 审批入口受权限控制 -> Given 批记录版本待提交审批 When 用户调用提交或回调接口 Then 后端使用 `mes:pro-batch-record-template:version-approve` 权限校验，并记录当前登录用户作为提交/审批人。
- RED: `node .\scripts\edhr-batch-version-phase1-contract.test.mjs` -> FAIL, 前端导入结果类型缺少 `batchRecordDefinitionId / batchRecordVersionId / sourceBatchRecordVersionId / versionNo / versionStatus`，页面缺少版本审批面板和提交审批动作。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest#contractMappings_exposeVersionApprovalEndpointsWithApprovalPermission+importResponseAndApprovalResponse_exposePhaseOneVersionContract" test` -> FAIL, 控制器缺少 `/version-approval/submit` 和 `/version-approval/callback` 真实入口；首次运行还暴露同 worktree 治理服务编译状态需同步。
- GREEN: `node .\scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest#contractMappings_exposeVersionApprovalEndpointsWithApprovalPermission+importResponseAndApprovalResponse_exposePhaseOneVersionContract" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseOneSchemaTest,MesProBatchRecordReportControllerTest#contractMappings_exposeVersionApprovalEndpointsWithApprovalPermission+importResponseAndApprovalResponse_exposePhaseOneVersionContract,MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent+batchRecordVersionApproval_switchesCurrentVersionAfterApprovedCallbackAndIgnoresDuplicateEvent+recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_createsDefinitionScopedVersionSnapshot+recognizeUploadedRoute_whenSameNameAndRouteExistsWithoutUpgrade_failsFastBeforeGatewaySave+batchRecordVersionApproval_rejectedCallbackDoesNotSwitchCurrentVersion+batchRecordVersionApproval_blocksWhenMigrationHasBlockerOrConfirmRequired+batchRecordVersionApproval_concurrentApprovalOnlyOneVersionCanSwitchCurrent+recognizeUploadedRoute_whenSameHashReimportedForPendingVersion_returnsIdempotentResultWithoutNewSnapshots,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_persistsBatchRecordVersionSnapshotToExecution,MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask" test` -> PASS, Tests run: 14, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKER: phase1-real-e2e -> 本地 `http://127.0.0.1:48096/actuator/health` 与 `http://127.0.0.1:8096` 均无法连接；未启动本地运行态前，不得声明测试租户写入 E2E 或 `芋道源码/admin` 只读复验通过。

## 运行态与登录预检 - 2026-07-08 20:59:24

- GREEN: runtime-ownership -> PASS，后端 http://127.0.0.1:48096/actuator/health 返回 {"status":"UP"}，前端 http://127.0.0.1:8096 返回 200；进程命令行均指向 D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version。
- GREEN: experience-preflight -> PASS，官方 login-preflight.mjs 使用本机测试租户 测试租户/aoteman/111111 成功进入 /mes/pro/feedback/edhr-version-governance。
- BLOCKER: admin-readonly-login -> 芋道源码/admin/111111 官方登录预检失败，接口返回账号密码不正确；按登录门禁不得猜测密码、不得静默切换账号、不得修改 admin 租户数据。影响：芋道源码/admin 最终只读复验暂不能完成。

## 阶段一最终证据补充（2026-07-08）

- RED: route-use-response-version-contract -> FAIL, 路线用途绑定响应缺少 `batchRecordDefinitionId / batchRecordVersionId`，新执行无法从路线用途继承版本快照。
- GREEN: route-use-response-version-contract -> PASS, `MesProRouteUseBatchRecordRespVO` 与 `MesProRouteUseConfigServiceImpl` 已透传定义 ID 和版本 ID。
- RED: report-list-response-version-contract -> FAIL, 批记录报表列表缺少 `batchRecordDefinitionId / batchRecordVersionId`，前端无法证明新旧 reportId 隔离。
- GREEN: report-list-response-version-contract -> PASS, `BatchRecordReportRespVO`、`MesProBatchRecordReportView` 和列表转换已透传版本字段。
- RED: approval-permission-baseline -> FAIL, 阶段一审批权限菜单 `mes:pro-batch-record-template:version-approve` 未在迁移 SQL 和测试租户审批角色中完整建立。
- GREEN: phase-one-sql-apply -> PASS, 本机 SQL `sql/mysql/20260708_mes_batch_record_version_phase_one.sql` 已应用，statements=43。
- GREEN: approval-permission-baseline -> PASS, 测试租户 `tenant_id=122` 审批人 `smokeappr1` 具备 `mes:pro-batch-record-template:version-approve` 权限，count=1。
- GREEN: phase1-backend-regression-17 -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseOneSchemaTest,MesProRouteUseConfigServiceImplTest#getRouteUseProcessConfigList_shouldReturnDefaultRowWhenAnyCurrentRouteProcessLacksUseConfig,MesProBatchRecordReportControllerTest#contractMappings_exposeVersionApprovalEndpointsWithApprovalPermission+importResponseAndApprovalResponse_exposePhaseOneVersionContract+importEndpointsAndPageEndpoints_delegateToService,MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent+recognizeUploadedRoute_whenSameHashUsedByExistingApprovedVersion_generatesVersionScopedReportCodes+batchRecordVersionApproval_switchesCurrentVersionAfterApprovedCallbackAndIgnoresDuplicateEvent+recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_createsDefinitionScopedVersionSnapshot+recognizeUploadedRoute_whenSameNameAndRouteExistsWithoutUpgrade_failsFastBeforeGatewaySave+batchRecordVersionApproval_rejectedCallbackDoesNotSwitchCurrentVersion+batchRecordVersionApproval_blocksWhenMigrationHasBlockerOrConfirmRequired+batchRecordVersionApproval_concurrentApprovalOnlyOneVersionCanSwitchCurrent+recognizeUploadedRoute_whenSameHashReimportedForPendingVersion_returnsIdempotentResultWithoutNewSnapshots,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_persistsBatchRecordVersionSnapshotToExecution,MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask" test` -> PASS, Tests run: 17, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: phase1-frontend-contract -> `node .\scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS。
- GREEN: phase1-e2e-syntax -> `node --check .\tests\e2e\edhr-batch-version-phase1-real-flow.e2e.js` -> PASS。
- GREEN: phase1-real-e2e -> `node .\tests\e2e\edhr-batch-version-phase1-real-flow.e2e.js` -> PASS, batchRecordName=E2E-PHASE1-1783518588713, definitionId=9, versionId=9, versionNo=V1.0, routeId=922070, approvalInstanceId=BRV-9-1783518691424, submitter=aoteman, approver=smokeappr1。
- GREEN: phase1-runtime-recheck -> PASS, 后端 `http://127.0.0.1:48096/actuator/health` 返回 UP，前端 `http://127.0.0.1:8096` 返回 200。
- BLOCKER: admin-readonly-login -> `芋道源码/admin/111111` 官方登录预检失败，接口返回账号密码不正确；按门禁未猜测密码、未切换账号、未修改 admin 租户数据，影响：一期最终 admin 只读复验暂不能完成。

## 阶段一复验（2026-07-08 21:58）

- GREEN: phase1-backend-regression-17-rerun -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseOneSchemaTest,MesProRouteUseConfigServiceImplTest#getRouteUseProcessConfigList_shouldReturnDefaultRowWhenAnyCurrentRouteProcessLacksUseConfig,MesProBatchRecordReportControllerTest#contractMappings_exposeVersionApprovalEndpointsWithApprovalPermission+importResponseAndApprovalResponse_exposePhaseOneVersionContract+importEndpointsAndPageEndpoints_delegateToService,MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent+recognizeUploadedRoute_whenSameHashUsedByExistingApprovedVersion_generatesVersionScopedReportCodes+batchRecordVersionApproval_switchesCurrentVersionAfterApprovedCallbackAndIgnoresDuplicateEvent+recognizeUploadedRoute_whenSameFileReimportedUnderNewBatchName_createsDefinitionScopedVersionSnapshot+recognizeUploadedRoute_whenSameNameAndRouteExistsWithoutUpgrade_failsFastBeforeGatewaySave+batchRecordVersionApproval_rejectedCallbackDoesNotSwitchCurrentVersion+batchRecordVersionApproval_blocksWhenMigrationHasBlockerOrConfirmRequired+batchRecordVersionApproval_concurrentApprovalOnlyOneVersionCanSwitchCurrent+recognizeUploadedRoute_whenSameHashReimportedForPendingVersion_returnsIdempotentResultWithoutNewSnapshots,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_persistsBatchRecordVersionSnapshotToExecution,MesProEdhrBatchExecutionServiceTest#openOrCreate_persistsBatchRecordVersionSnapshotFromRouteBindingToTask" test` -> PASS, Tests run: 17, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: phase1-frontend-contract-rerun -> `node .\scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS, exit=0。
- GREEN: phase1-e2e-syntax-rerun -> `node --check .\tests\e2e\edhr-batch-version-phase1-real-flow.e2e.js` -> PASS, exit=0。
- GREEN: phase1-real-e2e-rerun -> `node .\tests\e2e\edhr-batch-version-phase1-real-flow.e2e.js` -> PASS, batchRecordName=E2E-PHASE1-1783519081929, definitionId=11, versionId=11, versionNo=V1.0, routeId=922072, approvalInstanceId=BRV-11-1783519172627, submitter=aoteman, approver=smokeappr1。
- BLOCKER: admin-readonly-login-rerun -> `node "D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs" --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password 111111 --target-path /mes/pro/batch-record-template` -> FAIL, HTTP 200 登录失败，账号密码不正确；未猜测密码、未切换账号、未修改 admin 租户数据。

## 主控 review 修复 - 2026-07-09

- REVIEW: independent-agents -> FAIL，阶段一、阶段二、阶段三审查均判定不可放行；主要阻塞为 admin 只读复验、阶段二确认门禁/草稿重传真实链路、阶段三真实行为契约与真实 E2E。
- GREEN: runtime-ownership -> PASS，`48096` Java 进程和 `8096` Vite 进程均来自 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version`。
- GREEN: login-preflight-test-tenant-minimal -> PASS，使用系统 Chrome 真实登录 `测试租户/aoteman/111111` 进入 `/mes/pro/feedback/edhr-version-governance`。
- RED: phase2-review -> FAIL，`CONFIRM_REQUIRED` 确认后仍被旧 `countBlockingItems` / 巡检指标口径计为阻断。
- GREEN: phase2-confirm-gate-contract -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests / 0 failures / 0 errors；已将阻断口径统一为 `BLOCKER + 未确认 CONFIRM_REQUIRED`。
- BLOCKER: admin-readonly-login -> `芋道源码/admin/111111` 仍登录失败；未猜测密码、未切换账号、未修改 admin 租户数据。
- BLOCKER: phase2-real-e2e -> 草稿重传仍需接入真实 Word 文件上传/解析/迁移证据重建，阶段二真实 E2E 不可声明通过。
- BLOCKER: phase3-real-e2e -> 仍需真实版本治理数据、回滚目标版本、签核证据哈希、admin 只读网络写请求断言和数据库前后只读比对。
## 阶段二 review 修复复验 - 2026-07-09 00:39:23

- RED: phase2-draft-reupload-contract -> FAIL，旧契约仍允许 sourceFileName/sourceFileSha256 元数据式草稿重传，且服务实现残留 buildReuploadVersionNo 合成版本路径。
- GREEN: phase2-draft-reupload-contract -> PASS，草稿重传改为 multipart file + productNames + remark，后端复用 recognizeUploadedRoute 执行真实 Word 导入，前端使用 FormData 和 request.upload，禁止手工填写 SHA/文件名。
- GREEN: phase2-phase3-contract-rerun -> node scripts\edhr-version-governance-contract.test.mjs -> PASS；mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKER: admin-readonly-login -> 芋道源码/admin/111111 仍为最终只读复验硬前置；未猜测密码、未切换账号、未修改 admin 租户数据。

## 阶段三真实 E2E 推进与阻塞 - 2026-07-09 01:23

- GREEN: phase3-schema-contract -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，已覆盖 `mes_pro_edhr_unified_change_request / impact / event` 表结构。
- GREEN: phase3-menu-permission-sql -> 本地库已插入 `system_menu` 900303-900306，权限包括 `mes:pro-batch-record-version:governance-query / confirm / import / rollback-request`；测试租户 `tenant_id=122` 的 `超级管理员` 与 `eDHR演练-审批人` 已绑定上述权限。
- RED: phase3-real-e2e-selector -> FAIL，回滚 E2E 初版按输入顺序定位，被草稿重传表单和异步重载覆盖目标版本；未绕过页面，改为等待 summary/impact/inspection/metrics/migration-diff 全部完成后再填写回滚表单，并断言实际 POST `targetVersionId=15`。
- GREEN: phase3-test-tenant-rollback-write -> `pnpm e2e:edhr:version-governance` 已在本地 `测试租户/aoteman` 真实页面路径创建受控回滚申请：`change_code=EDHR-CHANGE-20260709012217`，`controlled_object_type=BATCH_RECORD_VERSION`，`controlled_object_id=12`，`current_version=V2.0`，`target_version=V1.0`，`change_status=DRAFT`，事件表记录 `CREATE -> DRAFT`。
- GREEN: phase2-phase3-backend-contract-rerun -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest,MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 14, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: phase3-frontend-contract-rerun -> `pnpm e2e:edhr:version-governance:check` -> PASS；`node --check tests/e2e/edhr-version-governance-real-flow.e2e.js` -> PASS。
- BLOCKER: admin-readonly-login-final -> 官方登录预检 `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password 111111 --target-path /mes/pro/feedback/edhr-version-governance --timeout 90000` -> FAIL，HTTP 200 登录失败，账号密码不正确；按门禁未猜测密码、未切换账号、未修改 admin 租户数据。
- BLOCKER: release-gate -> 因 `芋道源码/admin` 最终只读复验未通过，当前不能提交、不能合并进 `int_main`，也不能删除 `edhr_version` worktree。

## 继续任务阻塞复核 - 2026-07-09

- GREEN: runtime-state-recheck -> PASS，`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version` 前后端 worktree 均存在，后端 `http://127.0.0.1:48096/actuator/health` 返回 `{"status":"UP"}`，前端 `8096` 与后端 `48096` 均有监听进程。
- BLOCKER: admin-readonly-login-recheck -> 官方登录预检 `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password 111111 --target-path /mes/pro/feedback/edhr-version-governance --timeout 90000` -> FAIL，退出码 1，HTTP 200 登录失败，账号密码不正确。
- INFO: admin-account-readonly-db-check -> 本地库只读核对 `system_users.username='admin'`：`tenant_id=1` 账号存在、启用、未删除，所属租户 `芋道源码` 启用、未删除；`tenant_id=122` 同名账号密码为空，不作为最终 `芋道源码/admin` 只读复验账号。
- BLOCKER: release-gate-recheck -> 当前仍缺少可通过真实登录页的本机 `芋道源码/admin` 凭据或用户明确授权的本地 admin 租户账号修复；按门禁未猜测密码、未切换账号、未修改 admin 租户数据，因此不能提交、不能融合进 `int_main`，不能删除 `edhr_version` worktree。

## admin 只读门禁恢复 - 2026-07-09

- GREEN: admin-readonly-login-admin123 -> 用户提供本机 `芋道源码/admin` 密码 `admin123` 后，官方登录预检 `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password admin123 --target-path /mes/pro/feedback/edhr-version-governance --timeout 90000` -> PASS，真实登录已进入目标页。
- RED: phase3-admin-query-e2e -> `pnpm e2e:edhr:version-governance` -> FAIL，admin 登录已通过，但治理查询脚本等待测试租户版本接口超时；诊断确认 admin 租户页面可进入且无治理写请求，测试租户版本 ID 在 admin 租户下按租户隔离显示 `版本信息未生成`。
- GREEN: phase3-admin-readonly-e2e -> `pnpm e2e:edhr:version-governance:check` -> PASS；`pnpm e2e:edhr:version-governance` -> PASS，测试租户真实页面创建受控回滚申请 `EDHR-CHANGE-20260709081905`，`芋道源码/admin` 真实登录同一路径并验证治理写请求数 `0`。
- REVIEW: phase2-real-e2e-gap -> 阶段二任务文档仍记录 `phase2-real-e2e` 为阻塞，且当前已知证据只覆盖确认门禁契约和草稿重传 multipart 契约；融合前必须补齐阶段二真实页面 E2E 或明确记录不可放行原因。

## 最终任务范围门禁 - 2026-07-09

- GREEN: backend-contracts-db -> PASS，阶段二结构化迁移状态流、阶段二契约、阶段三治理契约目标测试均通过，16 tests / 0 failures / 0 errors。
- GREEN: task-closeout-cleanup-preview -> PASS，已清理后端运行日志临时产物；保留任务文档、设计文档与执行日志。
- BLOCKER: merge-main-dirty-overlap-pending -> worktree 分支可提交；融合 `int_main` 前必须完成主工作区脏改重叠审计。

## SQL 契约门禁与后端提交 - 2026-07-09

- RED: backend-commit-tdd-hook -> FAIL，提交包含 `sql/mysql/20260708_mes_batch_record_version_phase_one.sql`，仓库门禁要求同步新增 `script/tests` 契约测试。
- GREEN: backend-sql-contract-test -> `python -X utf8 -m pytest script/tests/test_mes_batch_record_version_sql.py -q` -> PASS，6 tests / 0 failures。
- GREEN: backend-task-commit -> `git commit -m '任务: 完成eDHR批记录三阶段升版后端'` -> PASS，commit `e952894902`。

## 融合门禁审计 - 2026-07-09

- GREEN: frontend-task-commit -> PASS，`codex/edhr_version` 前端提交 `0cafa8c56`，工作区干净。
- GREEN: backend-task-commit -> PASS，`codex/edhr_version` 后端提交 `e952894902`，工作区干净。
- BLOCKER: frontend-int-main-merge-tree -> FAIL，`git merge-tree --write-tree int_main codex/edhr_version` 退出码 `1`，`src/api/mes/pro/scheduleorder/index.ts` 内容冲突。
- BLOCKER: frontend-main-dirty-overlap -> FAIL，主工作区前端脏改 `89` 个文件，与本任务重叠 `src/api/mes/pro/scheduleorder/index.ts`、`src/views/mes/pro/batchrecordtemplate/index.vue`。
- BLOCKER: backend-main-dirty-overlap -> FAIL，后端 merge-tree 退出码 `0`，但主工作区后端脏改 `71` 个文件，与本任务重叠 `yudao-module-mes/src/test/resources/sql/create_tables.sql`。
- BLOCKER: merge-cleanup -> 按 worktree 门禁，重叠归因/处理前不得融合 `int_main`，不得删除 `edhr_version` worktree。


## 合并后收尾 - 2026-07-09 09:51:50

- GREEN: backend-int-main-merge -> PASS，`git merge --no-ff codex/edhr_version` 生成 merge commit `9c0fd4471e`。
- GREEN: backend-overlap-restore -> PASS，恢复本地重叠脏改后 `create_tables.sql` 同时包含 eDHR 版本表和工艺主数据字段，无冲突标记。
- GREEN: backend-post-merge-contracts -> PASS，SQL 契约 6 tests 与 Maven 16 tests 均通过。
