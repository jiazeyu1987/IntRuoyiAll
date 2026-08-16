# 生产放行闭环开发计划

## Supervisor Milestone Index

> 本索引仅用于交付监督脚本识别既有 T1-T11；详细范围、依赖、写入边界和验收标准仍以下方原任务定义为准。

### 里程碑 1：T1 SP-0 角色、权限迁移与严格候选解析

目标：新增两个最小权限角色，按目标租户幂等绑定首版用户，并提供只按租户、角色码、启用状态解析候选人的正式服务。

### 里程碑 2：T2 MIG-RF-1、生命周期/CAS、结构化错误与共享任务合同

目标：交付申请聚合和工作任务唯一的共享结构迁移，并建立后续阶段统一使用的状态、版本、审计、幂等和 blocker 合同。

### 里程碑 3：T3 SP-1 生产组长完工到 PQC 待办后端

目标：将组长提交事务改为只创建申请和一个 PQC 待办，并提供按 activeOrderId 的权威回执。

### 里程碑 4：T4 SP-1 生产组长前端合同与页面

目标：让真实组长页面按双 100% 提交新申请，展示五个持久状态和结构化 blocker，并正确处理成功、刷新失败与响应不确定。

### 里程碑 5：T5 SP-2 PQC 决策、申请唯一批次与三类正式映射后端

目标：新增 PQC 查询/通过/拒绝接口；通过时在同一事务使用冻结路线创建申请唯一批次、写三类正式映射并初始化四报告阶段。

### 里程碑 6：T6 SP-2 PQC 工作台前端

目标：在候选待办真实页面提供 PQC 详情、通过和拒绝操作，严格按任务候选、版本和回执处理状态。

### 里程碑 7：T7 SP-3 四报告任务、附件、版本门禁与最终阶段初始化后端

目标：PQC 通过后精确创建四个并行必填 FILL 待办；完成前三份保持上传阶段，第四份原子创建管理者放行事务和待办。

### 里程碑 8：T8 SP-3 四报告上传前端

目标：在 WorkTaskBoard 和批次详情提供三类负责人完成四份资料的真实上传路径，移除目标节点 skip/暂存/覆盖入口。

### 里程碑 9：T9 SP-4 管理者代表放行、报告快照与可追溯后端

目标：实现管理者角色最终待办 provider 和 approve 扩展；复核四报告快照，原子写 RELEASED，并强制 trace 只返回 RELEASED。

### 里程碑 10：T10 SP-4 管理者审批和可追溯前端

目标：管理者代表通过真实页面完成最终放行，并在双条件固定的可追溯列表即时查看批次。

### 里程碑 11：T11 全链路集成、回归、真实 E2E 与独立验收

目标：用真实运行态、真实页面、多账号和任务自有数据证明 AC-01 至 AC-34，并验证无回归、无 fallback、无任务残留。

## Delivery Graph

```text
T1 角色/候选解析 ─┐
                  ├─> T3 SP-1 后端 ─> T4 SP-1 前端 ─┐
T2 共享迁移/状态 ─┘                                │
                                                   v
T5 SP-2 后端 ─> T6 SP-2 前端 ─> T7 SP-3 后端 ─> T8 SP-3 前端
                                                   │
                                                   v
                                T9 SP-4 后端 ─> T10 SP-4 前端
                                                   │
                                                   v
                                      T11 集成回归与真实 E2E
```

T1 与 T2 可并行。每个后端阶段的提供方合同通过后，相邻前端任务才可开始；SP-2 至 SP-4 的共享服务按依赖串行落地，禁止并发抢改。

## Shared Conflict Map

| 高风险共享区 | 唯一写入任务 | 后续消费者 |
| --- | --- | --- |
| 角色/权限迁移、角色候选解析 | T1 | T3、T5、T7、T9 |
| 申请表、WorkTask 共享 DDL、申请 DO/Mapper、状态/错误/审计常量 | T2 | T3、T5、T7、T9 |
| 组长 generation/application/controller/VO | T3 | T4、T5 |
| ProductionRelease PQC controller/service/VO、批次申请唯一入口、三 writer 集成 | T5 | T6、T7 |
| WorkTask 查询、BatchExecution special-node、附件 VO/服务 | T7 | T8、T9 |
| Release service/controller/VO、trace 投影 | T9 | T10、T11 |
| `WorkTaskBoardPage.vue` | T6 首次改造，T8 依赖后继续改造 | T10 只消费 |
| 前端共享 API 类型 | 各阶段按依赖串行 | T11 |

## Task T1

