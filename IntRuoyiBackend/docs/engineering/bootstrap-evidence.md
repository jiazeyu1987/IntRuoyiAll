# IntRuoyi Local Worktree Runtime Bootstrap Evidence

## Goal and Scope

- Goal: support parallel local frontend/backend worktrees with deterministic ports.
- Scope: local deployment tooling under `script/deploy`; no business code, schema, or production deployment behavior is changed.

## Stack and Evidence

- Backend: Spring Boot / Maven project, evidenced by `pom.xml` and `yudao-server`.
- Frontend: Vue 3 / Vite / pnpm project, evidenced by sibling `yudao-ui-admin-vue3/package.json`.
- Shell target: Windows PowerShell 5.1 compatible scripts.

## Local Prerequisites

- `git` must be available and both frontend/backend worktrees must be registered in Git.
- `int_main` must exist in both frontend and backend repositories.
- Every non-main frontend worktree must have a backend worktree with the same derived name, and vice versa.
- For runtime restart only: `pnpm`, `java`, `mvn`, Docker local MySQL/Redis prerequisites used by the existing restart script.

## Commands

- Sync current worktree port registry:
  `powershell -ExecutionPolicy Bypass -File .\script\deploy\sync-int-ruoyi-worktree-ports.ps1`
- Show a worktree status:
  `powershell -ExecutionPolicy Bypass -File .\script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json`
- Restart a worktree:
  `powershell -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`
- Test the port planner:
  `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1`

## Environment Variables

- Frontend runtime script sets `VITE_PORT`, `VITE_BASE_URL`, `VITE_PROXY_TARGET`, and `VITE_OPEN=false` for the selected worktree process.
- Backend runtime script passes `--server.port=<assigned backend port>` to the selected worktree jar.
- No `.env.local` or `application-local.yaml` files are rewritten.

## TDD Evidence

- RED: `powershell -ExecutionPolicy Bypass -File .\script\deploy\test-worktree-port-map.ps1` failed because `worktree-port-map.ps1` did not exist.
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test-worktree-port-map.ps1` passed after adding deterministic assignment, historical max increment, and mismatch fail-fast logic.

## Current Verification

- `sync-int-ruoyi-worktree-ports.ps1 -Json` wrote local registry `D:\ProjectPackage\Int\IntRuoyi\worktrees\.ports\worktree-ports.json`.
- Current assignments:
  - `int_main`: `8081/48081`
  - `automation-2-ebr-visual-fidelity-20260524-review`: `8083/48083`
  - `edhr-test`: `8084/48084`
  - `20260524-release-readiness-gates-dev`: `8085/48085`
  - inactive historical assignment: `20260524-doc-readiness-worktree-check`: `8082/48082`
- `show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` returned `frontendPort=8081` and `backendPort=48081`.
- `show-int-ruoyi-local-status.ps1 -WorktreeName edhr-test -Json` returned `frontendPort=8084` and `backendPort=48084`.

## CI Status

- No CI job was added. This is local Windows runtime tooling and depends on local Git worktree state.

## Known Blockers

- Port separation does not isolate shared MySQL, Redis, MQ, or scheduled jobs. Parallel runtime data isolation remains a separate environment task.
