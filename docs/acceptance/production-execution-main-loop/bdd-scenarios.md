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

### Scenario: 并发重复确认不产生重复闭环事实

Given 同一 `processPoolEventId` 已经完成质量放行和强制复核
And 两个班组长页面或两个浏览器请求几乎同时提交同一 FIFO 确认幂等键或同一来源事件终态
When 后端处理并发确认请求
Then 系统只能写入一组有效分配明细、一个订单工序完成结果和一批批记录字段审计
And 第二个请求必须返回同一正式结果或明确重复拒绝
And 系统必须在写入事务内重新校验质量、强制复核、剩余数量和字段审计唯一性

### Scenario: 正式字段必须有数据库约束支撑

Given 开发者为生产提交、PQC、复核、FIFO 或批记录回填新增正式来源 ID 或幂等键
When 该字段只存在于 VO、DTO、前端类型、rawPayload 或测试对象
Then 当前 slice 必须保持 RED 或 BLOCKED
And 只有迁移脚本、DO 字段、Mapper 映射、测试 schema、唯一约束或索引均可验证后，才能进入 GREEN
And 系统不得把接口字段存在当作正式持久化链路完成

### Scenario: 真实 E2E 写入前必须只读核验运行态迁移

Given P0 真实 E2E 已具备前后端 URL、租户账号、工单、设备、签名、PQC 和批记录测试数据
And 浏览器即将通过真实页面写入生产提交、PQC、复核、FIFO 和批记录链路
When 缺少 `P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD`
Then 真实 E2E 必须记录 `BLOCKED`，且不得启动浏览器写入
When 只读运行态迁移验证器返回缺字段、缺索引或历史断链 blocker
Then 真实 E2E 必须继续 `BLOCKED`
And 不得用 release policy gate、H2 测试 schema、页面文案、rawPayload 或人工说明替代真实 MySQL 运行态核验

### Scenario: PQC 正式提交进入工序池质量链路

Given PQC 员工选择活跃生产工单、路线工序、PQC 任务和 QA 规程快照
And PQC 员工选择或继承正式设备账号、设备和工作站上下文
And PQC 员工完成逐件检验、质量结果和电子签名
When PQC 员工提交检验结果
Then 系统保存 PQC 任务状态、逐件明细和质量结论
And 系统创建或绑定一条工序池 PQC 提交事件
And 该事件关联生产工单、工序、PQC 任务、规程版本、逐件明细、实际员工、设备账号、设备、工作站、签名、服务端提交时间和原始 payload
And PQC 结果必须在统一闭环 trace 中可见

### Scenario: PQC 事件不能替代生产提交根事件

Given PQC 员工提交后系统生成了一条 `PQC_INSPECTION` 工序池事件
And 该 PQC 事件通过正式字段绑定到一条 `PRODUCTION_SUBMIT` 生产提交事件
When 用户用 PQC 事件 ID 打开生产执行闭环 trace
Then 系统必须解析并返回唯一生产提交根事件 ID
And PQC 事件只能出现在 `quality.sourceIds.pqcEventId`
And 若无法解析唯一生产提交根事件，trace 必须返回候选或 `status=BLOCKED`
And 系统不得把 PQC 事件 ID 作为完整闭环的 `processPoolEventId`

### Scenario: PQC 结果必须正式绑定生产提交

Given 同一生产工单和同一路线工序存在多条生产提交事件
And PQC 员工提交了一条正式 PQC 检验结果
When 系统把 PQC 结果纳入某条生产执行 trace
Then 系统必须通过正式结构化 ID 证明 PQC 结果绑定到目标 `processPoolEventId` 或目标提交数量片段
And 绑定证据至少包含 PQC 工序池事件、PQC 任务、生产提交事件或正式分配来源 ID
And 系统不得仅按生产工单、工序名称、提交时间、员工姓名、备注或页面文案推断绑定关系

### Scenario: PQC 绑定不能只藏在 rawPayload

Given 一条 PQC 工序池事件的 `rawPayload` 中带有生产提交事件 ID
But 数据库正式字段或正式关联表中没有 PQC 到生产提交事件的绑定记录
When 系统生成生产执行 trace 或计算质量是否可分配
Then 质量分组必须返回 `status=BLOCKED`
And blocker 必须说明缺少结构化生产提交绑定
And 系统不得把 rawPayload 解析结果单独当作 `complete=true`、FIFO 可分配或 E2E PASS 的证据

### Scenario: 电子签名必须由后端验真

Given 员工或班组长提交生产、PQC、复核或确认动作
And 请求中包含签名 ID、签名员工和签名快照
When 后端校验签名证据
Then 签名动作用途必须匹配当前业务动作
And 签名员工必须等于实际操作者；若业务允许正式授权签名人，授权关系、授权范围和被代表员工必须固化到签名快照
And 签名快照必须固化到对应事件或复核记录
And 缺失、过期、越权、用途不匹配或仅有前端字段的签名不得通过

### Scenario: PQC 失败阻塞可分配数量

