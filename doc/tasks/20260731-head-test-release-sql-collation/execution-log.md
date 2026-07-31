# Execution Log

## User Intent

- 继续完成当前分支 HEAD 的仅测试服发布；未提交改动不得进入构建产物，只能发布测试服，不允许正式服/备份服/mark-tested/promote 动作。

## Bootstrap

- 2026-07-31：维护仓发布任务 `20260730-head-test-only-release` 已在 `publish-test` required-sql 阶段失败，失败 SQL 为 `20260726_dcc_codex_test_items_seed.sql`，MySQL `ERROR 1267 Illegal mix of collations`。
- 当前主程序仓 `E:\IntRuoyi` 分支 `int_main`，当前 HEAD `d1ffcef87e9a6af884cfe47bb0ad69b78febecfd`。
- 已读取 `AGENTS.md`、`docs/backend-development.md`、`docs/database-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`、`docs/worktree-restrictions.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md` 和 `docs/experience-index.md`。
- 目标文件 `IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql` 与 `IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py` 当前无未提交差异。

## BDD

- BDD: required SQL collation compatibility -> Given 测试服真实表 `system_codex_test_case.name` / `system_codex_test_checkpoint.name` 使用 `utf8mb4_0900_ai_ci`，When seed SQL 用临时表中文名称与真实表名称做等值比较，Then 临时表字符串列或比较表达式必须使用一致 collation，避免 `ERROR 1267` 并保持租户、删除标记和 checkpoint 完整性校验不变。
- BDD: smart scheduling seed collation compatibility -> Given 智能排产测试项 seed 使用三个临时表承载 case/checkpoint 名称，且测试库默认 collation 为 `utf8mb4_general_ci`、目标测试管理表名称列为 `utf8mb4_0900_ai_ci`, When required SQL 在测试服执行名称等值比较, Then 三个临时表的字符列必须显式使用 `utf8mb4_0900_ai_ci`，migration 成功且不修改数据库或真实表默认 collation。

## RED / GREEN / REGRESSION

- RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> FAIL, 新增 `test_dcc_codex_test_items_seed_temp_tables_match_target_text_collation` 断言旧 SQL 临时表仍为 `COLLATE=utf8mb4_unicode_ci`，无法证明与测试服目标列 `utf8mb4_0900_ai_ci` 对齐。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py` -> PASS, 5 passed，seed SQL 临时表 collation 已对齐为 `utf8mb4_0900_ai_ci`。
- REGRESSION: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 400 migrations passed。

## Command Evidence

- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260726_dcc_codex_test_items_seed.sql` -> FAIL, 预期原因：单文件运行未带依赖 `20260724_system_codex_test_management`，policy gate 正确拒绝缺依赖上下文。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260724_system_codex_test_management.sql --sql-file sql/mysql/20260726_dcc_codex_test_items_seed.sql` -> FAIL, 预期原因：仍未带 `20260724_system_codex_test_management` 的上游依赖 `20260721_admin_full_scope_role_standardization`。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 使用完整 SQL 图校验依赖和元数据。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260731-head-test-release-sql-collation\bug-regression-evidence.md` -> PASS, Bug regression evidence is valid.
- `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence E:\IntRuoyi\doc\tasks\20260731-head-test-release-sql-collation\ci-cd-evidence.md` -> PASS, CI/CD environment evidence is valid.
- `git diff --check -- IntRuoyiBackend/script/tests/test_dcc_codex_test_items_seed.py IntRuoyiBackend/sql/mysql/20260726_dcc_codex_test_items_seed.sql doc/tasks/20260731-head-test-release-sql-collation` -> PASS.

## Issues

- P006 continuation: `20260726_dcc_codex_test_items_seed.sql` 临时表显式 `COLLATE=utf8mb4_unicode_ci`，与测试服目标列 `utf8mb4_0900_ai_ci` 比较时触发 `ERROR 1267`；本任务将以正式 SQL 修复和契约测试阻止复发。

## Staging Boundary

- 主程序仓存在并行任务 tracked 改动：`doc/tasks/20260731-mes-three-tab-test-sync/artifacts/preflight-report.json`、`doc/tasks/20260731-mes-three-tab-test-sync/artifacts/preflight-summary.md`、`doc/tasks/20260731-mes-three-tab-test-sync/tools/three_tab_sync_preflight.py`。
- 本任务不得暂存或提交上述并行任务文件；只允许暂存 SQL、目标 pytest 和 `doc/tasks/20260731-head-test-release-sql-collation/`。

## Release Continuation

