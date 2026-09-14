# Execution Log

## User Intent

- 用户反馈在“新增活跃订单”搜索栏输入 `88` 时，没有显示此前确认可加入的四个生产工单。
- 目标是修复真实搜索行为，而不是要求用户输入完整工单号规避问题。

## BDD

- BDD: 宽关键词优先保留符合资格的活跃订单候选 -> Given 多个生产工单均匹配关键词 `88`，其中包含已满足正式路线、DCC 项目和当前 ACTIVE 路线版本 QA 的目标工单，也包含不符合资格的其它匹配项；When 生产组长在新增活跃订单弹窗输入 `88`；Then 目标四个符合资格工单必须出现在候选列表并显示“符合要求”，不得在资格判定前被候选上限截断。

## Milestone Evidence

- M1：Playwright 真实页面打开“生产组长工作台 -> 新增活跃订单”，输入 `88` 后下拉仅返回 20 条已取消工单；四个目标工单均未出现。
- M1：租户 `1` 只读数据库核验显示 `88` 共匹配 1053 个生产工单，其中状态 `1`（已确认）810 个、状态 `2` 72 个、状态 `3`（已取消）171 个；仅把“已确认”提前排序仍无法保证四个目标工单进入前 20。
- M1：根因定位为 `MesProWorkOrderMapper.selectCandidatesByKeyword` 在资格解析前按工单 ID 倒序执行 `LIMIT 20`；`MesTeamLeaderActiveOrderServiceImpl` 只能对这 20 条执行正式路线/DCC/QA 资格判断和排序。

## RED / GREEN / REGRESSION

- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProWorkOrderMapperTest#testSelectCandidatesByKeyword_doesNotTruncateBeforeEligibilityEvaluation,MesTeamLeaderActiveOrderServiceTest#shouldApplyCandidateLimitAfterEligibilityEvaluationForBroadKeyword" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，mapper 预期 24 条实际 20 条；service 预期资格排序后 20 条实际 24 条，准确证明上限位于错误层级且 service 缺少最终上限。
- GREEN: 同一命令重跑 -> PASS，2 tests，0 failures，0 errors。
- REGRESSION: `mvn.cmd -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，46 tests，0 failures，0 errors。
- PACKAGE: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，30/30 reactor modules `SUCCESS`，生成 `yudao-server-exec.jar`；SHA-256 `E0B1F5F68D3289786B1E14295839337C1D6A3DBB0630736FC6DF5A56B7A7B4BE`。
- PACKAGE INSPECTION: 最终 fat JAR 内 `yudao-module-mes-2026.04-SNAPSHOT.jar` 的 `MesProWorkOrderMapper.selectCandidatesByKeyword` 为两参数签名且无 mapper `LIMIT`；`MesTeamLeaderActiveOrderServiceImpl` 在资格映射和排序后执行 `Stream.limit(20)`。
- RUNTIME: 新包曾在本机 `48081` 启动且 `/actuator/health` 返回 `UP`；验证结束后因数据库迁移前置缺失，已恢复验证前 `v3` 包，恢复后 health `UP`。
- PLAYWRIGHT: 真实登录 `http://127.0.0.1:8081/mes/pro/process-pool/team-leader`，打开“新增活跃订单”并输入 `88` -> BLOCKED，候选接口返回“系统异常”；未点击“加入活跃订单”，未写入生产工单。

## Blockers

