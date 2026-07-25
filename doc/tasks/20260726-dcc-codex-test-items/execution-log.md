# Execution Log

## Intent

- User request: 仿照测试管理里的“排产工单手动重排”测试项，为智能文控模块增加应补充的测试项。
- Scope: 当前任务仅修改与测试管理/Codex Runner 测试项相关的项目文件及任务证据，不操作远端服务器、生产数据或共享运行环境。

## BDD

- BDD: 智能文控测试项可被测试管理发现 -> Given 测试管理已有排产工单手动重排测试项作为模板, When 智能文控模块测试项被补充到同一测试项契约, Then 测试管理/Codex Runner 能发现这些智能文控测试项且字段完整。
- BDD: 智能文控关键路径覆盖 -> Given 智能文控模块存在台账、文件变更、审批发布、版本生命周期和日志追溯等高风险路径, When 新增测试项清单生成, Then 每个测试项应包含模块、目标、前置条件、步骤和可验证检查点。

## Milestone Updates

- in_progress: 已创建任务目录并记录任务目标、BDD 场景和经验门禁。
- completed: 已定位测试管理表结构和本机现有排产手动重排样例证据；现有正式迁移 `20260724_system_codex_test_management.sql` 只建表、菜单与测试管理员权限，业务测试项需通过新增 seed 沉淀。
- completed: 已新增 `IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql`，包含 6 个智能文控测试项，全部 `SEQUENTIAL`、`parallelSafe=false`、`ENABLE`，按名称幂等 upsert，检查点按 `case_id + sort` 原位更新。
- completed: 已新增 `IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py`，用静态合同约束测试项名称、检查点、真实页面路径、任务自有数据和无 API-only 绕过。

## Verification Evidence

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> FAIL, expected reason: missing `IntRuoyiBackend\sql\mysql\20260726_dcc_codex_test_items_seed.sql`.
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS, 4 passed.
- REGRESSION: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_test_management_migration.py IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py -q` -> PASS, 6 passed.
- REGRESSION: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql` -> FAIL, existing unrelated blocker `20260725_mes_edhr_recordbook_global_setting.sql: config-seed`.
- GREEN: filtered migration policy gate excluding existing blocker `20260725_mes_edhr_recordbook_global_setting.sql` -> PASS, `migrationCount=374`.
- GREEN: backend evidence validator -> PASS, `backend-api-evidence.md` contains Scope/Contract/Validation/BDD/RED/GREEN/Verification/Blockers.
- GREEN: `git diff --check -- IntRuoyiBackend\sql\mysql\20260726_dcc_codex_test_items_seed.sql IntRuoyiBackend\script\tests\test_dcc_codex_test_items_seed.py doc\tasks\20260726-dcc-codex-test-items` -> PASS.
- GREEN: UTF-8 readback -> PASS, 6 task/implementation files read with `python -X utf8`.
- GREEN: task-closeout preview -> PASS, kept task core docs and `backend-api-evidence.md`, no delete/blocked/warnings.
- GREEN: task-closeout apply -> PASS, no delete/blocked/warnings.
- GREEN: project-experience-consolidation -> PASS/no-op, existing `docs/release-build-preflight-lessons.md#2026-07-05-release-migration-dependson-必须引用真实-migrationid` already covers durable dependsOn guidance; no new long-term document needed.

## Blockers

- Full migration policy gate remains blocked by pre-existing unrelated SQL metadata type `config-seed` in `20260725_mes_edhr_recordbook_global_setting.sql`; this task's filtered gate and targeted static contracts passed.

## Closeout

- Current status set to `completed` after cleanup apply and verification.
