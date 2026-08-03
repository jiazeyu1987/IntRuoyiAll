# P0 生产执行主闭环 TDD 计划

## Purpose and Scope

本文档把 P0 主闭环 BDD 场景映射为严格 TDD 顺序。实现阶段必须先写失败测试并记录 RED，再做最小正式实现并记录 GREEN；本文档设计阶段不修改生产代码。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/scope-contract.md`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesProFrontlineFeedbackSubmitServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesFrontlinePqcContextServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderReportConfirmationServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderBatchRecordBackfillServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/.../MesTeamLeaderTraceServiceImpl.java`
- `IntRuoyiFronted/package.json`

## Command Conventions

- 后端 Maven 命令必须在 `E:\IntRuoyi\IntRuoyiBackend` 作为工作目录执行，或显式使用 `mvn -f IntRuoyiBackend/pom.xml ...`。
- 前端 pnpm 命令必须在 `E:\IntRuoyi\IntRuoyiFronted` 作为工作目录执行；若使用 `pnpm --dir` 失败，按 `docs/e2e-rules.md` 改为显式工作目录复核。
- 当前 `IntRuoyiFronted/package.json` 尚未包含 `e2e:p0-production-execution-loop:static` / `real` 脚本；实现任务必须先把缺脚本记录为 RED，再新增脚本和正式 spec。
- PowerShell 不得使用 `&&` 串联命令；每条 RED/GREEN 必须单独记录退出码。

## TDD Sequence

| Step | 闭环段 | RED Commands | Expected Failures | 最小 GREEN 目标 | GREEN Commands | Refactor Checks |
| --- | --- | --- | --- | --- | --- | --- |
| P0-T00 | 实现前置门禁 | `workdir=IntRuoyiFronted; python -X utf8 -c "import json,pathlib; s=json.loads(pathlib.Path('package.json').read_text(encoding='utf-8'))['scripts']; assert 'e2e:p0-production-execution-loop:static' in s and 'e2e:p0-production-execution-loop:real' in s"` | 当前前端缺 P0 专用脚本或 spec，不能启动真实 E2E。 | 新增正式脚本、static spec、real E2E spec，并在实现任务记录脚本入口 PASS。 | 同 RED 命令 PASS。 | 不新增空脚本、假脚本或 API-only wrapper 冒充真实 E2E。 |
| P0-T01 | 主提交幂等 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest" test` | 一线主提交请求缺少主提交级幂等键或重复请求产生重复事件。 | 生产提交按提交幂等键或签名唯一约束保证只形成一条有效主事件。 | 同 RED 命令 PASS。 | 不把记录本幂等字段当成整个闭环幂等；不得吞掉重复请求。 |
| P0-T02 | 生产提交闭环合同 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionSubmitClosedLoopContractTest#shouldCreateFeedbackRecordbookAndProcessPoolEventInOneTransaction" test` | 不能同时证明报工、记录本和工序池事件同事务写入并互相关联。 | 返回 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、`processPoolEventId`，且任一失败整体回滚。 | 同 RED 命令 PASS。 | 不用前端串联接口模拟事务。 |
| P0-T03 | PQC 入工序池事件 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldCreateProcessPoolEventWhenSubmittingPqcInspection" test` | 当前实现保存 PQC 任务和逐件明细，但未调用工序池事件服务。 | PQC 提交创建或绑定 `PQC_INSPECTION` 工序池事件，并关联 PQC 任务、规程、逐件明细和签名。 | 同 RED 命令 PASS。 | PQC 事件不得旁路保存成孤立质量数据。 |
| P0-T04 | PQC 质量门禁 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0PqcQualityAllocationGateTest" test` | FIFO 可消费失败、待检或质量状态无法确认的数量。 | FIFO 分配前必须确认质量状态可分配；失败、待检或缺状态均 fail-fast。 | 同 RED 命令 PASS。 | 不用默认合格或前端提示替代后端门禁。 |
| P0-T05 | 复核电子签名 schema | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest" test` | `mes_pro_process_pool_submission_review` 缺复核签名 ID、签名员工和签名快照。 | 复核记录模型和迁移包含正式签名字段。 | 同 RED 命令 PASS。 | 复核签名不能只写备注或登录用户。 |
| P0-T06 | 复核签名服务门禁 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" test` | 无签名仍可复核或确认分配。 | `reviewSubmission` 和 `confirmSubmission` 必须校验复核签名，签名员工等于复核人或正式授权复核人。 | 同 RED 命令 PASS。 | 复核只写复核事实，不修改原提交。 |
| P0-T07 | FIFO 活跃订单闭环 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest" test` | 分配可指向非活跃订单，或总数与确认数量不一致。 | FIFO 和手工分配都只允许活跃生产工单，且总数、剩余数量、当前工序一致。 | 同 RED 命令 PASS。 | 不回退到排产、创建时间或非活跃订单。 |
| P0-T08 | 工序完成批记录回填 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0BatchRecordBackfillClosedLoopTest" test` | 完成后无法证明回填来自正式逐工序绑定和字段映射。 | 完成触发正式批记录执行和字段审计，缺绑定或映射时阻塞。 | 同 RED 命令 PASS。 | 禁止使用 `formBindings`、默认 `MAIN` 或空批记录替代。 |
| P0-T09 | 统一闭环 trace 后端 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest" test` | 现有 trace 分散，无法按事件返回提交、质量、复核、分配、完成、批记录字段审计。 | 新增或扩展 trace 服务，按 `processPoolEventId` 返回完整结构化闭环。 | 同 RED 命令 PASS。 | trace 只读，不执行分配、复核、回填或修改。 |
| P0-T10 | trace 缺投影阻塞 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceFailureTest" test` | 批记录字段审计缺失时仍返回追溯成功。 | 缺关键投影返回明确阻塞状态和缺失字段。 | 同 RED 命令 PASS。 | 不用空列表或摘要文案冒充追溯完成。 |
| P0-T11 | 前端静态合同 | `workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:static` | 缺脚本、缺真实入口、缺签名字段、缺 trace UI 或错误展示。 | 页面和 API wrapper 暴露生产提交、PQC 提交、复核签名、FIFO 确认和 trace 入口。 | 同 RED 命令 PASS。 | 前端不得本地拼接越权数据。 |
| P0-T12 | 真实 E2E | `workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:real` | 缺测试租户、账号、签名、活跃订单、PQC 任务、批记录绑定或 trace 入口时 BLOCKED。 | Playwright 走真实页面完成完整闭环并写入证据。 | 同 RED 命令 PASS 或 BLOCKED 记录正式前置。 | API 只用于只读核验和清理证据。 |

