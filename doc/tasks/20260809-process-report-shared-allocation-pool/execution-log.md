# Execution Log

## 2026-08-09 Task Initialization

- 用户意图：按已确认的工序共享报工池方案完成开发和验证，不增加额外业务限制。
- 命令意图：读取适用规则与技能，建立任务持久化记录；尚未运行构建、测试或数据写入。
- 当前阶段：analysis。
- 变更路径：仅本任务文档。
- 阻塞：无。

## 2026-08-09 Repository Exploration

- 调查范围：生产组长 FIFO 服务、报工确认服务、分配表与 Mapper、报工时间线读模型、活跃订单放行链路、前端报工分配入口。
- 当前行为：FIFO 要求当次报工全部分完；确认仅允许首次写入；提交复核和分配绑定为同一终态；列表未返回分配订单明细；放行终态来自 eDHR 放行事务 `RELEASED`。
- 适用经验门禁：当前工序快照严格来源、写接口页签上下文、运行态迁移契约、一对多读模型聚合。
- 当前阶段：planner_review。
- 阻塞：无。

## 2026-08-09 Planning Gate Review 1

- 结论：needs_revision。
- 已通过：范围、FIFO 顺序、部分分配、未放行调整、正式放行锁定、历史/待处理独立投影、并发和审计均有稳定验收编号。
- 待修订：明确本系统现有 PQC 抽样判定如何生成“大数量报工”的正式合格可分配数量，避免把抽检样本数错误当成生产数量上限；明确同一活跃订单任一正式 eDHR `RELEASED` 事务即可形成不可逆锁定，不能被后续非终态申请覆盖。

## 2026-08-09 Planning Gate Review 2

- 结论：approved。
- PQC 数量口径：唯一正式 PQC 结构化绑定且整次抽样判定为 `SUCCESS` 后，完整 `outputQuantity` 进入共享池；抽检样本数不作为产量上限，损耗不二次扣减。
- 放行锁定口径：任一未删除正式放行申请关联 eDHR `RELEASED` 事务即永久锁定；后续非终态申请不得覆盖或解锁。
- 迁移口径：既有有效分配逐行生成 `INITIAL_BASELINE` 审计；放行状态实时查询正式事务，不持久化 release snapshot。
- 当前阶段：plan_decomposition。
- 阻塞：无。

## 2026-08-09 Development/Test Plan Gate

- 结论：approved。
- 任务图：DB-01 -> BE-01 -> BE-02 -> (BE-03, BE-04) -> FE-01 -> E2E-01 -> VERIFY-01。
- 验收覆盖：AC-01 至 AC-18 全部映射到 schema、后端、前端或真实页面测试；每个生产代码任务均定义行为 RED、最小 GREEN 和相邻回归。
- 冲突约束：当前工作区已有大量用户/并行任务改动；执行者只能在现状上做任务范围内的增量修改，不得清理、覆盖或回退无关改动。
- 当前阶段：execution_db_01。
- 阻塞：无。

## 2026-08-09 DB-01 BDD

- BDD: 分配版本、生命周期和迁移基线 -> Given 迁移前存在零分配和既有有效分配报工；When 在满足依赖的数据库执行共享分配池迁移；Then 每个既有有效分配逐行生成唯一 `INITIAL_BASELINE` 审计，当前分配与数量片段具备可替换生命周期和版本，且 schema 不保存 release snapshot。
- RED 目标：先以 `MesReportSharedAllocationSchemaTest` 固化迁移头、字段、索引、DO/Mapper 和 fail-fast 基线合同；失败原因必须是目标合同尚不存在。

## 2026-08-09 DB-01 RED

- 前置波动：首次执行 Maven 时，工作区并行修改中的 `MesFrontlinePqcContextServiceImpl` 与其测试构造器短暂不一致，导致模块 testCompile 失败；该失败未记为业务 RED，也未修改相关文件。源码稳定后原命令可进入本任务测试。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesReportSharedAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 tests，原因分别为共享分配迁移不存在、`MesProcessPoolReportAllocationStateDO` 不存在，符合预期。

## 2026-08-09 DB-01 Static GREEN

- 实现：新增 `20260809_mes_process_pool_report_shared_allocation.sql`；增加事件级 state、分配/数量片段生命周期与版本、调整审计；既有有效分配逐行生成 `INITIAL_BASELINE`。
- 数据安全：迁移只新增表/列/索引；部分列或部分数据回填使用 `SIGNAL`；放行状态没有持久化快照。
- GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportSharedAllocationSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests。
- 环境说明：并行 Maven 曾将共享 `target/classes` 中一项无关 class 写成 0 字节；等待其它 Maven 结束后复跑通过，未删除共享 target、未终止其它进程。
- 待验证：release migration policy gate、database evidence validator、当前本地运行库只读 preflight/迁移。

