# Task: DCC 分发规则重复保存唯一键修复

## Goal

修复 DCC 分发规则在重复保存同一 `category + department` 时触发唯一键冲突的问题，恢复
`/dcc/file-categories/{id}/distribution-rules` 的真实替换语义。

## Scope

- 复现 `dcc_file_category_distribution_rule.uk_dcc_category_distribution_department`
  唯一键冲突。
- 修复 `replaceDistributionRules(...)` 的删除/重建逻辑。
- 保持现有分发规则契约不变。
- 只修本次分发规则保存阻塞，不顺带改动无关 DCC 行为。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-distribution-medium-model/task.md`
- Status before this task: completed for code delivery.
- Impact: the medium-model slice exposed this runtime save blocker during real
  frontend verification, so this fix is the direct next blocker-clearing task.

## Milestones

- [x] M1: Create this backend bugfix task package before code edits.
- [x] M2: Record BDD scenarios and RED evidence for duplicate-save failure.
- [x] M3: Implement the minimal backend fix for replace semantics.
- [x] M4: Run targeted backend verification and update evidence.
- [ ] M5: Commit only task-scoped files if verification fully passes and the
  write set is clean.

## Expected Verification

- `mvn --% -pl yudao-module-dcc -Dtest=DccCategoryDistributionRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260517-dcc-distribution-rule-replace-unique-fix\backend-api-evidence.md`

## Current Status

Completed for code delivery. Distribution-rule save now uses a hard delete for
the owned category rows before reinsert, so repeated save of the same
department no longer collides with the unique key.

## Blocker And Impact

- Blocker: a task-scoped backend commit is not yet safe because the repository
  still contains unrelated dirty backend work outside this fix.
- Impact: the bugfix is implemented and verified, and the paired frontend
  runtime validation is now green, but commit still needs a cleaner write set.

## Final Verification Result

- RED:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccCategoryDistributionRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> FAIL after adding the real unique constraint to the H2 test schema,
    reproducing a duplicate-key error when saving the same department twice.
- GREEN:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccCategoryDistributionRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> PASS after switching replace logic to hard-delete owned category rows.
- Runtime impact:
  - the paired frontend Playwright verification on `DCC下发` save now returns
    backend `code=0` instead of the previous duplicate-key `500`.

## Cleanup Keep

- `doc/tasks/20260517-dcc-distribution-rule-replace-unique-fix/backend-api-evidence.md`
