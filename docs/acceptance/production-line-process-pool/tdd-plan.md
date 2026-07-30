# 生产一线报工工序池 TDD 计划

## Purpose and Scope

本文档为 10 个可先推进的功能点定义严格 TDD 顺序、RED 预期失败、GREEN 验证命令和重构检查。文档阶段不修改生产代码；实现阶段必须先补测试，再写最小生产代码，最后运行回归。

## Evidence Reviewed

- `docs/acceptance/production-line-process-pool/bdd-scenarios.md`
- `docs/inception/project-brief.md`
- `docs/inception/evidence-inventory.md`
- F1-F8 子 agent 输出草案和主审修正，其中 F5/F6 为原审核副本与修改日志增量草案。
- 本次线程新增 F9/F10：生产班组长 / PQC 班组长复核、异常上报和班组级基础维护口径。
- `docs/backend-development.md`
- `docs/frontend-development.md`
- `docs/e2e-rules.md`

## TDD Sequence

| Step | 功能点 | RED Commands | Expected Failures | 最小实现目标 | GREEN Commands | Refactor Checks |
| --- | --- | --- | --- | --- | --- | --- |
| T01 | F1 工序池专用模型 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest#shouldCreateDedicatedProcessPoolTables" test` | 缺少正式工序池主表、事件表或实现错误复用 `mes_pro_feedback_surplus_pool`。 | 新增工序池主模型、工序池提交事件模型、事件来源关联字段。 | 同 RED 命令 PASS。 | 模型命名体现工序池和提交事件；现有余量池只可作为参考或下游余量表。 |
| T02 | F1 事件必填上下文 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventServiceTest#shouldRejectEventWhenRequiredContextMissing" test` | 事件缺生产工单、路线、工序、实际员工、设备账号、设备、模板类型、报工来源、记录本来源、payload、签名仍可保存。 | 事件创建前集中校验正式上下文，缺失即失败。 | 同 RED 命令 PASS。 | 不使用默认员工、默认设备、默认模板、空 payload 或空工序池。 |
| T03 | F1 服务端提交时间和签名 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTimeSignatureTest" test` | 后端接受前端时间，或签名主体可被设备账号替代。 | 提交时间只由服务端生成；每条事件唯一签名；签名员工等于实际员工。 | 同 RED 命令 PASS。 | 登录账号、实际员工、电子签名员工字段必须分离。 |
| T04 | F1 PQC 入池 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest#shouldStorePqcInspectionAsPoolEvent" test` | PQC 成功/失败不能关联工序池事件。 | PQC 简化结果保存为工序池过程检验事件。 | 同 RED 命令 PASS。 | PQC 特有字段放在质量过程明细或 payload，不破坏统一事件结构。 |
| T05 | F2 组合提交接口契约 | `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitControllerTest" test` | 现有接口不能一次承载报工 payload、记录本 payload、工序池上下文、实际员工和签名。 | 新增或扩展一线组合提交契约，明确 `feedbackPayload`、`recordbookPayload`、`processPoolContext`、`actualEmployeeId`、`signatureId`。 | 同 RED 命令 PASS。 | 不退化为前端连续调用多个普通接口。 |
| T06 | F2 组合提交事务 | `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackSubmitRollbackTest" test` | 无法同时断言 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、`processPoolEventId`，或中途失败留下部分数据。 | 组合提交应用服务统一事务编排报工、记录本、工序池事件。 | 同 RED 命令 PASS。 | 事务边界在组合服务层；禁止补偿式默认成功。 |
| T07 | F2 payload 拆分 | `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackPayloadSplitterTest" test` | 设备参数、上工序输入、输出、损耗无法稳定拆分到报工和记录本。 | 建立字段映射器和正式来源关联。 | 同 RED 命令 PASS。 | 设备参数不得硬塞进报工备注；不得用文本匹配追溯。 |
| T08 | F2 路线不阻断和超限原始值 | `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackRawLimitBypassTest" test` | 提交依赖路线前后置状态，或 `min/max` 拦截 `10/50`。 | 一线组合提交只做授权、结构、必填、签名校验；原始超限值保留。 | 同 RED 命令 PASS。 | 这是正式原始记录规则，不是 fallback。 |
| T09 | F3 固定模板目录 | `mvn -pl yudao-module-mes -am "-Dtest=FrontlineTemplateCatalogTest,FrontlineTemplateResolverTest" test` | 任意模板编码可通过，或缺绑定时返回默认/空模板。 | 固定模板类型受控；按实际员工 + 当前工序解析模板；缺失 fail-fast。 | 同 RED 命令 PASS。 | 不引入默认模板兜底，不混用批记录表单、表单槽位、工序开始。 |
| T10 | F3 模板字段契约 | `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,PqcSimpleTemplateContractTest,FrontlineTemplatePayloadContractTest" test` | 生产模板缺四类字段，PQC 允许非成功/失败，payload 缺工序池上下文。 | 生产模板和 PQC 模板输出标准 payload。 | 同 RED 命令 PASS。 | 固定字段字典集中维护，模板模块不直接写工序池汇总。 |
| T11 | F3 前端模板切换和渲染 | `pnpm --dir IntRuoyiFronted test:unit frontline-template-switch`; `pnpm --dir IntRuoyiFronted test:unit frontline-template-render` | 切换员工后 UI 未切换、旧 payload 残留、页面展示手填时间或无关批记录字段。 | 员工或工序变化时重新加载固定模板，清空不适用状态。 | 同 RED 命令 PASS。 | 模板状态按实际员工隔离，不复用上一员工 payload。 |
| T12 | F4 设备账号路线范围 | `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" test` | 缺少设备账号 -> 路线集合 -> 工序集合模型，或 `X=0` 仍能进入。 | 解析设备账号绑定路线和可切换工序；`X >= 1`。 | 同 RED 命令 PASS。 | 不引入默认路线或默认工序。 |
| T13 | F4 员工切换和模板解析 | `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineEmployeeSwitchServiceTest,MesFrontlineTemplateResolverTest" test` | 可切换未绑定员工，或切换员工触发登录/认证。 | 工序内只列绑定员工；切换实际员工不改变登录账号、不二次验证。 | 同 RED 命令 PASS。 | 不把员工切换命名或实现为 login、auth、impersonate。 |
| T14 | F4 提交身份留痕 | `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineSubmitIdentityTraceTest,MesFrontlineSubmitAuthorizationTest" test` | 提交未保存登录账号、实际员工、签名员工、设备、工作站，或签名员工不一致仍成功。 | 提交前重校验授权并保存完整身份和现场上下文。 | 同 RED 命令 PASS。 | 前端候选仅用于交互，后端是唯一可信门禁。 |
| T15 | F7 FIFO 排序和缺失时间阻塞 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldAllocateWorkOrdersByPlannedStartTime,MesProcessPoolFifoAllocationServiceTest#shouldBlockWhenPlannedStartTimeIsMissing" test` | 仍按排产或其它字段分配，或空计划开始时间参与排序。 | 只按生产工单 `plannedStartTime ASC`，缺失时整体阻塞。 | 同 RED 命令 PASS。 | 禁止 fallback 到创建时间、工单号、当前时间。 |
| T16 | F7 分配明细和锁定 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest#shouldPersistAllocationLinesFromEventFragmentsToWorkOrder,MesProcessPoolAllocatedFragmentLockTest" test` | 无法表达事件片段到生产工单关系，或已分配片段仍可改。 | 建立正式分配明细；已分配片段锁定。 | 同 RED 命令 PASS。 | 锁定逻辑集中在服务层，前端只读不是保护依据。 |
| T17 | F7 累计完成和并发防超分 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest" test` | 完成只看单条报工或路线状态，并发分配可超分。 | 多事件片段累计判断工序完成；事务/锁防止超分。 | 同 RED 命令 PASS。 | 完成数量口径未确认时必须显式阻塞或配置化。 |
| T18 | F8 时间轴查询 | `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineDateFilterTest,ProcessPoolTimelineFilterTest" test` | 缺少正式工序池事件查询模型，无法按天或多条件过滤。 | 只读查询返回时间、签名、账号、员工、工序、设备、模板、生产工单。 | 同 RED 命令 PASS。 | 过滤字段必须使用正式关联字段。 |
| T19 | F8 内容摘要和追溯 | `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineContentSummaryTest,ProcessPoolTimelineTraceabilityTest" test` | 无法展示生产、损耗、设备参数、PQC 或 FIFO/审核/修改状态。 | 事件详情只读返回模板摘要和追溯状态。 | 同 RED 命令 PASS。 | 时间轴模块只查状态，不执行分配、修正或修改。 |
| T20 | 前端真实路径静态与 E2E | `pnpm --dir IntRuoyiFronted test:e2e frontline-device-account-switch-employee.spec.ts`; `pnpm --dir IntRuoyiFronted test:e2e process-pool-timeline.spec.ts` | 真实页面无入口、无数据、无电子签名或无法查询结果。 | Playwright 走真实报工入口、切换员工、模板填写、签名提交、时间轴查询。 | 同 RED 命令 PASS。 | 不用 mock 数据、静态 JSON、API-only 代替真实路径。 |
| T21 | F5 审核副本 schema | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest#shouldCreateReviewCopyTables" test` | 缺少审核副本主表、字段明细、修正规则、审核签名和状态字段。 | 新增审核副本主模型和字段明细模型。 | 同 RED 命令 PASS。 | 审核副本表归属工序池，不写入余量池或备注。 |
| T22 | F5 原始值不改写 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldPreserveRawEventPayloadWhenGenerateReviewCopy" test` | 生成审核副本时改写原始 payload 或记录本原始条目。 | 副本只写审核副本表，原始事件和记录本来源不更新。 | 同 RED 命令 PASS。 | 原始数据和修正数据必须分表或分字段保留。 |
| T23 | F5 上下限修正规则 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldClampValueToMaxWhenRawValueExceedsMax,MesProcessPoolReviewCopyServiceTest#shouldClampValueToMinWhenRawValueBelowMin,MesProcessPoolReviewCopyServiceTest#shouldKeepValueWhenRawValueWithinRange" test` | `50` 未修正到 `40`、`10` 未修正到 `20`，或范围内 `30` 被错误修改。 | 保存 raw/corrected/rule 三元组。 | 同 RED 命令 PASS。 | 只处理有正式上下限元数据的数值字段。 |
| T24 | F5 元数据和字段映射阻塞 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldBlockWhenLimitMetadataMissing,MesProcessPoolReviewCopyServiceTest#shouldBlockWhenFieldMappingMissing" test` | 缺上下限或字段映射时默认成功、跳过字段或按字段名猜测。 | 缺正式元数据时 fail-fast，不生成有效副本。 | 同 RED 命令 PASS。 | 禁止默认上下限、空范围和文本匹配。 |
| T25 | F5 审核签名和 FIFO 锁定 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldRequireReviewerSignatureWhenSubmitReviewCopy,MesProcessPoolReviewCopyServiceTest#shouldRejectReviewCorrectionForAllocatedQuantityFragment" test` | 无审核签名仍提交，或已分配字段仍被审核副本修正。 | 审核副本提交要求唯一电子签名；影响分配字段已分配时阻塞。 | 同 RED 命令 PASS。 | 不通过审核副本绕过 F7 锁定。 |
| T26 | F6 revision schema 和主路径 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest#updateUnallocatedEventCreatesFieldDiffAndSignatureLog" test` | 缺 revision 表、字段级 diff、修改原因或重新签名校验。 | 新增原始记录 revision 模型；未分配记录修改写入版本和字段级日志。 | 同 RED 命令 PASS。 | 修改日志必须结构化关联工序池提交事件。 |
| T27 | F6 签名和修改原因阻塞 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature,MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutChangeReason" test` | 无签名、重复签名或空修改原因仍可修改。 | 修改请求必须提供新的唯一电子签名和非空修改原因。 | 同 RED 命令 PASS。 | 不复用原提交签名，不生成默认修改原因。 |
| T28 | F6 FIFO 锁定阻塞 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionFifoLockTest#rejectsQuantityFieldUpdateWhenFragmentAllocated,MesProcessPoolEventRevisionFifoLockTest#rejectsUpdateWhenFifoLockStatusCannotBeConfirmed" test` | 已分配数量字段仍可改，或锁定状态查不到时默认未锁定。 | 接入 F7 锁定查询；已分配或锁定状态无法确认时拒绝。 | 同 RED 命令 PASS。 | 禁止影响生产工单 FIFO 分配结果。 |
| T29 | F6 字段级 diff 合同 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionDiffContractTest#requiresFieldLevelDiff" test` | 只记录 before/after payload 或备注文本，缺字段编码和值差异。 | 每个变化字段记录 fieldCode、fieldName、beforeValue、afterValue、affectsQuantityFragment。 | 同 RED 命令 PASS。 | diff 基于固定模板字段定义，不靠文本解析。 |
| T30 | F5/F6 前端和时间轴只读追溯 | `pnpm --dir IntRuoyiFronted test:e2e process-pool-review-copy-and-revision.spec.ts`; `node IntRuoyiBackend\yudao-module-mes\src\test\js\process-pool-review-copy-revision-static.spec.cjs` | 页面无审核副本/修改历史入口，或时间轴提供写操作。 | 前端真实路径覆盖审核副本生成、原始记录修改重签名、时间轴只读摘要。 | 同 RED 命令 PASS。 | 时间轴仅展示 F5/F6 状态，不执行审核或修改写操作。 |
| T31 | F9 班组长负责范围模型 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest#shouldResolveProductionLeaderEmployeeScope,MesTeamLeaderScopeServiceTest#shouldResolvePqcLeaderEmployeeScope" test` | 无法区分生产班组长、PQC 班组长和负责员工范围，或默认返回全量员工。 | 建立班组长负责范围查询能力，优先复用部门、岗位、角色、班组、工序或工作站配置。 | 同 RED 命令 PASS。 | 不用 admin 权限、全量员工或前端过滤替代后端范围控制。 |
| T32 | F9 班组长提交看板查询 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionWorkbenchTest#shouldListOnlyScopedEmployeeSubmissions,MesTeamLeaderSubmissionWorkbenchTest#shouldHideOutOfScopeSubmissionDetail" test` | 班组长可看到非负责员工提交，或查询缺少复核状态、PQC 状态、异常状态。 | 工作台按负责范围返回提交事件、报工、记录本和 PQC 摘要。 | 同 RED 命令 PASS。 | 后端接口必须范围过滤；前端不能拿全量数据后隐藏。 |
| T33 | F9 复核员工提交 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionReviewServiceTest#shouldCreateReviewLogWithoutChangingRawSubmission,MesTeamLeaderSubmissionReviewServiceTest#shouldRejectReviewForOutOfScopeSubmission" test` | 复核直接改写原始 payload，或越权复核成功。 | 保存复核状态、复核人、服务端时间和复核说明；原始记录只读。 | 同 RED 命令 PASS。 | 复核是管理状态，不进入原始记录 revision，也不修改电子签名。 |
| T34 | F9 生产工单异常标记与上报 | `mvn -pl yudao-module-mes -am "-Dtest=MesWorkOrderAbnormalReportServiceTest#shouldListAllWorkOrdersForTeamLeaderAbnormalHandling,MesWorkOrderAbnormalReportServiceTest#shouldCreateAbnormalReport,MesWorkOrderAbnormalReportServiceTest#shouldNotModifyRawProcessPoolEventWhenReportAbnormal" test` | 班组长只能看到负责范围关联工单、无法保存异常上报，或异常标记改写一线提交 / FIFO 分配。 | 生产工单异常处理列表对班组长展示所有生产工单；保存异常记录和上报记录，关联工单、工序、来源事件和说明。 | 同 RED 命令 PASS。 | 全量生产工单列表不得放大员工提交明细权限；异常上报与一线原始记录、审核副本、FIFO 明细分离。 |
| T35 | F10 班组员工添加和禁用 | `mvn -pl yudao-module-mes -am "-Dtest=MesTeamEmployeeBindingServiceTest#shouldAddEmployeeToProcessCandidate,MesTeamEmployeeBindingServiceTest#shouldDisableEmployeeForFutureSelectionOnly" test` | 添加员工不进入候选，或禁用员工隐藏历史提交。 | 维护工序可选员工绑定和禁用状态，保存审计日志。 | 同 RED 命令 PASS。 | 禁用只影响后续候选；历史事件、签名和报工来源仍可查询。 |
| T36 | F10 不良原因列表维护 | `mvn -pl yudao-module-mes -am "-Dtest=MesDefectReasonCatalogServiceTest#shouldCreateReasonWithScopeAndAudit,MesDefectReasonCatalogServiceTest#shouldExposeReasonToLossAndPqcTemplates" test` | 不良原因无生效范围、无审计，或模板无法选择新增原因。 | 复用字典或原因配置保存不良原因、生效范围和审计记录。 | 同 RED 命令 PASS。 | 新增原因不得改写历史原因值。 |
| T37 | F10 工序设备和参数上下限维护 | `mvn -pl yudao-module-mes -am "-Dtest=MesProcessDeviceParameterRuleServiceTest#shouldBindDeviceToProcessWithAudit,MesProcessDeviceParameterRuleServiceTest#shouldExposeLimitRuleToReviewCopyButNotRawSubmit" test` | 工序无法新增设备，或上下限变成一线提交硬拦截。 | 保存工序设备绑定、参数上下限、生效范围和审计日志；提供给审核副本规则读取。 | 同 RED 命令 PASS。 | 参数上下限只服务审核副本、复核提示和异常判断，不裁剪原始 payload。 |
| T38 | F9/F10 前端真实路径 | `pnpm --dir IntRuoyiFronted test:e2e team-leader-workbench-and-maintenance.spec.ts`; `pnpm --dir IntRuoyiFronted test:unit team-leader-workbench-scope` | 页面没有班组长入口，或前端使用全量数据本地隐藏，或维护动作无审计反馈。 | 前端真实路径覆盖提交看板、复核、异常上报、员工启停、不良原因、工序设备和上下限维护。 | 同 RED 命令 PASS。 | 前端只展示后端授权结果；所有维护动作必须显示审计和生效范围。 |

