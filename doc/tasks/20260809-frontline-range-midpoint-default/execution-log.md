# Execution Log：一线生产设备参数范围中点默认值

- 2026-08-09 USER INTENT：用户要求将截图中的清洗功率默认值改为上下限中间值，并让其他类似范围参数使用同一默认值规则。
- BDD: 完整数值范围默认取中点 -> Given 一线生产设备数值参数未配置显式默认值且上下限为 20 和 30 / When 后端生成运行态设备参数 / Then 返回默认值 25，前端初始显示 25 且状态位于正常范围。
- BDD: 显式默认值保持优先 -> Given 数值参数已配置显式默认值 / When 后端生成运行态设备参数 / Then 返回显式默认值，不重新计算中点。
- BDD: 不完整范围保持空值 -> Given 参数没有显式默认值且缺少下限或上限，或参数为文本标准 / When 页面初始化设备参数 / Then 默认值保持未配置，不转换为 0，也不猜测其它数值。
- DIAGNOSIS：历史压力泵配置明确把范围 `x-y` 保存为 `target=NULL`；后端运行态原样返回 `defaultValue=null`，前端仅排除 `undefined`，随后 `Number(null)` 得到 `0`，因此清洗功率显示 0 并被标红为低于下限。
- EXPERIENCE GATE：已读取 `docs/experience-index.md` 并命中一线生产正式运行态、设备参数和前后端严格来源门禁；本轮用户明确将范围默认值规则修订为中点。

## Milestone Status

- M1 completed：完成配置来源、后端映射和前端初始化链路定位。
- M2 completed：已新增后端 JUnit 与前端静态合同，并取得预期 RED。
- M3 completed：后端运行态对完整数值范围统一解析中点，显式默认值优先；前端同时拒绝 `null` / `undefined` 空值进入数值归一化。
- M4 completed：相邻回归、ESLint、类型检查、证据校验和差异检查均通过。
- M5 in_progress：已完成经验合并，准备执行任务清理预览与应用。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`expected: <25> but was: <null>`，证明后端尚未对完整范围生成中点默认值。
- RED: `node tests\e2e\frontline-production-device-parameter-midpoint-default-static.spec.cjs` -> FAIL，前端初始化条件没有排除 `defaultValue=null`。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，6 tests，0 failures，0 errors。
- GREEN: `node tests\e2e\frontline-production-device-parameter-midpoint-default-static.spec.cjs` -> PASS。
- REGRESSION: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineRuntimeConfigControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，8 tests，0 failures，0 errors。
- REGRESSION: `node tests\e2e\frontline-production-device-parameter-range-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\pressure-pump-device-parameter-standard-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\frontline-team-config-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm exec eslint src\views\mes\pro\feedback\FrontlineFixedTemplatePanel.vue tests\e2e\frontline-production-device-parameter-midpoint-default-static.spec.cjs` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- EVIDENCE: bug regression validator self-test -> PASS；`bug-regression-evidence.md` validator -> PASS。
- DIFF: 任务范围 `git diff --check` -> PASS；仅报告仓库既有 LF/CRLF 转换提示，无空白错误。
- EXPERIENCE: 按 `project-experience-consolidation` 将“完整数值范围由后端统一返回中点、可空数值进入 Number 前排除 null”合并到 `docs/backend-development.md` 和 `docs/experience-index.md`，未新建长期经验文档。
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260809-frontline-range-midpoint-default --mode preview` -> PASS；保留 task/execution-log/verification-report，删除已归档结论的 bug-regression-evidence，blocked/warnings 均为 `<none>`。
- CLEANUP APPLY: `task_closeout.py --task-id 20260809-frontline-range-midpoint-default --mode apply` -> PASS；仅删除任务附属 `bug-regression-evidence.md`，核心任务记录、生产代码和正式回归测试保留，blocked/warnings 均为 `<none>`。
- M5 completed：经验沉淀和任务清理完成；任务状态更新为 completed。

## Blockers

- 无当前阻塞。
