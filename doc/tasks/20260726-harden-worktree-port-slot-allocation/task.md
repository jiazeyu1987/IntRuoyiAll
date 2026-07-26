# Worktree 端口槽位隔离加固

## Task Goal

修复附加 worktree 使用高槽位时可能计算到其他 runtime profile 基准端口的问题，确保五组基准端口永远不会被新 worktree 占用。

## Milestones

- [x] M1：建立任务记录并保存任务开始前的脏工作区基线。
- [x] M2：补充 BDD 场景与失败回归测试，证明 `slot >= 20` 和跨 profile 端口冲突当前未被拒绝。
- [x] M3：实现固定端口段、全局保留端口和登记冲突的 fail-fast 校验。
- [x] M4：完成目标测试、端口门禁及相关回归验证。
- [ ] M5：完成经验沉淀、cleanup、提交和推送。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py`
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1`
- Bug regression evidence validator
- `git diff --check`

## Current Status

`ready_for_closeout`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过固定 profile 端口段和统一运行时校验消除高槽位碰撞。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger：创建、登记或启动 `D:\IntRuoyiWorktree\` 下的 worktree。
- Preflight check：读取端口登记表，确认 profile、slot、前后端端口配对，并保证 worktree 存续期间槽位不释放。
- Blocker：槽位越界、命中任一基准端口、与未释放登记项重复或端口归属无法确认时立即停止。
- Verification：运行 runtime profile 回归测试、branch runtime port guard，并记录登记端口和计算端口一致。
- Forbidden action：禁止随机换端口、复制其他工作区运行态、停止未知进程或仅因服务停止而释放槽位。
- Evidence：`docs\worktree-memory.md`、`docs\worktree-restrictions.md`、`docs\branch-runtime-ports.md`。

## Cleanup Keep

doc/tasks/20260726-harden-worktree-port-slot-allocation/bug-regression-evidence.md
