# Frontend Feature Evidence

## Feature

- Feature: 工艺路线关系图“工序开始”卡片新增“生产组长”字段配置面板。
- Non-goal: 不在生产填写前端增加额外压力泵全工序菜单权限判断。

## Acceptance

- 工序开始字段列表显示“生产组长”。
- 生产组长面板独立展示，支持负责产线、账号/权限角色来源、候选账号/角色多选。
- 前端 API 声明读取可负责产线、读取生产组长配置、保存生产组长配置。
- 批次执行页签可见性不新增额外权限；切换范围由后端返回的生产组长配置结果决定。

## BDD:

- BDD: 生产组长配置入口 -> Given 配置人员打开工艺路线关系图 When 点击工序开始卡片 Then 字段明细中出现生产组长配置入口。
- BDD: 生产组长账号角色配置 -> Given 路线工序已绑定工作站产线 When 新增生产组长配置 Then 可选择负责产线、账号或权限角色并保存。
- BDD: 菜单权限不参与切换授权 -> Given 生产填写前端加载 Then 不依赖 `frontline-pressure-pump:all-processes` 控制工序/员工切换。

## RED:

- RED: `node tests/e2e/mes-route-start-production-leaders-static.spec.js` -> FAIL, expected reason: missing `productionLeader` boundary field and API declarations before implementation.

## GREEN:

- GREEN: `node tests\e2e\mes-route-start-production-leaders-static.spec.js` -> PASS.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS.

## Verification

- Static contract verifies `data-flow-boundary-field="productionLeader"`, `data-flow-panel="route-start-production-leader-detail"`, production line/source/candidate selectors, `USERS` and `ROLE` source options, and API method names.
- Relaxed Vue TypeScript check passed with project memory setting.
- Default Node heap type check failed with OOM only; rerun with project script memory setting passed.

## Blockers

- No remaining frontend blocker. Full build was not run because current task scope is static contract + relaxed type verification, and the workspace contains unrelated dirty changes.
