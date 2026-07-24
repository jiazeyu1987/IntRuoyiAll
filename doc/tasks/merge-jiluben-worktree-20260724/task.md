# 20260724 融合 jiluben worktree 到 int_main

## Task Goal

将 `D:\IntRuoyiWorktree\jiluben_20260722_clean` 中与记录本、批记录、eDHR 批次执行、字段审计、作废审批、路线表单绑定、时间格式和相关 SQL/测试有关的未提交内容融合进当前 `int_main` 工作区 `E:\IntRuoyi`，保持 `int_main` 端口与运行规则不被覆盖。

## Milestones

- [x] M1: 完成规则读取、Git 基线和目标 worktree 差异清单确认。
- [x] M2: 迁移目标 worktree 中记录本相关后端、SQL、前端和测试文件到 `int_main`。
- [x] M3: 解决融合后的编译/类型/静态测试问题，不引入 fallback、降级或吞异常。
- [x] M4: 运行分支端口守卫、后端/前端定向验证，并记录失败或通过证据。
- [ ] M5: 完成任务文档、经验沉淀、提交、推送和收尾。

## Expected Verification

- `git status --short --branch` 确认当前分支为 `int_main` 且融合范围可解释。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过，确认端口矩阵未被 worktree 覆盖。
- 后端：运行记录本/eDHR/路线绑定相关定向 Maven 测试，至少覆盖新增迁移测试、作废审批依赖、批次执行、路线配置和 ParsedCell 契约。
- 前端：运行新增/受影响的静态 E2E 合同测试，至少覆盖记录本批次同步、路线批记录保存、时间格式硬化和记录变更时间格式。
- `git diff --check` 无空白错误。
- 若验证因缺少环境、依赖或现有并发改动失败，必须记录真实 blocker，不宣称完成。

## Verification Evidence

- PASS: `mvn -pl yudao-module-mes,yudao-module-bpm -am "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest,FormActionPolicyResolveServiceTest,FormCenterRuntimeContractTest,MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordParsedCellTest,MesProBatchRecordRouteBRecognizerTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionVoidStatusListenerTest,MesProEdhrBatchVoidApprovalDependencyContractTest,MesProEdhrBatchVoidFormEffectExecutorTest,MesProEdhrFormFillLogServiceImplTest,MesProEdhrRecordChangeContractTest,MesProEdhrRecordChangeServiceTest,MesProEdhrWorkTaskServiceImplTest,MesProRecordbookBatchControlledSyncMigrationContractTest,MesProRouteFlowConfigServiceImplTest,MesProRouteProcessFlowServiceImplTest,MesProRouteVersionPlatformAdapterTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionWorkflowServiceTest,MesProScheduleReplanApprovalContractTest,MesReleaseCompanionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> Tests run: 498, Failures: 0, Errors: 0, Skipped: 6.
- PASS: target worktree modified frontend static contracts -> 8 tracked static contract scripts passed.
- PASS: new frontend static contracts -> `edhr-recordbook-batch-sync-static`, `route-batch-record-save-contract-static`, `edhr-system-time-format-hardening-static`, `edhr-record-change-time-format-static`, `system-time-format-followup-static`, `system-time-format-remaining-modules-static`.
- PASS: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> `int_main` frontend `8081`, backend `48081`.
- PASS: `git diff --check` -> no whitespace errors; only LF/CRLF conversion warnings.

## BDD Scenarios

- BDD: 记录本批次同步配置保留 -> Given 路线工序配置同时存在批记录与记录本开关, When 从 worktree 融合到 int_main 并保存路线配置, Then `recordbookEnabled`、`recordCategory`、`formSlotType` 和批次执行任务快照必须完整保留，不被默认值或旧保存逻辑覆盖。
- BDD: 记录本填写保留审计证据 -> Given 用户以记录本不受控模式填写 eDHR 执行页, When 保存草稿、提交签名或字段审计, Then 字段审计、电子签名、签名时间和 `fillCarrier=RECORDBOOK` 必须写入正式请求，不允许以静默成功隐藏失败。
- BDD: 批次作废审批策略可解析 -> Given 用户发起 eDHR 批次执行作废, When 前端或服务端请求解析作废业务审批策略, Then 后端必须返回明确审批策略依赖或失败原因，而不是缺接口或默认成功。
- BDD: 路线动态表单实例一致 -> Given 同一批次中存在共享路线表单或批记录绑定, When 创建或打开批次执行任务, Then 共享任务必须绑定同一个 Form Center 实例或执行实例，身份不一致时 fail fast。


## 经验门禁

- `docs/backend-development.md#eDHR 批次任务配置来源门禁`：当前 BATCH 工序配置存在时必须使用当前配置并严格校验；当前配置整体缺失时才允许按已发布快照建任务。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：目标 worktree 自身失败的静态合同要先区分 CRLF/合同过期/产品失败，并同步真实 E2E 脚本与当前页面路径。
## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，已按当前配置/发布快照的正式边界解决融合回归，并同步真实 E2E 与测试前置条件。
- `是否存在临时补丁或绕过`：否。