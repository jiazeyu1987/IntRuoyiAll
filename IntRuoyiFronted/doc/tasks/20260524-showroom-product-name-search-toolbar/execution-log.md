# 执行日志：简化产品管理筛选栏

BDD: 产品管理只按产品名称搜索 -> Given 用户进入产品管理列表 / When 查看筛选栏 / Then 只能看到产品名称搜索框，查询和重置按钮紧贴搜索框右侧，生命周期、资料状态、审批状态和固定租户输入框不再作为可见筛选控件。

BDD: 产品名称搜索支持模糊查询参数 -> Given 用户在产品名称搜索框输入部分产品名称 / When 点击查询或按回车 / Then 前端提交修剪后的 `keyword` 参数，并清空其他筛选参数，交由后端现有分页接口执行模糊查询。

## TDD 记录

- RED: `node --test scripts\showroom-admin-product-list.test.mjs` -> FAIL，当前工具栏仍显示旧的“搜索产品编码 / 中文名 / 英文名”输入框、固定租户输入和生命周期/资料状态/审批状态筛选，不满足只按产品名称搜索且查询/重置紧贴搜索框右侧的要求。
- GREEN: `node --test scripts\showroom-admin-product-list.test.mjs` -> PASS，产品列表工具栏只暴露产品名称搜索组，查询和重置位于搜索框右侧，隐藏筛选参数提交为空。
- GREEN: `node --test scripts\showroom-admin-product-company-field-layout.test.mjs scripts\showroom-admin-product-owner-fixed-yingtai.test.mjs scripts\showroom-admin-frontend.test.mjs` -> PASS。
- INFO: `pnpm exec eslint src\views\showroom-admin\components\ProductListTable.vue scripts\showroom-admin-product-list.test.mjs` -> FAIL，本地 `node_modules\.bin\eslint.cmd` shim 缺失导致 `eslint` 命令不可识别。
- GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src\views\showroom-admin\components\ProductListTable.vue scripts\showroom-admin-product-list.test.mjs` -> PASS。
- GREEN: Playwright real path `http://127.0.0.1:8081/showroom/product` with `测试租户 / aoteman` -> PASS，页面工具栏实际显示 placeholder `搜索产品名称`，隐藏筛选控件数量 `0`，查询按钮在搜索框右侧，重置按钮在查询按钮右侧。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260524-showroom-product-name-search-toolbar\frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-name-search-toolbar --mode preview` -> READY，计划保留 `task.md` 和 `execution-log.md`，清理 `frontend-feature-evidence.md`。
- CLOSEOUT APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-name-search-toolbar --mode apply` -> BLOCKED，任务状态识别为 `unknown`；已补充英文 `Current Status` 章节后重试。
- CLOSEOUT APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-name-search-toolbar --mode apply` -> APPLIED，已清理 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。
