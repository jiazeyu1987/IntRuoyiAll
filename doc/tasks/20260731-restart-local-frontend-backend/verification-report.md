# Verification Report

## Result

blocked

## Checks

- `8081` 端口：仍由重启前旧 Vite 进程 `57460` 监听，full 脚本未执行到前端重启。
- `48081` 端口：未监听，旧后端已在标准重启过程中停止，新后端未成功启动。
- 后端 Maven 打包：`mvn -pl yudao-server -am -DskipTests package` 连续两次停在 `yudao-module-infra` javac class 写入阶段，未生成新的启动 Jar。
- JVM 线程栈：`jcmd Thread.print` 显示 Maven 主线程停在 `sun.nio.ch.FileDispatcherImpl.write0` / `com.sun.tools.javac.jvm.ClassWriter.writeClass`。

## Evidence Files

- `doc/tasks/20260731-restart-local-frontend-backend/restart-full.log`
- `doc/tasks/20260731-restart-local-frontend-backend/restart-full-retry2.log`

## Not Completed

- 未通过 `http://127.0.0.1:48081/actuator/health` 的 `status=UP` 验证。
- 未通过重启后 `http://127.0.0.1:8081/` HTTP 200 验证。
- 未执行 fallback：未用旧 runtime Jar 代替标准打包启动，未换端口，未停止非本任务进程。
