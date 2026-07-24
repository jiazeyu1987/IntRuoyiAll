# Task: Clear frontend vue-tsc compile errors

## Goal

Clear the current frontend repository-wide `vue-tsc` compile errors on
`int_main` so the full frontend type-check command passes again.

## Scope

- Frontend repository only.
- Reproduce the current repository-wide `vue-tsc` failure.
- Group the failing files and error families to find shared root causes.
- Implement the smallest code fixes needed to restore full-repo type-checking.
- Record BDD/TDD evidence and focused risks for the cleanup.

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-int-main-blocker-repair/task.md`
- Status before this task: completed.
- Impact: the electronic batch-record page slice was already verified, but the
  repository still had broader `vue-tsc` failures outside that owned page scope.

## Milestones

- [x] M1: Confirm the latest frontend task is complete before new work.
- [x] M2: Create this task document before production code changes.
- [x] M3: Record BDD scenarios and RED evidence for the current `vue-tsc` failure.
- [x] M4: Implement the minimal code fixes that remove the repository-wide type errors.
- [x] M5: Run full and focused verification, update evidence, and prepare a scoped commit.

## Expected Verification

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit`
- `pnpm exec eslint --ext .ts,.vue src`

## Current Status

Completed. The repository-wide frontend type-check now passes again on
`int_main`, and the companion full-source lint pass also succeeds.

Final verification status:
- `node node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit --pretty false` -> PASS
- `node node_modules\\eslint\\bin\\eslint.js --ext .ts,.vue src` -> PASS

The cleanup was executed in staged waves. The earlier 465-error repository-wide
baseline was reduced through focused directory-owned repairs until the final
full-run wave reached zero remaining TypeScript errors.

## Blocker

- Blocker: none.
- Impact: the frontend repository-wide compile-error cleanup is complete.
