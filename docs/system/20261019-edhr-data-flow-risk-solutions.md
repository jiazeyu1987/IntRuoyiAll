# eDHR 多用户数据流风险：根因与解决方案

日期：2026-10-19。状态：**设计方案，尚未实施**。

本文依据当前工作区代码及上一轮六个 Astra high 子 Agent 的静态审查结果，由主 Agent 复核整理。代码中存在其他任务尚未提交的改动，本文以当前工作区实际实现为依据，不以 Git HEAD 代替当前代码。没有执行数据库变更、代码修复、服务重启、E2E 或压力测试。

## 1. 目标与结论

业务链路：ERP 同步 → 工艺路线与 QA 配置 → 组长配置人员 → 加入活跃工单 → 生产与 PQC 提交、复核和补正 → 双 100% 完工 → 正式回填活跃详情单 → PQC 生产放行 → 上传资料 → 上市放行 → 历史追溯；并覆盖偏差、不合格审批对主链路的影响。

30 个用户同时使用会增加交叉操作和等待的机会，但并不意味着必然出现故障。已确认的问题主要是数据一致性和证据完整性问题，其中部分只需两个人操作同一业务对象即可触发。容量问题单列，不能把静态推测写成“30 人一定跑不动”。

方案的核心是明确每个业务动作究竟对哪份数据负责：

- 编辑和补正必须基于操作者实际看过的版本；数据库行锁只解决同时写入，不能识别旧页面。
- 发布后的配置、签名覆盖的资料必须不可被后续编辑悄悄改变。
- 数据库、文件存储、密码失败计数各有不同的事务边界，不能用一个业务事务假装它们一起成功或一起回滚。
- 关闭、放行等终态动作必须留下“谁针对哪一版证据完成了什么动作”的业务证据。

## 2. 问题台账

| 编号 | 业务节点 | 已确认风险 | 优先级 | 根因归类 |
|---|---|---|---|---|
| R01 | QA 配置保存、发布 | 并发保存可能覆盖已发布版本内容，普通并发保存也可能互相覆盖 | P1 | 生命周期与编辑版本不在同一并发协议中 |
| R02 | 生产组长普通补正 | 后提交的旧表单可能覆盖先提交的新值 | P1 | 缺少操作者所见的基线版本 |
| R03 | PQC 普通补正 | 旧检测明细可能覆盖其他人刚完成的补正 | P1 | 与 R02 相同，涉及正式检测表及样本明细 |
| R04 | 上传资料、上市放行、追溯 | 活跃订单资料未进入最终审批正文，审批版本未感知资料变化 | P1 | 审批证据边界小于业务放行证据边界 |
| R05 | 上传、删除资料 | 数据库回滚无法恢复已物理删除的文件；上传后失败可能留下孤立文件 | P1/P2 | 数据库与文件存储跨资源一致性缺失 |
| R06 | 偏差正常关闭 | 关闭动作未携带操作者看到的处理内容版本 | P2 | 签字有版本约束，关闭动作没有 |
| R07 | PQC 生产放行签名认证 | 错误密码的失败次数可能随外层放行事务回滚 | P2 | 安全认证结果错误依附业务事务 |
| R08 | 偏差正常关闭、追溯 | 关闭动作未显式固化关闭人及领域审计证据 | P2 | 终态转换与业务行动证据分离 |

P1 表示优先处理的数据、证据或主链路风险；P2 表示需要修复的安全或操作上下文风险，不代表已经发生业务事故。R05 中物理删除后回滚为 P1，上传孤立文件为 P2。

## 3. R01：QA 配置保存与发布必须共同维护一个版本状态

### 现状和代码依据

页面保存、发布进入同一个 `saveDraftInternal`。该方法先查版本状态，后按 ID 更新内容，再删除、重建子项。`publish` 在调用此方法后才完成旧版本退役、目标版本发布和主表当前版本指向更新。

- [QA 页面保存、发布入口](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue:5070)
- [保存与发布事务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java:132)
- [先检查状态、后无条件更新及重建子项](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java:715)
- [已发布主配置不更新的分支](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java:777)
- [活跃订单读取并冻结配置](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:4032)

