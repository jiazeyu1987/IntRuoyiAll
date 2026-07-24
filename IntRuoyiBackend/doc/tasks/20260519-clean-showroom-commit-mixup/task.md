# Task: Clean Accidental Showroom Commit Mixup

## Goal

Remove the showroom scaffold that was accidentally mixed into the operations help-page commit so the current `int_main` branch keeps only the verified IntRuoyi operations tooling.

## Scope

- Confirm the latest same-repository backend task is explicitly completed before starting this cleanup task.
- Record BDD and strict TDD evidence for a repository-level guard that fails when showroom scaffold files are present.
- Remove only the showroom scaffold files and Maven references that were mixed into the operations-help commit.
- Keep unrelated user changes, temporary files, and other ongoing work untouched.
- Verify the repository no longer references `yudao-module-showroom` and the operations tooling tests still pass.

## Previous Task Check

- Previous backend task: `doc/tasks/20260519-ops-doc-table-format/task.md`
- Status before this task: completed.
- Impact: the previous operations documentation task is already closed, so this cleanup can proceed independently.

## Milestones

- [x] M1: Confirm the previous backend task is closed and create this cleanup task package.
- [x] M2: Record BDD and RED evidence for a guard against accidental showroom scaffold files.
- [x] M3: Remove the mixed showroom scaffold files and Maven references.
- [x] M4: Verify the cleanup with targeted tests, repository scans, and task closeout preview.

## Expected Verification

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_no_accidental_showroom_scaffold.py -q`
- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_no_accidental_showroom_scaffold.py -q`
- `rg -n "yudao-module-showroom|sql/showroom|showroom_v1_schema" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\pom.xml D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests -g !test_no_accidental_showroom_scaffold.py; if ($LASTEXITCODE -eq 1) { Write-Output 'NO_FORBIDDEN_SHOWROOM_REFERENCES'; exit 0 } else { exit $LASTEXITCODE }`
- `mvn -q -pl yudao-server -am -DskipTests validate`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-clean-showroom-commit-mixup --mode preview`

## Current Status

Completed on 2026-05-19. The accidentally mixed showroom scaffold has been removed from `int_main`, and a repository guard test now blocks the same mistake from being reintroduced.

## Final Verification Result

- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_no_accidental_showroom_scaffold.py -q`
- PASS: `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_no_accidental_showroom_scaffold.py -q`
- PASS: `rg -n "yudao-module-showroom|sql/showroom|showroom_v1_schema" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\pom.xml D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests -g !test_no_accidental_showroom_scaffold.py; if ($LASTEXITCODE -eq 1) { Write-Output 'NO_FORBIDDEN_SHOWROOM_REFERENCES'; exit 0 } else { exit $LASTEXITCODE }`
- PASS: `mvn -q -pl yudao-server -am -DskipTests validate`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-clean-showroom-commit-mixup --mode preview`

## Blocker And Impact

- Blocker: none.
- Impact: none.
