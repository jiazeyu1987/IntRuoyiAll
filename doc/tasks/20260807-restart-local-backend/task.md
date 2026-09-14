# 重启 int_main 本地后端

## Task Goal

按项目本地运行规则重启 `E:\IntRuoyi` 的 `int_main` 后端，并确认 `48081` 健康检查恢复为 `UP`。

## Milestones

- [x] 核对当前 `48081` 监听进程、命令行与工作区归属。
- [x] 执行项目标准脚本并记录打包失败与运行态恢复结果。
- [x] 验证后端健康状态、监听端口和运行归档约束。
- [x] 完成任务清理与最终记录。

## Expected Verification

- `48081` 监听进程归属 `E:\IntRuoyi` 的 `int_main` 后端。
- `http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 新后端进程 PID、命令行和启动结果已记录。

## Applicable Gates

- 使用 `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` 的标准本地后端重启路径。
- 仅允许停止已确认属于当前 `int_main` 后端的旧进程；未知进程或其他 runtime profile 占用 `48081` 时立即阻塞。
- 标准重启保持 tokenless Runner 模式，不生成、读取或注入 Runner token。
- 长期运行 Jar 必须位于 `output\runtime\int_main\` 稳定目录，不能直接运行 Maven `target` Jar。
- 不修改端口、数据源、凭据或应用配置，不引入降级路径。

## Current Status

completed - 后端已由新 PID `59460` 在 `48081` 恢复，连续健康检查为 `UP`；标准打包失败已显式记录，cleanup preview/apply 已通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用项目标准重启脚本与固定运行归档。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260807-restart-local-backend/task.md
- doc/tasks/20260807-restart-local-backend/execution-log.md
- doc/tasks/20260807-restart-local-backend/verification-report.md
