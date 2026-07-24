# 任务：eDHR 表单流转、权限控制、电子签名专项验收

- Task ID: 20260701-edhr-form-permission-signature-verification
- Created: 2026-07-01
- Current Status: blocked

## Task Goal

专项验证 eDHR 的表单流转、对象权限/租户权限、电子签名与流程追踪是否在当前 int_main 真实环境中通顺；仅凭既有静态测试不宣称全链路正确，必须通过真实用户路径或明确记录阻塞。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 与中文文本读写使用显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- 命中 `docs/login-access.md`：真实 E2E 使用测试租户 `测试租户/aoteman`，最终只读验证可用芋道源码/admin，不得用 live 租户改数据。
- 命中 E2E 门禁：通过 Playwright 操作前端真实路径，API 仅用于最终校验；缺少真实数据时记录 BLOCKED，不 mock、不接口造数、不绕过前端。
- 命中 worktree/运行态门禁：本轮验证的是主工作区 `int_main` 的 `8081/48081` 运行态。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；追踪页 500 已以正式事件数据源修复，并补后端回归测试。
- 是否存在临时补丁或绕过：否；缺少真实前置时记录 BLOCKED，不以 mock 或默认成功代替。

## BDD 场景

- BDD: 表单流转真实路径 -> Given 测试租户用户进入 eDHR 表单/批次详情 / When 执行表单查看、提交或流转相关路径 / Then 页面应展示真实数据、关键接口返回 200 且无静默失败。
- BDD: 权限控制真实路径 -> Given 测试租户用户具备 eDHR 菜单与角色权限 / When 进入权限矩阵或受控对象页面 / Then 权限页可打开、权限接口返回真实结果且无误报成功。
- BDD: 电子签名真实路径 -> Given 用户进入 eDHR 签名/追踪链路 / When 执行签名追踪或签名页校验 / Then 签名记录、签名时间、追踪入口与接口链路保持通顺。
- BDD: 缺少真实前置即阻塞 -> Given 缺少可验证 required 缺失草稿或审批链多角色/签名密码/未使用工单任务 / When 执行真实 E2E / Then 记录 BLOCKED，不宣称通过。

## Milestones

1. M1：建立专项验收任务台账。completed
2. M2：运行表单、权限、电子签名相关静态检查。completed
3. M3：修复追踪真实数据 500 并补回归测试。completed
4. M4：配合真实 E2E 验证权限矩阵、追踪/签名链路。completed
5. M5：记录完整提交/审批链路阻塞。completed

## Expected Verification

- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -pl yudao-module-mes '-Dtest=MesProBatchRecordExecutionServiceImplTest#trackingAndSignatureQuery_returnMesOwnedExecutionAndSignatureData+trackingAndSignatureQuery_archiveSignatureUsesEventNodeWhenRouteProcessMetadataMissing' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- 前端真实 E2E：权限矩阵、追踪/签名、必填提交门禁、审批流脚本结果以 `yudao-ui-admin-vue3` 任务文档为准。

## Verification Summary

- PASS：后端追踪节点缺失工序名回归测试 RED/GREEN 已完成。
- PASS：后端追踪/签名查询回归套件通过，Tests run: 2。
- PASS：本地 int_main 后端已恢复健康，`/actuator/health` 返回 UP。
- PASS：审批历史只读真实 UI 通过后端合同校验，真实已关闭 executionId `761` 的 `/approval-detail`、`/tracking-timeline`、`/signature-page` 展示 `APPROVE` 证据；真实已驳回 executionId `760` 展示 `REJECT` 与驳回原因。
- BLOCKED：完整 eDHR 表单提交门禁和审批生命周期仍依赖前端真实 E2E 的真实数据/凭据前置，目前不能宣称全部流程通顺。

## Current Blockers

- 必填提交门禁：需要测试租户内真实数字型 DRAFT executionId，且该执行快照必须至少包含一个未填写的 required 非签名/非附件字段。
- 完整审批流：需要完整 `EDHR_E2E_*` 多角色账号、签名密码、fresh 工单/任务上下文和 SUBMITTED 负向输入。