典型时序：已有正式 V1 和草稿 V2，甲、乙同时读取 V2 为草稿；甲发布成功；乙先前通过状态检查，随后继续按版本 ID 写入内容和重建子项。当前更新语句没有以“仍为草稿”为条件，发布后的内容和子项 ID 因而可能改变。

### 本质问题

`@Transactional` 保证单次事务原子性，不保证两次事务的“检查后更新”互斥。这里也不是缺一个前端按钮禁用。配置版本的状态、内容、子项和当前正式版本指向共同组成一个业务整体，但没有共同的写入协议。

### 修改方案

1. 保存、发布、退役等修改同一配置的方法，统一先锁配置主记录，再锁目标版本；锁定后重新读取状态，禁止根据加锁前的对象继续判断。与审计锁的先后顺序统一，不允许不同入口反向取锁。
2. 草稿新增编辑修订号 `draftRevision`（拟新增），它不同于业务版本号 V1/V2。查询返回修订号，保存和发布必须提交 `expectedDraftRevision`。
3. 更新同时约束租户、配置 ID、版本 ID、`DRAFT` 状态和预期修订号；影响行数不是 1 即返回明确的版本冲突。子项删除、重建必须在成功取得该次修改资格之后进行，并与主版本更新同事务提交。
4. 发布在同一事务内完成草稿内容确认、旧正式版本退役、新版本发布、当前正式版本指向和审计。发布后禁止修改内容及子项；需要调整则新建下一草稿。
5. 首次创建主配置或版本时，由唯一约束裁决并发创建，冲突明确返回，不采用静默覆盖或“自动改写最新版本”。

### 验收

两窗口同时保存：仅一个基线成功，另一个提示冲突并保留本地输入。保存与发布交叉：要么先保存且发布者因旧基线被拒绝，要么先发布且保存被拒绝；正式版本内容及子项 ID 均不得在发布后改变。加入活跃订单读取的冻结配置必须与该正式版本一致。

## 4. R02、R03：补正必须表达“我修改了哪一版”

### 现状和代码依据

生产组长页面先加载记录，再把完整字段提交给普通补正接口。后端已锁记录并重新读取最新内容，但将请求中的旧表单字段覆盖到最新对象上。PQC 补正同样接收完整检测结果、样本数据，并继续更新正式检测表、补正复核记录及汇总。

- [生产补正表单组装及提交](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:9237)
- [生产普通补正入口及覆盖字段](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolProductionReportCorrectionService.java:138)
- [普通生产补正请求](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolProductionReportCorrectionReqVO.java:19)
- [PQC 补正命令](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionCommand.java:12)
- [PQC 读取、重建明细及正式表同步](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java:135)
- [修订记录写入](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolEventRevisionServiceImpl.java:90)

例：甲乙打开同一生产记录，原值“参数 20、损耗 1”。甲改参数为 25 并提交；乙只改损耗为 2，整份表单仍带参数 20。乙后提交时，即使等待行锁，仍可能把参数恢复为 20。PQC 的两个不同样本修改存在同类问题。

### 本质问题

行锁保证写入串行，不保证后提交的人看过前一人的修改。审计能记录覆盖，却不能阻止覆盖。仅在服务端比较“最新值与请求值”也无法判断哪些变化是操作者有意修改、哪些只是旧表单残留。

### 修改方案

1. 生产和 PQC 普通补正统一返回并要求提交“所见基线”：事件 ID、最新修订 ID、规范化内容哈希。未补正原始记录也必须有明确的初始基线表示，不能用缺参数表示“不检查”。
2. 在已有锁内先校验基线、当前状态和权限，再计算差异、准备签名。任何一项变化均提示记录已更新；冲突时不写修订、正式表、汇总或签名。
3. 签名绑定业务对象、修改前基线、修改后内容、补正原因和身份；签名完成到事务提交之间仍校验同一上下文。客户端传来的哈希仅用于比较，服务端必须自行计算权威哈希。
4. 前端保留尚未提交的输入，展示需要重新加载和核对的提示；不自动覆盖、不自动重试、不静默合并检测数据。
5. 继续保持现有修订、正式来源、PQC 样本、补正复核记录及汇总的原子更新。保留当前已具备预期修订检查的“本人退回重提”分支，不将其误判为同一缺陷，也不新增不必要的审批层级。

