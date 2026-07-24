# Execution Log - 20260701-edhr-form-permission-signature-verification

BDD: 表单流转真实路径 -> Given 测试租户用户进入 eDHR 表单/批次详情 / When 执行表单查看、提交或流转相关路径 / Then 页面应展示真实数据、关键接口返回 200 且无静默失败。
BDD: 权限控制真实路径 -> Given 测试租户用户具备 eDHR 菜单与角色权限 / When 进入权限矩阵或受控对象页面 / Then 权限页可打开、权限接口返回真实结果且无误报成功。
BDD: 电子签名真实路径 -> Given 用户进入 eDHR 签名/追踪链路 / When 执行签名追踪或签名页校验 / Then 签名记录、签名时间、追踪入口与接口链路保持通顺。
BDD: 缺少真实前置即阻塞 -> Given 缺少可验证 required 缺失草稿或审批链多角色/签名密码/未使用工单任务 / When 执行真实 E2E / Then 记录 BLOCKED，不宣称通过。

GREEN: task-bootstrap -> PASS，已建立表单流转、权限控制、电子签名专项验收任务台账。
GREEN: experience-preflight -> PASS，已读取 PowerShell、登录、worktree 与 E2E 相关项目门禁；本轮使用真实测试租户路径验收，不使用 mock、接口造数或测试专用 UI。

RED: tracking-signature-real-flow -> FAIL，真实追踪页 `/tracking-page` 请求 executionId `40` 返回 500：`eDHR 追踪最后操作缺少工序名称: executionId=40`。
RED: backend-tracking-node-regression -> FAIL，`mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test` 复现缺少工序名称异常。
GREEN: backend-tracking-node-regression -> PASS，后端追踪最后操作节点名按正式数据源优先级解析：route process 名称 -> 签名 BPM 任务名 -> 签名动作业务含义；actor 仍 fail-fast，不引入通用 fallback。
GREEN: backend-tracking-regression-suite -> PASS，`mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_returnMesOwnedExecutionAndSignatureData+trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test`，Tests run: 2。
GREEN: tracking-signature-real-flow -> PASS，前端真实 E2E 复跑通过；executionId `40` 追踪列表展示 `归档封存`、详情页只读追踪时间线可见、签名页展示 5 条真实签名记录，`ARCHIVE_SEAL` 过滤返回 2 条。

BLOCKER: backend-runtime-restart -> `restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` 超时，遗留本轮 Maven package 进程且 48081 暂时不监听。
GREEN: backend-runtime-resume -> PASS，已停止本轮挂起 Maven 构建进程，并使用最新 int_main runtime jar 手动恢复后端；`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。

BLOCKER: required-submit-gate-real-flow -> 前端真实 E2E 使用真实草稿 executionId `711` 得到 BLOCKED：该草稿页面显示“无必填项/当前快照没有必填字段”，无法验证“缺失 required 字段阻止提交”。
BLOCKER: approval-tracking-real-flow -> 前端真实 E2E 缺少完整 `EDHR_E2E_*` 账号、签名密码、fresh 工单/任务与 SUBMITTED 负向记录。

GREEN: approval-history-readonly-real-flow -> PASS，前端真实只读 E2E 通过后端审批详情、追踪时间线和签名分页合同；executionId `761` 展示已关闭 `APPROVE` 证据，executionId `760` 展示已驳回 `REJECT` 证据与驳回原因。

RESULT: partial-pass-with-blockers -> 后端追踪/签名根因缺陷已修复并验证；权限控制、电子签名/追踪样本链路、审批历史只读展示链路已通过真实 E2E；表单必填提交门禁与完整审批生命周期因真实前置缺失被阻塞，不能宣称全部正确通顺。
