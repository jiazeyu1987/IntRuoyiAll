# Execution Log

## User Intent

- 活跃订单池每行操作改为“移除”和“报异常”两个按钮。
- “报异常”针对当前行活跃订单，仅填写异常原因。
- 异常订单的生产订单 ID 显示红色，且不参与后续工作分配。
- 删除独立“异常”页签，将其功能合并到行内“报异常”。
- 真实页面点击“报异常”并填写原因后返回“请求参数不正确:不能为空”，要求修复。

## BDD Scenarios

- BDD: 活跃订单行内上报异常 -> Given 生产组长活跃订单池存在正常订单，When 点击该行“报异常”、填写异常原因并确认，Then 系统为该生产订单记录未关闭异常、刷新列表后生产订单 ID 显示红色，且订单不再参与自动或手工工作分配。
- BDD: 未关闭异常的服务端约束 -> Given 生产订单存在已上报或处理中的未关闭异常，When 活跃订单列表、FIFO 分配、手工分配或再次上报读取该订单，Then 列表返回异常状态，FIFO 跳过该订单，手工分配及重复上报明确失败。
- BDD: 异常入口合并 -> Given 生产组长进入任一生产管理模块，When 查看顶部页签和活跃订单池，Then 不再显示独立“异常”页签，活跃订单行显示“移除”和“报异常”，旧独立异常表单不存在。
- BDD: 异常原因必填 -> Given 生产组长打开某活跃订单的异常对话框，When 未填写异常原因即确认，Then 前端阻止提交；后端收到空白原因时也明确拒绝。
- BDD: 运行态仅校验异常原因 -> Given 前端提交 `workOrderId + abnormalDescription` 且异常原因非空，When 本机 `48081` 处理异常上报，Then 请求不得再因旧 `abnormalReasonCode` 为空而被 Bean Validation 拒绝，并应进入正式活跃订单异常服务。

## Command Intent

- 只读检索现有前后端实现、测试、项目规则及工作区状态，确认正式数据来源和受影响范围。
- 先运行聚焦静态测试和后端单元测试建立 RED 证据，再修改生产代码。
- 完成实现后运行定向 GREEN、相关回归和真实页面 E2E。

## TDD Evidence

- RED: `node .\\tests\\e2e\\work-order-abnormal-minimal-report-static.spec.js` -> FAIL, 当前页面仍保留 `showProductionExceptionModule` 独立异常模块，符合预期失败原因。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesWorkOrderAbnormalReportServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderActiveOrderServiceTest' test` -> FAIL，新增测试要求的异常状态服务、接口字段和构造依赖尚未实现；Maven 在已知损坏的旧 `target_corrupt_m4_20260802_1327` 所在卷上未返回完整编译诊断，后续 GREEN 将以清晰的退出码和 Surefire 报告复核。
- GREEN: `java @doc/tasks/20260807-production-leader-active-order-abnormal-actions/backend-junit-targeted.args` -> PASS，4 个相关测试类共 38 个测试全部通过，覆盖异常上报、重复异常拒绝、活跃订单异常投影、FIFO 排除和手工分配拒绝。
- GREEN: `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js` -> PASS，行内“移除 / 报异常”、仅异常原因、红色生产订单 ID、删除异常页签及前后端合同通过。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `validate_frontend_feature.py --evidence ...` 与 `validate_backend_api.py --evidence ...` -> PASS；两个 validator self-test 同时 PASS。
- REGRESSION: 3 个受构造依赖影响的 P0 测试类共 21 项通过 20 项；`reviewSubmissionShouldPersistStructuredReviewSignature` 在旧的事件根路径校验处失败，未进入本次异常状态或分配逻辑。

## Milestone Updates

- M1 completed：确认异常正式记录表和上报接口已存在；确认当前独立异常页签要求原因码与原因说明；确认活跃订单列表、FIFO 与手工分配尚未使用未关闭异常状态。
- M2 completed：新前端行内交互契约、后端异常上报、列表投影、FIFO 排除和手工分配拒绝测试已写入，并取得预期 RED。
- M3 completed：新增统一未关闭异常状态查询；异常上报锁定当前组长活跃订单、拒绝重复未关闭异常；列表投影异常状态；FIFO 排除且手工分配明确拒绝异常订单。
- M4 completed：活跃订单行显示“移除”和“报异常”；弹框仅填写异常原因；异常生产订单 ID 使用错误色；前端分配候选过滤异常订单；生产组长独立“异常”页签及旧表单已删除。
- M5 partial：后端 38 项相关测试、20 项 P0 回归、前端聚焦静态合同和类型检查通过；真实写入型 E2E 被本机后端运行态阻塞。
- 经验沉淀检查：现有 `docs/powershell-memory.md#Maven-javac/Lombok-class-写入长时间运行门禁` 已完整覆盖共享 `target` 并发和隔离 javac/JUnit 补充验证，本任务没有新增可复用规则，因此未修改长期经验文档。
- M6 completed：task-closeout-cleanup preview 的 keep 为 3 个核心记录、blocked/warnings 均为空；apply 删除本任务 evidence、隔离 class、参数文件和日志。脚本遗漏的 1 个含 `$` 文件名的 class 经路径与唯一内容复核后显式删除，最终任务目录仅保留 3 个核心记录。

