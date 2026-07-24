# Execution Log

## BDD

BDD: 旧工序在当前路线唯一映射 -> Given 历史业务引用已删除工序且路线存在唯一同编码当前工序, When 报工及下游实时业务解析工序身份, Then 使用当前路线工序并保留历史来源 ID。

BDD: 旧工序无法唯一映射 -> Given 指定路线没有候选或存在多个同编码当前工序, When 实时业务解析工序身份, Then 明确失败且不得静默跳过检验、BOM 或资源逻辑。

BDD: 计划实际使用不同新旧 ID -> Given 同一排产工序的计划保存旧 ID 且实际保存新 ID, When 查询日报, Then 依据稳定排产工序关系聚合为一行。

## Evidence

RED: mvn -q -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest -DskipITs test -> FAIL, 统一工序身份解析接口、忽略逻辑删除查询和明确错误码尚不存在。
GREEN: mvn -q -pl yudao-module-mes -Dtest=legacy-process-focused-regression -DskipITs test -> PASS, 旧工序 ID 目标回归、自动排程回归和真实导入旧工序错误消除验证通过。

- `GREEN: previous-task-status -> PASS`：上一任务 `20260710-legacy-process-id-audit` 已完成并提交审计报告。
- `GREEN: experience-preflight -> PASS`：已读取 PowerShell、worktree 和项目错误预防经验；独立后端 worktree 创建前已确认主工作区任务文件无重叠。
- `GREEN: worktree-bootstrap -> PASS`：已从后端 `int_main` 提交 `2714df1ccb` 创建分支 `codex/legacy-process-id-eradication`。
- `GREEN: task-artifacts -> PASS`：已建立任务目标、PRD、开发计划、测试计划和机器状态文件。
- `RED: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest -DskipITs test -> FAIL`：统一工序身份解析接口、忽略逻辑删除查询和明确错误码尚不存在，测试编译按预期失败。
- `GREEN: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest -DskipITs test -> PASS`：6 个测试通过，覆盖当前直连、历史路线工序快照、旧工序编码唯一映射、缺失与歧义失败。
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProFeedbackServiceImplTest,MesProFeedbackImportRecordServiceImplTest" -DskipITs test -> FAIL`：40 个测试中，新旧工序规范化、审批检验标志和导入归属测试按预期暴露旧严格匹配调用。
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProFeedbackServiceImplTest,MesProFeedbackImportRecordServiceImplTest" -DskipITs test -> PASS`：40 个测试通过；报工创建、排产快照导入、既有旧工序报工审批和导入检验标志均使用统一身份解析。
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessServiceImplTest,MesWmItemConsumeServiceImplTest,MesQcIpqcServiceImplTest" -DskipITs test -> FAIL`：批量工序身份映射、等价工序 BOM 查询和 IPQC 规范工序接口尚不存在，测试编译按预期失败。
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessServiceImplTest,MesWmItemConsumeServiceImplTest,MesQcIpqcServiceImplTest" -DskipITs test -> PASS`：33 个测试通过；等价工序 BOM、物料消耗及工作站旧工序 IPQC 均使用当前工序身份。
- `RED: mvn -pl yudao-module-mes "-Dtest=MesMdWorkstationServiceImplTest,MesDvMachineryProcessServiceImplTest" -DskipITs test -> FAIL`：当前工作站查询遗漏旧工序绑定，设备工序服务缺少按当前工序身份加载历史绑定的接口。
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesMdWorkstationServiceImplTest,MesDvMachineryProcessServiceImplTest,MesMdWorkstationCapacityServiceTest,MesProRouteResourceServiceImplTest" -DskipITs test -> PASS`：14 个测试通过；旧工序工作站和设备产能绑定规范化到当前工序，工作站产能及路线资源页不再因 ID 更替丢失资源。
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderDailyCompareLegacyProcessTest" -DskipITs test -> FAIL`：同一排产工序的计划旧 ID 与实际新 ID 被拆成两条日报记录。
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProScheduleOrderDailyCompareLegacyProcessTest" -DskipITs test -> PASS`：日报改用日期与排产工序快照 ID 作为稳定聚合键，计划量与实际量合并为一行。
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest#requireRouteProcess_deletedSnapshot_resolvesCurrentRouteProcess" -DskipITs test -> FAIL`：批记录入口仍只查询未删除路线工序，历史快照无法进入当前工序。
- `GREEN: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest#requireRouteProcess_deletedSnapshot_resolvesCurrentRouteProcess" -DskipITs test -> PASS`：批记录入口在直接关系不存在时使用统一工序身份解析器。
- `RED: schedule-config-normalization -> FAIL`：排程工序配置、流程配置和 WIP 快照仍按历史 `route_process_id` 精确匹配，新增 3 个回归测试按预期失败。
- `GREEN: schedule-config-normalization -> PASS`：排程工序配置、流程配置和 WIP 快照统一映射到当前路线工序；保存配置时将命中的历史配置正式迁移到当前路线工序 ID。
- `RED: auto-schedule-snapshot-normalization -> FAIL`：自动排程刷新仍保留历史路线工序和前置工序 ID。
- `GREEN: auto-schedule-snapshot-normalization -> PASS`：自动排程刷新在应用配置前规范化当前路线工序、前置工序及工序 ID。
- `RED: route-flow-and-schedule-config-services -> FAIL`：路线流程配置与路线排程配置无法读取或更新历史路线工序配置。
- `GREEN: route-flow-and-schedule-config-services -> PASS`：两类配置服务均按当前路线工序身份读取历史配置，更新时不再插入重复配置。
- `GREEN: direct-import-historical-schedule-config -> PASS`：直接报工导入可使用历史路线工序保存的启用排程流程配置。
- `RED: edhr-batch-task-config-historical-process -> FAIL`：eDHR 批任务构建因流程配置保存旧路线工序 ID 而拒绝创建任务。
- `GREEN: edhr-batch-task-config-historical-process -> PASS`：eDHR 首任务解析、任务配置与批记录绑定按当前路线工序聚合，历史配置所有权仍按原快照校验。
- `RED: route-default-config-and-copy -> FAIL`：默认排程配置维护和路线复制会把历史路线工序配置判为缺失或重复创建。
- `GREEN: route-default-config-and-copy -> PASS`：默认流程/排程配置更新历史记录；路线复制先规范化源路线工序身份，再映射到目标路线工序。
- `RED: batch-execution-response-historical-process -> FAIL`：批记录详情、列表和审批上下文直接查询已删除路线工序，工艺和工序信息为空。
- `GREEN: batch-execution-response-historical-process -> PASS`：批记录响应、批量响应和审批上下文统一解析到当前路线工序。
- `RED: process-form-permission-historical-binding -> FAIL`：当前路线工序查询不到保存在历史路线工序下的表单绑定和权限范围。
- `GREEN: process-form-permission-historical-binding -> PASS`：表单权限服务可定位历史绑定并以当前路线工序身份返回；禁用的当前绑定仍明确拒绝。
- `RED: rehearsal-readiness-historical-process -> FAIL`：eDHR 演练预检将历史路线工序 ID 直接传入权限规则和对象权限检查。
- `GREEN: rehearsal-readiness-historical-process -> PASS`：演练预检使用当前路线工序执行权限检查，同时兼容读取尚未迁移的历史填写规则。
- `GREEN: focused-regression -> PASS`：统一解析、报工、导入归属、路线流程/排程配置、路线复制、批记录、eDHR 权限预检、工位和设备等目标测试集合通过；`git diff --check` 无空白错误。
- `BASELINE: ThirdPartyFeedbackImportServiceImplTest -> 2 failures + 1 strict-stubbing error`：相同 3 个失败在未应用本任务改动的 `int_main` 基线可复现，不属于本次回归。
- `BASELINE: auto-schedule-suites -> 24 failures/errors`：工作树与 `int_main` 基线失败集合一致，属于既有排程拓扑/日历测试问题。
- `GREEN: source-xlsx-present -> PASS`：确认 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 存在且未修改。
- `BLOCKER: real-playwright-import -> 本机 8081/48080/48081 均无运行态`：当前没有可连接的前端和后端进程，尚未执行测试租户真实页面导入；不得以接口或 mock 代替。

