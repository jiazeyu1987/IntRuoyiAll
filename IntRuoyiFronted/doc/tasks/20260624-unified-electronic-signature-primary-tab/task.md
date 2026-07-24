# 任务：统一电子签名一级页签前端嵌入

## 任务目标

将 `signature-governance` 改造成唯一的一级电子签名页面，把文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包与统一策略拆成统一页内子页签；不再暴露 `DCC电子签名`、`eDHR电子签名` 独立入口式文案。

## 里程碑

- [x] M1：创建独立 worktree，确认前一任务完成并记录经验门禁。
- [x] M2：先写前端 RED 契约测试，锁定子页签和旧路径禁止项。
- [x] M3：抽出 DCC 文件签名记录、用户授权和 eDHR 批记录签名记录嵌入组件。
- [x] M4：改造 `signature-governance` 为一级页签容器并同步个人中心/E2E 入口。
- [x] M5：运行静态验证、类型检查和真实 E2E。

## 预期验证

- `node scripts/signature-governance-page-contract.test.mjs`
- `node tests/e2e/signature-governance-e2e-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check`
- 真实 Playwright 登录 `http://127.0.0.1:8089/signature-governance`，逐个打开 `总览`、`文件签名记录`、`批记录签名记录`、`用户授权`、`统一策略`。

## 当前状态

已完成。前端实现、静态契约、类型检查、融合后真实 E2E 均已通过。

## 前一任务检查

- 前端前一相关任务 `20260623-unified-electronic-signature-tab` 已标记完成，允许继续本任务。
- 主工作区存在非本任务脏改动，本任务仅在独立 worktree 内实现和提交。

## 经验门禁

- `docs/worktree-memory.md`：前端验证必须使用本 worktree 的显式 FE/BE 端口与 runtime 归属，不默认使用主工作区。
- `docs/login-access.md`：真实 E2E 默认本机测试租户 `测试租户/aoteman/111111`；长链路前必须先跑登录最小路径。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：页面保持密集操作台风格，工具栏、表格、状态标签与入口卡片都要克制清晰。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口失败、缺权限、未授权要直接展示错误或阻断状态。
- `是否从根因和长期维护角度解决`：是。统一页签直接承载签名相关功能，后续模块接入统一容器。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 统一一级页签展示全部电子签名内容 -> Given 用户打开 /signature-governance / When 页面加载完成 / Then 看到总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略子页签。`
- `BDD: DCC/eDHR 旧入口不再暴露 -> Given 用户访问电子签名一级页签 / When 查看页面文案和模块卡片 / Then 不出现 DCC电子签名 或 eDHR电子签名 独立入口式文案。`
- `BDD: query 参数直达子页签 -> Given URL 为 /signature-governance?tab=batch-signatures&executionId=<id> / When 页面加载 / Then 自动选中批记录签名记录并按 executionId 过滤。`
- `BDD: 旧业务能力嵌入统一页 -> Given 用户有文件签名或批记录签名权限 / When 打开对应子页签 / Then 页面调用原真实 API 展示记录、授权和错误状态，不使用 mock 数据。`

## Cleanup Keep

- `doc/tasks/20260624-unified-electronic-signature-primary-tab/task.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/execution-log.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/frontend-feature-evidence.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/real-e2e-evidence.md`
