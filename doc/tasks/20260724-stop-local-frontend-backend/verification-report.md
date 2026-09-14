# Verification Report

## Scope

- Stop local IntRuoyi frontend and backend on the fixed `int_main` ports.

## Evidence

- Frontend before stop: port `8081`, PID `25356`, process `node.exe`, command line points to `E:\IntRuoyi\IntRuoyiFronted`.
- Backend before stop: port `48081`, PID `47120`, process `java.exe`, command line points to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Stop result: PID `25356` and PID `47120` were stopped.
- Post-stop port check: `8081` returned `FREE`.
- Post-stop port check: `48081` returned `FREE`.

## Result

- PASS: 前后端程序均已停止。
- PASS: 固定端口 `8081` 和 `48081` 已无监听进程。
- PASS: 任务收尾清理已执行，未删除任何文件。

## Remaining Blockers

- 无。