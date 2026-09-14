# Verification Report

## Scope

- 重启 `E:\IntRuoyi` 当前 `int_main` 本地前端和后端。
- 固定端口：前端 `8081`，后端 `48081`。
- 不修改源码、共享端口配置或数据库配置。

## Evidence

- 旧前端进程：PID `30732`，`node.exe`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`，已停止。
- 旧后端进程：PID `39380`，`java.exe`，命令行归属 `E:\IntRuoyi\IntRuoyiBackend`，已停止。
- 最终前端进程：PID `46764`，`node.exe`，命令行使用 `E:\IntRuoyi\IntRuoyiFronted\node_modules\vite\bin\vite.js --mode env.local --host 127.0.0.1 --strictPort`。
- 最终后端进程：PID `52152`，`java.exe`，命令行使用 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar --spring.profiles.active=local --server.port=48081`。
- 后端健康检查：`http://127.0.0.1:48081/actuator/health` 返回 `BACKEND_STATUS=UP`。
- 前端入口检查：`http://127.0.0.1:8081/` 返回 `FRONTEND_STATUS=200`，响应长度 `3458`。
- 监听状态：`48081` 由 PID `52152` 监听，`8081` 由 PID `46764` 监听。

## Result

- PASS: 前端已重启并可访问。
- PASS: 后端已重启并通过健康检查。

## Remaining Blockers

- 收尾提交/推送未执行：工作区存在大量既有脏改动，本任务只新增/更新自身任务记录，未处理无关改动。