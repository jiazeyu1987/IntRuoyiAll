# 任务：展厅管理端增加手动发布按钮

## 任务目标

- 在展厅管理端增加“手动发布展厅”按钮，供企宣角色手动触发全局 showroom release 发布。
- 按钮应接到现有后端 `POST /showroom/release/publish`，不新增临时中转接口。
- 交互必须包含明确确认、执行中 loading 和发布成功反馈，避免误触。

## 非目标

- 不修改 `IntRuoyi` 后端发布逻辑。
- 不调整版本中心 `republish` 行为。
- 不增加测试专用按钮、隐藏入口或 mock 发布结果。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-name-search-toolbar\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次展厅管理端发布按钮接入。

## 里程碑

- [x] M1：建立任务记录并补充前端 RED 测试，锁定管理端必须暴露手动发布按钮与接口接线。
- [x] M2：在 `showroom-admin/index.vue` 增加全局发布工具条与交互。
- [x] M3：补充 `showroom-admin` API 发布方法与合同校验。
- [x] M4：运行定向测试、必要静态检查与真实前端路径验证。
- [x] M5：更新任务文档、执行日志并提交本任务改动。

## 预期验证

- `node --test scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\index.vue src\\api\\showroom-admin\\index.ts scripts\\showroom-admin-manual-release-button.test.mjs`
- 如本地前端可用：Playwright 真实路径 `http://localhost:8081/showroom/company`

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已在 `showroom-admin/index.vue` 顶部增加全局工具条，企宣角色可见“手动发布展厅”按钮。
- 已补充前端 API `ShowroomAdminApi.publishRelease()`，直接调用现有 `/showroom/release/publish`。
- 已补充确认弹窗、执行中 loading、成功消息与失败直出逻辑。
- 已保持现有各工作台、版本中心与批量任务逻辑不变。

## Final Verification

- RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，初始状态缺少发布 API 接线与管理端按钮。
- GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules\\.pnpm\\eslint@8.57.1\\node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\index.vue src\\api\\showroom-admin\\index.ts scripts\\showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: Playwright 真实路径 `http://127.0.0.1:8081/showroom/company` 使用测试租户 `aoteman` -> PASS，按钮可见，点击后出现确认框“确认立即发布当前展厅内容吗？”。
- GREEN: Playwright 真实确认发布 -> PASS，前端实际发出 `POST /admin-api/showroom/release/publish`，当前本地测试租户返回真实后端错误 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`，页面已原样提示该错误，未发生静默成功或错误吞没。

## Residual Risk

- 当前本地测试租户的真实发布链路存在后端数据 blocker：`SHOWROOM_TARGET_NOT_FOUND: live product revision not found`。
- 本次前端任务已确保按钮、确认、请求与错误暴露链路正确；未额外修改后端或测试租户数据去绕过该错误。