Given 某工序池提交事件或其关联 PQC 结果为失败、待检或质量状态无法确认
When 班组长尝试将该数量片段 FIFO 分配给生产工单
Then 系统拒绝分配并说明质量状态不可分配
And 不写入分配明细、订单工序完成或批记录回填
And 不使用默认合格、忽略 PQC 或仅前端提示作为替代

### Scenario: PQC 合格数量不足阻塞 FIFO

Given 某生产提交根事件完成数量为 `80`
And 该事件绑定的 PQC 结果为合格，但正式合格数量只有 `60`
When 班组长尝试确认 `80` 数量并分配到活跃生产工单
Then 系统拒绝 FIFO 确认并返回 `QUALITY_QUANTITY_MISMATCH` 或等价机器可读 blocker
And 不写入确认、分配、订单工序完成或批记录回填终态
And 系统不得只凭 `inspectionResult=SUCCESS` 放行超过 PQC 合格数量的确认数量

### Scenario: 强制复核未完成不得进入 FIFO

Given 某工序池提交事件的质量状态已达到可分配白名单
And 后端规则要求生产组长复核，且当前工序或质量规则要求 PQC 组长复核
When 任一强制复核记录缺失、缺复核签名、签名员工不匹配或复核来源事件不一致
Then 系统拒绝 FIFO 确认和手工分配
And trace 的 `review` 或 `quality` 分组返回 `status=BLOCKED`
And blocker 必须说明缺少哪个强制复核角色、签名或来源事件
And 系统不得把“未配置强制复核”“待复核”或“页面已查看”显示为已复核通过

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
And 员工提交事件的质量状态可分配、完成数量为 `80`，且绑定 PQC 合格数量至少为 `80`
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
And 字段审计记录保存来源事件、来源分配、批记录执行、字段路径、单元格位置、来源值、旧值、新值、系统原因和幂等键
And 统一 trace 可从工序池提交事件进入批记录执行和字段审计投影

### Scenario: 统一闭环 trace 回答 P0 审计问题

Given 一条生产提交已经完成报工、记录本、复核、FIFO 分配、订单工序完成和批记录回填
And 同工序存在关联 PQC 结果
When 用户按生产提交根事件 `processPoolEventId` 打开生产执行闭环 trace
Then 系统返回谁提交、设备、工序、数量、质量结果、提交签名、目标生产工单、班组长复核、FIFO 分配、订单工序完成、批记录执行和字段审计投影
And trace 中所有对象都使用正式 ID 和结构化字段关联
And PQC 事件 ID 只能作为质量子事件来源 ID，不得覆盖生产提交根事件 ID
And 顶层 `complete=true` 只能在提交、质量、复核、分配、完成和批记录全部完整时返回
And trace 不依赖备注文本、页面文案、默认槽位或人工拼接截图

### Scenario: P0 收口证据逐项回答九个审计问题

Given 一次真实页面 run 捕获了新的生产提交根事件 `processPoolEventId`
And 后端 trace 声称该事件 `complete=true`
When 验收人员生成或查看 P0 闭环证据包
Then 证据包必须逐项回答谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、如何进入批记录追溯
And 每个答案都必须包含业务可读值、正式 `sourceIds`、来源分组、同源校验结果和只读复验入口
And 任一答案只能来自页面文案、截图、rawPayload、`formBindings`、历史 ID 或人工拼接时，本次验收必须为 `FAIL/RED` 或 `BLOCKED`
And 系统不得把分段单元测试 GREEN、静态合同 PASS 或 trace 页面加载成功当作 P0 收口证据

### Scenario: trace 不允许空壳完成

Given 一条 trace 响应包含提交、质量、复核、分配、完成和批记录分组名称
But 任一分组缺少正式 `sourceIds`、缺少机器可读 blocker、批记录字段审计为空或质量事件无法唯一绑定生产提交事件
When 前端展示该 trace
Then 后端必须返回 `complete=false`
And 缺失分组必须返回 `status=BLOCKED` 和解除条件
And 前端必须展示未闭环原因，不得把空分组、空数组、摘要文案或页面标签显示为已闭环

### Scenario: trace 不允许跨租户或跨工单拼接

Given 当前租户内存在一个生产提交事件
And 另一个租户、另一个生产工单或另一个路线工序存在相似的 PQC、复核、FIFO 或批记录事实
When 用户按 `processPoolEventId` 查询统一闭环 trace
Then trace 只能聚合与该事件同租户、同生产工单、同路线工序、同 MES 工序且权限允许的正式事实
And 任何跨边界候选必须被排除或作为 blocker 返回
And 系统不得为凑齐六分组而拼接其它租户、工单或工序的数据

### Scenario: 初版 trace 不能替代正式闭环

Given trace endpoint 已能返回 `submitEvent`、`quality`、`review`、`allocation`、`completion` 和 `batchRecord` 六个分组
And `quality` 只靠同工单同工序匹配 PQC，`review` 只取第一条复核，或 `batchRecord` 缺少 `sourceAllocationId` 与字段审计明细
When 开发者试图把该 trace 作为 P0 完成证据
Then 验收必须判定为未完成
And trace 顶层必须保持 `complete=false`
And 后续 TDD 必须补齐质量正式绑定、多候选 `candidateEvents`、强制复核聚合和批记录字段审计来源

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
Then trace 顶层必须返回 `complete=false`
And 质量链路必须返回 `status=BLOCKED`
And 明确说明缺少 PQC 工序池事件或事件关联
And 不自动补默认事件、不按时间接近匹配、不用备注或页面文案推断关联

