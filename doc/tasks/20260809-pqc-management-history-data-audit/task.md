# PQC 管理历史数据只读核查

## Task Goal

- 只读核对 `PQC组长 > PQC管理` 当前空列表是否仍有历史 PQC 提交数据。
- 区分数据被删除、提交日期筛选、人员范围或角色权限导致的不可见。
- 不修改业务数据、权限、人员范围或生产代码。

## Milestones

- [x] M1：读取项目规则、经验门禁和既有 PQC 测试数据证据。
- [x] M2：只读核对当前本机数据库中的历史 PQC 任务、事件、记录、提交日期和可见范围。
- [x] M3：形成结论和验证报告，进入任务收尾。

## Expected Verification

- 核对已知历史记录 `event 160`、`event 181..185` 及其 PQC task/record 是否仍存在。
- 汇总当前租户 PQC 正式提交事件的日期分布和最近记录。
- 核对 `芋道源码/admin` 的 PQC 人员范围、目标检验员和 `pqc_permission` 角色，不输出任何凭据。
- 本次为只读诊断，不执行数据恢复、日期更新或页面写入型 E2E。

## Applicable Gates

- `docs/backend-development.md#MES PQC组长人员范围与管理数据可见性门禁`：PQC 管理按当前组长唯一启用人员范围和事件 `server_submit_time` 读取；租户一致不等于列表可见。
- `docs/database-rules.md`：数据库核查必须使用真实 schema 和精确只读查询，不得从旧文档直接推断当前数据状态。
- `docs/experience-index.md`：命中 `PQC管理 No Data`、`submitDate`、`server_submit_time`、`pqc_permission` 和人员范围经验路由。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，分别核对历史数据、日期筛选和正式人员范围。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：只读核查已完成；历史 PQC 数据仍在，当前日期没有提交数据，cleanup preview/apply 均通过且无删除项。
