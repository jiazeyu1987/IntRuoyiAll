# Task: Workshop director post filter frontend

## Goal

Wire the MES workshop form so the workshop responsible-user selector only allows choosing users from the `WORKSHOP_DIRECTOR` post, and validate the behavior through the real frontend user path.

## Scope

- Inspect the current workshop form and the shared user selector behavior.
- Replace the unrestricted workshop responsible-user input path with the minimal filtered user-selection flow that preserves the current page style.
- Verify the real frontend path for post-backed workshop responsible-user selection.

## Milestones

- [x] M1: Previous frontend task checked; unresolved prior work was explicitly marked blocked before starting this task.
- [x] M2: This frontend task document was created before production code changes.
- [x] M3: Record BDD scenarios and run RED verification for the workshop responsible-user selector behavior.
- [x] M4: Implement the workshop form UI changes needed to filter candidates to the `WORKSHOP_DIRECTOR` post.
- [x] M5: Validate the live local path for selecting workshop responsible users.
- [x] M6: Run targeted frontend verification and record evidence.
- [x] M7: Update final status and prepare the frontend task commit if verification passes.

## Expected Verification

- The workshop form only presents `WORKSHOP_DIRECTOR` users as responsible-user candidates.
- The selector still supports existing create/edit flows without fallback behavior.
- BDD and RED/GREEN evidence are recorded in `execution-log.md`.
- Frontend evidence is recorded in `frontend-feature-evidence.md`.

## Current Status

Completed for the live frontend user path. On the temporary verification instance at `http://127.0.0.1:8084`, the workshop add dialog limits `负责人` candidates to the two users assigned to the `WORKSHOP_DIRECTOR` post.

## Blocker And Impact

- Blocker: None on the verified frontend path.
- Impact: Backend source-build verification remains tracked in the paired backend task document.

## Final Verification Result

- Targeted frontend lint:
  - `npx eslint src/views/mes/md/workstation/workshop/WorkshopForm.vue`
- Real frontend path:
  - temporary Vite instance `http://127.0.0.1:8084/mes/md/workstation/workshop`
  - the `负责人` selector shows exactly the two users assigned to `WORKSHOP_DIRECTOR`
