# Execution Log：NAS 管理页目录选择与导出占位（前端）

BDD: enter selection mode -> Given NAS 目录已刷新且页面展示目录树 When 用户点击 `选择` Then 目录树进入选择模式并显示可多选的勾选框

BDD: enable export after selecting directories -> Given 页面已经进入选择模式 When 用户选择一个或多个目录 Then `导出` 按钮从禁用态切换为可点击

BDD: export remains placeholder -> Given 用户已经选择目录且 `导出` 按钮激活 When 用户点击 `导出` Then 页面只给出占位提示，不触发新的后端导出请求

RED: `node --test scripts\system-nas-management.test.mjs` -> FAIL，新增 `选择 / 导出 / selectionMode / :show-checkbox=\"selectionMode\" / handleExportSelection` 等断言后，旧页面不满足目录选择模式契约。

GREEN: `node --test scripts\system-nas-management.test.mjs` -> PASS，2 tests green，确认页面已接入选择模式与导出占位按钮。

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。

GREEN: `pnpm exec eslint src/views/system/nas/index.vue scripts/system-nas-management.test.mjs --format stylish` -> PASS。

GREEN: Playwright 真实页面验证 -> PASS，以 `芋道源码 / admin / admin123` 登录 `http://127.0.0.1:8081/system/nas` 后：
- `选择` 按钮可进入选择模式，按钮文案切换为 `取消选择`
- 选择模式下目录树出现 `9` 个勾选框
- 未选择目录时 `导出` 按钮禁用；选中 1 个目录后 `导出` 按钮激活
- 选中 2 个目录后点击 `导出`，页面提示 `已选择 2 个目录，导出功能暂未开放`
- 网络请求仍只有 `nas-config/test` 与 `nas-files`，未新增导出后端请求
