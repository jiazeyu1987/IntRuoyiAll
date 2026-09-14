# 20260805 worktree slot registry cleanup

## Task Goal

清理 `D:\IntRuoyiWorktree\.ports\worktree-ports.json` 中创建日期早于 2026-08-05、且前后端端口均无监听的 `int_main` worktree slot 登记占用。

## Milestones

- [x] 读取 worktree、端口矩阵、任务收尾和 PowerShell/UTF-8 规则。
- [x] 建立清理候选清单并核对端口监听状态。
- [x] 标记符合条件的登记项为已释放，不停止进程、不删除 worktree 目录。
- [x] 复验 slot 1..19 的 active 状态和监听状态。

## Expected Verification

- 重新读取 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`，确认符合条件的旧无监听 slot 已 `active=false`。
- 重新扫描 `8082..8100` 与 `48082..48100` 监听状态，确认监听中的 slot 未被清理。
- 记录保留与清理清单。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按端口登记表和实际监听状态清理过期占用。
- 是否存在临时补丁或绕过：否。

## Applicable Gates

- Worktree 清理必须先读取 `docs/worktree-restrictions.md` 和 `docs/branch-runtime-ports.md`。
- 只释放用户授权范围内的登记占用；不得停止未知进程、随机换端口或删除非本任务 worktree。
- `docs/worktree-memory.md#Worktree 端口段与原子槽位门禁`：释放后必须确认 active 槽位和端口没有重复，并运行端口守卫。

## Final Result

- Registry updated: `D:\IntRuoyiWorktree\.ports\worktree-ports.json`
- Cleared slots: 1, 2, 3, 5, 6, 8, 9, 10, 11, 13, 14, 15, 16, 18, 19
- Kept slots: 4, 7, 12, 17
- Final verification: branch runtime port guard passed; only ports for slot 12 and slot 17 are listening.
