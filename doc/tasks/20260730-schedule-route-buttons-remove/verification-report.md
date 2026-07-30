# Verification Report

## Scope

- Removed the two排产工艺路线 UI buttons from the排产设置 dialog.
- Preserved the full data package buttons and policy save action.
- Did not change backend APIs, database, permissions, routes, or runtime services.

## Commands

- `node tests/e2e/mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-settings-dialog-static.spec.js` -> PASS
- `node tests/e2e/mes-pro-scheduler-workbench-static.spec.js` -> PASS
- `node tests/e2e/mes-scheduler-workbench-noise-reduction-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS

## Result

- PASS: “导出排产工艺路线”和“导入排产工艺路线” no longer appear in the scheduler workbench component.
- PASS: “导出全部数据包”“导入全部数据包”和“保存策略” remain covered by static contracts.
- PASS: TypeScript check passed without unused route import/export handlers.

## Remaining Blockers

- Push blocked: `git push origin int_main` was rejected as non-fast-forward because the local branch is behind `origin/int_main` by 8 commits.
- The workspace has concurrent non-task dirty files, so no pull/merge/rebase/stash was attempted in this task.
- Implementation and verification are complete locally, but final project completion requires safe integration and successful push.
