# Bug Regression Evidence

## Bug Summary

当前运行时端口通过 `profile base port + slot` 计算，但只校验 `slot > 0`，未限制 profile 端口段，也未拒绝计算结果命中其他 profile 的基准端口。

## Expected Behavior

附加 worktree 仅允许使用 `slot 1..19`；任何基准端口、越界槽位或未释放登记冲突必须 fail fast。

## Reproduction

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py`
- 修复前 8 项中 4 项失败，分别证明越界槽位、重复活动槽位、基准工作区附加槽位和自动分配器缺失。

## Root Cause

- `Get-BranchRuntimePorts` 只拒绝负数槽位，没有 profile 端口段上限。
- `Get-RegisteredBranchRuntimeContext` 只校验当前路径的重复登记和端口公式，没有校验整个登记表的活动槽位/端口唯一性。
- 非登记基准工作区仍可通过 `-RequestedSlot` 请求非零槽位。
- 仓库缺少带并发锁的统一槽位分配脚本。
- 基准路径匹配未在规范化路径末尾补分隔符，导致 `E:\IntRuoyi` 未命中 `\IntRuoyi\` marker，并因 `int_main` 分支同时属于两个 profile 而误选列表中的 `int_main_d`。

## Regression Test

- 更新 `IntRuoyiBackend\script\tests\test_branch_runtime_profile.py`，增加越界槽位、重复活动槽位、基准工作区非零槽位和最低空闲槽位分配覆盖。

## RED

- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> FAIL，4 failed / 4 passed，失败原因与预期一致。

## GREEN

- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py` -> PASS，11 passed。
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main 8081/48081`。
- GREEN: 主工作区和已登记 `slot=7` worktree 的 `show-branch-runtime.ps1` 均返回预期 profile、slot 和端口。

## Verification

- 目标 pytest 覆盖运行时解析、登记校验和原子槽位分配。
- branch runtime port guard 校验契约文档、运行时脚本、Git hooks 和当前活动登记表。
- PowerShell parser 校验三个受影响脚本无语法错误。

## Risk And Regression Scope

- 影响 runtime profile 解析、worktree 登记校验和分支启动脚本。
- 不修改五组现有基准端口，也不自动迁移或释放已有槽位。

## Blockers And Follow-up

- 当前无阻塞。
