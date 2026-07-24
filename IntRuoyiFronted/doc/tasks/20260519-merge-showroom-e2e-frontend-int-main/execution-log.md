# Execution Log: Merge Showroom E2E Frontend Worktree Into int_main

## BDD Scenarios

BDD: Showroom routes available from int_main -> Given the Showroom E2E frontend worktree has completed, When the branch is merged into `int_main`, Then `/showroom-admin/company` and `/showroom/display/home` route definitions are present in the main frontend workspace.

BDD: Existing dirty work is preserved -> Given the main frontend workspace has unrelated local modifications, When the Showroom branch is merged, Then those files are not staged, committed, overwritten, or reverted by this task.

## TDD Evidence

- RED: Before merge, `int_main` did not contain `src/router/modules/showroom-admin.ts` or `src/router/modules/showroom-frontstage.ts`; the user-visible result was 404 for `/showroom-admin/company` and `/showroom/display/home`.
- GREEN: `git merge --ff-only codex/showroom-phase1-e2e-cases` -> PASS, `int_main` advanced from `56462f41` to `3f966df3`.
- GREEN: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs` -> PASS, 12 tests passed.
- GREEN: `node scripts/run-showroom-phase1-e2e.mjs --dry-run` -> PASS, all three Phase 1 E2E case modules load with their required env lists.
- REGRESSION: `rg -n "showroomAdminRoutes|showroomFrontstageRoutes|/showroom-admin|/showroom/display" src\router\modules\remaining.ts src\router\modules\showroom-admin.ts src\router\modules\showroom-frontstage.ts` -> PASS.
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-merge-showroom-e2e-frontend-int-main --mode preview` -> PASS, no delete candidates and no blockers.

## Verification Evidence

- Fast-forward merge applied 16 Showroom files from `codex/showroom-phase1-e2e-cases`.
- Existing modified files `src/api/mes/md/item/productBom/index.ts`, `src/views/mes/md/item/MdItemForm.vue`, `src/views/mes/md/item/MdProductBomForm.vue`, and `src/views/mes/pro/route/RouteForm.vue` were not part of the merge diff and were not staged.

## Blockers

- Real browser E2E remains separately blocked by missing real E2E accounts and asset prerequisites.