### 验收

分别对生产字段、PQC 不同样本执行两窗口交叉补正。第二个旧基线必须被拒绝；首次补正的正式数据、修订历史和汇总均保持一致。刷新后再次补正可以成功。冻结、已放行、无权限、本人退回重提仍按原业务规则处理。

## 5. R04：上市放行必须签署实际交付的资料集合

### 现状和代码依据

活跃订单分支的审批正文纳入正式完工及 PQC 放行事实，但资料清单 `materialManifest` 和附件集合未从活跃订单上传资料构造。独立批次分支另有材料清单和文件完整性检查。活跃订单资料上传、删除不推进最终审批所依赖的内容版本。

- [资料上传、删除前端入口](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:4462)
- [上市放行提交入口](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue:1848)
- [两类来源的审批正文分支](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/approval/content/EdhrReleaseContentResolver.java:42)
- [活跃订单正式完工、PQC 放行事实校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/manager/MesEdhrActiveOrderReleaseFactService.java:34)
- [上市放行读取审批内容与校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java:679)
- [资料写入及审计](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:150)

甲预览资料后，乙合法新增或删除资料，甲仍可能按原审批上下文完成上市放行。最终追溯能看到资料，不等于能证明那些资料正是甲当时确认的集合。

### 本质问题

业务要求管理者代表基于上传资料最终放行，代码里的审批证据边界却只覆盖部分正式事实。单纯加“附件数量大于零”不能解决签字和资料脱节；已有审批版本和行锁也无法感知未纳入正文的资料变化。

### 修改方案

1. 为活跃订单建立正式资料清单快照，归属于租户、活跃订单、放行申请和批次的确定关系。沿用活跃订单资料模型，不把独立批次的“四项材料”规则直接套入此分支。
2. 清单包含资料类别、资料关联 ID、不可变文件 ID/版本、文件名、大小、内容类型和内容 SHA-256。清单顺序规范化，服务端生成哈希；临时下载 URL 不作为内容身份。
3. 所有新增、删除、替换及影响证据有效性的操作，同事务推进资料集合修订号。资料变更、审批内容读取和最终提交遵循同一并发协议，避免“读取半新半旧的清单”。
4. 审批预览返回该清单及其修订号，签名正文覆盖完工事实、PQC 放行事实和资料清单。最终审批在统一锁顺序内重新校验预期正文和清单；有变化则拒绝并要求重新查看、签名。
5. 放行提交时保存不可变的已签资料清单快照及证据关联；追溯展示该签署快照。当前资料列表可另行展示，但不能冒充已签集合。上市放行后按现有冻结语义禁止改动该批已签证据。
6. 使用新的审批内容策略版本，旧签名继续按原策略解释，不能补造旧资料清单或重算旧签名。切换前尚未提交的旧预览明确失效并重新预览。

资料是否必传、灭菌不适用如何表达、各资料类别由谁上传，是业务配置问题。实施前复用并核对现有规则；本方案不凭空增加统一材料数量门槛或新的角色限制。**把实际上传资料纳入签署范围，不依赖先扩张这些规则。**

### 验收

预览后新增、删除、替换任一资料，旧审批必须失败；未变更则成功。签名正文、最终快照和历史下载文件的哈希一致。跨租户、跨申请和错误批次的资料不能进入清单。实际缺失或损坏的文件明确失败，不能用只有数据库记录的“成功附件”代替。

## 6. R05：把文件生命周期与数据库事务分开管理

### 现状和代码依据

删除资料时，方法先删资料关联，再调用文件服务物理删除，之后写业务及 GxP 审计。如果后续异常，数据库可以回滚，已删除的磁盘或对象存储文件不能随之恢复。上传时先创建文件，只有关联插入失败的一段执行清理，后续审计或提交失败不在该清理范围内。

