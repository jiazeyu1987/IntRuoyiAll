# Verification Report

## Scope

- Runtime profile: `int_main_d`.
- Workspace: `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`.
- Frontend: `http://127.0.0.1:8101/`.
- Backend health: `http://127.0.0.1:48101/actuator/health`.
- MySQL: `127.0.0.1:23306/ruoyi-vue-pro` via Docker container `int-ruoyi-mysql`.
- Redis: `127.0.0.1:26379` via Docker container `int-ruoyi-redis`.

## Results

- `scripts\preflight\branch-runtime-port-guard.ps1`: PASS, `int_main/int_main_d frontend 8101 backend 48101`.
- Docker dependencies: PASS, `int-ruoyi-mysql` and `int-ruoyi-redis` started and ports `23306/26379` listening.
- Backend build: PASS, `mvn.cmd -pl yudao-server -am -DskipTests package` completed with `BUILD SUCCESS`.
- Backend process: PASS, PID `29624`, command line includes `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` and port `48101`.
- Backend health: PASS, `(Invoke-RestMethod http://127.0.0.1:48101/actuator/health).status` returned `UP`.
- Frontend dependencies: PASS, `pnpm install --frozen-lockfile --reporter append-only` completed successfully.
- Frontend process: PASS, PID `43336`, command line includes `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\IntRuoyiFronted\node_modules\.bin\..\vite\bin\vite.js --mode branch-main-d --host 0.0.0.0 --port 8101 --strictPort`.
- Frontend HTTP: PASS, `curl.exe -I --max-time 20 http://127.0.0.1:8101/` returned `HTTP/1.1 200 OK`.

## Build Corrections

- Synchronized missing BPM Form Center runtime source files from `E:\IntRuoyi`: `DefaultWordFormTemplateRecognizer.java`, `FormCenterBpmEventBridge.java`, `FormCenterRuntimeService.java`, `FormCenterRuntimeServiceImpl.java`.
- Synchronized missing ERP Kingdee sync runtime source files from `E:\IntRuoyi`: `ErpKingdeeSyncCommand.java`, `ErpKingdeeSyncContext.java`, `ErpKingdeeSyncRunResult.java`, `ErpKingdeeSyncRuntimeService.java`, `ErpKingdeeSyncRuntimeServiceImpl.java`, `ErpKingdeeSyncTask.java`.
- No fallback, mock success, empty implementation, port downgrade, or shared config rewrite was introduced.

## Notes

- Backend stdout log: `C:\tmp\int-main-d-backend-48101-20260725082712.out.log`.
- Frontend stdout log: `C:\tmp\int-main-d-frontend-8101-20260725084037.out.log`.
- Frontend stderr contains non-blocking warnings from Vite/Browserslist/UnoCSS; entry returned HTTP `200 OK`.
- `pnpm install` reported ignored dependency build scripts per pnpm v10 policy; Vite still started and served successfully.
## Closeout

- Cleanup preview/apply completed with no deletions and no blockers.
- Final implementation commit: `e12e865c fix: restore d main runtime source packages`.
- Final Git sync: `HEAD` equals `origin/int_main` at `e12e865c7c8dfdebc74c77b58881895288357df3`.
