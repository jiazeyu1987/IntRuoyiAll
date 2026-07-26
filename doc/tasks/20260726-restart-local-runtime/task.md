# 20260726 Restart Local Runtime

## Task Goal

重启 `E:\IntRuoyi` 当前 `int_main` 工作区的本地前端与后端运行态。

## Milestones

- [x] 读取任务、PowerShell/Git、本地运行和端口矩阵规则。
- [x] 保存开始前既有脏工作区基线提交。
- [x] 修复本地重启脚本仍指向旧前端目录的回归。
- [ ] 检查 `8081` / `48081` 端口归属。
- [ ] 停止确认属于本项目的旧前后端进程。
- [ ] 重新启动后端与前端。
- [ ] 验证后端 health 与前端入口可访问。
- [ ] 完成任务收尾记录。

## Expected Verification

- 后端：`http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 前端：`http://127.0.0.1:8081/` 返回可访问 HTTP 状态。
- 记录端口 PID、命令行归属、启动命令与验证结果。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行受控本地运行态重启，不修改端口或配置。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: 本地前后端重启、`8081`、`48081`、`restart-int-ruoyi-local`。
- Preflight check: 重启前读取 `docs/local-runtime.md`，确认 `E:\IntRuoyi` 的 `int_main` 端口为前端 `8081`、后端 `48081`，并记录端口占用进程的 PID 与命令行归属。
- Blocker: 端口被未知进程、非 IntRuoyi 进程或其他 runtime profile 占用；后端无法连接本地数据库导致 health 不为 `UP`。
- Verification: 记录旧 PID、新 PID、启动命令、`/actuator/health` 状态和前端入口 HTTP 状态。
- Forbidden action: 禁止静默换端口、强杀未知进程、修改共享 `.env` 或 `application-local.yaml`、跳过前端或后端后宣称重启完成。
- Evidence: `docs/local-runtime.md`、`docs/branch-runtime-ports.md`。
