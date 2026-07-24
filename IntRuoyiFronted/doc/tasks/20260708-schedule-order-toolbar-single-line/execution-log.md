# 执行日志：排产工单工具栏单行优化

- 初始化任务：用户要求“图里的 item 改成一行，排布好看些”，目标页面为排产工单顶部筛选和操作工具栏。
- BDD: 顶部 item 单行展示 -> Given 用户打开排产工单页面 / When 查看顶部筛选与操作区 / Then 快速筛选、完成筛选、同步工单、导出、手动重排和显示字段在同一行紧凑排布。
- BDD: 窄屏保持可用 -> Given 页面宽度不足以容纳全部顶部 item / When 工具栏空间收窄 / Then 顶部 item 可自动换行但不重叠、不挤压业务按钮。
- CHANGE: `src/views/mes/pro/scheduleorder/index.vue` 将查询表单改为 `schedule-order-pool__query-form` 单行 flex 布局；将 `UserTableColumnSettings` 收进 toolbar 内，与页面主操作并列展示；桌面宽度下禁用工具栏内部换行，窄屏再允许换行。
- RED: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> FAIL，旧静态契约仍允许工具栏作为独立全宽动作行，无法验证本次“item 改成一行”的目标。
- GREEN: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- BLOCKER: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> FAIL，证据文档缺少 `Acceptance`、`RED:`、`GREEN:` 标记；已补齐后复验。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-schedule-order-toolbar-single-line --mode preview` -> PASS，无删除项。
- BLOCKER: `git commit` -> 未执行；提交前复核发现 `src/views/mes/pro/scheduleorder/index.vue` 与 `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 的 HEAD 差异包含本轮开始前已存在的快速过滤、查询按钮移除、显示字段重置隐藏等改动。本轮只应提交单行布局相关内容，无法安全拆分为可验证的独立提交，故停止提交并保留工作区改动。
- CHANGE: 根据用户新截图要求，将 `完成筛选` 标记为右侧操作区起点，并把工具栏按钮组改为 `justify-content: flex-end`，让蓝框内 item 靠右排布。
- GREEN: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS，确认完成筛选使用 `margin-left: auto`，工具栏使用 `justify-content: flex-end`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260708-schedule-order-toolbar-single-line\frontend-feature-evidence.md` -> PASS。