- task_id: `T1`
- title: SP-0 角色、权限迁移与严格候选解析
- objective: 新增两个最小权限角色，按目标租户幂等绑定首版用户，并提供只按租户、角色码、启用状态解析候选人的正式服务。
- dependency_ids: `[]`
- affected_paths: `IntRuoyiBackend/sql/mysql/20260814_mes_production_release_roles.sql`; system API/Mapper read-only dependencies; MES production-release role resolver and tests.
- write_scope: `IntRuoyiBackend/sql/mysql/20260814_mes_production_release_roles.sql`; `IntRuoyiBackend/yudao-module-mes/src/main/java/**/productionrelease/role/**`; matching tests only.
- acceptance_ids: `AC-03`, `AC-07`, `AC-20`, `AC-21`, `AC-30`.
- validation_steps: 先建立角色缺失、空候选、跨租户、停用用户、非角色用户 RED；再实现迁移与 resolver；运行定向 JUnit、SQL 静态合同、用户名硬编码负向扫描。
- done_definition: 两个角色的权限集合精确、首版成员迁移幂等；角色缺失/重复/空候选明确失败；代码不比较用户名或固定用户 ID；无业务申请和待办副作用。

## Task T2

- task_id: `T2`
- title: MIG-RF-1、生命周期/CAS、结构化错误与共享任务合同
- objective: 交付申请聚合和工作任务唯一的共享结构迁移，并建立后续阶段统一使用的状态、版本、审计、幂等和 blocker 合同。
- dependency_ids: `[]`
- affected_paths: `20260808_mes_active_order_release_application.sql` 的正式后继迁移；申请 DO/Mapper；WorkTask DO/Mapper/VO/PageReq；MES error code/exception/advice；共享 lifecycle/audit/idempotency classes and tests.
- write_scope: `IntRuoyiBackend/sql/mysql/20260814_mes_production_release_flow.sql`; release application DO/Mapper; WorkTask shared DO/Mapper/VO/PageReq; new `productionrelease/core/**`; matching tests. 不修改阶段 Controller/页面。
- acceptance_ids: `AC-05`, `AC-06`, `AC-11`, `AC-19`, `AC-28`, `AC-29`, `AC-30`, `AC-31`, `AC-34`.
- validation_steps: 先写 schema/DO/Mapper/Advice/CAS RED；实现精确 MIG-RF-1、生成列唯一约束、加锁读取和 CAS；验证 JSON blocker data、Long string、旧状态预检和回滚审计。
- done_definition: 申请字段/索引、PQC scope、nullable batchExecutionId 条件、版本/CAS、结构化 blocker 和审计源全部唯一；SP-2～SP-4 不再需要另建共享 DDL；旧数据不被自动推断。

## Task T3

- task_id: `T3`
- title: SP-1 生产组长完工到 PQC 待办后端
- objective: 将组长提交事务改为只创建申请和一个 PQC 待办，并提供按 activeOrderId 的权威回执。
- dependency_ids: `[T1, T2]`
- affected_paths: team leader release application service/generation/persistence/controller/request/response; active-order list projection; matching backend tests.
- write_scope: `MesTeamLeaderActiveOrderRelease*`; `MesProcessPoolTeamLeaderController` 的 release apply/get；相关 VO 和测试。不得改批次/特殊节点/release service。
- acceptance_ids: `AC-01`, `AC-02`, `AC-04`, `AC-05`, `AC-06`, `AC-28`, `AC-29`, `AC-31`.
- validation_steps: BDD 先锁定双 100%、归属、只两个对象、原子失败、同键重放、同一权威业务身份异键仍返回同一申请、请求键载荷冲突和 GET 权限；RED 必须证明旧 generation 会越级；最小实现后复跑组长相邻测试。
- done_definition: 旧 `openOrCreate`、writer、submitForApproval 不再由 SP-1 调用；成功状态固定 `PQC_RELEASE_PENDING`，响应无伪下游 ID；回执和列表投影一致。

## Task T4

- task_id: `T4`
- title: SP-1 生产组长前端合同与页面
- objective: 让真实组长页面按双 100% 提交新申请，展示五个持久状态和结构化 blocker，并正确处理成功、刷新失败与响应不确定。
- dependency_ids: `[T3]`
- affected_paths: `teamLeader.ts`; `TeamLeaderWorkbenchPage.vue`; named static test and SP-1 Playwright spec.
- write_scope: 上述前端文件、`tests/e2e/sp1-production-release-contract.spec.cjs`、`tests/e2e/sp1-team-leader-to-pqc-release.spec.ts`、named-test target registration.
- acceptance_ids: `AC-01`, `AC-04`, `AC-06`, `AC-27`, `AC-28`, `AC-29`.
- validation_steps: 先登记命名测试入口并运行入口 PASS；业务静态合同先 RED；实现字符串 ID、状态、按钮、回执恢复；运行命名测试、目标 lint/type check 和组件相邻回归。
- done_definition: 页面不再要求 batch/release ID；重复/不确定提交锁定原幂等键；提交成功不被刷新失败覆盖；无重申请入口。

