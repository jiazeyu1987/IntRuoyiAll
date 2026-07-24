# 任务：排程日历日详情产线显示名称

## 任务目标

排程日历日详情弹框中 `产线` 列显示产线名称，不再显示 `产线编码 / 产线名称` 拼接文本。保持现有日详情弹框列结构、工单链接、工序、产品、数量、锁定展示，不改后端接口和数据来源。

## 里程碑

- [x] M1：读取前端交付技能、经验索引和统一前端样式门禁，创建任务文档。
- [x] M2：更新 RED 静态契约测试，锁定产线列使用名称展示。
- [x] M3：调整日详情任务行和工单汇总产线名称字段。
- [x] M4：运行静态契约、排程日历回归、类型检查和必要的本机只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-day-summary-dialog-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

completed。实现完成，日详情弹框产线名称静态契约、排程日历回归静态测试、类型检查、前端证据校验和 cleanup 预览均通过，待提交本任务改动。

## 完成记录

- 完成工作：新增 `lineNameTitle` 作为日详情任务行产线名称展示字段；任务明细产线列改用 `buildTaskLineNameLabel`；工单汇总产线聚合改用产线名称。
- 验证结果：日详情弹框列契约、日详情卡片静态契约、右侧页签静态契约、前端类型检查、前端证据校验和 cleanup 预览均通过。
- 真实页面验证：本轮未执行真实页面点击验证；本次为展示字段静态调整，不新增或改写业务数据。

## 前一任务检查

- 前一排程日历日详情弹框列调整任务 `20260624-schedule-calendar-day-summary-dialog-columns` 已提交。
- 当前前端仓库存在自动排产入口、签名、eDHR、排产员工作台等无关脏改动；本任务只触碰排程日历日详情产线展示、对应静态测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹框表格保持密集操作台风格，列内容应直接可读；产线列按用户要求显示名称，避免编码占据宽度。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整前端显示字段，不改变错误处理和数据来源。
- `是否从根因和长期维护角度解决`：是。新增明确的产线名称展示字段，避免复用编码/名称拼接字段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 日详情产线显示名称 -> Given 用户打开白班详情或任务详情弹框 / When 任务明细表显示 / Then 产线列显示产线名称而不是产线编码/名称拼接。`
- `BDD: 工单汇总产线聚合名称 -> Given 用户打开工单详情弹框 / When 工单汇总表显示产线 / Then 聚合产线名称而不是产线编码/名称拼接。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-day-summary-line-name/task.md`
- `doc/tasks/20260624-schedule-calendar-day-summary-line-name/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-day-summary-line-name/frontend-feature-evidence.md`
