# Database Schema Evidence

## Data Change Goal

写入并修订当前本机测试管理中的 6 个批记录测试节点项，每个节点包含业务可读的测试方法项、测试目标项，以及可重复执行的前置复位、固定样本、清理/恢复闭环。

## Affected Entities

- `system_codex_test_case`
- `system_codex_test_checkpoint`

## Database Engine And Tooling

- Database engine: MySQL 8 in local Docker container `int-ruoyi-mysql`
- Database: `ruoyi-vue-pro`
- Tooling: container-local `mysql` client with UTF-8 input

## Migration

- No schema migration was created.
- This task is a scoped data seed/update for existing testing-management tables.
- The write path uses current schema columns verified from `information_schema.columns`.

## Data Safety

- Scope is current local tenant `tenant_id=1`.
- Writes are constrained to `project='批记录'` and exact node test item names.
- Existing matching node names are updated idempotently; missing names are inserted.
- 闭环修订只更新 6 个目标节点的 `method_text`、`test_data_text` 和对应 24 个检查点文案。
- Non-target projects are only counted for before/after verification.

## Rollback / Recovery

- Rollback can soft-delete inserted or updated node items by exact names under `tenant_id=1 AND project='批记录'`.
- Prior deleted batch/DCC legacy items are not restored by this task.

## BDD Scenarios

- BDD: 批记录测试节点可见 -> Given 当前测试管理需要按节点管理批记录测试项 / When 写入 6 个批记录节点测试项 / Then 每个节点都能按 `批记录` 项目检索到，并展示对应测试方法项和测试目标项。
- BDD: 测试目标完整 -> Given 每个测试节点代表批记录生命周期中的一个风险面 / When 节点测试项写入 / Then 每个节点至少包含 3 个方法项和 4 个可验证目标项。
- BDD: 写入范围受控 -> Given 测试管理已有其它项目测试项 / When 写入批记录节点测试项 / Then 非批记录项目测试项数量不被修改。
- BDD: 测试节点可重复闭环 -> Given 测试人员要重复执行批记录节点测试 / When 上次执行留下同名测试数据或固定样本状态变化 / Then 每个节点的方法项都先复位、再执行、再验证、最后清理或恢复，下一次测试不会被残留数据阻塞。

## Verification Evidence

- Schema: `system_codex_test_case` 存在 `id/name/project/method_text/test_data_text/default_execution_mode/parallel_safe/status/sort/deleted/tenant_id`；`system_codex_test_checkpoint` 存在 `id/case_id/sort/name/expected_text/deleted/tenant_id`。
- RED: 6 个目标节点写入前均缺失。
- GREEN: 写入使用 `utf8mb4_0900_ai_ci` 临时表，插入测试项 6 行，插入检查点 24 行。
- GREEN: 业务可读化修订更新测试项 6 行，替换测试目标 24 行。
- RED: 闭环准备度扫描显示 `node_cases=6`、`closed_loop_ready_nodes=0`，旧文案未明确固定样本、清理和恢复闭环。
- GREEN: 闭环修订更新 6 个测试项和 24 个检查点，补充前置复位、固定样本、页面动作、页面验证和测后清理/恢复。
- Final verification: `final_node_cases=6`，`final_node_checkpoints=24`。
- Structure verification: 每个节点 `method_item_count=3`，`target_item_count=4`，状态 `ENABLE`，默认执行方式 `SEQUENTIAL`，`parallelSafe=false`。
- Closed-loop verification: `node_cases=6`，`node_targets=24`，`nodes_with_3_methods=6`，`nodes_with_4_targets=6`，`closed_loop_nodes=6`。
- Business-readable verification: 内部词扫描命中数为 0，扫描词包含 `接口/ID/hash/CELL_RULE/task/open/REVIEW/API/JSON/WORM` 等。
- Non-target verification: 当前租户非目标项目保持 `工艺路线=4`、`智能排产=4`。

## Blockers

无。
