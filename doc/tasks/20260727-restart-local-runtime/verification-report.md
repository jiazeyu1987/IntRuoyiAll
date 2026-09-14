# Verification Report

## Runtime Restart

- Old frontend process: PID `55676`, `node.exe`, command line under `E:\IntRuoyi\IntRuoyiFronted`.
- Old backend process: PID `44480`, `java.exe`, command line under `E:\IntRuoyi\IntRuoyiBackend`.
- New frontend process: PID `49552`, `node.exe`, `E:\IntRuoyi\IntRuoyiFronted\node_modules\vite\bin\vite.js --mode env.local --host 127.0.0.1 --strictPort`.
- New backend process: PID `5700`, `java.exe`, `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --spring.profiles.active=local --server.port=48081`.

## Verification Evidence

- `PORT 8081 PID 49552 NAME node.exe CMD "D:\Programs\node.exe" E:\IntRuoyi\IntRuoyiFronted\node_modules\vite\bin\vite.js --mode env.local --host 127.0.0.1 --strictPort`
- `PORT 48081 PID 5700 NAME java.exe CMD "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe" -jar E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --spring.profiles.active=local --server.port=48081 --yudao.runtime-control.repo-root=E:\IntRuoyi\IntRuoyiBackend`
- `FRONTEND_CHECK HTTP 200`
- `BACKEND_CHECK status=UP`

## Result

PASS. The local `int_main` frontend and backend are restarted and listening on the required fixed ports.

## Closeout

- `task-closeout-cleanup --mode preview`: PASS, kept task records, no delete, blocked, or warnings.
- `task-closeout-cleanup --mode apply`: PASS, no deletion required.
- Final status: completed.
