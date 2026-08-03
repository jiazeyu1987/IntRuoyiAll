# P0 生产执行主闭环 BDD 场景

## Purpose and Scope

本文档定义 P0“生产执行主闭环”的可观察行为。所有场景围绕“工序池提交事件”展开，目标是让一次提交可以串联报工、记录本、PQC、电子签名、班组长复核、生产工单 FIFO 分配、订单工序完成和正式批记录追溯。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/scope-contract.md`
- `docs/acceptance/production-line-process-pool/bdd-scenarios.md`
- `doc/tasks/20260730-production-line-process-pool-implementation/task.md`
- `doc/tasks/20260730-process-pool-f5-f6-implementation/task.md`
- `doc/tasks/20260731-team-leader-workbench-prd-plan/prd.md`
- `doc/tasks/20260731-team-leader-workbench-prd-plan/p6-real-e2e-evidence.md`
- `AGENTS.md` 中“批记录表单、表单槽位、工序开始”三类配置术语契约。

## Feature Scenarios

### Scenario: 生产员工一次提交形成主事件

Given 设备账号已登录，实际员工已在设备账号内切换到本人  
And 当前生产工单、活跃订单、工艺路线、路线工序、设备、工作站、生产模板和电子签名均有效  
When 员工填写完成数量、损耗数量、设备参数和不良原因并点击提交  
Then 系统在同一事务内创建正式报工、记录本原始条目、记录本事件和工序池提交事件  
And 工序池提交事件保存 `feedbackId`、`recordbookEntryId`、`recordbookEventId`、实际员工、设备账号、设备、工作站、生产工单、路线、工序、原始 payload、服务端提交时间和电子签名  
And 电子签名员工必须等于实际员工  
And 系统不得用前端连续调用多个普通接口模拟同一次提交成功

### Scenario: 重复点击不产生重复主事件

Given 员工已经用同一提交幂等键或同一正式签名完成一次生产提交  
When 前端因重复点击、网络重试或浏览器恢复再次发送相同主提交请求  
Then 系统不得创建第二条有效报工、记录本条目或工序池提交事件  
And 系统返回原提交结果或明确拒绝重复提交  
And 追溯视图仍只展示一条主提交事实

### Scenario: PQC 正式提交进入工序池质量链路

Given PQC 员工选择活跃生产工单、路线工序、PQC 任务和 QA 规程快照  
And PQC 员工完成逐件检验、质量结果和电子签名  
When PQC 员工提交检验结果  
Then 系统保存 PQC 任务状态、逐件明细和质量结论  
And 系统创建或绑定一条工序池 PQC 提交事件  
And 该事件关联生产工单、工序、PQC 任务、规程版本、逐件明细、实际员工、签名、服务端提交时间和原始 payload  
And PQC 结果必须在统一闭环 trace 中可见

### Scenario: PQC 失败阻塞可分配数量

Given 某工序池提交事件或其关联 PQC 结果为失败、待检或质量状态无法确认  
When 班组长尝试将该数量片段 FIFO 分配给生产工单  
Then 系统拒绝分配并说明质量状态不可分配  
And 不写入分配明细、订单工序完成或批记录回填  
And 不使用默认合格、忽略 PQC 或仅前端提示作为替代

### Scenario: 班组长复核必须签名且不改写原始提交

Given 生产组长或 PQC 组长负责该员工和工序  
And 该员工存在一条待复核工序池提交事件  
When 班组长查看结构化提交详情并执行复核  
And 班组长完成复核电子签名  
Then 系统保存复核状态、复核说明、复核人、服务端复核时间、复核签名 ID、复核签名员工和签名快照  
And 复核记录关联原工序池提交事件  
And 原始 payload、报工数量、PQC 结果、原提交电子签名、提交时间、FIFO 分配明细和批记录字段审计均不得被复核动作改写

### Scenario: 生产工单 FIFO 分配只消耗活跃订单

Given 班组长已把本地生产工单 `WO-A` 和 `WO-B` 加入活跃订单池  
And `WO-A` 早于 `WO-B` 加入活跃队列，且两者当前工序均有剩余数量  
And 员工提交事件的质量状态可分配、完成数量为 `80`  
When 班组长选择 FIFO 自动分配  
Then 系统按活跃订单队列顺序预分配数量  
And 分配目标只能是活跃生产工单  
And 分配总数量必须等于本次确认数量  
And 手工调整仍必须满足活跃订单、当前工序、剩余数量和总数校验

### Scenario: 组长确认后累计完成订单工序

Given 生产工单 `WO-A` 当前工序目标数量为 `200`  
And 该订单该工序已确认分配数量为 `120`  
When 班组长确认一条分配给 `WO-A` 的 `80` 数量  
Then 系统把 `WO-A` 当前工序累计确认数量更新为 `200`  
And 订单工序状态变为完成  
And 完成记录保存最后来源事件、最后复核记录、确认时间和目标数量  
And 并发确认不得导致超分配、重复完成或重复回填

### Scenario: 工序完成后回填正式批记录

Given 订单工序达到完成条件  
And 当前路线工序存在正式逐工序批记录表单绑定  
And `PROCESS_POOL_REPORT` 字段映射完整且目标单元格可写  
When 系统执行批记录回填  
Then 系统打开或创建对应生产工单和工序的批记录执行实例  
And 系统按字段映射写入完成数量、设备参数、不良原因或其它正式映射字段  
And 字段审计记录保存来源事件、来源分配、批记录执行、字段路径、单元格位置、新值、系统原因和幂等键  
And 统一 trace 可从工序池提交事件进入批记录执行和字段审计投影

### Scenario: 统一闭环 trace 回答 P0 审计问题

Given 一条生产提交已经完成报工、记录本、复核、FIFO 分配、订单工序完成和批记录回填  
And 同工序存在关联 PQC 结果  
When 用户按 `processPoolEventId` 打开生产执行闭环 trace  
Then 系统返回谁提交、设备、工序、数量、质量结果、提交签名、目标生产工单、班组长复核、FIFO 分配、订单工序完成、批记录执行和字段审计投影  
And trace 中所有对象都使用正式 ID 和结构化字段关联  
And trace 不依赖备注文本、页面文案、默认槽位或人工拼接截图

## Failure Scenarios

### Scenario: 缺少主提交上下文时失败

Given 生产提交缺少生产工单、路线工序、实际员工、设备账号、设备、工作站、固定模板、记录本 payload、报工 payload、原始 payload 或电子签名中的任一项  
When 员工点击提交  
Then 系统返回明确失败原因  
And 不写入报工、记录本或工序池事件  
And 不使用默认员工、默认设备、默认模板、默认成功或空记录替代

### Scenario: PQC 未绑定工序池事件时失败

Given PQC 员工正在执行一次新的正式 PQC 提交  
When 系统无法在同一业务事务内创建或绑定对应工序池 PQC 提交事件  
Then 本次 PQC 提交必须整体失败  
And 不写入新的 PQC 任务终态、逐件明细终态、质量结论或签名记录  
And 不得把缺失工序池事件的 PQC 结果展示为闭环 trace 已完成

### Scenario: 历史 PQC 断链只显示阻塞

Given 系统中已经存在历史 PQC 任务和逐件明细  
And 这些历史数据没有正式工序池 PQC 事件或正式事件关联字段  
When 用户打开统一闭环 trace  
Then trace 必须返回质量链路 `BLOCKED`  
And 明确说明缺少 PQC 工序池事件或事件关联  
And 不自动补默认事件、不按时间接近匹配、不用备注或页面文案推断关联

### Scenario: 复核缺电子签名时失败

Given 班组长提交复核或报工分配确认请求  
When 请求缺少复核电子签名、签名员工或签名快照  
Then 系统拒绝复核或确认  
And 不写入复核记录、分配明细、订单工序完成或批记录回填

### Scenario: 非负责范围复核失败

Given 班组长不负责该员工或该 PQC 员工  
When 班组长尝试查看详情、复核、确认分配或访问 trace 中原始 payload  
Then 系统拒绝请求  
And 不返回原始 payload、签名、记录本来源或批记录字段审计详情

### Scenario: 缺正式批记录绑定或字段映射时失败

Given 订单工序已达到完成数量  
When 当前路线工序缺少正式逐工序批记录表单绑定、字段映射或目标单元格投影  
Then 系统阻塞批记录回填并返回明确原因  
And 不使用 `formBindings`、默认 `MAIN`、工序开始配置、空批记录或任意默认单元格替代

### Scenario: trace 投影缺失时失败

Given 订单工序完成记录指向批记录执行  
When 批记录字段审计投影缺失或无法证明字段来自工序池提交事件  
Then 统一 trace 返回阻塞状态  
And 不把只有批记录执行 ID、空字段列表或摘要文案当成追溯成功

## Boundary Scenarios

### Scenario: 批记录表单边界

Given 当前工艺路线同时存在正式批记录表单、表单槽位 `formBindings` 和工序开始配置  
When 系统执行批记录回填或 trace 展示批记录来源  
Then 系统只能使用工序设置中的正式逐工序批记录表单绑定  
And 不允许用 `formBindings`、工序开始配置、特殊表单、默认 `MAIN` 或前端字段名推断正式批记录来源

### Scenario: 原始记录、复核、审核副本和批记录边界

Given 同一工序池提交事件可能存在原始 payload、班组长复核、审核副本、原始 revision 和批记录回填  
When 任一环节写入数据  
Then 原始 payload 只能由正式原始 revision 链路修改  
And 班组长复核只能写复核记录  
And 审核副本只能写审核副本和字段明细  
And 批记录回填只能写正式批记录字段审计  
And 任一环节不得覆盖另一环节的事实记录

### Scenario: 活跃订单 FIFO 边界

Given 生产工单未加入活跃订单池、已移出活跃订单池或当前工序剩余数量不足  
When 班组长执行 FIFO 或手工分配  
Then 系统拒绝分配  
And 不按排产、创建时间、工单号、当前时间或非活跃订单兜底分配

### Scenario: 多事件 trace 查询边界

Given 同一生产工单和同一路线工序存在多条生产提交事件或多条 PQC 提交事件  
When 用户只按生产工单和工序打开统一 trace  
Then 系统必须显示可选择的结构化事件列表或要求补充 `processPoolEventId`  
And 不得把多条事件合并为一条闭环事实  
And 每条候选必须展示事件类型、提交员工、提交时间、数量、质量状态和事件 ID

## Implementation Decisions Before GREEN

- 质量状态词典必须在实现前冻结；未冻结前，只有明确后端白名单状态可进入 FIFO，其它失败、待检、让步待审、返工待处理或无法确认状态一律不可分配。
- 复核电子签名接口必须在实现前冻结；未冻结前，复核和确认分配测试必须保持 RED/BLOCKED，不得用登录人、备注、普通密码或前端确认弹窗替代。
- 统一 trace 至少要有一个真实用户入口；入口可以先落在生产组长工作台，但实现任务必须在静态合同中锁定 route、菜单权限、按钮和 trace 页面主标题。

## Open Questions

- 当前无可绕过的开放问题；上方未冻结事项均按实现前置 blocker 处理。
- 如果后续业务决定把返工、报废、让步接收或再检纳入 P0 第一版，必须先补 BDD/TDD/E2E 场景，再允许实现。

## Test Blockers

- 缺少可写测试租户、设备账号、生产员工、PQC 员工、生产组长、PQC 组长、电子签名和正式权限。
- 缺少活跃生产工单、路线工序、PQC 任务、QA 规程快照、固定模板、记录本模板或正式批记录字段映射。
- PQC 提交无法创建工序池事件时，P0 质量链路阻塞。
- 班组长复核缺电子签名正式字段时，P0 复核链路阻塞。
- 统一 trace 不能按事件聚合提交、质量、复核、分配和批记录字段审计时，P0 追溯链路阻塞。
