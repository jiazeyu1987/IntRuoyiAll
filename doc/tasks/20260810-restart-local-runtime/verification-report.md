# Verification Report

## Result

PASS

## Runtime Preflight

- `int-ruoyi-mysql`：running。
- `int-ruoyi-redis`：running。
- `docker-minio-1`：running/healthy。
- Docker 依赖端口 `23306/26379/9000`：监听正常。
- 重启前 `8081/48081` 占用均属于 `E:\IntRuoyi` 的当前 `int_main` 运行态。

## Restart Evidence

- 命令：`& 'E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1' -Component full`
- 退出码：`0`。
- Maven reactor：30/30 模块 `SUCCESS`。
- Maven 最终结果：`BUILD SUCCESS`。
- 可执行 Jar：`E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 脚本派发结果：`Restart command dispatched for local full (int_main, frontend=8081, backend=48081)`。

## Runtime Verification

- 验证时间：`2026-08-10 08:13:49 +08:00`。
- 后端：`http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 前端：`http://127.0.0.1:8081` 返回 HTTP `200`。
- 前端监听：PID `56312`，进程 `node.exe`，命令行归属于 `E:\IntRuoyi\IntRuoyiFronted`。
- 后端监听：PID `29240`，进程 `java.exe`，运行 Jar 与端口参数归属于 `E:\IntRuoyi\output\runtime\int_main` 和 `48081`。

## Scope Notes

- 本任务未修改产品代码、配置或端口。
- 本任务未清理、提交或覆盖工作区内既有并行改动。
- Maven 命令由标准脚本使用 `-DskipTests` 执行；本任务为运行态重启，不涉及行为变更测试。

## Closeout

- `task-closeout-cleanup --mode preview`：`status=ready`，无删除项、阻塞项或警告。
- `task-closeout-cleanup --mode apply`：`status=applied`，退出码 `0`。
- 最终状态：`completed`。
