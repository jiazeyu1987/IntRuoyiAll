# 执行日志：电子签名页面去除重复页头与内层 Tab

BDD: 子路由直接显示实际内容 -> Given 用户从左侧电子签名子菜单进入任一 /signature-governance/<child> 路由 / When 页面加载 / Then 不显示页面内标题卡和内层 tabs，直接显示当前子页签实际内容。
BDD: 总览刷新只属于总览内容 -> Given 用户进入非总览电子签名子路由 / When 页面加载 / Then 不显示“刷新电子签名”全局页头按钮；进入总览时刷新按钮位于总览内容区。

INFO: scope -> 只改前端电子签名页面结构和契约测试；不改后端 API、菜单 SQL、权限码。
RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, 页面仍包含 <el-tabs>、signature-governance__toolbar 和红框页头文案。
RED: node tests\e2e\signature-governance-e2e-static.spec.js -> FAIL, 页面仍包含 <el-tabs>。
GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 6 tests passed。
GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS。
GREEN: npm run ts:check -> PASS。
GREEN: experience-preflight -> PASS, 已读取登录、前端样式和 worktree 门禁；真实 E2E 使用本机 http://localhost:8081 与测试租户 aoteman，只做页面只读验证。
GREEN: Playwright real E2E 登录测试租户并访问 /signature-governance/overview、/file-signatures、/batch-signatures、/authorizations -> PASS, visibleInnerTabs=0, failedApi=[]，总览保留 1 个内容区刷新按钮，其他子路由刷新按钮为 0。
