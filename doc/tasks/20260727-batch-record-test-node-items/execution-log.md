# Execution Log

## User Intent

- 用户要求先按 6 个批记录测试节点：解析、版本治理、绑定快照、批次任务、填写审批、归档追溯，分别拆解测试方法和测试目标，写入测试管理。

## Scope

- 当前任务只写入本机测试管理数据中的 `批记录` 项目测试项与直属检查点。
- 不访问远端服务器，不发布，不操作生产/备用环境。

## BDD / TDD Evidence

- BDD: 批记录测试节点可见 -> Given 当前测试管理需要按节点管理批记录测试项 / When 写入 6 个批记录节点测试项 / Then 每个节点都能按 `批记录` 项目检索到，并展示对应测试方法项和测试目标项。
- BDD: 测试目标完整 -> Given 每个测试节点代表批记录生命周期中的一个风险面 / When 节点测试项写入 / Then 每个节点至少包含 3 个方法项和 4 个可验证目标项。
- BDD: 写入范围受控 -> Given 测试管理已有其它项目测试项 / When 写入批记录节点测试项 / Then 非批记录项目测试项数量不被修改。
- RED: `SELECT missing_expected_nodes ...` -> FAIL, 6 个期望节点均缺失。
- RED: `mysql seed with top-level IF` -> FAIL, MySQL 顶层 `IF` 语法不合法，未写入真实表。
- RED: `mysql seed with temp collation=utf8mb4_unicode_ci` -> FAIL, 临时表与业务表 collation 不一致，未写入真实表。
- GREEN: `mysql seed with temp collation=utf8mb4_0900_ai_ci` -> PASS, 插入 6 个测试项和 24 个检查点。
- GREEN: `final verification SQL` -> PASS, 6 个节点均为 `ENABLE/SEQUENTIAL/parallelSafe=false`，每个 3 个方法项和 4 个目标项。

## Activity

- in_progress: 已读取 database-schema-delivery 技能、database-contract、数据库规则、任务收尾规则、PowerShell 编码规则、服务器访问规则、备份恢复规则。
- completed: 已读取经验索引和命中的 `Codex Runner` 测试管理门禁。
- completed: schema 核对通过：`system_codex_test_case` 和 `system_codex_test_checkpoint` 必需字段存在。
- completed: 写入前当前租户项目统计：`工艺路线=4`，`智能排产=4`，6 个批记录节点均缺失。
- completed: 写入当前租户 `tenant_id=1`：测试项 6 个，检查点 24 个。
- completed: 写入后当前租户项目统计：`工艺路线=4`，`批记录=6`，`智能排产=4`。
