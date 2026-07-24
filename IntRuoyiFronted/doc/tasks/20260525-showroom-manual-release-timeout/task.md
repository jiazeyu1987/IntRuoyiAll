# 任务：去除手动发布展厅 30 秒超时限制

## 任务目标

- 修复公司工作台点击“手动发布展厅”时报 `timeout of 30000ms exceeded` 的问题。
- 让 `POST /showroom/release/publish` 不继承全局 axios `request_timeout: 30000`。
- 保留真实后端成功/失败结果，不新增 fallback、不吞异常、不默认成功。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260525-showroom-manual-release-result-dialog\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次超时修复。

## BDD 场景

- `BDD: 手动发布展厅不受前端 30 秒限制 -> Given 用户点击手动发布展厅 / When 发布接口耗时超过 30 秒但最终返回 / Then 前端请求不应被全局 30000ms timeout 主动中断`
- `BDD: 手动发布展厅失败仍暴露真实错误 -> Given 后端发布接口返回失败 / When 前端收到失败响应 / Then 页面弹框展示真实错误`

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务已完成。
- [x] M2：补 RED 测试，锁定发布接口必须覆盖全局 30 秒 timeout。
- [x] M3：最小实现去除 `publishRelease` 的 30 秒前端超时。
- [x] M4：运行定向测试和相关回归验证。
- [x] M5：更新任务文档和执行日志，执行 closeout 预览并提交本次改动。

## 预期验证

- `node --test scripts/showroom-admin-manual-release-button.test.mjs`
- `node node_modules\eslint\bin\eslint.js src\api\showroom-admin\index.ts scripts\showroom-admin-manual-release-button.test.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-manual-release-timeout/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-showroom-manual-release-timeout --mode preview`

## 当前状态

状态：已完成

## Current Status

Completed

- 已定位入口：`CompanyWorkbench.vue` 的“手动发布展厅”调用 `ShowroomAdminApi.publishRelease()`。
- 已定位疑似根因：`publishRelease()` 未设置 timeout，默认继承 `src/config/axios/config.ts` 的 `request_timeout: 30000`。
- 已完成最小修复：`publishRelease()` 传入 `timeout: 0`，去除前端 30 秒主动截断。

## Verification Evidence

- RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，缺少 `SHOWROOM_RELEASE_PUBLISH_REQUEST_TIMEOUT = 0`。
- GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node node_modules\eslint\bin\eslint.js src\api\showroom-admin\index.ts scripts\showroom-admin-manual-release-button.test.mjs` -> PASS。
- GREEN: `node --test scripts/showroom-admin-ai-request-timeout.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-manual-release-timeout/bug-regression-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-showroom-manual-release-timeout --mode preview` -> PASS，delete `<none>`、blocked `<none>`。

## Cleanup Keep

- `doc/tasks/20260525-showroom-manual-release-timeout/bug-regression-evidence.md`
