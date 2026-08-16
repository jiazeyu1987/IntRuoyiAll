# Execution Log

## Pass 0 - Supervisor Bootstrap

- task id：FLOW-SUPERVISOR
- changed paths：`doc/tasks/20260814-production-release-flow-implementation/task.md`、本文件
- implemented behavior：建立总任务、里程碑、验收门禁、设计约束和独立 worktree 目标。
- validation commands：待 worktree 创建后记录。
- validation results：待执行。
- covered acceptance ids：规划阶段尚未分配。
- known risks or blockers：当前主工作区包含其它任务改动；本任务业务代码只允许写入独立 worktree。Git 提交、合并和推送未获授权，不属于当前自动执行范围。

### Worktree Evidence

- absolute path validation：`D:\IntRuoyiWorktree\pqc-production-release-flow` 已验证属于 `D:\IntRuoyiWorktree\`。
- command：`git worktree add -b codex/pqc-production-release-flow D:\IntRuoyiWorktree\pqc-production-release-flow int_main`
- result：PASS；目标 worktree 基于 `int_main` 的 `bba5ba689a75008a0fb8d1ce3eb9f38ee68e47a4` 创建。
- runtime：尚未启动服务，尚未占用端口；启动前必须原子预留 profile slot。

## Pass 1 - Planning Gate

- task id：FLOW-PLANNING
- changed paths：`request-analysis.md`、`prd.md`、`task-state.json`
- implemented behavior：基于实际代码和 SP-0 至 SP-4 冻结文档形成统一实施 PRD；明确五阶段状态机、角色、三类正式映射、三类负责人四份报告、事务边界、幂等/CAS、迁移阻塞和 34 项验收标准。
- independent code audit：FAIL（当前代码）；确认组长提交会越级创建批次、映射资料并提交最终放行，四报告可跳过且顺序派发，PQC/管理者角色及目标接口缺失，因此必须实施而不能宣称现状已满足。
- review revisions：修正 SP-1 apply 不接收 `expectedVersion`、回执按 `activeOrderId` 查询、冻结错误码 `UNSUPPORTED_RELEASE_ACTION`，补齐工作流规定的七个请求分析章节。
- validation command：PowerShell UTF-8 结构与冻结合同扫描。
- validation result：PASS；request-analysis 和 PRD 必需章节、34 项 AC、两个角色码、三负责人四报告、SP-1 输入边界和稳定错误码均通过。
- covered acceptance ids：AC-01 至 AC-34（规划映射，尚未形成执行证据）。
- known risks or blockers：目标租户、角色用户、三类负责人、三类正式表单、旧数据预检、文件存储和签核证据仍需在集成/E2E 前验证。

## Pass 2 - Decomposition Gate

- task id：FLOW-DECOMPOSITION
- changed paths：`dev-plan.md`、`test-plan.md`、`task-state.json`
- implemented behavior：形成 11 项依赖任务和 14 组测试；明确 T1/T2 第一波并行、共享文件唯一 owner、后续阶段串行集成和独立测试门禁。
- validation command：PowerShell artifact contract scanner。
- validation result：PASS；11 个任务均含 9 个必需字段，14 个测试均含 7 个必需字段，AC-01 至 AC-34 全覆盖，冲突图和集成顺序存在。
- covered acceptance ids：AC-01 至 AC-34（计划映射）。
- known risks or blockers：T1/T2 只能写各自范围；MIG-RF-1 未通过前不得执行 SP-2 至 SP-4 数据集成。

### Contract Reconciliation

- source of truth：`interface-contract.md` 6.1 明确 SP-1 的业务幂等键由后端权威快照生成，同一业务身份即使换用不同请求键也必须返回同一申请。
- issue：实施 PRD/测试计划曾把 SP-1 的异请求键重复错误写成冲突，无法同时满足源合同。
- resolution：已将 SP-1 修正为按 `businessIdempotencyKey` 复用同一申请；仅同一请求键对应不同权威快照时返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`。SP-2 至 SP-4 的异键重复动作仍按各自版本合同返回冲突。
- validation result：PASS；PRD、开发计划和测试计划口径已和冻结接口合同一致。

## Pass 3 - Restart Recovery And Baseline Gate

- task id：`T1`、`T2`
- changed paths recovered：T1 角色 SQL、角色候选解析服务及测试；T2 两份共享合同 RED 测试。重启恢复本身未新增生产代码。
- BDD: 角色候选必须由当前租户的启用角色成员产生 -> Given 当前租户存在冻结角色及启用成员 / When 系统解析 PQC 或管理者代表候选 / Then 只返回该租户启用成员，缺失、停用、空候选或歧义均明确失败。
- validation command：`python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q`
- validation result：PASS；`6 passed in 0.19s`，证明 MIG-RF-0 的目标租户解析、精确权限、幂等绑定、无固定 ID 和非破坏性静态合同。
- validation command：`mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseRequiredCandidateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- validation result：BLOCKED；Maven 在目标 JUnit 执行前编译现有 MES 主源码失败，共 39 个缺类错误。代表性缺失类为 `MesProductionReportManagementSummaryService`、`MesTeamLeaderActiveOrderDetailRespVO`、`MesTeamLeaderActiveOrderDetailService`、`MesFrontlineSessionSnapshotService`、`MesProRouteProductCandidateCopyReqVO`、`MesProSchedulerWorkbenchRuntimeStatusService`。
- provenance check：上述缺失文件不在 `int_main` 提交 `bba5ba689` 中；部分文件只存在于主工作区或其它任务 worktree 的未跟踪状态，不能作为本任务的正式依赖复制。
- RED status：未记录业务 RED；本次失败发生在目标测试执行前，不能冒充 TDD RED。
- covered acceptance ids：AC-03 的 SQL 静态部分；其余仍未通过执行门禁。
- blocker：需要正式可编译的基线提交，或用户明确授权一个不会混入并发任务未提交修改的基线整合方案。未解除前 T1/T2 及其下游任务全部暂停。

### Independent Baseline Investigation

- investigator：独立只读子 Agent `baseline_investigator`。
- origin：缺失引用由提交 `333029852` 引入；当前任务 HEAD `bba5ba689` 继承这些引用，但正式 Git 树没有相应类型定义。
- ref audit：对全部 177 个本地、远程和 tag refs 核查代表性缺失文件，531 个 `ref:path` 查询结果 `PRESENT_COUNT=0`；不存在可直接采用的正式已提交分支或标签。
- untracked provenance：`20260812-frontline-pqc-dcc-qa-int12` worktree 有 20 个未跟踪 Java 文件，其中 18 个 MES 文件包含缺失定义；该 worktree 历史 Maven PASS 的编译输入实际包含这些未跟踪文件，不能证明正式 Git 基线可编译。
- rejected shortcut：不得复制 INT12 或主工作区的未提交先决文件，不得退回父提交 `817687224`，不得用 SQL 静态测试冒充后端 JUnit GREEN。
- recommended prerequisite：原基线责任任务先审查、验证并正式提交 20 个先决文件；之后需用户明确授权把该先决提交集成到当前 PQC worktree。当前 Git Policy 未授权 commit/merge/rebase/cherry-pick，主 Agent不能自行执行。

## Pass 4 - Prerequisite Commit, Integration And Runtime Slot Gate

- task id：`T1`、`T2` 前置基线恢复。
- user authorization：用户明确授权审查并提交 INT12 worktree 中的 20 个前置 Java 源码，并将正式提交接入 PQC worktree；禁止复制未提交文件、混入无关文件和推送远端。
- source worktree precheck：`task/20260812-frontline-pqc-dcc-qa-int12` 暂存区为空，工作区恰有 20 个目标未跟踪 Java 文件。
- risk scan：对 20 个文件扫描 `fallback`、`mock`、`default-success`、`TODO`、`FIXME`、`password`、`secret`、`token`，结果 `RISK_HITS=NONE`。
- validation command：`git diff --check -- <20 个 Java 文件>`。
- validation result：PASS。
- validation command：`mvn -pl yudao-module-mes clean "-DskipTests" compile`。
- validation result：PASS；删除本模块旧 `target` 后重新编译 2665 个主源码，`BUILD SUCCESS`，证明原 39 个缺类编译错误已解除，未使用旧编译产物冒充 GREEN。
- source commit：`2810aec91fa55eedea3e1a0fd1b5e1195371ad26 fix: add missing frontline prerequisite sources`；仅包含 20 个目标 Java 文件，提交后源 worktree 干净。
- PQC integration：PQC worktree 原有 12 个 T1/T2 未跟踪文件与 20 个前置文件无路径重叠；已将前置提交安全接入为 `28923b17164a22feee51ee2ec9e42ead9b4ef3cc`，无冲突，原有 T1/T2 文件完整保留。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS，`6 passed in 0.20s`。
- RED attempt：`mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 已完成 DCC 767 个及 MES 2672 个主源码重新编译，随后在 `testCompile` 失败。T2 预期缺失生命周期、CAS、结构化错误、审计、幂等和共享任务字段；同时基线缺少已提交的 `MesProFrontlineFeedbackSubmitSnapshotTestSupport`，因此本次结果不登记为纯业务 RED。
- helper provenance：测试辅助类来自正式提交 `3e0df78fe`；未复制其它 worktree 的未提交文件，也未接入该提交中的其它无关文件。已用 `git restore --source=3e0df78fe --worktree -- <精确路径>` 提取单个正式 Git 文件，风险词扫描无命中，暂存区恰为该 1 个文件，`git diff --cached --check` PASS。
- commit blocker：`git commit -m "test: add missing frontline submit snapshot support"` 被分支运行端口钩子拒绝，未产生提交；原因是 PQC worktree 没有活动端口登记。
- required gate command：`scripts\\runtime\\reserve-worktree-slot.ps1 -Name pqc-production-release-flow -Path D:\\IntRuoyiWorktree\\pqc-production-release-flow -Branch codex/pqc-production-release-flow -Profile int_main -AsJson`。
- blocker result：FAIL；`int_main` 的槽位 `1..19` 均被其它活动 worktree 登记占用，脚本返回 `No available runtime slot for profile 'int_main' in range 1..19.`，登记表未写入。
- current staged state：仅 `A IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitSnapshotTestSupport.java`；未暂存原有 12 个 T1/T2 文件。
- blocker：项目规则禁止释放其它任务槽位、手工改登记表或绕过提交钩子。需要其它任务正式释放至少一个 `int_main` 槽位后，才能完成辅助类提交并继续 T1/T2 严格 TDD。

