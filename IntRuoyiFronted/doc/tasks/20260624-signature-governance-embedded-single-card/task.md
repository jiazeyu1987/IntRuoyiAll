# 任务：电子签名嵌入子页只保留外层卡片

## 任务目标

修复电子签名子页签里仍能看到多层卡片嵌套的问题。电子签名一级页已经有最外层 `ContentWrap`，嵌入的文件签名记录、用户授权和批记录签名记录在 `embedded=true` 时不应再生成自己的 `ContentWrap` 卡片。

## 里程碑

- [x] M1：写 RED 契约，要求 embedded 签名子页面使用普通内容容器而不是内层 `ContentWrap`。
- [x] M2：改造 DCC 签名页和 eDHR 签名记录页的 embedded 根容器。
- [x] M3：运行契约测试、静态 E2E 和类型检查。
- [x] M4：真实登录验证文件签名、批记录签名、用户授权子路由只有外层卡片。
- [x] M5：记录证据、closeout 并只提交本任务文件。

## 预期验证

- `node scripts\signature-governance-page-contract.test.mjs`
- `node tests\e2e\signature-governance-e2e-static.spec.js`
- `npm run ts:check`
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，验证 `/signature-governance/file-signatures`、`/signature-governance/batch-signatures`、`/signature-governance/authorizations` 页面中电子签名内容下没有嵌套 `content-wrap` 卡片。

## 当前状态

已完成。DCC 签名页和 eDHR 签名记录页在 embedded 模式下改为普通 `div` 根容器，独立访问仍保留 `ContentWrap`。

## Current Status

completed

## 完成记录

- 已用 RED 契约复现 embedded 子页仍生成内层 `ContentWrap`。
- 已通过契约测试、静态 E2E、类型检查和真实 Playwright 验证。
- 真实 E2E 中 3 个电子签名子路由的 `nestedContentWraps=0`。

## 前一任务检查

- 上一电子签名前端任务 `20260624-signature-governance-flatten-content` 已完成。
- 当前前端仓库存在其它任务脏改动；本任务只修改电子签名嵌入页相关组件、电子签名契约测试和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：页面结构避免 nested cards，保留密集操作台风格。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，embedded 模式从根容器上去掉内层卡片。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 嵌入签名子页只保留外层卡片 -> Given 用户进入电子签名下的文件签名、批记录签名或用户授权子路由 / When 页面渲染 embedded 子页面 / Then 子页面内容直接显示在电子签名外层卡片内，不再生成额外 ContentWrap 卡片。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-embedded-single-card/task.md`
- `doc/tasks/20260624-signature-governance-embedded-single-card/execution-log.md`
- `doc/tasks/20260624-signature-governance-embedded-single-card/frontend-feature-evidence.md`
