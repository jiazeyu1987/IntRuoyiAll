# 20260831 修正 project_code 迁移依赖验证报告

## Status

ready_for_commit

## Verification Summary

- RED: 1 failed/1 passed，旧 data dependsOn 被精确捕获。
- GREEN: 目标测试 2 passed。
- Regression: release metadata/policy/preflight 组合 31 passed。
- Actual maintenance gate: status=passed，migrationCount=551。
- Target-bound code-only preflight: status=passed、blockedCount=0、目标 action=APPLY。
- Data safety: DDL 正文未改、无数据库写入、无 data migration、无 ledger 修改。

## Required Evidence

- RED/GREEN 目标测试
- migration policy gate
- target-bound code-only preflight plan
- Git allow-list、提交和 `int_main` 融合证据
