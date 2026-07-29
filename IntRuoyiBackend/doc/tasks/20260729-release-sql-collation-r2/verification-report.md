# Verification Report

## Result

PASS for local SQL fix; release verification remains in the maintenance release task and requires a new build/publish releaseTag.

## Commands

- `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py -q`
  - RED before SQL fix: failed on missing `collate=utf8mb4_0900_ai_ci`.
- `python -X utf8 -m pytest script\tests\test_codex_smart_scheduling_test_items_seed.py script\tests\test_dcc_codex_test_items_seed.py -q`
  - GREEN after SQL fix: `9 passed in 0.20s`.
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260729-head-test-only-release\evidence\migration-policy-gate-r260729c.json`
  - GREEN after SQL fix: status `passed`.

## Files

- `sql/mysql/20260726_system_codex_smart_scheduling_test_items.sql`
- `script/tests/test_codex_smart_scheduling_test_items_seed.py`

## Remaining Release Gate

After commit and push, create a new clean release worktree from the new commit and rebuild with a new releaseTag. The failed r2 releaseTag must not be reused.
