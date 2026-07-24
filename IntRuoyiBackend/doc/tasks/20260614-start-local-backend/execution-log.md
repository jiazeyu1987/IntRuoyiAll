# 20260614 启动本机后端执行日志

## 需求记录

- 用户需求：启动后端。

## BDD / 验证记录

- BDD: 本机后端健康可访问 -> Given IntRuoyi 本机后端应运行在 48081 端口；When 检查端口监听并访问 `/actuator/health`；Then 服务应返回 HTTP 200 且状态为 `UP`。
- GREEN: `Get-NetTCPConnection -LocalPort 48081 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,State,OwningProcess` -> PASS，`::48081` 为 `Listen`，进程 PID `51344`。
- GREEN: `Get-CimInstance Win32_Process -Filter "ProcessId=51344" | Select-Object ProcessId,CommandLine` -> PASS，PID `51344` 为 Java 后端进程。
- GREEN: `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 10` -> PASS，HTTP 200，响应 `{"status":"UP"}`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260614-start-local-backend --mode preview` -> PASS，`delete: <none>`、`blocked: <none>`、`warnings: <none>`。
- RED: `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 10` -> FAIL，提交后复查时健康接口不可连接，说明原有后端进程已退出或端口已释放。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File .\script\deploy\restart-ruoyi-local-component.ps1 -Component backend` -> PASS，项目现有本机后端脚本返回成功。
- GREEN: `Get-NetTCPConnection -LocalPort 48081 -ErrorAction SilentlyContinue | Select-Object LocalAddress,LocalPort,State,OwningProcess` -> PASS，`::48081` 为 `Listen`，进程 PID `42628`。
- GREEN: `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 10` -> PASS，HTTP 200，响应 `{"status":"UP"}`。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260614-start-local-backend --mode preview` -> PASS，最终收尾预览仍为 `delete: <none>`、`blocked: <none>`、`warnings: <none>`。

## 执行结论

- 后端已通过项目现有本机启动脚本启动并健康运行。
- 当前健康地址：`http://127.0.0.1:48081/actuator/health`。
- 当前监听 PID：`42628`。
- 未修改业务源码、配置或数据库。
