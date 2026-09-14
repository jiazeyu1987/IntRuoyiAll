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

## P7/V29 主 Agent 复核

- 结论：`BLOCKED / NOT COMPLETE`。共享运行态当前最新压力泵版本已是 `632/V29/ACTIVE`，不是此前的 V28/V27；P7 状态已同步到 V29 当前事实。
- 合同复核：已回退 P7 中断留下的 formBinding writer/test 合同偏移；四个 writer 相关文件当前对 `FORM_BINDING_WRITER_REQUIRED`、template `28/25` 常量和新增 helper 的 diff 为 0。当前测试计划仍明确不允许 dynamic formBindings 作为 batch record source。
- 数据复核：V29 当前 14 个工序拥有 `MAIN 14/14`、`PROCESS_INSPECTION form_template_id=28 14/14`、`LOSS_REPORT form_template_id=25 14/14`；但 PI/LOSS 传统 `batch_record_report_id` 仍为 `0/14`，传统三类完整组合仍为 `0/14`。
- 其它 blocker：启用产品仍为 `901965/902149/924005` 三选一未冻结；V29 QA regulation 为 `0/14`；三类 source mapping 总数为 `0`；release owner 仍缺真实 UI 登录/签名证明。
- 验证命令：`node --check tests/e2e/active-order-release-dossier-v4-preflight.cjs` PASS；`node --check tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` PASS；`node tests/e2e/active-order-release-dossier-v4-preflight-static.spec.cjs` PASS；completion gate 仍 FAIL 于 P7 未完成和 blockers 非空。
- 补充产品证据：现有 route `922119` active orders 中，启用产品唯一命中 `902149 / AW.107.02.01.2010`；V29 下 active order count 为 `0`，因此该证据只能作为推荐冻结对象，不能替代 V29 task-owned fixture 创建和真实 E2E。

## P3/A4 动态过程检验主审结果

- P3 `approved`：PROCESS_INSPECTION 已按用户授权的路线绑定 `form_template_id=28` 作为正式 FormCenter 目标；MAIN 批记录仍走逐工序传统批记录绑定，不被动态表单替代。
- 正式来源对应：产品冻结为 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`；QA reader 必须先解析唯一启用 DCC 项目，再按 `productId + routeId + stable processId` 选择同项目身份的最新 PUBLISHED QA，禁止按名称、项目代码或旧 routeProcessId 猜测。
- 动态写入边界：writer 只写当前 batch task 已关联的 `EDHR_ROUTE_FORM` FormCenter instance，校验 task/binding/template/actionCode/slot hash/业务上下文后保存草稿并提交 EFFECTIVE；返回提交快照 ID、head hash、source hash 和 PQC/复核原始签名证据。
- GREEN：`mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；动态端口 2/2、reader 4/4、writer 12/12，合计 18/18。
- 剩余门禁：P4/LOSS_REPORT template 25 动态写入仍未完成；P7 真实 E2E 仍被运行包未更新、既有 V29 快照 hash 为空、字段映射 0、QA 设备关联缺失和 fixture 未创建阻塞。

## P4 独立验证结果

- 结论：`RELEASED / 16 tests / 0 failures / 0 errors`。
- 命令：`mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；`BUILD SUCCESS`。
- 范围：loss report writer 9/9、dynamic form port 3/3、dossier completeness checker 4/4，合计 16/16；Failures: 0，Errors: 0，Skipped: 0。

## P7 Dynamic Form Mapping Save 独立验证 - 2026-08-11

- 结论：`PASS（本修复）/ BLOCKED（P7整体）`；只读复核确认 `FORM_TEMPLATE_VERSION` scope 下允许 `PQC_AGGREGATE_DETAIL` 与 `PRODUCTION_LOSS` 字段级 sourceType 保存，非模板 scope 仍在保存入口拒绝，`buildFormTemplateVersionPrefillData` 生产工单自动预填仍只接受 `PRODUCTION_WORK_ORDER`。
- 命令：`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；Tests run: 10，Failures: 0，Errors: 0，Skipped: 0。
- 命令：`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseLossReportDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；Tests run: 44，Failures: 0，Errors: 0，Skipped: 0。
- 剩余 P7 blockers：本轮仅验证正式保存入口，不创建 V29 实际字段映射、动态绑定 hash、DCC 产品身份关系、QA 设备关联、任务自有 fixture/manifest 或真实跨角色 Playwright 证据。

## P7 压力泵 DCC/QA/模板身份独立验证 - 2026-08-11

- 切片结论：`PASS / 本实现切片可放行`。P7 整体结论仍为 `BLOCKED / NOT COMPLETE`，不得把本轮单元级身份门禁通过解释为正式 fixture、真实页面流程或最终放行资料已完成。
- 路线 DCC 身份：`PASS`。生产 reader 只解析命令指定的已发布 ACTIVE/SUPERSEDED 路线版本快照，以 `configSnapshots.products[].itemId -> MES item.code` 与唯一启用 DCC `projectCode` 精确相交；测试以 MES productId `102`、DCC `productMasterId=11` 和路线物料代码 `ID` 证明两套 ID 不混用，并证明仅存在 `IDPR` 时不猜测命中。新增损坏已发布快照用例证明 JSON/产品结构无效时抛出 `IllegalStateException`，不伪装成无匹配，也不继续读取下游数据。
- QA 来源：`PASS`。provenance 同时要求 regulation/version 均为 `PUBLISHED`、当前版本与 regulation/tenant 身份闭环、发布时间非空，且 regulation code 精确以 `PQC-ID-` 开头；`PQC-IDPR-001` 与 `M0-PQC-ROUGH-WASH` 均返回 `PQC_DCC_QA_PROVENANCE_REQUIRED`，未进入 verified/latest 候选。
- template 28 身份：`PASS`。动态目标解析先校验精确 PUBLISHED template 28 版本，再从 recognized field 标签提取唯一 `PQC-<projectCode>-*`；expected=`ID`、actual=`[IDPR]` 时返回 `PROCESS_INSPECTION_TEMPLATE_DCC_IDENTITY_REQUIRED`。测试明确验证未查询 FormCenter instance、未调用 `saveDraft`、未调用 `submitInstance`；writer 对带 blocker 的 plan 也在 write 入口拒绝。
- 无 fallback/猜测/副作用：`PASS`。路线项目要求唯一精确交集，损坏快照 fail fast；QA 不使用名称、MES productId、processId 或 M0/IDPR 别名补齐；模板身份缺失、多值或不一致均阻塞。独立测试只运行 mocked 单元测试，未连接运行态/数据库，未写路线、模板、QA、FormCenter 或 fixture。
- 独立命令：`mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest,MesTeamLeaderActiveOrderReleaseProcessInspectionQaProvenancePortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> `BUILD SUCCESS`；Tests run: `25`，Failures: `0`，Errors: `0`，Skipped: `0`（dynamic port 3、QA provenance 3、reader 5、writer 14）。
- 当前数据证据边界：本 tester 按授权未重查 48081 或数据库；“template 28 / version 32 的正式标签为 `PQC-IDPR-001`”来自既有只读 API 证据 `execution-log.md#P7-Pressure-Pump-Template-Identity-Gate-2026-08-11`，本轮以相同 recognized-field 内容独立验证代码阻塞行为，不能表述为实时数据二次复验。
- P7 剩余 blocker：仍需发布并重绑与 DCC `ID` 一致的正式过程检验模板及修正映射；当前 14 个工序中仍有 13 个最新 QA 候选不是 `PQC-ID-*`，且 13/14 缺设备关联；V29 PI/LOSS 动态绑定 record/slot snapshot hash 仍为空；任务自有 fixture/manifest、真实跨角色 Playwright 和最终只读业务断言均未完成。

