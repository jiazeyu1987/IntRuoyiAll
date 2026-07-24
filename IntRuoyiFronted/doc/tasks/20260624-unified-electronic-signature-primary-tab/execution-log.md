# 执行日志：统一电子签名一级页签前端嵌入

INFO: skill -> 使用 `worktree`、`frontend-feature-delivery`，并读取前端证据契约。

INFO: experience-index -> matched `docs/worktree-memory.md`, `docs/login-access.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

GREEN: experience-preflight -> PASS，已确认使用独立 worktree `D:\ProjectPackage\Int\IntRuoyiWorktrees\signature-primary`，分支 `codex/unified-signature-primary-tab`，规划端口 `8089/48089`，真实 E2E 使用本机测试租户 `测试租户/aoteman/111111`；禁止 fallback、mock、静默跳过和跨环境切换。

BDD: 统一一级页签展示全部电子签名内容 -> Given 用户打开 /signature-governance / When 页面加载完成 / Then 看到总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略子页签。

BDD: DCC/eDHR 旧入口不再暴露 -> Given 用户访问电子签名一级页签 / When 查看页面文案和模块卡片 / Then 不出现 DCC电子签名 或 eDHR电子签名 独立入口式文案。

BDD: query 参数直达子页签 -> Given URL 为 /signature-governance?tab=batch-signatures&executionId=<id> / When 页面加载 / Then 自动选中批记录签名记录并按 executionId 过滤。

BDD: 旧业务能力嵌入统一页 -> Given 用户有文件签名或批记录签名权限 / When 打开对应子页签 / Then 页面调用原真实 API 展示记录、授权和错误状态，不使用 mock 数据。

RED: node scripts/signature-governance-page-contract.test.mjs -> FAIL，统一页仍只有 `电子签名/electronic-signature` 单页签，缺少 `route.query.tab`，并仍出现 `DCC 电子签名 / eDHR 电子签名` 独立入口式文案。

RED: node tests/e2e/signature-governance-e2e-static.spec.js -> FAIL，真实 E2E helper 仍断言 `/dcc/controlled-file/signatures`、`/mes/pro/feedback/edhr-signatures` 旧主入口。

GREEN: node scripts/signature-governance-page-contract.test.mjs -> PASS，统一页签、query tab、嵌入组件、个人中心/helper 旧路径禁用契约通过。

GREEN: node tests/e2e/signature-governance-e2e-static.spec.js -> PASS，真实 E2E helper 改为统一 tab query 且不再依赖旧签名路径。

BLOCKER: npm run ts:check -> FAIL，worktree 初始缺少 `node_modules/vue-tsc`，无法执行类型检查。

GREEN: pnpm install -> PASS，按项目锁文件补齐 worktree 本地依赖。

BLOCKER: npm run ts:check -> FAIL，Node 默认 4GB heap 在 relaxed ts check 中 OOM。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check -> PASS。

GREEN: post-merge real Playwright E2E -> PASS，真实登录 `测试租户/aoteman/111111`，请求 `tenant-id=122`；打开 `/signature-governance` 后依次进入 `总览`、`文件签名记录`、`批记录签名记录`、`用户授权`、`统一策略`，相关真实接口均 HTTP 200 且业务 `code=0`，页面无可见错误。
