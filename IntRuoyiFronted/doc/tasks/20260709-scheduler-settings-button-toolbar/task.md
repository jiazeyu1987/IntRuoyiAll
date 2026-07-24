# Task: 排产设置按钮迁移到工序列表工具栏

## 任务目标

- 将排产员工作台顶部红框中的 `排产设置` 按钮移动到工序列表标准列表模板工具栏右侧蓝框位置。
- 保留 `openSchedulerSettingsDialog` 打开排产设置弹框能力，不改接口、不改排产算法、不引入 fallback。
- 顶部排产标题区不再承载该按钮，工序列表工具栏与显示字段、重置列保持同一行。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文读写显式 UTF-8，命令不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；按钮迁移保持标准列表模板工具栏结构，不做无关视觉重设计。
- 前端交付技能：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；按 BDD + RED/GREEN 记录静态契约证据。
- 高风险动作：本任务只修改本机前端源码、静态测试和任务文档；不操作服务器、不写入真实业务数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；使用 `UnifiedListTemplate` 既有 `actions` 插槽承载页面级按钮，避免自建工具栏。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 排产设置按钮进入工序列表工具栏 -> Given 用户打开排产员工作台并停留在工序列表 / When 查看快速过滤右侧工具栏 / Then `排产设置` 按钮显示在显示字段按钮左侧并打开原设置弹框。
- BDD: 顶部入口不再显示排产设置按钮 -> Given 用户查看工作台顶部排产说明区 / When 页面渲染完成 / Then 顶部说明区不再显示 `排产设置` 按钮。
- BDD: 设置弹框能力保持 -> Given 用户点击工序列表工具栏中的 `排产设置` / When 弹框打开 / Then 原班时、策略、冒烟测试入口仍保留。

## 里程碑

- [x] M1：创建任务记录并读取经验门禁。
- [x] M2：补 RED 静态契约测试。
- [x] M3：迁移按钮到标准列表模板 actions 插槽。
- [x] M4：运行目标验证并更新证据。
- [x] M5：隔离提交本任务改动。

## 预期验证

- `node tests/e2e/mes-scheduler-workbench-process-wip-controls-static.spec.js`
- `node tests/e2e/mes-scheduler-workbench-process-wip-unified-list-template-static.spec.js`
- `node tests/e2e/mes-scheduler-workbench-noise-reduction-static.spec.js`
- `node tests/e2e/mes-scheduler-workbench-density-layout-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-scheduler-settings-button-toolbar/frontend-feature-evidence.md`

## 当前状态

COMPLETED：排产设置按钮已迁移到工序列表标准模板工具栏；RED/GREEN 静态契约、frontend evidence 校验和 closeout preview 均通过。

## Cleanup Keep

- `doc/tasks/20260709-scheduler-settings-button-toolbar/task.md`
- `doc/tasks/20260709-scheduler-settings-button-toolbar/execution-log.md`
- `doc/tasks/20260709-scheduler-settings-button-toolbar/frontend-feature-evidence.md`
