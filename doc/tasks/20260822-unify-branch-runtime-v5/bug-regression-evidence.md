# Bug Regression Evidence

## Bug Summary

主工作区运行时脚本仍声明 V4（slot `1..30`），共享登记表已声明 V5（slot `1..40`）。主工作区的完整登记表校验因此拒绝仍在使用的合法 `int_main slot=31`。

## Expected Behavior

主工作区与共享登记表使用同一 V5 合同；slot `31..40` 按第二扩展端口段校验，不再被误报为非法。

## Reproduction

`python -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -k second_extended_slot -q` -> FAIL.
主工作区 V4 脚本报错：`Registered worktree slot ... must be between 1 and 30, got 31.`

## Root Cause

主工作区 `scripts/runtime/branch-runtime-profile.ps1`、守卫文档和端口矩阵仍为 V4；V5 仅存在于另一个有大量未提交改动的 worktree。共享登记表已经是 V5 并包含合法 slot 31。

## Regression Test

已补：为 slot 31 第二扩展端口映射增加 `int_main 8206/48206` 回归测试。

## RED / GREEN

RED: `python -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -k second_extended_slot -q` -> FAIL, V4 只接受 slot `1..30`。

GREEN: `python -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q -p no:cacheprovider` -> PASS, `16 passed`；端口守卫 -> PASS。

## Verification

- `python -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q -p no:cacheprovider` -> PASS, 16 passed。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- Shared V5 registry remains valid with active slot 31 mapped to 8206/48206。

## Risk

只同步端口合同、守卫、槽位脚本和测试文档；不迁移另一个 worktree 的业务源码、SQL、前端或任务产物。

## Blockers / Follow-up

无阻塞项。共享登记表未被本任务写入；后续新增 worktree 继续通过 V5 原子槽位分配脚本取得 `1..40` 中的空闲 slot。
