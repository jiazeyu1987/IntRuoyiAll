# 任务：排程日历日详情弹框列调整

## 任务目标

排程日历日详情弹框中隐藏截图黄框内的 `任务`、`班次`、`时间`、`车间` 列；将红框内合并的 `工序 / 产品` 列拆成 `工序`、`产品` 两列。保留工单、数量、锁定、产线等现有有效信息，不改后端接口和数据来源。

## 里程碑

- [x] M1：读取前端交付技能、经验索引和统一前端样式门禁，创建任务文档。
- [x] M2：新增 RED 静态契约测试，锁定日详情弹框隐藏列和拆分列。
- [x] M3：调整日详情弹框列定义与展示辅助函数。
- [x] M4：运行静态契约、排程日历回归、类型检查和必要的本机只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

completed。实现完成，日详情弹框列静态契约、排程日历回归静态测试、类型检查、前端证据校验和 cleanup 预览均通过，待提交本任务改动。

## 前一任务检查

- 前一排程日历短缺弹框任务 `20260624-schedule-calendar-shortage-hide-work-order` 已提交。
- 当前前端仓库存在签名、eDHR、排产员工作台等无关脏改动；本任务只触碰排程日历日详情弹框、对应静态测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹框表格保持密集操作台风格，优先保留关键业务字段；低价值列按用户要求隐藏，合并字段拆列以便扫描。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整前端列展示，不改变错误处理和数据来源。
- `是否从根因和长期维护角度解决`：是。直接收敛日详情弹框展示结构，将合并的工序/产品拆成明确列。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 日详情弹框隐藏低价值列 -> Given 用户打开白班详情或任务详情弹框 / When 任务明细表显示 / Then 不显示任务、班次、时间、车间列。`
- `BDD: 工序产品拆列显示 -> Given 任务行包含工序名称、产品编码和产品名称 / When 任务明细表显示 / Then 分别显示工序、产品两列，不再显示工序 / 产品合并列。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-day-summary-dialog-columns/task.md`
- `doc/tasks/20260624-schedule-calendar-day-summary-dialog-columns/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-day-summary-dialog-columns/frontend-feature-evidence.md`

## 完成记录

- 完成工作：日详情弹框任务明细表移除 `任务`、`班次`、`时间`、`车间` 列；将 `工序 / 产品` 拆成 `工序` 和 `产品`；保留 `工单`、`数量`、`锁定`、`产线`。
- 验证结果：新增列契约、日详情卡片静态契约、右侧页签静态契约、产能生成静态回归、短缺弹框静态回归、前端类型检查、前端证据校验和 cleanup 预览均通过。
- 真实页面验证：本轮未执行真实页面点击验证；本次为静态列展示调整，不新增或改写业务数据。
