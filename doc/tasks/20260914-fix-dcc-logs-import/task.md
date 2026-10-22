# DCC logs import repair

## Goal
Restore the missing controlled-file logs view so Vite can compile the router.

## Milestones
1. Reproduce missing view and source ignore failure.
2. Restore the existing implementation and protect source tracking.
3. Verify route imports and Vite transforms; review task assets.

## Expected verification
Node regression test for route view existence and Git ignore rules; live Vite module transforms.

## BDD
Given the int_qms checkout, when Vite resolves static route imports, then every referenced Vue view exists, including controlled-file/logs/index.vue.
Given a source directory named logs, when Git evaluates ignores, then its Vue source is not ignored while runtime logs remain ignored.

## Design constraints check
Restore repository implementation; preserve route/API contracts. No placeholder, overlay suppression, database changes, E2E, commits or pushes. User instructions prohibit commits/pushes without current-turn authorization and override the closeout document.

## Status
ready_for_closeout

All implementation and verification milestones passed. The task-closeout-cleanup tool/script is not available in this checkout or the current skill catalog; automated preview/apply cannot run. Manual asset review found no task-owned temporary artifacts. No commit/push authorization was given.
