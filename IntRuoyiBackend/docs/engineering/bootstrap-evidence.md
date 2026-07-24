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

## Reusable Startup Checks

- 启动脚本依赖规范的 Git worktree 名称、前后端配对路径和已生成的端口注册表；直接复制或改名目录时，先确认脚本解析到的实际路径。
- 启动后端前先检查 Docker 容器的 bind mount 源文件是否仍存在；容器存在不代表可以启动，过期挂载会在应用启动前直接阻断数据库。
- 复用已有 MySQL 数据卷重建容器时，必须保留数据字典初始化时使用的 MySQL 参数；当前本机库要求 `lower_case_table_names=1`，否则会在 Data Dictionary 初始化阶段失败。
- `application-local.yaml` 中的示例数据源不等同于当前运行时数据源；启动失败时先核对实际监听端口、容器状态和凭据来源，不要静默切换到另一个数据库。
- 前端 Vite 可以在后端未就绪时监听端口，但代理请求会失败；前端 HTTP 200 不代表前后端联调已完成。
