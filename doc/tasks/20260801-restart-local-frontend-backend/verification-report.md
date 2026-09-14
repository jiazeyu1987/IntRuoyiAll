# Verification Report

## Scope

本报告验证 `E:\IntRuoyi` 主工作区 `int_main` 本地前后端重启结果。

## Results

- 重启命令：`IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component full`，退出码 `0`。
- 后端端口：`48081`，PID `52620`，运行 Jar `output/runtime/int_main/backend-runtime-control-20260801-102211.jar`。
- 后端健康检查：`http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 前端端口：`8081`，PID `23752`，归属 `E:\IntRuoyi\IntRuoyiFronted` Vite。
- 前端访问：`http://127.0.0.1:8081/` 返回 HTTP `200`。

## Notes

- 未换端口，未停止未知进程。
- 任务记录中不保留数据库密码、token 或私钥等敏感明文。
- 当前 Git 工作区在任务开始前已有大量非本任务改动；本任务未暂存、未提交、未推送。

## Final Verification

PASS: 本地前端与后端均已重启并通过访问验证。

## Closeout Verification

- `task-closeout-cleanup` preview/apply 均通过。
- 未发现本次任务需要清理的临时产物。
- 现有长期经验文档已覆盖本地重启门禁，无需新增经验文档。