- [上传创建文件与局部清理](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:125)
- [删除关联、物理文件及后续审计](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesActiveOrderDossierFileService.java:192)
- [基础文件服务的存储与数据库操作](E:/IntRuoyi/IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/FileServiceImpl.java:383)

### 本质问题

数据库事务不覆盖外部文件系统。扩大 `try/catch` 或增加 `@Transactional` 都不能获得跨资源原子性；仅加 `afterCommit` 内存回调也无法处理提交后进程突然退出。

### 修改方案

采用持久化文件操作状态和待处理任务；优先复用项目内经核实适用的持久化任务机制。以下状态、字段为拟议设计，不表示当前已存在相关表。

**上传：先有可恢复的操作记录，再上传，最后原子挂接。**

1. 短事务创建上传操作记录，固化租户、操作者、目标对象、预分配存储路径、请求标识和状态。记录必须先持久化，便于恢复“文件已上传但进程退出”的情况。
2. 事务外上传到不可变的暂存路径并核对大小、内容哈希，持久化校验结果；失败明确返回，不把暂存当成正式业务资料。
3. 短业务事务重新校验身份权限、目标状态、归属和资料集合修订号；原子完成正式资料关联、集合版本推进、审计及操作记录的已挂接状态。
4. 挂接失败时保留可诊断状态，调度清理尚未挂接的暂存文件。清理前检查正式引用和保留约束，不能误删已被其他合法事务引用的文件。

**删除：先完成业务取消关联，再异步处理允许物理删除的对象。**

1. 业务事务内完成权限和状态检查、取消关联、集合版本推进、审计及持久化删除任务；事务失败时上述动作一起回滚，文件仍存在。
2. 提交后工作器根据删除任务处理存储，记录完成或失败，支持有界重试、任务抢占及崩溃恢复。仅在明确的删除任务中，把目标已经不存在视为该删除动作已达成；读取资料缺失仍必须报错。
3. 已进入签名或追溯证据的文件受不可变引用及保留规则约束，不能因为从当前页面移除就物理删除。未被保留证据引用且满足既定删除政策的文件才进入物理清理。
4. 页面区分“业务关联已移除”和“存储清理待处理”；禁止向用户报告一个实际失败的正式上传成功。清理失败可追踪、可重试，不阻止无关业务对象继续流转。

### 验收

故障注入覆盖：上传后、挂接前；关联写入后、审计前；业务提交后、工作器执行前；物理删除后、任务回执前。每种情况均能恢复或明确失败，不出现数据库指向被本次回滚操作永久删除的文件，也不出现无法定位来源的暂存对象。此项需要文件存储和数据库集成验证，纯单元测试不足。

## 7. R06、R08：偏差关闭要绑定所见版本并留下关闭证据

### 现状和代码依据

偏差处理签字已经校验内容版本、哈希及有效签名。正常关闭也检查当前所需签名，因此**没有确认“未签完也能关闭”的漏洞**。但页面关闭只传偏差 ID，不传所见内容版本；关闭 SQL 更新状态、原因、时间和版本，没有显式更新关闭操作者，服务方法也未写专门的领域关闭审计。

- [前端关闭操作](E:/IntRuoyi/IntRuoyiFronted/src/views/mes/pro/edhr-deviation/DeviationDetail.vue:566)
- [关闭请求结构](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProEdhrDeviationHandlingCloseReqVO.java:7)
- [关闭状态与有效签名校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDeviationHandlingServiceImpl.java:204)
- [关闭 SQL](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrDeviationMapper.java:81)

R06 的触发条件是：甲查看旧内容，乙修订并完成新一轮签字，甲未刷新便关闭。后端校验的是新一轮完整签字，无法证明甲关闭的是自己看过的内容。R08 则是关闭证据不能只依赖通用 HTTP 日志；既有签名的签署人也不能代替实际关闭人。

### 修改方案