## Blockers

- 本机前端 `8081` HTTP 200，但 `http://127.0.0.1:48081/actuator/health` 连接被拒绝；真实页面需要当前后端合同，不能用旧 Jar 或 API-only 代替。
- 主工作区存在其他任务的大量脏改动和持续并发 Maven 进程，按运行态门禁不得从该脏工作区打包或重启 `48081`。
- 标准 Maven 定向测试因共享 `yudao-module-mes/target` 被并发进程反复覆盖而阻塞；任务隔离的显式 javac + JUnit Launcher 补充验证已通过 38 项，但不冒充 Maven/Surefire 通过。
- `mes-process-pool-team-leader-static.spec.js` 当前在并发任务的“生产报工修订接口”断言处失败；该断言与本次异常订单功能无关，聚焦异常订单合同已单独通过。

## Runtime Regression Resume

- RED: 真实页面 `POST /admin-api/mes/pro/process-pool/team-leader/work-order/abnormal/report`，请求体已填写生产订单 ID 与异常原因 -> FAIL，响应“请求参数不正确:不能为空”。
- 运行态基线：`48081` PID `40088`，稳定 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260807-erp-connection-switch.jar`，health=`UP`；源码请求 VO 已只有 `workOrderId` 与 `abnormalDescription`，说明运行 Jar 合同落后于源码。
- RED: 反编译旧运行 Jar 内嵌 MES 请求 VO -> FAIL，运行态类为 `fields: 3`，旧字段 `abnormalReasonCode` 仍带 `@NotBlank`；旧外层 Jar SHA-256 为 `0A4C30B257A8C9ACEA2CF171A8F60234702CEDD07B077532133B689C2EB43868`，根因已锁定为稳定运行 Jar 请求合同陈旧。
- GREEN: 从旧稳定 Jar 内嵌 MES Jar 构建最小类组补丁并通过 `-Xverify:all`；新外层 Jar SHA-256=`A8695B9B0E0E06249B7424A37474984DC5A4DD44A1C7B158CF1016099C3EC244`，内嵌 MES 条目唯一且未压缩，旧外层 Jar hash 保持不变。
- GREEN: 停止旧 PID `40088` 后启动新 PID `61676`，`48081/actuator/health` 返回 `UP`，端口归属和命令行均确认指向 `backend-runtime-control-20260807-active-order-abnormal-fix.jar`。
- GREEN: Playwright 真实页面对生产订单 `980028` 以异常原因 `123123` 完成上报；刷新后接口返回 `abnormal=true`，页面 ID 颜色为 `rgb(245, 108, 108)`、原因标题为 `123123`、报异常按钮禁用，页面错误和失败请求均为 0。
- REGRESSION: `node tests/e2e/work-order-abnormal-minimal-report-static.spec.js` -> PASS；运行 Jar 请求 VO 反编译为 `fields: 2` 且不再包含 `abnormalReasonCode`。
- 非目标链路观察：页面初次加载的 `/team-device/list` HTTP 200 但业务码 404；该请求属于人员/设备配置链路，未造成活跃订单控件缺失，settled 截图时无可见消息或通知，本任务不扩大范围修改该并行功能。
- 经验沉淀复核：`docs/local-runtime.md#2026-07-24-隔离构建-Jar-加载门禁` 已明确覆盖运行 Jar 内嵌模块字节码核对、以旧模块为底保留并行 class、类组与 `$*.class` 成组替换、`jar uf0` 未压缩写回、PID/hash/health/登录态 API/真实页面验证；本轮没有新增通用规则，因此不修改或新建长期经验文档。
- 并发运行态复核：首次 cleanup 后发现另一任务已将 `48081` 切换为 `backend-latest-20260807-1919-team-device-list.jar`（PID `2396`）。该新 Jar 外层 SHA-256=`8F8C8443C1F2B66613899C79FED5E97631DE7A6848A147EA5830445121982691`、内嵌 MES SHA-256=`6E11A8D55652DA496F88A3E4612C0202609CF4A726BE9DF3FB4B0A398453B841`、嵌套条目未压缩；反编译请求 VO 仍为 2 个字段且不含 `abnormalReasonCode`，因此未停止或覆盖该并行任务进程。
- 最终真实页面只读复核：在并发新 Jar 上重新登录生产组长页面，订单 `980028` 仍为红色 `rgb(245, 108, 108)`、原因标题 `123123`、报异常按钮禁用；`visibleMessages=0`、`pageErrors=[]`、`requestfailed=[]`。
- 最终清理：task-closeout-cleanup 两轮 preview/apply 已删除任务内编译、反编译、验证器、内嵌 Jar、Playwright 截图/快照等任务自有临时产物；任务目录最终仅保留 `task.md`、`execution-log.md`、`verification-report.md`，未停止并发 PID `2396`，未触碰其它任务文件。
