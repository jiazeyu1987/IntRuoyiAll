# 任务：电子签名授权状态中文显示

## 任务目标

将电子签名用户授权列表中的授权状态从后端枚举值（例如 `UNAUTHORIZED`）显示为中文业务文案，避免页面露出英文状态码。

## 里程碑

- [x] M1：写 RED 契约，覆盖授权状态中文标签和 `UNAUTHORIZED`。
- [x] M2：补齐 DCC 签名授权状态枚举映射。
- [x] M3：运行契约测试、静态 E2E 和类型检查。
- [x] M4：真实登录验证用户授权页不显示英文 `UNAUTHORIZED`。
- [x] M5：记录证据、closeout 并只提交本任务文件。

## 预期验证

- `node scripts\signature-governance-page-contract.test.mjs`
- `node tests\e2e\signature-governance-e2e-static.spec.js`
- `npm run ts:check`
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，访问 `/signature-governance/authorizations`，确认授权状态列显示中文。

## 当前状态

进行中。已确认授权表格调用 `getDccSignatureAuthorizationStateLabel`，但共享映射缺少后端真实返回的 `UNAUTHORIZED`。

## Current Status

completed

## 完成记录

- 代码结果：`DCC_SIGNATURE_AUTHORIZATION_STATE_OPTIONS` 已补齐 `UNAUTHORIZED -> 未授权`，并将 `ENABLED` 展示文案统一为 `已启用`。
- 验证结果：契约测试、静态 E2E、类型检查和测试租户真实 Playwright 验证均通过。
- 真实 E2E：`http://localhost:8081/signature-governance/authorizations` 可见 `未授权`，不可见 `UNAUTHORIZED`。

## 前一任务检查

- 上一电子签名前端任务 `20260624-signature-governance-embedded-single-card` 已完成。
- 当前前端仓库存在其它任务脏改动和部分 staged 文件；本任务只修改授权状态共享枚举、电子签名契约测试和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：状态标签应短小、可扫读，业务状态使用中文文案。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，补齐真实后端枚举到中文文案的映射。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 授权状态显示中文 -> Given 后端返回授权状态 UNAUTHORIZED/ENABLED/DISABLED/LOCKED / When 用户打开电子签名用户授权列表 / Then 授权状态列显示未授权/已启用/已停用/已锁定，不显示英文枚举值。`

## Cleanup Keep

- `doc/tasks/20260624-signature-authorization-state-chinese/task.md`
- `doc/tasks/20260624-signature-authorization-state-chinese/execution-log.md`
- `doc/tasks/20260624-signature-authorization-state-chinese/frontend-feature-evidence.md`
