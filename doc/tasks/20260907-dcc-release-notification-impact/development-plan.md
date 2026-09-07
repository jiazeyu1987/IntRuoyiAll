# DCC 发布通知与关联影响评估开发计划

## Goal

在现有 DCC 新文件发布链上交付可靠、幂等、可追溯的发布后通知与关联文件影响评估，并复用现有大版本流程完成需要升版的后续跟踪。

## Scope

- 新发布事件的后续账本、可见范围快照、通知收件人、影响任务、决定与升版链接。
- DCC 详情、工作台和文控管理入口。
- 新 schema、权限/菜单、消息模板、后端服务、前端页面、测试和真实 E2E。

## Non-Scope

- 历史文件回填、跨项目关系、外部消息渠道、自动催办、自动升版、目录模板版本化。

## Milestones

### 里程碑 1：发布后续账本与冻结快照

目标：发布成功时可靠建立唯一后续批次，冻结可见范围、通知候选与正反向关联事实。

- Dependencies: 已完成 P4 发布策略；关联文件 migration metadata 修复进入代码基线。
- Acceptance: AC-01、AC-02、AC-03、AC-06、AC-07、AC-14、AC-16。

涉及文件：

  - 新 migration `20260907_dcc_publication_followup.sql` 及 SQL contract。
  - `DccControlledFileFinalizationServiceImpl` 的发布事务接入点。
  - 新 publication-followup DO/Mapper/domain service、关系查询扩展和单元/集成测试。

交付物：

  - 每个 published controlled-file 唯一后续批次。
  - 可见范围来源及解析用户明细快照、通知候选快照、正向/反向关系快照。
  - 重复回调幂等；小版本检入零副作用。

### 里程碑 2：影响评估任务与升版跟踪

目标：按相关 Master 去重生成任务，支持分配、转派、决定、关联现有大版本和解决状态。

- Dependencies: P1 批次和冻结关系可查询。
- Acceptance: AC-06、AC-07、AC-08、AC-09、AC-10、AC-11、AC-12、AC-13。

涉及文件：

  - 新 impact assessment DO/Mapper/service/controller/VO 和错误码。
  - DCC project access、用户状态和 major-revision 服务的正式集成点。
  - 影响任务审计与并发版本字段。

交付物：

  - `PENDING/UNASSIGNED/IN_REVIEW/COMPLETED` 任务状态。
  - `NO_REVISION_REQUIRED/REVISION_REQUIRED` 决定和必填原因。
  - `NOT_STARTED/REVISION_LINKED/RESOLVED` 升版跟踪，禁止自动建版。

### 里程碑 3：幂等通知与真实前端入口

目标：向责任明确的收件人发送站内通知，并提供工作台、详情和文控管理页面。

- Dependencies: P1 收件人快照、P2 影响负责人。
- Acceptance: AC-03、AC-04、AC-05、AC-09、AC-14、AC-15、AC-17。

涉及文件：

  - 扩展 DCC message job 的业务类型和幂等发送，新增消息模板/菜单/权限 migration。
  - publication-followup 查询 API、失败重试 API。
  - `controlled-file/detail`、`controlled-file/workbench`、新文控管理视图和前端 API。

交付物：

  - 一人一消息、多个收件原因、PENDING/SENT/FAILED、只重试失败记录。
  - “我的影响评估”和“发布后续”页面。
  - 通知链接继续经过当前 VIEW 权限校验，Long ID 无损。

### 里程碑 4：审计、回归与真实验收

目标：独立证明发布、通知、影响决定和升版跟踪完整，且不回归文件生效和权限边界。

- Dependencies: P1-P3 全部完成。
- Acceptance: AC-01 至 AC-18。

涉及文件：

  - DCC lifecycle log/query projection、测试报告、Playwright E2E 和任务证据。
  - 仅将测试发现回退到对应 P1-P3 owned paths 修复。

交付物：

  - 发布后续完整时间线。
  - 后端、SQL、前端合同、类型检查和真实页面闭环全部通过。
  - 独立 tester 放行。

## Implementation Steps

1. P1 先写 schema、唯一约束、正反向关系与发布失败保护 RED；再接入发布事务和只读查询。
2. P2 先写负责人缺失、越权、并发决定、无需升版零副作用和已有开放大版本 RED；再实现任务状态机和 existing major-revision link。
3. P3 先写收件人去重、消息失败隔离、SENT 禁止重放、消息不授权和 Long ID RED；再实现通知服务和页面。
4. P4 先跑全量定向回归；获得当轮 E2E/数据库/重启授权后部署迁移和新运行态，用 Playwright 完成真实业务动作，再让独立 tester 审核。

## Verification Gates

- P1 gate: migration contract、首次/重复执行、事务失败回滚、发布幂等、正反向关系快照测试全部通过。
- P2 gate: 任务生成、分配/转派、两种决定、并发 CAS、升版链接与解决状态测试全部通过。
- P3 gate: 通知去重/失败/重试/权限，页面静态合同和 `vue-tsc` 全部通过。
- P4 gate: DCC 相邻生命周期回归、真实 Playwright、只读 API/DB 对账、console 0 error 和独立 tester 全部通过。
- 任一阶段必须在 `execution-log.md` 记录 BDD、RED、GREEN，tester 通过后主 Agent 才更新状态。

## Rollback or Stop Conditions

- 发布后续账本不能与 ACTIVE 状态同事务保存：停止 P1，不允许 catch 后继续发布。
- 无法确定相关 Master 当前正式版本或负责人：保存 UNASSIGNED/明确 blocker，不选择工作版本或默认用户。
- 通知幂等接口、消息模板或权限缺失：停止 P3，不改用非幂等发送或外部渠道。
- 需要升版路径只能绕过现有 OWNER/major-revision 规则：停止 P2，不新增特权入口。
- 测试账号、任务自有数据、页面入口、数据库 migration 或运行态缺失：P4 标记 BLOCKED，不使用 API/SQL 代替业务动作。

## Ownership And Sequencing

- 主 Agent 维护 `task-state.json`、阶段门禁和最终决策。
- 获得当轮子 Agent 授权后，每阶段只启用一个 executor；阶段测试必须由不同 tester 执行。
- P1 -> P2 -> P3 -> P4 串行，避免抢改 finalization、workflow、detail 和 workbench 共享文件。

## Blockers

- 本轮已授权子 Agent 和 Git，可执行 P1 并提交/推送；数据库写入、E2E 和 48081 重启仍未授权，但不是 P1 静态实现与测试的阻塞项。
- 独立的 DCC 关联文件 migration metadata 修复将在 P1 executor 启动前提交到执行基线。