1. 关闭请求增加 `expectedContentVersion`、`expectedContentHash`；如偏差主状态版本也影响关闭判断，一并提交并校验该版本。读取详情时返回服务器计算的基线。
2. 沿现有锁定路径重新核对偏差状态、处理内容、所需节点和当前有效签名；基线不一致先拒绝，不自动关闭更新后的版本。
3. 关闭事务固化关闭人、服务器关闭时间、原因、关闭所依据的处理版本和哈希、有效签名关联，并写入领域关闭审计。可采用关闭事实记录或扩展现有领域结构，避免仅靠最后修改人推断业务关闭人。
4. 关闭状态、证据和审计同事务提交。新增 GxP 审计前统一该链路的锁顺序，检查保存、签字、关闭等交叉入口，不能持有偏差锁后随意补取一个顺序相反的审计锁。
5. 已关闭、版本冲突、缺少签名等分别明确报错。保持现有关闭权限，不额外增加签字节点或审批角色。

### 验收

旧页面不能关闭已变更且重新签完的内容；刷新后合法关闭成功。签字不足仍失败。成功关闭后可查到实际关闭人和依据版本；审计失败则关闭状态及关闭事实一起回滚。通用请求日志是否存在，不替代本项验收。

## 8. R07：密码认证失败必须独立于放行业务提交

### 现状和代码依据

PQC 放行业务在外层事务里调用旧密码重认证接口。旧接口更新失败计数后抛业务异常，外层事务回滚使计数失效。内部声明 `noRollbackFor` 不能让已经加入外层的同一事务独立提交。

- [PQC 放行事务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java:145)
- [旧密码认证调用](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java:255)
- [错误密码更新计数后抛错](E:/IntRuoyi/IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserServiceImpl.java:438)
- [现有独立签名认证及事务内身份重校验](E:/IntRuoyi/IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/signature/SystemSignatureAuthenticationService.java:39)

### 本质问题

“密码尝试发生了”是安全事实，即使最终业务没放行也必须保存；它不应成为可回滚的放行业务副作用。不能仅给原方法机械添加 `REQUIRES_NEW`：外层若已锁同一身份行，再开新事务取锁，可能造成自身等待。

### 修改方案

1. 优先接入现有独立签名认证能力。在进入放行业务锁、审计锁、用户行锁之前，先以独立短事务完成密码认证，持久化失败计数及认证事件。认证失败结果提交后，由编排层向页面返回失败。
2. 认证成功返回服务器内部身份凭据，绑定租户、操作者、签署主体、凭据代次和版本、请求身份；放行业务将它与本次动作及内容上下文关联，禁止使用客户端传来的“已认证”布尔值。
3. 进入短业务事务后，按统一锁顺序调用现有 `lockAndRevalidate`，重新核对禁用、锁定、密码状态及凭据是否变化，再执行业务状态校验和签名提交。
4. 认证成功不代表必然放行成功；业务失败照常回滚业务记录，认证事件仍保留。认证与业务之间被禁用、改密或撤销身份时，业务签名必须失败。
5. 实施时检索其他旧签名认证调用者，逐条确认外层事务和取锁顺序后迁移；当前已确认的故障路径是上述 PQC 放行路径，不能直接宣称全部签名入口都有同一问题。

### 验收

连续输入错误密码且每次放行都失败，失败计数仍递增，达到现行策略阈值后锁定。成功认证后业务校验失败，不抹除认证事件。认证后到提交前改密、禁用、锁定均拒绝签名；无自锁和跨租户身份复用。

## 9. 30 用户场景下需验证的容量风险

以下是已定位代码结构的容量风险，**尚未通过压测确认超时、死锁或容量不足**，不与 R01—R08 的确定性逻辑缺陷混为一谈。

