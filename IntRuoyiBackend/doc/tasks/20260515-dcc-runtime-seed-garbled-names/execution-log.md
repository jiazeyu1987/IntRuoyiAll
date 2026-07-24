# Execution Log: DCC runtime 种子中文名称乱码修复

BDD: DCC runtime seed names stay readable in real backend responses -> Given the local backend serves DCC runtime seed directories and categories to the running frontend, When an operator opens DCC directory or category management through the real local app, Then the backend response and stored records should expose readable simplified Chinese names instead of `?` placeholders.

- M1: Completed. The previous backend task `doc/tasks/20260515-kingdee-material-sync-page-until-empty/task.md` was explicitly blocked because the user switched priority to the DCC runtime seed garbled-name issue.
- M2: Completed. This task document and execution log were created before any backend production changes for the fix.

RED: `python doc\\tasks\\20260515-dcc-runtime-seed-garbled-names\\scripts\\check_dcc_runtime_seed_names.py` -> FAIL, the current runtime seed rows were still stored as `DCC???????`, `??PDF??`, and `????????`.

RED: real local page `http://127.0.0.1:8081/dcc/controlled-file/directories` -> FAIL, the table rendered `DCC???????` and `??PDF??` because the backend API response already contained those question-mark names.

GREEN: apply `sql\\mysql\\20260515_dcc_runtime_seed_name_fix.sql` to the running local MySQL at `127.0.0.1:23306` -> PASS, the affected runtime seed rows were updated to readable Chinese names.

GREEN: `python doc\\tasks\\20260515-dcc-runtime-seed-garbled-names\\scripts\\check_dcc_runtime_seed_names.py` -> PASS, the three runtime seed names are now readable.

GREEN: real local page `http://127.0.0.1:8081/dcc/controlled-file/directories` -> PASS, the table now shows `DCC运行时根目录` and `运行时PDF目录`.

## Root Cause

- The DCC directory page bug was caused by corrupted seed data, not by frontend template copy.
- MySQL stored the runtime seed names themselves as literal `?` placeholders, while the table and connection charsets were already `utf8mb4`.
- The data corruption therefore happened before read time, during the earlier seed write path.

## Fix Summary

- Added `doc/tasks/20260515-dcc-runtime-seed-garbled-names/scripts/check_dcc_runtime_seed_names.py` to fail fast when the tracked runtime seed rows still contain `?`.
- Added `sql/mysql/20260515_dcc_runtime_seed_name_fix.sql` as the tracked, repeatable SQL correction for the broken runtime seed names.
- Applied the SQL correction to the live local MySQL used by the running backend so the current frontend page recovered immediately.