## Pass 5 - Pre-2026-08-12 Worktree Cleanup Preflight

- user authorization：用户要求清除 2026-08-12 之前创建的 worktree 并释放槽位。
- cutoff contract：以 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 的 `createdAt < 2026-08-12T00:00:00+08:00` 且 `active=true` 为目标范围，共 8 项。
- runtime precheck：8 个目标的登记前后端端口均无监听，未发现命令行指向目标路径的运行进程。
- safe delete target：`allocation-dialog-production-columns`，分支 `codex/allocation-dialog-production-columns`，HEAD `4ceae2d5d13371903062867e16c33d1079abf05a`；工作区干净、相对 `int_main` ahead 0、提交已包含于 `int_main`，允许删除并释放 slot 8。
- protected targets：`20260805-process-config-unification`、`frontline-pqc-latest-active-version`、`shared-word-parser-implementation`、`route-version-config-inherit-20260810`、`20260811-route-publish-config-inherit`、`route-publish-chain-clarity-20260811-verify` 存在未提交文件或未合入提交，未获得丢弃或集成授权，不得强删。
- disconnected target：`20260805-production-personnel-management` 已无 `.git` 且不在 Git worktree 注册表中，分支提交已合入 `int_main`，但物理目录仍含完整源码与资源，无法证明只剩可再生产物；本轮不删除、不释放 slot 1。
- planned action：只对 `allocation-dialog-production-columns` 执行标准 `git worktree remove`；确认 Git 注册和物理目录均消失后，持端口登记表 mutex 将该单条记录标记为 inactive，并验证登记表合同。

### Cleanup And Resume Result

- worktree removal：`git worktree remove D:\IntRuoyiWorktree\allocation-dialog-production-columns` 已移除 Git 注册；Windows 首次因忽略产物返回 `Directory not empty`。复核无 `.git`、无 Git 注册、无 8089/48089 监听、无路径进程后，仅删除该目标残留目录；最终 `Test-Path=False`。
- slot release：持与 `reserve-worktree-slot.ps1` 相同的跨进程 mutex，结构化更新登记表并通过运行端口合同校验；旧记录 slot 8 已标记 `active=false`，`releasedAt=2026-08-15T11:35:47.2194414+08:00`。
- PQC reservation：官方 `reserve-worktree-slot.ps1` 原子登记 PQC worktree 为 `int_main` slot 8，前端 8089、后端 48089；未启动任何服务。
- helper commit：分支运行端口 guard PASS；测试辅助类单文件提交为 `4aeb6f8692470c88e3f766c7b40bba0f1dbb898a test: add missing frontline submit snapshot support`，提交后暂存区为空，原有 12 个 T1/T2 未跟踪文件完整保留。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；已完成全部上游模块并重新编译 MES 444 个测试源码，失败仅来自 T2 合同要求但尚未实现的生命周期、CAS Mapper、状态/阶段/审计/幂等/结构化错误及共享任务字段。此前 39 个主源码缺类和测试辅助类错误均未再出现，现登记为正式 T2 RED。
- protected old worktrees：其余 7 个 2026-08-12 之前的登记项因未提交文件、未合入提交或断链目录内容无法证明可丢弃而保留，槽位未释放；未强删、未停止进程、未修改其分支。

## Pass 6 - T1/T2 GREEN And Migration Policy Review

- task id：`T1`、`T2`。
- implementation：完成角色候选解析、MIG-RF-1、申请聚合加锁/CAS、五个持久状态、结构化 blocker、ASCII 幂等键、工作待办投影和生产放行审计端口。
- audit transaction review：现有 eDHR 通用审计服务使用 `REQUIRES_NEW`，不满足生产放行“状态、下游初始化、审计同一调用方事务”合同；T2 的唯一审计端口明确要求同步加入调用方事务、写失败向上传播，禁止 `REQUIRES_NEW`、异步和 no-op。后续 T3 至 T9 必须提供正式适配器。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；15 tests，0 failures，0 errors，0 skipped。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS；6 passed。
- GREEN: MIG-RF-1 release migration policy dependency closure -> PASS；递归包含 11 个迁移，metadata、依赖环境和 checksum 合同通过，证据为 `migration-policy-gate.json`。
- BASELINE BLOCKER: 全 SQL 目录 migration policy gate -> FAIL；既有无关文件 `20260814_mes_batch_record_repeat_row_group.sql` 缺少 release-migration metadata。失败发生在扫描本任务迁移前；未修改无关文件，证据为 `migration-policy-gate-full-baseline-failure.json`。
- remaining gate：运行 backend/database evidence validator、任务范围风险词与 diff check；然后分别精确暂存并提交 T1、T2，不混入其它文件。
- GREEN: backend API evidence validator -> PASS；`Backend API evidence is valid.`
- GREEN: database schema evidence validator -> PASS；`Database schema evidence is valid.`
- commit：`ac44d020e feat: add production release role baseline`；精确包含 10 个 T1 文件。
- commit：`d7f5d0122 feat: add production release flow core contracts`；精确包含 23 个 T2 文件。
- final worktree state：`git status --short --branch --untracked-files=all` 仅显示分支名，无修改、未跟踪或暂存文件。
- milestone result：T1、T2 completed；依赖图允许 T3 开始。

## Pass 7 - T3 SP-1 RED

- task id：`T3`。
- BDD: 生产组长提交只创建申请与一个 PQC 待办 -> Given 当前用户是活动订单归属组长且生产、过程检验均为正式 100% / When 提交 SP-1 生产放行申请 / Then 同一事务只创建一条 `PQC_RELEASE_PENDING` 申请、一个冻结 `MES_PQC_RELEASE_OWNER` 候选的 `PQC_PRODUCTION_RELEASE` 待办和一条审计，不创建批次、三类映射、四报告节点、放行事务或管理者任务。
- BDD: 双 100% 与归属失败无写入 -> Given 生产或检验任一不足 100%，或当前用户不是活动订单归属组长 / When 提交申请 / Then 返回对应结构化 blocker，申请、待办、审计均无写入。
- BDD: SP-1 双幂等 -> Given 已存在相同请求键或相同后端业务身份的申请 / When 重放相同载荷、以新请求键提交相同业务身份，或同一请求键对应已变化的权威快照 / Then 前两者返回原回执，最后一种返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`，且不重复写入。
- BDD: 申请回执按活动订单和冻结候选授权 -> Given 申请及 PQC 待办已经持久化 / When 归属组长、冻结 PQC 候选或无关用户按 activeOrderId 查询 / Then 前两者取得同一回执，无关用户得到结构化无权失败。
- RED target：`MesProductionReleaseApplySp1Test` 先冻结新构造依赖、申请/PQC 待办字段、原子事务、幂等和回执合同；旧 generation/persistence/get 合同尚不满足，预期目标 Maven 编译失败。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseApplySp1Test" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；清除测试自身导入和 Mockito 重载噪音后，剩余 12 个编译错误全部来自旧 generation/persistence 构造仍含批次、三 writer 和最终放行依赖，且缺少 PQC task 回执字段、`selectLatestByActiveOrderId` 与 GET 合同，符合预期。

## Pass 8 - T3 SP-1 GREEN And Commit

- task id：`T3`。
- implementation：组长提交先校验活动订单归属、冻结路线、逐工序正式生产完成和正式过程检验完成；成功事务只创建一条 `PQC_RELEASE_PENDING` 申请、一个 `PQC_PRODUCTION_RELEASE` 待办、申请待办绑定和 `PQC_PRODUCTION_RELEASE_APPLIED` 审计。
- no downstream creation：SP-1 generation/persistence 中 `openOrCreate`、`submitForApproval`、三类 writer、批次执行和放行事务调用扫描均无命中；响应不再包含批次执行、放行事务或管理者待办伪 ID。
- idempotency：业务键严格为 `SHA-256(PQC_RELEASE|tenantId|activeOrderId|workOrderId|batchCode|routeId|routeVersionId)`；同请求键权威快照变化返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`，同业务身份异请求键返回原申请。
- receipt and projection：新增按 `activeOrderId` 的权威 GET；仅归属组长或冻结 PQC 候选可读。回执和活跃订单列表统一投影申请编号、PQC 待办编号、状态、来源快照哈希和版本；Long ID 按字符串输出。
- audit transaction：新增通用 eDHR 审计的 `REQUIRED` 入口，生产放行适配器只调用该入口；申请、待办、绑定和审计加入调用方事务，审计写失败向上传播，原有其它调用方的 `REQUIRES_NEW` 入口保持不变。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithFormalRouteDisplayFieldsUsingBatchQueries" test` -> FAIL；新列表测试因四个权威投影字段尚不存在而产生 4 个预期编译错误。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseApplySp1Test" test` -> PASS；9 tests，覆盖成功对象增量、双 100%、归属、双幂等、回执授权、角色候选失败、待办写失败和事务传播。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor module 全部成功，MES 主源码可编译。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseApplySp1Test,MesProductionReleaseApplyControllerJsonTest,MesTeamLeaderActiveOrderReleaseSourceSnapshotHasherTest,MesTeamLeaderActiveOrderReleaseAuditRecorderTest,MesTeamLeaderActiveOrderServiceTest#shouldListActiveOrdersWithFormalRouteDisplayFieldsUsingBatchQueries,MesProEdhrOperationAuditServiceTest,MesProEdhrOperationAuditServiceFailureTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest,MesReleaseFlowCoreContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；61 tests，0 failures，0 errors，0 skipped。
- baseline test note：额外运行完整 `MesTeamLeaderActiveOrderServiceTest` 时共 30 项，其中 28 项通过，2 项在既有候选订单用例结束阶段因各自未使用 Mockito 桩报错；不是业务断言失败，且目标列表投影用例单独及最终组合均通过。本任务未越界修改这两个既有测试桩。
- static gates：新增/修改行未命中 `fallback`、`default-success`、`FIXME`、`password`、`secret`、`token`；`TODO` 仅为正式待办状态常量，`mock` 仅存在于测试；`git diff --cached --check` PASS。
- runtime gate：`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；`codex/pqc-production-release-flow/int_main` 使用 slot 8，frontend 8089，backend 48089，未启动服务。
- commit：`a229312e92c78f05a3bda12f2da129a5d6c1db90 feat: add team leader PQC release application flow`；精确包含 22 个 T3 文件，提交后 PQC worktree 干净。
- milestone result：T3 completed；依赖图允许 T4 与 T5 开始，当前执行点推进到 T4。

