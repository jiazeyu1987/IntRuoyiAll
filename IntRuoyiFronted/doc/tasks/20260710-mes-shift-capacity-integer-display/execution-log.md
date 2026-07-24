# 执行日志

BDD: 班次产能显示整数 -> Given 工序班次产能为带浮点误差的数值 `4799.99998` When 用户查看排产员工作台的工序列表 Then 班次产能显示为带千分位的整数 `4,800`，不显示小数部分。

BDD: 其他数量精度不受影响 -> Given 未完需求或今日报工包含有效小数 When 用户查看工序列表 Then 这些列继续使用原有通用数量格式，不因班次产能调整而被强制取整。

REPRODUCTION: 用户截图中的“班次产能”显示 `4,799.99998`、`2,999.999995`、`2,500.00002` 等浮点误差小数，并因列宽发生换行。

ROOT_CAUSE: `src/views/mes/pro/scheduler-workbench/index.vue` 的班次产能列调用通用 `formatNumber`，该函数允许最多 6 位小数；页面已有 `formatIntegerNumber`，但此前仅用于在制单数。

RED: `node tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js` -> FAIL，班次产能列未调用 `formatIntegerNumber(row.shiftCapacityTotal)`。

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js` -> PASS，班次产能列使用整数格式化，`4799.99998` 的格式化结果为 `4,800`。

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-unified-list-template-static.spec.js` -> PASS

GREEN: `node tests/e2e/mes-scheduler-workbench-process-wip-night-shift-start-date-static.spec.js` -> PASS

GREEN: `pnpm.cmd exec eslint src/views/mes/pro/scheduler-workbench/index.vue tests/e2e/mes-scheduler-workbench-process-wip-short-titles-static.spec.js` -> PASS

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS

REGRESSION: `node tests/e2e/mes-scheduler-workbench-process-wip-controls-static.spec.js` -> FAIL，既有测试仍要求已移除的“顶部排产说明区”存在；失败断言与班次产能格式化无关，本任务未修改该测试或说明区。

GREEN: experience-preflight -> PASS，真实验证仅访问本机 `http://localhost:8081`，使用测试租户 `aoteman` 执行只读页面检查。

BLOCKER: official-login-preflight first attempt -> Playwright 自带 `chromium_headless_shell-1223` 报 `Invalid file descriptor to ICU data received`，浏览器在登录前退出。

GREEN: official-login-preflight -> PASS，显式使用系统 Chrome 后，测试租户真实登录并进入 `/mes/pro/scheduler-workbench`。

GREEN: real-page-check -> PASS，真实页面检查 20 个班次产能值，均为无小数点的千分位整数；示例包括 `10,520`、`28,720`、`959,792`。

GREEN: visual-check -> PASS，截图确认班次产能保持单行整数显示，未再出现浮点小数换行。

BLOCKER: task-directory-overlap -> 原任务目录被并行工作扩展为“白夜班 + X2”范围；为避免覆盖或提交其他任务内容，本任务切换到独立目录 `20260710-mes-shift-capacity-integer-display`，并在 Git 索引中仅暂存整数格式修改。

BLOCKER: none
