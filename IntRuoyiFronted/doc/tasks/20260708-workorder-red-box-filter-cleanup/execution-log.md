# 执行日志：生产工单红框筛选区清理

- BDD: 红框筛选区清理 -> Given 用户打开生产工单列表 / When 查看顶部查询区 / Then 不再显示快速过滤文字标签、工单编号、产品名称、产品编码、需求日期和重复查询/重置按钮。
- BDD: 业务控件保留 -> Given 用户需要执行生产工单业务操作 / When 查看查询区右侧 / Then 导出、增量同步、全部展开、全部折叠和显示字段配置仍可见。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `references/frontend-contract.md`；本轮只做本地前端源码、静态测试和任务文档改动，不执行真实 E2E、高风险写入、发布或服务器操作。
- RED: `node tests\e2e\workorder-red-box-filter-cleanup-static.spec.js` -> FAIL，预期原因：旧页面仍渲染红框内的重复筛选标签、输入控件和查询/重置按钮。
- CHANGE: `src/views/mes/pro/workorder/index.vue` 删除红框内快速过滤文字标签、工单编号、产品名称、产品编码、需求日期显式筛选项，以及重复查询/重置按钮；保留 `TableQuickFilter`、导出、增量同步、展开折叠和显示字段配置。
- GREEN: `node tests\e2e\workorder-red-box-filter-cleanup-static.spec.js` -> PASS，生产工单红框清理静态契约通过。
- GREEN: `node tests\e2e\workorder-key-columns-static.spec.js` -> PASS，生产工单表格关键列回归通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-workorder-red-box-filter-cleanup\frontend-feature-evidence.md` -> PASS，前端证据文档格式通过。
- GREEN: `pnpm ts:check:schedule` -> PASS，排产/生产工单相关 TypeScript 检查通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-workorder-red-box-filter-cleanup --mode preview` -> PASS，无删除项。
- GREEN: commit-isolation -> PASS，`src/components/TableQuickFilter/index.vue` 与 `src/hooks/web/useTableQuickFilter.ts` 均为受跟踪文件且无本次 diff；本次提交只纳入生产工单页查询区改动、红框清理静态契约和任务记录。
