# 任务：排程日历短缺弹框拆分物料列

## 任务目标

排程日历短缺/问题弹框中隐藏截图红框内的顶部 `错误/阻塞 / 警告` 页签和 `说明` 列；将黄框内合并的 `物料` 列拆成 `物料编码`、`物料名称` 两列。保留 `工单`、`缺口` 和工单链接，不改后端接口与短缺数据来源。

## 里程碑

- [x] M1：读取前端交付技能、经验索引和统一前端样式门禁，创建任务文档。
- [x] M2：更新 RED 静态契约测试，锁定隐藏页签/说明列和物料拆列。
- [x] M3：调整短缺弹框列定义与行数据展示。
- [x] M4：运行静态契约、排程日历回归、类型检查和必要的本机只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-shortage-dialog-columns-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

completed。实现完成，静态契约、排程日历回归静态测试、类型检查、前端证据校验和 cleanup 预览均通过，待提交本任务改动。

## 前一任务检查

- 前端前一排程日历任务 `20260624-schedule-calendar-shortage-dialog-columns` 已提交。
- 当前前端仓库存在 DCC、eDHR、报工等无关脏改动；本任务只触碰排程日历短缺弹框、对应静态测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：弹框表格保持密集操作台风格，优先保留关键业务字段，减少低价值分组和说明列造成的横向拥挤；编码、名称分列以便扫描和复制。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只调整前端列展示，不改变错误处理和数据来源。
- `是否从根因和长期维护角度解决`：是。直接收敛短缺弹框展示结构，物料字段按编码/名称明确拆列。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 短缺弹框隐藏页签和说明列 -> Given 用户打开短缺明细或预览问题弹框 / When 弹框表格显示 / Then 不显示错误/阻塞、警告页签和说明列。`
- `BDD: 物料列拆成编码和名称 -> Given 短缺行包含物料编码和名称 / When 弹框表格显示 / Then 分别显示物料编码、物料名称两列，不再显示合并的物料列。`
- `BDD: 工单和缺口保留 -> Given 短缺行包含工单和缺口 / When 弹框表格显示 / Then 工单链接和缺口数值仍可见。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-shortage-material-split/task.md`
- `doc/tasks/20260624-schedule-calendar-shortage-material-split/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-shortage-material-split/frontend-feature-evidence.md`

## 完成记录

- 完成工作：移除短缺弹框顶部分组页签；移除 `说明` 列；将 `物料` 拆分为 `物料编码`、`物料名称` 两列；保留 `工单` 和 `缺口` 列。
- 验证结果：短缺弹框静态契约、日详情卡片静态契约、右侧页签静态契约、产能生成静态回归、前端类型检查、前端证据校验和 cleanup 预览均通过。
- 真实页面验证：本轮未执行真实页面点击验证；前一排程短缺弹框任务已确认当前月份及前 12 个月没有可打开的短缺日期，本次为同一弹框的展示列调整，未新增真实数据写入或接口变更。
