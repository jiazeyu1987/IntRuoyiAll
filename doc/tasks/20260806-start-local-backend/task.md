# 启动本地后端

## Task Goal

启动 `E:\IntRuoyi` 主工作区 `int_main` 本地后端，使用固定端口 `48081`，并验证 `/actuator/health` 返回 `UP`。

## Milestones

- [x] 读取本地运行、worktree、PowerShell/Git、编码和任务收尾规则。
- [x] 检查 Git 状态并保存启动前已有脏改动基线。
- [x] 检查 `48081` 当前监听与健康状态。
- [ ] 启动后端服务。
- [ ] 验证后端 health 为 `UP`，记录 PID、命令和归属。
- [ ] 完成本任务记录、收尾和推送。

## Expected Verification

- `Get-NetTCPConnection -LocalPort 48081 -State Listen` 显示监听进程。
- 监听进程命令行归属 `E:\IntRuoyi\IntRuoyiBackend` 或稳定 `output\runtime\int_main` 运行副本。
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- 不修改端口、不切换数据源、不使用 mock 或降级启动。

## Applicable Experience Gates

- `docs/local-runtime.md`：`int_main` 后端固定使用 `48081`；端口被未知进程占用时必须 fail fast；启动成功必须记录 PID、命令和 health。
- `docs/worktree-restrictions.md`：非 `int_main` profile 禁止使用 `48081`；不得随机换端口或强杀未知进程。
- `docs/powershell-memory.md`：PowerShell 命令不得用 `&&`；脏工作区先做基线提交；提交/推送前复查状态。
- `docs/task-closeout-rules.md`：任务记录需包含 `task.md`、`execution-log.md`、`verification-report.md`，完成前进入 `ready_for_closeout` 并执行 cleanup。

## Current Status

in_progress

已确认 `48081` 无监听，后端未运行。已创建启动前脏工作区基线提交 `e4a8226e6`，当前准备启动后端。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；仅执行正式本地后端启动与健康检查，不改端口或配置。
- `是否存在临时补丁或绕过`：否。
