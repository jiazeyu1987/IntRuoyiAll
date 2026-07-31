# Verification Report

## Summary

Status: PASS. 已恢复 Docker Engine、本地 MySQL/Redis 依赖，并重启 `int_main` 本地后端；当前 `48081` 由本项目后端监听，健康检查为 `UP`。

## Evidence

- Old listener: PID `56272`，归属 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar`。
- Stop result: `Stop-Process -Id 56272` PASS，旧进程释放 `48081`。
- Initial dependency precheck: `127.0.0.1:23306` MySQL unavailable；`127.0.0.1:26379` Redis unavailable；Docker API initially unavailable。
- Docker recovery: 用户态启动 `com.docker.backend.exe` 后 `docker version` PASS，Server `29.2.1`。
- Dependency recovery: `docker start int-ruoyi-mysql int-ruoyi-redis` PASS；`23306` 与 `26379` 均可连接。
- Backend start: 使用稳定运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar` 启动。
- Final port check: `Get-NetTCPConnection -LocalPort 48081` -> listener PID `39004`，归属 `OWNED_INT_MAIN_RUNTIME_JAR`。
- Final health check: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。

## Notes

未改端口、未改数据源、未重建或清理 MySQL 卷、未提交或回滚并行脏改。

## Cleanup

- Preview: ready；delete `<none>`；blocked `<none>`；warnings `<none>`。
- Apply: applied；deleted_paths `<none>`。
