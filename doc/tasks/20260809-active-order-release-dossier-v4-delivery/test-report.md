# Test Report

当前可执行范围的独立测试已完成，但真实 fixture、跨角色 Playwright 和最终集成验收仍被正式前置阻塞。TC-A2-01、TC-A3-01、TC-A4-01、TC-A5-01、TC-A1-01 已有聚焦证据；TC-A6-01 和 TC-INT-03 仍未完成。

## 并行波次主审结果

- A3 `approved`：已将当前批次任务匹配从 plan 移到 write 的首个写入前门禁；`batchExecutionId=null` 时 side-effect-free plan 仍可完成，错配 task 时不调用 backfill。
- A4 `approved`：正式 `NUMBER` 实测值映射为 `BigDecimal`，`CHOICE/BOOLEAN/STRING` 保留正式字符串；新增 CHOICE plan 回归覆盖。
- A5 `approved`：损耗明细已改为每条原因都必须与正式反馈原因快照精确匹配；额外伪造原因回归覆盖通过。
- A5 完成性复审更正：checker 实际已按每类至少一条、逐条校验证据实现；新增同类多工序文档测试通过，撤销此前“恰好一条”的中间判断。
- 统一串行 GREEN：`mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossSourceReaderTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS；Tests run: 37, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。

## A2-INTEGRATE 主审结果

- A2 `approved`：按固定顺序完成组长授权/锁定、正式双 100、三 writer plan、batch、三 writer write、完成性、precheck、RELEASE_APPROVE 和 PENDING 回执；BLOCKED 通过生成事务回滚后的 `REQUIRES_NEW` 独立保存。
- 主审 RED 锁定并修正 6 项合同缺陷：请求幂等不得绕过当前组长授权；请求键 128/备注 500 长度前置；业务回执严格核对正式快照；跨资料复用的相同 `signatureId` 全局去重。
- canonical hash 覆盖正式主数据、writer 来源/绑定/映射/签名和 RELEASE_APPROVE 候选，具备集合顺序、decimal scale、秒级时间稳定性及正式值变化回归。
- GREEN：A2 聚焦 18/18；A2+A3+A4+A5 串行回归 55/55，Failures: 0, Errors: 0，BUILD SUCCESS。

## A1 主审结果

- A1 `approved`：申请请求严格只发送 `activeOrderId/idempotencyKey/applyRemark`；正式 `BLOCKED` 定位字段完整展示；写入成功、响应不确定和列表刷新失败状态分层且不吞错误。
- 主审 RED 锁定并修正列表请求成功但未投影本次正式 `result.status` 时错误解锁的问题；当前必须等同一 `activeOrderId` 的列表状态与正式回执一致后才解除本地重复申请锁。
- GREEN：任务专用静态合同、相邻工作台合同和 SFC scoped style 编译合同通过；并发任务修正非 A1 类型错误且不再有重叠 `vue-tsc` 进程后，主 Agent 重新执行完整 `pnpm ts:check`，退出码 0。
- 同一页面与 API 文件中的活跃订单上下移动属于另一并发任务；A1 未修改、回退或把该并发功能计入本任务验收。

## A6 前置门禁

- A6 `STRUCTURED_BLOCKED / PRECONDITION`：8081/48081 运行基准和 Playwright 工具链通过。
- 全库没有任一工序同时具备非空传统 `MAIN + PROCESS_INSPECTION + LOSS_REPORT` report ID；现有过程检验/损耗仅有动态 `form_slot_type + form_template_id`，不得替代。
- 历史账号/签名授权数据行存在，但五类业务账号登录凭据和签名口令未注入并证明可用。
- 按 fail-fast 未启动写入型 Playwright：真实业务测试 0、manifest 0、业务 ID 0、A6 残留 0。
- A6 已新增可执行 `active-order-release-dossier-v4-preflight.cjs` 及静态合同；主审 RED 修正 backend base URL 缺失、BIT(1) 读取、凭据自设长度和 report ID 格式后，静态合同与两个 `node --check` 均 PASS。
- 主 Agent 独立复验实际 preflight：结构化输出为 `BLOCKED/MISSING_EXPLICIT_ENV`、缺失变量名 27 项、`canRunRealE2E=false`，进程 exit 2；不输出秘密值，四项副作用均为零。
- 该 preflight 只证明 fail-fast 门禁可执行，不证明 TC-A6-01 或 AC-01 至 AC-15 的真实业务链路通过。

## Independent Gate

- Verdict: `BLOCKED / NOT COMPLETE`。
- A1-A5 聚焦行为已完成；AC-01 至 AC-15 的真实跨角色页面验收、AC-12 负责人终态、AC-14 真实重复计数和 AC-15 manifest 仍无证据。
- 当前前端集成复验：A6 静态合同、A1 专用合同、相邻工作台合同、SFC style 合同和 `pnpm ts:check` 全部 PASS。
- 后端最终稳定窗口串行重跑 55/55 PASS，Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS；此前共享 `target` 并发污染 caveat 已关闭。
- 详见 `verification-report.md` 与 `a6-prerequisite-gate-evidence.md`。

## P7/A6 独立阻塞复核

- 独立测试结论：`RELEASED BLOCKER EVIDENCE`。该结论只放行“当前无法进入首次业务写入”的阻塞证据；P7/A6 里程碑仍为 `BLOCKED / NOT COMPLETE`，不得解释为 fixture、真实 E2E 或 AC-01 至 AC-15 已通过。
- P7-AC1 `NOT COMPLETE`：未建立任务自有正式 fixture，真实历史、自然双 100%、申请、三资料和负责人处理均未执行。
- P7-AC2 `NOT COMPLETE`：精确 fail-fast 证据有效且未记录假 PASS，但正式 manifest 为 0、业务 Playwright spec 为 0、真实页面测试为 0、最终只读业务断言为 0，不满足“manifest 完整且真实页面/最终只读断言全绿”。
- P7-AC3 `NOT COMPLETE`：`task-state.json` 保持 `status=blocked/current_phase=P7/test_status=running`；P7 三项 AC 为 `0 completed / 3 pending`，blocking prerequisites 为 5 项。
- 结构一致性：`P7 A6 Blocking Prerequisites Recheck 4` 含 5 条编号 blocker，并明确 `browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`；23 工序来源审查矩阵为 23 行，仍明确 MAIN `20/23`、PQC 直接 `9/23` 及三组待 QA 唯一拆分。
- 产物一致性：任务目录无正式 manifest；同名前端 E2E 仅存在 2 个 preflight 文件，无业务流 spec。既有 `a6-preflight-blocked.json` 可解析为 `BLOCKED/canRunRealE2E=false`，缺失环境变量 27 项，四类副作用均为零。
- 命令：`node --check tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> PASS；`node --check tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；语法检查 `2/2`。
- 命令：`node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；静态合同 `1/1`。
- 命令：`node tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> 预期 `BLOCKED/MISSING_EXPLICIT_ENV`，`NODE_EXIT_CODE=2`，缺失变量 27 项，`canRunRealE2E=false`，四类副作用为零；这是前置阻塞验证，不计业务 FAIL 或 PASS。
- 本轮未注入凭据、未运行真实业务 Playwright、未发出业务/API/SQL 写请求，也未连接数据库重跑查询。数据库 blocker 只独立核对现有只读证据与五项 blocker 口径；由于可执行 preflight 在环境校验阶段已 fail fast，本轮不得把旧数据库证据或静态合同冒充实时数据库/页面验收。
- 计数：语法 `2 PASS`，静态合同 `1 PASS`，实际 preflight `1 STRUCTURED_BLOCKED`，P7 AC `0/3 complete`，正式 manifest `0`，业务 E2E spec `0`，真实业务测试 `0`，最终只读业务断言 `0`。

## P7/A6 压力泵目标独立阻塞复核

- 独立测试结论：`RELEASED BLOCKER EVIDENCE`。该结论只放行“压力泵目标当前不能进入首次业务写入”的阻塞证据；P7/A6 仍为 `BLOCKED / NOT COMPLETE`，TC-A6-01、TC-INT-03、正式 fixture manifest、真实业务 E2E 和最终只读业务断言均未完成。
- 目标纠偏核对：`execution-log.md#P7-A6-Target-Correction-To-Pressure-Pump` 已将导管 route `900025`、products `902231/902252/902262/907242` 和导管 DCC/PQC 证据标记为 `STALE_FOR_CURRENT_P7_TARGET`；当前复核目标为 tenant `1` 的 `922119 / RT000028 / 球囊扩张压力泵 / routeVersionId=627 / V27 / ACTIVE`。
- 压力泵 blocker 计数自洽：product `4` 条绑定中启用 `3` 个，未冻结唯一 product；工序表 `14` 条、V27 snapshot `14` 条但 routeProcessId 口径不同；传统绑定 `MAIN=14 / PROCESS_INSPECTION=2 / LOSS_REPORT=4`，但 PI/LOSS 非空传统 report 为 `0`，三类完整组合 `0/14`；QA regulation `14` 条仅 `1` 条 PUBLISHED 且 item count `3`，其余 `13` 条 RETIRED；三类 source mapping `0/0/0`。
- RELEASE_APPROVE 仅部分通过：route `922119` 有启用规则 `9000253153`，candidate `USER/1/admin` 且 DB 中 admin 启用、电子签名授权 ENABLED、active signature image count `1`；但 release owner 仍必须通过显式 env、真实 UI 登录和签名路径证明，DB 行不能替代凭据可用性。
- 可执行 preflight 复核：`node tests/e2e/active-order-release-dossier-v4-preflight.cjs` 当前返回 `BLOCKED/MISSING_EXPLICIT_ENV`，`NODE_EXIT_CODE=2`，缺 `27` 项显式授权/fixture/五角色登录及签名变量，`canRunRealE2E=false`，`browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`。执行日志压力泵段中 `exit 1` 与当前可复现命令不一致，应按实际命令和后续独立门禁记录的 `exit 2` 解释；不影响阻塞结论。
- `task-state.json` 核对：状态仍为 `status=blocked/current_phase=P7/test_status=running`，P7 AC 为 `0 completed / 3 pending`；但 `blocking_prereqs` 仍保留 route `900025` 导管旧口径且不含 pressure pump `922119`。本轮按授权不得修改 `task-state.json`，因此报告中明确禁止把该 stale blocker list 当作当前压力泵证据来源。
- 产物计数：正式 manifest `0`，业务 E2E spec `0`，同名前端 preflight 文件 `2`，既有 `a6-preflight-blocked.json` 可解析为 `BLOCKED/canRunRealE2E=false/missingEnv=27/四类副作用=0`。
- 命令：`node --check tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> PASS；`node --check tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；语法检查 `2/2`。
- 命令：`node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` -> PASS；静态合同 `1/1`。
- 命令：`node tests/e2e/active-order-release-dossier-v4-preflight.cjs` -> `STRUCTURED_BLOCKED/MISSING_EXPLICIT_ENV`，`NODE_EXIT_CODE=2`，缺失 env `27`，四类副作用 `0`；这是前置阻塞验证，不计业务 PASS 或 FAIL。
- 本轮没有运行 SQL/API/UI 业务写，没有注入秘密变量，没有创建或修改 fixture/manifest/业务 ID，也没有修改 `task-state.json`、`development-plan.md`、`execution-log.md` 或产品代码。