## Pass 9 - T4 SP-1 Frontend BDD And RED

- task id：`T4`。
- BDD: 双 100% 才可提交 -> Given 活跃订单生产或过程检验任一未达到正式 100% / When 生产组长查看完工操作 / Then 按钮不可提交并显示具体缺失进度；两者均为 100% 且没有既有申请时才可提交。
- BDD: SP-1 只确认申请和 PQC 待办 -> Given 双 100% 且当前组长有权限 / When 确认完工 / Then 页面只接受申请编号、PQC 待办编号、状态、快照和版本，不要求批次、报告任务或最终放行事务。
- BDD: 失败必须展示结构化 blocker -> Given 后端返回生产进度、权限、快照或幂等 blocker / When 请求失败 / Then 页面显示 blocker 类型、原因、建议和对象定位，不把失败显示为成功。
- BDD: 不确定响应按权威回执恢复 -> Given POST 超时或响应不完整 / When 页面无法确认提交结果 / Then 使用同一 activeOrderId 查询正式回执，保留原幂等键且禁止重复申请；已确认成功后的列表刷新失败不得覆盖成功事实。
- RED target：新增 `sp1-production-release-contract` 命名合同，冻结五状态、字符串 ID、SP-1 下游边界、正式 GET 回执恢复、结构化 blocker 透传和旧合同负向断言。
- RED: `pnpm test sp1-production-release-contract` -> FAIL；首个断言为 `Missing persistent release status: PQC_RELEASE_PENDING`，证明页面和 API 仍依赖旧两状态与旧回执。

## Pass 10 - T4 SP-1 Frontend GREEN

- task id：`T4`。
- implementation：前端 API 对齐五个持久状态和字符串 Long ID；SP-1 成功回执只接受申请、冻结路线、PQC 待办、快照哈希和版本；新增按 `activeOrderId` 的正式 GET 回执。
- blocker handling：Axios 错误保留 `CommonResult.data`；页面严格解析 `stage/currentStatus/blockers` 并展示类型、原因、建议和对象定位。结构化业务失败不进入不确定恢复；响应不确定才查询权威回执并锁定重复提交。
- page state：生产或检验未 100%、异常订单或已有任一持久状态均不可再次申请；五状态使用中文标签。成功响应先固定“已提交”事实，列表刷新失败或投影未同步不会改报写入失败。
- SP-1 boundary：确认文案明确本阶段只创建申请和 PQC 待办，不创建批次、报告上传任务或最终放行事务；页面不再读取伪下游 ID、资料摘要或旧状态名。
- GREEN: `pnpm test sp1-production-release-contract` -> PASS。
- GREEN: `node src/api/mes/pro/processpool/teamLeaderReleaseApplication.static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/team-leader-active-order-release-application-static.spec.js` -> PASS。
- GREEN: `pnpm test e2e:team-leader-workbench:static` -> PASS。
- environment prerequisite：worktree 初始没有 `node_modules`；`pnpm install --offline --frozen-lockfile --reporter=silent` -> PASS，未改 lockfile，未使用其它包管理器。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- static gates：新增行风险词扫描无命中；`git diff --check` PASS。
- runtime gate：`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；slot 8，frontend 8089，backend 48089，未启动服务。
- commit：`f9186fa35 feat: add SP-1 production release frontend contract`；精确包含 7 个 T4 前端文件，提交后 PQC worktree 干净。
- real E2E boundary：本阶段未启动服务、未使用账号、未写业务数据；真实多账号 Playwright 写入链路按计划在 T11 使用任务自有数据执行，静态合同不冒充真实 E2E。
- milestone result：T4 implementation and required local verification completed；M4（SP-1）完成，执行点推进到 T5。

## Pass 11 - T5 SP-2 Backend BDD And RED

- task id：`T5`。
- BDD: PQC 批准后才创建申请唯一批次 -> Given 申请处于 `PQC_RELEASE_PENDING`、版本匹配且当前用户同时属于冻结待办候选与启用的 `MES_PQC_RELEASE_OWNER` / When PQC 批准 / Then 同一事务创建或取得 `PQC_RELEASE:{applicationId}` 唯一批次、写入三类正式映射、初始化四个报告上传待办，并把申请推进为 `REPORT_UPLOAD_PENDING`。
- BDD: PQC 拒绝不创建下游对象 -> Given 合法的 PQC 待办和拒绝原因 / When PQC 拒绝 / Then 申请终止于 `PQC_RELEASE_REJECTED`，批次、三类映射和报告上传待办均不创建。
- BDD: 角色与冻结候选必须同时满足 -> Given 当前用户只满足当前角色或只存在于历史候选快照 / When 尝试处理 PQC 待办 / Then 返回结构化权限 blocker，且不执行批次或映射写入。
- BDD: 正式报告来源不可被动态表单替代 -> Given 批记录、过程检验或损耗报告缺少正式逐工序绑定，而仅存在 `formBindings` 动态表单 / When PQC 批准 / Then 返回对应正式来源 blocker，不创建批次，动态表单不作为替代来源。
- BDD: 旧批次不得被隐式复用 -> Given 同工单、批号、路线已有批次但没有 `PQC_RELEASE:{applicationId}` 有效关联 / When PQC 批准 / Then 返回 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`，禁止猜测或复用旧批次。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcReleaseBatchExecutionServiceTest#pqcApproveCreatesBatchExecutionOnlyAfterPqcRelease" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL；MES `testCompile` 仅缺计划内 PQC service、唯一批次端口、三类 dossier 端口及四报告阶段初始化合同，符合 T5 预期 RED。

## Pass 12 - T5 SP-2 Backend GREEN And Verification

- implementation：PQC 批准在同一调用方事务内校验冻结候选、当前启用角色、申请版本和正式来源快照；通过 `PQC_RELEASE:{applicationId}` 创建或取得唯一批次，写入批记录、过程检验、损耗三类正式映射，初始化四个报告上传待办，并以 CAS 推进 `REPORT_UPLOAD_PENDING`。PQC 拒绝只推进 `PQC_RELEASE_REJECTED`，不创建任何下游对象。
- formal source gate：三类 writer 均要求逐工序正式 `batchRecordReportId` 绑定；`formBindings` 不参与批记录表单推断。旧的同工单/批号/路线批次没有生产放行上下文时返回 `LEGACY_BATCH_EXECUTION_MIGRATION_REQUIRED`，禁止隐式复用。
- persistence hardening：报告上传 `FILL` 待办直接写入映射表时补齐确定性 `taskCode`，并由单测捕获四条写入记录验证任务编号、候选快照和节点数量。
- BDD: PQC 批准创建唯一批次、三类正式映射和四报告待办 -> Given `PQC_RELEASE_PENDING` 且冻结候选与当前角色同时授权 / When 批准 / Then 申请进入 `REPORT_UPLOAD_PENDING`，下游对象只创建一次。
- BDD: PQC 拒绝无下游副作用 -> Given 合法待办和拒绝原因 / When 拒绝 / Then 申请进入 `PQC_RELEASE_REJECTED`，不创建批次、映射或报告任务。
- BDD: 正式来源和旧批次严格阻塞 -> Given 缺少逐工序正式绑定或存在无生产放行上下文的旧批次 / When 批准 / Then 返回结构化 blocker，不使用动态表单或旧批次猜测。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseReportStageInitializerTest" test` -> PASS；2 tests，0 failures，0 errors，覆盖四报告任务和 `taskCode` 持久化合同。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseBatchExecutionPortTest,MesProductionReleaseReportStageInitializerTest,MesProductionReleaseControllerJsonTest,MesTeamLeaderActiveOrderReleaseAuditRecorderTest" test` -> PASS；19 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseApplySp1Test,MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest,MesTeamLeaderActiveOrderReleaseLossReportWriterTest,MesReleaseFlowCoreContractTest,MesProEdhrBatchExecutionServiceTest#openOrCreate_mustGenerateSpecialNoTemplateNodesAroundRouteForms+detailTask_includesFillableUsersFromStartBatchRecordAttachmentOwnersForSpecialNodes" test` -> PASS；46 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules，MES 主源码和前置依赖编译成功。
- GREEN: `git diff --check` -> PASS；新增 T5 文件与修改文件无空白错误。风险词扫描仅命中正式 `MesProEdhrWorkTaskStatus.TODO` 和测试 Mockito，未发现 fallback、default-success、FIXME、password、secret 或 token。
- runtime gate：未启动前后端或远程服务；未使用业务账号、未写业务数据；branch runtime guard 保持此前 slot 8 / frontend 8089 / backend 48089 约束。
- milestone result：T5 后端实现与目标回归完成，M5 后端门禁通过；下一阶段为 T6 SP-2 PQC 工作台前端。

## Pass 13 - Prerequisite Integration And T1/T2 Regression

