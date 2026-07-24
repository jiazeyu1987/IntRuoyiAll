# Task: 前端批记录页面切到纯预览链路

## Goal

在新的前端 worktree 中，把实际运行的电子批记录页面链路切到当前 backend
worktree 已经提供的 `/jmreport/view/...` 预览路径，并让页面文案能正确反映
“预览”而不是继续误称为“设计器”。

## Scope

- 新增一个前端 worktree 专用运行模式，指向 `http://127.0.0.1:48082`
- 调整 `电子批记录` 相关页的提示文案，使其能根据后端返回的 path 区分
  “预览” 和 “设计器”
- 使用真实前端 dev server 和真实用户路径验证 iframe 已切到 `/jmreport/view/...`

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-workorder-status-column-and-kingdee-confirmed/task.md`
- Status before this task: completed
- Impact: no unfinished latest frontend task blocks this preview-chain integration

## Milestones

- [x] M1: 建立任务包并新增 worktree 专用前端运行模式
- [x] M2: 调整批记录页面文案与预览提示
- [x] M3: 启动前端 worktree dev server 并连到 backend `48082`
- [x] M4: 用真实页面验证 iframe 已切到 `/jmreport/view/...`
- [x] M5: 更新任务证据并收口

## Expected Verification

- `node tests/e2e/batch-record-preview-chain.spec.js`
- `npm exec eslint src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue src/views/report/jmreport/index.vue tests/e2e/batch-record-preview-chain.spec.js`
- 真实前端 worktree dev server 启动成功
- Playwright/真实页面验证 `batch-record-template` 页面 iframe `src` 指向 `http://127.0.0.1:48082/jmreport/view/...`

## Current Status

Completed. The frontend worktree now exposes a dedicated `batch-record-preview` mode, and the real page on `8082` has been verified to load the batch-record iframe from `http://127.0.0.1:48082/jmreport/view/...`.

## Residual Risk

- Repo-wide `npm run ts:check` still fails on many pre-existing missing auto-import globals unrelated to this task; this task did not attempt to repair that baseline.
