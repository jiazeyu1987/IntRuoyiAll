# Execution Log

## User Intent

- 用户授权逐个删除除了 `int_main` 之外的其它 worktree。

## Preflight

- 2026-08-05 读取 `docs/worktree-restrictions.md`：worktree 删除必须按 `D:\IntRuoyiWorktree\` 根目录和当前任务归属边界执行。
- 2026-08-05 读取 `docs/task-closeout-rules.md`：修改环境前创建任务目录，完成后记录验证与收尾。
- 2026-08-05 读取 `docs/powershell-memory.md`：PowerShell 编排不得使用 `&&`，Git 提交前检查 status、branch、remote 和 staged 清单。
- 2026-08-05 读取 `docs/powershell-encoding.md`：任务文档按 UTF-8 路径读写。
- 2026-08-05 `git status --short --branch` 显示 `int_main...origin/int_main [ahead 1]`，并存在既有任务文档脏改动。
- 2026-08-05 既有脏改动敏感词扫描无命中。
- 2026-08-05 基线提交：`cf0306987 chore: baseline pre-existing task docs before worktree cleanup`。

## BDD / TDD Evidence

- BDD: 删除非主 worktree -> Given 当前仓库存在 `E:/IntRuoyi` 主工作区和多个 `D:/IntRuoyiWorktree/*` 附加 worktree / When 执行用户授权的逐个删除 / Then 最终 `git worktree list --porcelain` 只保留 `E:/IntRuoyi` 且分支为 `refs/heads/int_main`。
- RED: `git worktree list --porcelain` -> FAIL, 删除前仍存在 36 个非 `int_main` worktree。

## Milestone Updates

- 2026-08-05 Preflight 完成：规则文件已读取，既有脏改动已独立提交为基线。
- 2026-08-05 经验索引命中并读取 `docs/worktree-memory.md` 相关 Worktree 删除门禁与 `docs/release-build-preflight-lessons.md` 物理根目录复核门禁。
- 2026-08-05 删除前门禁结果：目标总数 36；`pathOutOfRoot=0`；dirty worktree 13；HEAD 未进入 `int_main` 14；detached 10；unreferenced detached 0。
- 2026-08-05 用户已明确授权删除除 `int_main` 之外的其它 worktree；本任务仅移除 worktree 注册与物理目录，不删除分支引用。
- 2026-08-05 第一轮 `git worktree remove --force`：13 个直接移除，23 个 Git 注册已移除但物理目录残留，失败文本为 `Directory not empty` 或 `Invalid argument`。
- 2026-08-05 残留门禁复核：23 个残留目录均无 `.git` 文件；命令行进程仅命中 `20260805-process-loss-reasons` 与 `worktree_20260805_ac_m20_pqc_review` 的本地 Vite/Java/esbuild/启动脚本。
- 2026-08-05 停止目标运行态：停止 `20260805-process-loss-reasons` 的 PID 46024、45904、44960、8140、38944；停止 `worktree_20260805_ac_m20_pqc_review` 的 PID 38256、51240、51052、39852、44460。复核 8083、8093、48083、48093 均无监听。
- 2026-08-05 残留目录清理：先用任务专用脚本验证路径、Git 注册、`.git` 文件和进程占用；对 `node_modules` 残留使用空目录镜像清空后删除，23 个残留目录最终全部不存在。
- 2026-08-05 `profile-erp-table-auto-sync` 仍有未提交工作时，先在其自身分支保全提交 `35c583ce5 chore: baseline profile erp worktree before cleanup`，随后移除该 worktree。
- 2026-08-05 端口登记表释放：使用与 `reserve-worktree-slot.ps1` 相同 mutex，释放 7 个已删除目标 active 槽位：`rrm-m0-m6-verification-20260803`、`production-leader-tab-20260804`、`profile-nas-table-auto-sync`、`20260805-process-loss-reasons`、`worktree_20260805_ac_m20_pqc_review`、`20260805-integrate-production-personnel`、`profile-erp-table-auto-sync`。
- 2026-08-05 未触碰项：`D:\IntRuoyiWorktree\20260805-production-personnel-management` 未出现在本仓库 `git worktree list --porcelain` 目标清单中，且不是本次 36 个目标之一；保留其 active 登记项。
- 2026-08-05 经验沉淀：更新 `docs/worktree-memory.md` 的 Dirty Worktree 删除保全门禁、进程扫描排除当前 PID、`node_modules` 残留 `Could not find a part of the path` 处理；同步更新 `docs/experience-index.md`。
- 2026-08-05 task-closeout-cleanup preview：keep `task.md`、`execution-log.md`、`verification-report.md`；delete 临时脚本和空镜像目录；blocked/warnings 均为 none。
- 2026-08-05 task-closeout-cleanup apply：已删除 `empty-node-modules-mirror`、`release-deleted-worktree-slots.ps1`、`remove-residual-worktrees.ps1`。
- 2026-08-05 最终复扫发现新注册 `profile-erp-table-auto-sync-verify`；该 worktree 有 2 个未提交任务文档改动，因缺少端口登记导致提交钩子阻塞。按规则临时登记 slot 2（8083/48083），保全提交 `7d56e21a3 chore: baseline profile erp verification worktree before cleanup` 后移除该 worktree。
- 2026-08-05 释放 `profile-erp-table-auto-sync-verify` 临时槽位：`RELEASED=1`，随后再次运行 task-closeout-cleanup preview/apply 删除 `release-verify-worktree-slot.ps1`；缺失候选项仅为前次 cleanup 已删除的脚本/空目录提醒。

## Verification Evidence

- GREEN: `git worktree list --porcelain` -> PASS, 仅保留 `worktree E:/IntRuoyi`，分支为 `refs/heads/int_main`。
- GREEN: 36 个目标路径 `Test-Path` -> PASS, `REMAINING=0`。
- GREEN: `D:\IntRuoyiWorktree\.ports\worktree-ports.json` -> PASS, 本次已删除目标 active 槽位均已释放；剩余 active 项 1 个且不属于本次 Git worktree 目标清单。
- GREEN: 8083、8093、48083、48093 端口监听复核 -> PASS, 监听数均为 0。
- GREEN: `rg -n "Dirty Worktree 删除保全|process scan exclude current PID|Could not find a part of the path" docs/experience-index.md docs/worktree-memory.md` -> PASS, 经验索引可定位新增门禁。
- GREEN: `task_closeout.py --task-id 20260805-remove-non-main-worktrees --mode apply` -> PASS, blocked/warnings 均为 none。
- GREEN: `profile-erp-table-auto-sync-verify` 补充清理 -> PASS, 保全提交 `7d56e21a3`，`git worktree list --porcelain` 后续不含该路径，临时 active 槽位已释放。

## Blockers

无。
