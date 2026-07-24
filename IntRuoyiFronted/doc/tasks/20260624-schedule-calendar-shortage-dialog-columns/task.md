# 任务：排程日历短缺弹框隐藏冗余列

## 任务目标

排程日历短缺/问题弹框中隐藏截图红框里的 `级别`、`工序`、`工作站` 三列，只保留操作员需要看的 `工单`、`物料`、`缺口`、`说明` 列。保留短缺弹框入口、错误/警告 tab、工单链接和现有短缺数据来源。

## 里程碑

- [x] M1：读取经验索引、前端样式门禁和前端交付合同，创建任务文档。
- [x] M2：新增 RED 静态契约测试，锁定短缺弹框隐藏红框列。
- [x] M3：调整短缺弹框列定义，移除不再使用的工序/工作站/级别辅助逻辑。
- [x] M4：运行静态契约、排程日历回归、类型检查和必要的本机只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

已完成。短缺弹框列收敛、静态契约、排程日历回归、类型检查、前端证据校验和 cleanup 预览已通过；本机真实只读验证因当前月份及向前 12 个月都没有短缺数 > 0 的日期，无法触发短缺弹框，已记录为真实数据前置阻塞。

## 验证结果

- `node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`：PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`：PASS。
- 本机真实只读验证：BLOCKED，`http://localhost:8081` 测试租户 `aoteman` 登录后，当前月份及向前 12 个月未找到短缺数 > 0 的日期，无法触发短缺弹框。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-shortage-dialog-columns/frontend-feature-evidence.md`：PASS。
- `task-closeout-cleanup --mode preview`：PASS，无删除项、无阻塞项、无警告。

## 前一任务检查

- 前端前一排程日历任务 `20260624-schedule-calendar-summary-cards-only` 已标记完成。
- 当前前端仓库存在调度工时、DCC、eDHR、报工导入等无关脏改动；本任务只触碰排程日历相关文件、测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹框表格保持密集操作台风格，优先显示关键业务字段，减少低价值列造成的横向拥挤；保留链接式关键标识和紧凑表格密度。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整前端列展示，不改变错误处理和数据来源。
- `是否从根因和长期维护角度解决`：是。删除冗余列及其仅服务于该列的辅助逻辑，避免继续维护不可见列。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 短缺弹框隐藏红框列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示级别、工序、工作站列。`
- `BDD: 短缺弹框保留关键列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 仍显示工单、物料、缺口、说明列。`
- `BDD: 工单链接保留 -> Given 短缺记录带工单 ID / When 用户点击工单 / Then 仍进入工单产线分析弹框。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-shortage-dialog-columns/task.md`
- `doc/tasks/20260624-schedule-calendar-shortage-dialog-columns/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-shortage-dialog-columns/frontend-feature-evidence.md`
