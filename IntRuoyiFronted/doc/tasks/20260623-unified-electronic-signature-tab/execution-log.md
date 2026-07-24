# 执行日志：统一电子签名页签前端入口

BDD: 统一电子签名页签展示 DCC/eDHR 摘要 -> Given 当前账号具备统一电子签名页签权限 / When 打开统一电子签名页签 / Then 页面展示 DCC 与 eDHR 的授权状态、签名记录数、待处理数和正式入口。

BDD: 未授权签名必须显式暴露 -> Given 当前账号未开通电子签名授权 / When 页面加载摘要 / Then 模块卡片展示未授权阻塞，不展示为正常可用。

BDD: 正式签名仍回模块执行 -> Given DCC/eDHR 存在待处理签名项 / When 用户点击处理入口 / Then 跳转到模块正式签名页。

BDD: 后续模块按统一协议接入 -> Given Showroom 或 IntAuth 未来接入 / When 后端返回模块接入信息 / Then 统一页签无需复制独立签名中心页面。

GREEN: worktree-setup -> PASS, backend `D:\ProjectPackage\Int\IntRuoyiWorktrees\sign2\ruoyi-vue-pro` and frontend `D:\ProjectPackage\Int\IntRuoyiWorktrees\sign2\yudao-ui-admin-vue3` both created on branch `codex/sign2`.

GREEN: experience-gate-read -> PASS, read `docs/worktree-memory.md`, `docs/login-access.md`, and `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`; high-risk E2E/startup actions still require `experience-preflight` before execution.

RED: `node scripts/signature-governance-page-contract.test.mjs` -> FAIL, expected current page still exposes four separate governance tabs and lacks `portal.ts` / unified `电子签名` tab.

RED: `node tests/e2e/signature-governance-e2e-static.spec.js` -> FAIL, expected real-flow helper does not verify `/signature-governance/portal/overview`.

GREEN: `node scripts/signature-governance-page-contract.test.mjs` -> PASS, 4 tests passed; page now exposes one `电子签名` tab, portal API contract, and route title.

GREEN: `node tests/e2e/signature-governance-e2e-static.spec.js` -> PASS, real-flow helper requires portal overview and formal DCC/eDHR entry route assertions.

BLOCKER: `pnpm install --frozen-lockfile` -> FAIL, `pnpm-lock.yaml` missing dependency entry `adler-32@1.3.1`; frontend `ts:check` cannot run until lockfile is repaired, and this task did not alter dependency lock state.

GREEN: `pnpm install --no-frozen-lockfile` -> PASS, repaired missing `xlsx@0.18.5` transitive dependency lock entries and installed local dependencies for verification.

GREEN: `pnpm install --frozen-lockfile` -> PASS, lockfile no longer blocks reproducible install.

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm run ts:check` -> PASS, Vue relaxed TypeScript check completed successfully.

BLOCKER: task-closeout-cleanup preview -> blocked, linked worktree cannot be fast-forward merged into `int_main` and main frontend worktree is dirty; no cleanup or merge was applied.

BDD: 真实 E2E 验证统一电子签名页签 -> Given sign2 前后端运行在独立端口且当前账号使用测试租户真实登录 / When 打开统一电子签名页并刷新门户摘要 / Then 页面必须返回 portal overview 和 current policy 的真实数据，且不命中主工作区默认端口。

GREEN: experience-preflight -> PASS, sign2 真实 E2E 将使用前端 `http://127.0.0.1:8086`、后端 `http://127.0.0.1:48086`，登录身份为 `测试租户/aoteman/111111`，不访问芋道源码租户。

GREEN: login-preflight -> PASS, `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8086 --tenant 测试租户 --username aoteman --password ****** --target-path /signature-governance --target-text 刷新电子签名`，真实登录已进入目标页。

GREEN: browser-navigation-check -> PASS, 真实浏览器点击统一页签 DCC `签名管理` 跳转到 `/dcc/controlled-file/signatures`，点击 eDHR `签名记录` 跳转到 `/mes/pro/feedback/edhr-signatures`，两个目标页均非 404。

BLOCKER: `node tests/e2e/signature-governance-policy.e2e.js` -> FAIL, `portal/overview` 真实返回 `BLOCKED` 而非 `READY`；页面与接口均显式暴露 `POLICY_SOURCE_MISSING`，说明当前测试租户真实数据缺少 DCC/eDHR 权威策略源。

GREEN: portal-overview-diagnostics -> PASS, 真实浏览器在 `http://127.0.0.1:8086` 登录后抓到 `portal/overview` 与 `policies/current` 两个接口都命中 sign2 后端 `http://127.0.0.1:48086/admin-api/...`，电子签名授权状态为 `ENABLED`，但 DCC/eDHR 仍因 `POLICY_SOURCE_MISSING` 被阻断。

RED: `node tests/e2e/signature-governance-policy.e2e.js` -> FAIL, 后端策略源修复后第一次重跑时，脚本在页面初始 `portalLoading || policyLoading` 期间立即点击 `刷新电子签名`，真实按钮仍处于加载禁用态，测试失败信息为 `Button is disabled: 刷新电子签名`。

GREEN: test-wait-contract -> PASS, 更新 `clickVisibleButton` 等待按钮从加载禁用态恢复可点击；若 30 秒仍禁用仍会 fail fast，不吞错、不强点、不绕过真实 UI。

GREEN: `node tests/e2e/signature-governance-policy.e2e.js` -> PASS, 真实登录测试租户后刷新统一电子签名页签，`portal/overview` 与 `policies/current` 均满足 `READY` 策略断言。

GREEN: login-preflight -> PASS, `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8086 --tenant 测试租户 --username aoteman --password ****** --target-path /signature-governance --target-text 刷新电子签名`，真实登录已进入目标页。

BLOCKER: task-closeout-cleanup preview -> blocked, `delete=<none>`；因主前端 worktree `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 仍为脏状态且 `codex/sign2` 不能快进合并到 `int_main`，本次不执行 cleanup apply、合并或删除 worktree。

GREEN: experience-preflight -> PASS, 用户明确要求先提交前后端代码、融合进 `int_main`、融合后真实 E2E 验证成功再删除 `sign2` worktree；融合前将临时保护主工作区非本任务脏改动，不覆盖、不回滚、不混入本任务提交。

GREEN: merged-result-e2e -> PASS, `int_main` 干净融合后重新启动 `http://127.0.0.1:48081` 与 `http://127.0.0.1:8081`，`node scripts/preflight/login-preflight.mjs ...` 与 `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js` 再次通过，`policy.json` 记录 `status=PASS`。
