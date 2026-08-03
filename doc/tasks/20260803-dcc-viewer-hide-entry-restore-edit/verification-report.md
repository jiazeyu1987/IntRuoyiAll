# Verification Report

## Scope

- 目标：DCC 受控文件 viewer 只读预览页隐藏截图黄框“受控浏览入口”，并恢复基础信息面板“修改”按钮。
- 非目标：不恢复审批、分发、版本、识别基础信息等 viewer 只读路径按钮；不修改后端接口、路由权限或共享基础信息组件。

## Feature

- DCC viewer 基础信息面板按钮可见性修复。
- 受控浏览元信息区块在 viewer 只读路径隐藏，普通详情路径保留。

## Acceptance

- viewer 右侧基础信息面板显示“修改”按钮，入口仍受 `canEditMetadata && !!fileDetail` 控制。
- viewer 模板不显示黄框“受控浏览入口”区块。
- viewer 模板继续隐藏审批、分发、版本、识别基础信息按钮。
- 普通详情页继续显示受控浏览元信息，不影响业务可读性。

## Bug

- viewer 只读预览页仍显示截图黄框“受控浏览入口”，且上一轮隐藏动作误把“修改”按钮一起隐藏。

## Expected

- 只隐藏黄框“受控浏览入口”区块；恢复“修改”按钮；其它 viewer 只读动作保持隐藏。

## Reproduction

- RED: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> FAIL，viewer 基础信息面板缺少 `:show-edit="canEditMetadata && !!fileDetail"`，且 viewer 模板仍包含“受控浏览入口”区块。

## Root Cause

- viewer 模板的按钮隐藏范围过大，移除了基础信息面板编辑入口；同时 `dcc-detail-controlled-browser-linkage` 元信息区块仍存在于 viewer 分支，导致截图黄框区域继续渲染。

## BDD

- BDD: DCC viewer 区块隐藏并恢复修改 -> Given 用户打开 DCC 受控文件 viewer 只读预览页 When 右侧基础信息展示 Then “受控浏览入口”区块不渲染且“修改”按钮按 `canEditMetadata` 条件恢复显示。

## Result

- PASS: viewer 基础信息面板恢复 `:show-edit="canEditMetadata && !!fileDetail"`、`edit-button-text="修改"`、`edit-test-id="dcc-controlled-preview-detail-edit"`、`@edit="openMetadataDialog"`。
- PASS: viewer 模板不再包含 `data-testid="dcc-detail-controlled-browser-linkage"`、“受控浏览入口”、“查看受控浏览当前有效版”和 `controlled-browser-linkage-grid`。
- PASS: 普通详情页仍保留 `dcc-detail-controlled-browser-linkage`，并继续展示发布文件、盖章文件、当前有效版来源、最终目录路径等业务元信息。
- PASS: 未恢复 viewer 只读路径中的审批、分发、版本、识别基础信息按钮。

## Commands

- GREEN: `node tests\e2e\dcc-controlled-preview-hide-basic-actions-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\dcc-controlled-browser-ux-optimization-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\dcc-view-preview-copy-unification-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue IntRuoyiFronted/tests/e2e/dcc-controlled-preview-hide-basic-actions-static.spec.js IntRuoyiFronted/tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js doc/tasks/20260803-dcc-viewer-hide-entry-restore-edit/task.md doc/tasks/20260803-dcc-viewer-hide-entry-restore-edit/execution-log.md doc/tasks/20260803-dcc-viewer-hide-entry-restore-edit/verification-report.md` -> PASS，仅 CRLF/LF 工作区提示。

## Verification

- 目标静态契约、相邻受控浏览 UX 契约、预览文案统一契约和 TypeScript 检查均已通过。
- `task-closeout-cleanup` preview/apply 均已通过，无删除项。
- `validate_bug_regression.py --evidence doc\tasks\20260803-dcc-viewer-hide-entry-restore-edit\verification-report.md` -> PASS。
- `validate_frontend_feature.py --evidence doc\tasks\20260803-dcc-viewer-hide-entry-restore-edit\verification-report.md` -> PASS。

## Remaining Closeout Blockers

- Blockers:
- Cleanup 已完成：preview/apply 均通过，保留核心任务记录，无删除项。
- 当前工作区存在大量非本任务并行脏改动，且同一 Vue 文件包含既有并行 hunk；提交时必须选择性暂存，不能混入无关改动。
- 远端推送仍受 GitHub 代理/凭据阻塞：HTTPS 代理 `127.0.0.1:7890` 不可用，SSH 443 缺少可用 public key；在恢复前不能满足项目完成门禁。
