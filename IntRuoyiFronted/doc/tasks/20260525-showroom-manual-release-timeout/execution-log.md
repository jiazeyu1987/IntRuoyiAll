# 执行日志：去除手动发布展厅 30 秒超时限制

INFO: 已采用 `bug-regression-fix-loop` 工作流。
INFO: 已确认上一同仓任务 `20260525-showroom-manual-release-result-dialog` 状态为已完成。
INFO: 已定位按钮入口为 `src/views/showroom-admin/company/CompanyWorkbench.vue`，请求入口为 `src/api/showroom-admin/index.ts` 的 `publishRelease()`。

BDD: 手动发布展厅不受前端 30 秒限制 -> Given 用户点击手动发布展厅, When 发布接口耗时超过 30 秒但最终返回, Then 前端请求不应被全局 30000ms timeout 主动中断
BDD: 手动发布展厅失败仍暴露真实错误 -> Given 后端发布接口返回失败, When 前端收到失败响应, Then 页面弹框展示真实错误

RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL, expected reason: `publishRelease()` 未声明 `SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT = 0`，仍继承全局 30000ms timeout。
GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS
GREEN: `node node_modules\eslint\bin\eslint.js src\api\showroom-admin\index.ts scripts\showroom-admin-manual-release-button.test.mjs` -> PASS
GREEN: `node --test scripts/showroom-admin-ai-request-timeout.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs` -> PASS

INFO: 根因确认：`ShowroomAdminApi.publishRelease()` 未覆盖 `src/config/axios/config.ts` 的 `request_timeout: 30000`。
INFO: 最小修复：新增 `SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT = 0` 并仅用于 `POST /showroom/release/publish`。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-manual-release-timeout/bug-regression-evidence.md` -> PASS
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-showroom-manual-release-timeout --mode preview` -> PASS，delete `<none>`、blocked `<none>`。