## 2026-08-09 DB-01 Local Runtime Preflight

- 目标：本机 Docker MySQL `127.0.0.1:23306/ruoyi-vue-pro`，仅本地测试运行库；未访问远端服务器。
- 依赖容器：`int-ruoyi-mysql` 运行中，端口归属 23306。
- 旧表：`mes_pro_process_pool_event`、`mes_pro_process_pool_report_allocation`、`mes_pro_process_pool_fifo_allocation_line` 均唯一存在。
- 依赖缺口：`mes_pro_process_pool_active_order.sort_order` 当前不存在，必须先应用 `20260809_mes_process_pool_active_order_manual_sort.sql`。
- 数据计数：有效旧 report allocation=0，FIFO allocation line=0，孤立 FIFO source event=0；共享迁移不会回填既有分配业务行。
- 编码路径：Node 读取 SQL UTF-8 原始字节并通过 `docker exec -i` 输入容器 MySQL；容器内既有凭据只在 shell 内展开，不写日志。

## 2026-08-09 DB-01 Runtime GREEN

- 迁移顺序：`20260809_mes_process_pool_active_order_manual_sort.sql` -> `20260809_mes_process_pool_report_shared_allocation.sql`。
- GREEN: 同一迁移顺序连续执行两次 -> PASS；第二次无重复或合同冲突。
- 运行库核验：生产报工事件=81，allocation state=81 且版本和=0；有效旧 allocation=0，`INITIAL_BASELINE`=0；新增 allocation/FIFO lifecycle 字段类型与 nullability 符合合同；state/audit 放行快照字段=0；active order `sort_order IS NULL`=0。
- 门禁：release migration policy gate -> PASS，457 migrations；database schema evidence validator -> PASS；task-owned `git diff --check` -> PASS。
- DB-01 状态：completed。
- 剩余阻塞：无。

## 2026-08-09 BE-01 BDD

- BDD: 大数量 PQC 整体合格 -> Given `outputQuantity=411111` 且唯一正式 PQC 结构化绑定判定 `SUCCESS`，抽样数小于 411111；When 读取分配质量基数；Then 返回完整 411111，样本数和损耗不扣减。
- BDD: 正式候选顺序与目标工序 -> Given 活跃订单列表稳定顺序为 B/A/C 且三者自身目标 routeProcess 不同但 `processId` 相同；When FIFO 读取候选；Then 顺序保持 B/A/C，各自使用自己的唯一工序快照，不按异常、产品、来源订单或来源 routeProcess 过滤。
- BDD: 任一历史正式放行永久锁定 -> Given 同一 activeOrder 先有 eDHR `RELEASED`，后有非终态申请；When 读取 release 状态；Then released 始终为 true，后续非终态不得覆盖。

## 2026-08-09 BE-01 RED/GREEN

- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationFoundationContractTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessTargetServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 缺少质量池基数服务、正式放行历史存在性服务和候选自身工序解析方法，FIFO 仍二次排序并调用异常订单过滤。
- 实现：质量门禁在唯一 PQC `SUCCESS` 且所有实际抽样完整成功后返回完整 `outputQuantity`；FIFO 直接保留正式活跃列表顺序且不再过滤异常订单；目标按候选 activeOrder 自身唯一 `processId` 快照解析；放行状态批量检查全部未删除申请关联事务，任一 `RELEASED` 即锁定。
- GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationFoundationContractTest,MesReportAllocationQualityGateServiceTest,MesReportAllocationReleaseStateServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessTargetServiceTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 22 tests。
- BE-01 状态：completed；进入 BE-02 当前快照、部分保存、重分配事务与并发版本。

## 2026-08-09 BE-02 BDD

