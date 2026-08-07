# 执行日志

## 用户意图与范围

- 用户要求“不需要排产工单的限制”，并确认继续实施完整后端链路。
- 保留单一有效排产工单的原有正式行为；仅在有效排产工单数量为零时启用产品路线绑定 + 当前 ACTIVE 路线版本的正式模式。
- 多个有效排产工单仍属于数据冲突并阻塞，不静默任选一条。

## BDD

- BDD: 无排产工单可加入活跃订单 -> Given 已确认生产工单具有正数 ERP 数量、ERP 计划开工时间、唯一产品路线绑定、该路线唯一 ACTIVE 版本及完整发布工序/PQC 规程，When 生产组长搜索候选并加入订单，Then 候选可加入且系统按 ACTIVE 版本快照生成活跃订单工序快照和正式 PQC 任务。
- BDD: 无排产正式来源缺失时阻塞 -> Given 已确认生产工单没有有效排产工单，且产品路线绑定、ACTIVE 版本、发布工序、数量系数、ERP 计划开工时间或 PQC 规程任一缺失，When 搜索候选或加入订单，Then 系统明确返回不可加入原因且不写入活跃订单、工序快照或 PQC 任务。
- BDD: 单一有效排产工单行为保持不变 -> Given 已确认生产工单存在一条完整有效排产工单，When 搜索候选并加入订单，Then 系统继续使用排产路线、版本、工序计划数量和计划日期生成快照及 PQC 任务。
- BDD: 多个有效排产工单继续阻塞 -> Given 已确认生产工单存在多条有效排产工单，When 搜索候选或加入订单，Then 系统拒绝选择并提示有效排产不唯一。

## 命令意图与证据

