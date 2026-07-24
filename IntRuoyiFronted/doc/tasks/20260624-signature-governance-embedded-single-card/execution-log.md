# 执行日志：电子签名嵌入子页只保留外层卡片

BDD: 嵌入签名子页只保留外层卡片 -> Given 用户进入电子签名下的文件签名、批记录签名或用户授权子路由 / When 页面渲染 embedded 子页面 / Then 子页面内容直接显示在电子签名外层卡片内，不再生成额外 ContentWrap 卡片。

INFO: scope -> 只改前端 embedded 容器结构；不改业务 API、菜单 SQL、权限码。
RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, DCC/eDHR embedded 签名页仍以 ContentWrap 作为根卡片。
RED: node tests\e2e\signature-governance-e2e-static.spec.js -> FAIL, DCC embedded 签名页未使用动态 shell。
GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 7 tests passed。
GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS。
GREEN: npm run ts:check -> PASS。
GREEN: experience-preflight -> PASS, 已读取登录和前端样式门禁；真实 E2E 使用本机 http://localhost:8081 与测试租户 aoteman，只做页面只读验证。
GREEN: Playwright real E2E 登录测试租户并访问 /signature-governance/file-signatures、/batch-signatures、/authorizations -> PASS, nestedContentWraps=0, failedApi=[]。
