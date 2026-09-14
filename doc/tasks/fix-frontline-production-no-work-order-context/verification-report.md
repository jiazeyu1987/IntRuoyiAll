# 验证报告：一线生产取消工单匹配上下文

## Summary

- 结论：实现已完成，核心验证通过；相邻 JUnit 补跑因同模块并发 Maven 占用共享 `target` 阻塞，未强杀或清理其它任务进程。
- 用户口径：一线生产不需要匹配任何工单；电子签名校验应以选择员工/实际填写员工为准，不要求当前登录账号等于实际填写员工。
- 设计约束：未引入 fallback、默认工单、默认任务、默认记录本、默认成功或吞异常。

## Implementation Scope

- 后端运行态：`MesFrontlineRuntimeConfigServiceImpl` 生成一线生产 `productionSubmitContext` 时不再解析或要求 activeOrder/workOrder/task/recordbook，仅保留路线、路线工序、工序、工位和审批组长上下文。
- 后端正式提交：`MesProFrontlineFeedbackSubmitServiceImpl` 与 `MesProFrontlineFeedbackPayloadSplitter` 允许 `workOrderId/taskId/itemId/recordbookPayload` 为空；签名主体仍必须等于实际填写员工。
- 工序池事件：`MesProcessPoolEventServiceImpl` 对 `PRODUCTION_SUBMIT` 放开 `workOrderId` 和记录本来源必填，只在记录本上下文任一字段存在时要求三字段完整；PQC 非生产提交仍要求工单与记录本来源。
- 数据库迁移：`20260808_mes_process_pool_frontline_no_work_order.sql` 将生产提交相关 work_order/recordbook 字段改为可空，并用生成列 `work_order_context_key` 保持无工单场景唯一性。
- 前端正式提交：`FrontlineFixedTemplatePanel.vue` 只在 PQC 模式要求工单；生产正式提交不再把工单、任务、物料、记录本列为必填，幂等键改为路线/工序/工位/员工/草稿 key。

## Verification

- PASS: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolEventServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 22 tests，0 failures，0 errors。
- PASS: `node tests\e2e\frontline-formal-submit-static.spec.cjs` -> 正式提交静态合同通过。
- PASS: `node tests\e2e\frontline-formal-submit-selected-employee-static.spec.cjs` -> 所选员工电子签名静态合同通过。
- PASS: `node tests\e2e\role-matrix-ac-m10-sop-production-static.spec.cjs` -> AC-M10 SOP 生产合同通过。
- PASS: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output doc\tasks\fix-frontline-production-no-work-order-context\migration-policy-gate.json` -> status=passed，migrationCount=449。
- PASS: scoped `git diff --check -- <task-owned files>` -> 无 whitespace error，仅 LF/CRLF 工作区提示。
- PASS: bug regression evidence validator -> `Bug regression evidence is valid.`
- PASS: database schema evidence validator -> `Database schema evidence is valid.`

## Blocker

- BLOCKED: 相邻 JUnit `MesP0FrontlineSubmitIdempotencyTest,MesFrontlineEmployeeSwitchServiceTest` 未能补跑；同模块仍有并发 Maven 编译进程占用共享 `target`，等待 3 分钟后仍为 6 个相关进程。
- 处理原则：未清理 `target`，未强杀并发 Maven/Java 进程，避免破坏其它任务产物；一次本任务启动的 Maven 隔离目录尝试发现仍写入共享 `target\classes` 后已立即停止。

## Remaining Action

- 等并发 Maven 全部结束后，补跑：`mvn -pl yudao-module-mes "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`。