- READ: 已读取根 `AGENTS.md`、`docs/backend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md` 和 `docs/experience-index.md` 的适用门禁。
- READ: 已读取 `backend-api-delivery`、`behavior-driven-development` 及 backend evidence contract。
- SOURCE: `mes_pro_work_order.quantity` 作为 ERP 固定数量；无排产模式的业务日期使用明确的 `mes_pro_work_order.planned_start_time` 日期部分，缺失即阻塞，不切换到需求日期或当前日期。
- SOURCE: 无排产路线使用 `mes_pro_route_product` 唯一正式绑定、`mes_pro_route_version` 唯一 ACTIVE 版本及其 `route_snapshot_json.configSnapshots.flowGraph.nodes/scheduleUseConfigs`。
- BASELINE: 开始本任务时发现并发任务 `doc/tasks/20260807-production-leader-process-loss-reasons-random/execution-log.md` 仍有未提交改动；将按共享分支规则单独保存，不纳入本任务实现提交。
- CONCURRENT BASELINE: 并发任务在本任务创建文档后生成提交 `9c7507e1d`，该提交把本任务初始 `task.md`、`execution-log.md`、`backend-api-evidence.md` 与并发任务日志一并纳入；本任务未改写该提交，后续实现仍单独验证和选择性提交。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：新无排产测试要求服务接入 `MesProRouteProductMapper` 和 `MesProRouteVersionMapper`，现有构造器尚无正式来源依赖。
- CONCURRENT BASELINE: 并发基线提交 `c7aff9959` 和 `35595ee9f` 分别纳入本任务服务实现的中间状态与最终实现/初始测试；后续提交 `6b3a6b816` 纳入 3 个正式来源边界测试。本任务未重写这些共享提交，并以最终 `int_main` 提交 `fca53dda5` 创建 detached 验证 worktree。
- BUILD ISOLATION: 主工作区同时存在其它 MES Maven 构建并共享 `target`，出现生成测试目录删除警告和大面积已存在类缺失；按 `docs/worktree-restrictions.md` 在 `D:\IntRuoyiWorktree\active-order-without-schedule-verify` 创建 detached 验证 worktree，不启动服务、不分配端口。
- CLEAN BASELINE CHECK: detached worktree 完整反应堆主代码编译通过；MES 全体 `testCompile` 被无关基线 `MesTeamEmployeeBindingServiceTest` 引用不存在的 `MesTeamEmployeeBindingService` 阻塞，目标测试尚未执行。未修改或绕过该无关产品代码。
- GREEN PREP: 在 detached worktree 中通过 Maven `dependency:build-classpath` 生成正式测试依赖，并用 JDK `javac` 仅编译 `MesTeamLeaderActiveOrderServiceTest.java` 与 `MesProcessPoolTeamLeaderControllerTest.java`；编译退出码为 0。
- GREEN: `mvn -pl yudao-module-mes surefire:test "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` -> PASS，服务测试 21 项、控制器测试 16 项，共 37 项，Failures/Errors/Skipped 均为 0。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`、`node tests/e2e/team-leader-workbench-static.spec.cjs`、`node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> PASS，活跃订单池与生产组长工作台静态合同通过。
- GREEN: backend evidence validator -> PASS，`Backend API evidence is valid.`。
- DATABASE PREFLIGHT: `e2e_fixture.py --mode preflight` -> PASS，测试租户 `122`、生产组长 `acd04lead1/914525` 存在，任务前缀主对象计数均为 0。
- RED: `e2e_fixture.py --mode verify` -> FAIL，预期原因：任务 fixture 尚未 seed，返回 `Fixture work order is missing`。
- FIXTURE SCHEMA RED: 初次 `e2e_fixture.py --mode seed` -> FAIL，真实 schema 的 `mes_pro_route_version.active_unique_flag` 为生成列，事务已回滚；删除显式写入后 `verify-clean` 证明前缀计数仍为 0。
- GREEN: `e2e_fixture.py --mode seed` 与 `--mode verify-seed` -> PASS，已确认工单 `AONS-20260807-WO/980027` 具备唯一产品路线绑定、唯一 ACTIVE 版本、完整发布快照、已发布 PQC 规程且有效排产工单数为 0。
- E2E RED: `node tests/e2e/production-leader-active-order-focused.e2e.js` against `8081/48081` -> FAIL，真实页面候选可见但旧运行 Jar 返回 `活跃订单缺少唯一有效排产工单：980027`；未生成活跃订单写入。
- RUNTIME ISOLATION: 在任务验证 worktree 为 `int_main` 原子登记 `slot=8`（前端 `8089`、后端 `48089`），端口预检均无监听；`mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS（21:48），`pnpm install --frozen-lockfile` -> PASS（21:59）。
- GREEN: `node tests/e2e/production-leader-active-order-focused.e2e.js` against `8089/48089` -> PASS，测试租户 `测试租户/acd04lead1` 通过真实页面搜索并加入 `AONS-20260807-WO`；候选 `eligible=true`，新增载荷仅含 `workOrderId=980027`，业务码 `0`，随后由页面移出。
- GREEN: `e2e_fixture.py --mode verify-result` -> PASS，落库活跃订单 `40`、工序快照 1 条（系数 1、计划数量 100）和 PQC 任务 3 条，业务日期均为 ERP 计划开工日期 `2026-08-07`。
- CLEANUP: `e2e_fixture.py --mode cleanup` -> PASS，精确删除审计 2、PQC 任务 3、工序快照 1、活跃订单 1 及全部任务主数据；`--mode verify-clean` -> PASS，前缀主对象计数全部为 0。
- RUNTIME CLEANUP: 已停止任务自有 Java/Node 及两个 PowerShell wrapper，`8089/48089` 监听数均为 0；未停止或重启 `8081/48081` 并发运行态。
- GREEN: database evidence validator -> PASS，`Database schema evidence is valid.`。
- EXPERIENCE: `project-experience-consolidation` 将零排产活跃订单的正式来源、阻塞条件、验证方式和禁止默认值规则合并到 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`，并更新 `docs/experience-index.md`；未新建长期经验文档。

## 里程碑状态

- M1 completed：现有新增链路完全依赖排产路线、排产工序数量系数/计划数量/计划日期；生产工单可提供 ERP 数量和 ERP 计划开工时间，ACTIVE 路线发布快照可提供正式工序与排产用途数量系数。
- M2 completed：新增候选成功、候选缺绑定失败、新增成功和新增缺绑定失败测试；RED 在服务正式来源依赖尚未实现处失败。
- M3 completed：候选与新增共享产品唯一正式路线绑定、唯一 ACTIVE 版本及发布快照解析；无排产模式从 ERP 数量和计划开工时间生成工序快照与 PQC 任务，单排产保持原链路，多排产继续阻塞。
- M4 completed：聚焦服务/控制器回归、backend evidence、三个前端静态合同、隔离运行态真实页面加入、数据库快照/PQC 核验和任务数据归零均已通过。
- M5 in_progress：经验沉淀已完成；待完成 closeout cleanup、任务记录提交与 `origin/int_main` 推送核验。

## 阻塞项

- MES 全体测试源码存在与本任务无关的基线编译阻塞：`MesTeamEmployeeBindingServiceTest` 引用当前提交不存在的 `MesTeamEmployeeBindingService`。本任务已通过 exact-HEAD detached worktree 的聚焦编译与 37 项测试验证目标范围。
