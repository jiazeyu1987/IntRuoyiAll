# 任务：排程日历右侧详情/规则页签调整

## 任务目标

将排程日历右侧的 `排程规则` 与 `YYYY-MM-DD 日详情` 从上下两个面板调整为同一个页签容器；`YYYY-MM-DD 日详情` 作为第一个页签并默认选中，`排程规则` 作为第二个页签。保留现有接口、错误展示、规则保存、模拟推进、产能生成、自动排产和日详情操作行为。

## 里程碑

- [x] M1：确认前一任务已完成，读取经验索引与前端样式门禁，创建任务文档。
- [x] M2：新增前端静态 RED 契约测试，锁定页签顺序、默认页签和规则内容保留。
- [x] M3：调整排程日历右侧布局为详情优先页签容器。
- [x] M4：运行静态契约、排程日历回归和类型检查。
- [x] M5：收尾记录验证结果并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`
- `node scripts/schedule-calendar-inline-shift-editor.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

已完成。右侧详情优先页签实现、静态契约、排程日历回归、类型检查、本机真实 Playwright 只读验证和 cleanup 预览均已通过。

## 验证结果

- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`：PASS。
- `node scripts/schedule-calendar-inline-shift-editor.test.mjs`：PASS，5 tests passed。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`：PASS。
- 真实 Playwright 只读验证：PASS，`http://localhost:8081`，测试租户 `aoteman`，默认日详情 tab、切换排程规则 tab、点击日期后标题同步均通过。
- `task-closeout-cleanup --mode preview`：PASS，无删除项、无阻塞项、无警告。

## 前一任务检查

- 前端前一任务 `20260624-unified-electronic-signature-primary-tab` 已标记完成，允许开始本任务。
- 当前前端仓库存在无关脏改动：`src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`、`tests/e2e/edhr-batch-open-create-readiness-hint-static.spec.js`、`tests/e2e/edhr-readiness-business-action-static.spec.js`；本任务不触碰、不暂存这些文件。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：页面保持密集操作台风格，使用白色工作面、浅灰蓝边框、蓝色 active 状态、紧凑控件和清晰表格；详情面板可使用 tabs，但必须与主操作台风格保持连接，避免装饰化卡片堆叠和大面积空白。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口失败继续使用现有错误展示与阻断方式。
- `是否从根因和长期维护角度解决`：是。通过同一右侧页签容器明确详情优先的交互结构，不改变业务 API。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 默认显示日详情 -> Given 用户进入排程日历 / When 页面初始化 / Then 右侧第一个页签为当前选中日期日详情且默认选中。`
- `BDD: 排程规则进入第二页签 -> Given 用户查看右侧页签 / When 切换到排程规则 / Then 看到保存规则、周末模式、模拟推进、生成未来产能、生成预览和发布排产。`
- `BDD: 日详情随选中日期更新 -> Given 用户点击日历其他日期 / When selectedDate 更新 / Then 第一个页签文案同步为新的 selectedDayTitle。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-detail-first-tab/task.md`
- `doc/tasks/20260624-schedule-calendar-detail-first-tab/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-detail-first-tab/frontend-feature-evidence.md`