- BDD: FIFO 尽可能分配并保留余量 -> Given 报工池 300 且 A/B/C 当前工序剩余 100/80/50；When FIFO 预览和保存；Then 保存 230，`unallocatedQuantity=70`，不抛数量不足。
- BDD: 未放行可重分配且已放行锁定 -> Given A=100 未放行；When 人工改为 C=100；Then A 当前行被版本化替换、C 成为当前行并写差额审计；Given A 已正式 `RELEASED`；When 提交调整；Then 整个事务失败且 A 保持不变。
- BDD: 并发版本与幂等 -> Given 两个客户端读取同一版本；When 首个请求保存成功后第二个仍提交旧版本；Then 第二个明确版本冲突；同一 idempotencyKey 和同一请求重试返回同一版本且不重复写审计。
- BDD: 工序范围授权 -> Given 生产组长被授权负责报工所属 `processId`，但报工员工不在其员工范围；When 打开或保存报工分配；Then 按工序授权允许操作，不调用员工范围门禁。
- BDD: 分配碎片与完成量原子重建 -> Given 报工从 A=100 调整为 C=100，A/C 使用同一 `processId` 但不同目标 `routeProcessId`；When 保存新版本；Then 当前数量碎片按新分配重建，A 完成量回到 0，C 完成量变为 100，任一环节失败则整个事务回滚。
- BDD: 空分配保留全部余量 -> Given 报工池 300 且已有未放行 A=100；When 从空白开始保存零条可编辑分配；Then 当前分配为空且未分配余量为 300。

## 2026-08-09 BE-02 Transactional RED

- RED: `mvn -pl yudao-module-mes -am "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationCommandServiceTest,MesReportAllocationQuantityFragmentServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, testCompile 明确缺少 `MesReportAllocationQuantityFragmentService`；同时新增用例已固定命令服务的新依赖合同与跨目标工序完成量回算入口，符合预期。

## 2026-08-09 BE-02 Transactional GREEN

- 实现：生产组长报工分配改按授权 `processId` 判定；保存锁定 event/state、活跃订单、跨报工占用和正式放行事务；未放行行版本化替换，已放行行原样保留。
- 实现：新增 OUTPUT 数量碎片版本重建，明确排除 LOSS 碎片；新版本先完整校验再 supersede/insert，并同步 fragment allocated/available。
- 实现：完成量按受影响订单各自 `(workOrderId, routeProcessId, processId)` 从全部当前有效分配重算；A→C 同时把 A 回算为 0、C 回算为新数量，排产进度和未放行回填来源随事务更新。
- GREEN: `mvn -pl yudao-module-mes -am "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationCommandContractTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesReportAllocationQuantityFragmentServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProcessPoolFifoAllocationConcurrencyTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 43 tests。
- 回归修订：旧 P0 用例按已批准合同改为活跃列表原顺序，并将“任一样本失败”断言为 PQC 不可分配而非样本数量上限；`MesP0ActiveOrderFifoClosedLoopTest` 单类复跑 PASS, 3 tests；`MesP0PqcQualityAllocationGateTest` 与 release orchestration 在组合运行中业务断言通过。
- 环境波动：随后组合复跑时并行任务 Maven 曾把共享 `target/classes/MesTeamLeaderActiveOrderAddReqVO.class` 短暂写成 0 字节；未删除共享 target、未终止对方进程，待最终回归时统一重编译复跑。

## 2026-08-09 BE-03 BDD/RED

- BDD: 正式订单变化退池 -> Given 活跃订单存在未放行当前分配；When 活跃订单移除、工单冻结或取消；Then 在同一事务先退回全部未放行量、重建来源报工碎片和完成量、写 `ORDER_CHANGE` 审计，再改变订单状态。
- BDD: 工单减量只退超额 -> Given A 当前有效分配 100 且正式数量下调为 60；When 保存减量；Then 保留最早 60、退回超额 40，已放行订单不变。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationOrderChangeServiceTest,MesTeamLeaderActiveOrderServiceTest,MesProWorkOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, testCompile 明确缺少 `MesReportAllocationOrderChangeService` 及三个正式入口依赖，符合预期。

## 2026-08-09 BE-03 GREEN

- 实现：`MesReportAllocationOrderChangeService` 按 activeOrder 锁定全部当前分配和正式放行状态；放行订单保持不变，未放行订单按 event 生成新版本、退池、重建 OUTPUT 碎片、回算完成量并写 `ORDER_CHANGE` 审计。
- 实现：活跃订单移除在状态改为 REMOVED 前退池；工单减量保留各目标工序最早有效数量并只退超额；冻结和取消在正式状态变化前退回全部未放行分配；批量冻结逐工单走同一协调器。
- GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationOrderChangeServiceTest,MesTeamLeaderActiveOrderServiceTest,MesProWorkOrderServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 42 tests；新增减量入口用例后 `MesProWorkOrderServiceImplTest` -> PASS, 12 tests。
- BE-03 状态：completed。

## 2026-08-09 BE-04 GREEN

