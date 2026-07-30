# 20260730 DCC Product Catalog Remove Toolbar Buttons

## Task Goal

Remove the two DCC product catalog toolbar buttons highlighted by the user: `重置` and `注册证有效期`. Keep `新增产品目录`, quick filtering, table columns, row actions, and existing data loading behavior intact.

## Milestones

- [x] M1: Preserve pre-existing dirty workspace changes in baseline commits.
- [x] M2: Record BDD scenario and RED expectation for the toolbar removal.
- [x] M3: Remove the two buttons from the product catalog action toolbar and update static contracts.
- [x] M4: Run focused verification and record evidence.
- [x] M5: Close out with task evidence, cleanup, commits, and push.

## Expected Verification

- RED: `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js` fails before implementation because the existing contract still requires `重置` and `注册证有效期`.
- GREEN: `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js` passes after the UI and contract update.
- REGRESSION: `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` passes with preserved DCC product catalog behavior except the removed toolbar buttons.
- Static sanity: `git diff --check -- <task-owned files>` passes.

## Current Status

completed

## Applicable Gates

- `docs/frontend-development.md`: Frontend changes must preserve existing contracts and use focused RED/GREEN verification.
- `docs/task-closeout-rules.md`: Task documents, verification report, cleanup, commits, and push are required before completion.
- `docs/powershell-memory.md`: Existing dirty workspace changes must be captured separately and task-owned files must be selectively staged.

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除目标 toolbar action rendering and contract expectations.
- `是否存在临时补丁或绕过`：否。

## Baseline Commits

- `d2f2ec65`：baseline dirty worktree before this DCC button task.
- `f9be0387`：baseline residual MES process static contract that appeared after the first baseline.
- `4158334f`：a concurrent/shared-branch baseline captured this task's DCC implementation and task docs together with unrelated non-DCC files before this task could create its own implementation commit.

## Final Verification

- `node tests/e2e/dcc-product-catalog-unified-list-template-static.spec.js` -> PASS.
- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` -> PASS.
- `pnpm ts:check` -> PASS.
- `git diff --check -- <task-owned files>` -> PASS.
- `task_closeout.py --task-id 20260730-dcc-product-catalog-remove-toolbar-buttons --mode preview` -> ready, no delete/block/warnings.
- `task_closeout.py --task-id 20260730-dcc-product-catalog-remove-toolbar-buttons --mode apply` -> applied, no deleted paths.

## Cleanup Keep

- doc/tasks/20260730-dcc-product-catalog-remove-toolbar-buttons/frontend-feature-evidence.md