## RED Commands

实现阶段必须逐项记录类似以下证据：

```text
RED: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest#shouldCreateDedicatedProcessPoolTables" test -> FAIL, 缺少正式工序池主表和事件表
RED: pnpm --dir IntRuoyiFronted test:e2e frontline-device-account-switch-employee.spec.ts -> FAIL, 一线报工入口尚不能完成设备账号内切换员工
RED: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopyServiceTest#shouldClampValueToMaxWhenRawValueExceedsMax" test -> FAIL, 缺少审核副本上下限修正规则
RED: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionServiceTest#rejectsUpdateWithoutNewSignature" test -> FAIL, 原始记录修改尚未要求重新电子签名
RED: mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionReviewServiceTest#shouldCreateReviewLogWithoutChangingRawSubmission" test -> FAIL, 班组长复核尚未独立于一线原始记录保存
RED: mvn -pl yudao-module-mes -am "-Dtest=MesProcessDeviceParameterRuleServiceTest#shouldExposeLimitRuleToReviewCopyButNotRawSubmit" test -> FAIL, 设备参数上下限尚未与一线原始提交硬拦截分离
```

## Expected Failures

- 早期 RED 应失败在缺少正式模型、接口、字段、权限、模板或页面入口。
- 缺少生产工单 `plannedStartTime` 的 FIFO 测试应失败为明确 blocker，不应自动通过。
- 电子签名缺失或签名员工不一致应失败为业务校验错误。
- 一线原始超限值测试若被 `min/max` 拦截，应失败在现有记录本校验链路。
- 时间轴查询不得返回缺少提交时间或电子签名的事件为有效事件。
- 审核副本生成缺少上下限元数据、字段映射、审核权限或审核电子签名时，应失败为明确 blocker。
- 原始记录修改缺少修改原因、重新电子签名、字段级 diff 或 FIFO 锁定查询时，应失败为明确 blocker。
- 已 FIFO 分配的数量、质量状态或可分配状态字段被修改或修正时，应失败为锁定错误。
- 班组长负责范围缺失时，提交看板、复核、员工启停和基础维护应失败为权限或配置 blocker；生产工单异常列表仍要求班组长具备异常处理权限。
- 班组长复核如果改写原始 payload、电子签名、报工来源、记录本来源或 FIFO 明细，应失败。
- 工序设备参数上下限若在一线提交阶段拦截 `10/50` 原始值，应失败。

