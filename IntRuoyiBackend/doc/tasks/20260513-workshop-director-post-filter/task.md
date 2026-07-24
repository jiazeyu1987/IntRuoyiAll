# Task: Workshop director post filter backend

## Goal

Create the `WORKSHOP_DIRECTOR` post in the live local environment, assign users `wuxiaolei` and `guliya` to that post, and prepare backend source support so workshop responsible-user candidates can be constrained to that post.

## Scope

- Inspect current system post, user, and MES workshop contracts.
- Add the minimal backend API source support needed to filter candidate workshop responsible users by post without fallback behavior.
- Apply the live local data change for the new post and the two user-post assignments.
- Verify the live data through authenticated local admin APIs.

## Milestones

- [x] M1: Previous backend task checked; unresolved prior work was explicitly marked blocked before starting this task.
- [x] M2: This backend task document was created before production code changes.
- [x] M3: Record BDD scenarios and run RED verification for workshop responsible-user filtering.
- [x] M4: Implement the minimal backend contract in workspace source for filtering workshop responsible-user candidates by the `WORKSHOP_DIRECTOR` post.
- [x] M5: Create the `WORKSHOP_DIRECTOR` post and assign `wuxiaolei` and `guliya` in the live local environment.
- [x] M6: Run targeted backend source verification and real-flow validation with the frontend.
- [x] M7: Update evidence, mark final status, and prepare the backend task commit if verification passes.

## Expected Verification

- `WORKSHOP_DIRECTOR` exists as an enabled system post.
- Users `wuxiaolei` and `guliya` are assigned to the `WORKSHOP_DIRECTOR` post.
- The intended backend candidate-user contract only returns enabled users belonging to the `WORKSHOP_DIRECTOR` post.
- BDD and RED/GREEN evidence are recorded in `execution-log.md`.
- Backend evidence is recorded in `backend-api-evidence.md`.

## Current Status

Completed. The backend candidate-user endpoint is implemented in source, the live local `WORKSHOP_DIRECTOR` data is provisioned, and the targeted MES controller regression test now passes in the current workspace.

## Blocker And Impact

- Blocker: None currently discovered.
- Impact: Frontend and backend now share the same `WORKSHOP_DIRECTOR` filtering contract, so the workshop responsible-user selector fails fast when the post or assignments are missing instead of widening the candidate list.

## Final Verification Result

- Targeted backend regression:
  - `mvn -pl yudao-module-mes -am -Dtest=MesMdWorkshopControllerChargeUserListTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- Live local admin API verification:
  - `WORKSHOP_DIRECTOR` post exists with id `8`
  - `wuxiaolei` and `guliya` both return `postIds: [8]`
- Frontend real-path verification is recorded in the paired frontend task document under `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-workshop-director-post-filter\`.