## Task T5

- task_id: `T5`
- title: SP-2 PQC 决策、申请唯一批次与三类正式映射后端
- objective: 新增 PQC 查询/通过/拒绝接口；通过时在同一事务使用冻结路线创建申请唯一批次、写三类正式映射并初始化四报告阶段。
- dependency_ids: `[T3]`
- affected_paths: new ProductionRelease PQC controller/service/VO; batch open-for-release port; existing three writer integrations; T5 tests. 只消费 T7 预先冻结的 provider interface 时可用 test fake，真实集成在 T7 后补齐。
- write_scope: new `productionrelease/pqc/**`; new batch release entry port/implementation; three writer target-source restrictions and tests. 不修改 WorkTaskBoard/Batch special-node/Release service。
- acceptance_ids: `AC-07`, `AC-08`, `AC-09`, `AC-10`, `AC-11`, `AC-12`, `AC-13`, `AC-14`, `AC-29`, `AC-31`, `AC-33`, `AC-34`.
- validation_steps: RED 覆盖无独立 PQC 事务、旧批次误复用、活动路线漂移、动态表单替代；GREEN 覆盖通过/拒绝、角色+候选、CAS/幂等、三 writer 故障注入、零损耗和唯一批次；回归既有 writer 测试。
- done_definition: PQC 拒绝无下游；PQC 通过成功回执含唯一批次和三类证据；只有 `PQC_RELEASE:{applicationId}` 正式关联可复用；任一步失败全回滚。

## Task T6

- task_id: `T6`
- title: SP-2 PQC 工作台前端
- objective: 在候选待办真实页面提供 PQC 详情、通过和拒绝操作，严格按任务候选、版本和回执处理状态。
- dependency_ids: `[T5]`
- affected_paths: productionRelease API; `WorkTaskBoardPage.vue`; named static test and SP-2 Playwright spec.
- write_scope: new/updated PQC API types; `WorkTaskBoardPage.vue` PQC 区域；SP-2 tests and named target.
- acceptance_ids: `AC-06`, `AC-07`, `AC-08`, `AC-09`, `AC-27`, `AC-28`, `AC-29`.
- validation_steps: 入口合同 PASS 后业务 RED；实现候选过滤、无候选隐藏动作、reject reason、版本冲突和不确定恢复；运行命名测试与类型检查。
- done_definition: 只有 PQC 角色任务候选能看到处理动作；拒绝后无重申请动作；通过响应展示批次和四任务摘要；错误按 blockerType 处理。

## Task T7

- task_id: `T7`
- title: SP-3 四报告任务、附件、版本门禁与最终阶段初始化后端
- objective: PQC 通过后精确创建四个并行必填 FILL 待办；完成前三份保持上传阶段，第四份原子创建管理者放行事务和待办。
- dependency_ids: `[T5]`
- affected_paths: WorkTask query/service/controller/VO; BatchExecution special-node service/controller/VO; report stage provider and tests; manager-stage internal port interface.
- write_scope: WorkTask/BacthExecution shared files列于冲突图；new `productionrelease/report/**`; SP-3 tests. 不修改 Release service 实现（通过 T9 provider interface 集成）。
- acceptance_ids: `AC-14`, `AC-15`, `AC-16`, `AC-17`, `AC-18`, `AC-19`, `AC-23`, `AC-29`, `AC-30`, `AC-31`, `AC-34`.
- validation_steps: RED 覆盖顺序单任务和 skip；实现 nodeTypes 查询、四任务初始化、候选隔离、prepare 不增版本、complete CAS+幂等、附件锁定、前三份门禁、第四份故障回滚；复跑特殊节点/附件/WorkTask 测试。
- done_definition: 四任务精确且成品检负责人两份；四类 skip/delete/withdraw/overwrite 均失败；第四份和 manager-stage provider 原子；并发只初始化一次。

## Task T8