## GREEN Commands

第一版实现完成后至少需要以下定向验证通过：

```powershell
mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolSchemaTest,MesProcessPoolEventServiceTest,MesProcessPoolTimeSignatureTest,MesProcessPoolPqcEventTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackRawLimitBypassTest" test
mvn -pl yudao-module-mes -am "-Dtest=FrontlineTemplateCatalogTest,FrontlineTemplateResolverTest,ProductionTemplateContractTest,PqcSimpleTemplateContractTest,FrontlineTemplatePayloadContractTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineTemplateResolverTest,MesFrontlineSubmitIdentityTraceTest,MesFrontlineSubmitAuthorizationTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolFifoAllocationServiceTest,MesProcessPoolAllocatedFragmentLockTest,MesProcessPoolCompletionCalculatorTest,MesProcessPoolFifoAllocationConcurrencyTest" test
mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineDateFilterTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineContentSummaryTest,ProcessPoolTimelineTraceabilityTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolReviewCopySchemaTest,MesProcessPoolReviewCopyServiceTest,MesProcessPoolReviewCopyPermissionTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolEventRevisionSchemaTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionDiffContractTest,MesProcessPoolEventRevisionFifoLockTest,ProcessPoolTimelineRevisionSummaryTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderSubmissionWorkbenchTest,MesTeamLeaderSubmissionReviewServiceTest,MesWorkOrderAbnormalReportServiceTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesTeamEmployeeBindingServiceTest,MesDefectReasonCatalogServiceTest,MesProcessDeviceParameterRuleServiceTest" test
pnpm --dir IntRuoyiFronted ts:check
pnpm --dir IntRuoyiFronted test:e2e frontline-feedback-integrated.spec.ts
pnpm --dir IntRuoyiFronted test:e2e frontline-device-account-switch-employee.spec.ts
pnpm --dir IntRuoyiFronted test:e2e process-pool-timeline.spec.ts
pnpm --dir IntRuoyiFronted test:e2e process-pool-review-copy-and-revision.spec.ts
pnpm --dir IntRuoyiFronted test:e2e team-leader-workbench-and-maintenance.spec.ts
```

