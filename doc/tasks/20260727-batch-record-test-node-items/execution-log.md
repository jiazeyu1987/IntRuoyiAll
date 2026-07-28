# Execution Log

## User Intent

- 用户要求先按 6 个批记录测试节点：解析、版本治理、绑定快照、批次任务、填写审批、归档追溯，分别拆解测试方法和测试目标，写入测试管理。
- 用户进一步确认每个测试节点必须可重复执行并形成闭环：测试前先清理或恢复固定样本，测试中按页面操作验证，测试后删除、作废、恢复或保留可复用样本，避免下次执行被残留数据阻塞；测试方法和目标必须业务测试人员能理解。

## Scope

- 当前任务只写入本机测试管理数据中的 `批记录` 项目测试项与直属检查点。
- 不访问远端服务器，不发布，不操作生产/备用环境。

## BDD / TDD Evidence

- BDD: 批记录测试节点可见 -> Given 当前测试管理需要按节点管理批记录测试项 / When 写入 6 个批记录节点测试项 / Then 每个节点都能按 `批记录` 项目检索到，并展示对应测试方法项和测试目标项。
- BDD: 测试目标完整 -> Given 每个测试节点代表批记录生命周期中的一个风险面 / When 节点测试项写入 / Then 每个节点至少包含 3 个方法项和 4 个可验证目标项。
- BDD: 写入范围受控 -> Given 测试管理已有其它项目测试项 / When 写入批记录节点测试项 / Then 非批记录项目测试项数量不被修改。
- BDD: 测试节点可重复闭环 -> Given 测试人员要重复执行批记录节点测试 / When 上次执行留下同名测试数据或固定样本状态变化 / Then 每个节点的方法项都先复位、再执行、再验证、最后清理或恢复，下一次测试不会被残留数据阻塞。
- RED: `SELECT missing_expected_nodes ...` -> FAIL, 6 个期望节点均缺失。
- RED: `mysql seed with top-level IF` -> FAIL, MySQL 顶层 `IF` 语法不合法，未写入真实表。
- RED: `mysql seed with temp collation=utf8mb4_unicode_ci` -> FAIL, 临时表与业务表 collation 不一致，未写入真实表。
- GREEN: `mysql seed with temp collation=utf8mb4_0900_ai_ci` -> PASS, 插入 6 个测试项和 24 个检查点。
- GREEN: `final verification SQL` -> PASS, 6 个节点均为 `ENABLE/SEQUENTIAL/parallelSafe=false`，每个 3 个方法项和 4 个目标项。
- RED: `internal term scan` -> FAIL, 当前文案命中 `接口/ID/hash/CELL_RULE/task/open/REVIEW/API/JSON/WORM` 等程序员视角词。
- GREEN: `business-readable rewrite SQL` -> PASS, 更新 6 个测试项方法/测试数据，替换 24 个目标项。
- GREEN: `internal term scan` -> PASS, 程序员视角词命中数为 0，6 个节点和 24 个目标项仍完整。
- RED: `closed-loop readiness scan` -> FAIL, 6 个节点中 `closed_loop_ready_nodes=0`，旧文案未明确固定样本、清理和恢复闭环。
- GREEN: `closed-loop rewrite SQL` -> PASS, 6 个节点均补充前置复位、固定样本、执行验证、清理/作废/恢复/保留闭环。
- GREEN: `closed-loop verification SQL` -> PASS, `node_cases=6`、`node_targets=24`、`nodes_with_3_methods=6`、`nodes_with_4_targets=6`、`closed_loop_nodes=6`、`internal_term_hits=0`。

## Activity

- in_progress: 已读取 database-schema-delivery 技能、database-contract、数据库规则、任务收尾规则、PowerShell 编码规则、服务器访问规则、备份恢复规则。
- completed: 已读取经验索引和命中的 `Codex Runner` 测试管理门禁。
- completed: schema 核对通过：`system_codex_test_case` 和 `system_codex_test_checkpoint` 必需字段存在。
- completed: 写入前当前租户项目统计：`工艺路线=4`，`智能排产=4`，6 个批记录节点均缺失。
- completed: 写入当前租户 `tenant_id=1`：测试项 6 个，检查点 24 个。
- completed: 写入后当前租户项目统计：`工艺路线=4`，`批记录=6`，`智能排产=4`。
- completed: project-experience-consolidation 检查完成；本次是一次性测试项数据落地，已有测试管理 schema 和租户边界门禁覆盖，不新增长期经验文档。
- completed: 按用户要求将测试方法和测试目标改为业务测试人员可按页面观察判断的表述，删除内部字段、接口、状态码、hash、英文审批状态等程序员视角内容。
- completed: 按用户确认将 6 个节点改为闭环测试项，每个节点包含固定样本或任务自有标识、前置复位、页面动作、页面结果验证和测后清理/恢复。
- completed: project-experience-consolidation 检查完成；本次闭环规则适合长期复用，已合并到 `docs/e2e-rules.md#测试管理测试节点闭环门禁`，并补充 `docs/experience-index.md` 关键词路由。
- GREEN: `validate_database_schema.py --evidence doc/tasks/20260727-batch-record-test-node-items/database-schema-evidence.md` -> PASS, database schema evidence is valid。
- GREEN: `rg 测试管理测试节点闭环 docs/experience-index.md docs/e2e-rules.md` -> PASS, 长期门禁和索引关键词可命中。
- GREEN: `git diff --check -- <task docs and experience files>` -> PASS, 仅出现 Git 行尾转换 warning，无 whitespace error。
- ready_for_closeout: 数据更新和验证已完成；任务文档已更新为闭环版本，等待按项目收尾规则处理提交/推送。
- BLOCKER: closeout commit/push -> 当前 `int_main...origin/int_main [ahead 1]`，且存在 BPM 测试、附件任务目录等非本任务改动；为避免混入无关变更，本次未执行提交或推送。