### Scenario: PQC 多候选不能拼接成质量闭环

Given 同一生产工单、同一路线工序和同一业务日期存在多条生产提交事件
And 同一范围内存在一条或多条 PQC 事件
When trace 服务无法用正式 ID 唯一证明某条 PQC 事件属于当前 `processPoolEventId`
Then 质量分组必须返回 `status=BLOCKED`
And blocker 必须说明 `PQC_BINDING_AMBIGUOUS` 或等价机器可读原因
And trace 顶层必须 `complete=false`
And 不得把所有 PQC 事件汇总后展示为当前提交的质量结果

### Scenario: 复核缺电子签名时失败

Given 班组长提交复核或报工分配确认请求
When 请求缺少复核电子签名、签名员工或签名快照
Then 系统拒绝复核或确认
And 不写入复核记录、分配明细、订单工序完成或批记录回填

### Scenario: 签名员工与实际操作者不一致时失败

Given 一线员工、PQC 员工或班组长正在提交需要电子签名的动作
When 请求签名员工不是当前实际操作者，也不是正式授权签名人
Then 系统拒绝该动作并返回签名主体不一致原因
And 不写入主事件、PQC 终态、复核记录、FIFO 分配或批记录回填

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
Then 统一 trace 顶层返回 `complete=false`
And 批记录分组返回阻塞状态和缺失投影 blocker
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

### Scenario: 批记录回填顺序边界

Given FIFO 分配和订单工序完成尚未写入正式来源 ID
When 系统尝试回填批记录字段
Then 批记录回填必须失败或保持 `batchRecord.status=BLOCKED`
And 字段审计不得提前写入无来源分配、无来源值或无幂等键的记录
And 若批记录回填由异步任务执行，任务状态、来源事件、来源分配、失败原因和重试幂等键必须结构化持久化

## Implementation Decisions Before GREEN

- 质量状态词典必须在实现前冻结；未冻结前，只有明确后端白名单状态可进入 FIFO，其它失败、待检、让步待审、返工待处理或无法确认状态一律不可分配。
- 复核电子签名接口必须在实现前冻结；未冻结前，复核和确认分配测试必须保持 RED/BLOCKED，不得用登录人、备注、普通密码或前端确认弹窗替代。
- 强制复核角色必须在实现前冻结；未冻结前，生产组长复核视为 FIFO 强制前置，PQC 组长复核必须在 trace 中明确返回“未配置强制/待复核/已复核”之一。
- 统一 trace 至少要有一个真实用户入口；入口可以先落在生产组长工作台，但实现任务必须在静态合同中锁定 route、菜单权限、按钮和 trace 页面主标题。
- PQC 入池 RED 必须先把旧测试中 `never()` 断言改成期望创建正式 PQC 工序池事件；保留旧断言会形成“证明缺口存在”的假 GREEN。
- 真实 E2E 脚本必须区分三种结果：缺真实前置为 `BLOCKED`，前置齐备但完整页面步骤未实现为 `FAIL/RED`，只有真实页面完成全部断言才是 `PASS`。
- 数据库迁移、测试 schema、DO/Mapper、唯一约束和索引必须与新增正式字段同步冻结；缺任一层时实现任务必须先补 RED，不能继续写业务 GREEN。
- trace、复核、FIFO 和批记录查询必须冻结租户、权限、生产工单、路线工序和 MES 工序同源规则；未冻结时不得返回 `complete=true`。
- P0 收口证据包必须在实现前冻结字段形状；未冻结时真实 E2E 只能记录为未具备最终 PASS 证据，不得把分段 GREEN 拼成完成结论。

## Open Questions

- 当前无可绕过的开放问题；上方未冻结事项均按实现前置 blocker 处理。
- 如果后续业务决定把返工、报废、让步接收或再检纳入 P0 第一版，必须先补 BDD/TDD/E2E 场景，再允许实现。

## Test Blockers

- 缺少可写测试租户、设备账号、生产员工、PQC 员工、生产组长、PQC 组长、电子签名和正式权限。
- 缺少活跃生产工单、路线工序、PQC 任务、QA 规程快照、固定模板、记录本模板或正式批记录字段映射。
- PQC 提交无法创建工序池事件时，P0 质量链路阻塞。
- 班组长复核缺电子签名正式字段时，P0 复核链路阻塞。
- 统一 trace 不能按事件聚合提交、质量、复核、分配和批记录字段审计时，P0 追溯链路阻塞。
- 正式字段缺迁移、测试 schema、DO/Mapper、唯一约束或索引时，P0 持久化链路阻塞。
- trace 无法证明同租户、同工单、同路线工序、同 MES 工序和权限边界时，P0 追溯链路阻塞。
