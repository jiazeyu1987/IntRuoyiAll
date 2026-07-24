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
- 是否从根因和长期维护角度解决：是；发现追踪页真实数据 500 后已补后端根因修复和回归测试，发现 E2E 脚本登录/浏览器前置不稳后已对齐现有真实 E2E 登录方式。
- 是否存在临时补丁或绕过：否；必填提交门禁和完整审批流因缺少真实前置数据/凭据被阻塞，未用 mock 或默认成功替代。

## BDD 场景

- BDD: 表单流转真实路径 -> Given 测试租户用户进入 eDHR 表单/批次详情 / When 执行表单查看、提交或流转相关路径 / Then 页面应展示真实数据、关键接口返回 200 且无静默失败。
- BDD: 权限控制真实路径 -> Given 测试租户用户具备 eDHR 菜单与角色权限 / When 进入权限矩阵或受控对象页面 / Then 权限页可打开、权限接口返回真实结果且无误报成功。
- BDD: 电子签名真实路径 -> Given 用户进入 eDHR 签名/追踪链路 / When 执行签名追踪或签名页校验 / Then 签名记录、签名时间、追踪入口与接口链路保持通顺。
- BDD: 缺少真实前置即阻塞 -> Given 缺少可验证 required 缺失草稿或审批链多角色/签名密码/未使用工单任务 / When 执行真实 E2E / Then 记录 BLOCKED，不宣称通过。

## Milestones

1. M1：建立专项验收任务台账。completed
2. M2：运行表单、权限、电子签名相关静态检查。completed
3. M3：运行权限矩阵、签名追踪真实 E2E。completed
4. M4：运行必填提交门禁和完整审批流真实 E2E。blocked
5. M5：记录缺陷、阻塞或验收结论。completed

## Expected Verification

- `node tests/e2e/edhr-form-static.spec.js`
- `node tests/e2e/edhr-signature-page-ui-static.spec.js`
- `node tests/e2e/edhr-inline-signature-cells-static.spec.js`
- `node tests/e2e/edhr-signature-time-optional-static.spec.js`
- `node tests/e2e/edhr-special-node-skip-signature-static.spec.js`
- `node tests/e2e/edhr-permission-matrix-evaluate-advanced-static.spec.js`
- `node tests/e2e/edhr-permission-subject-selector-static.spec.js`
- `node tests/e2e/edhr-flow-intervention-static.spec.js`
- `node --check tests/e2e/edhr-permission-matrix-real-flow.e2e.js`
- `node tests/e2e/edhr-permission-matrix-real-flow.e2e.js`
- `node --check tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `node tests/e2e/edhr-tracking-signature-real-flow.e2e.js`
- `node --check tests/e2e/edhr-required-submit-gate-real-flow.e2e.js`
- `node tests/e2e/edhr-required-submit-gate-real-flow.e2e.js`
- `node --check tests/e2e/edhr-approval-history-readonly-real-flow.e2e.js`
- `node tests/e2e/edhr-approval-history-readonly-real-flow.e2e.js`
- `node --check tests/e2e/edhr-approval-tracking-real-flow.e2e.js`
- `node tests/e2e/edhr-approval-tracking-real-flow.e2e.js`

## Verification Summary

- PASS：静态表单/签名/权限/流程干预检查通过。
- PASS：权限矩阵真实 UI 流程通过，测试租户 `aoteman` 完成真实对象权限矩阵保存、读取和评估，返回真实 `scopeId`、规则明细与 `operationAuditEventId`。
- PASS：追踪/签名真实 UI 流程通过，真实 executionId `40` 能进入追踪页、详情页只读时间线、签名页和 `ARCHIVE_SEAL` 过滤；追踪节点显示 `归档封存`。
- PASS：审批历史只读真实 UI 流程通过，真实已关闭 executionId `761` 展示审批详情、追踪时间线和 `APPROVE` 签名；真实已驳回 executionId `760` 展示审批详情、追踪时间线、`REJECT` 签名与驳回原因 `E2E-REJECT-1782316557196-T1`。
- PASS：必填提交门禁脚本语法通过，并已修复登录方式与系统 Chrome 启动支持。
- BLOCKED：必填提交门禁真实 E2E 缺少“含 required 缺失字段”的真实草稿。已验证 candidate executionId `711` 为真实草稿，但页面显示“当前快照没有必填字段/无必填项”，不能证明缺失必填阻止提交。
- BLOCKED：完整审批流真实 E2E 缺少审批人凭据、签名密码、fresh 未使用工单/任务上下文、SUBMITTED 负向记录等真实前置。

## Current Blockers

- 必填提交门禁：需要测试租户内真实数字型 DRAFT executionId，且该执行快照必须至少包含一个未填写的 required 非签名/非附件字段。
- 完整审批流：需要提供 `EDHR_E2E_BASE_URL`、`EDHR_E2E_TENANT`、执行人/审批人账号密码、执行/审批/归档/FIELD_CHANGE 签名密码、DRAFT/APPROVE/REJECT fresh 工单任务、期望审批人姓名、期望驳回原因，以及 SUBMITTED 负向输入。
- 结论边界：权限控制、签名追踪样本链路、审批历史只读展示链路已通过；表单提交门禁和完整审批生命周期尚不能宣称全链路正确。