- prerequisite integration：PQC 分支历史已包含前置源码提交 `28923b171 fix: add missing frontline prerequisite sources`，其后测试支撑提交为 `4aeb6f869`；当前 T5 实现提交为 `3048b84e8`。未复制未提交文件，未执行 merge、rebase、push。
- timeout evidence：首次 `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 在 244 秒命令超时；未将当时生成的报告单独视为 GREEN，随后在无并发 Maven 编译进程的窗口延长超时复跑。
- GREEN: 同一 T1/T2 reactor Maven 命令以 420 秒超时设置复跑 -> PASS；24 个 reactor modules，目标测试 15 项，0 failures，0 errors，0 skipped，BUILD SUCCESS。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS；6 passed。
- post-commit state：PQC worktree `git status --short --branch --untracked-files=all` 仅显示分支名，无 staged、未暂存或未跟踪实现文件；未 push，按用户授权保留本地提交。
- current next step：T5 已完成，推进 T6 SP-2 PQC 工作台前端；本任务整体仍为 `in_progress`，不执行 closeout 或 worktree 删除。

## Pass 14 - T6 SP-2 Frontend BDD And RED

- task id：`T6`。
- BDD: 只有冻结候选能看到 PQC 决策 -> Given 当前用户在真实候选待办页，且任务类型为 `PQC_PRODUCTION_RELEASE`、状态为 `TODO`、当前用户仍属于冻结候选 / When 查看任务操作 / Then 显示“PQC通过”和“PQC拒绝”；普通任务、已完成任务和非候选页不显示该决策入口。
- BDD: 拒绝原因必填且拒绝终止 -> Given PQC 拒绝弹窗已打开 / When 原因为空时提交 / Then 前端阻止请求；填写有效原因并收到 `PQC_RELEASE_REJECTED` 回执后只显示终态，不提供重新申请或撤回操作。
- BDD: 批准回执展示正式下游摘要 -> Given PQC 以权威版本批准申请 / When 后端返回 `REPORT_UPLOAD_PENDING` / Then 页面展示正式批次编号和四个报告上传任务摘要，不从动态表单或旧批次推断结果。
- BDD: 版本冲突和不确定响应不得误报成功 -> Given 后端返回结构化 blocker，或写请求响应不确定 / When 页面处理失败 / Then blocker 按类型、原因和建议展示；只有按 applicationId 查询权威回执确认决策后才展示成功，否则锁定本次操作并提示人工核对。
- RED target：新增 `sp2-pqc-production-release-contract.spec.cjs`，冻结三个正式 API、字符串 Long ID、候选任务入口、拒绝原因、版本与幂等键、结构化 blocker、权威回执恢复及批准后的批次/四报告摘要。
- RED: `node tests/e2e/sp2-pqc-production-release-contract.spec.cjs` -> FAIL；首个失败为生产放行前端 API 文件不存在，证明 SP-2 页面尚无正式批准、拒绝和权威回执合同。
- RED: `pnpm test sp2-pqc-production-release-contract` -> FAIL；业务合同直接执行已转绿后，命名测试运行器仍返回 `Unknown frontend test target`，证明 T6 尚未接入统一命名验证入口。

## Pass 15 - T6 SP-2 Frontend GREEN And Commit Blocker

- implementation：候选待办查询不再固定为普通 `REVIEW`；新增 `PQC_PRODUCTION_RELEASE` 类型、字符串 Long ID 和任务版本投影。只有候选页 `TODO` 且业务范围为 `RELEASE_APPLICATION` 的任务展示 PQC 通过/拒绝动作，并继续叠加正式权限指令。
- decision contract：新增批准、拒绝和按 applicationId 查询权威回执 API；拒绝原因前端必填，写入请求携带权威版本和 ASCII 幂等键。结构化业务失败展示 blocker 类型、原因、建议和对象；非结构化响应异常只在 GET 回执确认相同决定后展示成功，否则锁定任务并提示人工核对。
- receipt projection：批准回执严格要求 `REPORT_UPLOAD_PENDING`、正式批次编号和恰好四个报告上传待办；拒绝回执严格要求 `PQC_RELEASE_REJECTED` 和非空原因。页面不提供拒绝后的重新申请、撤回或其它绕过操作。
- source review correction：后端对合法候选也会写入“当前用户在候选池中，需按候选审核路径处理”的 `inactionReason` 证据；该字段不是禁用标记。T6 已移除错误的非空门禁，并以负向合同防止合法候选动作再次被隐藏。
- GREEN: `pnpm test sp2-pqc-production-release-contract` -> PASS；统一命名测试入口已登记。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- GREEN: 四组 eDHR 工作台相邻静态合同 -> 全部 PASS。
- GREEN: `pnpm test sp1-production-release-contract` -> PASS；SP-1 前端合同未回归。
- static gates：任务文件 `git diff --check` PASS；风险词扫描仅命中正式 `TODO` 状态、测试中的 `default-success` 禁止性断言和既有命名，不存在秘密、临时成功或模拟成功逻辑。
- COMMIT BLOCKER: `scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL；共享登记中并发 worktree `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` 使用 slot 20，而当前规则只允许 1..19。该登记不属于本任务；按共享环境规则未修改、未释放、未绕过。T6 暂存区保持为空，未提交、未 push。
- real E2E boundary：未启动服务、未使用账号、未写业务数据；真实 PQC 角色和非候选角色路径仍按计划留在 T11。

## Pass 16 - T7 SP-3 Backend BDD And RED Preparation

- task id：`T7`。
- BDD: 四份报告待办按冻结负责人并行可见 -> Given PQC 已通过且申请为 `REPORT_UPLOAD_PENDING` / When 来料检、灭菌、成品检负责人查询候选待办 / Then 只返回本人冻结候选范围内的目标节点，支持 `nodeTypes` 与 `batchExecutionId` 过滤，并返回节点名称和申请当前版本。
- BDD: 目标报告节点不可跳过或改写 -> Given 工作待办属于 `RELEASE_REPORT_NODE` / When 调用 skip、删除待上传、撤回或完成后覆盖 / Then 返回结构化 `UNSUPPORTED_RELEASE_ACTION` 或报告锁定 blocker，任务、附件和申请不变。
- BDD: 报告完成使用版本和幂等门禁 -> Given 当前用户是冻结候选、附件有效且 expectedVersion 匹配 / When 完成自己的报告任务 / Then 附件和哈希被锁定、对应 FILL 待办完成、申请版本加一；同键同载荷重放返回原回执，同键异载荷或异键重复完成明确冲突。
- BDD: 前三份不初始化最终阶段 -> Given 四份报告中的任意一至三份已完成 / When 当前报告提交成功 / Then 申请保持 `REPORT_UPLOAD_PENDING`，不存在放行事务或管理者代表待办。
- BDD: 第四份和管理者阶段原子交接 -> Given 前三份已锁定且第四份有效 / When 第四份完成 / Then 同一事务冻结四报告快照、调用管理者阶段 provider、保存唯一事务及待办并推进 `MANAGER_RELEASE_PENDING`；provider 或 CAS 失败时第四份完成和附件写入一并回滚。
- RED target：新增 `MesProductionReleaseReportServiceTest`，先冻结候选授权、版本、幂等、前三份门禁、第四份 provider 交接和失败不落应用状态的服务合同；现有源码缺少 `productionrelease/report/**` 协调服务和 manager-stage provider，预期测试编译失败。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProductionReleaseReportServiceTest" test` -> FAIL；`testCompile` 的 6 个错误仅缺计划内 report service、node port、manager-stage initializer、command/evidence 合同，符合 T7 预期 RED。

## Pass 17 - T7 SP-3 Backend GREEN And Verification

- task id：`T7`。
- implementation：共享待办查询移除 `REVIEW` 硬编码，支持四类报告节点与批次过滤，并投影节点名称及申请版本；报告专用完成服务按申请、批次节点和工作待办加锁，校验冻结候选、`expectedVersion`、ASCII 幂等键和附件证据。
- attachment gate：报告附件准备使用同键同载荷重放；同键异载荷返回 `IDEMPOTENCY_PAYLOAD_CONFLICT`，同文件异键或完成后改写返回报告锁定 blocker。报告节点禁止通用 skip、旧 complete、删除待上传及批量保存入口，避免绕过版本和幂等门禁。
- stage gate：前三份报告完成只以 CAS 增加申请版本并保持 `REPORT_UPLOAD_PENDING`；第四份完成冻结四报告快照，要求正式 manager-stage initializer 返回放行事务、管理者待办和候选快照，并以单次 CAS 推进 `MANAGER_RELEASE_PENDING`。provider、证据或 CAS 失败时异常向上传播，由调用方事务整体回滚。
- BDD: 四份报告候选、不可跳过、版本/幂等、前三份无下游和第四份原子交接 -> Given T7 正式申请、报告节点和冻结候选 / When 准备附件或完成报告 / Then 仅授权候选可操作，附件不可覆盖，版本逐份推进，前三份不创建管理者阶段，第四份与管理者任务原子交接。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseReportServiceTest,MesProductionReleaseReportControllerContractTest,MesProductionReleaseWorkTaskQueryTest,MesProductionReleaseReportPersistenceContractTest,MesProductionReleaseReportStageInitializerTest,MesProEdhrBatchExecutionServiceTest#productionReleaseReportNodesRejectLegacySkipCompleteDeleteAndSavePendingActions+productionReleaseReportPrepareUploadReplaysSameKeyAndRejectsChangedPayload+prepareSpecialNodeAttachmentUpload_returnsTaskScopedMetadata+prepareSpecialNodeAttachmentUpload_persistsPendingAttachmentForReload+completeSpecialNode_persistsAttachmentsAndArchiveManifestContainsSpecialEvidence+specialNodeSkip_requiresReasonAndPasswordAndRecordsSignature" "-DforkCount=0" test` -> PASS；23 tests，0 failures，0 errors，0 skipped。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesPqcReleaseBatchExecutionServiceTest,MesReleaseFlowCoreContractTest,MesProductionReleaseReportStageInitializerTest,MesProEdhrWorkTaskServiceImplTest#getCandidateSignatureTodoPage_returnsResponsibilitySourceCandidatePoolAndInactionReason" "-DforkCount=0" test` -> PASS；17 tests，0 failures，0 errors，0 skipped。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules 全部成功。
- static gates：`git diff --check -- IntRuoyiBackend` PASS；风险词扫描未发现 fallback、default-success、FIXME 或真实秘密。命中项仅为正式 `TODO` 状态、既有签名 password 流程、上传 token 字段/局部变量和测试 Mockito。
- git state：暂存区为空；T6/T7 文件保持未暂存。共享 branch runtime guard 的非法 slot 20 阻塞仍未由本任务处理，因此未提交、未 push。
- milestone result：T7 completed；依赖图允许 T8 开始，任务整体仍为 `in_progress`。

