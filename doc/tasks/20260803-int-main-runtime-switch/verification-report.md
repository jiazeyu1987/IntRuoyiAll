# Verification Report

## Status

PASS for runtime switch; closeout commit/push blocked by unrelated concurrent workspace changes.

## Summary

本机 `48081` 已从旧 jar `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar` 切换为当前 `int_main` HEAD `6f5f52814547146d9c90cd70f34e8a274751ed32` 的干净构建 jar。

## Runtime

- 新 jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-int-main-6f5f52814.jar`
- SHA256：`D097DB0D8A1A846C03860E3186EF833A9A12C3146CDC176DBFF9AF9CB6E48C6B`
- 新 PID：`42064`
- 端口：`48081`
- Profile：`local`
- Health：`UP`

## Verification

- 构建命令：`mvn.cmd -pl yudao-server -am "-DskipTests" package` -> `BUILD SUCCESS`
- 路由内置检查：`DccControlledFileController.class` 包含 `controlled-print/records`
- 旧接口表现：`code=404` / `请求地址不存在:admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records`
- 新接口表现：`code=1080000189` / `Current controlled file cannot be printed as a controlled copy`
- 结论：目标 URL 已进入 DCC 受控打印业务校验，不再是 Controller 地址不存在。

## Cleanup

- 临时 detached worktree：`D:\IntRuoyiWorktree\20260803-int-main-runtime-switch`
- Cleanup result：`git worktree remove --force` -> `REMOVE_OK`

## Notes

- 目标文件 `2054545668044052098` 当前业务状态不允许作为受控副本打印；这不是路由缺失，而是正式业务校验返回。
- 本任务未修改业务源码，仅切换本机运行态 jar。
- 收尾提交/推送未执行，避免混入主工作区既有并发脏改动和 ahead 提交。
