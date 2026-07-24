# Execution Log

## 2026-07-24

- User intent: 将 `D:\IntRuoyiWorktree\jiluben_20260722_clean` 融合进当前 `int_main`。
- Rules read: `docs\worktree-restrictions.md`, `docs\branch-runtime-ports.md`, `docs\backend-development.md`, `docs\frontend-development.md`, `docs\database-rules.md`, `docs\e2e-rules.md`, `docs\powershell-encoding.md`, `docs\powershell-memory.md`, `docs\task-closeout-rules.md`, `docs\local-runtime.md`, `docs\login-access.md`, `docs\engineering\technology-stack-routing.md`.
- Skills read: `backend-api-delivery`, `frontend-feature-delivery`, `database-schema-delivery`, `behavior-driven-development`.
- BDD: 记录本批次同步配置保留 -> Given 路线工序配置同时存在批记录与记录本开关 / When 从 worktree 融合到 int_main 并保存路线配置 / Then `recordbookEnabled`、`recordCategory`、`formSlotType` 和批次执行任务快照必须完整保留，不被默认值或旧保存逻辑覆盖。
- BDD: 记录本填写保留审计证据 -> Given 用户以记录本不受控模式填写 eDHR 执行页 / When 保存草稿、提交签名或字段审计 / Then 字段审计、电子签名、签名时间和 `fillCarrier=RECORDBOOK` 必须写入正式请求，不允许以静默成功隐藏失败。
- BDD: 批次作废审批策略可解析 -> Given 用户发起 eDHR 批次执行作废 / When 前端或服务端请求解析作废业务审批策略 / Then 后端必须返回明确审批策略依赖或失败原因，而不是缺接口或默认成功。
- BDD: 路线动态表单实例一致 -> Given 同一批次中存在共享路线表单或批记录绑定 / When 创建或打开批次执行任务 / Then 共享任务必须绑定同一个 Form Center 实例或执行实例，身份不一致时 fail fast。

## 2026-07-25 Fusion Continuation

- User authorization: 用户授权在已读取 `worktree-restrictions`、`branch-runtime-ports`、`task-closeout-rules` 的前提下继续融合。
- Applied fix: 新建/返工批次的任务配置来源规则收敛为：当前 BATCH 工序配置存在时使用并严格校验当前配置；当前 BATCH 工序配置整体缺失且发布版本快照完整时使用已发布快照。
- Applied fix: 打开传统批记录任务时移除重复 `bindExecution` 调用，避免同一次打开任务重复绑定工作任务。
- Applied fix: 路线版本发布投影恢复受支持 legacy flat `batchRecordReportId` 快照解析，默认 MAIN / PROCESS / BATCH_RECORD / CONTROLLED_BATCH / reportSort=1。
- Applied fix: 补齐文控日志页面 `formatDateTimeValue(value, '-')` 安全时间格式化融合遗漏。
- Applied fix: 同步字段审计真实 E2E 为保存按钮直接等待 `fieldAuditSave` API 响应；修复 Windows CRLF 静态合同误判。
- Applied fix: 后端测试同步正式审计/BPM 行为：保存路线规则测试显式模拟登录用户；审批快照断言当前 BPM taskId。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=..." test` -> FAIL, 7 behavior regressions from initial conflict resolution.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProRecordbookBatchControlledSyncMigrationContractTest,MesProEdhrBatchVoidApprovalDependencyContractTest,MesProBatchRecordParsedCellTest,MesProEdhrBatchExecutionServiceTest,MesProRouteFlowConfigServiceImplTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteVersionPublishProjectionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 189.
- RED: extended backend suite -> FAIL, 4 missing login-user audit preconditions and 1 outdated BPM taskId assertion.
- GREEN: `mvn -pl yudao-module-mes,yudao-module-bpm -am "-Dtest=BusinessApprovalOrchestratorBpmRequiredTest,FormActionPolicyResolveServiceTest,FormCenterRuntimeContractTest,MesProBatchRecordExecutionFieldAuditControllerTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordParsedCellTest,MesProBatchRecordRouteBRecognizerTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrBatchExecutionVoidStatusListenerTest,MesProEdhrBatchVoidApprovalDependencyContractTest,MesProEdhrBatchVoidFormEffectExecutorTest,MesProEdhrFormFillLogServiceImplTest,MesProEdhrRecordChangeContractTest,MesProEdhrRecordChangeServiceTest,MesProEdhrWorkTaskServiceImplTest,MesProRecordbookBatchControlledSyncMigrationContractTest,MesProRouteFlowConfigServiceImplTest,MesProRouteProcessFlowServiceImplTest,MesProRouteVersionPlatformAdapterTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionWorkflowServiceTest,MesProScheduleReplanApprovalContractTest,MesReleaseCompanionContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 498, Failures: 0, Errors: 0, Skipped: 6.
- GREEN: target worktree modified frontend static contracts -> PASS, 8 tracked scripts.
- GREEN: new frontend static contracts -> PASS, 6 scripts.
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `int_main` ports 8081/48081.
- GREEN: `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.
- Current status: `ready_for_closeout`; commit/push and final closeout pending.
- GREEN: experience-preflight -> PASS, 已合并经验到 docs/backend-development.md 与 docs/e2e-rules.md，并更新 docs/experience-index.md 关键词路由。

- GREEN: task-closeout-cleanup preview/apply -> PASS, 删除本任务临时文件 `bug-regression-evidence.md` 与 `jiluben-tracked.patch`，保留 `task.md`、`execution-log.md`、`verification-report.md`。