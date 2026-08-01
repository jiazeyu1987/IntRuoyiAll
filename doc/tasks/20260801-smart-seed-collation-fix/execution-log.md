# Execution Log

## 2026-08-01

- User intent: 继续修复 `release-20260801-intmain-head-test-r260801a-r2` 测试服发布失败点。
- BDD: Codex smart scheduling seed collation -> Given 目标库 `system_codex_test_case` / `system_codex_test_checkpoint` 文本列可能为 `utf8mb4_0900_ai_ci`, When required SQL 使用临时 seed 表写入和校验测试项, Then 所有与目标文本列比较的临时字符串列必须显式对齐目标 collation，避免 MySQL `ERROR 1267`。
- BDD: No database fallback -> Given 发布 required SQL 在测试服失败, When 修复 seed SQL, Then 不修改测试库默认 collation、不手工更新 migration/lock、不复用失败 releaseTag，而是提交源码修复并重建新 releaseTag。
- GREEN: task-preflight -> PASS, 已读取 `AGENTS.md`、`docs/database-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/task-closeout-rules.md`、`docs/worktree-restrictions.md` 和 `docs/experience-index.md`。
- GREEN: frozen-base-guard -> PASS, 原发布冻结主程序提交为 `9420210f7ad4fb2519c179458fae0e823d082b54`；最终修复 worktree `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix` 从该提交创建，分支 `codex/20260801-smart-seed-collation-fix-frozen`，登记 `int_main slot=7`、前端 `8088`、后端 `48088`，只移植已验证的 SQL/test/经验修复。

## TDD Evidence

- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> FAIL, expected reason: 新增 `test_smart_scheduling_test_items_seed_temp_tables_match_target_text_collation` 断言 `tmp_codex_smart_scheduling_*` 临时表必须显式 `COLLATE=utf8mb4_0900_ai_ci`，当前 SQL 只有 `DEFAULT CHARSET=utf8mb4` / `ENGINE=Memory`。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py -q` -> PASS, 4 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix\doc\tasks\20260801-smart-seed-collation-fix\migration-policy-gate.json` -> PASS, status=`passed`, migrationCount=`403`，目标 migration `20260726_system_codex_smart_scheduling_test_items` SHA256=`fa31785e9e5dbf2e8f42364b484662aa59dfe28e5369e522a6ac9adc3771540d`。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py script/tests/test_codex_test_case_project_migration.py -q` -> PASS, 11 passed。

## Issues

### I001 migration policy gate 输出路径误设

- 现象：首次运行 `run-release-migration-policy-gate.py --output doc\tasks\...` 时从 `IntRuoyiBackend` 工作目录解析到后端子目录下不存在的 `doc\tasks`，返回 `FileNotFoundError`。
- 阶段：验证命令。
- 影响：未产生 gate 结果文件，但不代表 SQL 校验失败。
- 原因判断：任务文档目录位于仓库根 `doc\tasks`，命令工作目录为 `IntRuoyiBackend`。
- 处理动作：改用任务目录绝对路径输出后重跑。
- 结果：migration policy gate PASS。
- 是否可前置检查：是。
- 是否可自动化：是，验证脚本前先断言输出目录存在或统一使用绝对路径。
- 下次如何避免：后端子目录执行 release 脚本时，证据输出统一写绝对路径或 `..\doc\tasks\<task-id>`。

### I002 修复 worktree 未登记端口槽位导致提交钩子失败

- 现象：首次 `git commit -m "任务: 修复智能排产测试项排序规则"` 被 `scripts/preflight/branch-runtime-port-guard.ps1` 阻止，提示 `No worktree port registry entry is registered for 'D:\IntRuoyiWorktree\r260801-smartseed-collation-fix'`。
- 阶段：提交前 Git hook。
- 影响：修复无法形成已提交代码，不能作为后续 releaseTag 的合法构建来源。
- 原因判断：创建附加 worktree 后未先执行 `scripts\runtime\reserve-worktree-slot.ps1` 登记 runtime profile/slot，触发 worktree 端口门禁。
- 处理动作：执行 `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts\runtime\reserve-worktree-slot.ps1 -Name r260801-smartseed-collation-fix -Path D:\IntRuoyiWorktree\r260801-smartseed-collation-fix -Branch codex/20260801-smart-seed-collation-fix -Profile int_main -AsJson`，分配 `slot=6`、前端 `8087`、后端 `48087`；随后执行 `show-branch-runtime.ps1` 和 `branch-runtime-port-guard.ps1`。
- 结果：端口守卫 PASS，可重新提交。
- 是否可前置检查：是。
- 是否可自动化：是，新建发布/修复 worktree 后立即自动调用 reserve + guard。
- 下次如何避免：所有 `D:\IntRuoyiWorktree\` 附加 worktree 创建后，提交、推送或启动前先完成端口登记并记录 slot/ports。

### I003 首个修复分支基线晚于原发布冻结提交

- 现象：首个修复 worktree `D:\IntRuoyiWorktree\r260801-smartseed-collation-fix` 基线为 `7c7cce61ddf6ddd4c2d0dc2a8e002608a1f4a239`，晚于原仅测试服发布冻结提交 `9420210f7ad4fb2519c179458fae0e823d082b54` 四个提交。
- 阶段：重新发布前 Git/source gate。
- 影响：若直接从首个修复分支构建，会把原发布开始后产生的其它主线提交带入产物，违反“只构建发布一开始 worktree 里记录的提交”。
- 原因判断：修复时从当前 `E:\IntRuoyi` `int_main` HEAD 创建 worktree，而不是从旧发布冻结 worktree/提交创建。
- 处理动作：新建 `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix`，从 `9420210f7ad4fb2519c179458fae0e823d082b54` 创建分支 `codex/20260801-smart-seed-collation-fix-frozen`，只移植 collation SQL/test/经验修复。
- 结果：最终用于重新发布的源码基线恢复为原冻结提交加最小修复；后续 build-release 必须以该新提交为 manifest source。
- 是否可前置检查：是。
- 是否可自动化：是，修复发布 blocker 前自动比较修复分支 merge-base 与原 release frozen HEAD。
- 下次如何避免：发布 blocker 修复必须从原 release worktree 记录的 frozen HEAD 建分支，除非用户明确要求切到最新主线。

### I004 端口守卫首次从错误工作目录执行

- 现象：创建 `r260801b-frozen-smartseed-fix` 后首次用绝对路径执行 `branch-runtime-port-guard.ps1`，但当前工作目录仍是维护仓，脚本误解析 `repoRoot` 到 `D:\ProjectPackage\Int\IntRuoyiMaintance` 并找不到 `scripts\runtime\branch-runtime-profile.ps1`。
- 阶段：worktree 前置验证。
- 影响：端口守卫结果无效，不能作为提交或启动前置证据。
- 原因判断：脚本按当前 Git 工作目录解析 repo root，不能从其它仓目录直接调用。
- 处理动作：切换工作目录到 `D:\IntRuoyiWorktree\r260801b-frozen-smartseed-fix` 后重跑 `show-branch-runtime.ps1` 和 `branch-runtime-port-guard.ps1`。
- 结果：端口守卫 PASS，profile=`int_main`，slot=`7`，frontend=`8088`，backend=`48088`。
- 是否可前置检查：是。
- 是否可自动化：是，脚本包装器先 `Set-Location` 到目标 repo root。
- 下次如何避免：所有 worktree-local preflight 统一从目标 worktree 根目录执行，不跨仓绝对路径调用。
