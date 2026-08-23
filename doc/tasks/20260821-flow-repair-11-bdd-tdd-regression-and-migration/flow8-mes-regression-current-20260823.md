# 当前主线全 MES 回归归属矩阵（2026-08-23）

## 证据

命令：`MAVEN_OPTS=-Xms256m -Xmx2048m -XX:MaxMetaspaceSize=512m -XX:ReservedCodeCacheSize=128m -XX:CICompilerCount=2 -Xss512k`，bundled Maven `3.9.16`，`mvn.cmd -o -pl yudao-module-mes test '-Dsurefire.failIfNoSpecifiedTests=false'`。退出码 `1`，Surefire 已进入测试阶段，未发生 native-memory 崩溃。该全量工件和下方 314 条矩阵生成于旧 HEAD `a6574c3631dfa3c5f8381596fcef5c91acd98db0`，早于当前主线的 Flow7 `7770f36fb` 与 Flow10 `af4c6d4d1` 提交，不能作为当前 HEAD 的完整计数。

当前 XML 聚合为 `3643 tests / 56 failures / 258 errors / 18 skipped`，因此 failure/error 总数为 **314**，不是此前消息中的 202。202 只能作为过期的 `7F+195E` 基线差异保留，不能伪造为本次结果。矩阵按测试类聚合；每类的 failure/error 均继承该类的首个根因、owner 和级联判断。

## 汇总

| primary | 数量（F/E） | 责任流程/owner | 对流程8级联 | 处理建议 |
|---|---:|---|---|---|
| `F4/F6` | 235（26/209） | 流程4/6：回填、批次、批记录、任务门禁 | 是，若影响 receipt、BATCH_* 或正式批记录来源 | 先修正式回填/建批契约和测试 fixture，再重跑原类 |
| `F7/F10` | 历史 3（3/0）；当前定向 0（0/0） | 流程7/10：Origin/TraceLink、放行前映射/最终化 | 历史失败曾形成条件阻断；当前两类定向测试已通过 | 由流程7/10 owner 继续提供全量回归和真实链路证据 |
| `PAR` | 76（27/49） | 前线、排产/路线、反馈、流程池及测试基础设施 | 条件；仅在成为来源/签名前置时升级 | 对应并行 owner 修复，不由流程8线程代改 |
| `F8-GATE` | 0（0/0） | 流程8四材料/流程10最终放行 | 无直接 gate 失败证据 | 保留四节点定向绿证，不宣称全链路通过 |

## 测试类矩阵

