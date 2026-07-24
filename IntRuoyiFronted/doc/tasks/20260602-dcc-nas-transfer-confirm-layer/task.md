# 任务：修复 NAS 转移确认框层级

## Task Goal

修复 `NAS 管理 -> 转移到 DCC` 弹窗内点击 `确认转移` 后二次确认框被原弹窗遮挡的问题，确保真实用户可以点击 `确认开始` 并发起 `/dcc/controlled-files/nas-transfer` 请求，从而支撑 `1. QMS documents` 的完整转移、删除、再次转移闭环。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260601-unocss-entry-module-not-found/task.md`
- 状态：`completed`
- 影响：无阻塞；本任务不接管或回滚其他未跟踪任务目录。

## Milestones

- [x] M1: 建立前端任务记录，确认上一前端任务已完成，并记录设计约束。
- [x] M2: 增加确认框层级与离开页面清理转移弹窗的失败优先回归断言。
- [x] M3: 最小化修复 NAS 转移二次确认框层级和页面离开后的旧弹窗残留，保证真实点击可达。
- [ ] M4: 运行静态回归、真实浏览器点击验证和完整 1-7 E2E。
- [ ] M5: 更新任务证据并提交前端仓库改动。

## Expected Verification

- RED：`node scripts/system-nas-management.test.mjs` 在新增断言后失败，指向缺少 NAS 转移确认框专用 modal class。
- GREEN：同一命令通过。
- E2E：真实 Playwright 在 `http://localhost:8081` 打开 NAS 管理页，确认 `确认开始` 可点击并产生转移 POST。
- REGRESSION：完整 NAS QMS 1-7 闭环通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，根因是嵌套弹窗层级错误导致二次确认不可点击；通过显式、专用的 MessageBox modal class 固定交互层级。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

## Completed Work

- 已创建前端任务记录。
- 已确认上一前端任务 `20260601-unocss-entry-module-not-found` 为 `completed`。
- 已通过真实 Playwright 复现：`确认开始` 按钮可见但点击被 `转移到 DCC` 原弹窗 overlay 截获，未发出转移 POST。
- 完整 E2E 暴露第二个前端根因：第一次转移完成弹窗在离开 NAS 页面后仍保留，回到 NAS 页面执行第二次刷新时旧弹窗遮挡页面，导致 `刷新目录` 按钮禁用且第 6 步无法继续。
- 修复旧弹窗后，完整 E2E 暴露第三个前端根因：回到 NAS 管理页时已有配置已加载，但 `刷新目录` 仍只依赖当前页 `testResult`，第 6 步无法直接刷新并选择 `1. QMS documents`。
- 已补充静态回归断言并实现：二次确认框使用专用 modal class 提升层级，离开路由时关闭转移弹窗，已有完整 NAS 配置时允许刷新目录。

## Verification Evidence

- GREEN：`node scripts/system-nas-management.test.mjs` -> PASS，2 tests passed。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-nas-transfer-confirm-layer --mode preview` -> PASS，status ready，无 delete/blocked/warnings。
- E2E FAIL：`node output/playwright/20260602-dcc-nas-transfer-full-loop-after-delete-parallel/verify-full-loop.mjs` -> FAIL，第一轮转移、DCC/NAS 一致性和删除父文件夹均通过；第 6 步返回 NAS 页面时旧 `转移到 DCC` 弹窗仍打开，`刷新目录` 按钮禁用，Playwright 点击超时。
- E2E FAIL：`node output/playwright/20260602-dcc-nas-transfer-full-loop-after-completed-restore-fix/verify-full-loop.mjs` -> FAIL，旧弹窗已关闭；第 6 步返回 NAS 页面后 NAS 配置完整但 `刷新目录` 按钮仍禁用，Playwright 点击超时。

## Remaining Blockers

- 完整 1-7 Playwright E2E 尚未在本次提交前重新跑通，需和后端任务一起从步骤 1 重跑真实用户路径。
- 2026-06-02 阻塞记录：本任务需要与后端 NAS 转移状态一起从真实用户路径重跑完整 1-7 E2E；当前用户转入新的 DCC 下载失败缺陷，继续混合处理会导致前端仓库任务范围污染。本任务先明确阻塞，新缺陷另建任务记录处理。
