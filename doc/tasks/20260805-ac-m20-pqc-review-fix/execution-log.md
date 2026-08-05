# Execution Log

## User Intent

用户要求对 AC-M20 “PQC 组长确认 PQC 检验单”剩余不符合项进行修复。

## Gates Read

- 技能：bug-regression-fix-loop、backend-api-delivery、frontend-feature-delivery、database-schema-delivery、bdd-tdd-acceptance-planner。
- 收尾经验沉淀技能：project-experience-consolidation；本次经验合并到 `docs/powershell-memory.md`，未新建长期经验文档。
- 项目规则：task-closeout-rules、backend-development、frontend-development、database-rules、e2e-rules、powershell-encoding、powershell-memory、worktree-restrictions、branch-runtime-ports。
- 经验索引：docs/experience-index.md；命中 PQC 项目级检验快照门禁、前端静态契约隔离门禁、PowerShell Maven `-D` 参数引号门禁。

## BDD Scenarios

- BDD: PQC 事件只能由 PQC 组长复核 -> Given 工序池事件类型为 `PQC_INSPECTION`, When 非 `PQC` leaderType 调用复核, Then 服务必须 fail fast 且不得写复核记录或汇集记录。
- BDD: PQC 组长确认后任务正式完成 -> Given PQC 检验任务已提交并绑定 PQC 事件, When PQC 组长提交 `APPROVED` 复核, Then 系统必须保存复核记录、汇集过程检验记录，并把对应 `mes_pqc_inspection_task.task_status` 更新为 `CONFIRMED`。
- BDD: PQC 组长退回需保留原因且不算完成 -> Given PQC 检验任务已提交, When PQC 组长提交 `REJECTED` 复核且缺少退回原因, Then 服务必须拒绝；When 提供原因, Then 保存退回记录但不得汇集或确认 PQC 任务。
- BDD: 自我确认和重复确认被拒绝 -> Given PQC 检验员提交自己的检验单或已有终态复核, When 同一用户自审或再次/并发复核, Then 服务必须拒绝并保留原状态。
- BDD: 前端终态入口可见性 -> Given PQC 组长工作台列表显示待复核、已通过、已退回行, When 用户查看操作列, Then 待复核行可复核，已通过/已退回行不可重复复核，只有已退回行可修正，退回提交必须填写复核说明。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes "-Dtest=MesPqcProcessInspectionAggregationServiceTest" test-compile` -> FAIL，预期暴露 AC-M20 缺口：缺少 `MesPqcProcessInspectionAggregateDetailDO` / `MesPqcProcessInspectionAggregateDetailMapper`，`MesPqcProcessInspectionAggregationServiceImpl` 构造器缺正式事件、任务、逐件明细和汇集明细依赖，`MesProProcessPoolPqcRecordMapper.updateProcessInspectionAggregatedIfPending` 缺 tenant-aware 参数。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderSchemaTest,MesQaPqcSchemaTest" test` -> FAIL，上游模块无匹配测试；按 Maven Reactor 门禁需要补 `-Dsurefire.failIfNoSpecifiedTests=false`。
- GREEN: `node tests\e2e\team-leader-pqc-review-gate-static.spec.js` -> PASS，证明复核按钮仅待复核可见、修正按钮仅退回可见、终态二次打开被阻断、退回说明必填。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，432 个迁移元数据通过，包含 `20260805_mes_process_pool_ac_m20_pqc_review_closure`。
- GREEN: `git diff --check` -> PASS，仅提示 Windows CRLF 转换警告，无 whitespace error。
- BLOCKED: `mvn -pl yudao-module-mes "-DskipTests" compile` -> FAIL，JVM native memory allocation failed，`hs_err_pid44408.log` 显示页面文件不足。
- BLOCKED: `$env:MAVEN_OPTS='-Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC'; mvn -pl yudao-module-mes "-DskipTests" compile` -> BLOCKED，多次超过 120s/180s 后仍在 Javac/Lombok 编译且无 surefire 或最终退出码；本任务 Maven PID 已停止，避免与其它并发 Java 任务争用资源。

## Resume Verification 2026-08-05

