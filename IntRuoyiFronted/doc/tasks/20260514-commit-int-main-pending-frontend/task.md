# Task: Commit all pending frontend int_main changes

## Goal

Commit the current pending frontend changes on `int_main` after a repository-wide
type-check verification, including the currently modified source files and the
pending task documentation directories already present in the worktree.

## Scope

- Frontend repository only
- Introduce a relaxed repository-wide TypeScript check configuration without
  changing the main developer `tsconfig.json` baseline
- Normalize the `20260514-remove-auto-schedule-worktree` task files to the
  current `HEAD` version before staging
- Stage all remaining pending frontend changes in the current `int_main`
  worktree
- Run repository-wide frontend type-check verification
- Commit the staged changes with one scoped frontend commit

## Previous Task Check

- Previous frontend task: `doc/tasks/20260514-frontend-vuetsc-cleanup/task.md`
- Status before this task: blocked by user priority switch
- Impact: this commit task may include the currently pending `vue-tsc` cleanup
  changes if verification passes

## Milestones

- [x] M1: Confirm the previous frontend task state.
- [x] M2: Create this task document before Git operations.
- [x] M3: Introduce the relaxed repository-wide TypeScript check path.
- [x] M4: Run repository-wide frontend verification for the pending changes.
- [ ] M5: Stage and commit all pending frontend changes.
- [ ] M6: Record the final verification and commit result.

## Expected Verification

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit`

## Current Status

In progress. The previous repository-wide strict type-check failed. The user has
now chosen the relaxed-check path, so this task is updating the `ts:check` gate
to use a dedicated relaxed TypeScript configuration before rerunning
verification.

## Blocker

- Previous verification command failed:
  `node node_modules/.pnpm/vue-tsc@1.8.27_typescript@5.3.3/node_modules/vue-tsc/bin/vue-tsc.js --noEmit`
- Impact: the current frontend worktree still has unresolved repository-wide
  TypeScript errors across BPMN designer, AI, MES, Pay, MP, System and other
  modules under the strict baseline, so the relaxed gate is being introduced
  before deciding whether this pending frontend worktree can be committed.
- Relaxed verification command also failed:
  `pnpm run ts:check`
- Impact: the relaxed gate removes a portion of strict-only errors, but
  repository-wide contract/type errors still remain, so this task is still
  blocked from commit under the current verification policy.