| 容量项 | 代码与本质 | 解决方向 | 验证重点 |
|---|---|---|---|
| C01 同租户审计锁等待 | [GxP 审计服务](E:/IntRuoyi/IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/gxpaudit/GxpAuditServiceImpl.java:53)使用租户级串行锁；部分业务较早取得该锁，文件 I/O、同步后续请求等可能延长持锁时间 | 保留审计链的一致性；先将可独立校验的外部读取移出持锁区，缩短写事务。进入事务后核对来源版本，失败明确中止；不能靠删除审计锁解决 | 生产/PQC 提交叠加 ERP 同步、文件上传时的持锁时长、等待、回滚率 |
| C02 提交后通知仍占用请求 | [通知入口](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/notification/MesReleaseTaskNotificationService.java:31)已有持久化意图及失败处理，但提交后同步派发还会执行多次独立事务 | 复用现有通知持久化意图，交由有界工作器派发；保留认领、幂等、重试和可见失败。不用无持久化的后台线程 | 原业务提交与通知耗时分开统计，进程中断后能恢复，不重复制造业务待办 |
| C03 普通用户历史列表放大 | [批次列表](E:/IntRuoyi/IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java:495)普通权限分支先全量筛选可见性再内存分页，可能逐条追加查询；管理员分支不同 | 将同等授权谓词下推为 SQL `EXISTS`/关联条件，在授权集合上分页并批量读本页详情；统计总数使用同一谓词。不得先分页再删无权限行 | 普通角色深页及大数据量查询数、响应时间、总数准确性、权限撤销即时性 |

ERP 同步优化需特别保护“来源读取失败”和“来源确实取消”的区别，不能为了缩短事务把未成功读取的数据当成已取消订单。大批量拆事务前，应明确原同步是否承诺整批原子性，不能隐式改变。

建议以真实的 30 个角色会话覆盖不同订单及同一订单争用，混合生产/PQC 提交、复核、资料处理、ERP 同步、通知和历史查询；观察 P95/P99、数据库连接等待、行锁等待、长事务、SQL 数量及数据正确性。验收响应时间指标需结合实际车间需求确定，本文不编造通过阈值，也不以数据库连接池配置推定实际容量。

## 10. 实施顺序与变更边界

1. **编辑基线与不可变配置**：完成 R01、R02、R03。前后端协议一起变更，字段缺失明确失败，不能默认为最新版本而绕过保护。
2. **资料与审批证据闭环**：R04、R05 协同设计和交付。先明确资料集合修订、不可变文件引用及持久化操作记录，再接通审批正文、最终签名快照和追溯读取，避免只修某一入口。
3. **偏差关闭与认证事务**：R06、R08 同一关闭链路一起修改；R07 复用现有身份认证体系，避免另建一套密码政策。
4. **容量优化**：先取得 C01—C03 的测量结果，再修改已证实的瓶颈；所有优化必须保留租户隔离、审计顺序及授权语义。

可能涉及的结构变更为：草稿编辑修订号、必要的资料集合修订及已签快照、持久化文件操作状态、偏差关闭事实。先核对可复用字段、表与任务机制，再确定迁移脚本；本文未执行迁移。历史签名不回写、不重签、不伪造缺失证据；新策略从切换点明确生效，历史内容按原策略解释。

每项实现都应补充与根因对应的后端并发/事务测试，再集中做受影响页面回归。真实 E2E 若后续启动，必须按项目规则通过真实前端完成业务动作，API/数据库仅用于只读核验；本轮文档不代表 E2E 已通过。

## 11. 不应误改的现有保护

- 不能把“存在行锁”写成全链路无并发问题，也不能因为发现旧页面问题就删除已有行锁、幂等及冻结校验。
- 双 100% 完工、正式回填与放行申请的既有原子性需要保留；此次没有确认这些机制整体失效。
- 不合格审批、返工轮次及冻结分支的现有版本和状态保护需要保留；本轮没有证据把它们追加为新的确定性故障。
- 通知已有持久化意图和重试，当前容量风险不等于通知必然丢失。
- 当前资料接口已有归属和权限检查，R04 不等于已确认任意跨单上传；资料类别角色矩阵仍应以现有实现和明确业务要求核对。
- 历史追溯可用与签名证据完整是两件事。修复资料签署边界时应完善证据，而不是增加人工归档负责人；系统归档的业务定位保持不变。

## 12. 完成判据

本方案实施完成的标准是：旧页面不能覆盖新数据，发布内容不可变；签署资料与追溯证据一致；文件异常不制造不可恢复的数据库引用；密码失败计数不被业务回滚清除；偏差关闭可定位实际操作者和依据版本。容量结论另外以实测给出，不用“代码看起来有锁”或“单人流程成功”代替。

