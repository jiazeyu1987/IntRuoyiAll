# Frontend Feature Evidence

## Feature Goal

文控中心文档目录列表中，文件夹图标描边按是否存在子文件夹区分：有子文件夹为绿色，无子文件夹为黑色。

## Non-Goals

- 不修改目录 API、权限、懒加载逻辑、删除流程或访问规则入口。
- 不改变目录表格列结构和原有整行点击展开能力。

## Requirements And Acceptance

- AC-01: 有子文件夹的目录项使用绿色文件夹描边。
- AC-02: 没有子文件夹的目录项使用黑色文件夹描边。
- AC-03: 原有目录懒加载、目录名内联图标、摘要列移除和 TypeScript 编译边界保持通过。

## UI Entry Points And Owned Files

- Entry: `/dcc/controlled-file/directories` 文控中心文档目录页面。
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue`.
- Test: `IntRuoyiFronted/tests/e2e/dcc-directory-folder-border-static.spec.js`.
- Adjacent Test: `IntRuoyiFronted/tests/e2e/dcc-directory-folder-icon-inline-static.spec.js`.
- Script: `IntRuoyiFronted/package.json`.

## API Contracts And Data States

- Uses existing `ControlledFileDirectoryVO.hasChildren` and `ControlledFileDirectoryVO.children`.
- Uses existing `isDirectoryExpandable(row)` as the formal child-folder state.
- No new request, response, route, permission, mock data, fallback, or data mutation.

## BDD Scenarios

- BDD: 目录项边框按子文件夹状态区分 -> Given 文档目录列表展示文件夹项，When 一个文件夹存在子文件夹且另一个不存在子文件夹，Then 存在子文件夹的目录项使用绿色边框，不存在子文件夹的目录项使用黑色边框。

## RED

- `node tests/e2e/dcc-directory-folder-border-static.spec.js` -> FAIL, expected reason: existing directory folder icon wrapper did not bind a row-driven border-color state.

## GREEN

- `node tests/e2e/dcc-directory-folder-border-static.spec.js` -> PASS.
- `pnpm e2e:dcc:directory-folder-border:static` -> PASS.

## Regression Verification

- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS.
- `node tests/e2e/dcc-directory-lazy-loading-static.spec.js` -> PASS.
- `node tests/e2e/dcc-directory-summary-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.

## Responsive Accessibility Loading Empty Error Permission Checks

- Responsive: existing compact inline cell layout remains locked by `dcc-directory-folder-icon-inline-static.spec.js`.
- Accessibility: existing full-cell `role="button"`, keyboard Enter/Space, and disabled state remain unchanged.
- Loading: existing table loading and lazy child loading contracts remain unchanged.
- Empty: no changes to empty directory data handling.
- Error: child load error tag remains beside the directory name.
- Permission: no permission directive changes.

## E2E Or Component Verification Path

- Static component contract and TypeScript verification were used because the requirement is deterministic style behavior driven by existing row data.

## Blockers And Follow-Up Skills

- No implementation blocker.
- Commit/push closeout requires careful staging because the shared workspace currently contains unrelated dirty files and pre-existing ahead commits.
