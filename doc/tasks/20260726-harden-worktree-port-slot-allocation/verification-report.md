# Verification Report

## Result

PASS

## Verified Behavior

- 附加 worktree 仅接受 `slot 1..19`；`slot >= 20` 立即失败，不会进入下一 profile 的基准端口。
- 活动登记项按 `profile/slot`、前端端口和后端端口全局唯一。
- `reserve-worktree-slot.ps1` 使用跨进程互斥锁原子分配最低空闲槽位；并发测试获得不同槽位。
- 基准工作区只允许 `slot=0`，`E:\IntRuoyi` 正确解析为 `int_main 8081/48081`。
- 当前活动 `slot=7` worktree 继续解析为 `int_main 8088/48088`。

## Verification Commands

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS，11 passed。
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- `pwsh -NoProfile -File scripts\runtime\show-branch-runtime.ps1` -> PASS，主工作区为 `int_main 8081/48081`。
- 从 `D:\IntRuoyiWorktree\edhr-latest-published-form` 调用新版 `show-branch-runtime.ps1` -> PASS，`slot=7`、`8088/48088`。
- PowerShell parser -> PASS，三个受影响脚本无语法错误。
- Bug regression evidence validator -> PASS。
- 经验索引路由检查 -> PASS。
- 受保护运行时文件旧版契约引用检查 -> PASS，无 `v1/v2` 残留。

## Scope And Risk

- 未启动或停止任何前后端服务，未修改真实端口登记表，原子分配验证使用临时登记表。
- 未修改共享 `.env`、`application-local.yaml`、数据库或远端环境。
- 正式创建新 worktree 时仍需按规则在创建后、首次启动前运行槽位分配脚本。

