# Execution Log

## User Intent

- 用户要求：“融合int_main,然后启动前后端”。

## Rule Intake

- 已读取 `AGENTS.md`。
- 已读取 `docs\task-closeout-rules.md`。
- 已读取 `docs\worktree-restrictions.md`。
- 已读取 `docs\local-runtime.md`。
- 已读取 `docs\branch-runtime-ports.md`。
- 已读取 `docs\powershell-memory.md`。
- 已读取 `docs\powershell-encoding.md`。
- 已读取 `docs\experience-index.md`。
- 已读取 `docs\powershell-preflight-lessons.md`。

## BDD / Operational Scenario

- `BDD: merge int_main and start branch runtime -> Given current workspace is int_shedule with clean Git status, When int_main is merged and branch runtime scripts start services, Then backend listens on 48021 and frontend listens on 8021 without changing shared int_main ports.`

## Initial Evidence

- Current repository root: `E:\IntRuoyiBranch\Shedule\IntRuoyiAll`。
- Current branch: `int_shedule`。
- Remote: `origin https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- Initial `git status --short --branch`: `## int_shedule...origin/int_shedule`。

## Milestone Updates

- 2026-07-31: Created task directory and initial task records.
- `GREEN: experience-preflight -> PASS, matched local int_shedule runtime gate and PowerShell HTTP/process-scan gate.`
- `git fetch origin int_main -> PASS, origin/int_main updated from 33994045 to e9eca0b3.`
- `git merge --no-edit origin/int_main -> FAIL, background git merge-tree/read-tree processes remained and index.lock was left behind.`
- `git merge --ff-only origin/int_main -> FAIL, local changes/untracked files from the failed merge would be overwritten.`
- `git reset --merge origin/int_main -> FAIL, untracked target-tracked files from the failed merge blocked reset; later reset/read-tree attempts timed out and were stopped as task-owned stuck processes.`
- `BLOCKER: destructive-reset-approval -> required before running git reset --hard origin/int_main to recover the half-written merge state.`
- 用户回复“继续”，授权继续执行破坏性收敛与启动操作。
- `git reset --hard origin/int_main -> FAIL, two attempts each remained in a low-CPU wait state for more than ten minutes; task-owned processes were stopped and stale index locks were isolated.`
- `git read-tree origin/int_main with GIT_INDEX_FILE=.git/index.test -> PASS, target index created independently in 6.1 seconds.`
- 整树 `checkout-index`、Windows `tar` 和 Python tar 流式物化均因大量文件 I/O 极慢而停止；未把这些中间状态作为成功结果。
- `recover_fast_forward.py -> PASS, copied or reused 2595 changed target paths, read 21 dirty-source paths directly from target blobs, removed 12 target-deleted paths, installed the prepared target index, and updated int_shedule to e9eca0b3.`
- 恢复两个不在 `HEAD..origin/int_main` 差异列表内、但被失败合并意外删除的既有文件：`IntRuoyiBackend/.image/文件管理2.jpg`、`IntRuoyiBackend/doc/tasks/20260521-publish-test-server-website-path-fix/execution-log.md`。
- `GREEN: git-target-convergence -> PASS, HEAD=e9eca0b386a7a01b28421084a937792245609d8f equals origin/int_main.`
- `GREEN: tracked-worktree -> PASS, git -c core.safecrlf=false diff --name-only --no-renames returned 0 paths.`
- `GREEN: branch-runtime-port-guard -> PASS, int_shedule frontend 8021 and backend 48021.`
- 启动前确认 `8021/48021` 无监听，Docker 本地依赖 `23306/26379` 正在监听。
- 后端通过 `scripts/runtime/start-branch-backend.ps1` 启动，Java PID `50472` 监听 `48021`。
- `GREEN: backend-health -> PASS, http://127.0.0.1:48021/actuator/health status=UP.`
- 前端通过 `scripts/runtime/start-branch-frontend.ps1` 启动，Node PID `34312` 监听 `8021`，模式 `branch-shedule`，代理后端 `48021`。
- `GREEN: frontend-http -> PASS, http://127.0.0.1:8021/ returned HTTP 200.`
- `GREEN: operational-bdd -> PASS, int_main target integrated and both branch runtime services are reachable on the required ports.`

## Remaining

- 无。

## Closeout

- `project-experience-consolidation -> PASS, added Windows fast-forward half-written checkout recovery gate to docs/worktree-memory.md.`
- `task-closeout-cleanup preview -> PASS, no blocked paths or warnings.`
- 首次 apply 因状态值带 Markdown 反引号而识别为 `unknown`，未删除任何文件；将机器可读状态改为纯文本 `ready_for_closeout` 后重新 preview。
- `task-closeout-cleanup apply -> PASS, removed recovery scripts, diagnostic logs, temporary index backups, 184 MB tar archive, and three extraction directories.`
- 保留前后端运行日志以及 `task.md`、`execution-log.md`、`verification-report.md`。
- 最终 `git status` 复核发现失败的 Windows tar 解包在 `IntRuoyiBackend/.image/` 留下 56 个未跟踪乱码重复图片；任务重新标记为 `ready_for_closeout`。
- `git ls-files --others --exclude-standard -z -> PASS, exactly 56 untracked paths are under IntRuoyiBackend/.image and the only other untracked paths are the three core task records.`
- `tar-mojibake-artifact-cleanup -> PASS, deleted only the exact 56 untracked files after resolving every path under E:\IntRuoyiBranch\Shedule\IntRuoyiAll\IntRuoyiBackend\.image.`
- `GREEN: post-artifact-git-status -> PASS, tracked diff is only docs/worktree-memory.md and the only untracked paths are the three core task records.`
- `GREEN: post-artifact-runtime -> PASS, backend PID 50472 still owns 48021 with health UP; frontend PID 34312 still owns 8021 with HTTP 200; dependencies 23306/26379 remain listening.`
- Cleanup preview correctly kept the four live runtime logs and three core task records, but reported warnings for cleanup candidates already removed by the earlier apply; removed those stale candidate declarations before the final preview/apply.
- `task-closeout-cleanup final preview -> PASS, delete/blocked/warnings are empty and the four live runtime logs plus three core task records are kept.`
- `task-closeout-cleanup final apply -> PASS, no residual task artifacts remained to delete.`
- `project-experience-consolidation final refresh -> PASS, extended the existing Windows fast-forward recovery gate with NUL-safe untracked tar-artifact verification and exact-path cleanup rules.`
- 2026-07-31: Task status updated to `completed` after final cleanup and verification.