- 实现：报工时间线按 event 批量投影所有当前有效分配、订单编码和正式放行状态；WORKBENCH 仅保留存在余量或未放行分配的报工，HISTORY 永久保留全部生产报工。
- GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineReportAllocationProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 8 tests。
- BE-04 状态：completed；进入 FE-01。

## 2026-08-10 FE-01 BDD/RED/GREEN

- BDD: 报工分配列与编辑锁 -> Given 报工存在已放行和未放行分配，When 进入报工管理并打开分配，Then 列表显示每个订单和数量，已放行项绿色且锁定，未放行项可调整。
- BDD: FIFO 草稿与空白手工 -> Given 报工存在可分配余量，When 点击 FIFO 或从空白开始，Then 用户可修改未放行草稿并保留未分配余量。
- BDD: 版本冲突显式刷新 -> Given 两个客户端持有同一分配版本，When 旧版本保存被拒绝，Then 页面只加载最新快照并要求用户重新确认，不自动重试。
- RED: `node tests/e2e/team-leader-report-shared-allocation-static.spec.cjs` -> FAIL, 缺分配列/锁定与稳定请求身份、版本冲突显式刷新契约。
- 实现：报工管理增加分配订单/未分配数量；已放行行锁定；FIFO、新增行和从空白开始共用同一草稿；`WORKBENCH/HISTORY` 使用独立视图参数。
- 实现：幂等键按完整请求身份稳定生成；版本冲突码 `1040760357` 只触发显式最新快照加载。
- GREEN: `node tests/e2e/team-leader-report-shared-allocation-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-report-allocation-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-production-report-history-tab-static.spec.cjs` -> PASS。
- GREEN: `pnpm exec eslint src/api/mes/pro/processpool/teamLeader.ts src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue tests/e2e/team-leader-report-shared-allocation-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## 2026-08-10 Allocation Audit BDD/RED/GREEN

- BDD: 分配调整审计查询 -> Given 报工经过 FIFO、手动或订单变化调整，When 组长读取该报工审计，Then 接口按稳定顺序返回模式、版本、变化量、原因和操作人。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 控制器缺分配审计查询方法。
- 实现：新增 `GET /submission/allocation/audit`、稳定响应 VO 和前端 API 类型。
- GREEN: 同一控制器定向测试 -> PASS, 17 tests。

## 2026-08-10 Unified Regression and Runtime

- GREEN: 后端分配基础、质量门禁、FIFO、目标工序、命令/并发/碎片、完成量、订单变化、时间线和 P0 回归组合 -> PASS, 144 tests，0 failures/errors/skips。
- GREEN: `mvn -pl yudao-server -am -DskipTests "-Dmaven.compiler.useIncrementalCompilation=false" package` -> BUILD SUCCESS，所有 Reactor 模块成功，生成 `yudao-server-exec.jar`。
- RUNTIME: 仅停止 `48081` 原 int_main 旧 jar 进程，启动 `backend-report-shared-allocation-20260810-final-20260810-002331.jar`，PID `47284`；`GET /actuator/health` -> `UP`。

## 2026-08-10 Real Playwright E2E

- BDD: 指定大数量报工 FIFO -> Given 事件 `176`、完成数量 `411111`且位于工序共享池，When 生产组长在报工管理点击 FIFO 自动分配，Then 系统应尽可能生成草稿，或明确返回早于分配算法的正式质量数据阻塞，不得写入部分结果。
- BDD: 指定大数量报工手动 -> Given 同一事件的 FIFO 已明确被正式 PQC 绑定门禁拒绝，When 从空白开始、选择活跃订单并手动分配 `1`，Then 确认必须在同一正式门禁失败，分配前后投影不变。
- FIRST RUN BLOCKED: 任务脚本位于根任务目录，Node 无法自动解析前端 `node_modules/playwright`；修正为通过前端 `package.json` 正式解析。
- SECOND RUN BLOCKED: 页面筛选器实际为“新增筛选条件”交互，日期输入未默认展开；按真实 DOM 改为新增“提交日期”并查询。
- THIRD RUN BLOCKED: 打开当前分配快照已返回 `1040760326`；脚本改为保留该真实结果，使用工作台投影前后对比验证无写入。
- GREEN: `node doc/tasks/20260809-process-report-shared-allocation-pool/report-allocation-event-176.e2e.cjs` -> `PASS_WITH_DATA_BLOCKER`。
- FIFO: HTTP 200，`code=1040760326`，“报工确认缺少唯一正式 PQC 结构化绑定，eventId=176”；没有生成分配行。
- MANUAL: 真实页面从空白开始，选择活跃订单 `CODX-AO5-20260807-01`，提交模式 `MANUAL`、数量 `1`；HTTP 200，同样返回 `1040760326`。
- NO WRITE: 前后均为 `outputQuantity=411111`、`unallocatedQuantity=411111`、`allocations=[]`；当前分配门禁响应前后一致；`pageErrors=[]`、`requestFailures=[]`。
- HISTORY: `allocationView=HISTORY` 查询中事件 `176` 仍存在，未分配数量 `411111`，分配订单 0 条。
- BLOCKED: 未提供独立可写测试租户/账号和具备唯一正式 PQC 绑定、工序快照、活跃订单的任务自有样本；未用 admin 基线数据、SQL、API-only 或 mock 完成正向写入 E2E。

## 2026-08-10 Experience Consolidation

- 已按 `project-experience-consolidation` 搜索长期经验归宿。
- `docs/e2e-rules.md#写入型-e2e-任务自有模拟环境门禁` 已覆盖“只有 admin 基线时正向写入必须 BLOCKED、任务自有正式数据及清理闭环”；`docs/login-access.md` 已覆盖可写测试租户/账号要求。
- 本次新信息为事件 `176` 的一次性数据状态，只保留在任务记录；无需重复修改或新建长期经验文档。

