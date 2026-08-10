# Execution Log

## User Intent

- 给活跃工单的操作列表增加手动上移、下移按钮，使活跃订单可以手动排序。

## BDD Scenarios

- BDD: 生产组长上移活跃订单 -> Given 当前生产组长有至少两条活跃订单且目标订单不是首行 When 用户点击目标行“上移” Then 系统只交换目标订单与相邻上一条订单的顺序并刷新列表，刷新后顺序保持。
- BDD: 生产组长下移活跃订单 -> Given 当前生产组长有至少两条活跃订单且目标订单不是末行 When 用户点击目标行“下移” Then 系统只交换目标订单与相邻下一条订单的顺序并刷新列表，刷新后顺序保持。
- BDD: 边界按钮不可执行 -> Given 订单位于当前列表首行或末行 When 用户查看操作列 Then 首行“上移”或末行“下移”按钮禁用且不发送写请求。
- BDD: 越权或失效订单移动失败 -> Given 请求订单不属于当前登录生产组长或已不再活跃 When 调用移动接口 Then 服务明确失败且不修改任何顺序。
- BDD: 缺少相邻订单移动失败 -> Given 目标订单已处于首行或末行 When 服务收到对应方向的移动请求 Then 服务明确失败且不修改任何顺序。

## Command Intent

- 只读检查活跃订单页面、API、Controller、Service、Mapper、DO、既有测试与 SQL 迁移，确认当前按 `joinedAt, id` 排序且没有持久化人工顺序。
- 读取 `docs/frontend-development.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/task-closeout-rules.md` 和技能证据契约，冻结无 fallback、BDD/TDD 与收尾要求。

## Milestone Updates

- M1 completed：页面入口为 `TeamLeaderWorkbenchPage.vue` 的“活跃订单池”，列表数据来自 `/mes/pro/process-pool/team-leader/active-order/list`；当前 Mapper 按加入时间和 ID 升序。
- M2 completed：已新增任务专用前端静态合同、后端服务测试与迁移合同测试，并取得预期 RED。
- M3 completed：新增 `sort_order` 正式字段及迁移、组长范围内相邻排序值原子交换接口、前端上移/下移图标按钮、边界禁用与刷新分层提示。
- M4 completed：任务专用前端静态合同、SQL 合同、MES 活跃订单定向测试、TypeScript 检查、迁移策略门禁、三份技能证据校验和 `git diff --check` 均通过。
- M5 completed：`task-closeout-cleanup` preview 无 blocked/warnings，apply 仅删除三份临时技能证据和迁移门禁 JSON；核心任务文档、实现、迁移与正式测试均保留。可复用的相邻持久化排序规则已合并到 `docs/backend-development.md` 并加入 `docs/experience-index.md`。

## Verification Evidence

- RED: `node tests/e2e/team-leader-active-order-manual-sort-static.spec.cjs` -> FAIL，前端缺少 `TeamLeaderActiveOrderMoveReqVO` 和移动接口。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_manual_sort_sql.py` -> FAIL，正式排序迁移文件不存在。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期缺少 `MesTeamLeaderActiveOrderMoveReqBO`。
- 说明：首次 Maven 命令未给包含点号的 `-D` 参数加引号，被 PowerShell 解析为无效 lifecycle phase；已按项目 PowerShell 规则更正并取得有效 RED，该无效命令不作为 TDD 证据。
- GREEN: `node tests/e2e/team-leader-active-order-manual-sort-static.spec.cjs` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_active_order_manual_sort_sql.py -q` -> PASS，3 项通过。
- GREEN: `mvn -Pmes-ac-m04-active-order-targeted-tests "-Dtest=MesTeamLeaderActiveOrderManualSortTest,MesTeamLeaderActiveOrderServiceTest" test`（工作目录 `IntRuoyiBackend/yudao-module-mes`）-> BUILD SUCCESS，31 项通过。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: release migration policy gate -> PASS，`migrationCount=456`，包含 `20260809_mes_process_pool_active_order_manual_sort`。
- GREEN: frontend/backend/database 技能证据校验 -> PASS。
- GREEN: `git diff --check` -> PASS；仅输出工作区既有 LF/CRLF 转换警告，无空白错误。
- GREEN: `task_closeout.py --mode preview` -> PASS，删除范围仅限 4 个任务专用临时证据文件；`--mode apply` -> PASS。
- GREEN: 经验沉淀结构检查 -> PASS，`docs/backend-development.md#持久化列表相邻手动排序门禁` 与 `docs/experience-index.md` 索引均存在。
- E2E BLOCKED: 未执行真实写入 Playwright；缺少已确认可写的测试租户账号和两条任务自有活跃订单，按规则禁止借用现有业务记录或使用 API/SQL 代替页面路径。

