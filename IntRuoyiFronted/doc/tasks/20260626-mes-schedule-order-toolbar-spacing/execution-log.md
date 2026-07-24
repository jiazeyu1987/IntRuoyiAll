# 执行日志：20260626-mes-schedule-order-toolbar-spacing

- BDD: 排产工单工具栏分组显示 -> Given 用户打开排产工单列表 / When 页面渲染查询和批量操作按钮 / Then 查询动作与页面级操作动作应按分组展示，不再紧贴成一串按钮。
- BDD: 工具栏在较窄宽度下仍保持可读 -> Given 工具栏可见按钮数量较多 / When 可用宽度收窄导致按钮需要换行 / Then 按钮组应允许换行并保留明确间距，不发生相互挤压。
- RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> FAIL, 当前排产工单页工具栏仍把查询和页面级动作放在同一 `el-form-item` 中，没有独立全宽动作行和分组容器。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js` -> PASS
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- BLOCKER: `pnpm ts:check` -> 仓库存在与本任务无关的全局 Pinia/Store 类型错误，首个报错位于 `src/App.vue`，无法把本轮任务校验提升到全量类型通过。
- BLOCKER: `experience-preflight` -> 官方 `login-preflight.mjs` 两次尝试均未在 60 秒内等到 `/mes/pro/scheduleorder` 目标页关键文本，因此未继续执行自定义 Playwright 登录探针或只读页面截图。
