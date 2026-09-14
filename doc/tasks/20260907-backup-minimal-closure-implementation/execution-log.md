# Execution Log

## Intent

- 用户要求在 worktree `backup_20260907` 实现最小备份恢复闭环及开发验证。
- 用户未授权远程、数据库写入、E2E、Git commit/push 或操作 `int_main` 服务。

## Preflight

- 已读取 worktree、端口、后端、前端、备份恢复、PowerShell 和 task closeout 规则。
- 已读取 ponytail、backend-api-delivery、frontend-feature-delivery 和 BDD 技能。
- worktree `D:\IntRuoyiWorktree\backup_20260907` 从 `int_main` 创建，分支 `codex/backup_20260907`。
- worktree slot 预约成功：`int_main slot=28`，frontend `8162`，backend `48162`。
- 初始 `git status --short --branch` 仅显示分支头，无脏文件。

## BDD

- BDD: BDD-MIN-01 全量备份 -> Given/When/Then 见 task.md。
- BDD: BDD-MIN-02 增量备份 -> Given/When/Then 见 task.md。
- BDD: BDD-MIN-03 恢复演练 -> Given/When/Then 见 task.md。
- BDD: BDD-MIN-04 导出通过证据 -> Given/When/Then 见 task.md。
- BDD: BDD-MIN-05 导出阻塞证据 -> Given/When/Then 见 task.md。

## RED / GREEN

- RED: `python -X utf8 -m pytest script\tests\test_backup_minimal_closure.py -q` -> FAIL，3 项预期缺口：checksum 未覆盖 mysql/object 恢复文件，MySQL 和对象恢复缺少写入前内容校验。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_minimal_closure.py -q` -> PASS，3 tests。

## Milestone M1

- `New-BackupOpsChecksums` 改为递归覆盖备份点内全部本地恢复文件，排除自身和尚未生成的根 manifest。
- MySQL dump 写入后生成 `.sha256`，恢复在 `DROP DATABASE` 前执行非空、`gzip -t` 和 SHA-256 校验。
- 对象备份对新增/修改对象计算真实 SHA-256；对象恢复在 `mc mirror` 前逐对象验证。
- Status: completed。

## Current Status

- GREEN (previous baseline): Python 154 tests; Java 44 tests。
- GREEN (M7 final): `python -X utf8 -m pytest ... --basetemp .pytest-temp\m7-final2 -q` -> PASS，160 tests。
- GREEN (M7 final): Java定向测试 -> PASS，46 tests；后端 package -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-infra -am '-DskipTests' package -q` -> PASS。
- GREEN: `node tests\e2e\system-backup-plan-standard-list-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\system-backup-minimal-closure-static.spec.js` -> PASS。
- GREEN: `pnpm exec eslint src/api/system/backupPlan/index.ts src/views/system/backup-plan/index.vue` -> PASS。
- BASELINE BLOCKED: `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit` -> FAIL，4 个错误均位于未修改的 DCC 上传页；同命令在主工作区复现相同错误。
- GREEN: 10 个变更 PowerShell 文件 AST 解析、内嵌 retention Python `compile()`、配置 JSON 解析、`branch-runtime-port-guard.ps1` -> PASS。
- RED: M7 静态合同 -> FAIL，确认 DCC 不在停服窗口、跨模块父点不一致、生产确认退化、演练吞错、binlog 未限定数据库、证据 TOCTOU、计划注册失败不回滚和旧迁移无升级路径。
- GREEN: M7 相关 Python 回归 -> PASS，160 tests；Java 定向回归 -> PASS，46 tests；前端静态合同/目标 ESLint、后端 package、PowerShell AST 与端口门禁均 PASS。
- CLOSEOUT PREVIEW: `task_closeout.py --task-id 20260907-backup-minimal-closure-implementation --mode preview` -> BLOCKED；删除集合仅为 `IntRuoyiBackend/.pytest-temp`，但脚本同时要求 ff-only 合并和移除 linked worktree。当前分支无法快进到已前进的 `int_main`，主工作区有其它任务改动，且本轮未授权 Git commit/merge/worktree removal，因此未运行 apply、未删除任何文件。
- INTEGRATION: 用户明确要求融合进 `int_main`；允许本任务提交与合并，不包含 push。主工作区与本任务唯一重叠脏文件为 `docs/release-backup-restore.md`，主版本已有更完整规则，因此本任务旧基线上的重复追加不进入提交。
- CLEANUP: `IntRuoyiBackend/.pytest-temp` 已由 preview 锁定为唯一删除项，但递归删除命令被执行策略拒绝；该目录不暂存，保留到 worktree 可安全移除时处理。
- COMMIT: `d33aae230 feat: complete backup recovery evidence closure`（rebase 后提交 ID），仅包含本任务代码、测试、迁移和任务证据；未 push。
- REBASE: `git rebase int_main` -> PASS；当前 `int_main...codex/backup_20260907` 为 `0 1`，满足 fast-forward 历史条件。
- CLOSEOUT PREVIEW AFTER REBASE: 仅因 `E:\IntRuoyi` 主工作区存在大量并行未提交改动而 BLOCKED；未执行 merge 或 worktree removal。
- CLOSEOUT APPLY: 主干检查点先提交为 `31fc6ca21`，并行任务后续提交为 `a9cda5fe1`；备份分支 rebase 后通过 ff-only 合并，最终 `int_main` 包含 `c64490205` 和 `7d368381c`。Git worktree 注册已移除，`.pytest-temp` 已清理。
- NOTE: task docs ignored by local Git exclude -> `doc/tasks/20260907-backup-minimal-closure-implementation/` 命中 `E:/IntRuoyi/.git/info/exclude:17:/doc/tasks/*/`，本地任务记录已写入但不会自动进入 Git。
- BLOCKED: 真实备份 / 远程服务器 / 数据库写入 / Playwright E2E / Git commit / push -> 用户未授权；按任务边界不执行。

## Current Status

- ready_for_closeout：M7 静态分析修复和本地回归完成；真实环境、Git 集成和 closeout apply 仍受既有授权/工作区条件约束。
