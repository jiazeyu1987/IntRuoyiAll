# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 文控中心文档目录表格用文件夹图标替代默认三角视觉，并让图标与目录文字同行。
- Non-goals: 不改后端接口、请求参数、响应 schema、权限、菜单、目录数据或业务操作。

## Requirements And Acceptance IDs

- REQ-1: 目录名称前显示 `ep:folder` 文件夹图标。
- REQ-2: 文件夹图标、目录名称和子目录加载失败标签保持同一行。
- REQ-3: 默认 Element Plus 三角展开图标不再可见。
- REQ-4: 有子目录的目录仍通过 Element Plus 正式 `toggleRowExpansion(row)` 展开/折叠。
- REQ-5: 单击目录名称列红框范围内任意位置都能展开/折叠，不只限文件夹图标。

## UI Entry Points, Routes, Components, And Owned Files

- Entry: 文控中心 > 文档目录。
- Component: `IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue`
- Test: `IntRuoyiFronted/tests/e2e/dcc-directory-folder-icon-inline-static.spec.js`
- Script: `IntRuoyiFronted/package.json`

## API Contracts And Data States

- API contracts unchanged: `getDirectoryChildren`, `searchDirectories`, create/update/delete/access-rule APIs untouched.
- Data states preserved: root directories, lazy child directories, search results, child-load error tag.

## BDD Scenarios

- `BDD: 文档目录文件夹图标同行展示 -> Given 文控中心文档目录存在目录行 When 用户查看目录名称列 Then 每个目录名称前显示文件夹图标且图标与文字同行展示，不显示默认三角图标。`
- `BDD: 文件夹图标保留树形展开入口 -> Given 目录行存在子目录 When 用户点击文件夹图标 Then 使用 Element Plus 表格正式展开能力展开或折叠该目录，不改变懒加载接口和目录数据。`
- `BDD: 目录名称整格点击展开 -> Given 文档目录表格中某一行代表目录 When 用户单击该行目录名称列红框范围内任意位置 Then 该目录按 Element Plus 正式展开能力展开或折叠，而不是只有点击文件夹图标才生效。`

## RED Command And Expected Failure

- RED: `pnpm e2e:dcc:directory-folder-icon-inline:static` -> FAIL，缺少表格 ref、文件夹图标和同行布局契约。

## GREEN Command And Passing Result

- GREEN: `pnpm e2e:dcc:directory-folder-icon-inline:static` -> PASS。
- GREEN: `pnpm e2e:dcc:directory-summary:static` -> PASS。
- GREEN: `pnpm e2e:dcc:directory-lazy-loading:static` -> PASS。

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 目录文本使用 ellipsis-safe inline 元素，避免图标加入后换行。
- Accessibility: 文件夹按钮提供行名相关 `aria-label`；不可展开行禁用展开按钮但仍显示文件夹图标。
- Loading/Error: 懒加载接口与子目录加载失败标签保留。
- Permission: 行操作权限指令未改。

## E2E Or Component Verification Path

- Static verification completed through Node contracts.
- Real browser screenshot not executed because existing adjacent contract and `ts:check` blockers prevented clean closeout.

## Blockers And Follow-Up Skills

- Existing adjacent contract blocker: `dcc-directory-expand-actions-toolbar-static.spec.js` still expects `useTreeTableExpand(true)`.
- Existing typecheck blocker: `ProjectCodeTabPanel.vue` product onboarding methods are missing.
- Git closeout blocker: shared branch is ahead of `origin` with concurrent dirty changes; no safe task-only commit/push performed.
