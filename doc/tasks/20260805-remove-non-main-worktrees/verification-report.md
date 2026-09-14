# Verification Report

## Summary

- 目标：删除 `E:\IntRuoyi` 仓库中除 `E:/IntRuoyi` / `int_main` 之外的 Git worktree。
- 结果：删除前 36 个非主 worktree；删除后 `git worktree list --porcelain` 仅保留 `E:/IntRuoyi`。
- 物理目录：36 个目标路径全部 `Test-Path=False`。
- 端口槽位：释放 7 个已删除目标 active 槽位；保留 1 个非本次 Git worktree 目标的 active 登记项。

## Commands

- `git worktree list --porcelain` -> PASS，仅主工作区。
- `Test-Path` 36 个删除目标 -> PASS，剩余 0。
- `Get-NetTCPConnection` 8083、8093、48083、48093 -> PASS，监听数均为 0。
- `release-deleted-worktree-slots.ps1` -> PASS，`releasedCount=7`。
- `task_closeout.py --task-id 20260805-remove-non-main-worktrees --mode preview` -> PASS，blocked/warnings 均为 none。
- `task_closeout.py --task-id 20260805-remove-non-main-worktrees --mode apply` -> PASS，仅删除本任务临时脚本与空镜像目录。
- `profile-erp-table-auto-sync-verify` 补充清理 -> PASS，保全提交 `7d56e21a3` 后删除，并释放临时 slot 2。

## Notes

- `profile-erp-table-auto-sync` 删除前有未提交实现文件；已先提交保全为 `35c583ce5`，再删除 worktree，避免未提交代码丢失。
- `profile-erp-table-auto-sync-verify` 在最终复扫时重新出现；已先提交保全为 `7d56e21a3`，再删除 worktree，并释放临时登记槽位。
- `D:\IntRuoyiWorktree\20260805-production-personnel-management` 不在本次 `git worktree list --porcelain` 的 36 个目标内，且目录没有 `.git` 文件，因此未删除、未释放。
- 经验沉淀已合并到 `docs/worktree-memory.md` 和 `docs/experience-index.md`，不新增长期经验文档。
