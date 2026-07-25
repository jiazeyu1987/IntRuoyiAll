# Execution Log

## Intent

- User requested implementation of the System Management backup plan feature.
- Work is isolated in `D:\IntRuoyiWorktree\system-backup-plan` on branch `codex/system-backup-plan` to avoid dirty main-workspace changes.

## Preflight

- Read skills: backup disaster recovery, frontend feature delivery, backend API delivery, database schema delivery, QA test suite.
- Read trigger docs: worktree restrictions, frontend development, backend development, database rules, E2E rules, release backup restore, task closeout, local runtime, login access, PowerShell encoding.
- Read `docs/experience-index.md` after creating the task directory.
- GREEN: experience-preflight -> PASS, matched backup/release, worktree, menu permission, frontend, backend, and E2E gates.

## BDD

- BDD: 管理员查看备份计划 -> Given 管理员有 `system:backup-plan:query` 权限, When 打开系统管理备份计划, Then 页面显示自动备份状态、频率、时间、下次运行、上次结果和历史备份包列表。
- BDD: 管理员保存每天备份计划 -> Given 管理员有 `system:backup-plan:update` 权限, When 选择“每天”并保存 `01:30`, Then 后端写入配置并注册真实调度任务，返回新的下次运行时间。
- BDD: 管理员保存每周备份计划 -> Given 管理员有 `system:backup-plan:update` 权限, When 选择“每周”和星期/时间, Then 后端写入配置并注册每周调度任务。
- BDD: 缺少脚本时阻塞 -> Given 备份脚本路径不存在, When 保存或启用计划, Then 接口返回明确错误，不启用错误任务。
- BDD: 管理员立即备份一次 -> Given 管理员有 `system:backup-plan:execute` 权限, When 确认“现在备份一次”, Then 后端调用现有备份动作并显式传入生产备份确认链路。

## RED / GREEN

- Pending: backend RED tests.
- Pending: frontend static RED tests.