## 2026-08-10 Cleanup Preview

- PREVIEW: `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --task-id 20260809-process-report-shared-allocation-pool --mode preview` -> PASS。
- KEEP: `task.md`、`execution-log.md`、`verification-report.md`。
- DELETE: 仅本任务 E2E 脚本/截图/结果、迁移辅助与证据、中间规划文档、任务热补丁 jar。
- BLOCKED/WARNINGS: 均为空；当前主工作区 `int_main`，不执行 worktree 合并或删除。

## 2026-08-10 Cleanup Apply and Final Status

- APPLY: `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --task-id 20260809-process-report-shared-allocation-pool --mode apply` -> PASS。
- 已删除仅属于本任务的 E2E 脚本/截图/结果、迁移辅助与证据、中间规划文档和热补丁 jar；关键验证结论已留在三份核心记录。
- 保留：`task.md`、`execution-log.md`、`verification-report.md`、全部生产代码、迁移和正式测试。
- Git：用户未要求 commit/push，未执行任何 Git 写操作。
- FINAL STATUS: `completed`；正向写入型 E2E 的独立测试租户/正式 PQC 样本前置仍按验证报告记录，未用降级或假成功覆盖。
- POST-APPLY PREVIEW: 仅 keep 三份核心记录，delete/blocked/warnings 均为空。
- FINAL RUNTIME: `48081` 仍由 PID `47284` 的最新任务 jar 监听，`/actuator/health` -> `UP`。
- FINAL STRUCTURE: 任务目录仅剩 `task.md`、`execution-log.md`、`verification-report.md`；三文件 `git diff --check` -> PASS。

## 2026-08-10 Completion Audit Reopen

- STATUS: 完成性审计发现真实成功写入路径尚无权威证据，任务从 `completed` 重新打开为 `in_progress`；原自动化回归与拒绝路径证据继续保留。
- BDD: FIFO 正向部分分配 -> Given 可写测试身份、唯一正式 PQC 成功绑定、当前工序快照和按活跃订单列表排序的候选订单齐备，When 组长从真实页面对大数量报工执行 FIFO 并确认，Then 应按列表先后尽可能分配，超出需求的数量保留在报工池，工作台与历史投影一致。
- BDD: 手动重分配 -> Given FIFO 已保存且相关分配均未放行，When 组长从真实页面把早订单的部分数量调整到后续订单并确认，Then 当前分配、未分配数量和审计应更新；已放行分配不得可编辑。
- BDD: 从空白手动分配 -> Given 报工仍有可分配余量，When 组长从空白开始选择相同工序的活跃订单并保存部分数量，Then 未分配余量继续保留，报工继续出现在工作台且永久可在历史查询。
- PREREQUISITE AUDIT: 当前进程环境仅发现 `PLAYWRIGHT_BROWSERS_PATH`，未发现项目规则要求的可写测试租户/账号凭据键；禁止用默认 admin、SQL 写入、API-only 或 mock 冒充正向 E2E。
- NEXT: 只读核对本地测试租户、账号角色、事件 `176` 的正式 PQC 数据链路以及前端是否存在建立该绑定的真实入口。

## 2026-08-10 PQC Source Context Regression

