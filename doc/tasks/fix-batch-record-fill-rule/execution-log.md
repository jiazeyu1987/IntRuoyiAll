# 执行日志：修复批记录模板填写规则误报

## User Intent

批次执行里点击“打开填写”时，系统提示“批记录模板存在未确认填写规则的可填单元格”，并列出大量单元格位置。需要修复该误报，使流程能够按真实模板规则打开填写。

## Milestone Log

- 2026-07-24：创建任务记录，准备执行缺陷复现、根因定位、回归测试和最小修复。
- 2026-07-24：检查 `docs/experience-index.md`，结果为不存在；本任务按低风险缺陷修复继续推进，并记录门禁缺失事实。
- 2026-07-24：按用户确认补充 BDD/TDD 根治设计；设计明确复用 `saveCellRules`、`MesProBatchRecordCellRuleSupport`、Jimu JSON 网关和执行层 fail fast 校验，不新增冗余规则系统。
- 2026-07-24：根据文档 review 结论优化验收口径：自动识别不等于可执行确认，历史 JSON 修复改为后续受控任务；当前任务只声明保存边界归一化和现有 fail fast 语义不变。
- 2026-07-24：复查原 `LinkedHashMap` 编译阻塞已不再存在；重跑目标测试后发现新的范围外编译阻塞，当前未修改这些并发任务文件。
- 2026-07-24：按 TDD 计划新增支持层组合回归用例，断言保存归一化输出会被现有 `isReviewedRule` 识别为确认规则；复用既有保存服务和执行快照测试构成完整链路证据。
- 2026-07-24：目标模块已恢复可编译状态；关键 Maven GREEN 验证通过。完整相关回归集发现一项与本次来源归一化无直接关系的损耗报告 Word 解析断言失败，未修改其并发变更。
- 2026-07-24：确认本地前端 `8081` 和后端 `48081` 均在监听；浏览器会话登录超时，未获得任务专用测试账号，因此未执行写型真实 E2E。
- 2026-07-24：单独复现损耗报告回归失败前，构建再次被范围外的 `MesProRouteFlowConfigServiceImpl` 阻塞：新增调用 `resolveRecordbookEnabled`，但 helper 未实现。该文件不属于当前批记录填写规则任务范围，未修改。
- 2026-07-24：继续复跑门禁；三份文档结构校验均通过。关键 Maven 用例在 compile 阶段被 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled(Boolean,String)` 缺失阻塞，未进入本任务新增断言。

## BDD Scenarios

- BDD: 批次执行打开填写不误报已确认模板单元格 -> Given 批记录模板包含已配置填写规则的可填单元格 / When 用户在批次执行中点击打开填写 / Then 系统不应把这些单元格提示为未确认填写规则并阻断打开填写
- BDD: 保存已确认自动建议时归一化为人工确认 -> Given 规则保存请求包含 `source=AUTO` 且 `reviewed=true` 的可填单元格 / When 后端保存规则并写回 Jimu JSON / Then 持久化规则必须为 `source=MANUAL` 且 `reviewed=true`
- BDD: 真正未确认规则仍阻断打开填写 -> Given 模板存在缺少有效确认规则的可填单元格 / When 用户点击打开填写 / Then 系统必须 fail fast 并返回具体坐标，不得创建执行快照
- BDD: 历史异常规则显式修复 -> Given 历史模板存在 `source=AUTO` 且 `reviewed=true` 的异常规则 / When 管理员 dry run 后对指定报表执行修复 / Then 系统只修复经校验和确认范围内的候选规则，不静默批准未知模板

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test` -> BLOCKED，当前模块主代码编译失败，阻塞到达新增断言；未取得严格 RED。早期阻塞缺失方法为 `MesProEdhrWorkTaskServiceImpl.recordWorkTaskRuleSaveAudit`、`recordCandidateSignatureCompleteAudit`、`recordFillTaskReassignAudit`。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation test` -> BLOCKED，主代码编译通过后，全量 testCompile 因范围外测试依赖缺失而失败，未到达新增用例。
- GREEN: `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation' '-Dmaven.compiler.testIncludes=cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImplDbTest.java' test` -> BLOCKED，`MesProBatchRecordExecutionFieldAuditServiceImpl` 缺少 `parseOptionalJson`、`parseCanonicalValueJson`，且 `ResolvedChange` 构造参数不匹配；该文件已有未提交改动，不属于本次任务。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule test` -> PASS，1 个测试通过。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot+openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution,MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule+applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed test` -> PASS，5 个测试通过。

