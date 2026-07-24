# eDHR 待处理工序信息右侧栏迁移

## 任务目标

将批次详情页左侧“待处理工序”卡片中的表单说明、可填写人、批记录/记录本选择、状态/角色标签和打开动作迁移到右侧当前工序摘要栏，左侧列表只保留可扫描的工序索引和名称。

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持蓝灰运维台风格和紧凑信息密度。
- 前端复刻：已读取 `replicate-frontend-ui`，只改当前前端页面展示，不改接口、DTO、后端、mock 或数据源。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定左侧精简与右侧详情承载。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过主从布局职责拆分解决左侧卡片拥挤，右侧详情复用既有任务动作和权限判断。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 待处理工序详情右侧展示 -> Given 用户在批次详情页选择一个待处理工序 / When 右侧当前工序摘要栏展示 / Then 表单说明、可填写人、批记录/记录本选择、状态、角色和打开动作在右侧可见并复用原处理函数。

BDD: 左侧待处理工序保持可扫描 -> Given 批次详情页存在多个待处理工序 / When 用户查看左侧工序列表 / Then 待处理卡片只保留序号和工序名，不再堆叠表单详情、可填写人、承载选择和操作按钮。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前详情仍在左侧卡片。
- [x] M3：实现待处理工序详情迁移到右侧栏，不改变业务逻辑。
- [x] M4：运行静态测试和必要语法检查，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交或报告提交阻塞。

## 预期验证

- `node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-batch-pending-form-entry-static.spec.js` 保持通过。
- `node --check tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` 通过。

## 当前状态

completed

## 实现结果

- 左侧“待处理工序”卡片只保留序号与工序名，继续支持点击、Enter 和 Space 选择工序。
- 右侧“当前工序摘要”新增“待处理详情”，展示表单说明、可填写人、批记录/记录本选择、状态、角色和处理按钮。
- 处理按钮、承载选择、权限禁用和动作函数继续复用原有 `selectedTaskForEvidence`、`openPendingTaskByFillCarrier` 与 `handlePendingTaskAction`，不改 API、DTO、后端或数据源。

## 验证记录

- RED: `node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` -> FAIL，左侧仍包含 `resolvePendingTaskDescription(task)` 等详情内容。
- GREEN: `node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-pending-form-entry-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` -> PASS。
- GREEN: `pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-pending-task-rail-relocation-static.spec.js tests/e2e/edhr-batch-pending-form-entry-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-pending-task-rail-relocation --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
