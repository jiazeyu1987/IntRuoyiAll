# 20260809 Restart Local Runtime Round 2

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前后端运行态，保持前端端口 `8081`、后端端口 `48081`，并验证服务恢复。

## Milestones

- [x] 创建本轮任务记录并读取本地运行、worktree、任务收尾和 PowerShell 规则。
- [x] 读取经验索引并补充本轮适用门禁。
- [x] 检查本地依赖与 `8081/48081` 旧进程归属。
- [x] 使用项目标准脚本重启前后端。
- [x] 验证前端 HTTP `200`、后端 health `UP` 和监听进程归属。
- [x] 完成任务清理和最终记录。

## Expected Verification

- `http://127.0.0.1:8081/` 返回 HTTP `200`。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `8081`、`48081` 监听 PID 的命令行归属 `E:\IntRuoyi` 主工作区。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准重启脚本、固定端口和正式本地依赖。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`；本任务命中标准本地 full 重启、依赖容器、固定端口、脚本路径和进程归属门禁，按 `docs\local-runtime.md`、`docs\worktree-restrictions.md` 与 `docs\branch-runtime-ports.md` 执行。

## Verification Evidence

- 标准 full 重启脚本 -> PASS；Maven reactor `BUILD SUCCESS`，30 个模块全部成功，前后端启动命令已派发。
- 前端 Vite 新 PID `51912` 在 `209697 ms` 后就绪；`http://127.0.0.1:8081/` 最终返回 HTTP `200`，复核耗时 `8602 ms`。
- 后端新 PID `52880` 监听 `48081`；`http://127.0.0.1:48081/actuator/health` 返回 `UP`，复核耗时 `191 ms`。
- 后端运行 Jar `backend-runtime-control-20260809-140430.jar` 与 Java 启动命令一致，Jar 写入时间早于进程启动时间，不可变检查通过。
- 经验沉淀检查完成；本次复用了已有标准本地重启门禁，没有新的故障模式或通用规则，因此未修改长期经验文档。
- cleanup preview/apply -> PASS；无删除项、无阻塞和警告，保留三份核心任务记录。