## Blockers

- 本功能定向验证无 blocker。
- 非任务基线：无定向 profile 的 MES 全量测试编译被并发任务文件 `MesFrontlinePqcContextServiceTest` 构造参数不匹配阻断。
- 非任务基线：后续 `-am` 重跑被并发 DCC 模块大量缺失 class 的测试编译错误阻断；改用 MES 模块已有活跃订单定向 profile 后 31 项通过。
- 非任务基线：`mes-process-pool-team-leader-static.spec.js` 因并发任务在页面中新增 `ignoreErrorMessage: true` 而失败；本任务两个相邻静态合同与专用合同均通过。

## Independent Re-verification 2026-08-09

- 用户意图：对已实现的活跃订单手动排序执行独立复验，不修改既有业务数据。
- GREEN: `node tests/e2e/team-leader-active-order-manual-sort-static.spec.cjs`、`production-leader-active-order-pool-tab-static.spec.js`、`team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_active_order_manual_sort_sql.py -q` -> PASS，3 项通过。
- GREEN: `mvn -Pmes-ac-m04-active-order-targeted-tests "-Dtest=MesTeamLeaderActiveOrderManualSortTest,MesTeamLeaderActiveOrderServiceTest" test` -> PASS，31 项通过，`BUILD SUCCESS`。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS，457 个迁移通过，包含 `20260809_mes_process_pool_active_order_manual_sort`。
- GREEN: 本机 `8081` HTTP 200，`48081` health `UP`；端口进程均归属 `E:\IntRuoyi` 的 `int_main` 运行态。
- GREEN: 运行库 `mes_pro_process_pool_active_order.sort_order` 为 `bigint NOT NULL`，`idx_mes_pp_active_order_manual_sort` 六列索引存在，14 条未删除记录均无空排序值。
- GREEN: Playwright 以本机默认身份只读进入“生产组长 -> 活跃订单池”，页面有 7 行、7 个上移按钮、7 个下移按钮，首行仅上移禁用、末行仅下移禁用；浏览器 console error 为 0，未发起 MES 写请求。
- E2E BLOCKED: `48081` 当前运行 Jar `backend-runtime-control-20260809-batch-codex-runner-fix-v3.jar` 的内嵌 MES 模块不包含 `MesTeamLeaderActiveOrderMoveReqVO`，`javap` 证明 Controller 不含 `moveActiveOrder` 方法，无法验证移动接口、相邻交换及刷新持久化。
- E2E BLOCKED: 页面 7 条活跃订单属于既有业务记录，不是本任务创建、可追踪、可清理的数据；按 E2E 数据门禁未点击移动按钮。
- Overall: 源码、定向自动化、迁移策略、运行库 schema 与页面只读展示 PASS；真实写入用户路径 BLOCKED，任务状态回退为 `blocked`。
- Experience consolidation: 本轮暴露的“运行 Jar 必须包含目标 Controller/VO，页面只读展示不能替代写接口验收”已由 `docs/local-runtime.md` 的运行 Jar 加载门禁和 `docs/e2e-rules.md` 的静态合同/真实 E2E 分层门禁覆盖，不重复修改长期经验文档。
- Cleanup: 已关闭 Playwright 会话，并删除本轮 7 个浏览器临时产物及运行 Jar 解包临时目录；保留 `.playwright-cli` 中其它任务既有产物。

## Runtime Route Fix 2026-08-09

