# Verification Report

## Current Result

SQL collation 修复已通过定向验证，尚未重新构建/发布测试服。

## Evidence

- RED：目标 pytest 先因缺少 `COLLATE=utf8mb4_0900_ai_ci` 失败，证明测试覆盖发布失败根因。
- GREEN：智能排产 seed 目标 pytest 通过，4 passed。
- GREEN：智能排产 seed + DCC seed + case project 迁移相邻 pytest 通过，11 passed。
- GREEN：release migration policy gate 通过，status=`passed`，migrationCount=`403`。
- 变更范围：`20260726_system_codex_smart_scheduling_test_items.sql` 仅给三个临时表补充 `utf8mb4_0900_ai_ci` collation；测试文件新增静态契约。
- 冻结基线：最终发布修复分支从原测试服发布冻结提交 `9420210f7ad4fb2519c179458fae0e823d082b54` 创建，只叠加本次 collation 修复提交，不包含后续 `int_main` 新提交。

## Remaining Work

要完成原测试服发布目标，还需要将本修复提交后，用新 releaseTag 重新执行 `build-release -> publish-test`，并重新验证测试服真实运行态。失败 releaseTag `release-20260801-intmain-head-test-r260801a-r2` 不得复用。