## P7/A6 当前正式前置门禁独立实时复核 - 2026-08-11

- 阻塞证据结论：`PASS / RELEASED BLOCKER EVIDENCE`。独立实时复核支持 `execution-log.md#P7/A6-Current-Formal-Prerequisite-Gate-2026-08-11` 的四类阻塞和零副作用结论；这只放行 blocker evidence，不放行 P7 完成。
- P7 验收结论：`BLOCKED / NOT COMPLETE`。`P7-AC1`、`P7-AC2`、`P7-AC3` 仍为 `pending`，即 `0/3` 完成；TC-A6-01 与 TC-INT-03 未执行，不能进入下一里程碑或宣称真实放行链路通过。
- 运行态：`127.0.0.1:8081` 实时 HTTP `200`，`127.0.0.1:48081/actuator/health` 实时为 `UP`；监听端口分别存在。仅执行首页/health 只读 GET，未登录、未调用业务 API。
- 路线与 DCC：只读 MySQL 复核 route `922119` 的唯一 `active=true` 版本为 `632 / V29 / ACTIVE`，不存在更高版本；当前工序 `14` 条，ID 为 `9908090160..9908090173`。V29 产品快照含 `902149` 与路线项目物料 `924005/code=ID`；启用 DCC 对 `ID` 精确命中 `1` 条，`IDPR` 是独立启用项目，二者 `productMasterId` 分别为 `11/13`。
- 模板身份：template `28` 最新 PUBLISHED 仍为 `versionId=32 / V3.0`；recognized schema 命中 `PQC-IDPR-001`，不命中 `PQC-ID-*`，与路线 DCC `ID` 不一致。
- QA provenance：按 `productId=902149 + routeId=922119 + stable processId` 选出最新 PUBLISHED QA `14/14`；精确 `PQC-ID-*` 仅 `1/14`，为工序 `9908090162` 的 `PQC-ID-001-RP980647 / versionId=54 / G/0`；其余 `13/14` 为 `RRM-*`，不能作为 DCC `ID` 正式来源。
- QA 设备：设备关联完整工序 `1/14`，不完整 `13/14`，缺少 required-item equipment 关联合计 `44`；逐工序缺口为 `2,2,0,2,2,8,2,2,3,7,8,2,2,2`，与主线证据一致。
- 动态绑定 hash：V29 `PROCESS_INSPECTION` 绑定 `14` 条，record/slot hash 非空均为 `0/14`；`LOSS_REPORT` 绑定 `14` 条，record/slot hash 非空也均为 `0/14`。对应模板版本仍为 `28/32` 与 `25/27`。
- 交付物与副作用：任务目录无正式 fixture manifest；前端仅有 preflight 及其静态合同，没有真实业务 Playwright spec。本轮 `browserBusinessWrites=0`、`businessApiWrites=0`、`sqlWrites=0`、`manifestCreated=false`：未启动浏览器，HTTP 仅 GET，数据库命令仅 `SHOW/SELECT`，未创建或修改业务数据。
- 安全边界：数据库复核使用本机 `int-ruoyi-mysql` 容器既有环境变量，只输出结构、ID、版本和计数；未使用用户口令，未输出数据库密码、token、Cookie、签名口令或连接秘密。
