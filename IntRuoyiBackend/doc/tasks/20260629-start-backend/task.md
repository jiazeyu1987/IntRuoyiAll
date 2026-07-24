# 任务：启动本机后端

## 任务目标

- 启动 `IntRuoyi` 本机后端服务，并确认 `http://127.0.0.1:48081/actuator/health` 返回健康状态。

## 前置任务检查

- 最近后端任务：`20260629-red-dot-png`。
- 状态：`Completed`。
- 影响评估：上一任务为独立图片产物生成，不影响本机后端启动。

## 经验门禁

- `docs/powershell-memory.md`
  - PowerShell 5.1 命令执行前需显式设置 UTF-8 输入输出编码。
  - 串联命令不得使用 `&&`，改用分行或 `;`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是
- 是否存在临时补丁或绕过：否

## 里程碑

1. M1：确认项目现有本机后端启动入口与端口状态。
2. M2：使用既有脚本启动或重启本机后端。
3. M3：验证 `48081` 端口监听与 `/actuator/health` 健康状态。
4. M4：回填执行证据并完成任务收尾。

## 预期验证

- `Get-NetTCPConnection -LocalPort 48081 -State Listen` 能看到本机监听进程。
- `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 HTTP 200。
- 健康响应包含 `UP`。

## 当前状态

- Completed

## 完成情况

- 已确认本机 `48081` 端口存在后端监听进程，当前监听 PID 为 `74444`。
- 已确认 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- 已尝试执行项目标准脚本 `script\deploy\restart-int-ruoyi-local.ps1 -Component backend`，但脚本在本地 MySQL 前置探针阶段失败，错误为 `Unknown column 'ole_id' in 'where clause'`；该问题未影响当前已运行的健康后端实例。

## 最终验证结果

- PASS：`Get-NetTCPConnection -LocalPort 48081 -State Listen` 显示 Java 进程监听。
- PASS：`Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