- 用户授权：更新并重启本机 `48081` 后端，修复 `/active-order/move` 运行态缺失并继续验证。
- RED: `verify-runtime-active-order-move.ps1` 检查旧运行 Jar -> FAIL，缺少 `MesTeamLeaderActiveOrderMoveReqVO.class`；旧 Controller 也已由前次 `javap` 证明没有 `moveActiveOrder` 方法。
- 门禁脚本首次因 PowerShell 多行条件解析失败，修正后才取得有效 RED；首次解析失败不计入业务证据。
- GREEN: `mvn -Pmes-ac-m04-active-order-targeted-tests "-Dtest=MesTeamLeaderActiveOrderManualSortTest,MesTeamLeaderActiveOrderServiceTest" test` -> PASS，31 项通过，`BUILD SUCCESS`。
- 运行包构建门禁：以当时健康运行的 `backend-runtime-20260809-qa-inspection-detail-fields.jar` 为底生成任务运行包，保持内嵌 MES Jar 为 STORE，并通过移动 Controller/VO/Service/Mapper/DO 结构门禁。
- 启动失败证据：任务运行包启动时 Spring 反射 Controller 触发 `NoClassDefFoundError: MesReportAllocationSnapshot`。根因是脏主工作区的同一 Controller 同时含另一并发任务的新报工分配类型，定向替换 Controller 未形成完整依赖闭包；该失败运行包未作为成功版本。
- 受控恢复：停止失败进程后，`int_main` 完整运行包 `backend-runtime-control-20260809-202548.jar` 接管 `48081`；SHA256 为 `87DBA62E274F7601F56E7A37B3AC57E0A56B75910FBF51C37B1C550E6A1E946B`，health `UP`，运行 Jar 移动路由门禁 PASS。
- GREEN: `verify-runtime-active-order-move.ps1 -RuntimeJar backend-runtime-control-20260809-202548.jar` -> PASS，移动 VO、Controller 方法、`/active-order/move`、`PutMapping`、Service 和 Mapper 契约均存在。
- Playwright 测试数据：通过真实页面把候选工单 `881MO090935` 加入活跃订单，生成任务专用活跃订单 ID `50`；加入前确认该工单不在 7 条活跃订单中。
- GREEN: 真实页面点击任务订单“上移” -> `PUT /active-order/move` HTTP 200、业务码 `0`，顺序从 `[35,36,37,38,39,48,49,50]` 变为 `[35,36,37,38,39,48,50,49]`。
- GREEN: 真实页面点击任务订单“下移” -> `PUT /active-order/move` HTTP 200、业务码 `0`，顺序恢复为 `[35,36,37,38,39,48,49,50]`；页面刷新后顺序仍保持。
- GREEN: 边界按钮 -> 8 条数据时首行上移禁用、末行下移禁用；上下移动按钮各 8 个。
- Cleanup data: 通过真实页面移除任务专用活跃订单 `50`；最终 UI 和登录态 API 都恢复为 `[35,36,37,38,39,48,49]`，任务行不存在。
- GREEN: Playwright 最终 console -> 0 errors、0 warnings；`8081` 页面与 `48081` health 均可用。
- Experience consolidation: `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁` 已明确覆盖“脏主工作区混有其它任务改动时阻塞”“相关 class 形成依赖闭包”“启动前校验内嵌 Jar”；本轮不重复修改长期经验文档。
- Remaining blocker: 无。本任务不操作远端环境；无定向 profile 的既有全量编译问题不影响本任务 31 项定向回归和真实运行态验收。
- GREEN: `validate_bug_regression.py --evidence bug-regression-evidence.md` -> PASS，运行态缺路由回归证据结构有效。
- GREEN: `task_closeout.py --mode preview` -> PASS，删除范围仅含任务附属脚本/证据、失败运行包及其日志、当前 Playwright 临时产物；无 blocked/warnings。
- GREEN: `task_closeout.py --mode apply` -> PASS，14 个任务专用临时路径已删除，三份核心任务文档保留；主工作区无需 worktree 合并或删除。
- Post-closeout runtime recheck: 共享 `48081` 被完整运行包 `backend-report-shared-allocation-20260809.jar` 接管后，PID `51204`、health `UP`、SHA256 `392D860CDE71F102A9FFE1AE510F3A7295126440A421274AE47BC236DBA7BEB2`；只读解包确认内嵌 MES Jar 为 STORE，移动 Controller/VO/BO/Service/Impl/Mapper class 均存在，`javap` 再次确认 `moveActiveOrder`、`/active-order/move` 和 `PutMapping`，当前实际运行态未被并发更新回退。

