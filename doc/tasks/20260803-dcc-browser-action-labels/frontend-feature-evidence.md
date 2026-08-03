# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: Rename the DCC controlled browser row action labels to `预览`、`追溯`、`签核`、`下载`.
- Non-goals: No backend API, permission, route, viewer, traceability, download, print, table layout, or data-state changes.

## Requirements And Acceptance

- Acceptance: The operation column template contains the four visible labels in order: `预览`, `追溯`, `签核`, `下载`.
- Acceptance: The old long labels `预览当前有效版`, `查看版本追溯`, and `查看签核证据` are not used as visible row action button text in the browser table.
- Acceptance: Existing click handlers remain `openPreview`, `openDetail`, `openSignatureEvidence`, and `openDownload`.

## UI Entry Points And Owned Files

- Entry point: DCC controlled browser list operation column.
- Owned source: `IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue`.
- Owned test: `IntRuoyiFronted/tests/e2e/dcc-controlled-browser-ux-optimization-static.spec.js`.

## API Contracts And Data States

- No API contract changes.
- No data-state changes.

## BDD Scenarios

- BDD: DCC 受控浏览行操作按钮精简 -> Given 用户进入 DCC 受控浏览列表且某行具备预览、追溯、签核和下载权限 / When 页面渲染该行操作区 / Then 四个按钮依次显示为 `预览`、`追溯`、`签核`、`下载`，并继续调用原有预览、详情追溯、签核证据和下载 handler。

## Verification Plan

- RED: Run the focused static contract after updating expected labels and before implementation.
- GREEN: Run the focused static contract after implementation.
- Regression: Run `pnpm ts:check`, old-label scan, `git diff --check`, and the frontend feature evidence validator.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- Responsive: Label shortening should reduce operation column pressure without changing layout.
- Accessibility: Button text remains visible semantic text.
- Loading: Download loading binding remains unchanged.
- Empty/Error/Permission: No change.

## Blockers And Follow-Up Skills

- None currently.
