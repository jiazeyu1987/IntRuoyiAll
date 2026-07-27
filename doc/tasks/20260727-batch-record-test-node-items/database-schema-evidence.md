# Database Schema Evidence

## Data Change Goal

写入当前本机测试管理中的 6 个批记录测试节点项，每个节点包含测试方法项和测试目标项。

## Affected Entities

- `system_codex_test_case`
- `system_codex_test_checkpoint`

## Database Engine And Tooling

- Database engine: MySQL 8 in local Docker container `int-ruoyi-mysql`
- Database: `ruoyi-vue-pro`
- Tooling: container-local `mysql` client with UTF-8 input

## Data Safety

- Scope is current local tenant `tenant_id=1`.
- Writes are constrained to `project='批记录'` and exact node test item names.
- Existing matching node names are updated idempotently; missing names are inserted.
- Non-target projects are only counted for before/after verification.

## Rollback / Recovery

- Rollback can soft-delete inserted or updated node items by exact names under `tenant_id=1 AND project='批记录'`.
- Prior deleted batch/DCC legacy items are not restored by this task.

## BDD Scenarios

- BDD: 批记录测试节点可见 -> Given 当前测试管理需要按节点管理批记录测试项 / When 写入 6 个批记录节点测试项 / Then 每个节点都能按 `批记录` 项目检索到，并展示对应测试方法项和测试目标项。
- BDD: 测试目标完整 -> Given 每个测试节点代表批记录生命周期中的一个风险面 / When 节点测试项写入 / Then 每个节点至少包含 3 个方法项和 4 个可验证目标项。
- BDD: 写入范围受控 -> Given 测试管理已有其它项目测试项 / When 写入批记录节点测试项 / Then 非批记录项目测试项数量不被修改。

## Verification Evidence

- Schema: `system_codex_test_case` 存在 `id/name/project/method_text/test_data_text/default_execution_mode/parallel_safe/status/sort/deleted/tenant_id`；`system_codex_test_checkpoint` 存在 `id/case_id/sort/name/expected_text/deleted/tenant_id`。
- RED: 6 个目标节点写入前均缺失。
- GREEN: 写入使用 `utf8mb4_0900_ai_ci` 临时表，插入测试项 6 行，插入检查点 24 行。
- Final verification: `final_node_cases=6`，`final_node_checkpoints=24`。
- Structure verification: 每个节点 `method_item_count=3`，`target_item_count=4`，状态 `ENABLE`，默认执行方式 `SEQUENTIAL`，`parallelSafe=false`。
- Non-target verification: 当前租户非目标项目保持 `工艺路线=4`、`智能排产=4`。

## Blockers

无。
