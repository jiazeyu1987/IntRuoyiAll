# 执行日志：DCC 文控中心子页签改为四字名称

- 2026-06-29：创建前端任务文档，按菜单文案统一任务执行严格 TDD。
- BDD: DCC 文控中心子页签显示四字标题 -> Given 用户进入系统并展开 `DCC文控中心` / When 子页签渲染 / Then 文控权限之外的目标页签均显示无 `DCC` 前缀且互不重名的 4 字名称。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-subtab-four-char-static.spec.js` -> FAIL，路由与页头文案仍存在旧标题。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-subtab-four-char-static.spec.js` -> PASS，前端静态契约已更新为新标题。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mdm-tenant-package-real-setup-static.spec.js` -> PASS，受控上传/我的文件/受控浏览套餐菜单断言已同步新名称。