- `GREEN: auto-schedule-focused-regression -> PASS`：`mvn -q -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProAutoScheduleRouteDependencyTest,MesProRouteVersionAndCopyTest,MesProAutoScheduleAlgorithmContractTest,MesProScheduleOrderAdmissionDiffServiceTest" -DskipITs test` 通过，覆盖旧工序快照规范化、剩余旧快照保留、无拓扑历史快照和夜班窗口。
- `GREEN: legacy-process-focused-regression -> PASS`：`mvn -q -pl yudao-module-mes "-Dtest=MesProRouteProcessServiceImplTest,MesProFeedbackServiceImplTest,MesProFeedbackImportRecordServiceImplTest,ThirdPartyFeedbackImportServiceImplTest#isEnabledScheduleRouteProcess_shouldUseHistoricalProcessConfig+importDirectWorkReportWorkbook_shouldCreateFeedbackAndSubmitApprovalSkippingMiscRows+importDirectWorkReportWorkbook_shouldMatchTaskCodeWhenScheduleProcessLinksMultipleTasks+importDirectWorkReportWorkbook_shouldMatchHistoricalTaskProcessIdentity,MesProRouteFlowConfigServiceImplTest,MesProRouteScheduleConfigServiceTest,MesProRouteServiceImplTest,MesProScheduleResourceAdjustmentServiceImplTest,MesProRouteResourceServiceImplTest,MesProBatchRecordExecutionServiceImplTest#requireRouteProcess_deletedSnapshot_resolvesCurrentRouteProcess+buildResp_deletedSnapshot_usesCurrentRouteProcessIdentity+entryContextAndOpenOrCreateByContext_shouldIgnoreScheduleTaskAndWorkstationFields,MesProEdhrBatchExecutionLegacyProcessTest,MesProEdhrWorkTaskLegacyProcessTest,MesProEdhrTravelerLegacyProcessTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProEdhrRehearsalReadinessServiceTest,MesProProcessServiceImplTest,MesMdWorkstationServiceImplTest,MesDvMachineryProcessServiceImplTest,MesProScheduleOrderDailyCompareLegacyProcessTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleRouteDependencyTest,MesProRouteVersionAndCopyTest,MesProAutoScheduleAlgorithmContractTest,MesProScheduleOrderAdmissionDiffServiceTest" -DskipITs test` 通过。
- `GREEN: git diff --check -> PASS`：代码和任务文档无空白错误。
- `GREEN: real-playwright-import -> PASS`：测试租户真实前端导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 返回成功弹窗，`oldProcessErrorSeen=false`，未出现“未找到对应的工艺工序配置”。
- `LIMITATION: real-playwright-import-positive-create -> 测试租户未匹配工单/任务`：真实文件导入结果为创建报工 0、提交审批 0、跳过杂务行 70；只读审计显示该文件在测试租户无匹配工单/任务，因此无法在不造数、不 SQL 写入的前提下验证正向创建报工。

## Current Status

代码修复、静态审计、目标回归和真实前端导入旧工序错误验证已完成。

- `GREEN: implementation-commit -> PASS`：提交 `1b39bb04c0`（rebase 后为 `4df739059d`）记录实现改动。
- `GREEN: closeout-record-commit -> PASS`：提交 `73d503cb83`（rebase 后为 `f731d56427`）记录收尾文档。
- `GREEN: rebase-int_main -> PASS`：任务分支成功 rebase 到 `int_main`。
- `GREEN: post-rebase-focused-regression -> PASS`：rebase 后旧工序目标回归 Maven 命令通过。
- `GREEN: fast-forward-merge -> PASS`：`codex/legacy-process-id-eradication` 已快进融合到主工作区 `int_main`。
- `GREEN: merged-result-focused-regression -> PASS`：融合结果上旧工序目标回归 Maven 命令通过。
- `GREEN: merged-result-diff-check -> PASS`：融合结果任务相关路径 `git diff --check` 通过。
- `GREEN: worktree-cleanup -> PASS`：任务专用 worktree 已清理。

最终状态：`completed`。
