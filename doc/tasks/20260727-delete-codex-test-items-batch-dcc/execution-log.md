# Execution Log

## User Intent

- 用户确认测试管理应新增“测试节点”维度，当前第一步要求删除项目为 `批记录` / `文控` 的现有测试项。

## Scope

- 仅处理当前测试管理数据中的 `批记录` / `文控` 项目测试项及其直属检查点。
- 不操作远端服务器、生产数据、发布、备份或共享运行态。

## BDD / TDD Evidence

- BDD: 删除目标项目测试项 -> Given 测试管理中存在项目为 `批记录` 或 `文控` 的测试项 / When 按项目字段执行删除 / Then 目标项目测试项不再可见且非目标项目测试项保留。
- BDD: 删除范围精确受控 -> Given 测试管理中存在其它项目测试项 / When 删除 `批记录` / `文控` 项目测试项 / Then 其它项目测试项数量和名称不应被误删。
- RED: `node doc\tasks\20260727-delete-codex-test-items-batch-dcc\delete-batch-dcc-codex-test-items.e2e.cjs` -> FAIL, expected reason: 本机登录页提示后端服务响应超时，页面删除路径未进入删除动作。
- GREEN: `docker exec int-ruoyi-mysql ... mysql --default-character-set=utf8mb4 ruoyi-vue-pro` -> PASS, 使用当前本地库事务删除当前租户目标项目测试项和直属检查点。

## Activity

- in_progress: 已读取数据库交付技能、数据库规则、任务收尾规则和经验索引。
- in_progress: 已创建当前任务文档，准备核对测试管理表结构和目标数据。
- completed: 已读取本地运行态、登录、E2E、PowerShell 编码、服务器访问、备份恢复规则；未访问远端服务器。
- completed: schema 核对通过：`system_codex_test_case` 存在 `id/name/project/deleted/tenant_id`，`system_codex_test_checkpoint` 存在 `id/case_id/name/deleted/tenant_id`。
- completed: 删除前当前库未删除项目统计：`工艺路线=4/16 checkpoints`，`批记录=6/24 checkpoints`，`文控=6/12 checkpoints`，`智能排产=4/16 checkpoints`。
- completed: 删除范围限定为当前登录租户 `tenant_id=1`，目标项 10 个：批记录 ID 2-7，文控 ID 14-17；未发现运行中的目标执行快照。
- completed: 事务执行结果：删除直属检查点 32 行，软删除测试项 10 行。
- completed: 删除后复核：当前租户目标项目测试项 0，目标直属检查点 0，非目标测试项 8。
- completed: 跨租户保留项：`tenant_id=122` 下 `文控` 2 条未删除，原因是不属于当前 `芋道源码/admin` 页面上下文。
- completed: 已清理任务临时脚本 `delete-batch-dcc-codex-test-items.e2e.cjs` 和失败截图，仅保留 summary、task、execution-log、verification-report。
- completed: project-experience-consolidation 检查完成；本次经验已由现有数据库租户边界和测试管理 schema 门禁覆盖，不新增长期经验文档。
