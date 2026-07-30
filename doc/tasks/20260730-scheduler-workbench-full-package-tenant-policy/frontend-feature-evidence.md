# Frontend Feature Evidence：排产员工作台全量包导入提示优化

## Feature Goal

- 导入“全部数据包”成功后，前端成功提示需要展示策略设置导入计数，避免用户误以为只导入了角色和重排数据。

## Acceptance

- API 类型必须声明 `policySettingsCount: number`。
- 导入成功 toast 必须展示 `策略设置 ${result.policySettingsCount} 条`。
- 现有导入超时设置和手动重排三类计数提示必须保持。

## UI Entry Point

- `IntRuoyiFronted/src/views/mes/pro/scheduler-workbench/index.vue`
- 设置弹窗中的“导出全部数据包 / 导入全部数据包”按钮。

## API Contract

- `SchedulerWorkbenchFullConfigImportRespVO` 新增 `policySettingsCount: number`。

## BDD

- `BDD: 导入摘要展示策略计数 -> Given 用户导入全量数据包成功 / When 前端收到导入结果 / Then 成功提示展示用户角色、手动重排数据和策略设置计数。`

## Verification

- `RED: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> FAIL`，缺少 `policySettingsCount: number`。
- `GREEN: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。
- 响应类型新增 `policySettingsCount: number`。
- 导入成功提示新增 `策略设置 ${result.policySettingsCount} 条`。
- 本次未改布局、路由、权限或控件状态；响应式、空态和加载态沿用既有设置弹窗行为。

## Blockers

- 无前端实现 blocker。