## Refactor Checks

- 不新增 fallback、默认模板、默认时间、默认员工、默认路线或模拟成功。
- 报工、记录本、工序池事件的来源关联必须是结构化字段。
- 批记录表单、表单槽位、工序开始三类配置不得混用。
- 工序池与现有报工余量池保持边界；余量池不得成为工序池主模型。
- 电子签名、实际员工、设备登录账号字段必须语义分离。
- FIFO 模块只消费工序池已明确为可分配的数量，不临时推断 PQC 业务规则。
- 时间轴模块保持只读，不执行修改、分配、审核副本生成。
- 审核副本模块不得改写原始 payload，不得使用默认上下限或跳过缺失字段。
- 原始记录修改模块不得把修改日志写成备注文本，不得复用原提交电子签名。
- F5/F6 都必须在写入前复核 FIFO 锁定状态；无法确认锁定状态时阻塞。
- 班组长员工提交范围控制必须在后端完成，不能只靠前端隐藏；生产工单异常列表全量可见不得放大提交详情权限。
- 班组长复核、异常上报和基础维护均不得覆盖一线原始记录、记录本原始条目、工序池提交事件、电子签名或 FIFO 分配明细。
- 员工禁用只影响后续候选，不得影响历史追溯。
- 不良原因、工序设备和参数上下限维护必须保存维护人、服务端时间、生效范围和审计日志。