## RED Commands

实现阶段必须记录以下类型的 RED 证据：

```text
RED: workdir=IntRuoyiFronted; python -X utf8 -c "<script existence check>" -> FAIL, P0 前端脚本尚未登记
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldCreateProcessPoolEventWhenSubmittingPqcInspection" test -> FAIL, PQC 正式提交尚未创建工序池 PQC 事件
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureServiceTest" test -> FAIL, 班组长复核尚未要求电子签名
RED: workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest" test -> FAIL, 尚无按 processPoolEventId 聚合的生产执行闭环 trace
RED: workdir=IntRuoyiFronted; pnpm e2e:p0-production-execution-loop:static -> FAIL, P0 trace 页面或签名字段尚未接入
```

## Expected Failures

- 当前 PQC 提交测试应先失败在“未创建或绑定工序池事件”。
- 当前复核签名测试应先失败在 schema、VO 或服务层缺复核签名字段。
- 当前统一 trace 测试应先失败在缺少单一聚合服务或缺少 PQC/复核/批记录字段审计投影。
- 当前主提交幂等测试应先失败在缺少主提交级幂等键或重复请求防护。
- 当前前端 P0 脚本检查应先失败在 package scripts 缺失；这是 M0 前置 RED，不是业务链路 PASS。
- 缺少正式批记录绑定、字段映射、电子签名、质量可分配状态时，应失败为明确 blocker。

## GREEN Commands

P0 第一版实现完成后至少运行：

```powershell
cd E:\IntRuoyi\IntRuoyiBackend
mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesP0ProductionSubmitClosedLoopContractTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesP0PqcQualityAllocationGateTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest,MesP0TeamLeaderReviewSignatureServiceTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0ActiveOrderFifoClosedLoopTest,MesP0BatchRecordBackfillClosedLoopTest" test
mvn -pl yudao-module-mes -am "-Dtest=MesP0ProductionExecutionTraceServiceTest,MesP0ProductionExecutionTraceFailureTest" test
cd E:\IntRuoyi\IntRuoyiFronted
pnpm ts:check
pnpm e2e:p0-production-execution-loop:static
pnpm e2e:p0-production-execution-loop:real
```

## Refactor Checks

- 不新增 fallback、默认成功、默认员工、默认设备、默认质量合格、默认签名或 mock 成功。
- 生产提交、PQC 提交、复核、分配和批记录回填都必须有正式结构化 ID 关联。
- 复核、审核副本、原始 revision、批记录回填是不同写链路，不得互相覆盖。
- 统一 trace 只能读取和聚合，不得带写副作用。
- 批记录表单来源只能是工序设置中的正式逐工序批记录表单绑定。
- 前端只展示后端授权结果，不拿全量数据后本地隐藏。

## Evidence Log Template

```text
BDD: <场景名> -> Given/When/Then 摘要
RED: <命令> -> FAIL, <预期失败原因>
GREEN: <命令> -> PASS
E2E: <命令> -> PASS/BLOCKED, frontend=<url>, backend=<url>, tenant=<label>, dataPrefix=<prefix>
BLOCKER: <缺失正式前置> -> <影响和解除条件>
```

## Test Blockers

- 缺少真实电子签名能力或签名测试账号时，生产提交、PQC 提交和复核链路阻塞。
- PQC 任务、QA 规程快照或逐件明细模型不可用时，PQC 入池链路阻塞。
- 复核签名 schema 未完成时，班组长复核链路阻塞。
- 统一 trace 无法读取批记录字段审计投影时，P0 追溯链路阻塞。
- 前端无真实入口、菜单权限、路由、按钮或脚本时，真实 E2E 阻塞。