## Pass 18 - T8 SP-3 Frontend BDD And RED Preparation

- task id：`T8`。
- BDD: 三类负责人只见自己的四报告待办 -> Given PQC 已通过且四个报告待办按冻结负责人生成 / When 来料检、灭菌和成品检负责人进入候选待办 / Then 分别只看到 1、1、2 个可处理任务，入口携带字符串任务 ID、节点类型、批次和申请当前版本。
- BDD: 报告附件准备必须使用版本与幂等 -> Given 当前用户是目标报告冻结候选 / When 从批次详情选择真实文件 / Then prepare 请求携带 taskId、expectedVersion、同文件稳定幂等键，成功后展示文件名、哈希和待提交状态，失败明确展示且不伪造附件。
- BDD: 四报告完成不可绕过 -> Given 目标节点是四类生产放行报告之一 / When 查看详情或完成报告 / Then 页面不提供 skip、删除待上传、批量暂存或覆盖入口；缺附件、灭菌批号、候选任务或权威版本时前端阻止提交并说明原因。
- BDD: 第四份回执展示管理者阶段 -> Given 前三份已完成且当前提交第四份 / When 后端返回 `MANAGER_RELEASE_PENDING` / Then 页面展示正式放行事务和管理者待办已建立；前三份只展示申请版本已推进，不推断最终阶段。
- RED target：新增 `sp3-production-release-report-upload-contract.spec.cjs`，冻结候选查询字段、字符串 ID、报告专用 prepare/complete、工作台入口、版本/幂等、四报告禁用 skip/delete 和第四份阶段回执。
- RED: `node tests/e2e/sp3-production-release-report-upload-contract.spec.cjs` -> FAIL；首个断言为候选查询缺少 `nodeTypes?: string[]`，证明当前前端无法按四类报告节点和批次读取本人冻结候选任务，符合 T8 预期。

## Pass 19 - T8 SP-3 Frontend GREEN And Verification

- task id：`T8`。
- implementation：候选待办识别 `RELEASE_REPORT_NODE + FILL + 四类 nodeType`，只在候选 TODO 且批次、批次任务、工作待办和版本齐全时显示“上传报告”；进入批次详情时保留所有字符串 ID、节点类型和 `expectedVersion`。
- candidate and permission：批次详情按四类 `nodeTypes + batchExecutionId` 查询当前用户候选待办，逐项校验字符串 ID、节点类型、批次和版本；非候选、加载失败、任务结束或版本缺失时操作明确禁用并显示原因。
- attachment and completion：报告附件 prepare 使用 taskId、权威版本和按文件稳定的 ASCII 幂等键，并校验字符串文件 ID、SHA-256 和留存哈希；完成使用独立稳定幂等键，灭菌报告强制灭菌批号。回执必须为 `COMPLETED` 且版本加一；第四份必须包含报告快照、放行事务和管理者代表待办，页面显示正式阶段建立结果。
- no bypass：四报告节点不显示 skip 或待提交附件删除入口；完成后禁止补传，放行前批量暂存检测到报告附件时直接阻塞。普通特殊节点的既有跳过、删除、补充附件和保存行为保持不变。
- regression fix：首次相邻合同发现上传锁定条件误作用于全部特殊节点；已收窄为只锁定已完成的四报告节点，普通特殊节点补充附件合同恢复。
- GREEN: `pnpm test sp3-production-release-report-upload-contract` -> PASS。
- GREEN: `pnpm test sp2-pqc-production-release-contract` -> PASS。
- GREEN: `pnpm test sp1-production-release-contract` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-open-task-worktaskid-static.spec.js` -> PASS；工作待办到批次详情入口回归通过。
- GREEN: `node tests/e2e/edhr-work-task-formcenter-navigation-static.spec.js` -> PASS；共享工作待办导航回归通过。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- GREEN: `git diff --check -- IntRuoyiFronted` -> PASS。
- baseline tests：`edhr-special-node-attachment-actions-static.spec.js` 在主分支已用 `{ taskId, attachment, reason }`，但旧断言仍要求无 `reason`，随后还引用已不存在的 `openReleaseTransactionDialog`；`edhr-special-node-skip-signature-static.spec.js` 仍要求旧固定弹窗标题。均为任务开始前已存在的测试/源码不一致，本任务未修改这些旧测试，也未用其冒充 GREEN。
- static gate：扫描命中仅为正式 `TODO` 状态、上传 token、既有签名 password、既有归档文件名参数和测试中的 default-success 负向断言；未新增 fallback、default-success、FIXME、秘密、mock 数据或静默成功。
- E2E boundary：未启动服务、未使用账号或写业务数据；三类负责人 1/1/2 真实候选和四次真实文件上传留 T11。
- milestone result：T8 completed，M6 completed；下一开发项为 T9。

## Pass 20 - T9 SP-4 Backend BDD And RED

- task id：`T9`。
- BDD: 管理者角色和冻结候选双重授权 -> Given 当前用户既属于当前租户启用的 `MES_MANAGEMENT_REPRESENTATIVE`，又存在于第四份报告完成时冻结的最终待办候选中 / When 提交最终批准 / Then 才能处理；route rule、用户名、固定用户 ID、普通管理员身份均不能替代。
- BDD: 报告快照变化阻断 -> Given SP-3 已冻结四报告附件、哈希、版本和灭菌批号 / When 管理者代表批准前重算快照 / Then 任一证据变化都返回 `REPORT_SNAPSHOT_CHANGED`，放行事务、申请和待办均不写入。
- BDD: 最终放行双 CAS 和幂等 -> Given 申请为 `MANAGER_RELEASE_PENDING`、事务为 `PENDING_APPROVAL` / When 携带 taskId、事务 expectedVersion、可见 ASCII 幂等键和正式电子签名批准 / Then 事务、申请、待办、事件和审计原子进入 `RELEASED`；同键同载荷重放，同键异载荷和旧版本失败。
- BDD: 目标流程不支持拒绝或撤回 -> Given 放行事务属于生产放行申请 / When 调用 reject 或 withdraw / Then 返回 `UNSUPPORTED_RELEASE_ACTION`，保持管理者待办待处理。
- BDD: trace 双条件强制 -> Given 调用方打开可追溯并尝试传入其它状态 / When `completedTraceOnly=true` / Then 服务端覆盖为 `RELEASED` 且批次必须存在已放行事务，归档或驳回状态不能替代。
- RED: `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseManagerStageInitializerTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseTraceContractTest" "-DforkCount=0" test` -> FAIL；4 个测试编译错误仅指向缺少计划内 manager provider、approval service 和共享快照类。

## Pass 21 - T9 SP-4 Backend GREEN And Verification

- implementation：新增正式 manager-stage provider，以当前租户管理者代表角色创建 `PENDING_APPROVAL` 放行事务和冻结 `RELEASE_APPROVE` 待办；provider 复核申请版本、批次和四报告快照，已有事务时 fail fast。
- final approval：审批请求扩展 `workTaskId/expectedVersion`，后端同时校验当前角色、冻结候选、待办作用域、事务版本、申请状态、电子签名和四报告证据；事务与申请分别执行条件 CAS，并在同一事务完成待办、事件和 SP-4 审计。
- idempotency and actions：同键同载荷从事件快照重放已放行回执；同键异载荷、旧版本、非角色/非候选和报告变化均有结构化 blocker；目标 reject/withdraw 明确禁止。
- trace：`completedTraceOnly` 强制覆盖 releaseStatus 为 `RELEASED`；批次 mapper 只允许存在已放行事务，不再把归档/驳回批次当完成替代条件。
- GREEN: T9 定向测试 -> PASS；9 tests，0 failures，0 errors，0 skipped。
- GREEN: T9 + `MesProEdhrReleaseServiceImplTest` -> PASS；35 tests，0 failures，0 errors，0 skipped。
- GREEN: release/SP-3/core/role 组合回归 -> PASS；54 tests，0 failures，0 errors，0 skipped。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules 全部成功。
- static gate：`git diff --check -- IntRuoyiBackend\yudao-module-mes` PASS；风险词扫描待文档证据校验后统一执行。未启动服务、未写业务数据。
- git state：T6 至 T9 改动保持未暂存；共享 branch runtime guard 非法 slot 20 阻塞仍未由本任务修改或绕过。
- milestone result：T9 completed；下一开发项为 T10 管理者审批和可追溯前端。

## Pass 22 - T10 SP-4 Frontend BDD And RED

- task id：`T10`。
- BDD: 只有管理者冻结候选可最终放行 -> Given 当前用户在候选待办页读取 `RELEASE_APPROVE` 任务 / When 任务为 `RELEASE_TRANSACTION + TODO` 且权限有效 / Then 才显示最终放行入口；非候选、非目标任务和终态任务无入口。
- BDD: 首版最终阶段无拒绝 -> Given 管理者最终待办 / When 查看操作区和弹窗 / Then 只有批准，不显示拒绝、退回或撤回。
- BDD: 权威版本与回执确认 -> Given 事务为 `PENDING_APPROVAL` / When 提交事务、待办、版本、幂等键和正式签核证据 / Then 只接受身份一致的 `RELEASED` 回执；响应不确定时通过正式 GET 回执恢复，否则锁定并提示人工核对。
- BDD: trace 双条件固定 -> Given 打开放行追溯页签 / When 查询分页 / Then 同时发送 `completedTraceOnly=true` 和 `releaseStatus=RELEASED`，不能用归档或驳回批次替代。
- RED: `node tests/e2e/sp4-manager-release-trace-contract.spec.cjs` -> FAIL；首个失败为 `releaseTransactionId?: string` 缺失，证明旧 API 仍把 Long ID 建模为 number，且管理者最终放行页面合同未建立。

