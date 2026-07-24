# 执行日志：删除排产工单工具栏排产前检查按钮

## 2026-06-26

- 初始化任务：根据用户截图定位 `/mes/pro/scheduleorder` 工具栏中的独立 `排产前检查` 按钮，目标是删除该按钮但保留 `手动重排` 抽屉内真实预检能力。
- 初步排查：`src/views/mes/pro/scheduleorder/index.vue` 当前同时存在 `openPreflightDrawer` 和 `openReplanDrawer` 两个工具栏入口；前者仅负责打开同一抽屉并立即执行 `runPreflight()`，属于可清理的重复入口。
- CHANGE：先更新 `tests/e2e/mes-pro-schedule-order-usability-static.spec.js`，把静态合同从“页面存在排产前检查文案”收紧为“工具栏不得保留独立 `openPreflightDrawer` / 绿色按钮，但抽屉内必须保留 `排产前检查` 标题和 `重新检查`”。
- RED: `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> FAIL，断言 `Schedule order toolbar must not keep the removed standalone preflight entry.` 失败，确认旧工具栏按钮与入口方法仍存在。
- CHANGE：`src/views/mes/pro/scheduleorder/index.vue` 删除工具栏中的独立 `排产前检查` 按钮，并清理不再使用的 `openPreflightDrawer()`；保留 `openReplanDrawer()`、`runPreflight()` 和抽屉内预检区块。
- CHANGE：`tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` 调整断言语义为“未勾选时手动重排入口必须不可点击”，并新增“工具栏不应继续保留独立排产前检查入口方法”。
- GREEN: `node tests/e2e/mes-pro-schedule-order-usability-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS
- GREEN: `node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js` -> PASS
- BLOCKER: `node tests/e2e/mes-scheduling-scope-static.spec.js` -> FAIL，现有源码缺少 `排程日历用于日历/班次视角的局部预览` 等静态语义文案；经核对本次未修改相关文件，属于仓库既有合同/源码不一致，不由本次删按钮引入。
- BLOCKER: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> FAIL，本地 `node_modules` 缺少 `@volar/typescript/lib/quickstart/runTsc`，无法启动 `vue-tsc`，属于工具链依赖前置条件缺失。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-mes-remove-preflight-toolbar-button\frontend-feature-evidence.md` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-mes-remove-preflight-toolbar-button --mode preview` -> READY，默认 keep `task.md` / `execution-log.md`，delete 仅为 `frontend-feature-evidence.md`。