- BDD: PQC 绑定确切生产报工 -> Given PQC 页面由生产报工事件入口打开且 URL 携带 `productionSubmitEventId` 或既有别名 `processPoolEventId`；When 检验员通过真实页面正式提交 PQC；Then 前端请求携带同一 `productionSubmitEventId`，后端从该事件读取设备账号、设备和工作站并建立唯一结构化绑定。
- ROOT CAUSE: PQC 正式入口已经把生产报工事件写入 URL，前后端请求合同也已支持该字段，但 `FrontlineFixedTemplatePanel.vue` 未将路由字段水合到页面上下文，且请求构造器未传递该字段。
- RED: `node tests/e2e/pqc-production-source-context-static.spec.cjs` -> FAIL，断言首先确认 `FrontlineTemplateContext` 缺少 `productionSubmitEventId?: number`；路由水合和 PQC 请求传递合同同样尚未满足。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesFrontlinePqcContextServiceTest#shouldDerivePqcDeviceContextFromProductionSubmitEvent+shouldRejectPqcWhenProductionSubmitEventIdentityDoesNotMatchTask" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 2 tests；实际值仍为客户端命令中的设备账号 `99901` 而非来源事件的 `9201`，工序不匹配的来源事件也未抛出异常。

## 2026-08-10 Allocation Scope Correction

- E2E FACT: 使用专用 `PQC E2E` 账号从真实一线 PQC 页面打开事件 `176` 的来源参数时，正式页面只加载活跃订单并选择工单 `923834`、活跃订单 `50`、路线工序 `980647`、PQC 任务 `348`；事件 `176` 原路线工序为 `928611`，其来源订单已移出活跃池，不能从正式页面建立匹配 PQC 绑定。
- SCOPE: 用户批准的共享分配池规则没有 PQC 前置条件，且明确要求“不增加额外限制”。新报工分配工作台不应调用 PQC 质量门禁；旧的报工确认/PQC 终结链路继续保留其既有质量规则。
- REVERT: 已收回本轮 `FrontlineFixedTemplatePanel`、页面上下文、PQC 服务和相应测试中的来源事件传递改动，不把无关 PQC 修复带入本任务。
- BDD: 无 PQC 也可按报工量分配 -> Given 生产报工事件类型为 `PRODUCTION_SUBMIT`、`rawPayload.outputQuantity=411111` 且没有 PQC 结构化绑定；When 生产组长打开当前分配、执行 FIFO 或保存手动分配；Then 分配池数量为 `411111`，不得查询或要求 PQC 记录。
- BDD: 旧质量确认保持不变 -> Given 旧报工确认链路要求正式 PQC 成功；When 走旧确认接口；Then 原有 PQC 质量门禁及其测试继续生效，不因新工作台移除额外限制而降级。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationPoolQuantityServiceTest,MesReportAllocationFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, testCompile 明确缺少 `MesReportAllocationPoolQuantityService`；新用例已固定“生产报工量直接形成共享分配池且不要求 PQC”的合同。
- 实现：新增 `MesReportAllocationPoolQuantityService`，只验证根事件为生产报工并从正式 `rawPayload.outputQuantity` 读取共享池总量；新分配命令服务改用该服务。旧 `MesReportAllocationQualityGateService` 组合数量服务后继续执行完整 PQC 绑定、结果和逐件抽样门禁，旧确认链路行为不变。
- GREEN: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationPoolQuantityServiceTest,MesReportAllocationQualityGateServiceTest,MesReportAllocationFoundationContractTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesP0PqcQualityAllocationGateTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 21 tests；其中旧 PQC 门禁回归 8 tests 全部通过。

## 2026-08-10 Positive E2E Review Persistence Regression

