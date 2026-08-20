# Execution Log

## BDD Scenarios

- BDD: migration policy catches executable evidence dependency -> Given 一个 `schema` 迁移 dependsOn `preflight/backfill/postflight/rollback-dry-run` 等 evidence-only 迁移, When app 仓 migration policy gate 运行, Then gate 必须 fail fast，错误包含 `executable migration cannot depend on evidence-only migration`。
- BDD: C015 schema remains executable without evidence-only dependency -> Given C015 schema 迁移需要发布执行, When 构建发布前运行 app gate 和维护仓实际 ops gate, Then C015 schema 只能依赖可执行 bootstrap，evidence-only preflight/backfill/postflight 保持独立证据门禁。
- BDD: committed-only release input -> Given 主工作区存在未提交改动, When 提交修复后的新 HEAD 并重新创建发布 worktree, Then 构建产物只能来自该已提交 HEAD。

## TDD / Gate Evidence

- GREEN: source-fix-authorization -> PASS, 用户在发布阻塞后回复“授权”，允许正式源码修复和提交新的已提交 HEAD。
- GREEN: app-preflight-read -> PASS, 已读取 `E:\IntRuoyi\AGENTS.md`、`docs\database-rules.md`、`docs\backend-development.md`、`docs\release-backup-restore.md`、`docs\worktree-restrictions.md`、`docs\powershell-memory.md`、database/bug/CI 技能规则和合同。
- RED: pytest from repo root -> FAIL, `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_release_migration_policy_gate.py -q` 在仓库根目录报 `ModuleNotFoundError: No module named 'script'`；这是工作目录错误，不是业务断言。
- RED: executable-evidence-dependency-regression -> FAIL, `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py -q` 在 `E:\IntRuoyi\IntRuoyiBackend` 下执行，新增测试失败：`DID NOT RAISE MigrationPolicyError`。
- RED: full-app-migration-policy-after-new-rule -> FAIL, app 完整 gate 首次发现第二个同类违规：`20260818_mes_pressure_pump_same_name_item_convergence` (`data`) dependsOn `20260814_mes_c015_route_dcc_qa_reconciliation_postflight` (`postflight`)。
- GREEN: focused-migration-policy-tests -> PASS, `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py script\tests\test_mes_pressure_pump_same_name_item_convergence_sql.py -q`，12 passed。
- GREEN: app-full-migration-policy-gate -> PASS, `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260819-c015-release-migration-gate-fix\app-migration-policy-gate.json`，status=passed, migrationCount=505。
- GREEN: maintenance-actual-ops-gate -> PASS, `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260819b\m\ops\release\run-release-migration-policy-gate.py --sql-root E:\IntRuoyi\IntRuoyiBackend\sql\mysql --output E:\IntRuoyi\doc\tasks\20260819-c015-release-migration-gate-fix\maintenance-ops-migration-policy-gate.json`，status=passed, migrationCount=505。
- GREEN: evidence-validators -> PASS, bug regression/database schema/CI-CD evidence validators all passed。
- GREEN: git-diff-check -> PASS, `git diff --check -- <task files>` returned 0；仅有 Git CRLF warning，无 whitespace error。
- GREEN: implementation-commit -> PASS, `git commit -m "任务: 修复C015发布迁移门禁"` created `e0c488267dabf77ce566acd0013cc400fdf88bfe`; branch runtime port guard passed for `int_main/int_main` frontend 8081 backend 48081。

## Issue Log

### A001: app 仓发布门禁漏检 executable -> evidence-only 依赖

- 现象：新增回归期望 `schema -> preflight` 依赖被拒绝，但 app gate 未抛错。
- 阶段：源码修复 / RED 回归。
- 影响：app 仓本地预检可能 PASS，而维护仓实际发布 gate 在构建前才 fail，导致发布耗时后阻塞。
- 原因判断：app `release_migration_policy_gate.py` 缺少维护仓 ops gate 中的 `_check_executable_dependency_closure` 规则。
- 处理动作：新增回归测试并补齐 app gate 的 executable/evidence-only 类型集合与依赖闭包检查。
- 结果：待 GREEN 验证。
- 是否可前置检查：是。
- 是否可自动化：是。
- 下次如何避免：app 仓和维护仓发布迁移门禁规则必须保持同步，并在 build-release 前跑实际 ops gate。

