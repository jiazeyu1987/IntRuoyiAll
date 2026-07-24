# eDHR 工序栏顶部上下文显示

## 任务目标

按截图要求删除左侧红框“待处理工序”标题，并在绿色位置展示当前批记录对应的生产工单和批记录号。

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持蓝灰运维台风格和紧凑信息密度。
- 前端复刻：已读取 `replicate-frontend-ui`，只改当前前端页面展示，不改接口、DTO、后端、mock 或数据源。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定红框删除与绿框上下文展示。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过顶部上下文条直接展示当前批记录关键标识，左侧列表只保留工序扫描内容。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 工序栏顶部显示批记录上下文 -> Given 用户打开批次详情页 / When 查看工序复盘顶部 / Then 页面在绿色位置展示当前批记录对应的生产工单和批记录号。

BDD: 左侧待处理标题删除 -> Given 批次详情页存在待处理工序 / When 用户查看左侧工序列表 / Then 红框位置不再显示“待处理工序”标题，列表直接展示工序卡片。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前未满足绿框上下文和红框删除。
- [x] M3：实现顶部上下文展示并删除左侧标题，不改变业务逻辑。
- [x] M4：运行静态测试和必要语法检查，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交或报告提交阻塞。

## 预期验证

- `node tests/e2e/edhr-process-header-context-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` 保持通过。
- `node --check tests/e2e/edhr-process-header-context-static.spec.js` 通过。

## 当前状态

completed

## 实现结果

- 工序列表顶部新增当前批记录上下文，显示 `生产工单` 和 `批记录号`。
- 批记录号优先取当前已填写工序或待处理工序的 `batchRecordReportCode`，再回退到报告名称和报告 ID。
- 左侧待处理工序列表删除红框位置的“待处理工序”标题，保留工序卡片和键盘选择能力。
- 保留右侧待处理详情、批记录/记录本切换和处理按钮，不改 API、DTO、后端或数据源。

## 验证记录

- RED: `node tests/e2e/edhr-process-header-context-static.spec.js` -> FAIL，缺少 `edhr-batch-detail__process-context` 上下文展示。
- GREEN: `node tests/e2e/edhr-process-header-context-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-pending-task-rail-relocation-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-pending-form-entry-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-process-header-context-static.spec.js` -> PASS。
- GREEN: `pnpm exec eslint src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue tests/e2e/edhr-process-header-context-static.spec.js tests/e2e/edhr-pending-task-rail-relocation-static.spec.js tests/e2e/edhr-batch-pending-form-entry-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-process-header-context --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