## Pass 23 - T10 SP-4 Frontend GREEN And Verification

- implementation：候选工作待办新增管理者专用操作区，只接受 `RELEASE_APPROVE + RELEASE_TRANSACTION + TODO` 的完整字符串 ID/版本上下文，并叠加 `mes:pro-edhr-release:approve` 权限；旧 route-rule 配置只保留配置职责，不参与运行时候选判定。
- final approval：打开弹窗先读取权威事务回执；请求携带 releaseTransactionId、workTaskId、expectedVersion、稳定 UUID 幂等键、正式签核证据和可选意见。专用区域不存在 reject/return/withdraw；签核证据缺失时前端校验阻止请求。
- receipt recovery：写响应异常或回执不完整时按原事务 ID 重新读取，只有权威状态 `RELEASED` 且待办、事务、签核证据一致才保留成功；否则锁定事务。已确认成功后列表刷新失败只显示独立 warning。
- precision and trace：Release API 的事务、批次、待办及事件 Long ID 改为字符串；事件列表不再转换为 number。表单追溯放行页签固定发送 `completedTraceOnly=true + releaseStatus=RELEASED`；通用事务历史页继续保留全状态只读筛选，避免破坏既有审计合同。
- GREEN: `pnpm test sp4-manager-release-trace-contract` -> PASS。
- GREEN: SP-1、SP-2、SP-3、SP-4 四个命名合同 -> 全部 PASS。
- GREEN: release transaction、form trace tabs、trace actions、owner return、precheck 相邻静态合同 -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS；退出码 0。
- GREEN: T10 文件 `git diff --check` -> PASS；风险词扫描没有新增 fallback、default-success、FIXME、秘密、token 或 mock 成功逻辑，命中仅为正式 TODO 状态常量。
- baseline note：额外 `edhr-release-flow-trace-print-static.spec.js` 仍因开始前已存在的 ViewModel 定位断言与当前批次详情结构不一致失败；该断言不涉及 T10 修改的管理者放行和双条件 trace，未用于 GREEN 证据，留 T11 回归盘点。
- git state：暂存区为空；T6 至 T10 保持未暂存。共享 branch runtime guard 的非法 slot 20 登记未由本任务修改或绕过，未提交、未 push。
- milestone result：T10 completed；开发实现阶段全部完成，下一项为 T11 全链路集成、真实 E2E 与独立验收。

## Pass 24 - T11 Real Multi-Account E2E Specification And Environment Gate

- task id：`T11`；本轮仅新增真实 Playwright 规格并执行无运行态依赖的集成回归，不修改业务源码、`task-state.json` 或 `test-report.md`。
- BDD: 双 100% 后只创建 PQC 待办 -> Given 生产组长在真实页面看到任务自有主链和拒绝链两个双 100% 活跃订单 / When 分别点击完工并确认申请 / Then 每次响应只有申请与 PQC 待办，状态为 `PQC_RELEASE_PENDING`，没有批次、报告或放行事务。
- BDD: PQC 角色候选通过或终态拒绝 -> Given `zhulijiang` 是当前租户启用的 PQC 角色及冻结候选且非候选账号可登录共享待办页 / When 非候选检查无操作入口，PQC 对拒绝链填写原因拒绝、对主链通过 / Then 拒绝链无下游且不能重开；主链原子返回唯一批次、三类正式证据和四个字符串 ID 报告待办。
- BDD: 三类负责人以 1/1/2 完成四报告 -> Given 来料、灭菌、成品三个不同账号各自读取冻结候选待办 / When 通过工作待办真实进入批次详情，上传四个真实文件并完成报告 / Then 来料和灭菌各完成一份、成品完成两份，灭菌批号必填；前三份保持报告阶段，第四份返回唯一放行事务和管理者待办，完成账号无候选待办残留。
- BDD: 管理者代表放行并进入双条件追溯 -> Given `xujianhai` 同时是管理者角色与冻结候选，非候选账号没有最终放行入口 / When 从真实待办页提交正式 SHA-256 签核证据 / Then 事务、申请和待办进入 `RELEASED`；表单追溯页面请求固定携带 `completedTraceOnly=true` 与 `releaseStatus=RELEASED`，展示四附件和电子签名，拒绝链不出现。
- BDD: 缺正式运行前置时零写入阻断 -> Given branch runtime guard、七个账号凭据、两个双 100% 活跃订单 fixture、四个附件、灭菌批号或签核证据任一缺失 / When 准备执行写入型 E2E / Then 在启动服务和业务写请求之前记录 `BLOCKED`，不得 mock、API-only、SQL 推状态、旧运行产物或随机端口降级。
- specification：新增 `IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts`；单个 serial Playwright 用例串联生产组长→非候选→PQC→来料/灭菌/成品负责人→非候选→管理者→trace，逐个页面登录七个不同账号，捕获目标写请求身份、权威响应、console/page/request failure，并通过 `testInfo` 附加结果 JSON 与 trace 截图。规格只从环境变量读取凭据，不含明文账号密码，不直接调用写 API，不写数据库。
- static prerequisite contract：规格要求本机 HTTP 入口、显式写入确认、`zhulijiang`、`xujianhai`、七个互异账号、主链/拒绝链两个独立 fixture、四个真实附件路径、灭菌批号和 64 位 SHA-256 签核证据；文件缺失或前置为空时 fail fast。
- GREEN: `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` -> PASS；Playwright 识别 1 个 Chromium 真实多账号用例，证明 spec 与 runner 入口可解析，但该结果不是业务 E2E PASS。
- GREEN: `pnpm exec prettier --check tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` -> PASS。
- GREEN: `pnpm test sp1-production-release-contract`、`sp2-pqc-production-release-contract`、`sp3-production-release-report-upload-contract`、`sp4-manager-release-trace-contract` -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- GREEN: `mvn -o -pl yudao-module-mes "-Dtest=MesProductionReleaseApplySp1Test,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseReportServiceTest,MesProductionReleaseManagerApprovalServiceTest,MesProductionReleaseTraceContractTest,MesProEdhrReleaseServiceImplTest" "-DforkCount=0" test` -> PASS；60 tests，0 failures，0 errors，0 skipped。
- GREEN: `mvn -o -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 个 reactor modules 全部成功。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_production_release_roles_sql.py -q` -> PASS；6 passed。
- GREEN: `git diff --no-index --check -- NUL IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` -> PASS；spec 尚未跟踪，因此使用 no-index 精确检查，只有 Windows LF→CRLF 归一化提醒，无 whitespace error。
- runtime registry：目标 worktree 仍正式登记 `int_main slot=8`、frontend `8089`、backend `48089`、active=true；`Get-NetTCPConnection` 确认 8089/48089 均无监听。
- PREFLIGHT BLOCKED: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/preflight/branch-runtime-port-guard.ps1` -> FAIL；共享登记项 `D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-backfill-remediation` 为 slot 20，而当前 PQC worktree 内 guard 仍执行 v3 `1..19` 合同。主工作区已有 v4 `1..30` 变更，但混在其它任务提交中，本任务未 cherry-pick、复制或绕过无关变更。
- PREFLIGHT BLOCKED: 27 个 `EDHR_FULL_E2E_*` 正式变量全部未配置，其中七个账号密码变量均为空；同时缺本机入口、租户、写入确认、七个用户名、两个活跃订单 fixture、四个附件路径、灭菌批号和管理者签核证据。检查仅记录变量名和是否存在，没有读取或输出秘密。
- E2E result：未运行真实 Playwright 写入用例，未启动任何服务，未登录、未上传文件、未调用目标写接口、未写业务数据；因此 TC-13、AC-01 至 AC-34 的系统级真实页面证明仍为 `BLOCKED`，不能用 `--list`、named static、Maven 或 API-only 结果冒充。
- regression inventory：`node tests/e2e/edhr-release-flow-trace-print-static.spec.js` 仍 FAIL 于“必须能定位放行流程步骤 ViewModel”；`edhr-special-node-attachment-actions-static.spec.js` 仍 FAIL 于旧删除待提交附件断言；`edhr-special-node-skip-signature-static.spec.js` 仍 FAIL 于旧固定弹窗标题。三项与 Pass 19/23 已冻结的任务开始前测试/源码不一致相同，本 executor 无这些文件 write scope，未修改断言或业务源码绕过；T11 全量回归不能记为全绿。
- risk scan：新增 spec 对 `fallback|mock|default-success|TODO|FIXME|password|secret|token` 的命中只有环境密码字段、密码输入选择器、正式 `TODO` 工作任务状态和 `candidate-todo-page` 路径；没有明文秘密、mock、fallback、默认成功或静默降级。
- milestone result：T11 保持 blocked；解除条件为正式同步 v4 guard（不得整体接入无关提交）、配置七个测试账号与完整任务 fixture、在 slot 8 当前代码成对运行态执行该 Playwright 用例，并由独立 tester 复验 AC-01 至 AC-34。

## Pass 25 - T11 Adjacent Static Regression Closure

- task id：`T11`；针对 Pass 24 暴露的三项相邻静态回归执行根因核对和最小修复。
- RED: `node tests/e2e/edhr-release-flow-trace-print-static.spec.js` -> FAIL；旧断言定位仓库所有 worktree 中均不存在的 `releaseFlowStepsViewModel`，未识别当前批次详情的单一放行工序入口。
- RED: `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> FAIL；旧断言未包含当前正式删除原因字段，并仍定位已被电子签名入口替代的 `openReleaseTransactionDialog`。
- RED: `node tests/e2e/edhr-special-node-skip-signature-static.spec.js` -> FAIL；旧断言要求已被统一附件面板明确禁止的弹窗私有上传处理器；继续核对发现后端已在 `specialPayloadJson` 写入 `skipReason/skipSignatureId`，但页面未展示。
- implementation：放行打印合同改为验证当前单一放行工序入口与表单追溯打印；附件删除合同强化为非空原因加后端删除调用；跳过合同改为验证动态正式标题和统一附件列表，并在批次详情只读展示跳过原因与签名证据编号。
- GREEN: `node tests/e2e/edhr-release-flow-trace-print-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-special-node-attachment-actions-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-special-node-skip-signature-static.spec.js` -> PASS。
- GREEN: SP-1、SP-2、SP-3、SP-4 四个命名合同 -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- GREEN: `pnpm exec prettier --check`（T11 涉及的批次详情、三项相邻静态合同和真实 E2E 规格）-> PASS。
- GREEN: `git diff --check`（T11 涉及的批次详情、三项相邻静态合同和真实 E2E 规格）-> PASS；只有 Windows LF→CRLF 归一化提醒，无 whitespace error。
- regression correction：Prettier 将隐藏上传控件的 `disabled` 表达式合法换行后，附件动作静态合同因单行文本写死出现假失败；已将断言改为仅容忍空白换行的结构正则，重新执行三项相邻静态合同均 PASS，页面仍使用独立 `canUploadSpecialNodeAttachment` 门禁。
- scope：未改跳过、删除、上传或放行业务写入路径；没有恢复私有附件状态、跳过生产放行报告节点或其它绕过。三项静态回归阻塞已解除，T11 仍被真实运行态/fixture 和独立 tester 门禁阻塞。

