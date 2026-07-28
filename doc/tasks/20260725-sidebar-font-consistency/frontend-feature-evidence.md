# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 固定侧边栏所有页签字体族与粗细，使 115 浏览器展示与参考图 2 一致。
- Non-goal: 不调整菜单结构、权限、路由、徽标数量、选中态颜色或后端接口。

## Requirements And Acceptance IDs

- `REQ-1`: 侧边栏一级/二级页签文字必须显式使用统一字体族。
- `REQ-2`: 侧边栏一级/二级页签文字必须显式使用统一字重。
- `REQ-3`: 样式修复不得依赖浏览器 fallback 或兼容分支。

## UI Entry Points, Routes, Components, And Owned Files

- UI entry: global left sidebar menu and Element Plus tab labels.
- Components/styles: `src/layout/components/Menu/src/Menu.vue`, `src/styles/var.css`, `src/styles/index.scss`.
- Tests: `tests/e2e/sidebar-tab-font-consistency-static.spec.js`, `tests/e2e/element-plus-tabs-fixed-bold-static.spec.js`.

## API Contracts And Data States

- Not applicable; style-only frontend change.

## BDD Scenarios

- `BDD: Sidebar tab font consistency -> Given the app renders the left sidebar in browsers including 115, When menu tabs and submenu titles are displayed, Then their font family and font weight are explicitly constrained to the approved sidebar style instead of relying on browser fallback rendering.`

## RED Command And Expected Failure

- `RED: node tests/e2e/sidebar-tab-font-consistency-static.spec.js -> FAIL, expected reason: --app-fixed-tab-font-family was not declared before implementation.`

## GREEN Command And Passing Result

- `GREEN: node tests/e2e/sidebar-tab-font-consistency-static.spec.js -> PASS.`
- `GREEN: node tests/e2e/element-plus-tabs-fixed-bold-static.spec.js -> PASS.`
- `GREEN: pnpm ts:check -> PASS.`
- `GREEN: pnpm build:local -> PASS.`

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: menu width, collapse, hover, and active-state layout rules were not changed.
- Accessibility: existing menu text, icon, badge, and aria-label behavior was preserved.
- Loading/empty/error/API: not applicable; no data flow or API behavior changed.
- Permission: not applicable; route filtering and permission store behavior were not changed.

## E2E Or Component Verification Path

- Static contract validates all target style sources; build verification confirms SCSS/CSS compiles.

## Blockers And Follow-Up Skills

- No implementation blocker. Final Git closeout may be blocked by pre-existing unrelated dirty files and branch ahead state.
