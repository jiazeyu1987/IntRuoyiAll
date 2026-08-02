# 本地主工作区前后端重启

## Task Goal

按用户要求重启 `E:\IntRuoyi` 主工作区本地前端和后端运行态，保持 `int_main` 固定端口：前端 `8081`、后端 `48081`。

## Milestones

1. 读取本地运行态、worktree、PowerShell 和任务收尾规则。`completed`
2. 确认当前端口归属、运行脚本和前后端运行前置条件。`completed`
3. 停止归属明确的旧本地前后端进程并重新启动。`completed`
4. 验证后端 health 与前端 HTTP 入口。`completed`
5. 记录验证证据并完成任务收尾。`completed`

## Expected Verification

- `scripts\runtime\show-branch-runtime.ps1` 显示 `int_main`、slot `0`、前端 `8081`、后端 `48081`。
- 旧监听进程命令行必须归属 `E:\IntRuoyi` 后才能停止。
- 重启后 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 重启后 `http://127.0.0.1:8081/` 返回 HTTP `200`。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅按既有本地运行态脚本重启，不改端口、不改配置。
- `是否存在临时补丁或绕过`：否。