- 2026-08-09：发现 3 个非本任务 Maven 进程同时在 `E:\IntRuoyi\IntRuoyiBackend` 执行 MES reactor 编译/测试，其中命令分别涉及 `MesFrontlinePqcContextServiceTest`、`-DskipTests compile`、`MesTeamLeaderActiveOrderManualSortTest,MesTeamLeaderActiveOrderServiceTest`。这些进程共同写入 Maven `target`，并且最后一个任务与本任务修改的测试类重叠。
- 已终止本任务自己的 GREEN Maven 进程，未终止或修改其它任务进程。按照共享环境冲突规则，需等外部进程结束后重新独占执行 GREEN 和回归；当前运行态仍是修复前版本，不得声称页面已修复。
- 2026-08-09 16:32 后共享 Maven 进程已全部结束；上述阻塞解除，已重新独占执行 GREEN 和相邻回归并通过。
- 2026-08-09 17:32 真实页面阻塞：`/admin-api/mes/pro/process-pool/team-leader/active-order/candidates` 查询 QA 明细时抛出 `java.sql.SQLSyntaxErrorException: Unknown column 'inspection_tool' in 'field list'`。仓库已有正式迁移 `IntRuoyiBackend/sql/mysql/20260809_mes_qa_inspection_item_display_fields.sql`，但 `doc/tasks/20260809-frontline-qa-inspection-detail-fields/execution-log.md` 明确记录本机数据库尚未应用。应用该迁移会修改共享数据库，当前验证请求未授权此数据变更；禁止通过忽略列、旧字段或旧运行包伪造验证成功。
- 用户随后明确授权应用该迁移。执行前发现并行任务 `20260809-frontline-qa-inspection-detail-fields` 已先行完成同一正式迁移：首次执行与幂等重跑均退出码 0，两个目标列均为 `varchar(512) NULL`，目标表仍为 166 行且两列非空计数均为 0。因此本任务不重复写库。
- 并发门禁：上述并行任务已启动 `backend-runtime-20260809-qa-inspection-detail-fields.jar` 占用 `48081` 并执行 Playwright；本任务不终止其进程、不抢占端口，等待其自然完成后再继续搜索页面验证。
- SCHEMA GREEN: 本任务通过本机 Docker MySQL 只读复核，两个目标列均为 `varchar(512)` 且 `IS_NULLABLE=YES`；表共 166 行，`inspection_tool` 与 `sampling_plan_text` 非空计数均为 0。授权的 schema 前置已经满足，无需重复执行迁移。
- RUNTIME CONFLICT: 等待期间并行 QA 任务持续保留 `qa-detail-20260809` Playwright 守护进程及 `48081` 专用后端；其 `task.md` 仍为 `verification_in_progress`。本任务对该 JAR 只读反编译，确认 `MesProWorkOrderMapper.selectCandidatesByKeyword(String, Collection, int)` 与 service 资格前传入 `20` 仍存在，因此当前运行态不能用于验证本次宽关键词修复。按共享端口冲突规则不得强停、替换或把旧实现结果作为通过证据。
- 用户明确要求继续。执行前重新核对：`48081` 已无监听，QA Playwright/Maven 活动进程均不存在；并行运行态已自然释放，本任务无需终止其它任务进程，可继续启动任务专用完整 JAR。

## Final Runtime And E2E Verification

- RUNTIME BYTECODE: 最终复验时共享运行包为 `output/runtime/int_main/backend-report-shared-allocation-20260809.jar`，SHA-256 前缀 `392D860CDE71F102A9F`。只读反编译其嵌套 MES JAR，确认 mapper 为 `selectCandidatesByKeyword(String, Collection)` 两参数签名且没有资格前 `LIMIT`；service 在资格映射、排序后执行 `Stream.limit(20)`。
- RUNTIME HEALTH: `8081` 与 `48081` 均有监听，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。未停止、替换或重启其它任务的共享运行进程。
- PLAYWRIGHT GREEN: 真实登录 `http://127.0.0.1:8081/mes/pro/process-pool/team-leader`，点击“新增活跃订单”，输入 `88`；候选列表显示 `881MO090935`、`881MO090972`、`881MO090973`、`881MO090974`，四条均标记“符合要求”。`881MO090889` 同样显示“符合要求”。
- CANCELED CONTRACT GREEN: 在同一弹窗精确输入 `881MO100646`，唯一候选明确显示“生产工单已取消”，未被当作可加入订单。
- REQUEST EVIDENCE: 后端访问日志记录 `keyword=88` 请求耗时 `130 ms`，`keyword=881MO100646` 请求耗时 `88 ms`，均正常完成。
- BROWSER EVIDENCE: Playwright 控制台 `Errors: 0, Warnings: 0`；截图 `playwright-active-order-88.png`。此前会话中的一次 30 秒 Axios 超时发生在共享运行态切换期间，最终稳定运行态的独立干净会话未复现。
- DATA SAFETY: 只操作搜索框和弹窗；未选择候选、未点击“加入活跃订单”，未新增、修改或删除生产业务数据。

## Experience And Closeout

- EXPERIENCE: 将“宽关键词候选上限必须在正式路线、ACTIVE 版本、DCC 和 QA 资格解析及排序之后应用”的长期门禁合并到 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`，并更新 `docs/experience-index.md` 关键词索引；未新建长期经验文档。
- CLEANUP PREVIEW: `task-closeout-cleanup` 预览结果为 `blocked=[]`、`warnings=[]`，保留 `task.md`、`execution-log.md`、`verification-report.md`、回归证据和最终截图；删除范围仅为本任务隔离编译/反编译目录、旧专用 JAR、专用日志和 Playwright 临时文件。
- CLEANUP APPLY: 首次 apply 在删除 `backend-active-order-88-search-retry.stderr.log` 时因文件占用 fail-fast。只读定位到本任务旧 JAR 遗留 Java 进程，确认其未监听 `48081` 且当前共享运行态由其它 JAR 提供后，仅停止该本任务进程；共享后端 health 继续为 `UP`。
- CLEANUP GREEN: 原清单重跑后状态 `applied`，全部本任务临时目录、旧专用 JAR、专用日志和列明的 Playwright 临时文件已删除；两条 `continued` 日志因首次 apply 已删除而在重跑中仅报告“不存在”警告。正式源码、回归测试、SQL 迁移、任务文档和最终截图均保留。
- GIT: 用户未要求 Git 操作；未 stage、commit、merge 或 push。
