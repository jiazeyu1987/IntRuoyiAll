# Task: 前端隐藏 JMReport 预览工具条

## Goal

在前端 worktree 中继续收口电子批记录预览页的最后一个视觉阻塞：
- 真实页面已经切到 `/jmreport/view/...`
- 但 JMReport viewer 自带顶部工具条仍然存在

本任务目标是在不改后端报表 JSON 结构的前提下，通过前端通用方案把预览页收敛成更接近纸质单据的纯净视图。

## Scope

- 使用 4 个子 agent 并行处理下列四个方向：
  - 同源代理链路
  - iframe 预览注入/裁切
  - 预览页接线
  - 浏览器级验证与测试接线
- 保持通用方案，不允许按具体模板名写分支
- 优先复用现有 `/jmreport/view/...` 预览链路
- 若缺少同源注入前提，必须直接报告阻塞，不做 fallback

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260517-frontend-batch-record-preview-chain/task.md`
- Status before this task: completed
- Impact: 上一步已经把真实前端链路切到 worktree backend 的 `/jmreport/view/...`，本任务只继续处理 viewer 自带 chrome

## Milestones

- [x] M1: 建立任务包并完成 4 个子 agent 分工
- [x] M2: 落地 `/jmreport` 同源代理与预览 URL 接线
- [x] M3: 落地 iframe 内 viewer 顶栏抑制方案
- [x] M4: 完成 RED/GREEN 测试、真实页面验证与证据更新
- [x] M5: 收口提交

## Expected Verification

- `node tests/e2e/batch-record-preview-toolbar.spec.js`
- `npm exec eslint src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue src/components/IFrame/src/IFrame.vue vite.config.ts tests/e2e/batch-record-preview-toolbar.spec.js`
- `http://127.0.0.1:8082/mes/pro/batch-record-template?mode=designer&reportId=<id>` 真实页面验证
- iframe `src` 应切到同源 `/jmreport/view/...`
- viewer 顶部工具条在真实页面中不可见

## Current Status

Completed. 代码、测试、真实页面验证和收尾清理都已完成。

## Verification Summary

- `node tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS
- `node tests/e2e/batch-record-preview-chain.spec.js` -> PASS
- `npm exec eslint src/views/mes/pro/batchrecordtemplate/DesignerWrapper.vue src/components/IFrame/src/IFrame.vue vite.config.ts tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename doc/tasks/20260517-frontend-jmreport-viewer-chrome-suppression/scripts/verify-batch-record-preview-toolbar.mjs` -> PASS

## Outcome

- `batch-record-preview` 模式下，前端 `8082` 现在会同源代理 `/jmreport` 到 backend `48082`
- 电子批记录预览页在预览模式下会把 iframe 切到同源 `/jmreport/view/...`
- 同源 iframe 内会按稳定 vendor 结构 `#jm-sheet-wrapper .ty-bar-container` 抑制 viewer 顶栏，并同步关闭 `rpViewInst.rpBar`
- 若缺少同源代理前提，页面会明确报错，不会静默回退到原始带工具条 viewer

## Cleanup Notes

- `task-closeout-cleanup` 预览已执行
- 预览阻塞点：当前 linked worktree 没有找到已 checkout 的 `master` 主 worktree，无法自动完成 worktree closeout
- 已按预览结果手动清理 task 临时 artifacts、一次性探针脚本和 `frontend-feature-evidence.md`