## Production Order Number Display Follow-up 2026-08-09

- 用户意图：截图中的活跃订单列表当前显示 `workOrderId`（如 `980022`），该列应显示正式生产订单号 `workOrderCode`（如 `CODX-AO5-20260807-01`）。
- BDD: 活跃订单展示正式生产订单号 -> Given 活跃订单响应同时包含内部 `workOrderId` 和正式 `workOrderCode` When 用户进入“生产组长 -> 活跃订单池” Then 表头显示“生产订单号”，单元格只展示 `workOrderCode`，内部 ID 仍保留用于新增、报异常、移动和其它接口身份参数。
- Root cause: 活跃订单响应和前端类型已包含 `workOrderCode`，但表格列及 `activeOrderColumns` 错误绑定为 `workOrderId` 并标注“生产订单ID”；属于展示字段选择错误，无需修改后端契约。
- Command intent: 新增聚焦静态合同，先证明当前表格仍绑定 `workOrderId` 取得 RED，再只修改目标表格列和列元数据，复跑相邻活跃订单合同、类型检查及真实只读页面。
- RED: `node tests/e2e/team-leader-active-order-number-display-static.spec.cjs` -> FAIL，预期原因：活跃订单可见列仍标注“生产订单ID”并绑定 `workOrderId`。
- GREEN: `node tests/e2e/team-leader-active-order-number-display-static.spec.cjs` -> PASS，目标表格正向锁定“生产订单号”/`workOrderCode`，负向禁止可见列回退到内部 `workOrderId`。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`、`team-leader-active-order-manual-sort-static.spec.cjs`、`team-leader-workbench-static.spec.cjs`、`work-order-abnormal-minimal-report-static.spec.js` -> PASS，活跃订单页签、手动排序、工作台及异常上报相邻合同未回归。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: scoped `git diff --check` -> PASS；仅输出工作区既有 LF/CRLF 转换警告，无空白错误。
- GREEN: 本机 `8081` HTTP 200，`48081` health `UP`。
- GREEN: Playwright 登录真实页面并进入“生产组长 -> 活跃订单池”，表头为“生产订单号”，行内显示 `CODX-AO5-20260807-01`、`CODX-AO5-20260807-02`、`881MO090889`、`881MO090935` 等正式订单号；目标列未出现内部 ID `980022`、`923889`，console 为 0 errors、0 warnings。
- GREEN: `validate_bug_regression.py --evidence bug-regression-evidence.md` 与 `validate_frontend_feature.py --evidence frontend-feature-evidence.md` -> PASS。
- Experience consolidation: `docs/frontend-development.md#用户可见描述与内部编码隔离门禁` 已完整覆盖“可见字段直接使用正式描述/编码字段、ID 仅保留为提交身份、禁止 ID fallback”的本轮经验，`docs/experience-index.md` 已有索引；无需重复修改长期经验文档。
- M7 completed：只修正活跃订单可见列和列元数据，不改变后端契约、移动接口、排序或异常上报参数。任务状态进入 `ready_for_closeout`。
- GREEN: `task_closeout.py --mode preview` -> PASS，删除范围仅含 4 个本轮 Playwright 临时文件和 2 份临时技能证据，无 blocked/warnings。
- GREEN: `task_closeout.py --mode apply` -> PASS，上述 6 个任务附属路径已删除，生产实现、正式测试及三份核心任务文档均保留；当前为主工作区，无 worktree 合并或删除。
- GREEN: post-closeout `task_closeout.py --mode preview` -> PASS，delete/blocked/warnings 均为空。
- Closeout completed：任务状态更新为 `completed`。
