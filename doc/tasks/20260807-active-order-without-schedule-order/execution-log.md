# 执行日志

## 用户意图与范围

- 用户要求“不需要排产工单的限制”，并确认继续实施完整后端链路。
- 用户进一步要求移除“ERP 计划开工时间缺失”限制，并明确 PQC 业务日期不得作为候选或新增限制。
- 保留单一有效排产工单的原有正式行为；仅在有效排产工单数量为零时启用产品路线绑定 + 当前 ACTIVE 路线版本的正式模式。
- 多个有效排产工单仍属于数据冲突并阻塞，不静默任选一条。

## BDD

- BDD: 无排产工单可加入活跃订单 -> Given 已确认生产工单具有正数 ERP 数量、ERP 计划开工时间、唯一产品路线绑定、该路线唯一 ACTIVE 版本及完整发布工序/PQC 规程，When 生产组长搜索候选并加入订单，Then 候选可加入且系统按 ACTIVE 版本快照生成活跃订单工序快照和正式 PQC 任务。
- BDD: 无排产正式来源缺失时阻塞 -> Given 已确认生产工单没有有效排产工单，且产品路线绑定、ACTIVE 版本、发布工序、数量系数、ERP 计划开工时间或 PQC 规程任一缺失，When 搜索候选或加入订单，Then 系统明确返回不可加入原因且不写入活跃订单、工序快照或 PQC 任务。
- BDD: 单一有效排产工单行为保持不变 -> Given 已确认生产工单存在一条完整有效排产工单，When 搜索候选并加入订单，Then 系统继续使用排产路线、版本、工序计划数量和计划日期生成快照及 PQC 任务。
- BDD: 多个有效排产工单继续阻塞 -> Given 已确认生产工单存在多条有效排产工单，When 搜索候选或加入订单，Then 系统拒绝选择并提示有效排产不唯一。
- BDD: 零排产缺少 ERP 计划开工时间仍可加入 -> Given 已确认生产工单没有有效排产工单、ERP 计划开工时间为空且其它正式路线和 PQC 前置完整，When 生产组长搜索并加入订单，Then 候选可加入、系统生成工序快照和 PQC 任务，且 PQC 记录日期等于活跃订单实际加入日期。
- BDD: 有排产工单的 PQC 日期保持不变 -> Given 已确认生产工单存在一条有效排产工单及完整工序计划日期，When 生产组长加入订单，Then PQC 任务继续使用排产工序计划日期。

> 需求变更说明：以上新增的两条 2026-08-07 BDD 已替代早期“零排产缺少 ERP 计划开工时间即阻塞”的约束；早期条目保留为历史 RED/基线证据，不再代表当前验收标准。

## 命令意图与证据

