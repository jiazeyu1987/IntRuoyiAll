# 执行日志：生产工单工具栏红框入口清理

- BDD: 工具栏红框入口清理 -> Given 用户打开生产工单列表 / When 查看顶部工具栏 / Then 不再显示全部展开、全部折叠和重置列。
- BDD: 业务控件保留 -> Given 用户需要导出、同步或配置字段 / When 查看顶部工具栏 / Then 导出、增量同步、显示字段和保存仍可见。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `references/frontend-contract.md`；本轮只做本地前端源码、静态测试和任务文档改动，不执行真实 E2E、高风险写入、发布或服务器操作。
- RED: `node tests\e2e\workorder-toolbar-red-box-cleanup-static.spec.js; node tests\e2e\workorder-red-box-filter-cleanup-static.spec.js; node tests\e2e\workorder-key-columns-static.spec.js` -> FAIL，预期原因：旧查询区契约仍要求保留 `TreeExpandActions`，与本次删除全部展开/折叠入口的新需求冲突。
- CHANGE: `tests/e2e/workorder-red-box-filter-cleanup-static.spec.js` 调整上一轮筛选区契约，不再要求生产工单工具栏保留 `TreeExpandActions`。
- CHANGE: `src/views/mes/pro/workorder/index.vue` 移除生产工单页 `TreeExpandActions` 调用与导入，通过 `:show-reset="false"` 隐藏本页显示字段组件的 `重置列` 按钮，并保留导出、增量同步、显示字段和保存。
- GREEN: `node tests\e2e\workorder-toolbar-red-box-cleanup-static.spec.js` -> PASS，生产工单工具栏红框入口清理静态契约通过。
- GREEN: `node tests\e2e\workorder-red-box-filter-cleanup-static.spec.js` -> PASS，上一轮查询区红框清理回归通过。
- GREEN: `node tests\e2e\workorder-key-columns-static.spec.js` -> PASS，生产工单关键列回归通过。
- GREEN: `pnpm ts:check:schedule` -> PASS，排产/生产工单相关 TypeScript 检查通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-workorder-toolbar-red-box-cleanup\frontend-feature-evidence.md` -> PASS，前端证据文档格式通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-workorder-toolbar-red-box-cleanup --mode preview` -> PASS，无删除项。