## Evidence Log Template

```text
BDD: <场景名> -> Given/When/Then 摘要
RED: <命令> -> FAIL, <预期失败原因>
GREEN: <命令> -> PASS
REGRESSION: <命令> -> PASS, 覆盖相邻报工、记录本、eDHR、生产工单能力
BLOCKER: <前置条件> -> <缺失内容和影响>
```

## Initial Blocker Definitions

- 工序池 schema 最小集：主表、事件表、数量片段表、PQC 记录表、FIFO 分配明细、审核副本主/明细、原始记录 revision/diff；缺任何一类时对应模块 RED 必须失败。
- FIFO 计划时间：`plannedStartTime` 为空或重复时测试预期为 FAIL/blocker；不允许测试通过时偷偷改用创建时间、工单号或当前时间。
- PQC 可分配规则：成功转 `AVAILABLE`，失败转不可分配；测试只允许 FIFO 消费 `AVAILABLE` 片段。
- 班组长权限：提交看板必须后端范围过滤；生产工单异常列表可全量，但详情接口仍拒绝非负责员工提交明细。
- 审核副本和原始修改：审核副本只写副本表；原始修改只写 revision/diff；两者都不得覆盖原始事件 payload。

## Test Blockers

初始定义和解除条件见 `docs/acceptance/production-line-process-pool/open-questions-blockers.md`；RED/GREEN 证据中遇到阻塞时，应引用对应 BLK 编号。

- 缺少正式工序池 schema、事件表、数量片段、分配流水。
- 缺少电子签名正式接口或测试签名数据。
- 缺少设备账号、设备、工作站、实际员工、工艺路线、工序绑定和固定模板测试数据。
- 缺少生产工单计划开始时间或 FIFO 二级排序规则。
- 缺少 PQC 成功/失败对可分配数量的正式规则。
- 缺少模板字段上下限元数据、审核权限、审核电子签名或审核副本状态枚举。
- 缺少原始记录 revision 表、字段级 diff、修改原因、重新电子签名或 FIFO 锁定查询。
- 缺少班组长负责范围、复核状态、全量生产工单异常入口权限、异常上报状态、员工启停状态或维护审计模型。
- 缺少不良原因列表、工序设备绑定、设备参数上下限规则或与审核副本规则的正式关联。
- 本地前端、后端、数据库、Redis、测试租户、账号或权限不可用时，真实 E2E 阻塞。
