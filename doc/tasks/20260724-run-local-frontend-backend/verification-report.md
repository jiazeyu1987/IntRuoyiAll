# Verification Report

## Scope

- Confirm local IntRuoyi frontend and backend are running on the fixed `int_main` ports.

## Evidence

- Frontend port: `8081`, PID `25356`, process `node.exe`, command line points to `E:\IntRuoyi\IntRuoyiFronted`.
- Backend port: `48081`, PID `16416`, process `java.exe`, command line points to `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`.
- Backend health: `http://127.0.0.1:48081/actuator/health` returned `BACKEND_STATUS=UP`.
- Frontend entry: `http://127.0.0.1:8081/` returned `FRONTEND_STATUS=200`, `FRONTEND_LENGTH=3578`.

## Result

- PASS: 前后端程序均已在本地主工作区固定端口运行并通过可访问性验证。
- PASS: 任务收尾清理已执行，未删除任何文件。
- BLOCKED: Git 提交/推送收尾因非本任务改动未归属而暂停，任务状态保持 `ready_for_closeout`。

## Remaining Blockers

- Git closeout blocked by non-task dirty file `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`.
