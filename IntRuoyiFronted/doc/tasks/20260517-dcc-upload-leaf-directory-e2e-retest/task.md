# Task: DCC 上传叶子目录 E2E 复测

## Goal

使用真实前端入口、真实登录态与真实 PDF 文件，对 DCC 受控文件上传页执行一次完整 E2E 上传复测，确认叶子目录选择后的提交路径可成功走通。

## Scope

- 先确认上一条相关前端任务已完成，再创建本任务记录。
- 使用 `http://127.0.0.1:8081` 真实页面执行一次 DCC 上传。
- 使用真实后端 `http://127.0.0.1:48081`、真实登录与真实 PDF 文件。
- 仅做验证，不改生产代码；若发现失败则记录真实阻塞。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-upload-leaf-directory-selection/task.md`
- Status before this task: completed.
- Impact: the leaf-directory upload feature is already delivered, so this task can focus on real E2E verification only.

## BDD

BDD: 上传页选择叶子目录后可成功提交 -> Given 用户打开 DCC 受控文件上传页并选择一个绑定到多层目录树的文件类别 / When 用户上传真实 PDF、选择最后一层叶子目录并提交 / Then 系统应成功提交审批并跳转到我的文件页。

## Milestones

- [x] M1: 创建任务文档与执行日志。
- [x] M2: 编写最小 Playwright 验证脚本并执行真实上传。
- [x] M3: 记录 GREEN 结果与阻塞情况。

## Expected Verification

- 真实 Playwright 路径验证 `http://127.0.0.1:8081/dcc/controlled-file/upload`

## Current Status

Completed.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-leaf-directory-e2e-retest run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-leaf-directory-e2e-retest\scripts\verify-dcc-upload-leaf-directory-e2e-retest.mjs` -> PASS
- 真实上传结果：
  - 文件类别：`图纸`
  - 绑定路径：`3.DMR/01.图纸`
  - 叶子目录：`01成品图纸/00- 作废图纸_成品`
  - 提交结果：成功跳转到“我的文件”，出现 `受控文件已提交审批`
  - 提交 payload：`directoryId=4`

## Residual Risk

- 本次成功提交期间页面同时弹出两条 `系统内部错误` toast。
- 浏览器控制台记录了多次 `@vite/client` `ReferenceError: document is not defined`，更像本地 Vite HMR 客户端噪声，而不是本次上传接口失败。
- 上传接口最终返回 `code=0`，且“我的文件”页已能看到本次记录，因此本次 E2E 复测判定为通过，但该 dev-server 噪声建议后续单独排查。
