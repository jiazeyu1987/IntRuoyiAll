# 执行日志：修复运行控制台 IntRuoyi 整套行不显示

## Bug

运行控制台页面需要稳定显示 `IntRuoyi 整套` 组件行；用户反馈运行控制台中该行不显示。

## Expected

`IntRuoyi 整套` 必须作为固定组件行渲染在 `IntRuoyi 后端` 与 `Website 前端` 之间；即使 overview 某个环境缺少 `intruoyi-full` 状态，单元格也显示 `-`，不得隐藏整行。

## Reproduction

Reproduction: `node tests\e2e\runtime-control-full-row-visible.spec.js` -> 在修复前 FAIL，因为页面缺少固定行源命名、行级 DOM 标记和整套行可见样式，无法证明 `IntRuoyi 整套` 一定作为表格行渲染。

## Root Cause

运行控制台已有 `intruoyi-full` 字符串，但旧静态测试只检查源码包含字符串，没有约束它必须是固定表格行，也没有行级 DOM 标记用于真实页面/E2E 定位；后续发布或重构时容易退化成“数据里有组件但页面不可定位/不可见”。

## Regression Test

BDD: IntRuoyi 整套固定显示为组件行 -> Given 后端 overview 返回 local/test/prod 状态但可能缺少 `intruoyi-full` 状态 / When 用户打开运行控制台 / Then 表格组件列必须显示 `IntRuoyi 整套` 行，状态单元格缺失时显示 `-`，不得因为后端状态缺失隐藏该行。

INFO: current source review -> `src/views/infra/runtime-control/index.vue` 包含 `intruoyi-full`，但现有 `tests/e2e/runtime-control-static.spec.js` 只检查源码字符串，未约束它必须作为固定组件行渲染。

RED: `node tests\e2e\runtime-control-full-row-visible.spec.js` -> FAIL，expected reason: 当前页面缺少 `displayComponentRows` 固定行源和 `data-runtime-component-row` 行标记，测试无法证明 `IntRuoyi 整套` 一定作为可见组件行渲染。

GREEN: `node tests\e2e\runtime-control-full-row-visible.spec.js` -> PASS，新增回归测试确认固定组件行源、行级 DOM 标记、整套行可见样式和行顺序。

GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS，运行控制台 API、组件、访问路径与生产确认契约未回退。

GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS，运维按钮、日志弹窗和操作契约未回退。

INFO: `pnpm ts:check` -> FAIL，Node 默认堆约 4GB，`vue-tsc` OOM 退出 134；非类型错误。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: Chrome CDP DOM check on `http://localhost:8081/infra/monitors/runtime-control` -> PASS，`[data-runtime-component-row="intruoyi-full"]` 可见，行顺序为 `intruoyi-frontend`, `intruoyi-backend`, `intruoyi-full`, `website-frontend`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260526-runtime-control-full-row-visible\execution-log.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260526-runtime-control-full-row-visible --mode preview` -> PASS，delete `<none>`，blocked `<none>`，warnings `<none>`。

## Verification

已完成目标回归、既有静态契约、类型检查、真实本地页面 DOM 验证、bug 证据校验和 task-closeout-cleanup preview。风险范围仅限运行控制台组件表格行渲染和整套行访问路径展示。

## Follow-up

无。

## Blockers

无。
