# Verification Report

## Result

PASS

## Evidence

- Stopped authorized old backend process: PID `43376`, previously using `48081` with repo root `D:\IntRuoyiWorktree\20260804-upload-approval-quick-action\IntRuoyiBackend`.
- Stopped old frontend process: PID `17816`, previously using `8081` with frontend root `E:\IntRuoyi\IntRuoyiFronted`.
- Restart command passed: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Backend listener: PID `56580`, port `48081`, repo root `E:\IntRuoyi\IntRuoyiBackend`, runtime Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260805-090357.jar`.
- Frontend listener: PID `39080`, port `8081`, frontend root `E:\IntRuoyi\IntRuoyiFronted`, Vite mode `env.local`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `status=UP`.
- Frontend entry: `http://127.0.0.1:8081/` returned HTTP `200`.
- Cleanup: preview/apply passed with no delete candidates and no blocked paths.

## Notes

- One immediate post-script port scan ran before listeners finished binding and showed no listeners; repeated verification after service readiness confirmed both listeners and endpoints.
- Backend process command line includes local datasource password parameters; they are not copied into task evidence beyond this redacted note.
