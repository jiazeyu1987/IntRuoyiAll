# Execution Log

## User Intent

- 2026-08-06：用户要求“提交前后端代码”。
- 按项目规则解释为：提交当前统一仓库中的前后端代码及关联验证资产，并推送当前 `int_main` 到 `origin`。

## Command Intent And Evidence

- `git status --short --branch; git remote -v; git rev-parse --show-toplevel`
  - 目的：确认前后端 Git 所有权、分支和远端。
  - 结果：前后端均归属 `E:\IntRuoyi` 单一仓库；当前分支 `int_main`；远端 `origin` 可用；本地落后远端 6 个提交。
- 读取 `docs\task-closeout-rules.md`、`docs\powershell-memory.md` 和 `docs\experience-index.md`
  - 目的：确认提交、脏工作区基线、PowerShell 编排、推送和收尾门禁。
  - 结果：必须保全全部既有脏改动，提交后复扫，推送前运行端口守卫和大文件检查，禁止强推、重写历史或丢弃并行改动。
- `git diff --stat; git diff --cached --stat; git diff --name-status; git diff --cached --name-status`
  - 目的：盘点已暂存与未暂存变更。
  - 结果：存在后端 Java/JUnit、前端 Vue/TypeScript/静态与真实 E2E、SQL、任务证据、长期经验文档及 evidence 清理删除项；另有一组已暂存任务规划文档。

## Milestone Status

- M1：completed。
- M2：in_progress。
- M3：pending。
- M4：pending。

## Blockers

- `E:\IntRuoyi\.git\index.lock` 为非空 `1,441,792` 字节。此前关联任务也因同一非空锁阻塞提交。
- 首次复核时存在活动进程 `git merge --no-ff --no-edit origin/int_main`；等待 15 秒后进程自然退出，未停止或干预该并发任务。
- 进程退出后锁仍存在，最后写入时间未变化；当前无 `MERGE_HEAD`、`MERGE_MSG`、`CHERRY_PICK_HEAD`、`REBASE_HEAD`、`rebase-merge` 或 `rebase-apply`。
- 正式 `.git/index` 与 `.git/index.lock` 的长度和 SHA-256 均不同；锁不是当前索引的相同副本。
- 只读尝试把锁作为索引运行 `git ls-files --stage` 时 Git 异常退出，退出码 `-1073741819`；未写入、删除、移动或替换任何 Git 元数据。
- 按 `docs/powershell-memory.md#Git index.lock 陈旧锁恢复门禁`，只有零字节、超过 60 秒且无活动 Git 进程的精确锁才允许删除。本锁非空，当前任务必须 fail fast。
- 影响：无法执行 `git add`、`git commit`、合并 `origin/int_main` 或推送；任务状态更新为 `blocked`。

## Authorized Index Lock Recovery

- 2026-08-06 用户明确回复“授权”，允许一次性备份非空锁、删除原锁并继续提交。
- 第一次恢复尝试检测到 Codex 桌面只读 `git diff --no-index` 进程，按门禁安全中止，未复制或删除锁。
- 等待 10 秒后确认无活动 Git / Git LFS 进程。
- `Copy-Item` 将精确锁文件备份到 `E:\IntRuoyi\.git\index.lock.backup-20260806-090540`。
- 原锁与备份 SHA-256 均为 `9B97BC1366A299084C544168EFD3C81C2F5099D15FE7913BB356760BA073D869`。
- 哈希验证通过后仅删除 `E:\IntRuoyi\.git\index.lock`；未替换正式 `.git/index`。
- 恢复后 `git status --short --branch` 和 `git diff --cached --name-status` 均可正常读取。
- 恢复后远端跟踪状态更新为 `int_main...origin/int_main [behind 16]`，说明等待期间远端新增提交；后续必须在本地基线保全后以非历史重写方式同步。

## Additional Preflight Evidence

- `git fetch origin int_main` -> PASS；当前 `int_main` 落后 `origin/int_main` 6 个提交。
- `git diff --check` -> PASS。
- `git diff --cached --check` -> PASS。
- 未跟踪文件最大为 `371,303` 字节，未发现超大文件。
- 强敏感模式扫描未发现私钥、AWS/GitHub/OpenAI token 或 Bearer token；本机 RRM 包装脚本命中密码变量赋值逻辑，提交恢复后仍需完成脱敏内容复核。
- `doc/tasks/20260805-restart-local-frontend-backend/` 是并发任务新增目录，本任务未修改且不会纳入提交。

## Verification Before Baseline Commit

- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main` 前端 `8081`、后端 `48081`。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`MesQaPqcSchemaTest` 6 tests、`MesFrontlinePqcContextServiceTest` 19 tests，合计 25 tests / 0 failures / 0 errors / 0 skipped。
- 前端静态合同批量命令 -> PASS：
  - `node tests/e2e/p0-production-execution-loop-static.spec.cjs`
  - `node tests/e2e/pqc-production-source-context-static.spec.cjs`
  - `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
  - `node tests/e2e/production-leader-function-tabs-static.spec.js`
  - `node tests/e2e/production-leader-remove-header-content-static.spec.js`
  - `node tests/e2e/production-leader-tabs-flat-style-static.spec.js`
  - `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs`
  - `node tests/e2e/role-requirement-matrix-local-wrapper-static.spec.cjs`
  - `node tests/e2e/unified-list-template-multi-filter-static.spec.js`
  - `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js`
  - `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js`
- `pnpm ts:check` -> PASS。
- `git diff --check` -> PASS。
- `git diff --cached --check` -> PASS。
- Hardcoded secret review -> PASS：`run-rrm-real-e2e-local.ps1` 中两处 12 字符 quoted literal 为 SQL here-string 中的 `$escapedHash` 变量占位，SHA-256 一致且包含 `$`，不是实际密码值；真实临时密码通过 `RRM_LOCAL_E2E_TEMP_PASSWORD` / `RRM_*_PASSWORD` 进程环境变量传入。
- Existing history large-object scan attempted against partial clone -> inconclusive due promisor remote TLS EOF；改为提交前 staged 文件大小扫描。
