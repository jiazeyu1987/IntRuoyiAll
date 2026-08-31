# Execution Log

## User Intent

- 在 worktree 中完成文档要求的开发和验证，验证成功后融合进 `int_main`。
- 最新 MVP 口径：五个状态页签；待放行行操作为 `放行`、`不合格审查`；不再提供单独驳回操作；确认放行后完成电子签名提交、状态刷新和批记录入口。

## BDD

- BDD: PQC 查看五类生产放行记录 -> Given PQC 负责人进入生产放行页面; When 切换任一状态页签; Then 系统按权威状态返回该页签记录并显示对应数量和业务状态。
- BDD: PQC 签字放行 -> Given 待放行申请属于当前 PQC 候选且批次未冻结; When 输入当前账号电子签名密码并确认放行; Then 后端校验签名、完成正式放行事务、页面将记录移出待放行并提供查看批记录入口。
- BDD: 签名失败不放行 -> Given 待放行申请存在; When 电子签名密码为空或错误; Then 系统明确拒绝且申请保持待放行。
- BDD: 发起不合格审查 -> Given 待放行申请尚未创建批次执行但包含正式工单身份; When 点击不合格审查; Then 进入统一不合格评审入口并冻结工单，后续由 QA 处置为让步放行、返工或作废。
- BDD: 待放行页面不提供拒绝 -> Given PQC 负责人查看待放行记录; When 查看行操作; Then 只显示放行和不合格审查，不显示旧拒绝动作。

## Evidence

- M0：当前生产放行审批位于通用工作待办页面，动作仍为“通过/拒绝”；放行请求没有签名字段；现有放行追溯页面不是 PQC 专用五状态列表。
- M0：现有统一不合格评审已覆盖 PQC 放行来源、QA 让步放行/返工/作废和追溯，可直接复用。
- M0：现有批次详情已有电子签名密码放行模式，可复用正式签名校验服务，不新增第二套签名体系。
- RED: `node src\test\js\mes-pqc-production-release-mvp-completion-static.spec.cjs` -> FAIL，预期原因：后端尚无 PQC 专用分页接口、签名字段和申请级不合格评审支持。
- RED: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> FAIL，预期原因：前端尚无五状态常量、专用页面和签名放行合同。

## Current Milestone

M5 - ready for branch commit and int_main integration.

## GREEN And Regression

- GREEN: `node src\test\js\mes-pqc-production-release-mvp-completion-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProEdhrNonconformanceReviewApplicationScopeTest -DforkCount=0 test` -> PASS，13 tests。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，30 reactor modules。
- GREEN: release migration policy gate -> PASS，包含两条 20260831 迁移。
- GREEN: 既有不合格评审前后端静态合同、SP-2 生产放行合同、工作待办静态合同 -> PASS。
- GREEN: Playwright 真实页面 -> PASS；从真实动态菜单进入 `/mes/production-release/pqc`，五个页签全部可见，分页接口业务码为 0，页面无 pageerror，业务写请求 0。

## Runtime Findings

- 首次本地迁移执行命令因 `sh -lc` 参数被 PowerShell 拆分，命令返回 0 但 schema 未变化；改为将完整 shell 命令作为单一参数传入，并回读 `batch_execution_id` 已可空、生产放行菜单已成为正式页面。
- 首次真实列表请求因历史申请全部缺 PQC 待办 ID，调用 `selectByIds(empty)` 生成 `IN ()`；补测试和空集合门禁后，真实页面返回空列表而非系统异常。
- 真实页面唯一失败资源为外部头像地址连接拒绝，属于非目标链路，不影响菜单、目标接口或页面渲染。

## Not Executed

- 当前 `芋道源码` 租户待放行行数为 0，因此未执行电子签名放行和不合格评审写入。未通过 SQL、API-only 或 mock 造业务样本；对应写入行为由后端事务测试和前端合同覆盖。
- 两个既有非目标静态测试在当前基线失败：工作任务上下文测试要求另一 API 的 `workTaskId`，批次详情放行测试要求旧“质量拒收”按钮；本任务未修改对应业务链路。

## Closeout Preview

- `task-closeout-cleanup --mode preview` 正确保留三份任务记录并建议删除 migration policy JSON；生成 JSON 已删除。
- 自动 apply 被阻塞：主工作区存在并行未提交改动，且 cleanup 脚本无法从任务文档自动识别全部生产代码归属。按用户明确要求改为精确提交当前分支、核对与主工作区脏文件无交集后执行 `ff-only` 融合；不覆盖或提交并行改动。

## Integration And Cleanup

