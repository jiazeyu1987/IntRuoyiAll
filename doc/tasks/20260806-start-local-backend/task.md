# 启动本地后端

## Task Goal

启动 `E:\IntRuoyi` 主工作区 `int_main` 本地后端，使用固定端口 `48081`，并验证 `/actuator/health` 返回 `UP`。

## Milestones

- [x] 读取本地运行、worktree、PowerShell/Git、编码和任务收尾规则。
- [x] 检查 Git 状态并保存启动前已有脏改动基线。
- [x] 检查 `48081` 当前监听与健康状态。
- [x] 启动后端服务。
- [x] 验证后端 health 为 `UP`，记录 PID、命令和归属。
- [x] 完成本任务记录、收尾和推送。

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

completed

后端已从独立运行 Jar 启动，PID `27116` 正在监听 `48081`，`/actuator/health` 返回 `UP`。启动期间发现 `target` Jar 陈旧导致构造器注入失败；重新打包被并行 Maven PID `44732` 占用同一 MES `target` 阻塞，因此未继续触碰 `target`，改用项目规则要求的 `output\runtime\int_main` 独立运行 Jar 完成启动。

cleanup preview/apply 已完成，仅删除本任务临时启动脚本；长期经验沉淀检查已完成，现有 `docs/local-runtime.md` 与 `docs/powershell-memory.md` 已覆盖本次经验，无需新增长期经验文档。

## Cleanup Candidates

- doc/tasks/20260806-start-local-backend/start-backend.ps1
- doc/tasks/20260806-start-local-backend/start-existing-runtime-jar.ps1

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；未切换端口、数据源、profile、mock 或默认成功，使用 `output\runtime\int_main` 独立运行 Jar 属于本地长期运行正式路径。
- `是否从根因和长期维护角度解决`：是；识别出 `target` Jar 陈旧与并行 Maven `target` 冲突，未强行覆盖共享构建目录。
- `是否存在临时补丁或绕过`：否。