- GREEN: implementation-commit -> PASS，commit=`b6370020247aac7fd27e25a9842601a992a816c7`，subject=`任务: 修复测试项种子排序规则`。
- GREEN: clean-release-worktree -> PASS，`D:\IntRuoyiWorktree\r260731b-release-app` detached HEAD=`b6370020247aac7fd27e25a9842601a992a816c7`，构建后 tracked/untracked 状态为空。
- GREEN: build-release -> PASS，releaseTag=`release-20260731-sqlfix-head-test-r260731b-r2`，构建退出码 `0`，migration policy 400 passed，后端 Maven、前端 Vite、镜像构建/导出和 NAS 上传完成。
- GREEN: manifest-source-proof -> PASS，Manifest v1 与前端 `dist-intruoyi-test\release-info.json` 均声明 backend/admin-frontend commit=`b6370020247aac7fd27e25a9842601a992a816c7`、dirty=false、publishScope=code-only。
- GREEN: packaged-sql-hash -> PASS，包内 `20260726_dcc_codex_test_items_seed.sql` 与冻结源文件 SHA-256 均为 `9299952C4FA4B5DD835024E08EA6F71F0E77F8F7F0F9895364D3EC677FA7B44D`。
- GREEN: experience-preflight -> PASS，本轮仅允许发布测试服 `172.30.30.58:/opt/intruoyi/runtime`；禁止 `mark-tested`、`promote-prod`、`promote-backup`。
- RED: publish-test-r260731b-r2 -> FAIL，operation=`op-2026-07-31T103401370060300Z-c08e9175-0d92-4cf2-a9d5-cddb65e4cf78`；`20260726_dcc_codex_test_items_seed` 已 APPLIED，`20260726_system_codex_smart_scheduling_test_items` 在第 313 行报 `ERROR 1267`，原因是临时表默认 general_ci 与目标名称列 0900_ai_ci 混用。
- GREEN: smart-seed-fix-worktree -> PASS，创建 `D:\IntRuoyiWorktree\r260731c-smart-seed-fix`，分支 `codex/20260731-smart-seed-collation-fix`，基线固定为冻结提交 `b6370020247aac7fd27e25a9842601a992a816c7`，未引入 `int_main` 后续提交。
- RED: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py` -> FAIL, 1 failed / 3 passed；新增测试证明三个智能排产 seed 临时表均未声明 `COLLATE=utf8mb4_0900_ai_ci`。
- GREEN: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py` -> PASS, 4 passed；case seed、case id 映射、checkpoint seed 三个临时表均显式对齐 `utf8mb4_0900_ai_ci`。
- REGRESSION: `python -X utf8 -m pytest script/tests/test_codex_smart_scheduling_test_items_seed.py script/tests/test_dcc_codex_test_items_seed.py` -> PASS, 9 passed。
- REGRESSION: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, 400 migrations passed。
- GREEN: `git diff --check -- IntRuoyiBackend/script/tests/test_codex_smart_scheduling_test_items_seed.py IntRuoyiBackend/sql/mysql/20260726_system_codex_smart_scheduling_test_items.sql` -> PASS。
- RED: implementation-commit-first-attempt -> FAIL，仓库提交钩子拒绝未登记端口槽位的新 worktree，提示 `No worktree port registry entry is registered`；未创建 commit，暂存区仍仅含两个目标文件，未使用 `--no-verify`。
- GREEN: worktree-slot-reservation -> PASS，`reserve-worktree-slot.ps1` 为 `r260731c-smart-seed-fix` 分配 profile=`int_main`、slot=`2`、frontend=`8083`、backend=`48083`；`show-branch-runtime.ps1` 复核通过。
- GREEN: implementation-commit -> PASS，commit=`7b9d8c36f3aa19779277be1a2cddaa50789a3821`，subject=`任务: 修复智能排产种子排序规则`；仅包含目标 SQL 和 pytest，提交后 worktree clean。
- BLOCKER: local-int-main-integration -> `E:\IntRuoyi\.git\index.lock` 为非空 `2752512` 字节，Git 在写入前拒绝 cherry-pick；无 `CHERRY_PICK_HEAD` / `MERGE_HEAD`，未删除非空锁、未部分修改主线。发布继续使用已提交修复 `7b9d8c36...`，融合后续走隔离 integration worktree/远端 fast-forward 门禁。
- GREEN: release-continuation-preflight-r260731c -> PASS，发布输入继续固定为当前任务正式修复提交 `7b9d8c36f3aa19779277be1a2cddaa50789a3821`；不从持续前进的 `int_main` 取新提交，不复用失败 releaseTag。
- COMMAND_INTENT: 从 `7b9d8c36f3aa19779277be1a2cddaa50789a3821` 创建 detached release worktree `D:\IntRuoyiWorktree\r260731c-release-app`，恢复锁文件依赖并构建新候选 `release-20260731-smartseed-sqlfix-head-test-r260731c-r1`。
- GREEN: release-worktree-r260731c -> PASS，`D:\IntRuoyiWorktree\r260731c-release-app` detached HEAD=`7b9d8c36f3aa19779277be1a2cddaa50789a3821`，frozen-lockfile 安装后 `cross-env.cmd`、`vite.cmd`、`vue-tsc.js` 存在，`git status --porcelain --ignored=no` 为空。
- REGRESSION: release-worktree seed tests -> PASS，两个 seed pytest 共 `9 passed`。
- REGRESSION: release-worktree migration policy -> PASS，`migrationCount=400`。
- GREEN: smart-seed-source-hash-r260731c -> PASS，`20260726_system_codex_smart_scheduling_test_items.sql` SHA-256=`FA31785E9E5DBF2E8F42364B484662AA59DFE28E5369E522A6AC9ADC3771540D`；DCC seed SHA-256=`9299952C4FA4B5DD835024E08EA6F71F0E77F8F7F0F9895364D3EC677FA7B44D`。
