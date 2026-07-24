# Execution Log - 20260701-edhr-form-permission-signature-verification

BDD: 表单流转真实路径 -> Given 测试租户用户进入 eDHR 表单/批次详情 / When 执行表单查看、提交或流转相关路径 / Then 页面应展示真实数据、关键接口返回 200 且无静默失败。
BDD: 权限控制真实路径 -> Given 测试租户用户具备 eDHR 菜单与角色权限 / When 进入权限矩阵或受控对象页面 / Then 权限页可打开、权限接口返回真实结果且无误报成功。
BDD: 电子签名真实路径 -> Given 用户进入 eDHR 签名/追踪链路 / When 执行签名追踪或签名页校验 / Then 签名记录、签名时间、追踪入口与接口链路保持通顺。
BDD: 缺少真实前置即阻塞 -> Given 缺少可验证 required 缺失草稿或审批链多角色/签名密码/未使用工单任务 / When 执行真实 E2E / Then 记录 BLOCKED，不宣称通过。

GREEN: task-bootstrap -> PASS，已建立表单流转、权限控制、电子签名专项验收任务台账。
GREEN: experience-preflight -> PASS，已读取 PowerShell、登录、worktree 与 E2E 相关项目门禁；本轮使用真实测试租户路径验收，不使用 mock、接口造数或测试专用 UI。
GREEN: static-form-permission-signature -> PASS，`edhr-form-static`、`edhr-signature-page-ui-static`、`edhr-inline-signature-cells-static`、`edhr-signature-time-optional-static`、`edhr-special-node-skip-signature-static`、`edhr-permission-matrix-evaluate-advanced-static`、`edhr-permission-subject-selector-static`、`edhr-flow-intervention-static` 均通过。
BLOCKER: official-login-preflight -> 本机 Playwright headless shell 启动失败，报 `Invalid file descriptor to ICU data received`；该失败发生在浏览器启动阶段，尚未进入登录页，不能作为 eDHR 表单/权限/签名业务失败证据。后续真实 E2E 改用系统 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`。
GREEN: real-e2e-script-syntax -> PASS，`edhr-permission-matrix-real-flow.e2e.js`、`edhr-tracking-signature-real-flow.e2e.js`、`edhr-approval-tracking-real-flow.e2e.js`、`edhr-required-submit-gate-real-flow.e2e.js` 均通过 `node --check`。

GREEN: permission-matrix-real-flow -> PASS，使用显式系统 Chrome 运行 `node tests/e2e/edhr-permission-matrix-real-flow.e2e.js`，测试租户 `aoteman` 在真实对象权限矩阵页完成保存、读取和评估；后端返回真实 `scopeId=1`、规则明细和 `operationAuditEventId=4767`。

RED: tracking-signature-real-flow -> FAIL，真实追踪页 `/tracking-page` 请求 executionId `40` 返回 500：`eDHR 追踪最后操作缺少工序名称: executionId=40`。
RED: backend-tracking-node-regression -> FAIL，`mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test` 复现缺少工序名称异常。
GREEN: backend-tracking-node-regression -> PASS，后端追踪最后操作节点名按正式数据源优先级解析：route process 名称 -> 签名 BPM 任务名 -> 签名动作业务含义；actor 仍 fail-fast，不引入通用 fallback。
GREEN: backend-tracking-regression-suite -> PASS，`mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_returnMesOwnedExecutionAndSignatureData+trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test`，Tests run: 2。
GREEN: tracking-signature-real-flow -> PASS，`node tests/e2e/edhr-tracking-signature-real-flow.e2e.js` 通过；executionId `40` 追踪列表展示 `归档封存`、详情页只读追踪时间线可见、签名页展示 5 条真实签名记录，`ARCHIVE_SEAL` 过滤返回 2 条。

GREEN: backend-runtime-resume -> PASS，`48081` 后端在重启超时后已停止本轮挂起 Maven 构建进程，并用最新 int_main runtime jar 手动恢复；`/actuator/health` 返回 `{"status":"UP"}`，`8081` 前端返回 HTTP 200。

RED: required-submit-gate-real-flow -> FAIL，首次运行因脚本登录 helper 与当前登录页结构不一致，停留在 `/login`。
GREEN: required-submit-gate-script-login -> PASS，已将必填提交门禁 E2E 登录方式对齐通过的权限/追踪真实 E2E：固定 `/login?redirect=/index`、显式租户选择、验证码 fail-fast、系统 Chrome 支持。
BLOCKER: required-submit-gate-real-flow -> 运行 `node tests/e2e/edhr-required-submit-gate-real-flow.e2e.js`，使用真实草稿 executionId `711`，结果为 BLOCKED：该真实草稿页面显示“无必填项/当前快照没有必填字段”，无法验证“缺失 required 字段阻止提交”。提交接口请求数为 `0`；未使用 mock、接口造数或测试专用 UI。
BLOCKER: required-submit-data-discovery -> 对测试租户 `tenant_id=122` 的近期 DRAFT/执行记录扫描未发现 `execution_snapshot_json` 或 `sheet_layout_json` 中存在 `"required":true` / `"required": true` 的可用真实草稿；当前缺少真实 required 缺失数据。

BLOCKER: approval-tracking-real-flow -> `node tests/e2e/edhr-approval-tracking-real-flow.e2e.js` 结果为 BLOCKED：缺少 `EDHR_E2E_*` 真实账号、签名密码、fresh 工单/任务和 SUBMITTED 负向记录，不能执行完整审批生命周期真实 UI E2E。

RED: approval-history-readonly-real-flow -> FAIL，首轮只读审批历史 E2E 能拿到真实审批详情/追踪/签名接口，但 DOM 可见性断言误命中隐藏的执行快照 `<pre>`，导致无法证明审批人姓名在当前可见页签中展示。
GREEN: approval-history-readonly-real-flow -> PASS，`node --check tests/e2e/edhr-approval-history-readonly-real-flow.e2e.js` 与 `node tests/e2e/edhr-approval-history-readonly-real-flow.e2e.js` 通过；真实已关闭 executionId `761` 展示 `APPROVE` 审批详情、追踪和签名证据，真实已驳回 executionId `760` 展示 `REJECT` 审批详情、追踪、签名与驳回原因 `E2E-REJECT-1782316557196-T1`。证据：`test-results/edhr-approval-history-readonly/result.json` 与 `doc/tasks/20260701-edhr-form-permission-signature-verification/approval-history-readonly-evidence.md`。

RESULT: partial-pass-with-blockers -> 权限控制、电子签名/追踪样本链路、审批历史只读展示链路已通过真实 E2E；表单必填提交门禁与完整审批生命周期因真实前置缺失被阻塞，不能宣称 eDHR 表单流转、权限控制、电子签名功能和流程“全部正确且通顺”。
