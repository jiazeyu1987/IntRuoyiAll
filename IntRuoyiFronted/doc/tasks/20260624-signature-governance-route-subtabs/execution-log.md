# 执行日志：电子签名页签改为路由子页签

INFO: skill -> 使用 `frontend-feature-delivery`；因后端 portal 返回导航路径同步变更，同时使用 `backend-api-delivery`。

INFO: experience-index -> matched `docs/login-access.md`, `docs/server-access.md`, `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。

BDD: 电子签名 tab 作为子页签路由 -> Given 用户打开 /signature-governance/file-signatures / When 页面加载完成 / Then 选中 文件签名记录 子页签并展示对应内容。

BDD: 页签切换更新路径 -> Given 用户在电子签名任一子页签 / When 点击 批记录签名记录 / Then 地址栏变为 /signature-governance/batch-signatures 且保留 executionId 等业务 query。

BDD: 内部入口使用子页签路径 -> Given 用户从个人中心、eDHR 明细或总览卡片进入签名记录 / When 点击入口 / Then 跳转到 /signature-governance/<子页签>，不再使用 /dcc/controlled-file/signatures、/mes/pro/feedback/edhr-signatures 或 ?tab= 主入口。

RED: node scripts/signature-governance-page-contract.test.mjs -> FAIL，缺少 `signatureTabRoutes` 与 `/signature-governance/<tab>` 子页签路由，个人中心/helper 仍含旧入口。

RED: node tests/e2e/signature-governance-e2e-static.spec.js -> FAIL，helper 仍断言 `/signature-governance?tab=` query 主入口。

GREEN: node scripts/signature-governance-page-contract.test.mjs -> PASS，电子签名页已使用 `/signature-governance/<tab>` 子页签路径，个人中心/helper 不再依赖旧入口。

GREEN: node tests/e2e/signature-governance-e2e-static.spec.js -> PASS，真实 E2E helper 的 portal 断言和页签操作均迁移到子页签路径。

GREEN: npm run ts:check -> PASS，使用 `NODE_OPTIONS=--max-old-space-size=8192`。

GREEN: experience-preflight -> PASS，真实 E2E 只访问本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`，只读打开电子签名子页签并检查真实接口响应；不访问远端环境、不写入业务数据、不切换账号或租户。

GREEN: real-e2e -> PASS，真实登录 `测试租户/aoteman/111111`，逐个打开 `总览`、`文件签名记录`、`批记录签名记录`、`用户授权`、`长期留存`、`周期复核`、`CSV质量包`、`统一策略`；地址栏均为 `/signature-governance/<tab>`，portal 返回 DCC `文件签名`，路径为 `/signature-governance/file-signatures` 与 `/signature-governance/authorizations`，EDHR primary path 为 `/signature-governance/batch-signatures`，无失败 `admin-api` 响应、无控制台 error。

GREEN: task-closeout-cleanup --mode preview/apply -> PASS，无临时产物需要删除。
