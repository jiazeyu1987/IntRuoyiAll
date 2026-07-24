# Execution Log：待归属页归属写入口权限收口

- `BDD: 缺少 update 权限时待归属页不暴露写入口 -> Given 当前登录用户只有 mes:pro-feedback:query / When 打开待归属页 / Then 页面不显示选择归属、修改归属、确认报工按钮，也不提供草稿补填输入框。`
- `BDD: 程序化触发写入口时前端显式 fail-fast -> Given 用户通过代码路径直接调用归属弹窗或整批确认方法 / When 当前权限缺少 mes:pro-feedback:update / Then 前端立即提示缺少生产报工更新权限，不继续请求后端。`
- `BDD: 有 update 权限时待归属原有闭环保持可用 -> Given 当前登录用户拥有 mes:pro-feedback:update / When 打开待归属页并执行归属或整批确认 / Then 原有补填字段、归属弹窗和确认报工闭环保持不变。`
- `GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md、docs\powershell-memory.md，并确认本次只做前端权限入口收口，不做无关视觉改动。`
- `RED: git show HEAD:src/views/mes/pro/feedback/index.vue -> FAIL，旧版页面不包含 `checkPermi(['mes:pro-feedback:update'])`、`canUpdateImportRecord`、待归属按钮 `v-hasPermi="['mes:pro-feedback:update']"`，也没有“缺少生产报工更新权限” fail-fast 文案。`
- `CHANGE: src/views/mes/pro/feedback/index.vue`，新增 `checkPermi`、`canUpdateImportRecord`、按钮级 `v-hasPermi` 与 `openAttribution/handleConfirmBatch` fail-fast 保护，同时把草稿补填输入统一改为 `isImportRecordEditable`。`
- `CHANGE: tests/e2e/mes-feedback-permission-static.spec.js`，补待归属写入口与 fail-fast 文案的静态权限合同。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-permission-static.spec.js -> PASS。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-tracking-static.spec.js -> PASS。`
