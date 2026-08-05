# Execution Log

## User Intent

用户要求对 `AC-M22 | 检查批记录完整性` 的代码不符合项进行修复。当前修复聚焦后端可单元验证的正式来源完整性：PQC 状态、activeOrder 路线版本、调拨/库存来源类型、调拨数量和来源状态。

## Preflight

- SKILL: `bug-regression-fix-loop` -> LOADED，按回归修复闭环执行。
- RULE: `docs/powershell-encoding.md`、`docs/backend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md` -> READ。
- EXPERIENCE: `docs/experience-index.md` -> READ，命中 eDHR 批次任务配置来源、MES PQC 项目级检验快照、Maven `-D` 引号和 Maven target 异常门禁。
- GIT_BASELINE: `e7c27613e` 等基线提交已保存开始前既有脏工作区改动；本任务从该基线之后开始。

## BDD Scenarios

- BDD: AC-M22 PQC 未确认阻塞放行 -> Given 活跃订单存在正式 PQC 任务且任务仅为 `SUBMITTED`，When 执行 eDHR 放行预检，Then `INSPECTION_RESULT` 必须为 `BLOCKER` 且不能标记预检通过。
- BDD: AC-M22 调拨来源不完整阻塞放行 -> Given 活跃订单只存在部分调拨/库存 trace，When 执行 eDHR 放行预检，Then 缺少 `TRANSFER/SHIPMENT/BATCH_TRACE` 等必备正式来源时必须返回 `BLOCKER`。
- BDD: AC-M22 调拨数量或状态无效阻塞放行 -> Given 活跃订单存在 trace 但数量为空/非正或来源状态未闭环，When 执行放行预检，Then `INVENTORY_CONSISTENCY` 必须阻塞并说明异常来源。
- BDD: AC-M22 activeOrder 路线版本必须匹配 -> Given 同工单同路线有不同版本 activeOrder，When 执行放行预检，Then 必须按批次 `routeVersionId` 精确匹配，缺失版本匹配时阻塞。

## TDD Evidence

- RED: attempted -> 新增/更新 `MesOrderReleaseCompletenessServiceTest` 与 `MesPqcProcessInspectionAggregationServiceTest`，覆盖 routeVersion 精确 activeOrder、缺 `TRANSFER/SHIPMENT/BATCH_TRACE`、trace 数量非正、movement `sourceStatus` 未闭环、PQC 聚合后 task 必须 `SUBMITTED -> CONFIRMED`。
- RED_BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest" test` -> FAIL，reactor 上游 `yudao-common` 无匹配测试，需按门禁补 `"-Dsurefire.failIfNoSpecifiedTests=false"`。
- RED_BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT，任务自有 Maven PID `55508` 超时后仍后台运行且无目标 surefire 报告，按当前任务范围停止 PID `55508/15244`，未停止并行任务进程。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Surefire 合计 41 tests / 0 failures / 0 errors。

## Command Log

- `git status --short --branch -uno` -> branch `int_main...origin/int_main [ahead 13]`，存在大量并行脏改动；本任务仅修改 AC-M22 相关文件和本任务文档。
- `mvn -pl yudao-module-mes "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT after 244s；进程 `41936` 仍后台运行且无目标 surefire 报告。
- `jcmd 41936 Thread.print` -> 主线程位于 `sun.nio.fs.WindowsNativeDispatcher.SetEndOfFile`、`PathFileObject.openOutputStream`、`ClassWriter.writeClass`、`JavaCompiler.generate/compile`，未进入 Surefire；后续进程退出但仍无 `MesOrderReleaseCompletenessServiceTest` / `MesProEdhrReleaseServiceImplTest` 报告。
- `git diff --check -- <AC-M22 paths>` -> PASS，仅有 CRLF warning，无 whitespace/error 输出。
- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PARTIAL PASS；Surefire 报告显示 `MesPqcProcessInspectionAggregationServiceTest` 4/0/0、`MesTeamLeaderSubmissionReviewServiceTest` 6/0/0、`MesFrontlinePqcContextServiceTest` 13/0/0。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> TIMEOUT，但进程退出后 `javap` 显示 `MesTeamLeaderOrderProcessCompletionService` class 已刷新为 8 参数构造器，解除旧 target class 签名阻塞。
- 复跑 `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT；`jcmd 60704 Thread.print` 显示主线程在 `lombok.core.PostCompiler$1.close` / `ClassWriter.writeClass` / `JavaCompiler.compile`，未生成 AC-M22 surefire 报告；已停止本任务自有 PID `60704`，未触碰并行 PID `49984/48672`。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260805-role-matrix-code-repair\bug-regression-evidence.md` -> PASS。
- EXPERIENCE_CONSOLIDATION: 已按 `project-experience-consolidation` 将 Maven javac/Lombok class 写入长时间运行门禁沉淀到 `docs/powershell-memory.md`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，进入 Surefire 后仅 `MesProEdhrReleaseServiceImplTest#approveRejectsUnverifiableSignoffEvidenceHash` 失败；原因是测试未清除 `insertPendingApprovalRelease(...)` 前置预检产生的 `operationAuditService.record(...)` mock 调用，导致审批失败路径误判为产生终态审计。
- 修复：仅在该测试进入 `approve(...)` 前补 `clearInvocations(operationAuditService)`，隔离前置预检审计调用；未修改生产放行逻辑。
- `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`MesOrderReleaseCompletenessServiceTest` 8/0/0、`MesPqcProcessInspectionAggregationServiceTest` 7/0/0、`MesProEdhrReleaseServiceImplTest` 24/0/0、同名根包兼容测试 2/0/0，合计 41/0/0。
- `git diff --check -- <AC-M22 paths + task docs>` -> PASS，仅 CRLF warning。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260805-role-matrix-code-repair\bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-role-matrix-code-repair --mode preview` -> PASS，keep `task.md` / `execution-log.md` / `verification-report.md` / `bug-regression-evidence.md`，delete/blocked/warnings 均为 none。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260805-role-matrix-code-repair --mode apply` -> PASS，deleted_paths 为 none。
- PROJECT_EXPERIENCE_CONSOLIDATION: 已复核 `docs/*memory*.md`；将 `docs/powershell-memory.md` 中 AC-M22 Maven javac/Lombok 证据行校正为“阻塞释放后必须复跑标准 Maven 并以 Surefire PASS 为 GREEN”，未新建长期经验文档。

## Blockers

- CLOSEOUT_BLOCKED: 当前共享工作区为 `int_main...origin/int_main [ahead 13]`，且存在大量并行修改/未跟踪文件，范围覆盖 MES 后端、前端、SQL、多个并行任务文档和 `docs/backend-development.md` / `docs/powershell-memory.md`。为避免把其它任务改动混入 AC-M22 实现提交，本次不执行宽泛暂存、提交或推送；AC-M22 代码与验证已完成，等待用户确认共享工作区提交策略或由对应并行任务完成基线/推送。