## 13. 20261019 工作区实施记录

实施位置：`E:/IntRuoyiWorktree/20261019`，分支 `codex/20261019`。基于本次审查时主工作区的代码建立工作副本；未修改主工作区，未提交或推送。

| 风险 | 已实施的行为 |
|---|---|
| R01 | QA 草稿保存携带独立编辑修订号，保存与发布按同一父记录取锁；旧页面保存明确失败，不再覆盖新草稿。导入、JSON 发布及通用规程套升版入口同步携带修订依据。 |
| R02、R03 | 生产、PQC 普通补正携带所见修订号和内容摘要，服务端在事件锁内比较；签名挑战绑定补正前后内容。本人退回补正继续使用独立的退回审核版本保护。 |
| R04 | 活跃订单上市放行启用 v2 签署正文，将资料集合修订号、文件归属、大小和内容摘要纳入签名；决定审计保存清单快照。追溯通过签名事实校验后读取已签清单，页面增加“已签资料”入口。 |
| R05 | 上传前独立提交持久化上传意图，外部存储操作移出业务锁；文件关联、资料修订与审计原子提交。过期未绑定文件由有界清理任务处理；已绑定文件保留，通用文件接口禁止覆盖、移动或删除保留目录。 |
| R06、R08 | 偏差关闭携带所见处理版本和摘要，在锁内复核全部有效签名，并将实际关闭人、依据版本、摘要及签名集合与关闭审计同事务保存。 |
| R07 | PQC 放行先独立完成密码认证，再进入业务锁并重新校验身份状态；业务失败不再抹除密码错误次数及认证事件。签名使用既有认证与 V2 签名体系，下游证据读取同步适配。 |

数据库结构脚本：`IntRuoyiBackend/sql/mysql/20261019_edhr_concurrency_and_close.sql`，包含 QA 编辑修订、资料集合修订、偏差关闭事实及持久化上传意图表。**脚本尚未执行，部署新版前必须先应用该结构迁移；前后端协议须一起更新。**

历史签名不回写、不重签；旧策略资料未独立签署时，页面明确说明，不能冒充新策略证据。C01—C03 仍为待实测的容量优化项，本次没有进行 30 用户压测，也未据此修改通知及历史分页语义。

验证结果：

- 后端完整主代码编译通过；最终补充编译及放行请求内部字段契约检查通过。
- 18 个定向测试类的最新报告合计 **288 项通过，失败 0、错误 0、跳过 0**。该数字按各类最后一次有效回归报告汇总，并非一次全项目测试；包含文件清理事务、签名证据链、认证事务边界、旧页面保护、偏差关闭及通用规程套升版。
- 全量前端 `vue-tsc`（项目默认 `tsconfig.relaxed.json`、8GB 内存）通过。检查发现的时间线修订号类型遗漏已经修复，最终退出码为 0。
- 受影响的前端定向静态检查通过；旧 `role-matrix-qa-regulation-static.spec.cjs:95` 检查仍失败：其正则要求的旧 PQC 组包写法与当前 `FrontlineFixedTemplatePanel.vue` 不一致。该页面与本次审查基线的 SHA-256 完全相同，本任务未修改它；不以该旧正则判断实际 PQC 功能通过或失败。
- 正式前端构建未完成：Vite 构建阶段长时间无 CPU 进展且 Node 内存占用约 5 GB，已停止本任务启动的进程。构建日志只有 Vite CJS API 弃用及 Browserslist 数据过期警告，没有构建成功或失败结论；前端类型检查已通过。

验证期间出现过 Node 默认 4GB 上限和并发 Java 内存不足，均未作为业务测试通过；最终类型检查采用项目原有 8GB 配置，最后 39 项后端回归使用有界 Java 内存并成功。前端依赖来源核对为相同锁文件，验证缓存放在任务自有目录，不修改其他任务的依赖缓存。

数据库迁移尚未执行；未启动业务服务，未执行真实 E2E、30 用户压测或业务数据写入。
