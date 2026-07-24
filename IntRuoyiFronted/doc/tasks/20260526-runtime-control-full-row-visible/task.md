# 任务：修复运行控制台 IntRuoyi 整套行不显示

## 任务目标

- 修复运行控制台 `/infra/monitors/runtime-control` 中 `IntRuoyi 整套` 行不显示的问题。
- 保证 `IntRuoyi 整套` 是固定组件行，即使后端 overview 暂未返回该组件状态，也要显示行并用 `-` 展示缺失状态。
- 使用回归测试覆盖页面结构，避免只检查源码包含字符串。

## 前序任务检查

- 前端上一同仓任务：`doc/tasks/20260526-runtime-control-effectscope/`
- 状态：completed
- 边界：本任务仅修改运行控制台前端展示与对应脚本测试，不改后端接口、不改运行数据。

## BDD 场景

- BDD: IntRuoyi 整套固定显示为组件行 -> Given 后端 overview 返回 local/test/prod 状态但可能缺少 `intruoyi-full` 状态 / When 用户打开运行控制台 / Then 表格组件列必须显示 `IntRuoyi 整套` 行，状态单元格缺失时显示 `-`，不得因为后端状态缺失隐藏该行。

## 里程碑

- [x] M1：建立任务文档并确认前序任务边界。
- [x] M2：新增先失败的回归测试。
- [x] M3：最小修改页面渲染结构，保证整套行固定显示。
- [x] M4：运行目标测试、记录 RED/GREEN、收尾预览并提交前端仓库改动。

## 预期验证

- RED：新增运行控制台组件行结构测试失败，证明当前测试未约束 `IntRuoyi 整套` 必须作为固定行渲染。
- GREEN：运行控制台静态测试通过。
- GREEN：task-closeout-cleanup 预览通过。

## 当前状态

- 状态：completed。
- 已完成：任务文档初始化；新增回归测试；页面使用固定 `displayComponentRows`、行级 DOM 标记和整套行可见样式；目标测试、既有静态契约、类型检查、本地 Chrome CDP DOM 验证、bug 证据校验和 task-closeout-cleanup preview 已通过。
- 阻塞：无。

## 最终验证

- RED：`node tests\e2e\runtime-control-full-row-visible.spec.js` -> FAIL，修复前缺少固定行源和行级 DOM 标记。
- GREEN：`node tests\e2e\runtime-control-full-row-visible.spec.js` -> PASS。
- GREEN：`node tests\e2e\runtime-control-static.spec.js` -> PASS。
- GREEN：`node tests\e2e\runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：Chrome CDP DOM check -> PASS，`IntRuoyi 整套` 行可见。
- GREEN：bug regression evidence validation -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，无删除项、无阻塞项、无警告。
