BDD: 排产设置按钮进入工序列表工具栏 -> Given 用户打开排产员工作台并停留在工序列表 / When 查看快速过滤右侧工具栏 / Then `排产设置` 按钮显示在显示字段按钮左侧并打开原设置弹框。
BDD: 顶部入口不再显示排产设置按钮 -> Given 用户查看工作台顶部排产说明区 / When 页面渲染完成 / Then 顶部说明区不再显示 `排产设置` 按钮。
BDD: 设置弹框能力保持 -> Given 用户点击工序列表工具栏中的 `排产设置` / When 弹框打开 / Then 原班时、策略、冒烟测试入口仍保留。
GREEN: experience-preflight -> PASS, 已读取 PowerShell、前端样式和前端交付门禁；本任务不执行服务器写入或真实数据写入。
RED: node tests/e2e/mes-scheduler-workbench-process-wip-controls-static.spec.js -> FAIL, 排产设置按钮尚未放入工序列表标准模板工具栏 actions 插槽。
GREEN: implementation -> PASS, 已删除顶部排产说明区按钮，并在工序列表 UnifiedListTemplate actions 插槽放入同一 openSchedulerSettingsDialog 按钮。
GREEN: node tests/e2e/mes-scheduler-workbench-process-wip-controls-static.spec.js -> PASS
GREEN: node tests/e2e/mes-scheduler-workbench-process-wip-unified-list-template-static.spec.js -> PASS
GREEN: node tests/e2e/mes-scheduler-workbench-noise-reduction-static.spec.js -> PASS
GREEN: node tests/e2e/mes-scheduler-workbench-density-layout-static.spec.js -> PASS
GREEN: node tests/e2e/mes-scheduler-workbench-top-metrics-static.spec.js -> PASS
GREEN: node tests/e2e/mes-scheduler-workbench-settings-dialog-static.spec.js -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-scheduler-settings-button-toolbar/frontend-feature-evidence.md -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-scheduler-settings-button-toolbar --mode preview -> PASS, delete=<none>, blocked=<none>, warnings=<none>
GREEN: task-complete -> PASS, 已完成按钮迁移与验证，准备隔离提交本任务改动。
