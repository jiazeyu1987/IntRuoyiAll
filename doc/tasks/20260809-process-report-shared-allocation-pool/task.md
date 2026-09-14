# 工序报工共享分配池

## Task Goal

将组长报工分配改为按工序管理的共享数量池：FIFO 按活跃工单列表稳定顺序尽可能分配，未分配数量保留；未放行分配可调整，已放行分配锁定；报工管理展示分配订单及放行状态，全部分配且全部放行后从待处理列表移出，同时永久保留报工历史和调整审计。

## Milestones

- M1：完成现状调查、需求规格和验收标准。 `[完成]`
- M2：完成依赖图、BDD 场景和严格 TDD 测试计划。 `[完成]`
- M3：完成后端领域模型、持久化、接口及测试。 `[完成]`
- M4：完成前端报工列表、FIFO/手动调整交互及测试。 `[完成]`
- M5：完成真实用户路径 E2E、回归验证和独立验收。 `[完成]`
- M6：完成经验沉淀和任务清理收尾。 `[完成]`

## Expected Verification

- 后端单元/集成测试覆盖 FIFO 部分分配、剩余池、已放行锁定、未放行重分配、并发复核、订单变化和调整审计。
- 前端定向测试覆盖分配订单列、绿色放行状态、编辑锁定、FIFO 草稿和从空白手动分配。
- Playwright 使用真实前端路径验证大数量报工的自动分配、手动调整、剩余保留和历史状态。
- 相关模块定向回归通过，无 fallback、静默降级或默认成功。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；按报工池、分配行、放行锁定和调整审计建立正式数据链路。
- 是否存在临时补丁或绕过：否。
- 用户范围约束：只实现已确认的业务规则，不增加额外业务限制；候选工单范围按工序及组长现有可见范围，FIFO 顺序按活跃工单列表的正式稳定顺序。

## Current Status

completed：实现、BDD/TDD、后端/前端回归、真实页面成功 E2E、经验沉淀和任务附属产物清理均已完成。

## Completed Work

- 已确认用户业务口径和“不增加额外限制”的范围约束。
- 已读取任务收尾、PowerShell 编码和复杂交付技能规则。
- 已完成首轮代码现状调查：当前 FIFO 超额即失败、确认要求分配总量等于报工量、确认后写入终态复核、分配行无独立放行状态和调整流水。
- 已读取经验索引并命中以下适用门禁：
  - FIFO 自动预览按正式当前工序快照计算，缺当前 `routeProcessId + processId` 快照的候选跳过，手工指定仍严格校验正式目标。
  - 确认写请求的 `leaderType` 使用当前正式页签状态，不得读取可清空筛选条件。
  - Schema 变更先核对现有迁移和运行态结构，禁止用业务 fallback 适配旧库。
  - 报工列表聚合分配明细时保持一行一个报工事件，分页 count 与 page 使用相同主对象口径。

## Verification Evidence

- 任务目录及核心记录文件已按 UTF-8 创建。
- 现状证据：`MesTeamLeaderFifoAllocationService` 在剩余报工量大于零时抛错；`MesTeamLeaderReportConfirmationServiceImpl` 要求分配总量等于报工量并禁止二次分配。
- DB-01：共享分配迁移两次执行通过，运行库 81 条生产报工均有版本状态，schema 不保存放行快照。
- BE-01：22 个定向测试通过，覆盖大数量完整池基数、正式 FIFO 顺序、候选自身工序快照和历史任一 `RELEASED` 锁定。
- BE-02：43 个事务/控制器定向测试通过，覆盖部分和空保存、重分配、放行锁、版本/幂等、OUTPUT 碎片重建和跨目标工序完成量回算。
- BE-03：订单变化协调器、活跃订单移除、工单减量/冻结/取消入口定向测试通过；状态变化与退池处于同一事务。
- BE-04：时间线当前分配批量投影、待处理/历史独立条件 8 个定向测试通过。
- FE-01：报工管理增加分配订单和未分配数量，已放行绿色且不可编辑；支持 FIFO 草稿、从空白开始、手动增删改和冲突后显式刷新；报工历史使用独立 `HISTORY` 视图。
- FE-01：分配保存幂等键绑定完整请求身份，精确相同重试复用，草稿改动后生成新键；版本冲突不自动重试。
- 审计：新增报工分配调整审计查询接口和前端 API 合同。
- 统一后端回归：144 tests PASS；审计控制器补充测试 17 tests PASS；`yudao-server` 完整反应堆构建 `BUILD SUCCESS`。
- 前端验证：3 个定向契约 PASS，目标 ESLint PASS，`pnpm ts:check` PASS。
- Playwright：真实页面事件 `176` 的池总量为 `411111`；FIFO 确认成功写入 `100/2248/517`、余量 `408246`；随后手动把最早订单改为 `50` 后确认成功写入 `50/2248/517`、余量 `408296`；报工管理分配订单列、未放行状态和报工历史均与当前版本一致。

## Remaining Blockers

- 无。真实前端成功写入和后端/前端回归证据已齐备；任务附属产物清理是收尾动作，不构成实现阻塞。

## Cleanup Candidates

## Cleanup Keep

- `doc/tasks/20260809-process-report-shared-allocation-pool/task.md`
- `doc/tasks/20260809-process-report-shared-allocation-pool/execution-log.md`
- `doc/tasks/20260809-process-report-shared-allocation-pool/verification-report.md`
- `output/runtime/int_main/backend-report-shared-allocation-final-20260810-0345.jar`
- `output/runtime/int_main/logs/stderr-0345.log`
- `output/runtime/int_main/logs/stdout-0345.log`
- `output/runtime/int_main/logs/yudao-server.log`