- task_id: `T8`
- title: SP-3 四报告上传前端
- objective: 在 WorkTaskBoard 和批次详情提供三类负责人完成四份资料的真实上传路径，移除目标节点 skip/暂存/覆盖入口。
- dependency_ids: `[T7, T6]`
- affected_paths: WorkTaskBoard report view; BatchExecutionDetailPage; workTask/batchExecution APIs; named static test and SP-3 Playwright spec.
- write_scope: 前述前端文件的报告区域、对应 API 类型和 SP-3 tests。
- acceptance_ids: `AC-14`, `AC-15`, `AC-16`, `AC-17`, `AC-18`, `AC-27`, `AC-28`, `AC-29`.
- validation_steps: 静态 RED 证明 skip 和旧协议存在；实现四节点、附件哈希回执、灭菌批号、版本/幂等和成功/刷新失败分层；运行命名测试、类型检查和组件回归。
- done_definition: 三类负责人只见自己的任务；成品检两份分开完成；四报告页面无 skip/覆盖；第四份响应展示管理者阶段已建立。

## Task T9

- task_id: `T9`
- title: SP-4 管理者代表放行、报告快照与可追溯后端
- objective: 实现管理者角色最终待办 provider 和 approve 扩展；复核四报告快照，原子写 RELEASED，并强制 trace 只返回 RELEASED。
- dependency_ids: `[T7]`
- affected_paths: Release service/controller/VO/mapper projection; manager-stage provider; application lifecycle integration; tests.
- write_scope: `MesProEdhrRelease*` 目标流程分支；new `productionrelease/manager/**`; trace response projection and T9 tests. 不修改前端。
- acceptance_ids: `AC-18`, `AC-19`, `AC-20`, `AC-21`, `AC-22`, `AC-23`, `AC-24`, `AC-25`, `AC-26`, `AC-29`, `AC-30`, `AC-31`, `AC-34`.
- validation_steps: RED 证明旧候选来自 route rule、无申请 CAS/快照；实现 role provider、taskId/version/signoff、报告快照重算、目标对象拒绝/撤回阻断、原子 RELEASED；运行 release/precheck/trace 相邻回归。
- done_definition: xujianhai 仅因角色成员获得权限；非角色不能处理；四报告变化阻断；同键幂等/旧版本冲突；trace 后端双条件强制。

## Task T10

- task_id: `T10`
- title: SP-4 管理者审批和可追溯前端
- objective: 管理者代表通过真实页面完成最终放行，并在双条件固定的可追溯列表即时查看批次。
- dependency_ids: `[T9, T8]`
- affected_paths: release API; ApprovalPage/ApprovalDetailPage; ReleasePage/FormTraceReleaseTab; named static test and SP-4 Playwright spec.
- write_scope: 上述前端文件、API types、SP-4 tests and named target.
- acceptance_ids: `AC-20`, `AC-21`, `AC-22`, `AC-24`, `AC-25`, `AC-26`, `AC-27`, `AC-28`, `AC-29`.
- validation_steps: 静态 RED 锁定 route-rule 候选、拒绝按钮和单条件 trace；实现管理者候选通过、无拒绝、签核、回执恢复和 trace 双条件；类型检查和命名测试。
- done_definition: 无角色/非候选无按钮；目标任务无拒绝/退回/撤回；审批成功和刷新失败分层；trace 只发双条件查询。

## Task T11

- task_id: `T11`
- title: 全链路集成、回归、真实 E2E 与独立验收
- objective: 用真实运行态、真实页面、多账号和任务自有数据证明 AC-01 至 AC-34，并验证无回归、无 fallback、无任务残留。
- dependency_ids: `[T4, T6, T8, T10]`
- affected_paths: Playwright E2E specs、任务证据；仅修复测试暴露的问题时回到对应任务 write_scope。
- write_scope: `IntRuoyiFronted/tests/e2e/sp*-*.spec.ts`; 本任务 `execution-log.md`; 独立 tester 只写 `test-report.md`。
- acceptance_ids: `AC-01` 至 `AC-34`.
- validation_steps: 执行 SQL/环境/登录前置；定向后端、前端 named tests、类型检查、集成测试；启动登记 slot 的成对运行态；Playwright 多账号主链和反向路径；E2E 后只读 API/DB 核验；独立 tester 复验。
- done_definition: 所有自动化与真实 E2E PASS、每个 AC 有执行和独立测试证据、无未解决 blocker；否则保持 blocked，禁止降级宣称完成。

## Integration Order

1. Wave 1：T1、T2 并行；两者分别独立测试。
2. Wave 2：T3；合同稳定后 T4 与 T5 的测试/新文件可并行，但 T5 不抢写 T3 文件。
3. Wave 3：T5 完成后 T6 与 T7 按冲突图执行；T6 不修改后端。
4. Wave 4：T7 完成后 T8 与 T9 可并行，前后端写域隔离。
5. Wave 5：T10。
6. Wave 6：T11 系统验证，失败回到唯一责任任务，最多三轮修复/复测。
