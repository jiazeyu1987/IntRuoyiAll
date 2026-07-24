# Execution Log: 拆分 AGENTS 触发式专项规则

## BDD / TDD Evidence

- `BDD: 触发式规则读取 -> Given Agent 需要执行本机运行、E2E、数据库、PowerShell、发布或收尾相关操作，When 操作前解析适用规则，Then AGENTS.md 应指向对应专项文档且 Agent 必须先读取该文档。`
- `RED: Test-Path docs/local-runtime.md docs/e2e-rules.md docs/database-rules.md docs/powershell-encoding.md docs/task-closeout-rules.md docs/release-backup-restore.md -> FAIL, expected reason: 当前缺少这些专项规则文件。`

## Command Log

- `git status --short --branch` -> PASS，当前分支 `int_main`，存在大量无关未提交/未跟踪改动；本任务只处理专项规则文件、`AGENTS.md` 和本任务目录。
- `Get-Content -Encoding utf8 -Raw AGENTS.md` -> PASS，读取当前总纲。
- `Get-Content -Encoding utf8 -Raw docs\server-access.md` -> PASS，现有未跟踪文件存在，本任务不修改。
- `Get-Content -Encoding utf8 -Raw docs\login-access.md` -> PASS，现有未跟踪文件存在，本任务不修改。
- `Get-Content -Encoding utf8 -Raw docs\experience-index.md` -> FAIL，expected reason: 当前工作区不存在 `docs\experience-index.md`；本任务不执行高风险动作。
- `docs/*.md edit -> PASS`，新增 `local-runtime.md`、`e2e-rules.md`、`database-rules.md`、`powershell-encoding.md`、`task-closeout-rules.md`、`release-backup-restore.md`。
- `AGENTS.md edit -> PASS`，新增 Trigger-Read Rule Files 索引，规定触发场景和必读文件；缺文件时必须 fail fast。
- `GREEN: python -X utf8 trigger-read docs structural verification -> PASS`，确认 `AGENTS.md`、新增专项规则文件、现有 `server-access.md` / `login-access.md` 均可 UTF-8 读取，且 `AGENTS.md` 引用完整。
- `rg Trigger-Read Rule Files -> PASS`，确认 `AGENTS.md` 已列出 worktree、本机运行、服务器、登录、E2E、数据库、PowerShell、任务收尾、发布备份恢复触发式必读文件。
- `python task_closeout.py --task-id split-agents-trigger-docs --mode preview` -> PASS，keep 仅包含 `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 none。
- `python task_closeout.py --task-id split-agents-trigger-docs --mode apply` -> PASS，无删除项，无阻塞项。
- `git commit -m "任务: 拆分触发式规则文档"` -> PASS，提交 `AGENTS.md` 和 6 个新增专项规则文件，提交号 `cec604a5`。
- `project-experience-consolidation -> PASS`，本任务本身即为专项规则沉淀；已按主题写入 `docs/*.md`，未另建无关经验文档。

## Milestone Status

- 创建任务目录并记录现状：completed。
- 新增专项规则文件：completed。
- 更新 `AGENTS.md` 触发式必读索引：completed。
- 验证专项文件、引用和编码：completed。
- 收尾并记录最终验证结果：completed。
