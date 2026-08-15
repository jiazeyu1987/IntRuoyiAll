# Execution Log

## 2026-08-15 Resume

- 用户意图：继续完成一线 PQC 剩余任务，并在验证通过后合并但保留 worktree。
- 基线：`int_main` / `cb0464ce8`。
- 隔离分支：`task/20260815-frontline-pqc-c00-df06-integration`。
- Worktree：`D:\IntRuoyiWorktree\20260815-frontline-pqc-c00-df06-integration`。
- BDD: C00 仅接受批准的活跃订单 QA 快照 -> Given 历史活跃订单缺少 QA 快照且没有批准清单，When 执行 C00 回填，Then 必须 fail-fast，不能从 PQC 任务版本推算并写入。
- BDD: 创建活跃订单锁定 QA 发布版本 -> Given 路线存在唯一启用的 DCC 项目且该项目存在唯一正式 QA 规程发布版本，When 加入活跃订单，Then 同一事务写入三个 QA 快照并生成 FIRST、PATROL_AM、PATROL_PM、FINAL 四类任务。
- BDD: 缺少正式 DCC/QA 关系时拒绝加入 -> Given 路线未绑定 DCC、DCC 已禁用或 QA 正式版本缺失，When 加入活跃订单，Then 明确失败且不写入半成品订单或任务。
- BDD: 移除订单重新激活保留历史锁定 -> Given 已移除订单有完整历史 QA 快照和任务，When 重新激活，Then 使用原锁定版本且不读取当前路线/DCC/QA 重新推算。

## Evidence

- C00 独立评审：FAIL；发现 `UNIQUE_TASK_VERSION` 被用作活跃订单 QA 快照来源。
- C00 已通过的非阻塞项：Schema 8/8、回归 21/21、MySQL 8 幂等与规范 hash、CANCELLED 排除、迁移闭环和 validators。
- DF06 隔离实现已通过原分支独立验证，但最新 `int_main` 未包含正式创建事务行为，需要在本集成分支重新建立 RED/GREEN。

## 2026-08-16 C00 Contract Repair

- 变更：删除 `20260813_mes_active_order_qa_decoupling.sql` 可空脚本断言，避免用后续弱化脚本覆盖 C00 postflight 的 NOT NULL 正式合同。
- 变更：`MesQaPqcSchemaTest` 增加 `c00_backfill_approved_active_order_snapshot` 正向断言和 `UNIQUE_TASK_VERSION/tmp_c00_active_order_unique_task_version` 负向断言。
- 变更：C00 evidence 从“task version 回填 active-order 快照”修正为“批准 activeOrderId 清单回填 active-order 快照”。

## Frontend Contract Regression

- BDD: QA 检验项目结果类型保持单一正式枚举 -> Given QA API 已导出 `BOOLEAN | NUMERIC | TEXT` 的正式结果类型别名，When 静态合同检查保存 DTO，Then 应同时确认别名内容和 DTO 对该别名的引用，不应要求重复内联同一联合类型。
- RED: `node tests/e2e/qa-regulation-dcc-direct-contract-static.spec.cjs` -> FAIL；静态合同只接受内联联合类型，未接受当前等价且更易维护的正式类型别名。

## Branch Runtime Guard Contract Repair

- BDD: 合并前端口门禁使用当前正式槽位合同 -> Given worktree registry 已按 `2026-08-15-branch-runtime-v4` 登记 20..30 扩展槽位，When 运行合并前 `branch-runtime-port-guard.ps1`，Then 脚本和文档必须统一接受 `1..30` 且按扩展端口段校验，不能继续用 v3 的 `1..19` 拒绝当前登记。
- RED: `scripts\preflight\branch-runtime-port-guard.ps1` -> FAIL；集成分支仍为 v3，拒绝已按 v4 登记的 slot 20/21。

## 2026-08-16 GREEN / Regression

- GREEN: C00 MySQL 8 正向 dry-run/apply/idempotent/postflight/rollback dry-run -> PASS；反向缺 manifest fixture -> FAIL fast 且业务 DML 为 0。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest,MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest,MesTeamLeaderActiveOrderErpPlannedStartTest,MesTeamLeaderActiveOrderManualSortTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，45 tests，0 failures/errors/skips。
- GREEN: VAL13 后端聚合 Maven 17 类 -> PASS，127 tests，0 failures/errors/skips。
- GREEN: `pnpm install --frozen-lockfile` -> PASS，退出码 0，lockfile up to date。
- GREEN: 6 个前端静态合同 -> PASS：route-DCC、QA-DCC direct、DCC QA status、一线 PQC process contract、一线 PQC runtime、正式提交合同。
- GREEN: `pnpm ts:check` -> PASS，退出码 0。
- GREEN: database/backend/frontend evidence validators -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，frontend 8155 / backend 48155。
- Verification: UTF-8 29 files PASS；冲突标记扫描 PASS；C00 禁止项扫描 PASS；PQC `NUMBER/CHOICE` resultType 禁止项扫描 PASS；QA-DCC 生产源码禁止项扫描 PASS；`git diff --check` PASS，仅 CRLF 提示。
- Blocker/Exception: 真实写入 Playwright 路径按用户 2026-08-15 “不用测试，继续推进” 明确豁免；不记录为 PASS。

## 2026-08-16 Independent Verification / Closeout Prep

- GREEN: `/root/val13_final_independent` independent verification -> PASS；后端 17 类 127 tests、6 个前端静态合同、`pnpm ts:check`、database/backend/frontend validators、branch runtime guard、`git diff --check`、冲突标记和 C00 禁止项扫描均通过。
- Verification: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260815-frontline-pqc-c00-df06-integration --mode preview` -> BLOCKED；自动脚本会处理 linked worktree 删除，且主工作区仍有并行脏改动。按用户“保留 worktree”要求，不执行 cleanup apply；已人工删除本任务临时 evidence/fixture 文件，并保留独立验证报告。
- Merge preflight: 主工作区与本次合并有 9 个 tracked overlap，需先生成 patch 备份并只处理这些重叠文件；其它主工作区脏改动不属于本任务，不暂存、不回滚、不删除。
