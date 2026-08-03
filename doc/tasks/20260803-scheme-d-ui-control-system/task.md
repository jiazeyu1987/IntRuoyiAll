# 20260803 Scheme D UI Control System

## Task Goal

Unify the current frontend UI control treatment using the approved Scheme D visual hierarchy, starting with the child pages under the "基础数据" menu group shown by the user.

Initial implementation scope:

- "基础数据" child pages and nested pages visible in the sidebar screenshot.
- Page actions, list toolbar buttons, query controls, dialog footer buttons, inline table actions, danger actions, status tags, navigation controls, form controls, and feedback controls where those controls exist in the scoped pages.
- Preserve existing routes, API contracts, permissions, business behavior, and error propagation.

Out of scope for this first phase:

- Backend behavior or data contract changes.
- Other top-level menu groups such as CRM 系统 and AI 大模型.
- Full project-wide UI conversion outside the "基础数据" scope.

## Milestones

1. Create task docs, record BDD/TDD plan, and capture dirty-worktree baseline evidence.
2. Locate "基础数据" child routes/pages and existing shared component/style entry points.
3. Add focused RED static contract for Scheme D control classes/tokens in scoped pages.
4. Implement minimal shared style/helpers and apply to scoped "基础数据" pages.
5. Run GREEN verification and a scoped regression check.
6. Update evidence and prepare closeout status.

## Expected Verification

- RED: Run the focused static contract before implementation and confirm it fails because Scheme D scoped control styling is absent.
- GREEN: Re-run the focused static contract and confirm it passes.
- Regression: Run a targeted frontend static/type check where feasible; if an existing unrelated blocker appears, record it with exact failure and impact.
- Manual source review: Confirm scoped pages use Scheme D classes without changing API calls, permissions, or route contracts.

## Current Status

completed

Implementation, scoped verification, cleanup, selective task commits, and remote push are complete for the first 基础数据 phase. The earlier GitHub push blocker was cleared by retrying `git push origin int_main`, which successfully updated `origin/int_main` to `6b1fd19eb`.

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。样式统一不得隐藏、吞掉或默认成功任何前后端错误。
- 是否从根因和长期维护角度解决：是。优先通过共享样式/类名和任务专用静态契约约束，而不是逐页临时覆盖。
- 是否存在临时补丁或绕过：否。若发现基础数据页缺少统一组件入口，将先记录阻塞或补正式共享入口。

## Applicable Gates

- Frontend development rules: keep existing Vue/TypeScript/Element Plus patterns, use pnpm, preserve permissions and API wrappers.
- Frontend static contract isolation: use a focused static contract if broad checks fail on unrelated historical issues.
- Unified frontend style: follow the Int operations-console style with compact blue/neutral surfaces and Scheme D approved action hierarchy.
- No fallback policy: no silent downgrade, mock success, compatibility shim, or swallowed errors.

## Baseline Commits Before This Task

- 2c64b8cb4 chore: baseline dirty worktree before ui control styling
- 114c6b039 chore: baseline residual pqc test changes
- 15331a1bb chore: baseline residual uncontrolled import migration
- d1281a93c chore: baseline residual pqc event source changes
- c8a38a39e chore: baseline residual nas import task metadata
- efecae435 chore: baseline concurrent task docs before scheme d ui work
- 6073d6e4 chore: baseline dirty workspace before controlled print preview fix

## Task Commits

- 740149060 feat: finish scheme d controls for basic data pages
- 1cdfd5b71 chore: close scheme d ui control task
- fe25101f1 chore: record scheme d push blocker

## Implementation Summary

- Added scoped Scheme D control tokens and classes through `src/styles/scheme-d-controls.scss` and global style import.
- Applied scoped classes to 基础数据 pages: 展厅主数据, DCC项目代码, DCC产品目录, DCC文件分类 and 表单模板.
- Standardized toolbar buttons, dialog footer buttons, row actions, dangerous actions, status tags, form-control shells, feedback states and FormCenter back controls.
- Converted FormCenter route/workspace back controls to icon-style arrow buttons using `ep:arrow-left`.
- Preserved existing routes, permission directives, API calls, status guards and error propagation.

## Verification Summary

- RED: `node tests/e2e/basic-data-scheme-d-controls-static.spec.js` -> FAIL, expected reason: missing `src/styles/scheme-d-controls.scss`.
- GREEN: `pnpm e2e:basic-data:scheme-d-controls:static` -> PASS.
- GREEN: `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/dcc-file-type-taxonomy-basic-data-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/form-center-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: `git diff --check -- <task-owned frontend files>` -> PASS.
- BLOCKED unrelated regression precondition: `node tests/e2e/dcc-basic-data-global-submenu-static.spec.js` and `node tests/e2e/dcc-project-code-basic-data-static.spec.js` fail before assertions because `E:\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260513_dcc_base_schema.sql` is missing in the current workspace.
