# Execution Log

## User Intent

- `2026-07-31`: 用户要求“重启前后端”。

## Rule Bootstrap

- 已读取 `docs/local-runtime.md`。
- 已读取 `docs/task-closeout-rules.md`。
- 已读取 `docs/experience-index.md`，本任务命中本地运行态重启门禁，适用规则来源为 `docs/local-runtime.md`。

## Milestones

- [x] 创建任务目录与最小任务记录。
- [x] 检查端口与进程归属。
- [x] 停止旧后端运行态。
- [ ] 启动前后端。
- [ ] 验证前端与后端入口。

## Verification Evidence

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
- 标准后端打包连续阻塞在 Windows javac class 文件写入阶段，导致 full 重启无法完成。按 no-fallback 规则，未改用旧 Jar、未随机换端口、未强杀非本任务进程。
- `project-experience-consolidation` 已检查；由于根因尚未确认，暂不写入长期经验文档，先保留在本任务证据中。
