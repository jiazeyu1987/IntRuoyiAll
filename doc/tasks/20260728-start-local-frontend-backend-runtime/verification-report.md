# Verification Report

## Runtime Verification

- Backend: `GET http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.
- Backend listener: port `48081`, PID `32728`, process `java.exe`.
- Frontend: `GET http://127.0.0.1:8081/` returned HTTP `200`.
- Frontend listener: port `8081`, PID `9040`, process `node.exe`.

## Startup Commands

- Full startup dispatch: `IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component full`.
- Frontend repair: `pnpm install --frozen-lockfile` followed by `pnpm install --frozen-lockfile --force` after verifying the lockfile dependency tree existed but the installed pnpm links were broken.
- Frontend restart dispatch: `IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component frontend`.

## Evidence

- Profile check: `scripts/runtime/show-branch-runtime.ps1` resolved `int_main` to frontend `8081` and backend `48081`.
- Port precheck: `8081` and `48081` were not listening before startup.
- Dependency precheck: Java, Maven, pnpm, Docker, local MySQL, Redis, MinIO, and required DCC runtime env var names were present.
- Frontend initial failure: Vite could not resolve `@babel/helper-validator-identifier` from `IntRuoyiFronted/vite.config.ts`.
- Frontend dependency repair: frozen lockfile force rebuild restored pnpm virtual-store resolution; direct Node probes passed before the final frontend restart.
- Final result: both local runtime entry points are available on fixed `int_main` ports.

## Blockers

- Closeout commit/push is blocked by pre-existing unrelated dirty workspace changes. This task did not stage, commit, revert, or clean those unrelated files.
