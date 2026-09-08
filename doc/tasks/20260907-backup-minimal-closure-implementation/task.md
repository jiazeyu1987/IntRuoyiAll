# 备份恢复最小闭环实施

## Task Goal

在 `D:\IntRuoyiWorktree\backup_20260907` 按 `E:\IntRuoyi\doc\tasks\20260907-backup-full-incremental-recovery-plan\minimal-development-plan.md` 实现首版闭环：全量备份、真实 MySQL binlog 与 MinIO/DCC 增量、恢复演练、备份链展示和一键审查证据 ZIP。

## Milestones

1. M1：补齐全载荷 checksum 与恢复前完整性校验。已完成。
2. M2：实现 FULL 类型、全量计划和停服一致性窗口。已完成。
3. M3：实现 INCREMENTAL binlog/object delta、父链和增量计划。已完成。
4. M4：实现完整链恢复演练与 RECOVERABLE 状态门禁。已完成。
5. M5：实现审查证据 ZIP 后端导出、权限和前端入口。已完成。
6. 运行定向回归、文档验证和无删除 cleanup。已完成本地定向验证，真实环境/E2E 未授权不执行。

## Expected Verification

- PowerShell/Python 合同测试覆盖 FULL、INCREMENTAL、payload hash、断链和恢复前零写入。
- Java 测试覆盖双计划、候选状态和证据导出。
- 前端静态合同和目标文件 ESLint 通过；全仓 `vue-tsc --noEmit` 若失败，必须用主工作区基线复跑证明是否由本任务引入。
- 不运行真实备份、远程服务器、数据库写入或 Playwright E2E，除非用户后续明确授权。
- 所有生产行为按相同测试命令记录 RED/GREEN/REGRESSION。

## Current Status

ready_for_closeout

静态代码分析与缺陷修复已完成。相关 Python 回归 160 项、Java 定向测试 46 项、后端 package、PowerShell 解析、前端静态合同和目标文件 ESLint、迁移 policy gate 均通过。真实备份、数据库迁移执行、远程恢复和 E2E 仍未授权。

## Worktree Runtime

- Path: `D:\IntRuoyiWorktree\backup_20260907`
- Branch: `codex/backup_20260907`
- Profile: `int_main`
- Slot: `28`
- Frontend: `8162`
- Backend: `48162`
- 本轮不启动服务，不占用 `48081`。

## BDD Scenarios

- BDD: BDD-MIN-01 全量备份 -> Given 前置完整且形成无写入窗口 / When 管理员执行 FULL / Then MySQL、MinIO/DCC 和运行配置完成全载荷校验后原子发布。
- BDD: BDD-MIN-02 增量备份 -> Given 有效 FULL 和连续父点 / When 管理员执行 INCREMENTAL / Then 生成真实 binlog 与对象 delta，不执行全量替代。
- BDD: BDD-MIN-03 恢复演练 -> Given FULL+I1+I2 完整 / When 在测试槽位恢复 I2 / Then 数据库、对象、登录和 DCC 文件通过后才标记 RECOVERABLE。
- BDD: BDD-MIN-04 导出通过证据 -> Given 最新链完整、未过期且已演练 / When 点击导出 / Then ZIP 固定内容、hash、脱敏和 PASS 摘要正确。
- BDD: BDD-MIN-05 导出阻塞证据 -> Given 当前存在缺口 / When 点击导出 / Then 仍导出 BLOCKED/FAIL 摘要和缺失项，不生成默认 PASS。

## Scope Boundaries

- 不实现生产恢复、备用服切换、新数据库表、通用 writer registry、PDF/XLSX、包级签名、归档副本、legal hold/处置平台、secret readiness 平台或 Linux parity。
- PowerShell 路径为首版唯一执行路径；不支持的执行模式明确 BLOCKED。
- 不把现有日全量改名为增量，不用 mock/API-only 代替真实恢复能力。

## 设计约束检查

- `fallback/降级`：禁止；增量前置缺失时阻断。
- `数据安全`：恢复前完整性校验，损坏载荷不得触发目标写。
- `敏感信息`：证据 ZIP、日志和测试输出不含 secret。
- `最小实现`：复用现有 config、manifest、operation/report、恢复演练和备份计划页面，不新增第三方依赖。
- `并行资产`：只修改本 worktree 和本任务文件，不触碰其它 worktree、端口或进程。

## Blockers

- 真实 MySQL binlog、NAS、MinIO、远程主机和演练槽位验证需要后续明确授权；本轮只完成代码和隔离测试。
- 未授权 Git commit/push，完成代码验证后任务最多进入 `ready_for_closeout`。
- 全仓 TypeScript 基线仍有 4 个 `src/views/dcc/controlled-file/upload/index.vue` 的 `versionNo` 可空错误；主工作区同样复现，本任务未修改该文件。
- closeout preview 发现 `int_main` 已前进且主工作区有其它任务改动，无法安全 ff-only 合并；本轮未获 commit/merge/worktree removal 授权，因此不执行 cleanup apply。

## Milestone Evidence

- M1 GREEN：本地恢复文件递归 checksum、MySQL dump gzip/SHA-256 预校验、对象仓库 SHA-256 预校验已实现。
- M2 GREEN：FULL/INCREMENTAL 显式 `BackupKind`、全量/增量独立调度、全量链身份和停服窗口合同已实现并通过测试。
- M3 GREEN：增量路径要求连续 MySQL binlog 段、对象 delta/tombstone、base/parent 链，缺基线或断链时 fail fast，不执行全量 fallback。
- M4 GREEN：恢复演练候选和 `RECOVERABLE` 状态由 manifest、checksum、DCC manifest、rehearsal report 共同证明；未演练或演练未通过显示不可恢复原因。
- M5 GREEN：后端生成审查证据 ZIP，前端仅调用 blob 下载；ZIP 固定 9 个文件、包内 hash 可重算、secret 脱敏、BLOCKED 不伪装 PASS。
- M6 GREEN（上一轮基线）：相关 Python 回归 154 项、Java 44 项通过。
- M7 GREEN：相关 Python 回归 160 项、Java 46 项、后端 package、PowerShell/内嵌 Python 语法、配置 JSON、分支端口门禁、前端静态合同、目标 ESLint 和 migration policy gate 通过。
- M7：静态代码正确性分析、缺陷修复与复验。已完成。

## Cleanup Keep

- doc/tasks/20260907-backup-minimal-closure-implementation/test-report.md
- doc/tasks/20260907-backup-minimal-closure-implementation/backend-api-evidence.md
- doc/tasks/20260907-backup-minimal-closure-implementation/frontend-feature-evidence.md
- doc/tasks/20260907-backup-minimal-closure-implementation/database-schema-evidence.md
- doc/tasks/20260907-backup-minimal-closure-implementation/bug-regression-evidence.md

## Cleanup Candidates

- IntRuoyiBackend/.pytest-temp/
