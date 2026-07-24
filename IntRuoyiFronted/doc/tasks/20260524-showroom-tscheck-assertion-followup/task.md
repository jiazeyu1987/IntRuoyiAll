# 20260524 Showroom Tscheck Assertion Follow-up

## Goal

Tighten the two remaining showroom frontend type assertions so `pnpm ts:check` stays green without changing runtime behavior.

## Milestones

- [x] Check previous frontend task status and current repository state.
- [x] Record BDD and RED evidence for the lingering TypeScript assertion failures.
- [x] Apply the minimal assertion narrowing in the affected showroom files.
- [x] Re-run frontend verification and record the result.
- [x] Commit only current-task changes.

## Expected Verification

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## Current Status

Completed. The remaining showroom assertion mismatches were narrowed in place, and full frontend `ts:check` is green again.

## Verification Evidence

- RED evidence came from the prior runtime-control verification pass: `pnpm ts:check` previously failed on `src/views/showroom-admin/narration/NarrationWorkspace.vue` and `src/views/showroom-frontstage/shared/payload.ts` because direct assertions from unknown payloads were rejected.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## Remaining Blockers

- No functional blocker remains for this assertion follow-up.

## Notes

- Previous completed frontend task: `20260523-runtime-control-access-path-display`.
- This follow-up intentionally changes only:
  - `src/views/showroom-admin/narration/NarrationWorkspace.vue`
  - `src/views/showroom-frontstage/shared/payload.ts`
