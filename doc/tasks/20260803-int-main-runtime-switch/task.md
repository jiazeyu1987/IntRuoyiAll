# 20260803 切换本机 int_main 后端运行态

## Task Goal

将本机 `48081` 后端运行态切换为当前 `int_main` 提交 `6f5f52814547146d9c90cd70f34e8a274751ed32` 构建出的最新代码，修复运行 jar 落后导致 `/dcc/controlled-files/{id}/controlled-print/records` 返回“请求地址不存在”的问题。

## Milestones

- [x] 建立任务记录并核对本机运行态门禁
- [x] 从干净 detached worktree 构建当前 `int_main` 后端 jar
- [x] 停止确认归属的旧 `48081` 后端进程并启动新 jar
- [x] 验证 health 与目标 DCC 受控打印记录接口
- [x] 记录验证结果和最终状态

## Expected Verification

- `git worktree add --detach` 使用当前 `int_main` HEAD 的干净快照，不从脏主工作区直接打包。
- Maven package 成功生成 `yudao-server-exec.jar`。
- 新运行 jar 位于 `E:\IntRuoyi\output\runtime\int_main\`，且包含 `controlled-print/records` Controller 字符串。
- `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 登录态只读请求 `/admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records` 不再返回“请求地址不存在”。

## Applied Experience Gates

- `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁`：主工作区存在并发脏改动时，不得从脏目录重新打包；必须使用干净构建产物并记录 PID、jar、health 和登录态接口验证。
- `docs/local-runtime.md#2026-07-27-本地后端运行-jar-不可变门禁`：长期运行后端必须使用 `output\runtime\int_main` 下的稳定 jar 副本，不能直接运行 Maven `target` jar。
- `docs/worktree-restrictions.md`：临时构建 worktree 必须位于 `D:\IntRuoyiWorktree\` 下，不占用 `48081` 以外的附加运行端口。
- `docs/experience-index.md`：DCC 受控打印、`controlled-print/records` 与 `viewer=1` 预览问题归属到 DCC 受控打印门禁。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务切换正式 `int_main` 构建运行态，不修改接口契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

运行态切换和验证已完成；本机 `48081` 当前运行当前 `int_main` HEAD 构建 jar。收尾提交/推送未执行，因为主工作区在本任务开始前已存在多项并发未提交改动且分支已 ahead `origin/int_main`，为避免混入非本任务文件，当前任务文档仅记录证据，不做宽泛基线提交。

## Verification Summary

- 构建来源：`D:\IntRuoyiWorktree\20260803-int-main-runtime-switch` detached at `6f5f52814547146d9c90cd70f34e8a274751ed32`。
- 构建命令：`mvn.cmd -pl yudao-server -am "-DskipTests" package` -> `BUILD SUCCESS`。
- 新运行 jar：`E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-int-main-6f5f52814.jar`。
- 新 jar SHA256：`D097DB0D8A1A846C03860E3186EF833A9A12C3146CDC176DBFF9AF9CB6E48C6B`。
- 路由验证：jar 内 `DccControlledFileController.class` 包含 `controlled-print/records`。
- 运行态：旧 PID `43876` 已停止，新 PID `42064` 正在监听 `48081`。
- Health：`http://127.0.0.1:48081/actuator/health` -> `UP`。
- 目标接口：登录态请求 `/admin-api/dcc/controlled-files/2054545668044052098/controlled-print/records` 不再返回“请求地址不存在”；当前返回业务校验 `code=1080000189` / `Current controlled file cannot be printed as a controlled copy`，说明路由已加载。
- Cleanup：本次临时 detached worktree 已删除，未保留附加端口或构建 worktree。