## Pass 26 - T11 Independent Verification

- independent tester：由不同于 T11 executor 的 tester 独立复跑三项相邻静态合同、SP-1 至 SP-4 命名合同、Playwright 规格 `--list`、T11 涉及文件 Prettier 与 `git diff --check`，全部 PASS；完整证据写入 `test-report.md`。
- evidence review：tester 核对 Pass 24 的 Maven 60 tests、24 模块 compile、角色 SQL 6 tests 与 `pnpm ts:check` PASS 记录，但未把既有证据或 Playwright `--list` 冒充真实 E2E。
- BLOCKED: runtime guard 实际复跑 FAIL；目标分支仍限定 slot `1..19`，并发 worktree 使用 v4 slot 20。27 项 `EDHR_FULL_E2E_*` 必需变量中 present 0、missing 27；检查未读取或输出秘密值。
- independent verdict：P11/T11 `BLOCKED`。真实七账号页面链、反向权限、两条任务 fixture、四附件上传、管理者签核、双条件追溯和测试数据清理均未执行；未启动服务、未登录、未写业务数据。
- state update：主 Agent 已移除“静态回归失败”和“独立 tester 证据未写入”两条过期 blocker；保留 runtime guard 与 27 项真实 E2E 前置缺失，P11 和总任务继续为 `blocked`。
- experience consolidation：将“长期分支旧 guard 不得通过改写并发合法登记来绕过”的通用门禁合并到 `docs/worktree-memory.md` 和 `docs/experience-index.md`；静态合同换行误判已有 `docs/e2e-rules.md#Windows-换行与脚本行为同步` 覆盖，未重复新增规则。

## Pass 27 - T11 Runtime V4 Guard Resolution

