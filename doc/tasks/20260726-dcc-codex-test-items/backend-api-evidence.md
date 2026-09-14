# Backend API Evidence

## Scope

- Backend-owned scope: versioned MySQL seed `IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql` and static validation `IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py`.
- No controller/service API contract was changed; the seed populates existing `system_codex_test_case` and `system_codex_test_checkpoint` tables created by `20260724_system_codex_test_management.sql`.

## Contract

- Data contract: six enabled tenant `1` Codex test cases for 智能文控, each with four checkpoints.
- Execution contract: all seeded cases use `default_execution_mode='SEQUENTIAL'`, `parallel_safe=b'0'`, `status='ENABLE'`.
- Persistence contract: cases are keyed by `tenant_id + name + deleted`; checkpoints are keyed by `case_id + sort + deleted` and updated in place.
- Error behavior: the seed emits `SIGNAL SQLSTATE '45000'` if expected case count or checkpoint presence is not satisfied.

## Validation

- BDD: 智能文控测试项可被测试管理发现 -> Given 测试管理表结构存在, When seed 执行, Then six enabled 智能文控 test cases and checkpoints are present.
- BDD: 智能文控测试项必须走真实页面路径 -> Given Runner 领取测试项, When 执行自然语言方法, Then steps reference DCC upload, approval tasks, browser, logs, training, and basic-data real routes.
- BDD: 智能文控写入路径不允许并行 -> Given test cases write task-owned DCC data, When user requests parallel execution, Then cases are marked `parallelSafe=false`.
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> FAIL, expected missing seed SQL.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS, 4 passed.

## Verification

- REGRESSION: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_test_management_migration.py IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS, 6 passed.
- REGRESSION: full migration policy gate -> BLOCKED by unrelated existing `20260725_mes_edhr_recordbook_global_setting.sql: config-seed`.
- GREEN: filtered migration policy gate excluding existing blocker -> PASS, `migrationCount=374`.

## Blockers

- Full migration policy gate is blocked by an unrelated pre-existing SQL metadata type. This task did not modify that file.
