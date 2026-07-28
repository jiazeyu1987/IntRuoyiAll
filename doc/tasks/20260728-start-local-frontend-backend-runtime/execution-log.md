# Execution Log

## 2026-07-28

- Created task record before starting local runtime services.
- BDD: local int_main runtime startup -> Given the `E:\IntRuoyi` baseline workspace and fixed local ports, When starting the backend and frontend, Then backend health must be `UP` on `48081` and frontend must return HTTP `200` on `8081`.
- Rule evidence: read `docs/task-closeout-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/powershell-memory.md`, and `docs/powershell-encoding.md`.
- Experience gate evidence: read `docs/experience-index.md`; matching gates are routed to `docs/local-runtime.md`, `docs/worktree-restrictions.md`, and `docs/powershell-memory.md`.
- Git state evidence: `git status --short --branch` showed many pre-existing modified and untracked files from unrelated task directories and source/test files. Current startup task will not stage, commit, revert, or clean those artifacts.
- Preflight: `scripts/runtime/show-branch-runtime.ps1` -> PASS; profile `int_main`, frontend `8081`, backend `48081`.
- Preflight: Java, Maven, pnpm, Docker commands -> PASS.
- Preflight: `int-ruoyi-mysql`, `int-ruoyi-redis`, and `docker-minio-1` were running.
- Preflight: required DCC download encryption env vars were configured without logging values.
- Port check: `8081` and `48081` were not listening before startup.
- GREEN: `restart-int-ruoyi-local.ps1 -Component full` -> PASS, dispatch completed.
- GREEN: backend health `GET http://127.0.0.1:48081/actuator/health` -> PASS, returned `{"status":"UP"}` with listener PID `32728`.
- RED: frontend `GET http://127.0.0.1:8081/` -> FAIL, connection refused; frontend log showed Vite failed to load `IntRuoyiFronted\vite.config.ts` because `@babel/helper-validator-identifier` could not be resolved.
- RED: `pnpm install --frozen-lockfile` -> PASS but did not repair the broken pnpm link layout; frontend restart still failed with the same missing Babel helper.
- Diagnosis: `pnpm list @babel/helper-validator-identifier --depth 5` showed the dependency exists in the lockfile tree, while Node resolution through the installed layout initially failed.
- GREEN: `pnpm install --frozen-lockfile --force` -> PASS, rebuilt `node_modules` links without changing the lockfile.
- GREEN: direct Node probes for `@babel/types` and `@babel/helper-validator-identifier` through the pnpm virtual store -> PASS.
- GREEN: `restart-int-ruoyi-local.ps1 -Component frontend` -> PASS, dispatch completed.
- GREEN: frontend `GET http://127.0.0.1:8081/` -> PASS, returned HTTP `200` with listener PID `9040`.
- GREEN: final backend health remained `UP` on PID `32728`.
- Experience consolidation: merged the pnpm link repair gate into `docs/local-runtime.md` and added the route in `docs/experience-index.md`; no new long-term document was created.
- UTF-8 / documentation verification: `rg` located the new pnpm link gate in `docs/local-runtime.md`, `docs/experience-index.md`, and this task directory.
- GREEN: `git diff --check -- docs/local-runtime.md docs/experience-index.md doc/tasks/20260728-start-local-frontend-backend-runtime/...` -> PASS, only line-ending warnings.
- CLOSEOUT: `task-closeout-cleanup --mode preview --task-id 20260728-start-local-frontend-backend-runtime` -> PASS, planned to keep core task records and delete only `runtime/restart-operation.json`.
- CLOSEOUT: `task-closeout-cleanup --mode apply --task-id 20260728-start-local-frontend-backend-runtime` -> PASS, deleted only `runtime/restart-operation.json`.
- BLOCKER: final commit/push is not safe in the current dirty workspace because many unrelated source, test, docs, and task files pre-existed this request. Per task ownership rules, this task did not stage or commit unrelated changes.
