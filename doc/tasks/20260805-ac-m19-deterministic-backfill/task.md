# AC-M19 确定性批记录聚合回填修复

## Task Goal

修复 AC-M19“写入工序批记录表单”不完全符合项：全部已确认报工必须按明确策略汇总到正式逐工序批记录，代表事件丢数、无聚合策略、缺正式绑定或重复回填必须被阻塞。

## Milestones

- [x] 建立 AC-M19 聚合回填任务记录和适用门禁。
- [x] 补充 RED 测试，复现代表事件/单分配导致多次报工丢数的问题。
- [x] 实现订单工序级已确认报工聚合、字段策略和聚合版本幂等。
- [x] 更新完成状态持久化字段，保留聚合源与幂等证据。
- [x] 运行目标后端测试、schema 测试和技能 evidence 校验。
- [x] 完成收尾记录；如存在非本任务脏改动或推送阻塞，明确记录 blocker。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderBatchRecordBackfillServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesProcessPoolTeamLeaderSchemaTest" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-ac-m19-deterministic-backfill/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m19-deterministic-backfill/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260805-ac-m19-deterministic-backfill/database-schema-evidence.md`

## Current Status

completed - implementation, schema updates, static checks, evidence validation, Maven/JUnit verification, cleanup apply, and origin synchronization are complete; verification worktree is intentionally retained with no running processes because it contains uncommitted verification-only baseline patches and should not be force-removed without explicit deletion approval.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复点定位在正式批记录回填数据模型、聚合策略、幂等键和完成状态追溯。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\task-closeout-rules.md`、`docs\backend-development.md`、`docs\database-rules.md`、`docs\powershell-encoding.md`、`docs\powershell-memory.md`、`docs\worktree-restrictions.md`。
- 已读取 `docs\experience-index.md`；本任务命中后端开发、schema 变更、PowerShell/Maven `-D` 参数引号、脏工作区边界、任务收尾和正式批记录来源门禁。
- 已按 `project-experience-consolidation` 将 Maven 页面文件不足/并发进程门禁归并到 `docs\powershell-memory.md`，未新建长期经验文档。
- 已按 `project-experience-consolidation` 将验证 worktree 编译基线差异门禁归并到 `docs\worktree-memory.md`，未新建长期经验文档。
- 正式批记录字段必须来自工序设置逐工序批记录绑定，不得由 `formBindings`、默认 `MAIN`、工序开始配置或代表事件替代。
- schema 变更必须基于当前迁移/DO/测试证据，禁止猜测字段或运行未验证 SQL。
- 当前 `E:\IntRuoyi` 已存在大量非本任务脏改动；本任务只修改 AC-M19 相关源码、测试、SQL 和任务文档，不回滚、不覆盖、不暂存其它并行改动。
- 2026-08-05 已在新 worktree `D:\IntRuoyiWorktree\ac-m19-verify-20260805` / `codex/ac-m19-verify-20260805` 完成 AC-M19 定向 Maven 验证；验证 worktree 仅额外同步主工作区已有 QA/PQC 编译基线以解除非 AC-M19 编译阻塞。

## Cleanup Keep

- doc/tasks/20260805-ac-m19-deterministic-backfill/bug-regression-evidence.md
- doc/tasks/20260805-ac-m19-deterministic-backfill/backend-api-evidence.md
- doc/tasks/20260805-ac-m19-deterministic-backfill/database-schema-evidence.md
