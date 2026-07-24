# 任务：电子签名页签改为路由子页签

## 任务目标

将 `电子签名` 页面内的 8 个 tab 改为可直接访问的路由子页签：`/signature-governance/overview`、`/signature-governance/file-signatures`、`/signature-governance/batch-signatures`、`/signature-governance/authorizations`、`/signature-governance/retention`、`/signature-governance/periodic-review`、`/signature-governance/csv-package`、`/signature-governance/policy`。个人中心、总览卡片、eDHR 记录跳转和 E2E helper 均使用子页签路径，不再把 `?tab=` 作为主入口。

## 里程碑

- [x] M1：读取经验门禁与前端/后端交付契约，确认上一电子签名任务已完成。
- [x] M2：先写 RED 契约测试，要求电子签名存在路由子页签并禁止 query tab 主入口。
- [x] M3：改造前端路由、页签切换和已知内部跳转。
- [x] M4：同步后端 portal 返回路径和对应测试。
- [x] M5：运行静态、类型、后端单测和真实 E2E 验证。

## 预期验证

- `node scripts/signature-governance-page-contract.test.mjs`
- `node tests/e2e/signature-governance-e2e-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- 后端 portal 相关单测：`DccSignatureGovernancePortalAdapterTest`、`MesEdhrSignatureGovernancePortalAdapterTest`、`SignatureGovernancePortalServiceTest`、`SignatureGovernanceControllerTest`
- 真实 Playwright 登录本机 `http://localhost:8081`，使用 `测试租户/aoteman/111111` 打开各电子签名子页签，确认非 404 且错误显式暴露。

## 当前状态

已完成。电子签名页内 8 个 tab 已迁移为 `/signature-governance/<tab>` 路由子页签，个人中心、eDHR 明细、E2E helper 与后端 portal 入口路径已对齐。

## Current Status

completed

## 前一任务检查

- 前端上一电子签名任务 `20260624-unified-electronic-signature-primary-tab` 已标记完成。
- 当前前端仓库存在排程、报工等非本任务脏改动；本任务只修改电子签名路由、页面、契约测试和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败必须阻塞，不切换账号或环境。
- `docs/server-access.md`：本机后端健康检查或重启失败时先核本机运行控制脚本和运行参数，不误判为业务功能失败。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：页面保持密集操作台风格，子页签不做营销化视觉重构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。子页签路径缺失或未知时显示明确错误，不静默跳回旧入口。
- `是否从根因和长期维护角度解决`：是。将页面内 tab 与路由、内部入口、portal 返回路径统一，后续模块可以继续挂到电子签名子页签。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 电子签名 tab 作为子页签路由 -> Given 用户打开 /signature-governance/file-signatures / When 页面加载完成 / Then 选中 文件签名记录 子页签并展示对应内容。`
- `BDD: 页签切换更新路径 -> Given 用户在电子签名任一子页签 / When 点击 批记录签名记录 / Then 地址栏变为 /signature-governance/batch-signatures 且保留 executionId 等业务 query。`
- `BDD: 内部入口使用子页签路径 -> Given 用户从个人中心、eDHR 明细或总览卡片进入签名记录 / When 点击入口 / Then 跳转到 /signature-governance/<子页签>，不再使用 /dcc/controlled-file/signatures、/mes/pro/feedback/edhr-signatures 或 ?tab= 主入口。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-route-subtabs/task.md`
- `doc/tasks/20260624-signature-governance-route-subtabs/execution-log.md`
- `doc/tasks/20260624-signature-governance-route-subtabs/frontend-feature-evidence.md`
