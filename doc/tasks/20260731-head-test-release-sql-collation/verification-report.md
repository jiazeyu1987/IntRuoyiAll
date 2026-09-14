# Verification Report

## Status

in_progress

## Verification Summary

- SQL 修复已完成：`tmp_dcc_codex_test_case_seed` 与 `tmp_dcc_codex_test_checkpoint_seed` 均改为 `COLLATE=utf8mb4_0900_ai_ci`。
- 目标测试通过：`python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> 5 passed。
- 发布前迁移门禁通过：`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> status passed, migrationCount 400。
- Evidence validators 通过：bug regression evidence valid；CI/CD environment evidence valid。
- 实现提交完成：`b6370020247aac7fd27e25a9842601a992a816c7`，提交说明 `任务: 修复测试项种子排序规则`。
- 新候选 `release-20260731-sqlfix-head-test-r260731b-r2` 已构建成功；Manifest v1 和前端 release-info 的 commit、dirty、publishScope 一致。
- 包内目标 SQL 与冻结源文件 SHA-256 一致，证明 collation 修复已进入候选包。
- `r260731b-r2` 测试服发布中，原 DCC seed 已 APPLIED，证明第一处修复生效。
- 新阻塞为 `20260726_system_codex_smart_scheduling_test_items.sql` 同类 collation mismatch；发布失败，运行态验收未达成，必须正式修复并使用新 releaseTag。
- 智能排产 seed 正式修复已完成：三个临时表均显式使用 `utf8mb4_0900_ai_ci`，目标测试 4 passed，两组 seed 回归 9 passed，全量 migration policy 400 passed，diff check 通过。
- 修复提交已创建：`7b9d8c36f3aa19779277be1a2cddaa50789a3821`，只包含目标 SQL 和测试；新 clean release worktree、重新构建和测试服发布仍待完成。
- 本地主线融合被非空 `index.lock` 阻塞，主线未发生部分修改；该阻塞不改变修复提交的可构建性，后续需通过隔离 integration worktree/远端 fast-forward 完成融合。
