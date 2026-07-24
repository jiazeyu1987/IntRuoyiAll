# Task: 工序主数据联动工艺路线

## 任务目标

工序主数据修改后，所有引用该工序的工艺路线详情接口实时返回最新工序主数据；工序名称允许重复，工序编码保持唯一。

## 经验门禁

- PowerShell：已读取 `docs/powershell-memory.md`，本轮命令和中文文档读写必须显式 UTF-8，Maven 参数必须加引号。
- 项目经验索引：本任务命中 PowerShell / Windows shell 门禁；不涉及服务器发布、真实 E2E、worktree 合并或数据库写入。
- Backend API：已读取 `backend-api-delivery` 和契约；接口响应字段变更必须补 BDD/TDD 证据，失败时 fail fast。
- Closeout：任务完成前先运行 `task-closeout-cleanup` preview，仅保留任务核心记录、生产代码和正式测试。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；缺失工序主数据或编号冲突保持明确失败。
- 是否从根因和长期维护角度解决：是；路线工序继续按 `processId` 引用工序主数据，接口动态返回最新字段。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 同名不同编码工序允许保存 -> Given 已存在工序名称为“清洗”的工序 / When 新增另一个名称同为“清洗”但编码不同的工序 / Then 保存成功。
- BDD: 工序编码仍保持唯一 -> Given 已存在编码为 `PROC-CLEAN-001` 的工序 / When 新增或修改另一个工序为同编码 / Then 接口失败并提示工序编码已存在。
- BDD: 工艺路线展示最新工序主数据 -> Given 两条路线工序引用同一个 `processId` / When 工序主数据变更名称、工艺要求、状态和人工班次产能 / Then 两条路线详情接口均返回最新主数据字段，路线级配置不被覆盖。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 测试覆盖同名工序与路线主数据响应字段。
- [x] M3：最小实现工序名称放宽与路线响应字段补充。
- [x] M4：运行目标 Maven 测试和后端 API evidence 校验。
- [x] M5：运行 closeout preview，记录最终验证与阻塞。

## 预期验证

- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest,MesProRouteProcessControllerWorkstationViewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260708-process-master-route-linkage/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-process-master-route-linkage --mode preview`

## Cleanup Keep

- `doc/tasks/20260708-process-master-route-linkage/backend-api-evidence.md`

## 当前状态

COMPLETED_WITH_FRONTEND_TSCHECK_BLOCKER。已完成后端最小实现、前端 API 类型同步、目标 Maven 测试、后端 evidence 校验和 closeout preview。前端全量 `pnpm ts:check` 被既有 `scheduler-workbench` 的 `bottlenecks` 字段类型错误阻塞，非本任务 route-process 类型改动引入。
