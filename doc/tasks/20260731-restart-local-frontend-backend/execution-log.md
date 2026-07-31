# Execution Log

## User Intent

- `2026-07-31`: 用户要求“重启前后端”。
- `2026-07-31`: 用户再次要求“启动前后端”，恢复本任务并验证当前运行态。
- `2026-07-31`: 用户再次要求“启动前后端”；检查发现 `8081` 与 `48081` 均未监听，恢复标准启动流程。

## Rule Bootstrap

- 已读取 `docs/local-runtime.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/experience-index.md`，本任务命中本地运行态重启门禁，适用规则来源为 `docs/local-runtime.md`。

## Milestones

- [x] 创建任务目录与最小任务记录。
- [x] 检查端口与进程归属。
- [x] 停止旧后端运行态。
- [x] 启动前后端。
- [x] 验证前端与后端入口。

## Verification Evidence

- 本次恢复启动前，`8081` 与 `48081` 均未监听。
- 首次标准 full 启动：`restart-int-ruoyi-local.ps1 -Component full` -> FAIL，原因是正式本地依赖容器 `int-ruoyi-mysql` 未运行。
- Docker 状态确认：`int-ruoyi-mysql` 与 `int-ruoyi-redis` 均为 `Exited (255)`，`docker-minio-1` 已停止；未切换数据库、Redis、MinIO、端口或数据卷。
- 恢复既有容器：`docker start int-ruoyi-mysql int-ruoyi-redis docker-minio-1`；MySQL ping、Redis ping 与三个容器 Running 状态通过。
- 第二次标准 full 启动命令退出码为 `0`。
- 当前后端：PID `37212`，稳定运行 Jar 位于 `E:\IntRuoyi\output\runtime\int_main\`，`48081` health 返回 `{"status":"UP"}`。
- 当前前端：PID `14800`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`，`8081` 入口返回 HTTP 200。
- 项目经验沉淀检查：`docs/local-runtime.md` 已覆盖本地 Docker 依赖、固定端口、容器缺失 fail-fast 与前后端独立验证，本次不新增重复长期经验。
- task-closeout-cleanup preview/apply：PASS；保留 `task.md`、`execution-log.md`、`verification-report.md`，无待清理产物。
- 恢复任务时端口状态：`48081` 由 Java PID `8820` 监听，运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260731-144208.jar`，`repo-root` 指向 `E:\IntRuoyi\IntRuoyiBackend`，health 返回 `{"status":"UP"}`。
- 恢复任务时 `8081` 未监听，未发现命令行归属 `E:\IntRuoyi\IntRuoyiFronted` 的活动 Node 进程。
- 执行标准命令：`restart-int-ruoyi-local.ps1 -Component frontend`。
- 首次标准启动后 Vite 已监听 `8081`，但依赖预构建期间请求超时；使用同端口、同配置的 debug 启动确认 `vite:deps Dependencies bundled in 1267432.57ms`，未切换端口、依赖源或构建算法。
- 依赖预构建完成后重新执行标准前端启动脚本，最终前端 PID 为 `36100`，命令行归属 `E:\IntRuoyi\IntRuoyiFronted`。
- 最终验证：`http://127.0.0.1:8081/` 返回 HTTP 200，稳定复验约 `0.009s`；`http://127.0.0.1:8081/@vite/client` 返回 HTTP 200，约 `0.005s`。
- 最终验证：`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`，后端 PID 为 `8820`。
- 项目经验沉淀检查：现有 `docs/local-runtime.md` 已覆盖固定端口、前端依赖预构建与 fail-fast 门禁；本次共享磁盘瞬时高 I/O 属运行时状态，不新增长期经验规则。
- 初始端口归属：`8081` 为 `E:\IntRuoyi\IntRuoyiFronted` 下 Vite / node 进程；`48081` 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260731-001040.jar` Java 进程。命令行中的数据库密码未写入任务记录。
- 首次调用失败：在 `E:\IntRuoyi\IntRuoyiBackend` 工作目录使用错误相对路径调用 `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1`，脚本文件不存在；随后改用绝对路径。
- 绝对路径 full 重启首次超时：shell 等待 120 秒超时，脚本进入 Maven 打包，旧后端 `48081` 已停止，前端 `8081` 仍为旧进程。
- 本任务启动的首次 Maven 子进程长时间无进展，停止任务自有进程 `51868`、`8540`、`60988`。
- full 重启日志承载重跑：`doc/tasks/20260731-restart-local-frontend-backend/restart-full.log`，15 分钟超时后停在 `yudao-module-infra` javac 编译阶段；`jcmd` 显示主线程停在 `WindowsNativeDispatcher.CreateFile` / `ClassWriter.writeClass`。
- 等待并行 Maven 结束后第二次重跑：`doc/tasks/20260731-restart-local-frontend-backend/restart-full-retry2.log`，仍在 `yudao-module-infra` javac 编译阶段超时；`jcmd` 显示主线程停在 `FileDispatcherImpl.write0` / `ClassWriter.writeClass`。
- 已停止第二次重跑的任务自有进程 `24568`、`22816`、`68884`。
- 最终端口状态：`48081` 未监听；`8081` 仍为重启前旧 Vite 进程 `57460`。
- 未执行通过验证：后端 health `UP` 与前端 HTTP 200 未满足。

## Blockers

- 当前根仓库已有大量非本任务未提交/未推送改动；本任务只触碰本次任务文档与本地运行态，不纳入或改写既有代码改动。
- 先前标准后端打包阻塞已由后续当前主工作区稳定运行态解除；本次恢复未重新打包或重启已健康的后端。
- 无剩余启动或运行验证 blocker。
- Closeout preview/apply 已通过：保留 `task.md`、`execution-log.md`、`verification-report.md`，清理旧失败启动日志与本次诊断 debug 日志。
- Git 收尾阻塞：共享 `int_main` 存在多项无关并行改动，且任务期间 HEAD 从并行任务产生了新提交；未执行宽泛基线提交、未混入其它任务文件、未推送。