- CHANGE: `docs/changes/20260807-remove-erp-planned-start-active-order-gate.md` -> ACCEPT，用户明确取消 ERP 计划开工时间与 PQC 业务日期加入门禁；零排产 PQC 记录日期改用活跃订单实际加入日期，不改数据库非空与唯一键约束。
- RED BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，未到达本次行为断言；同一测试文件中的并发未完成改动引用当前不存在的 `MesWorkOrderAbnormalStateService`，MES `testCompile` 在 4 个并发测试处失败。本任务不修改该并发业务。
- RED: `node doc/tasks/20260807-active-order-without-schedule-order/erp-planned-start-gate-static.spec.cjs` -> FAIL，预期原因：服务仍包含“ERP计划开工时间缺失”阻塞及 `workOrder.getPlannedStartTime().toLocalDate()` 日期来源。
- GREEN: `node doc/tasks/20260807-active-order-without-schedule-order/erp-planned-start-gate-static.spec.cjs` -> PASS，服务已移除 ERP 计划开工时间门禁和该字段的零排产日期来源，并显式区分零排产与有排产 PQC 日期规则。
- GREEN: 聚焦 `javac` 编译当前 `MesTeamLeaderActiveOrderServiceImpl`、正式来源依赖及 `MesTeamLeaderActiveOrderErpPlannedStartTest`/`MesTeamLeaderActiveOrderServiceTest` -> PASS；使用任务目录独立输出，未覆盖共享 Maven `target`。
- GREEN: JUnit ConsoleLauncher 执行 `MesTeamLeaderActiveOrderErpPlannedStartTest` 与 `MesTeamLeaderActiveOrderServiceTest` -> PASS，25 项全部成功，0 failed/skipped/aborted；覆盖零排产缺 ERP 开工时间候选可用、新增成功、PQC 日期等于 `joinedAt` 日期，以及有排产继续使用工序 `planDate`。
- GREEN: 上述 25 项服务测试加控制器 `activeOrderRequestsInjectCurrentLeaderUserAndExposeOnlyActivePool`、`activeOrderCandidateEndpointReturnsWorkOrderCodeOptions` 两项相关合同测试 -> PASS，27 项全部成功，0 failed/skipped/aborted。
- ADJACENT BLOCKED: 标准 Maven 目标测试在行为执行前被共享工作区并发中的 `MesWorkOrderAbnormalStateService` 源码/测试编译时序阻塞；随后 Maven 主编译在 Windows 文件系统的 Lombok post-compiler 写 class 阶段长期无进展，经 `jcmd` 确认后仅停止本任务 PID。目标生产类和测试已改用任务目录聚焦编译验证。
- ADJACENT DIAGNOSTIC: 混合共享 `target`、旧安装 MES Jar 与当前源码尝试执行控制器全类 16 项时，3 个与本变更无关的方法因旧类 `NoSuchMethodError`/旧映射断言失败；活跃订单新增与候选两项相关控制器合同已在同一聚焦运行中单独通过。
- GREEN: backend evidence validator 与 self-test -> PASS；change request evidence validator -> PASS；任务范围 `git diff --check` -> PASS。
- EXPERIENCE: `project-experience-consolidation` 修订 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线` 与 `docs/experience-index.md`，明确 ERP 计划开工时间不是零排产门禁、零排产 PQC 使用已落库活跃订单 `joinedAt` 日期、有排产保持 `planDate`；未新建长期经验文档。

- READ: 已读取根 `AGENTS.md`、`docs/backend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md` 和 `docs/experience-index.md` 的适用门禁。
- READ: 已读取 `backend-api-delivery`、`behavior-driven-development` 及 backend evidence contract。
- SOURCE SUPERSEDED: `mes_pro_work_order.quantity` 仍作为 ERP 固定数量；早期无排产模式曾使用 `mes_pro_work_order.planned_start_time`。本次已按用户变更取消该日期门禁，当前零排产 PQC 任务使用已落库活跃订单的 `joinedAt` 日期，有排产仍使用工序 `planDate`。
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
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-active-order-without-schedule-order --mode preview` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，删除集合只包含任务 evidence、fixture/诊断脚本、主工作区 E2E 结果和 `slot=8` 临时运行日志，无 blocked/warnings。
- CLEANUP APPLY: 同命令 `--mode apply` -> PASS，预览列出的 8 类任务产物全部删除，三份核心记录保留。
- SLOT RELEASE: 在确认 `8089/48089` 监听数均为 0 后，使用与原子预留相同的注册表互斥锁将 `active-order-without-schedule-verify` 的 `slot=8` 标记为 `active=false`，并记录本任务 cleanup 标识。
- WORKTREE CLEANUP: `git worktree remove --force D:\IntRuoyiWorktree\active-order-without-schedule-verify` 已解除 Git 登记但因忽略产物返回 `Directory not empty`；核对残留仅在任务 worktree 绝对路径且 Git 登记已消失后，精确删除该任务目录并删除临时分支 `codex/active-order-without-schedule-verify`。最终 `Test-Path=False`、分支不存在。
- DIRTY BASELINE: `7b3ac3004` (`chore: baseline concurrent workspace before active order closeout`) 保存 33 个当时工作区文件：`MesProRouteProcessFlowServiceImplTest.java`；本任务 9 个 task/evidence/fixture 文件；`20260807-form-template-import-dialog-layout` 3 个记录；`20260807-frontline-pqc-all-active-orders-search` 5 个记录/evidence；`20260807-frontline-route-process-workstation-binding-fix` 5 个记录/运行补丁；`20260807-pqc-leader-management-five-records` 5 个记录/fixture/runner；`20260807-production-leader-active-order-route-labels` 2 个验证产物；`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/experience-index.md`。这是按共享 dirty-worktree 强制政策保存的并发基线，不代表本任务对其它任务完成状态作出结论。
- DIRTY BASELINE: `cbc46b0e2` (`chore: preserve concurrent runtime patch before active order closeout`) 保存并发任务随后继续修改的 `doc/tasks/20260807-frontline-route-process-workstation-binding-fix/runtime-patch/publish-workstation-repair.ps1`。

## 里程碑状态

- M1 completed：现有新增链路完全依赖排产路线、排产工序数量系数/计划数量/计划日期；生产工单可提供 ERP 数量和 ERP 计划开工时间，ACTIVE 路线发布快照可提供正式工序与排产用途数量系数。
- M2 completed：新增候选成功、候选缺绑定失败、新增成功和新增缺绑定失败测试；RED 在服务正式来源依赖尚未实现处失败。
- M3 completed：候选与新增共享产品唯一正式路线绑定、唯一 ACTIVE 版本及发布快照解析；无排产模式从 ERP 数量和计划开工时间生成工序快照与 PQC 任务，单排产保持原链路，多排产继续阻塞。
- M4 completed：聚焦服务/控制器回归、backend evidence、三个前端静态合同、隔离运行态真实页面加入、数据库快照/PQC 核验和任务数据归零均已通过。
- M5 blocked_at_push：经验沉淀、cleanup preview/apply、任务数据/进程/日志清理、端口登记释放、验证 worktree/临时分支删除和 dirty baseline 均已完成；`git push origin int_main` -> FAIL，Git 无法通过本机代理 `127.0.0.1` 连接 `github.com:443`，本地 `int_main` 在 push 前领先 `origin/int_main` 4 个提交。
- M6 completed：已完成需求变更记录、BDD/RED、生产实现、静态合同、25 项服务回归、2 项相关控制器合同、backend evidence 校验、经验修订与 cleanup。

## 阻塞项

- MES 全体测试源码存在与本任务无关的基线编译阻塞：`MesTeamEmployeeBindingServiceTest` 引用当前提交不存在的 `MesTeamEmployeeBindingService`。本任务已通过 exact-HEAD detached worktree 的聚焦编译与 37 项测试验证目标范围。

## M6 收尾

- CLEANUP PREVIEW: `task_closeout.py --task-id 20260807-active-order-without-schedule-order --mode preview` -> PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，删除集合仅包含本任务 evidence、静态 RED 脚本、独立 class 输出、classpath 和 javac/JUnit 参数文件，blocked 为 0。
- CLEANUP APPLY: 同命令 `--mode apply` -> PASS；上述任务临时产物已删除，三份核心记录与正式 `src/test` 回归测试保留。
- FINAL STATUS: 本次未获 Git 操作授权，未执行 stage、commit、merge 或 push；这不属于当前完成门禁。
