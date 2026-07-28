# Verification Report

## Summary

- 本地前端与后端已通过修正后的项目脚本重新启动。
- 前端端口：`8081`，PID `58060`，进程 `node.exe`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 后端端口：`48081`，PID `52652`，进程 `java.exe`，运行 Jar 位于 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260726-103827.jar`。

## Commands

- `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test-worktree-port-map.ps1` -> PASS。
- `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260726-restart-local-runtime\bug-regression-evidence.md` -> PASS。
- `powershell -NoProfile -ExecutionPolicy Bypass -File IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full` -> PASS。

## Runtime Verification

- `http://127.0.0.1:48081/actuator/health` -> `UP`。
- `http://127.0.0.1:8081/` -> HTTP `200`。

## Notes

- 首次重启失败命中已知路径门禁：脚本查找旧前端目录 `E:\IntRuoyi\yudao-ui-admin-vue3`。
- 已通过回归测试和最小脚本修正改为当前项目目录 `E:\IntRuoyi\IntRuoyiFronted`。
- 端口和配置未改为随机值，未修改共享 `.env` 或 `application-local.yaml`。
- cleanup preview/apply 均通过，未删除任务证据文件。
