# Database Schema Evidence

## Data Change Goal

将当前本机测试管理中除 `批记录` 外的现有测试项，改写为业务可读、可重复执行的“测试节点 + 闭环”类型。

## Affected Entities

- `system_codex_test_case`
- `system_codex_test_checkpoint`

## Database Engine And Tooling

- Database engine: MySQL 8 in local Docker container `int-ruoyi-mysql`
- Database: `ruoyi-vue-pro`
- Tooling: container-local `mysql` client with UTF-8 input

## Migration

- No schema migration was created.
- This task is a scoped data update for existing testing-management tables.
- The write path uses current schema columns verified from `information_schema.columns`.

## Data Safety

- Scope is current local tenant `tenant_id=1`.
- Writes are constrained to existing rows where `project <> '批记录'`.
- `批记录` project rows are excluded from update statements and only counted for verification.
- Updated entities: 8 existing non-batch test cases and their 32 existing checkpoints.

## Rollback / Recovery

- Recovery can restore the prior test item names, method text, test data text, and checkpoint text for the exact 8 case IDs captured before this task.
- No schema rollback is required because no schema change was performed.

## BDD Scenarios

- BDD: 非批记录测试项节点化 -> Given 当前测试管理存在非批记录项目测试项 / When 按系统节点重写测试方法和目标 / Then 测试人员能按项目、节点名称、方法和目标执行测试。
- BDD: 节点闭环可重复 -> Given 上次测试可能留下同名测试数据 / When 下一轮测试开始 / Then 每个节点先按固定样本或任务自有标识复位，再执行页面动作，最后清理、作废、恢复或保留可复用样本。
- BDD: 批记录不被修改 -> Given 批记录节点已按闭环完成 / When 改写其他项目测试项 / Then 批记录项目测试项数量和内容不被本任务修改。

## Verification Evidence

- Schema: `system_codex_test_case` 存在 `id/name/project/method_text/test_data_text/default_execution_mode/parallel_safe/status/sort/deleted/tenant_id`；`system_codex_test_checkpoint` 存在 `id/case_id/sort/name/expected_text/deleted/tenant_id`。
- RED: 写入前 `non_batch_cases=8`、`non_batch_targets=32`、`cases_with_3_methods=4`、`closed_loop_cases=0`、`internal_term_cases=5`。
- GREEN: 闭环改写更新非批记录测试项 8 行，更新检查点 32 行。
- RED: 首次写入后 `closed_loop_cases=7`，`智能排产节点：产能口径` 缺少清理表述。
- GREEN: 补齐 `智能排产节点：产能口径` 清理表述后，最终 `closed_loop_cases=8`。
- Final verification: `non_batch_cases=8`，`non_batch_targets=32`，`cases_with_3_methods=8`，`cases_with_4_targets=8`，`node_named_cases=8`，`closed_loop_cases=8`，`internal_term_cases=0`。
- Project count verification: 当前租户保持 `工艺路线=4`、`批记录=6`、`智能排产=4`。

## Blockers

无。
