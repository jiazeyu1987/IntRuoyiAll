# 执行日志

BDD: 班次产能显示整数 -> Given 工序班次产能为带浮点误差的数值 `4799.99998` When 用户查看排产员工作台的工序列表 Then 班次产能显示为带千分位的整数 `4,800`，不显示小数部分。

BDD: 其他数量精度不受影响 -> Given 未完需求或今日报工包含有效小数 When 用户查看工序列表 Then 这些列继续使用原有通用数量格式，不因班次产能调整而被强制取整。

BDD: 开启夜班显示白夜班与双倍标识 -> Given 工序当前为白班且夜班未开启 When 用户开启夜班并保存成功 Then 班次状态显示“白夜班”，班次产能数值后显示绿色 `X2`，原始产能数值不被前端重复乘二。

BDD: 关闭夜班恢复白班 -> Given 工序当前显示“白夜班”和绿色 `X2` When 用户关闭夜班并保存成功 Then 班次状态显示“白班”，班次产能后的 `X2` 消失。

BDD: 混合夜班不误报双倍产能 -> Given 同一工序聚合数据的夜班配置为混合状态 When 用户查看工序列表 Then 班次状态显示“混合”，班次产能后不显示统一 `X2`。

REPRODUCTION: 用户截图中的“班次产能”显示为 `4,799.99998`、`2,999.999995`、`2,500.00002` 等浮点数，并因列宽发生换行。

ROOT_CAUSE: `src/views/mes/pro/scheduler-workbench/index.vue` 的班次产能列调用通用 `formatNumber`，该函数允许最多 6 位小数；页面已有 `formatIntegerNumber`，但仅用于在制单数。班次状态直接展示后端原始 `shiftStatus`，夜班开关没有统一映射为“白夜班”，班次产能单元格也没有根据有效夜班配置展示倍数标识。

RED: `node tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js` -> FAIL，班次产能列未调用 `formatIntegerNumber(row.shiftCapacityTotal)`，仍会展示浮点误差小数。

GREEN: 班次产能列已改为 `formatIntegerNumber(row.shiftCapacityTotal)`，待与白夜班展示一并回归。

RED: `node tests/e2e/mes-scheduler-workbench-process-wip-double-shift-static.spec.js` -> FAIL，班次产能列缺少有效双班 `X2` 标识，班次状态仍直接展示原始字段。

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-double-shift-static.spec.js` -> PASS，班次状态、快速过滤和倍数标签已使用统一白夜班口径。

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js`、`mes-scheduler-workbench-process-wip-night-shift-start-date-static.spec.js`、`mes-pro-scheduler-workbench-static.spec.js` -> PASS。

GREEN: `node node_modules/eslint/bin/eslint.js src/views/mes/pro/scheduler-workbench/index.vue tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js tests/e2e/mes-scheduler-workbench-process-wip-double-shift-static.spec.js` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。

REGRESSION: `node tests/e2e/mes-scheduler-workbench-process-wip-controls-static.spec.js` -> FAIL，仍断言已被其他已提交任务删除的顶部排产说明区，与本次班次展示改动无关。

GREEN: experience-preflight -> PASS，已读取 PowerShell、登录和 Playwright 门禁；官方登录预检使用前端仓 Playwright 1.60.0、系统 Chrome、本机 `http://localhost:8081` 和测试租户 `aoteman` 真实进入排产员工作台；写入验证仅切换测试租户目标工序夜班状态并在结束时恢复原值。

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-double-shift-real.e2e.cjs` -> PASS，测试租户 `tenant_id=122`、账号 `aoteman`、工序 `B050`（processId=`922748`）真实关闭状态开启夜班后显示“白夜班”和绿色 `X2`，结束时恢复 `nightShiftEnabled=false`。

GREEN: `validate_frontend_feature.py`、`validate_bug_regression.py`、真实 E2E 脚本语法检查与 ESLint -> PASS。

GREEN: task-closeout preview -> PASS，确认仅保留 `task.md` 与 `execution-log.md`，清理任务证据、截图、JSON 和一次性检查脚本。

GREEN: task-closeout apply -> PASS，任务目录仅保留 `task.md` 与 `execution-log.md`。

BLOCKER: none
