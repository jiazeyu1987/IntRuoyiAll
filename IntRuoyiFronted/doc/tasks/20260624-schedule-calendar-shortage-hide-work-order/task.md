# 任务：排程日历短缺弹框隐藏工单列

## 任务目标

排程日历短缺/问题弹框中隐藏 `工单` 列，仅保留 `物料编码`、`物料名称` 和 `缺口`。不改后端接口、不改短缺数据来源，不新增 fallback。

## 里程碑

- [x] M1：读取前端交付技能、经验索引和统一前端样式门禁，创建任务文档。
- [x] M2：更新 RED 静态契约测试，锁定短缺弹框不显示工单列。
- [x] M3：调整短缺弹框列定义与行数据展示。
- [x] M4：运行静态契约、排程日历回归、类型检查和必要的本机只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

completed。实现完成，短缺弹框静态契约、排程日历回归静态测试、类型检查、前端证据校验和 cleanup 预览均通过，待提交本任务改动。

## 前一任务检查

- 前端前一排程日历短缺弹框任务 `20260624-schedule-calendar-shortage-material-split` 已完成提交。
- 当前前端仓库仍存在 DCC、eDHR、报工等无关脏改动；本任务只触碰排程日历短缺弹框、对应静态测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹框表格保持密集操作台风格，优先保留关键业务字段，减少低价值列；本次仅移除工单列，不改变其他短缺信息展示。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整前端列展示，不改变错误处理和数据来源。
- `是否从根因和长期维护角度解决`：是。直接收敛短缺弹框展示结构，移除不再需要的工单列与辅助逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 短缺弹框隐藏工单列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示工单列。`
- `BDD: 物料列与缺口保留 -> Given 短缺行包含物料编码、物料名称和缺口 / When 弹框表格显示 / Then 保留物料编码、物料名称和缺口列。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-shortage-hide-work-order/task.md`
- `doc/tasks/20260624-schedule-calendar-shortage-hide-work-order/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-shortage-hide-work-order/frontend-feature-evidence.md`

## 完成记录

- 完成工作：移除短缺弹框 `工单` 列，并删除仅服务该列的工单打开与展示辅助函数；保留 `物料编码`、`物料名称` 和 `缺口` 列。
- 验证结果：短缺弹框静态契约、日详情卡片静态契约、右侧页签静态契约、产能生成静态回归、前端类型检查、前端证据校验和 cleanup 预览均通过。
- 真实页面验证：本轮未执行真实页面点击验证；近期同弹框任务已确认当前测试数据缺少可触发短缺弹框的日期，本次为展示列收敛，不新增或改写业务数据。
