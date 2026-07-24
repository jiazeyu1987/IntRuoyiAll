# 任务：统一电子签名页签前端入口

## 任务目标

在 `sign2` 前端 worktree 中建立统一电子签名页签，展示当前账号的 DCC、eDHR 签名入口、记录摘要、授权状态和待处理项，并保留跳转到各模块正式签名页执行的边界。

## 范围

- 扩展或改造现有 `signature-governance` 页面为统一电子签名页签。
- 首批接入 DCC、eDHR 摘要与正式入口。
- 保留 DCC/eDHR 原正式签名执行页，不用 BPM 通用审批按钮替代。
- 为 Showroom、IntAuth 和后续模块预留统一展示协议。

## 经验门禁

- `docs/worktree-memory.md`：前端验证必须使用 `sign2` worktree 的显式 FE/BE 端口与 runtime 归属，不默认使用主工作区。
- `docs/login-access.md`：真实登录/E2E 默认本机测试租户 `测试租户/aoteman/111111`；长链路前必须先跑登录最小路径。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：页面保持密集操作台风格，工具栏、表格、状态标签与入口卡片都要克制清晰。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口失败、缺权限、未授权要直接展示错误或阻塞状态。
- `是否从根因和长期维护角度解决`：是。统一页签消费统一后端聚合协议，不再在各页面复制签名中心。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 统一电子签名页签展示 DCC/eDHR 摘要 -> Given 当前账号具备统一电子签名页签权限 When 打开统一电子签名页签 Then 页面展示 DCC 与 eDHR 的授权状态、签名记录数、待处理数和正式入口。`
- `BDD: 未授权签名必须显式暴露 -> Given 当前账号未开通电子签名授权 When 页面加载摘要 Then 模块卡片展示未授权阻塞，不展示为正常可用。`
- `BDD: 正式签名仍回模块执行 -> Given DCC/eDHR 存在待处理签名项 When 用户点击处理入口 Then 跳转到模块正式签名页。`
- `BDD: 后续模块按统一协议接入 -> Given Showroom 或 IntAuth 未来接入 When 后端返回模块接入信息 Then 统一页签无需复制独立签名中心页面。`

## 里程碑

- [x] M1：创建 `sign2` 前端 worktree 并建立任务文档。
- [x] M2：补齐前端 RED 测试，锁定统一页签展示协议。
- [x] M3：实现统一电子签名页签的 DCC/eDHR 摘要、错误状态与跳转入口。
- [x] M4：运行静态验证、依赖锁验证和类型验证并记录证据。
- [x] M5：与后端聚合接口完成静态联调契约。

## 预期验证

- `node scripts/signature-governance-page-contract.test.mjs`
- `node tests/e2e/signature-governance-e2e-static.spec.js`
- `pnpm install --frozen-lockfile` 后再执行 `pnpm run ts:check`
- `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js`，使用 sign2 独立前后端端口与测试租户真实登录验证。

## 当前状态

已完成。静态构建、依赖锁、类型检查、独立运行态和真实数据 E2E 均已验证通过。后端补齐权威策略源配置后，测试租户真实登录进入统一电子签名页签，`portal/overview` 与 `policies/current` 均达到 `READY/PASS`；DCC/eDHR 正式入口仍跳转到各自模块页面，没有引入 fallback、mock 或额外测试控件。干净融合到 `int_main` 后，真实登录与真实 E2E 再次复验通过。

## Cleanup Keep

- `doc/tasks/20260623-unified-electronic-signature-tab/frontend-feature-evidence.md`
- `doc/tasks/20260623-unified-electronic-signature-tab/real-e2e-evidence.md`
