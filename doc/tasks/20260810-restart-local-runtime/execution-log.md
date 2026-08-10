# Execution Log

## User Intent

- 2026-08-10：用户要求“重启前后端”。

## Command Intent And Evidence

- 读取 `docs/local-runtime.md`：确认 `int_main` 标准端口、标准脚本、依赖容器与验证门禁。
- 读取 `docs/worktree-restrictions.md`：确认 `E:\IntRuoyi` 为 `int_main` 基准工作区，固定使用 `8081/48081`。
- 读取 `docs/task-closeout-rules.md` 与 `docs/powershell-encoding.md`：确认任务文档、UTF-8 和收尾要求。
- 读取 `docs/experience-index.md`：命中本地重启脚本、full 重启依赖容器和固定端口经验门禁，并已复制摘要到 `task.md`。
- 确认标准脚本存在：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1`。
- 发现工作区包含大量既有并行改动；本任务不修改、不清理、不提交这些改动，只管理 `8081/48081` 对应运行态及本任务文档。
- 重启前依赖预检：`int-ruoyi-mysql`、`int-ruoyi-redis`、`docker-minio-1` 均为 running；`23306/26379/9000` 均监听。
- 重启前端口归属：`8081` PID `56540` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite；`48081` PID `49856` 为 `E:\IntRuoyi` 的 `int_main` Java 运行态。
- 执行：`& 'E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1' -Component full`，退出码 `0`。
- Maven 打包：30 个 reactor 模块全部 `SUCCESS`，`BUILD SUCCESS`，总耗时 `06:19 min`；命令按脚本约定使用 `-DskipTests`，未执行测试用例。
- 重启后验证：2026-08-10 08:13:49 +08:00，后端 health `UP`，前端 HTTP `200`。
- 新进程归属：`8081` PID `56312` 为当前 `int_main` 前端；`48081` PID `29240` 为当前 `int_main` 后端。
- 经验沉淀检查：本次未出现新的、可复用的失败模式或环境陷阱；现有 `docs/local-runtime.md` 已完整覆盖脚本路径、容器、端口和健康检查门禁，因此未修改长期经验文档。

## BDD / TDD Applicability

- 本任务仅执行本地运行态重启，不修改产品代码或行为，不适用 BDD 与生产代码 RED/GREEN。

## Milestone Status

- Milestone 1：规则和经验门禁读取完成。
- Milestone 2：标准脚本与运行目录确认完成。
- Milestone 3：标准 full 重启完成，脚本退出码 `0`。
- Milestone 4：前后端入口与新 PID 归属验证通过。
- Milestone 5：`task-closeout-cleanup` preview/apply 均通过；仅保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、阻塞项或警告。

## Blockers

- 当前无。

## Final Status

- completed
- 最终验证：前端 HTTP `200`、后端 health `UP`、监听进程归属正确，cleanup apply 退出码 `0`。
