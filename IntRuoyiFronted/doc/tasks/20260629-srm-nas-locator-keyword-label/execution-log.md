# 执行日志：SRM NAS定位 搜索标签文案改为关键词

- 2026-06-29：创建任务文档，按前端文案微调任务执行严格 TDD。
- BDD: 用户查看 NAS定位 搜索栏时看到统一关键词文案 -> Given 用户进入 `/srm/nas-locator` / When 搜索表单渲染 / Then 标签显示“关键词”，输入框提示显示“请输入关键词”。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> FAIL, 断言缺少页面契约 `关键词`，说明源码仍保留旧文案。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS，页面标签、placeholder 与静态契约已统一为“关键词”。
