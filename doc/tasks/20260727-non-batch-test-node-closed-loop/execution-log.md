# Execution Log

## User Intent

- 用户要求将测试管理中除 `批记录` 外的其他测试项，也改成类似批记录的“测试节点 + 闭环可重复执行”类型。

## Scope

- 当前任务只更新本机 `ruoyi-vue-pro` 当前租户测试管理数据中 `project <> '批记录'` 的现有测试项和直属检查点。
- 不访问远端服务器，不发布，不操作生产/备用环境。
- 不修改 `批记录` 项目测试项。

## BDD / TDD Evidence

- BDD: 非批记录测试项节点化 -> Given 当前测试管理存在非批记录项目测试项 / When 按系统节点重写测试方法和目标 / Then 测试人员能按项目、节点名称、方法和目标执行测试。
- BDD: 节点闭环可重复 -> Given 上次测试可能留下同名测试数据 / When 下一轮测试开始 / Then 每个节点先按固定样本或任务自有标识复位，再执行页面动作，最后清理、作废、恢复或保留可复用样本。
- BDD: 批记录不被修改 -> Given 批记录节点已按闭环完成 / When 改写其他项目测试项 / Then 批记录项目测试项数量和内容不被本任务修改。
- RED: `non-batch closed-loop readiness scan` -> FAIL, 写入前 `non_batch_cases=8`、`non_batch_targets=32`、`cases_with_3_methods=4`、`closed_loop_cases=0`、`internal_term_cases=5`。
- GREEN: `non-batch closed-loop rewrite SQL` -> PASS, 更新 8 个非批记录测试项和 32 个检查点。
- RED: `closed-loop keyword verification` -> FAIL, 首次写入后 `closed_loop_cases=7`，`智能排产节点：产能口径` 缺少清理表述。
- GREEN: `capacity cleanup wording patch` -> PASS, 补齐产能口径节点未保存弹窗清理和口径恢复表述。
- GREEN: `final non-batch verification SQL` -> PASS, `non_batch_cases=8`、`non_batch_targets=32`、`cases_with_3_methods=8`、`cases_with_4_targets=8`、`node_named_cases=8`、`closed_loop_cases=8`、`internal_term_cases=0`。
- GREEN: `project count verification` -> PASS, 当前租户项目数量为 `工艺路线=4`、`批记录=6`、`智能排产=4`。

## Activity

- in_progress: 已读取 database-schema-delivery、quality-assurance-test-suite、数据库规则、任务收尾规则、PowerShell 编码规则和经验索引。
- completed: 已读取 `docs/e2e-rules.md#测试管理测试节点闭环门禁` 和 `Codex Runner` 测试管理相关门禁。
- completed: schema 核对通过：`system_codex_test_case` 和 `system_codex_test_checkpoint` 必需字段存在。
- completed: 写入前当前租户项目统计：`工艺路线=4`、`批记录=6`、`智能排产=4`。
- completed: 将 8 个非批记录测试项改为节点闭环名称、3 行方法项和 4 个目标项。
- completed: 最终验证通过：8 个非批记录节点全部闭环，内部词扫描命中数为 0，批记录数量保持 6 个。
- GREEN: `validate_database_schema.py --evidence doc/tasks/20260727-non-batch-test-node-closed-loop/database-schema-evidence.md` -> PASS, database schema evidence is valid。
- GREEN: `git diff --check -- doc/tasks/20260727-non-batch-test-node-closed-loop` -> PASS, 无 whitespace error。
- completed: project-experience-consolidation 检查完成；既有 `docs/e2e-rules.md#测试管理测试节点闭环门禁` 已覆盖本次规则，不新增长期经验文档。
- ready_for_closeout: 数据更新与验证完成，等待无关工作区变更处理后再提交/推送。
- BLOCKER: closeout commit/push -> 当前 `int_main...origin/int_main [ahead 2]`，且存在非本任务前端文件和其他任务目录改动；为避免混入无关变更，本次未执行提交或推送。
