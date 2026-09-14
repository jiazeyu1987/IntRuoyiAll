# 一线 PQC 正式发布路线全部工序执行日志

## User Intent

- 用户明确：工序列表应展示正式发布工艺路线中的全部工序，不是只显示正在进行、已有 QA 规程或当前有待检任务的工序。
- 粗洗是否展示只取决于它是否属于该活跃订单冻结的正式发布路线，不取决于 QA 检验项目配置。

## BDD

- BDD: 正式发布路线全部工序可选 -> Given 活跃订单冻结了多个正式路线工序且只有部分工序有 `PENDING` PQC 任务，When 一线 PQC 加载订单工序列表，Then 所有冻结工序均返回。
- BDD: 待检任务仅决定任务上下文与可提交性 -> Given 一个正式路线工序没有 QA 规程或 `PENDING` 任务，When 用户查看并选中该工序，Then 工序可见但不得伪造 `pqcTaskId`、检验项或提交成功。
- BDD: 当前配置变化不污染订单快照 -> Given 当前路线配置存在未冻结到订单的草稿或新增工序，When 加载订单工序列表，Then 列表只展示订单正式发布路线快照中的工序。
- BDD: 非正式 owner 快速失败 -> Given 一个 `PENDING` 任务关联的规程 owner 不是 `MES_QA`，When 系统为工序附着任务上下文，Then 系统显式拒绝而不作兜底。

## Verification Evidence

- GREEN: experience-preflight -> PASS，历史任务证据确认工序候选应来自活跃订单的正式发布路线快照，工序候选与待检任务必须分离。
- ROOT_CAUSE: `listProcessesByActiveOrder` 使用 `selectPublishedListByProductRouteVersion(...)` 的 QA 规程集合生成候选，导致没有 QA 规程的正式路线工序被排除；活跃订单工序快照 mapper 已注入但未参与候选解析。
- RED: `mvn -pl yudao-module-mes -Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayAllPublishedRouteProcessesWhenOnlyOneHasPendingPqcTask+shouldDisplayRouteProcessesWithoutQaInspectionItemsAndAttachTaskOnlyToPendingProcess+shouldRejectNonFormalQaOwnerWhenAttachingPendingTaskContext test` -> FAIL，正式路线 `[4001,4002]` 分别被缩减为有 QA 规程的 `[4001]`/`[4002]`，任务规程 owner 场景返回错误码 `1040760116` 而非身份错误 `1040760103`。
- GREEN: `javac @target-pqc-route-snapshot/javac-service.args`、`javac @target-pqc-route-snapshot/javac-tests.args`、`java @target-pqc-route-snapshot/junit-console.args` -> PASS，`MesFrontlinePqcContextServiceTest` 与 `MesProcessPoolActiveOrderMapperTest` 共 38 项全部通过。
- REGRESSION: 工序候选改为 `activeOrderProcessSnapshotMapper.selectListByActiveOrderId(...)`；无任务工序返回空任务/检验上下文；有任务工序仍校验正式 `MES_QA/PUBLISHED` 规程；快照身份、重复工序和快照外待检任务继续快速失败。
- BLOCKER: 标准 Maven GREEN 命令在全量 `testCompile` 阶段被当前工作区其他任务新增但缺少实现的 19 个类型阻塞，涉及 `MesTeamLeaderActiveOrderRelease*` 测试；本任务源码主编译已成功，使用仓库既有隔离编译参数完成本任务 38 项回归，未修改这些无关测试或实现。
- RUNTIME: 从稳定运行 Jar `backend-runtime-control-20260809-003455.jar` 提取原 `yudao-module-mes`，只替换本任务已验证的 `MesFrontlinePqcContextServiceImpl*.class`，以 `jar uf0` 写入新运行 Jar；新 Jar SHA256=`11C1C03339D853C31C76DE7E881B9DA1B876C535F0AE385BAE2BD2B1FF59C27E`，内嵌 MES 模块 SHA256 与补丁模块一致，nested entry `Stored=True`。
- GREEN: 后端重启后 `48081` PID=`46904`，运行 Jar 为 `backend-runtime-control-20260809-pqc-all-published-processes.jar`，repo root 为 `E:\IntRuoyi\IntRuoyiBackend`，health=`UP`；前端 `8081` HTTP 200。
- GREEN: 官方登录前置 `芋道源码/admin -> /index` PASS；首次把“选工序”作为页面常驻文本等待超时，确认原因是页面自动选单后关闭 picker，未作为业务失败。
- GREEN: `frontline-pqc-all-published-processes.e2e.mjs` 真实 Playwright -> PASS。订单 `881MO090889`、产品“球囊扩张压力泵”显示 14 个正式路线工序：粗洗、精洗、清洗、清洁、组装Ⅰ、光固Ⅰ、硅化Ⅰ、硅化Ⅱ、组装Ⅱ、检测、光固Ⅱ、单包装、中包装、大包装；UI 数量和 `active-order/processes` 响应一致，业务码 0，`pqcSubmitRequests=[]`、`targetNetworkFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- NON_TARGET: 导航期间一个 `/mes/pro/work-order/page` 只读请求被浏览器取消（`net::ERR_ABORTED`），不属于当前一线 PQC 目标接口且未影响目标控件；页面自动调用一次非事务性 `pqc/switch-employee` 上下文解析 POST，未触发 PQC 提交。
- GREEN: project-experience-consolidation -> PASS，已将长期经验修正为“活跃订单冻结的正式发布路线全部工序是唯一候选集合，`PENDING` 任务只附着上下文且不裁剪列表”，并同步更新经验索引。
- GREEN: task-closeout-cleanup preview -> PASS，清理范围仅包含本任务临时证据、E2E 脚本/结果、运行补丁目录和页面截图，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- GREEN: task-closeout-cleanup apply -> PASS，`blocked=<none>`、`warnings=<none>`，所有声明的任务临时产物已删除。
- GREEN: final runtime sanity -> PASS，收尾期间并发任务将后端重启为 `backend-runtime-control-20260809-123324-batch-record-codex-response.jar`（PID `37220`）；该 Jar 内 `MesFrontlinePqcContextServiceImpl.class` SHA256 与本任务已验证的 `target/classes` 产物完全一致，`48081` health=`UP`，前端 `8081` HTTP 200。

## Blockers

- 无。