| 测试类 | F/E | 首个根因 | 责任流程/owner | 级联流程8 |
|---|---:|---|---|---|
| `MesProEdhrBatchExecutionServiceTest` | 0/167 | Spring 缺少 `MesBatchExecutionEntryContractService` bean | 流程6/批次 owner | 是 |
| `MesProBatchRecordReportServiceImplDbTest` | 24/32 | Word 导入前置 DCC 项目代码/fixture 无效 | 流程6/批记录 owner；测试 fixture | 条件 |
| `MesProFeedbackServiceImplTest` | 0/18 | H2 缺 `loss_reason_id` 列 | 流程5/反馈 owner；schema fixture | 条件 |
| `MesProRouteScheduleConfigServiceTest` | 0/14 | 候选路线版本快照不完整 | 并行排产/路线 owner | 条件 |
| `MesProEdhrBatchExecutionTaskGateTest` | 0/7 | 反射目标 `resolveTaskGate` 不存在 | 流程6 owner | 是 |
| `MesP0TeamLeaderReviewSignatureServiceTest` | 5/2 | 预期错误码与实际签名校验码不一致 | 流程2/3/4相邻 owner | 是 |
| `MesP0PqcQualityAllocationGateTest` | 4/2 | PQC分配门禁错误码/fixture不一致 | 流程3/4相邻 owner | 是 |
| `MesProScheduleOrderControllerTest` | 0/5 | `scheduleIssueMapper` 未注入 | 排产 owner；测试 fixture | 条件 |
| `MesP0ActiveOrderFifoClosedLoopTest` | 2/1 | 活跃订单 FIFO 错误码不一致 | 流程4/前线 owner | 是 |
| `MesProAutoScheduleServiceImplTest` | 3/0 | 排产结果数量断言不一致 | 排产 owner | 条件 |
| `MesTeamLeaderActiveOrderErpPlannedStartTest` | 0/3 | 活跃订单缺生产系数/目标快照 | 流程4/ERP owner | 是 |
| `MesProEdhrTraceTerminalPartitionContractTest` | 历史 2/0；当前定向 0/0 | 历史终态追溯分区断言失败；当前提交后定向复验通过 | 流程7/10 owner | 历史是；当前定向否 |
| `MesProScheduleOrderPreflightServiceTest` | 2/0 | 预检得到 WARN 而非 PASS | 排产 owner | 条件 |
| `MesProcessPoolProductionReportRevisionPolicyTest` | 0/2 | Mockito 存在不必要 stub | 测试基础设施/流程池 owner | 否 |
| `MesP0BatchRecordBackfillClosedLoopTest` | 1/1 | 回填身份哈希与预期不一致 | 流程4/6 owner | 是 |
| `MesProRouteFlowConfigServiceImplTest` | 1/0 | 路线配置错误码不一致 | 路线 owner | 条件 |
| `MesProScheduleOrderAdmissionTest` | 0/1 | routeProcessId 生产系数为 0 | 排产 owner | 条件 |
| `MesProRouteVersionPlatformAdapterTest` | 0/1 | 缺 route projection result | 路线 owner | 条件 |
| `ProcessPoolTimelineFilterTest` | 1/0 | 时间线 ID 断言偏移 | 流程池 owner | 条件 |
| `MesProcessPoolTeamLeaderControllerTest` | 1/0 | Long/String 类型不一致 | 流程池 owner | 条件 |
| `MesProcessPoolReviewCopyServiceTest` | 1/0 | 期望业务异常却得到 SQL 异常 | 流程池/schema owner | 条件 |
| `MesFrontlineRuntimeConfigProcessScopeTest` | 1/0 | 运行时设备参数快照校验缺失 | 前线运行时 owner | 是（签名前置） |
| `MesProFeedbackImportRecordServiceImplTest` | 1/0 | mapper 调用次数与 stub 不一致 | 流程5/反馈 owner | 条件 |
| `MesFrontlineActiveOrderInitialAllocationContractTest` | 1/0 | 初始分配未在成功前持久化 | 流程4/前线 owner | 是 |
| `MesProBatchRecordWordParserOwnershipContractTest` | 1/0 | parser helper 反射清单不一致 | 流程6/批记录 owner | 条件 |
| `MesProBatchRecordRouteIdentityContractTest` | 历史 1/0；当前定向 0/0 | 历史 Word 导入未从 DCC 正式关系定位产品；当前提交后定向复验通过 | 流程7/来源 owner | 历史是；当前定向否 |
| `MesProEdhrWorkTaskLegacyProcessTest` | 0/1 | Mockito 不必要 stub | 流程6 owner/测试 fixture | 条件 |
| `MesProEdhrBatchExecutionLegacyProcessTest` | 0/1 | Mockito 不必要 stub | 流程6 owner/测试 fixture | 条件 |
| `MesProcessPoolSchemaTest` | 1/0 | 流程池 schema 断言失败 | 并行流程池 owner | 条件 |
| `MesC015RouteDccQaReconciliationSchemaTest` | 1/0 | 路线-DCC-QA schema 断言失败 | 路线/QA并行 owner | 条件 |
| `ProcessPoolTimelineDateFilterTest` | 1/0 | 日期过滤 ID 断言偏移 | 流程池 owner | 条件 |
| `MesProScheduleOrderServiceImplTest` | 1/0 | 排产服务布尔断言失败 | 排产 owner | 条件 |

## 级联与交付边界

1. `F4/F6=235` 中，批次执行 bean 缺失、任务门禁、正式回填和批记录来源问题是流程8的上游条件阻断；Word/H2/Mockito 等纯 fixture 问题先由测试基础设施处理，不能改写为业务 GREEN。
2. 历史 `F7/F10=3` 是放行前来源映射/终态追溯阻断；当前 HEAD 定向复验为 0/0，但尚未替代全量回归、真实映射和唯一 `RELEASED` 证据。即使四份材料上传，也不得绕过 `TRACE_MAPPING_BLOCKED` 或唯一 `RELEASED`。
3. `PAR=76` 不属于流程8材料 gate；仅当前线签名、正式来源或路由快照是流程8前置时才升级为级联阻断。
4. 本矩阵不使用 mock、API-only、直接 SQL、默认成功或跳过测试替代业务验证；只记录真实 Maven/Surefire 结果。
5. 建议由流程4/6/7/8/10及相邻 owner 分别消费本表对应行，修复后用原测试类命令回归；流程11只维护分类、runner 和证据，不代改业务所有权代码。

## 当前 HEAD 定向复验

- 当前 `int_main` HEAD：`af4c6d4d1f0febd987a0f652ccbd085f266ea490`（包含 Flow7 `7770f36fb`、Flow10 `af4c6d4d1`）。
- 命令：`$env:MAVEN_OPTS='-Xms256m -Xmx1536m -XX:MaxMetaspaceSize=384m -XX:CICompilerCount=2'; mvn.cmd -o -pl yudao-module-mes '-Dtest=MesProEdhrTraceTerminalPartitionContractTest,MesProBatchRecordRouteIdentityContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`。
- 结果：退出码 `0`；`MesProEdhrTraceTerminalPartitionContractTest` 2/0/0，`MesProBatchRecordRouteIdentityContractTest` 2/0/0，合计 `4 tests / 0 failures / 0 errors / 0 skipped`。
- 本次只校正 Flow7/10 两行；其余历史 311 条 F/E 未在当前 HEAD 重跑，不能将历史 314 条宣称为当前全量结果。`F8-GATE=0` 保持；流程1-10全链路、真实 Playwright、生产迁移、人工批准/回滚仍为 No-Go。
