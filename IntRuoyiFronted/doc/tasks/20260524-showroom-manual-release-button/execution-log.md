# 执行日志：展厅管理端增加手动发布按钮

BDD: 企宣角色可在展厅管理端手动发布全局 release -> Given 当前用户具备 `showroom_publicity` 角色并进入任一展厅管理页签 / When 点击“手动发布展厅”并确认 / Then 前端必须调用现有 `/showroom/release/publish` 接口，展示执行中状态，并在成功后回显新的 releaseId

BDD: 非企宣角色不得看到手动发布入口 -> Given 当前用户不具备 `showroom_publicity` 角色 / When 打开展厅管理端 / Then 页面不得渲染“手动发布展厅”按钮

BDD: 手动发布失败必须直接暴露真实错误 -> Given 后端发布接口返回失败 / When 用户确认手动发布 / Then 前端必须直接展示后端错误，不得吞错、静默成功或伪造发布完成

INVESTIGATION: 2026-05-24 -> 已确认真实管理端前端位于 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`，展厅主页面为 `src/views/showroom-admin/index.vue`。
INVESTIGATION: 2026-05-24 -> 已确认后端已存在 `POST /showroom/release/publish`，控制器位于 `ruoyi-vue-pro/yudao-module-showroom/.../ShowroomAdminController.java`，前端当前尚未接入该接口。
RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，当前 `showroom-admin` API 缺少 `publishRelease()`，首页也没有“手动发布展厅”按钮、loading 状态和点击处理器。
GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS，已补齐 `/showroom/release/publish` API 接线，以及首页工具条按钮、loading 和确认交互。
GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src\views\showroom-admin\index.vue src\api\showroom-admin\index.ts scripts\showroom-admin-manual-release-button.test.mjs` -> PASS。
GREEN: Playwright 真实路径 `http://127.0.0.1:8081/showroom/company` + 测试租户 `测试租户 / aoteman / admin123` -> PASS，按钮“手动发布展厅”可见，点击后弹出确认框“确认立即发布当前展厅内容吗？”。
GREEN: Playwright 真实确认发布 -> PASS，页面实际发出 `POST /admin-api/showroom/release/publish`；当前本地测试租户后端返回 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`，前端 toast 原样展示该错误，符合 fail-fast 预期。