- GREEN: `mvn -pl yudao-module-mes "-DskipTests" compile` -> PASS，旧 JVM 页面文件 blocker 已解除。
- GREEN: `mvn -pl yudao-module-mes -am -Pmes-ac-m20-pqc-review-targeted-tests "-Dtest=MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProcessPoolTeamLeaderSchemaTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Surefire 结果 22 tests / 0 failures / 0 errors / 0 skipped。
- GREEN: `node tests\e2e\team-leader-pqc-review-gate-static.spec.js` -> PASS，2026-08-05 复跑仍通过。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，432 migrations。
- GREEN: `git diff --check` -> PASS，仅 CRLF warning，无 whitespace error。
- GREEN: `mvn -pl yudao-server -am -Pmes-ac-m20-pqc-review-targeted-tests "-DskipTests" package` -> PASS，生成 `yudao-server\target\yudao-server-exec.jar`。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48083/actuator/health` -> `UP`；`Invoke-WebRequest http://127.0.0.1:8083/` -> `200`。
- BLOCKED: `Get-ChildItem Env:RRM_*` -> no entries；真实写入型 `role-requirement-matrix-real-flow.e2e.js` 需要 `RRM_FRONTEND_URL`、`RRM_BACKEND_URL`、`RRM_TENANT`、多角色账号/密码、`RRM_SIGNATURE_IDS_JSON`、生产订单、工艺路线、调拨/发货/补料/退料、批记录报表和 QA 规程版本 ID。当前缺少正式任务自有前置，不能执行真实页面写入和清理链路。
- NOT RUN: `node tests\e2e\role-requirement-matrix-real-flow.e2e.js --check`；该脚本硬编码写入 `doc/tasks/20260801-role-requirement-matrix-implementation` 和前端 `test-results/role-requirement-matrix-real-flow`，为避免修改非本任务证据目录，本轮只记录其源码声明的 `RRM_*` 前置缺口。

## Merge To int_main 2026-08-05

- User request: 用户要求将 AC-M20 修复融合进 `int_main`。
- Merge strategy: AC-M20 分支基点 `e7c27613` 已是 `int_main` 祖先，`int_main` 后续已有大量并行提交；为避免直接 merge 旧分支带入非 AC-M20 差异，采用先提交 AC-M20 自有改动、再 cherry-pick 该提交到 `E:\IntRuoyi` 的 `int_main`。
- Boundary: `E:\IntRuoyi` 当前存在非 AC-M20 脏文档改动，融合过程只暂存/提交 AC-M20 相关路径，不回滚、不删除、不提交并行任务文件。

## Worktree And Git

- 主工作区启动状态：`int_main` 已有未提交并行改动且 ahead 7。
- 隔离 worktree：`D:\IntRuoyiWorktree\worktree_20260805_ac_m20_pqc_review`，分支 `codex/ac-m20-pqc-review-fix-20260805`。
- Slot reservation: RESOLVED，当前任务已预约 `int_main slot 2`，前端 `8083`，后端 `48083`。

## Blockers

- 真实写入型 Playwright E2E 当前受任务专用 `RRM_*` 测试租户、账号、签名 ID 和业务数据前置缺失阻塞；不能用 API-only、静态扫描、旧任务结果或 admin 基线数据替代。
- 标准未定向 MES testCompile 仍存在历史无关失败，涉及 `MesWmTransferManualWriteControllerTest`、`MesQaInspectionRegulationServiceTest`、`MesP0BatchRecordBackfillClosedLoopTest`、`MesTeamLeaderBatchRecordBackfillServiceTest`、`MesTeamLeaderOrderProcessCompletionServiceTest`；这些文件当前未被本任务修改，未纳入 AC-M20 聚焦完成门禁。

## Resume 2026-08-05 14:33

- Intent: 用户要求继续 AC-M20 修复验收。
- Skill gates: loaded `bug-regression-fix-loop` and `independent-verification-gate`; bug evidence contract requires root cause, RED/GREEN, regression scope, blockers and follow-up.
- Runtime gate: previously full `int_main` slot pool was cleaned; slot 1 has since been occupied by `20260805-production-personnel-management`, so AC-M20 reserved the next available slot.
- GREEN: `pwsh -NoProfile -File scripts/runtime/reserve-worktree-slot.ps1 -Name worktree_20260805_ac_m20_pqc_review -Path D:\IntRuoyiWorktree\worktree_20260805_ac_m20_pqc_review -Branch codex/ac-m20-pqc-review-fix-20260805 -Profile int_main -AsJson` -> PASS, assigned `slot=2`, frontend `8083`, backend `48083`.