### A002: 首次 pytest 工作目录错误

- 现象：从 `E:\IntRuoyi` 运行脚本测试时报 `ModuleNotFoundError: No module named 'script'`。
- 阶段：测试命令执行。
- 影响：首次命令未进入业务测试断言，不能作为 RED/GREEN 业务证据。
- 原因判断：测试模块按后端根目录 `E:\IntRuoyi\IntRuoyiBackend` 解析 `script` 包。
- 处理动作：切换到后端根目录重跑同一测试文件。
- 结果：成功进入测试并获得业务 RED。
- 是否可前置检查：是。
- 是否可自动化：是。
- 下次如何避免：运行 `script\tests` 下 Python 测试默认 `workdir=E:\IntRuoyi\IntRuoyiBackend`。

### A003: 主程序任务目录创建命令参数不兼容

- 现象：`New-Item -ItemType Directory -Force -LiteralPath ...` 返回 `A parameter cannot be found that matches parameter name 'LiteralPath'`。
- 阶段：任务文档创建。
- 影响：首次目录创建失败，未写入文件。
- 原因判断：当前 PowerShell 环境下 `New-Item` 参数集未接受 `-LiteralPath`。
- 处理动作：改用 `New-Item -ItemType Directory -Force -Path ...` 创建任务目录。
- 结果：任务目录已创建。
- 是否可前置检查：是。
- 是否可自动化：是。
- 下次如何避免：目录创建优先使用 `-Path`；需要 literal 语义时用 .NET API 或先解析路径。

### A004: 压力泵 data 迁移依赖 C015 postflight evidence-only

- 现象：补齐 app gate 后，完整 migration policy gate 失败：`20260818_mes_pressure_pump_same_name_item_convergence` (`data`) dependsOn `20260814_mes_c015_route_dcc_qa_reconciliation_postflight` (`postflight`)。
- 阶段：完整迁移门禁 GREEN 复验。
- 影响：即使 C015 schema 已修复，后续 data 迁移仍会在发布前置门禁被阻塞；必须一并修复后才能提交新 HEAD 用于发布。
- 原因判断：该 data 迁移实际需要 C015 schema 已落地，不应依赖 evidence-only postflight 验收脚本。
- 处理动作：将 `20260818_mes_pressure_pump_same_name_item_convergence.sql` 首行 dependsOn 改为 `20260814_mes_c015_route_dcc_qa_reconciliation_schema`，并同步更新 `test_mes_pressure_pump_same_name_item_convergence_sql.py`。
- 结果：聚焦 SQL 合同、app full gate 和维护仓实际 ops gate 均通过。
- 是否可前置检查：是。
- 是否可自动化：是。
- 下次如何避免：开启 executable/evidence-only 依赖闭包后，必须用完整 SQL root 运行 gate，而不是只跑最初失败的单个 C015 文件。

### A005: 提交前暂存遇到陈旧 Git index.lock

- 现象：`git add -- <task files>` 返回 `fatal: Unable to create 'E:/IntRuoyi/.git/index.lock': File exists.`。
- 阶段：Git 选择性暂存。
- 影响：本任务文件无法暂存提交；发布修复无法形成新的已提交 HEAD。
- 原因判断：`.git/index.lock` 为 0 字节，最后写入时间 `2026-08-19T05:10:20+08:00`，复查时已超过 13903 秒；精确进程名复扫无 `git.exe` / `git-lfs.exe` 活动进程。
- 处理动作：按 `docs\powershell-memory.md` 陈旧锁门禁，精确移除 `E:\IntRuoyi\.git\index.lock`，未停止任何进程。
- 结果：`git status --short --branch -- <task files>` 可正常读取；下一步重试选择性暂存。
- 是否可前置检查：是。
- 是否可自动化：是。
- 下次如何避免：提交/暂存前先检查 `.git/index.lock`，使用精确进程名而非命令行宽泛 `git` 字符串扫描，避免误匹配 `xwechat` 等无关进程。

## Progress Review

- 2026-08-19：已完成修复授权确认、项目规则读取、业务 RED 测试、app gate 规则补齐、C015 schema 元数据调整、压力泵 data 迁移依赖修正、聚焦测试、双 gate GREEN 和实现提交；下一步由发布任务基于最终提交 HEAD 重新冻结。
