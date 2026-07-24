# 任务：简化产品管理筛选栏

## 任务目标

- 将 `showroom/product` 产品管理列表筛选栏简化为只按产品名称输入关键词搜索。
- 搜索支持模糊查询，查询按钮和重置按钮紧贴搜索框右侧。
- 保持新增、导入、导出和批量操作等页面级按钮可用，但不再把租户、生命周期、资料状态、审批状态作为可见筛选控件。

## 非目标

- 不修改后端接口合同。
- 不改产品列表字段、分页、批量任务和审批流程。
- 不引入前端本地过滤、mock 数据或静默降级。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-live-narration-empty-fix\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次前端筛选栏调整。

## 里程碑

- [x] M1：建立任务记录并补充产品名称搜索栏 BDD / RED 测试。
- [x] M2：最小修改 `ProductListTable.vue` 的筛选栏布局和可见控件。
- [x] M3：运行定向测试、组件静态检查与证据校验。
- [x] M4：完成任务文档、执行日志和本任务提交。

## 预期验证

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260524-showroom-product-name-search-toolbar/frontend-feature-evidence.md`

## 当前状态

- 状态：已完成
- Status: Completed

## Current Status

Completed

## Completed Work

- 已补充产品列表组件测试，锁定筛选栏只能显示产品名称搜索框，查询和重置按钮必须位于搜索框右侧。
- 已修改 `ProductListTable.vue`：
  - 筛选栏只保留“搜索产品名称”输入框。
  - 查询与重置按钮放入搜索框右侧的同组操作区。
  - 固定租户输入、生命周期、资料状态、审批状态筛选从可见工具栏移除。
  - 搜索提交时只保留修剪后的 `keyword`，隐藏筛选参数提交为空。

## Verification Evidence

- RED: `node --test scripts\showroom-admin-product-list.test.mjs` -> FAIL，当前工具栏仍显示旧的“搜索产品编码 / 中文名 / 英文名”输入框、固定租户输入和生命周期/资料状态/审批状态筛选。
- GREEN: `node --test scripts\showroom-admin-product-list.test.mjs` -> PASS。
- GREEN: `node --test scripts\showroom-admin-product-company-field-layout.test.mjs scripts\showroom-admin-product-owner-fixed-yingtai.test.mjs scripts\showroom-admin-frontend.test.mjs` -> PASS。
- GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src\views\showroom-admin\components\ProductListTable.vue scripts\showroom-admin-product-list.test.mjs` -> PASS。
- GREEN: Playwright real path `http://127.0.0.1:8081/showroom/product` with `测试租户 / aoteman` -> PASS，placeholder 为“搜索产品名称”，工具栏隐藏筛选控件数量为 0，查询按钮位于搜索框右侧，重置按钮位于查询按钮右侧。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260524-showroom-product-name-search-toolbar\frontend-feature-evidence.md` -> PASS。
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-name-search-toolbar --mode preview` -> READY，计划保留 `task.md` 和 `execution-log.md`，清理 `frontend-feature-evidence.md`。
- CLOSEOUT APPLY: 前两次 apply 因任务状态识别为 `unknown` 阻塞；已补充英文 `Current Status` 章节供清理脚本识别。
- CLOSEOUT APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-name-search-toolbar --mode apply` -> APPLIED，已清理 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。
