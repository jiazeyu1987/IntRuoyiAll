# 任务：修正打开工序进入填写界面

## 任务目标

将批记录详情页「打开工序 / 打开填写 / 打开返工」入口从 eDHR 执行详情页改为 eDHR 执行表单页，保持先调用 `openEdhrBatchTask` 获取真实执行记录和工作任务上下文。

## 里程碑

- [x] M1：读取 PowerShell、经验索引、前端交付技能和统一前端样式门禁。
- [x] M2：补充 RED 静态契约，锁定打开工序必须进入 `/mes/pro/feedback/edhr-execution/form`。
- [x] M3：最小修改 `handleOpenTask` 跳转路径，不改后端接口合同。
- [x] M4：运行 targeted 静态验证和前端证据校验。
- [x] M5：更新任务记录、收尾清理并提交本轮直接改动。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文任务文档和测试文件使用显式 UTF-8 写入。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本轮只改路由行为，不做视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；按 BDD + RED/GREEN 记录证据。
- 真实 E2E：本轮不执行真实登录或写入链路；若后续需要真实 E2E，需先读取 `docs/login-access.md` 并跑登录 preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；后端未返回 `executionId` 时仍直接报错。
- 是否从根因和长期维护角度解决：是；修正入口语义，让打开填写类入口进入已注册的表单路由。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 打开工序进入填写界面 -> Given 用户在批记录详情页选中当前工序 / When 点击打开工序、打开填写或打开返工 / Then 前端先调用 `openEdhrBatchTask`，再跳转 `/mes/pro/feedback/edhr-execution/form` 并携带 `id/executionId/workTaskId/returnPath`。
- BDD: 审签归档入口不受影响 -> Given 用户点击签名记录、审批记录或单表归档 / When 跳转到当前工序证据入口 / Then 仍保持此前详情/审签/归档定位逻辑。

## 预期验证

- `node tests/e2e/edhr-open-process-form-route-static.spec.js`
- `node tests/e2e/edhr-signature-change-execution-entry-static.spec.js`
- `node tests/e2e/edhr-side-action-buttons-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-edhr-open-process-form-route/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 已完成：`handleOpenTask` 已改为跳转 `/mes/pro/feedback/edhr-execution/form`；签名记录和单表归档入口保持 `/detail`。验证通过：新增打开工序表单路由静态测试、签名执行入口静态测试、侧栏按钮静态测试、前端证据校验。

## Cleanup Keep

- doc/tasks/20260703-edhr-open-process-form-route/frontend-feature-evidence.md
