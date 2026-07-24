# 执行日志：展厅手动发布结果改为弹框提示

BDD: 手动发布成功时弹框显示成功信息 -> Given 用户确认手动发布展厅且后端返回成功 / When 前端处理接口响应 / Then 页面必须弹框显示发布成功和 releaseId

BDD: 手动发布失败时弹框显示失败原因 -> Given 用户确认手动发布展厅且后端返回失败 / When 前端处理接口异常 / Then 页面必须弹框显示真实失败原因

BDD: 结果提示改为弹框后仍保留确认弹窗与真实接口调用 -> Given 用户点击手动发布展厅 / When 用户确认执行 / Then 前端仍必须先走确认框，再调用 `/showroom/release/publish`

RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，`handlePublishShowroomRelease()` 仍使用 `message.success(...)` 与 `message.error(...)` 进行 toast 提示。
GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS，成功路径已改为 `message.alertSuccess(...)`，失败路径已改为 `message.alertError(...)`。
GREEN: `node node_modules\\.pnpm\\eslint@8.57.1\\node_modules\\eslint\\bin\\eslint.js src\\views\\showroom-admin\\company\\CompanyWorkbench.vue scripts\\showroom-admin-manual-release-button.test.mjs` -> PASS。
