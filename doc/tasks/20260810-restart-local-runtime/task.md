# 重启本地前后端

## Task Goal

按项目标准本地重启脚本重启 `E:\IntRuoyi` 的 `int_main` 前端与后端，保持固定端口 `8081/48081`，并验证两个入口可用。

## Milestones

- [x] 读取本地运行、worktree、PowerShell 编码和任务收尾规则。
- [x] 确认标准重启脚本及 `int_main` 前后端目录。
- [x] 执行标准 full 重启。
- [x] 验证后端 health 为 `UP`、前端 HTTP 为 `200`，并确认监听进程归属。
- [x] 完成任务清理门禁并记录最终状态。

## Expected Verification

- `restart-int-ruoyi-local.ps1 -Component full` 正常返回。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `http://127.0.0.1:8081` 返回 HTTP `200`。
- `8081/48081` 的监听 PID 与命令行归属于 `E:\IntRuoyi` 的 `int_main` 运行态。

## Applicable Experience Gates

- 标准脚本路径必须为 `E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1`，前端目录必须解析为 `E:\IntRuoyi\IntRuoyiFronted`。
- full 重启前确认 Docker 本地依赖容器归属；缺失、退出或端口冲突时必须阻塞，不换端口、不切换数据源。
- `int_main` 只使用前端 `8081` 和后端 `48081`；未知进程占用时不得强杀。
- 重启完成必须同时验证后端 health `UP` 与前端 HTTP `200`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准重启脚本和固定运行契约。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

标准 full 重启、运行态验证和 cleanup preview/apply 均已通过；任务完成。

## Cleanup Keep

- doc/tasks/20260810-restart-local-runtime/task.md
- doc/tasks/20260810-restart-local-runtime/execution-log.md
- doc/tasks/20260810-restart-local-runtime/verification-report.md
