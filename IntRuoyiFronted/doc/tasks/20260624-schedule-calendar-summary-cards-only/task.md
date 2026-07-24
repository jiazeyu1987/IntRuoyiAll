# 任务：排程日历日详情仅保留六个摘要卡片

## 任务目标

将排程日历右侧 `YYYY-MM-DD 日详情` 页签收敛为只展示 6 个摘要卡片：任务、工单、白班、夜班、短缺、锁定。日详情内原本直接展示的物料汇总、产线/路线任务列表、头部短缺按钮等明细内容不再内联展示，改为通过对应卡片点击弹框查看。

## 里程碑

- [x] M1：确认前一排程日历任务已完成，读取经验索引和前端样式门禁，创建任务文档。
- [x] M2：新增 RED 静态契约测试，锁定日详情页签只保留六个卡片。
- [x] M3：调整日详情模板，移除内联明细，将短缺卡片接入弹框明细。
- [x] M4：运行静态契约、排程日历回归、类型检查和本机真实只读验证。
- [x] M5：收尾 cleanup 预览并提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`
- `node scripts/schedule-calendar-inline-shift-editor.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`

## 当前状态

已完成。日详情六卡片收敛、静态契约、回归验证、类型检查、本机真实只读验证、前端证据校验和 cleanup 预览均已通过。

## 验证结果

- `node tests/e2e/mes-pro-schedule-calendar-detail-cards-only-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-tabs-static.spec.js`：PASS。
- `node tests/e2e/mes-pro-schedule-calendar-capacity-generation-static.spec.js`：PASS。
- `node scripts/schedule-calendar-inline-shift-editor.test.mjs`：PASS，5 tests passed。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`：PASS。
- 真实 Playwright 只读验证：PASS，`http://localhost:8081`，测试租户 `aoteman`，日详情 tab 只有 6 个摘要卡片、无内联物料汇总/任务列表、规则 tab 可见；切到 `2026-05-23` 后点击任务卡片成功打开弹框。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260624-schedule-calendar-summary-cards-only/frontend-feature-evidence.md`：PASS。
- `task-closeout-cleanup --mode preview`：PASS，无删除项、无阻塞项、无警告。

## 前一任务检查

- 前端前一相关任务 `20260624-schedule-calendar-detail-first-tab` 已标记完成，允许继续本任务。
- 当前前端仓库存在 DCC、报工导入等无关脏改动；本任务只触碰排程日历相关文件、测试和本任务文档。

## 经验门禁

- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：详情面板保持密集操作台风格，使用白色工作面、浅灰蓝边框、蓝色 active 状态和紧凑摘要卡片；明细放入弹框/表格时保留清晰表格密度和链接式操作，不引入装饰化卡片堆叠。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。现有接口失败和错误展示不新增降级或静默吞异常。
- `是否从根因和长期维护角度解决`：是。通过摘要卡片作为统一入口，将明细显示聚合到已有弹框交互。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 日详情只展示六个卡片 -> Given 用户进入排程日历日详情 tab / When 详情加载完成 / Then 页面只显示任务、工单、白班、夜班、短缺、锁定六个摘要卡片，不内联显示物料汇总和任务列表。`
- `BDD: 点击卡片查看对应明细 -> Given 日详情卡片有数量 / When 用户点击任务、工单、白班、夜班、锁定或短缺 / Then 对应弹框展示任务、工单或短缺明细。`
- `BDD: 排程规则页签不受影响 -> Given 用户切换到排程规则 tab / When 查看规则表单 / Then 保存规则、模拟推进、产能生成、自动排产仍可见。`

## Cleanup Keep

- `doc/tasks/20260624-schedule-calendar-summary-cards-only/task.md`
- `doc/tasks/20260624-schedule-calendar-summary-cards-only/execution-log.md`
- `doc/tasks/20260624-schedule-calendar-summary-cards-only/frontend-feature-evidence.md`