- scope：只同步 branch runtime v4 治理文件，不修改其它任务登记，不整体接入包含无关业务变化的提交。
- source audit：确认 `ec86297a5` 是包含正式 v4 `1..30` 合同的可审计来源；七个纯运行治理文件与该提交逐文件一致，`docs/local-runtime.md` 仅同步 v4 端口合同，未带入无关 DCC 文档变化。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` -> PASS；14 passed。
- GREEN: PowerShell parser 校验 `scripts/runtime/branch-runtime-profile.ps1` -> PASS。
- GREEN: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；目标分支 slot 8 解析为 frontend `8089`、backend `48089`。
- GREEN: 八个治理文件 `git diff --cached --check` -> PASS；风险词扫描未发现新增秘密、mock 成功、default-success 或临时降级。
- git commit：`b68db945ba6928b576907831fe001f9d454ed53c`，message `fix: sync worktree runtime v4 guard`；提交仅包含八个运行治理文件，提交后暂存区为空，T6-T11 业务改动保持原状。
- blocker update：旧 `1..19` guard 阻塞已解除；剩余阻塞仅为真实 E2E 的 27 项正式变量、任务 fixture、附件、签核证据和未启动成对运行态。

## Pass 28 - T6-T11 Verified Commit Boundary

- scope：在 guard PASS 后固化已完成且有目标验证的 T6-T10 实现，再独立固化 T11 验收规格；不 push，不把 Playwright `--list` 冒充真实 E2E。
- GREEN: Maven 六组生产放行定向测试 -> PASS；60 tests，0 failures，0 errors，0 skipped。
- GREEN: SP-1、SP-2、SP-3、SP-4 四个前端命名合同 -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 三项相邻静态合同 -> 全部 PASS。
- GREEN: `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` -> PASS；识别 1 个 Chromium 用例，仅证明规格可解析。
- GREEN: T11 四个验收文件 Prettier -> PASS。
- staged boundary：T6-T10 暂存区精确为 57 个已核对业务/测试文件，`git diff --cached --check` 和运行守卫均 PASS；风险扫描命中仅为正式 token/password/TODO 字段、测试框架和禁止 default-success 的断言，无真实秘密或临时成功逻辑。
- git commit：`5227b8c2e`，message `feat: implement production release workflow`；57 files，5791 insertions，371 deletions。
- staged boundary：T11 暂存区精确为真实多账号规格和三项相邻静态合同共 4 个文件，`git diff --cached --check` 和运行守卫均 PASS。
- git commit：`336c82887`，message `test: add production release flow acceptance coverage`；4 files，906 insertions，46 deletions。
- environment audit：27 个 `EDHR_FULL_E2E_*` 正式变量 present 0、missing 27；8089/48089 均无监听。未启动服务、未登录、未上传文件、未调用业务写接口、未写业务数据。
- milestone result：T6-T10 实现和 T11 自动化资产已形成独立提交；P11 仍为 `blocked`，解除条件保持为正式真实 E2E 前置、slot 8 当前源码成对运行态、七账号页面链和独立复验。

## Pass 29 - T11 Incremental Independent Reverification

- independent tester：原独立 tester 在提交完成后仅更新 `test-report.md`，未修改源码、主状态、执行日志、暂存区或 Git 历史。
- GREEN: v4 runtime guard -> PASS；当前分支正式解析为 slot 8、frontend `8089`、backend `48089`，旧 guard blocker 已从独立报告移除。
- GREEN: commit boundary review -> PASS；`b68db945b` 仅 8 个运行治理文件，`5227b8c2e` 仅 57 个 T6-T10 实现/测试文件，`336c82887` 仅 4 个 T11 验收文件，三个提交线性包含于当前 HEAD。
- GREEN: target worktree status -> PASS；暂存、未暂存、未跟踪文件均为空。
- GREEN: 三项相邻静态合同、SP-1 至 SP-4 命名合同、Playwright `--list`、T11 四文件 Prettier 和 committed diff check -> 全部 PASS。
- BLOCKED: 27 项 `EDHR_FULL_E2E_*` 变量 present 0、missing 27；8089/48089 均无监听。独立检查未读取或输出任何秘密值，未启动服务。
- independent verdict：P11/T11 保持 `BLOCKED`；真实七账号页面链、反向权限、两条 fixture、四附件上传、管理者签核、双条件追溯和清理仍未执行。

## Pass 30 - Int Main Integration Pending Manual Acceptance

- user intent：用户明确授权先融合进 `int_main`，后续由用户手动测试；该授权只改变集成顺序，不等于 T11 验收通过。
- merge preflight：源 worktree `D:\IntRuoyiWorktree\pqc-production-release-flow` 干净，HEAD `336c82887`；主工作区 `int_main` 暂存区为空，25 个已跟踪并行改动和 11488 个未跟踪文件与 176 个源分支来入路径均为零重叠。
- integration isolation：创建 `D:\IntRuoyiWorktree\pqc-production-release-flow-integration`，分支 `codex/pqc-production-release-flow-integration`；正式登记 `int_main slot=24`、frontend `8158`、backend `48158`，未启动服务。
- conflict audit：三方合并出现 3 个冲突。两个 scheduler runtime 同名新增文件保留 `int_main` 较新正式实现；`MesTeamLeaderActiveOrderReleaseGenerationService` 采用功能分支的新生产放行实现。
- RED: 首次融合编译 -> FAIL；主分支在旧放行实现中孤立新增的 `setDccProjectCodeId` 没有数据对象字段、迁移列或本任务需求对应，合入新实现后编译报缺少 setter。
- resolution：移除该无正式数据模型支撑的孤立调用；功能分支的放行服务保持逐文件一致，scheduler 两文件保持与 `int_main` 一致，无冲突标记。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS；24 reactor modules 全部成功。
- GREEN: 生产放行六组定向测试加 `MesTeamLeaderActiveOrderServiceTest` -> PASS；90 tests，0 failures，0 errors，0 skipped。
- GREEN: SP-1、SP-2、SP-3、SP-4 前端命名合同 -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 三项相邻静态合同、Playwright 规格 `--list`、角色 SQL 6 tests -> 全部 PASS；`--list` 仍只证明规格可解析。
- GREEN: `git diff --cached --check` 和 branch runtime port guard -> PASS。
- merge commit：`ecb05caa615c384b3833dd9d7b9b9594df3ad30e`，message `merge: integrate production release flow`，父提交为 `1e8ec9b81` 与 `336c82887`。
- int_main integration：在 `E:\IntRuoyi` 执行 `git merge --ff-only codex/pqc-production-release-flow-integration` -> PASS；`int_main` 从 `1e8ec9b81` 快进到 `ecb05caa6`，post-merge guard PASS，原有并行改动未被暂存、覆盖或提交。
- release status：未 push、未启动服务、未登录、未写业务数据。P11 和总任务保持 `blocked`，当前状态为“已融合到 `int_main`，待用户手动验收”。

## Pass 31 - Temporary Integration Worktree Cleanup

- containment：`git merge-base --is-ancestor codex/pqc-production-release-flow-integration int_main` -> PASS；融合提交已包含于 `int_main`。
- safety：目标路径位于 `D:\IntRuoyiWorktree\`，无关联进程、8158/48158 无监听；Git 删除后确认目标无 `.git` 且不再出现在 `git worktree list --porcelain`。
- cleanup：`git worktree remove --force` 已移除 Git 登记；首次物理删除因 pnpm 依赖目录残留报告 `Directory not empty`，随后仅删除该任务自有的明确目标残留目录，最终 `Test-Path` 为 `False`。
- registry：只将 `pqc-production-release-flow-integration` 的 slot 24 登记更新为 `active=false`，并写入 `deletedAt` 与 `cleanupTask=20260814-production-release-flow-implementation`；其它登记未修改。
- closeout boundary：功能源 worktree `D:\IntRuoyiWorktree\pqc-production-release-flow` 保留，未删除；任务仍待用户手动验收，未标记 `completed`。

## Pass 32 - T11 Real E2E Specification Audit And Preflight Refresh

- task id：`T11`；本轮仅修改真实 Playwright 规格和本执行日志，不修改 `task-state.json`、`test-report.md`、业务源码或其它任务文件；未 stage、commit、push、启动或停止服务，也未登录、上传文件或写业务数据。
- BDD: 进度未完成不产生申请 -> Given 任务自有的未完成活跃订单 / When 生产组长在真实页面查看放行入口 / Then 生产和检验进度不能同时为 `100%`、申请按钮不可操作且目标 SP-1 写请求数为零。
- BDD: 角色边界不能互相越权 -> Given 主链已进入 PQC 或管理者待办阶段 / When 管理者代表查看 PQC 候选、PQC 查看管理者候选、非候选账号查看两个阶段 / Then 三者均没有对应处理动作。
- BDD: 已放行事实与最终只读核验一致 -> Given 四份报告已完成且管理者在真实页面确认放行 / When 页面返回 `RELEASED` 后执行只读核验并打开 trace / Then application、batch、release、manager task 的字符串 ID 与签核哈希一致，trace 固定双条件且可见四附件。
- BDD: 主链不得绕过报告或最终决策 -> Given 真实页面串联主链 / When 记录全部目标写请求 / Then 四报告 skip、待上传附件删除/暂存、最终 reject/withdraw 端点请求数均为零，最终对话框没有拒绝、退回或撤回动作。
- RED: `node -e "... markers=['EDHR_FULL_E2E_CLEANUP_PLAN_REFERENCE','verifyFinalReadOnlyState','refreshFailureInjected','FORBIDDEN_TARGET_WRITE_PATHS','expectStringId'] ..."` -> FAIL；原规格缺少清理计划标识、最终只读核验、禁止写端点观察和字符串 ID 合同。初版审计曾尝试把 Playwright `route.abort()` 刷新故障注入置入同一条真实多账号用例；复核确认这会把故障注入混入 TC-13 真实路径，已在最终实现前完整移除，不将其作为真实 E2E GREEN。AC-27 保留既有 SP-1/SP-4 静态/单元合同范围，真实运行态仍需另行、明确隔离的故障验证。
- implementation：新增一个未完成进度 fixture 的 fail-fast 环境合同及页面断言；主链仍要求主链/拒绝链两个独立双 100% fixture。新增 cleanup plan reference 作为正式前置并在 Playwright evidence 中标记 `PENDING_EXTERNAL_REAL_PAGE_CLEANUP`，不把未执行的清理写成完成。由于当前产品范围没有经核实的同页真实清理入口，该标记仅记录交接责任，不能替代 TC-13 清理结果。
- implementation：规格以真实页面成功后的管理者已登录上下文执行唯一的只读 `GET /mes/pro/edhr-release/get` 核验；该调用只检查最终放行事务、批次、管理者待办、签核和字符串 ID，不调用写 API，不用 SQL 推状态，也不替代页面主链。
- implementation：补足 application、PQC task、batch、四个 report task、附件、release 和 manager task 的非空字符串 ID 断言；四报告响应证据记录附件 ID 和 SHA-256；补足管理者与 PQC 的交叉反向权限检查、最终对话框的无拒绝/退回/撤回断言，以及 skip/delete/save-pending/reject/withdraw 零写入观察。
- GREEN: 修订后的静态合同命令 -> PASS；已存在 cleanup/incomplete/read-only/forbidden-write/string-ID 标识，且 `page.route(`、`route.abort(`、`refreshFailureInjected` 均不存在。
- GREEN: `pnpm exec prettier --check tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` -> PASS。
- GREEN: `pnpm exec playwright test tests/e2e/sp0-sp4-production-release-real-flow.spec.ts --list` -> PASS；识别 1 个 Chromium 用例，仅证明规格可解析，不是 TC-13 业务 E2E PASS。
- GREEN: `pnpm test sp1-production-release-contract`、`sp2-pqc-production-release-contract`、`sp3-production-release-report-upload-contract`、`sp4-manager-release-trace-contract` -> PASS。
- GREEN: `pnpm ts:check` -> PASS；`vue-tsc --noEmit -p tsconfig.relaxed.json` 退出码 0。
- GREEN: `git diff --check -- IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` -> PASS。
- GREEN: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；当前 `int_main` 端口合同为 frontend `8081`、backend `48081`。本轮没有据此启动、停止或使用运行态。
- preflight：只按变量名检查后，30 个 `EDHR_FULL_E2E_*` 前置变量 present 0、missing 30；新增的缺口是未完成活跃订单 ID/工单号和 cleanup plan reference。检查未读取、回显或记录任何密码、token 或其它秘密值。
- runtime observation：`8081` 和 `48081` 已有监听进程，但未验证其 Jar/Vite 与 `ecb05caa6` 的一致性；因全部正式环境输入仍缺失，本轮未使用该运行态，也未运行写入型 Playwright。
- risk scan：`fallback|mock|default-success|TODO|FIXME|password|secret|token` 命中仅为环境密码字段与密码输入选择器、正式 `TODO` 任务状态和候选待办路径；无 `fallback`、mock、默认成功、明文秘密或 token 值。
- AC audit：页面规格现可在正式运行态覆盖 AC-01 的前端门禁、AC-03/07/20/21 的已配置角色和交叉反向权限、AC-04/08/09、AC-14/16/17/18、AC-22、AC-24/25/26、AC-28 的字符串 ID 合同和 AC-32 主链。AC-27 的真实刷新/响应故障不计入该真实用例；仍由既有静态/单元合同验证。
- remaining acceptance blockers：AC-01/02 的直接后端拒绝、AC-03/07/20/21 的移除角色和跨租户反向验证、AC-05/10/19/24/34 幂等并发与回滚、AC-06 回执权限、AC-11 旧数据、AC-12/13/33 三类正式来源和零损耗、AC-15 服务端不支持动作、AC-23 快照篡改、AC-27 提交后响应不确定、AC-28 超安全整数 fixture、AC-29 全部结构化失败、AC-30 跨租户、AC-31 完整审计时间线及 TC-13 的正式真实页面清理结果均需要受控真实环境、专用 fixture 或后端测试证据；当前不得以静态合同、`--list`、监听端口或只读 API 代替。
- milestone result：T11 自动化规格和无运行态验证已更新；TC-13、TC-14 全量回归和 AC-01 至 AC-34 的独立验收继续为 `BLOCKED`。解除条件是提供 30 项正式前置、确认当前代码的成对运行态、按真实页面执行多账号主链和反向路径、完成同一正式页面清理闭环并由独立 tester 复验。

## Pass 33 - T11 Manual Acceptance Handoff And Independent Reverification

- manual handoff：`test-plan.md` 已补入“用户手动验收执行单”；明确三组订单、七账号加非归属组长和第二租户、四附件、签核、清理计划、页面执行顺序和 AC-01 至 AC-34 最低证据映射。该执行单不替代 TC-13 Playwright 或独立 tester 门禁。
- independent tester：不同于 T11 executor 的 tester 在 `int_main` 独立复核更新后的规格；Playwright `--list`、SP-1 至 SP-4 named contracts、`pnpm ts:check`、Prettier、差异检查、风险词和页面写入边界静态审计均 PASS。
- independent boundary：规格没有 `page.request.post/put/patch/delete`；唯一直接请求为页面成功后最终状态只读 `GET` 核验。已确认没有 `page.route`、`route.abort` 或其它故障注入混入 TC-13 真实页面用例。
- preflight：30 项 `EDHR_FULL_E2E_*` 变量 present 0、missing 30；当前 8081/48081 虽有监听，但运行产物尚未核验为 `ecb05caa6`，没有使用。未启动、停止或重启服务，未登录、上传或写业务数据。
- independent verdict：P11/T11 继续 `BLOCKED`。真实七账号/非归属/跨租户页面链、三组 fixture、四附件、最终签核、双条件追溯、只读核验和同一页面清理仍无执行证据。

## Pass 34 - T11 Spec Commit On Latest Int Main

- concurrent baseline：提交前 `int_main` 已由其它任务推进到 `fe117216c`；`ecb05caa6..fe117216c` 与本任务规格和任务目录零路径重叠，未回退、暂存或修改其它任务文件。
- GREEN: 在 `fe117216c` 当前工作树重新执行 T11 spec Prettier、Playwright `--list`、SP-1 至 SP-4 named contracts 和 `pnpm ts:check` -> 全部 PASS；`--list` 仍不代表真实 E2E。
- GREEN: 本任务 15 个文档文件和规格文件 whitespace check -> PASS；风险词复核命中仅为正式环境密码字段、TODO 状态和禁止 fallback/mock/default-success 的需求或证据文本，秘密模式扫描无命中。
- staged boundary：暂存区精确为 `IntRuoyiFronted/tests/e2e/sp0-sp4-production-release-real-flow.spec.ts` 一个文件；`git diff --cached --check` 和 branch runtime guard 均 PASS。
- git commit：`8ca580be3`，message `test: strengthen production release acceptance gate`；230 insertions，27 deletions。未 push，未使用 8081/48081 运行态，未登录或写业务数据。