- 初始功能提交：`ff417d3ef`。
- 开发期间 `int_main` 前进 3 个并行提交；已核对主线已提交文件、主工作区未提交文件与本任务 34 个文件交集均为 0。
- 将功能提交重放到最新 `int_main` 后得到 `d9fe88557`，branch runtime guard PASS。
- `int_main` 通过 `git merge --ff-only codex/20260831-pqc-production-release-mvp-completion` 快进到 `d9fe88557`；主工作区原有并行改动保持未暂存。
- 额外 worktree 已从 Git 登记移除；pnpm 残留目录清除只针对本任务 worktree，最终路径不存在；8311/48311 已停止监听。
- `int_main slot 56` 登记已在 worktree 路径删除后原子更新为 `active=false`，登记表无临时文件残留。

## M6 Completion Audit Continuation

- 完成审计结论：上一轮真实页面只证明菜单、五页签和空列表，未证明电子签名放行、不合格审查写入和状态迁移，因此重新标记 `in_progress`。
- 只读凭据核对：`芋道源码/admin` 已正式属于 `MES_PQC_RELEASE_OWNER`，本机默认测试凭据与该账号匹配；无需修改角色或密码。
- 计划路径：真实登录 -> 生产组长活跃订单 Stage1 模拟 -> 完成/申请放行 -> PQC生产放行签名或不合格审查 -> 状态核对 -> 通过正式清理入口删除任务自有模拟数据。
- RED: `node tests\e2e\pqc-production-release-write-flow-real.e2e.js` -> FAIL；真实页面已创建 Stage1 任务自有活跃订单并显示生产/检验进度均为 100%，直接申请放行被正式后端以“formal production progress is below 100%”拒绝，确认 Stage1 完成记录仍为未回填状态。
- RED: 补齐页面“模拟完工”步骤后复跑 -> FAIL，业务码 `1040750243`；正式回填发现当前路线批记录/过程检验模板存在未确认填写规则，坐标覆盖第 4 至 52 行的目标可填单元格。系统在创建批次或放行申请前正确 fail-fast。
- Supporting read-only audit: 当前本地路线绑定的多数生产记录模板仍为 `source=AUTO, reviewed=false`；没有 `CELL_RULE_RECONCILED` 确认证据。只读核对未修改模板、角色、密码或业务状态。
- Cleanup GREEN: 通过真实生产组长页面调用 `/active-order/remove`，业务码 `0`；任务订单 `347` 回读为 `REMOVED/REMOVED`，无本轮活动模拟订单残留。
- Blocker: 模板填写规则必须由模板管理员基于业务含义逐张确认。禁止自动把建议态规则标成已确认，禁止直接改 Jimu JSON、跳过 `validateConfirmedCellRules`、使用 SQL/API-only 造待放行申请或把当前整体 E2E 记为 PASS。

## M6 Formal Completion And Release Continuation

- BDD: 完工并申请放行原子编排 -> Given 活跃订单生产和检验进度均为 100% 且尚未形成完工回执; When 生产组长点击“完工”并确认申请放行; Then 系统必须先执行正式资料回填和完工回执，再创建 PQC 放行申请；任一回填失败时不得创建申请。
- BDD: 已有完工回执安全复用 -> Given 活跃订单已有成功完工回执; When 重试生产放行申请; Then 系统必须使用回执原始版本和幂等键重新校验当前正式来源，不得重复回填或跳过来源校验。
- BDD: 批记录绑定正式身份解析 -> Given 路线逐工序绑定具有正式 `batchRecordReportId`，但冗余定义/版本字段为空; When PQC 放行规划批记录映射; Then writer 必须从正式报表元数据解析定义和版本；冗余值与元数据冲突时仍应阻断。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseApplicationServiceImplTest,MesTeamLeaderActiveOrderCompletionServiceTest" -DforkCount=0 test` -> FAIL，缺少 `completeForRelease` 和完工/申请编排。
- GREEN: 同命令 -> PASS，13 tests；申请服务在同一事务内先完成回填，再生成 PQC 申请。
- RED: `node tests\e2e\team-leader-active-order-release-application-static.spec.js` -> FAIL，前端未区分明确业务错误与网络不确定响应，确认文案也未说明先完工回填。
- GREEN: 同命令 -> PASS；明确后端业务错误直接展示并允许修复后重试，只有无业务码的响应才进入不确定回执核对。
- RED: writer 目标测试 -> FAIL，构造器缺少正式报表 mapper，冗余定义/版本为空的绑定无法解析。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderActiveOrderReleaseBatchRecordWriterTest,MesTeamLeaderActiveOrderReleaseBatchRecordWriterImplTest" -DforkCount=0 test` -> PASS，9 tests。
- GREEN: `MesProductionReleaseControllerJsonTest` -> PASS，3 tests；生产放行 blocker 由控制器返回结构化业务响应，不再落入全局 `500 系统异常`。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，30 reactor modules。
- GREEN: 前端 `pnpm ts:check`、PQC MVP 静态合同、生产组长放行申请静态合同 -> PASS。
- Real write: Stage1 生成活跃订单 `348`；`/active-order/release/apply` 业务码 `0`，创建申请 `9`、PQC 待办 `2391`；不合格审查创建 `7` 并由 QA 让步处置，两个写接口业务码均为 `0`。
- Real gate: `/production-release/pqc/approve` 返回结构化 blocker `formal batch-record plan is missing`；申请仍为 `PQC_RELEASE_PENDING/version=1`、待办仍为 `TODO`、`PQC_RELEASE:9` 批次数量为 `0`。
- Formal mapping audit: 粗洗、精洗、清洗、清洁、组装 I、光固 I、硅化 I、硅化 II、组装 II、检测、光固 II、单包装、中包装、大包装共 14 张逐工序批记录，放行可接受映射数全部为 `0`；粗洗仅有 `PRODUCTION_WORK_ORDER` 映射，其余 13 张没有启用映射。
- Remaining blocker: 映射需要模板/工艺负责人确认每个目标单元格的业务来源。当前申请和评审属于任务自有可追踪证据，但系统没有对已让步且待签名申请的正式取消/清理入口，未擅自删除审计记录。
- Final focused regression: 相关后端 7 个测试类合计 38 tests 全部 PASS；前端生产组长放行申请合同、PQC MVP 合同、真实 E2E 语法检查和 `pnpm ts:check` 全部 PASS；`git diff --check` PASS。