## Verification Evidence

- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\fix-batch-record-fill-rule` -> PASS，BDD/TDD acceptance plan validation passed.
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-batch-record-fill-rule\bug-regression-evidence.md` -> PASS，Bug regression evidence is valid.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\fix-batch-record-fill-rule\backend-api-evidence.md` -> PASS，Backend API evidence is valid.
- 文档优化后复跑上述两个校验命令 -> PASS，确认验收口径调整后仍满足结构要求。
- 关键测试完成后复跑三份文档校验，并执行 `git diff --check` -> PASS。
- 代码修复已完成：`MesProBatchRecordCellRuleSupport.toRuleJson` 在规则 `reviewed=true` 且来源误带 `AUTO` 时持久化为 `MANUAL`。
- 文档已明确当前修复不声明历史模板已修复；历史 dry run/apply 需另行授权并处理 `getCellRules` 读时写回风险。
- `mvn -pl yudao-module-mes -Dtest=MesProBatchRecordCellRuleSupportTest,MesProBatchRecordReportServiceImplDbTest test` -> FAIL，129 个测试中 1 个失败：`uploadExtraFormSlot_whenLossReportWordHasMergedBody_expandsAllFillableFieldsAndDoesNotReuseOldHashReport` 期望 `□报废`，实际 `报废`；该行为与本次 `source/reviewed` 归一化无直接关系。
- 本地前端和后端端口均在监听；浏览器控制台报告登录超时，未执行写型 E2E。
- 继续复跑：`python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root doc\tasks\fix-batch-record-fill-rule` -> PASS。
- 继续复跑：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-batch-record-fill-rule\bug-regression-evidence.md` -> PASS。
- 继续复跑：`python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\fix-batch-record-fill-rule\backend-api-evidence.md` -> PASS。
- 继续复跑：`mvn -pl yudao-module-mes -Dtest=MesProBatchRecordReportServiceImplDbTest#saveCellRules_normalizesReviewedAutoSuggestionToManualConfirmation,MesProBatchRecordExecutionServiceImplTest#openOrCreateByContext_freezesReviewedNumberAndDateCellRulesIntoExecutionSnapshot+openOrCreateByContext_unreviewedFillableCellRule_mustFailFastWithoutCreatingExecution,MesProBatchRecordCellRuleSupportTest#toRuleJson_normalizesReviewedAutoSuggestionToExecutableManualRule+applyAutomaticSuggestions_setsRulesAfterWordImportWithoutMarkingUserReviewed test` -> BLOCKED，compile 阶段失败：`MesProRouteFlowConfigServiceImpl` 第 603、707 行调用的 `resolveRecordbookEnabled(Boolean,String)` 未实现。

## Blockers

- 严格 RED 未在修复前运行取得；代码已存在时才恢复测试环境，当前只能诚实保留该证据缺口。
- 相邻回归集存在范围外失败：损耗报告 Word 解析将 `□报废` 解析为 `报废`；该行为需其所属任务确认或修复。
- 后续单独复现相邻失败或继续本任务关键用例时，模块主代码被范围外 `MesProRouteFlowConfigServiceImpl.resolveRecordbookEnabled` 缺失阻塞，当前构建状态不稳定。
- 真实 E2E 缺少可用登录会话和任务专用测试账号；不得使用共享或生产业务账号替代。
