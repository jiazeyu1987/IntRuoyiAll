# run-qms-local-runtime-20260724

## Task Goal

Start the local `int_qms` backend and frontend programs from `E:\IntRuoyiBranch\QMS\IntRuoyiAll`, using the branch runtime port contract.

## Milestones

- [x] Read required local runtime, worktree, branch-port, task, PowerShell, and encoding rules.
- [x] Check `int_qms` runtime ports before startup.
- [x] Start backend on `48061`.
- [x] Start frontend on `8061`.
- [x] Verify backend health and frontend entry.
- [x] Record final runtime evidence.


### Fix Milestones

- [x] Compare QMS against Shedule working tree for runtime source and dependency differences.
- [x] Add regression guard for backend runtime source tracking.
- [x] Restore runtime source files from the working Shedule copy.
- [x] Fix Git ignore rules so Java runtime packages are trackable.
- [x] Run targeted backend tests and package verification.
- [x] Restart and verify QMS backend/frontend runtime.

## Expected Verification

- Backend health endpoint returns a successful response at `http://127.0.0.1:48061/actuator/health`.
- Frontend entry returns a successful response at `http://127.0.0.1:8061/`.
- Startup commands, working directories, ports, and process IDs are recorded in `execution-log.md`.

## Current Status

completed

### Runtime Result

- Backend `48061` is listening and `http://127.0.0.1:48061/actuator/health` returns `200 {"status":"UP"}`.
- Frontend `8061` is listening and `http://127.0.0.1:8061/` returns `200 OK`.
- Root cause was not an incomplete Git pull. The comparable Shedule workspace contained Java source packages under `runtime`, but the root `.gitignore` rule `**/runtime/` caused those Java source directories to be ignored and therefore absent from QMS.
- QMS was repaired by restoring the ignored Java runtime sources from Shedule, adding Git ignore exceptions for backend Java runtime source packages, breaking the BPM form-center Spring bean cycle with lazy `ObjectProvider` access, and reinstalling frontend dependencies.

## 经验门禁

### Local branch runtime gate

- Trigger: Starting or troubleshooting local frontend/backend services for `int_qms`.
- Preflight check: Read `docs/local-runtime.md`, `docs/worktree-restrictions.md`, and `docs/branch-runtime-ports.md`; confirm `8061/48061` port ownership before startup.
- Blocker: Port occupied by an unknown process, another profile, or unrelated program.
- Verification: Record port checks and verify `http://127.0.0.1:8061/` and `http://127.0.0.1:48061/actuator/health`.
- Forbidden action: Do not use `8081/48081`, random ports, shared `.env` edits, backend config edits, or silent service skips.
- Evidence: `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`.

### PowerShell and task documentation gate

- Trigger: Running multi-step PowerShell commands and writing task documents.
- Preflight check: Read `docs/powershell-memory.md`, `docs/powershell-encoding.md`, and `docs/task-closeout-rules.md`.
- Blocker: Missing UTF-8 path, missing task docs, hidden command failure, or secret-bearing output.
- Verification: Record command intent, exit status, and UTF-8 task document updates.
- Forbidden action: Do not use `&&`, default-encoded Chinese writes, or swallowed command failures.
- Evidence: `docs/powershell-memory.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；已恢复正式 BPM/ERP runtime 源码并修正 Git ignore 规则，避免 Java `runtime` 包再次被误忽略。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- doc/tasks/run-qms-local-runtime-20260724/vite-create-server-probe.mjs
### Closeout Evidence

- Stopped task-owned local runtime processes for ports `8061` and `48061` after confirming command lines pointed to the current QMS workspace.
- `task-closeout-cleanup` preview returned no blocked paths or warnings.
- `task-closeout-cleanup` apply deleted only current task temporary logs, PID files, and one-off probe/evidence artifacts.
- Experience consolidation is present in `docs/local-runtime.md` and routed from `docs/experience-index.md`; `rg "runtime 源码包缺失|本地运行构建输入完整性门禁" docs\experience-index.md docs\local-runtime.md` passed.