- BDD: 首次分配创建生产组长复核 -> Given 报工尚无复核记录且生产组长从真实页面保存 FIFO 或手动分配；When 分配服务创建 `mes_pro_process_pool_submission_review`；Then 必须写入 `leader_type=PRODUCTION`，整个分配事务成功提交。
- E2E RED: 事件 `176` 已从真实页面读取池总量 `411111`，FIFO 按活跃订单列表顺序生成 `100/2248/517`，余量 `408246`；确认请求 HTTP 200 但业务响应 `code=500`，MySQL 明确报错 `Field 'leader_type' doesn't have a default value`，事务回滚且未产生分配数据。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationCommandServiceTest#shouldPersistProductionLeaderTypeWhenAllocationCreatesReview" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `expected: <PRODUCTION> but was: <null>`。
- GREEN: 同一测试命令 -> PASS, 1 test；首次分配创建复核时已显式保存 `leader_type=PRODUCTION`。
- E2E RED 2: 复核字段修复部署后，同一 FIFO 确认返回 `code=1040270000`、“排产工单不存在”；只读核对三个可见候选工单 `980019/923889/923834` 的正式排产工单分页均为 `total=0`，而三者仍是活跃订单且具有正式逐工序快照。
- BDD: 已退出排产池的活跃订单仍可分配 -> Given 活跃订单及其逐工序目标快照有效，但原排产工单已退出；When 组长保存该订单的报工分配；Then 以活跃订单工序快照回算完成量，不因不存在可同步的排产进度记录而拒绝分配；存在唯一排产工单时仍必须原子同步进度。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesTeamLeaderOrderProcessCompletionServiceTest#shouldPersistCompletionFromActiveOrderSnapshotWhenScheduleOrderNoLongerExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 明确在 `requireSingleScheduleOrder` 抛出 `1040270000`。
- GREEN: `MesTeamLeaderOrderProcessCompletionServiceTest#shouldPersistCompletionFromActiveOrderSnapshotWhenScheduleOrderNoLongerExists+shouldKeepOrderProcessInProgressBeforeTargetQuantityIsReached` -> PASS, 2 tests；无排产记录时以活跃订单工序快照保存完成量，存在排产记录时仍同步进度。
- E2E RED 3: 再次部署并确认 FIFO 后返回 `code=1040760000`、“工序池提交事件缺少必填上下文：batchRecordBackfillSources”；根因是跨订单分配已经按目标订单自身 `routeProcessId` 保存，但批记录回填仍错误要求来源报工与目标分配的 `routeProcessId` 相同。
- BDD: 跨订单工序按目标上下文回填 -> Given 来源报工与目标活跃订单 `processId` 相同、`routeProcessId` 不同；When 目标订单该工序分配满足完成量；Then 正式批记录绑定、执行上下文和幂等身份必须使用目标分配的 `routeProcessId/processId`，来源事件只提供报工字段值。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest#shouldBackfillTargetOrderProcessWhenSourceEventRouteProcessDiffers" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `validateSources` 明确拒绝来源/目标 `routeProcessId` 不同。
- GREEN: `MesTeamLeaderBatchRecordBackfillServiceTest#shouldBackfillTargetOrderProcessWhenSourceEventRouteProcessDiffers+shouldBackfillReportPayloadFieldsThroughFormalBatchRecordBindingAndCellRules` -> PASS, 2 tests；批记录回填目标上下文与来源数据上下文已分离，既有同路线行为保持。
- E2E RED 4: 目标上下文修复部署后，FIFO 确认到达目标工序 `980647`，返回 `code=1040760317`、“订单工序完成缺少正式批记录绑定”；这证明共享分配命令仍误复用旧“确认即回填批记录”终结链路。
- BDD: 共享分配与旧确认终结解耦 -> Given 新共享分配达到订单工序目标但该工序尚未配置正式批记录；When 从分配工作台保存；Then 数量分配、完成量、碎片和审计原子提交，批记录配置不得成为新分配门槛；Given 走旧报工确认接口，Then 仍执行既有 PQC 与正式批记录回填门禁。
- E2E RED 5: 共享分配与旧确认链路解耦后，真实 FIFO 确认进入审计批量写入，但返回 `code=500`；后端日志明确为 `mes_pro_process_pool_report_allocation_adjustment_audit.adjustment_reason` 无默认值。前端允许不填写复核说明，故无说明分配不能因审计字段约束失败。
- BDD: 无说明分配仍可审计 -> Given FIFO 或手动分配未填写复核说明；When 保存分配；Then 分配数量、完成量和审计均成功提交，审计原因使用分配模式的明确系统原因，填写了说明时保留原说明。
- RED: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationCommandServiceTest#shouldUseAllocationModeAsAuditReasonWhenReviewRemarkIsBlank" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 预期审计原因为 `FIFO自动分配`，实际为 `null`。
- GREEN: 同一测试命令 -> PASS, 1 test；空复核说明时按 `FIFO自动分配` 写入审计，非空说明仍保留调用方内容。
- E2E GREEN 1: 部署包含审计原因修复的任务运行 jar 后，使用既有真实页面事件 `176` 执行 FIFO 确认；POST 返回 HTTP 200、业务 `code=0`，版本 `1`，池总量 `411111`，按活跃订单列表顺序写入 `100/2248/517`，未分配 `408246`，三个分配均显示“未放行”且可编辑。
- E2E GREEN 2: 同一真实页面重新打开事件 `176`，将最早订单分配从 `100` 调整为 `50` 后以 `MANUAL` 保存；POST 返回 HTTP 200、业务 `code=0`，版本 `2`，分配为 `50/2248/517`，未分配 `408296`。报工管理列表“分配订单”列同步显示三笔未放行分配，事件仍留在工作台。
- E2E HISTORY: 切换真实“报工历史”页签后，事件 `176` 仍可查询，显示当前分配 `50/2248/517`、未分配 `408296`，审核通过人为“瑛泰管理员”；未执行 API 写入、SQL 写入或 mock。
- GREEN REGRESSION: `mvn -pl yudao-module-mes "-Dmaven.compiler.useIncrementalCompilation=false" "-Dtest=MesReportAllocationPoolQuantityServiceTest,MesReportAllocationQualityGateServiceTest,MesReportAllocationFoundationContractTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesP0PqcQualityAllocationGateTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesReportAllocationOrderChangeServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 44 tests。
- GREEN PACKAGE: `mvn -pl yudao-server -am -DskipTests "-Dmaven.compiler.useIncrementalCompilation=false" package` -> BUILD SUCCESS。
- GREEN FRONTEND: 三个分配静态契约、目标 ESLint 和 `pnpm ts:check` -> PASS。

## 2026-08-10 Experience Consolidation (Final)

- 已读取并执行 `project-experience-consolidation` 规则；匹配到既有长期经验文档 `docs/backend-development.md` 的 FIFO/工序快照段落。
- 已将可复用边界合并到 `docs/backend-development.md`：共享分配与旧报工终结链路职责分离、跨订单目标工序上下文必须使用目标 routeProcess、空复核说明必须生成非空审计原因，并要求后端回归与真实 Playwright 双重验证。
- 未新建长期经验文档；事件 `176` 的数量和一次性运行态只保留在本任务记录。

## 2026-08-10 Final Cleanup Preview

- 已关闭仅属于本任务的 Playwright 会话 `allocation-176`，未影响其它浏览器会话。
- PREVIEW: `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --task-id 20260809-process-report-shared-allocation-pool --mode preview` -> PASS。
- KEEP: 三份核心任务记录、当前运行的 `backend-report-shared-allocation-final-20260810-0345.jar` 及其运行日志。
- DELETE: 仅本任务旧热补丁目录、旧任务 jar、Playwright 临时产物、旧任务运行日志、`bug-regression-evidence.md` 和机器状态文件。
- BLOCKED/WARNINGS: 均为空；当前主工作区 `int_main`，不执行 worktree 或 Git 写操作。

## 2026-08-10 Final Cleanup Apply

- 首次 APPLY 已删除部分任务产物，但 Windows 长路径文件树在删除旧热补丁目录时返回 `WinError 3`；改用同一正式清理脚本的 `\\?\E:\\IntRuoyi` 长路径工作区重新预览，范围仍只包含已声明的任务产物。
- 第二次 APPLY 清除了全部旧热补丁、旧任务 jar 和旧任务运行日志；Playwright 输出目录因任务浏览器守护进程占用而返回 `WinError 32`。
- 已核对 Playwright 会话列表并仅关闭本任务会话 `allocation-176`、`shared-allocation` 和 `pqc-source-176`；未关闭其它并行任务会话。
- APPLY: `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --workspace \\?\E:\\IntRuoyi --task-id 20260809-process-report-shared-allocation-pool --mode apply` -> PASS，`blocked/warnings` 均为空。
- DELETE: 本任务 Playwright 临时目录、旧热补丁目录、旧任务 jar、旧任务运行日志、`bug-regression-evidence.md` 和 `task-state.json` 均已清理。
- KEEP: `task.md`、`execution-log.md`、`verification-report.md`、生产代码/测试/迁移、当前运行 jar 及当前运行日志。
- Git：用户未要求 stage、commit、merge 或 push，未执行任何 Git 写操作。
- FINAL STATUS: `completed`。
- POST-APPLY PREVIEW: `delete/blocked/warnings` 均为空，任务目录仅保留三份核心记录。
- FINAL RUNTIME: `48081` 仍由 PID `49856` 的 `backend-report-shared-allocation-final-20260810-0345.jar` 监听，`/actuator/health` -> HTTP 200。
- FINAL STRUCTURE: 根目录、后端和前端 `git diff --check` 均以 exit code 0 结束；仅输出工作区既有 LF/CRLF 提示，无空白错误。
