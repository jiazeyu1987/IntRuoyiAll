# Execution Log: 生产报工顶部按钮位置调整

BDD: 顶部独立页签栏被移除 -> Given 用户打开生产报工页面 / When 查看页面顶部 / Then 不再显示截图蓝框中的独立 `ContentWrap` 页签区域。
BDD: 正式报工操作按钮位于筛选行红框位置 -> Given 用户停留在正式报工页签 / When 查看筛选工具栏 / Then `第三方导入`、`导出` 位于 `查询` 与 `重置` 之间的红框空白区。
BDD: 删除绿框按钮 -> Given 用户停留在正式报工页签 / When 查看筛选工具栏 / Then 不再显示绿框内的 `新增` 和 `模拟报工` 按钮。
BDD: 待归属视图保留导入操作 -> Given 页面因导入成功进入待归属视图 / When 查看待归属筛选行 / Then 仍保留第三方导入、模拟报工入口。

GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`、`frontend-feature-delivery` 与 `frontend-contract.md`。

RED: node tests/e2e/mes-feedback-header-action-relocation-static.spec.js -> FAIL，旧页面仍保留独立顶部 `ContentWrap` 和 `feedback-tabs`，按钮未进入正式报工筛选工具栏操作区。

GREEN: node tests/e2e/mes-feedback-header-action-relocation-static.spec.js -> PASS，页面不再保留独立顶部 `ContentWrap` / `el-tabs`，正式报工按钮位于 `UnifiedListTemplate` actions 操作区，待归属筛选行保留第三方导入与模拟报工入口。
GREEN: node --check tests/e2e/mes-feedback-header-action-relocation-static.spec.js -> PASS。
GREEN: node node_modules/eslint/bin/eslint.js src/views/mes/pro/feedback/index.vue -> PASS。

CORRECTION: 用户复核后确认，黄框按钮不是简单进入 actions 末尾，而是必须位于筛选行 `查询` 与 `重置` 之间的红框空白区；已新增 `feedback-filter-action-relocation` 与 `feedback-filter-reset-action` 结构约束。
GREEN: node tests/e2e/mes-feedback-header-action-relocation-static.spec.js -> PASS，静态契约确认 `新增`、`第三方导入`、`模拟报工`、`导出` 位于红框位置独立容器，`重置` 位于其后的独立容器。
GREEN: node --check tests/e2e/mes-feedback-header-action-relocation-static.spec.js -> PASS。
GREEN: node node_modules/eslint/bin/eslint.js src/views/mes/pro/feedback/index.vue -> PASS。

CORRECTION: 用户继续复核后要求删除绿框内的 `新增` 和 `模拟报工` 按钮；已从正式报工筛选行红框容器移除，仅保留 `第三方导入` 与 `导出`，`重置` 继续保留在独立容器。
