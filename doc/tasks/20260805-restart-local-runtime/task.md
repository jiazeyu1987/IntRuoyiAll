# 20260805 restart local runtime

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端运行态。

## Milestones

- [x] 读取本地运行、worktree、PowerShell 与收尾规则
- [x] 建立端口归属证据，确认 `8081/48081` 可按 `int_main` 安全重启
- [x] 重启后端与前端
- [x] 验证后端 health 与前端 HTTP 可访问
- [x] 记录验证结果与剩余阻塞

## Expected Verification

- `8081` 前端端口监听归属 `E:\IntRuoyi\IntRuoyiFronted` 或为空后启动。
- `48081` 后端端口监听归属 `E:\IntRuoyi\IntRuoyiBackend` / `output\runtime\int_main` 或为空后启动。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- `Invoke-WebRequest http://127.0.0.1:8081/` 返回 HTTP 200。

## Applicable Gates

- 本地重启必须使用 `int_main` 固定端口 `8081/48081`，不得换端口或跳过服务。
- 端口被旧 `int_main` 进程占用时，记录 PID、命令行和归属依据后才能停止。
- 端口被未知、非 IntRuoyi 或其它 profile 占用时必须 fail fast。
- 后端成功以 health `UP` 为准；前端成功以 `8081` HTTP 200 为准。
- 长运行后端应从 `output\runtime\int_main` 独立 Jar 运行，避免直接占用 Maven `target` Jar。

## Current Status

completed

2026-08-05 17:32 本次前后端重启、最终验证与 cleanup apply 已完成：前端 `8081` 返回 HTTP `200`，后端 `48081` health 返回 `UP`。当前分支仍有并行未提交改动；本任务未触碰或提交非本任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只按正式本地运行脚本/端口契约重启运行态。
- `是否存在临时补丁或绕过`：否。
