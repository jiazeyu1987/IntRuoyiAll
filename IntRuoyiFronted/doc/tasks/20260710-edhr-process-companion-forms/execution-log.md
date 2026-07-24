# 执行日志

BDD: 同工序表单分组展示 -> Given 一个工序包含主表和三类辅助表单 / When 用户打开批次详情 / Then 左侧只显示一个工序组并列出 4 个可区分表单。

BDD: 辅助表单显示真实状态 -> Given 同工序表单处于待打开、已提交或完成状态 / When 用户查看工序组 / Then 每个表单显示自己的状态和门禁原因。

BDD: 点击槽位打开对应表单 -> Given 用户选择损耗单或检验单 / When 点击表单项 / Then 使用该任务 ID 和报告 ID 打开对应执行页。

BDD: 表单页返回批次继续填写 -> Given 用户从批次详情进入表单页 / When 点击返回批次执行 / Then 返回原批次并聚焦原任务。

GREEN: previous-task-check -> PASS，上一前端任务已完成并提交。

RED: `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> FAIL，当前批次详情仍逐条渲染任务，缺少 `processTaskGroups` 工序组视图。

GREEN: experience-preflight -> PASS，官方 `login-preflight.mjs` 使用系统 Chrome 真实登录 `测试租户/aoteman` 并进入本机批次详情页。

GREEN: `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS，同工序分组、四类槽位标签、独立状态/门禁和返回上下文契约通过。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。

GREEN: `node tests/e2e/edhr-batch-process-companion-forms-real.e2e.js`（`READONLY_ADMIN=1`、`STRUCTURAL_ONLY=1`）-> PASS，管理员只读批次 `900000000480` 的 14 个工序按 `routeProcessId` 渲染，任务聚焦和返回 `batchExecutionId/batchTaskId` 上下文通过，MES 写请求为 0。

REGRESSION: `edhr-batch-process-card-density-static.spec.js`、`edhr-batch-process-display-sort-static.spec.js` -> PASS。

BLOCKER: interrupted-by-main-fill-layout-request -> 用户插入同页主区域填满视口的更高优先级布局修复；本任务尚未进入生产代码实现，暂停以避免两个同页布局任务交叉修改。影响：工序辅助表单分组展示暂未交付。

RESUMED: 主区域布局任务已完成并提交，本任务继续执行。

BLOCKER: full-companion-real-data -> 测试租户批次列表为空，管理员租户仅有单 MAIN 槽位任务；只读 SQL 对全部租户查询确认不存在 `COUNT(DISTINCT form_slot_type) > 1` 的历史工序任务。影响：无法在不创建测试数据的前提下执行四槽位真实页面验收；默认严格真实 E2E 会失败并暴露该前置缺口。

GREEN: task-closeout-cleanup -> PASS，仅保留 `task.md` 与 `execution-log.md`，前端一次性证据和本任务 E2E 输出目录已清理。
