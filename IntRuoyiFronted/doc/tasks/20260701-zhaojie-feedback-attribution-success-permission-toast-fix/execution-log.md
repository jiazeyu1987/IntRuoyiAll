# Execution Log：待归属确认归属成功后误报无权限（前端）

- `BDD: 归属成功后不再额外刷新正式报工列表 -> Given 用户停留在待归属页并完成确认归属 / When handleAttributionSuccess 执行 / Then 只刷新待归属当前上下文，不立即调用 getList。`
- `BDD: 待归属页当前上下文继续保持 -> Given 页面当前锁定导入批次与筛选条件 / When 归属成功 / Then importQueryParams 不被切到其他筛选，且待归属列表会刷新到最新状态。`
- `BDD: 用户切回正式报工页签仍能看到最新数据 -> Given 归属成功后未立即刷新正式报工列表 / When 用户切回正式报工页签 / Then handleTabChange 仍会调用 getList 获取最新正式报工数据。`
- `GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md、docs\powershell-memory.md、docs\login-access.md、D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md，以及 bug-regression-fix-loop / frontend-feature-delivery 技能与契约。`
- `GREEN: code-triage -> PASS，当前最可疑链路为 src/views/mes/pro/feedback/index.vue 的 handleAttributionSuccess 在待归属成功后同时调用 getImportRecordList 与 getList。`
- `RED: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-attribution-continuation-static.spec.js -> FAIL，断言命中 handleAttributionSuccess 仍包含 await getList()。`
- `CHANGE: D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\feedback\index.vue，移除 handleAttributionSuccess 中与当前待归属页无关的 await getList()。`
- `CHANGE: D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-attribution-continuation-static.spec.js，对齐当前源码锚点并新增“归属成功后不得直接刷新正式报工列表”的静态合同。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-attribution-continuation-static.spec.js -> PASS。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js -> PASS。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js -> PASS。`
- `GREEN: experience-preflight -> PASS，node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username zhaojie --password 111111 --target-path /mes/pro/feedback --target-text 待归属 --timeout 90000 已真实进入目标页。`
- `GREEN: experience-preflight -> PASS，node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username zhaojie --password 111111 --target-path /mes/pro/feedback --target-text 待归属 --timeout 90000 已真实进入目标页。`
