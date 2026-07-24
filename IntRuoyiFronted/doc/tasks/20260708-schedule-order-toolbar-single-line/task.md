# 任务：排产工单工具栏单行优化

## 任务目标

按用户截图将排产工单页面顶部筛选项、同步工单、导出、手动重排和显示字段排成一行，保持紧凑、对齐且不改变业务行为。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次仅优化工具栏布局，保持操作台式紧凑风格，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过排产工单页自身 toolbar/form flex 布局收敛顶部 item 排布，不改变通用组件行为。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 顶部 item 单行展示 -> Given 用户打开排产工单页面 / When 查看顶部筛选与操作区 / Then 快速筛选、完成筛选、同步工单、导出、手动重排和显示字段在同一行紧凑排布。
- BDD: 窄屏保持可用 -> Given 页面宽度不足以容纳全部顶部 item / When 工具栏空间收窄 / Then 顶部 item 可自动换行但不重叠、不挤压业务按钮。
- BDD: 蓝框 item 靠右展示 -> Given 用户打开排产工单页面 / When 查看顶部蓝框内 item / Then 完成筛选、同步工单、导出、手动重排和显示字段靠右排布。

## 里程碑

1. M1：建立任务文档与静态契约。`DONE`
2. M2：调整排产工单顶部 item 单行布局。`DONE`
3. M3：运行聚焦静态验证。`DONE`
4. M4：完善证据文档并提交本任务改动。`BLOCKED`

## 预期验证

- RED：`node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 在旧契约上失败，证明当前契约仍允许两行布局。
- GREEN：`node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 通过。
- REGRESSION：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` 通过，确认排产工单页面基础静态契约未回退。

## 当前状态

COMPLETED_WITH_COMMIT_BLOCKED：已完成排产工单顶部 item 单行布局优化；静态契约、基础回归、证据校验和收尾清理预览均已通过。提交阶段发现 `src/views/mes/pro/scheduleorder/index.vue` 与 `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 在本轮开始前已有未提交重叠改动，本轮无法安全只暂存当前任务 hunk，已停止提交以避免混入非本任务改动。

## 验证结果

- GREEN：`node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- BLOCKER：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> FAIL，首次证据文档缺少 `Acceptance`、`RED:`、`GREEN:` 标记；已补齐后复验。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-toolbar-single-line --mode preview` -> PASS，无删除项。
- BLOCKER：`git commit` -> 未执行；`src/views/mes/pro/scheduleorder/index.vue` 和 `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 存在本轮前置未提交重叠改动，无法在不混入非本任务改动的情况下安全提交。
- GREEN：`node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS，确认蓝框 item 靠右排布契约。
- GREEN：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS，确认排产工单基础契约未回退。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> PASS。

## Cleanup Keep

- `doc/tasks/20260708-schedule-order-toolbar-single-line/frontend-feature-evidence.md`
