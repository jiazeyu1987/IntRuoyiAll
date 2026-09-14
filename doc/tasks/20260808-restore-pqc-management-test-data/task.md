# 恢复 PQC 管理测试数据

## Task Goal

恢复 `PQC组长 > PQC管理` 页面应展示的测试数据，确认缺失原因是测试数据、人员范围、权限还是接口筛选，并按正式 PQC 组长人员范围链路恢复，不引入 fallback 或绕过。

## Milestones

- [x] M1：只读核对 PQC 管理页面、接口、数据库和历史测试数据证据。
- [x] M2：按正式链路恢复缺失测试数据或配置，并记录影响范围。
- [x] M3：验证 PQC 管理页面重新出现目标测试数据，核对接口和数据库一致。
- [x] M4：完成收尾、清理任务自有临时产物并记录最终结论。

## Expected Verification

- 只读核对 `mes_pro_process_pool_event`、PQC 记录、实际检验员、`pqc_permission` 角色和 `mes_pro_process_pool_team_leader_scope` 唯一启用范围。
- 如果需要写入数据，先核对目标表结构和租户范围，使用最小正式链路恢复，不直接伪造页面结果。
- 使用指定 PQC 组长真实页面或同一运行态接口确认 `PQC管理` 列表非空且显示恢复记录。
- 记录无法执行的验证前置条件及影响。

## Applicable Gates

- `docs/backend-development.md#mes-pqc组长人员范围与管理数据可见性门禁`：PQC 管理只按当前组长的唯一启用人员范围读取；不得给管理员全量可见 fallback，不得直接改事件实际检验员或插入多组长并存范围。
- `docs/database-rules.md`：数据修复前必须核对真实 schema、租户范围、影响行数和回滚/复核证据。
- `docs/experience-index.md`：命中 `PQC组长人员管理`、`PQC管理不可见`、`pqc_permission`、`leader_type=PQC`、`scope_type=EMPLOYEE` 经验路由。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先定位正式数据链路缺口再恢复。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：5 条 `CODX-PQC-20260807-SP` PQC 管理测试数据已恢复到 2026-08-08 默认提交日期；接口和真实页面均验证可见，cleanup preview/apply 均通过且无删除项。