## User-Authorized Mapping Deferral

- User decision: 当前正式字段映射暂时无法完成；明确授权本次跳过映射部分，若逻辑卡在映射门禁，则以静态代码逻辑检查通过作为验收通过。
- Scope: 仅调整本次验收证据口径，不修改生产门禁、不写入猜测映射、不把 blocker 改成默认成功，也不伪造批次或签名。
- Static logic review: PASS。生产组长申请在同一事务内先完成正式回填再创建 PQC 申请；已有完工回执按原版本/幂等身份重新校验；PQC 让步评审关闭后仍需再次签名；批准事务先验证角色、候选、冻结状态、评审结果和签名密码，再规划逐工序批记录/过程检验/损耗资料；映射 writer 只接受唯一正式逐工序绑定和已支持来源，校验生产提交、分配、复核签名与来源值后写入当前批次任务和字段审计；任一 blocker 会在批次、签名和状态推进前回滚。
- Static review result: 未发现可导致绕过签名、跨工序取错批记录、重复创建申请、业务错误误锁定前端或 blocker 被吞掉的逻辑缺口。
- Deferred risk: 14 张正式批记录完成映射后的真实批次创建、资料落库、PQC 电子签名和任务清理尚未动态验证；映射可用后必须恢复该 E2E。
- Local evidence retained: 申请 `9`、待办 `2391`、评审 `7` 和活跃订单 `348` 为本机任务自有审计证据；因当前没有正式取消已让步待签名申请的入口，未通过 SQL 或 API-only 删除。
- Final gate: 后端 7 个相关测试类共 38 tests PASS；前端两项静态合同、真实 E2E 语法检查、`pnpm ts:check`、branch runtime port guard 和 `git diff --check` PASS。
- Closeout preview: 默认保留三份任务记录、无建议删除文件；自动 apply 因无法从任务目录推断 17 个正式源码/测试归属、主工作区有无关脏改动、分支需重放到新主线而阻塞。按用户明确的提交/融合要求采用精确文件清单，不使用宽泛暂存或覆盖主工作区改动。
- Integration preflight: merge-base 为 `9b9b16274b4920005eaa9de421deae245c3c29e9`；本任务 20 个文件与 `int_main` 后续 23 个已提交文件交集为 0，与主工作区 36 个脏文件交集为 0。
- Implementation commit: 初始提交 `52b2f0cf6`；重放到最新 `int_main` 后为 `f23ed1252`。
- Post-rebase verification: 38 个相关后端测试、前端两项静态合同、真实 E2E 语法、`pnpm ts:check`、branch runtime port guard、`git diff --check int_main..HEAD` 和 30 模块 `yudao-server` package 全部 PASS。
- Integration: `int_main` 从 `85d0d91d0` 通过 `git merge --ff-only codex/20260831-pqc-production-release-write-e2e` 快进到 `f23ed1252`；主工作区原有并行脏文件保持未暂存且未覆盖。
- Worktree cleanup: Git worktree 登记已移除；残留目录仅包含本任务前端依赖/构建产物，精确删除后 `Test-Path=False`；8311/48311 无监听。
- Slot cleanup: `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 中本任务 `int_main slot 56` 已原子更新为 `active=false`，临时登记文件不存在。
- Final status: completed。
