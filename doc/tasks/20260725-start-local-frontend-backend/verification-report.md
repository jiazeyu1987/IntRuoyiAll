# Verification Report

## Scope

- 启动本地 `int_main` 前端和后端，使用固定端口：前端 `8081`，后端 `48081`。
- 后端连接 Docker MySQL 容器 `int-ruoyi-mysql`。

## Evidence

- Docker MySQL 容器：`int-ruoyi-mysql`，镜像 `mysql:8.0.39`，运行中，宿主机端口 `23306` 映射容器端口 `3306`。
- Docker MySQL 业务库：容器内 SQL 探针确认 `ruoyi-vue-pro` 存在。
- 后端进程：`48081` 由 `java.exe` PID `34940` 监听，JDBC URL 指向 `127.0.0.1:23306/ruoyi-vue-pro`。
- 后端健康：`http://127.0.0.1:48081/actuator/health` 返回 `BACKEND_STATUS=UP`。
- 前端进程：`8081` 由 `node.exe` PID `39008` 监听，命令行位于 `E:\IntRuoyi\IntRuoyiFronted`。
- 前端入口：`http://127.0.0.1:8081/` 返回 `FRONTEND_STATUS=200`，响应长度 `3474`。
- 经验沉淀：已更新 `docs/local-runtime.md` 与 `docs/experience-index.md`，避免后续把 Docker MySQL 端口问题误判为前端或后端端口问题。

## Result

- PASS: 前端已启动并可访问。
- PASS: 后端已连接 Docker MySQL 并通过健康检查。

## Remaining Blockers

- 无运行态阻塞。
- 收尾提交/推送需在 unrelated 工作区脏改动分离后执行。