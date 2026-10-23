# DCC 三套流程技术设计

本文件是三套流程的技术设计基线。业务规则以 [需求文档](README.md) R01-R09 为准，设计默认值见 D01-D07；验证对应 [验收计划](acceptance.md)。后续已有部分代码实施与开发期验证，当前实现状态、剩余 blocker 和验证证据以 `doc/tasks/20260921-dcc-three-workflows-implementation/` 为准；本文不表示真实数据库、正式 Flowable 部署或真实页面 E2E 已通过。

## 1. 当前源码证据

以下路径均相对仓库根。为便于阅读，路径前缀约定如下，不能作为运行目录或部署路径：

- `DCC` = `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`
- `BPM` = `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/`
- `SYSTEM` = `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/`
- `WEB` = `IntRuoyiFronted/src/`

| 编号 | 路径及方法/字段锚点 | 核实的当前行为及改造意义 |
|---|---|---|
| S01 | `DCC/service/file/DccControlledFileWorkflowServiceImpl.java`：`BPM_PROCESS_DEFINITION_KEY`、`submitControlledFile`、`submitWorkingIteration` | 上传与工作稿送审共用 `dcc-controlled-file-approval`；重提、签名审批校验也引用该常量，不能只改新提交入口 |
| S02 | 同文件：`persistApprovalRouteSnapshots`、`createApprovalProcess` | 提交时保存全部 resolvedUserIds，随后启动 BPM；变量未传 needTraining。现有快照可参考，但冻结时点和粒度不满足新需求 |
| S03 | `DCC/service/route/DccFixedApprovalRoutePolicy.java`：`FIXED_STAGE_MAP` | 校验固定四阶段：文控审核、会签、批准、文控批准 |
| S04 | `DCC/service/file/DccControlledFileApprovalRouteAssigneeResolver.java`：`resolveApprovers` | 仅 USER/POSITION；使用用户集合；无部门唯一负责人及逐部门任务合同 |
| S05 | `DCC/service/category/DccCategoryApprovalMatrixAdminServiceImpl.java`：`FIXED_STAGE_MAP`、`getActiveMatrixPositionIdsByCategoryIds` | 类别审批矩阵依赖岗位和固定阶段，不能把已存在的浏览权限 DEPT 矩阵误作审批会签能力 |
| S06 | `SYSTEM/dal/dataobject/dept/DeptDO.java`：`leaderUserId`；`SYSTEM/api/dept/DeptApi.java`：`getDeptList` | 已有部门单一负责人和跨模块读取接口；`DeptRespDTO` 已返回 leaderUserId，无需新增第二套负责人主数据 |
| S07 | `WEB/views/system/dept/DeptForm.vue`：`leaderUserId` | 部门管理已有单选负责人控件，可复用并补校验 |
| S08 | `DCC/controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java`：`selectedSignoffUserIds` | 当前工作稿送审 VO 只有幂等键和会签人集合，没有培训选择；上传页有控件不能证明升版入口完整 |
| S09 | `WEB/views/dcc/controlled-file/upload/index.vue`：`needTraining` | 上传保存培训布尔值，当前使用 switch，需要按需求改为 checkbox，并覆盖升版/重提 |
| S10 | `DCC/service/file/DccControlledFileFinalizationServiceImpl.java`：`finalizeOrdinaryApproval`、`finalizeRevision` | 普通文件 ordinaryFile 跳过培训分发计划，培训判断仅对非普通文件执行；普通流程最终审批后内部 READY_TO_PUBLISH 随即生效，不是独立人工发布 |
| S11 | 同文件：`createTrainingRecords`、`createDistributionRecords` | 培训依赖类别 trainingRequired 与分发对象计划；分发还受 distributionRequired 影响，均须与新流程门禁解耦 |
| S12 | `DCC/service/file/DccTrainingAssignmentAckServiceImpl.java`：`acknowledgeTraining` | 全部培训确认后只把文件改为 PENDING_MANUAL_DISTRIBUTION；没有新流程的 BPM 节点推进 |
| S13 | `DCC/service/file/DccTrainingTaskServiceImpl.java`：`loadTrainingVisibleFile`、`loadPublishedFile` | 培训访问受文件状态和 publishedFileId 限制，必须支持待生效内容快照，不能只移动节点顺序 |
| S14 | `DCC/service/file/DccDistributionReceiptServiceImpl.java`：`acknowledgeElectronicDistribution`；`DCC/service/file/DccDistributionTaskServiceImpl.java`：`getMyDistributionTaskPage` | 有分发人员及回执能力，状态准入与流程完成条件需适配，发消息不等于收到回执 |
| S15 | `DCC/service/file/DccControlledFileObsoleteServiceImpl.java`：`obsoleteControlledFile`、`applyApprovedObsoleteControlledFile` | 作废经过 FormCenter OBSOLETE 动作；允许传 startUserSelectAssignees，否则复用旧路线；正式效果执行状态变更、存储处理和通知 |
| S16 | `DCC/service/file/listener/DccControlledFileStatusListener.java`：`getProcessDefinitionKey`、`FORM_CENTER_BUSINESS_KEY_PREFIX` | 仅监听旧 key，显式忽略 FORM_ACTION 业务键；新流程须分别接入，不能对作废重复执行效果 |
| S17 | `BPM/framework/flowable/core/candidate/BpmTaskCandidateInvoker.java`：`calculateUsersByTask`、`requireConfirmedDccCandidates` | 旧 DCC key 有严格候选校验；其他 key 可能进入禁用过滤、空人补人、跳过发起人通用策略，新三 key 必须显式纳入严格合同 |
| S18 | `DCC/service/file/DccFrozenApprovalSignatures.java`：`requireComplete` | 当前签名完整性依赖旧路线和用户；新合同应检查每个部门任务，不按唯一人员数替代部门数 |
| S19 | `DCC/approval/DccApprovalTaskAdapter.java`：`DCC_PROCESS_DEFINITION_KEYS`；`WEB/api/dcc/controlledFile/workflow.ts`：`CONTROLLED_FILE_PROCESS_DEFINITION_KEY` | 待办、时间线、入口及前端都存在固定 key，需要联动更新 |
| S20 | `DCC/service/file/DccControlledFileObsoleteFormEffectExecutor.java`：`execute` | 作废效果仍由表单中心消费，需绑定当前作废申请、流程及事件，而非只根据原文件 changeType |
| S21 | `DCC/service/file/DccPaperDistributionAckServiceImpl.java`：`acknowledgePaperDistribution`、`replacePaperRecipients` | 当前由有 DISTRIBUTE 权限的操作者录入领取人并置整条 ACKNOWLEDGED，不能直接视为逐领取人本人签收；需分离发放登记与完成证据 |

历史 BPM XML 来源还包括 `IntRuoyiBackend/sql/mysql/20260717_dcc_applicant_rework_bpmn_contract.sql`，但脚本内容不能证明运行库当前部署模型。实施时必须通过正式模型管理读取目标租户的定义、版本和绑定，只读预检后再部署。

## T01. 流程身份、状态与节点合同

新建三个独立定义，key 见需求第 3 节。新增集中映射策略，以服务端业务动作选择定义，禁止客户端指定任意 processDefinitionKey；缺定义、定义未启用或动作绑定不匹配直接失败。文件工作稿的提交动作按正式来源/版本关系确定：首次上传返工仍为 NEW，已受控文件修订才为 REVISION，不能按入口名称或版本字符串猜测。

拟新增阶段使用语义标识，不再由 stageNo=1/2/3/4 推导状态：

| 阶段码（拟定） | 类型 | 下一阶段 | 门禁 |
|---|---|---|---|
| SIGNOFF | 部门并行人工会签 | APPROVAL | 每个部门任务同意 |
| APPROVAL | 人工批准 | TRAINING / DISTRIBUTION / DOC_CONTROL_REVIEW | 根据服务端冻结的 actionType、needTraining 分支 |
| TRAINING | 业务任务聚合等待节点 | DISTRIBUTION | 正式培训证据全部完成 |
| DISTRIBUTION | 业务任务聚合等待节点 | DOC_CONTROL_REVIEW | 正式分发证据全部完成 |
| DOC_CONTROL_REVIEW | 人工文控审核 | FINALIZING | 前序证据完整、有效签名、内容版本一致 |
| FINALIZING | 系统效果处理 | COMPLETED，文件 ACTIVE / OBSOLETE | 业务效果实际成功，失败保持可重试 |

新流程状态由独立工作流记录表达：`IN_PROGRESS / FINALIZING / COMPLETED / REJECTED / WITHDRAWN / FINALIZATION_FAILED`；`stageCode` 表示当前环节。上传/升版候选行可投影现有 pending 状态，但它不是第二套可独立推进的状态机。作废实例单独挂在原受控文件上，不把原文件状态在提交时改成审批中，更不能覆盖其历史上传 processInstanceId。

撤回仅允许尚未完成最终文控审核的 IN_PROGRESS 申请，并校验申请人/现有撤回权限；培训或分发等待期间仍允许撤回并执行取消收口。最终文控已通过、处于 FINALIZING/FINALIZATION_FAILED 后走授权重试，不允许把已完成审批重新当作可修改草稿。

培训未选择时记录为条件跳过，不创建伪完成任务；作废模型根本不包含培训与分发。模型部署校验必须检查节点、边、条件、候选策略、全部部门通过、禁止自动跳过同人/发起人，以及最终效果处理的完整性。

BPM 中 TRAINING/DISTRIBUTION 采用正式业务等待节点，由受控业务服务在证据完整时推进对应 execution；不是审批中心普通“同意”按钮。聚合完成命令必须校验 tenantId、workflowId、processInstanceId、executionId、stageCode，不能用裸文件 ID 触发任意节点。

## T02. 会签矩阵及部门唯一负责人

复用类别路线/节点配置，新增 actionType 维度和显式新合同版本；有效路线按 `(tenantId, categoryId, actionType)` 唯一，历史版本保留。保存新版本与启用切换在事务内完成，锁定配置身份并校验乐观版本；数据库有效行唯一约束不能简单使用 `(categoryId, actionType, active)`，否则会限制历史 inactive 行数量，具体 DDL 在核对现有迁移后确定。

SIGNOFF 只允许 `candidateSourceType=DEPT`，列表仅保存稳定 deptId；拒绝重复部门、空集合、USER/POSITION/ROLE、前端提交负责人 ID。批准、文控配置继续独立保存，不要把浏览矩阵、岗位任命和会签矩阵混用。类别矩阵批量维护、单条维护、种子导入、完整配置导入导出必须使用同一新合同；禁止旧导入覆盖新部门配置。

负责人主数据唯一来源为 `system_dept.leader_user_id`（实际列名需实施前以迁移复核）。不新增 DCC 负责人映射表，不根据部门员工、系统岗位、上级部门、姓名或账号字符串反查负责人。API 按正式 ID 解析；shanglei/xujianghai 只作为业务示例，不是种子 ID。

系统部门 API 扩展批量、可保持事务一致性的负责人快照读取能力。对每个请求部门要求恰好一条正式部门结果、恰好一个非空 leaderUserId，再检查部门启用、账号存在且启用、同租户及既有审批/签名资格。必须先验证源集合基数，不能先 distinct 或取第一条掩盖重复结果。

部门单选保存接口拒绝数组、多值或非整数，DCC 解析器对非单结果显式报错。部门未参与 DCC 时仍可尚未配置负责人；DCC 配置启用及任务创建两处执行严格必填检查，避免无关系统部门维护行为被扩大改变。

## T03. 快照时机、多部门任务与并发

拆开三类信息：提交时的配置快照、创建节点任务时的负责人快照、实际办理后的签名证据。历史页面不得用今天的负责人重新拼装过去的事实。

建议在会签多实例集合展开前的同步执行钩子中完成整批部门解析：

1. 按既有锁序取得 Master/候选、workflow，再按 deptId 排序取得部门当前读取锁；读取正式当前行，不能用可能过期的部门缓存或预览 DTO。系统部门更新同样受数据库行锁约束。要求 System、DCC 和 Flowable 参与同一可验证的本地事务，实施前通过集成测试证实，不能仅依据 @Transactional 推断。
2. 校验本轮是否已有完整节点实例/分配快照。相同幂等创建重放返回已创建结果；不重新读取新负责人来覆盖旧快照。
3. 对提交时冻结的全部部门读取当时负责人，整体校验；构造元素为 `{departmentId, assigneeUserId, obligationId}` 的集合。
4. 先生成每部门独立义务，再展开 BPM 任务，保存 taskId、nodeInstanceId 和分配快照；任何失败回滚整批创建。首次提交时会签就是首节点，失败应回滚提交本身。
5. 模型直接使用各元素的 assigneeUserId 分配，但集合身份是部门义务。不能经过仅支持 `Set<Long>` 的候选集合后丢失相同用户的第二个部门义务。
6. 完成任务时按 taskId 校验冻结 userId、当前账号及电子签名资格；所有部门义务完成后仅推进一次。并发最后两人完成须锁本轮聚合并使用当前锁定读汇总。

负责人配置变更以该批任务创建事务中的读取为边界；更新发生在读取之前使用新值，之后使用旧值。任务创建失败并完整回滚后再次创建使用届时配置；任务已提交后的网络重试使用原快照。审批任务已创建后负责人停用不自动转办；保留任务，按需求第 4.3 节恢复。

同一人既是申请人又是部门负责人、或者同时参与批准，不得被通用 BPM “跳过发起人/同人自动审批”策略免掉本次应履行的部门会签。禁止在审批中心通用转办/加签入口绕过本合同。

## T04. 数据设计与身份边界

下表为逻辑变更清单，不是已核实可执行 DDL。新表统一按现有租户基类、审计字段和 ID 规范落地；schema、长度、索引和迁移 dependsOn 必须在实施任务核对正式迁移后确定。

| 对象 | 拟新增或调整的关键字段 | 约束与用途 |
|---|---|---|
| 类别路线/节点 | actionType、contractVersion；SIGNOFF 的部门 IDs | 三动作独立有效配置，保留已有版本历史 |
| `dcc_controlled_file_workflow`（拟新增） | id、tenantId、controlledFileId、masterId、actionType、attemptNo、processDefinitionKey/Id、processInstanceId、formInstanceId、routeVersionNo、needTraining、status、stageCode、submitIdempotencyKey、payloadHash、approvedAt、finalizedAt、error | 一条表示一次业务申请；作废引用原文件并链接唯一 FormCenter 实例，不另造受控版本 |
| 路线配置快照 | workflowId、contractVersion、部门集合、批准/文控来源及通过策略、配置版本 | 新实例按 workflowId 读取，不能只按 controlledFileId 混入历史上传和作废快照 |
| `dcc_controlled_file_task_assignee_snapshot`（拟新增） | tenantId、workflowId、nodeInstanceId、stageCode、departmentId、departmentName、assigneeUserId、assigneeName、leaderConfigVersion/摘要、resolvedAt、bpmTaskId、obligationId | 每部门每次节点实例唯一；历史显示名称与身份均保存，负责人更新不回写 |
| 培训/分发主记录、人员和阅读进度 | workflowId、nodeInstanceId、contentSnapshotId/摘要、计划版本、取消状态 | 查询及唯一键加入轮次身份，避免旧轮已完成证据使新轮直接通过 |
| 签名及审计关联 | workflowId、bpmTaskId、obligationId、stageCode、departmentId、内容/版本摘要 | 复用统一签名与 DCC 投影；按部门任务验证完整性，培训/分发回执不能冒充内容审批签名 |

工作流幂等身份为租户、动作、操作者及正式提交键，并比较结构化规范载荷；载荷包含目标具体版本、needTraining、所选正式分发计划等会影响业务结果的字段。同键同载荷只读回放，同键改培训值/目标/动作明确冲突。先校验操作者身份与权限，再读取已提交幂等结果，不能让变化后的当前状态挡住合法重放。

任务分配唯一约束至少为 `(tenantId, workflowId, nodeInstanceId, departmentId)`，另约束 bpmTaskId 关联；不得对 userId 单独唯一。对批准/文控非部门任务使用单独明确的义务身份，不用 departmentId=0 假装部门。

沿用现有业务并发约束并以 Master 当前行锁串行化，同一逻辑文件不得并行启动会相互改变现行版本的升版与作废。初次上传、返工、升版的内容身份必须按已有正式链路确定；内容一旦进入本轮不可原地替换，修改后形成新轮和新证据。

## T05. 培训和分发的真实执行

### T05.1 计划与开关

在提交时冻结分发部门、介质、人员及培训时长等参数。来源复用现有单文件正式分发配置或类别分发规则，由服务端通过既有优先级解析后返回一份正式计划；不是由会签部门推断收件人。新流程分发必需，类别 distributionRequired 不能跳过节点，缺适用计划明确阻断。

若 needTraining=true，从同一冻结计划提取培训人员；电子人员复用既有解析，纸质计划必须有明确培训人员，否则报错。计划生成不创建分发消息或签收任务。校验无可用人员时拒绝，不能使用空列表 allMatch=true 自动完成。

培训开关唯一来源是服务端保存的工作流 needTraining，同时写入由服务端控制的 BPM 变量供网关使用；变量缺失、非法或与工作流记录不符报错，不默认 false。`OBSOLETE` 的此值为空且无网关，不复用原文件上传时的 needTraining。

### T05.2 进入培训

批准通过后进入 TRAINING，按 workflowId 和 nodeInstanceId 幂等创建培训主记录、分配、进度及消息；使用本轮冻结的不可编辑浏览版及内容摘要。调整 S13 的状态/产物前置及相关预览 API，未生效也可通过参与人任务权限阅读，不伪造 publishedFileId 来绕过。

保留真实阅读计时、心跳、最低时长和本人确认检查，直接确认接口也执行同等校验。全部完成后在同一事务内更新证据并推进本轮 BPM 等待节点。失败回滚本次确认与推进，重试只对同一业务证据生效；重复确认已有完成记录应幂等，不重复计时或生成任务。

### T05.3 进入分发

needTraining=false 从批准直接进入 DISTRIBUTION，true 从培训完成进入。此时才创建正式分发任务/通知；若保存计划需借用现有 distribution 行，必须有明确 PLAN 状态，不能提前当成已发放。

电子计划采用现有逐接收人回执；纸质采用现有发放和签收服务，不能把打印成功当签收。扩展待生效任务可见性和办理准入，仅授予本轮人员所需访问；每条计划须非空并按介质验证完成，全部完成才进入 DOC_CONTROL_REVIEW。消息 SENT、文件打开、文控点击“释放分发”均不能单独证明签收完成。

S21 目前只有授权操作者的整单确认，实施时须补齐纸质逐领取人确认：发放人登记发放事实，领取人通过本人待办签名确认本轮具体纸件，保存人员记录、签名、时间及内容身份，全部完成后才置整单 ACKNOWLEDGED。新纸质确认复用签名服务但必须有明确 PAPER 合同，不能直接调用仅支持 PUBLIC_FOLDER 的电子入口。不得由管理员或发放人替领取人生成本人签名。

现有 `createDistributionRecipientSign` 可追加接收人，新三流程的分发名单已冻结，必须拒绝该入口对本轮加人；否则会出现新接收人没有参加前序培训。纸质登记同样不得用 replacePaperRecipients 替换本轮冻结名单。

待生效纸件不带正式受控章，必须有待生效标识及可追溯编号。生效后正式纸件经既有受控打印/发放链取得，前述待生效回执不能冒充正式纸件发放记录；不能认为服务器生成受控 PDF 后手中的纸张也自动变为受控。D04 仅定义待生效接收准备这一节点完成标准。

当前 `releaseManualDistribution` 等入口如果可直接激活，必须对新流程禁止此副作用；它们只能完成相应分发业务，随后仍经过文控审核。分发服务不能直接调用 activateRevision。

### T05.4 取消、过期及通知

驳回/撤回后取消本轮所有未完成培训、分发及 BPM 待办；已完成证据保留并标明所属终结轮次，迟到回执/心跳不得推进新轮。已发纸件必须进入现有收回机制或补齐最小可审计收回任务，未收回状态明确显示，不能删除发放记录。此项属于 D04 默认业务口径。

冻结计划中的用户在实际创建任务时已停用，明确阻断并显示具体人员，不静默删人或用当前部门新增人员补齐；重提才能变更已冻结计划。通知复用现有消息任务及提交后投递，发送失败落为可重试失败，不能覆盖已提交业务成功或伪造签收。

## T06. 文控审核、生效、作废与失败处理

文控节点在培训/分发之后或作废批准之后创建。需要当前文控角色/权限、类别权限及本任务资格，审核签名绑定本轮内容与流程。内容批准时间在 APPROVAL 完成时保存；最终文控时间和正式生效/作废时间分别记录，不在盖章重试时重写内容批准时间。

上传/升版的最后审核通过后，由唯一效果执行链进入 FINALIZING，校验全套部门会签、批准、文控签名以及按需培训和必需分发证据，再执行正式盖章和版本激活。不能沿用“必须先上传已盖章 PDF 才能最终批准”的旧判断造成节点死锁，不能由前端任意上传盖章件替换已审批正文。

生成产物先存为未生效、可校验的派生件，数据库成功切换后才作为正式副本可见；外部存储不受数据库事务自动回滚保护。对作废迁移文件同样需要幂等产物清单和失败恢复，不能先不可逆移动唯一原件再假定事务能恢复路径。实施测试必须覆盖存储失败、数据库回滚和重试，保持已有正式版可读取。

正常生效在锁定 Master 后复核候选高于当前正式版，更新新 ACTIVE、旧 SUPERSEDED、过期 WORKING 收口及当前指针，绑定签名、审计和通知任务。同一最终事件重放只读取既有结果，不能重复派生文件或再次替代其他版本。业务状态投影失败不得返回受控成功。

作废保留现有 FormCenter OBSOLETE 申请入口和效果执行器，绑定独立作废流程 key；业务键仍采用表单中心正式 FORM_ACTION 身份，并通过表单/工作流记录定位 controlledFileId。原文件 changeType 可能仍为 NEW/REVISION，不能因此错走上传生效；不得把 FORM_ACTION 直接转 Long 或覆盖原上传流程字段。

作废只有一个效果所有者：FormCenter 的正式效果执行器，DCC 通用 BPM listener 不再重复作废。执行器验证本次实例、动作、目标版本和最终文控通过事件，复用原文件权限、存储、名称释放、Master 指针与通知语义；客户端 startUserSelectAssignees 不能覆盖部门矩阵。作废失败显示本次效果失败，原文件不能提前显示 OBSOLETE。

审计分别记录申请人、最终审核人和效果重试操作者；当前执行器传入 applicantUserId 的行为不能被用来宣称最终文控已签名。效果依据必须是完整本轮审批证据，系统执行身份不冒充人工签名人。

审批已经完成但效果失败时，前端展示“审核通过，生效/作废处理失败”及原因；工作流业务状态为 FINALIZATION_FAILED，不能仅因 BPM 已结束便显示业务完成。重试必须校验真实操作者和权限，按同一事件幂等继续，不再要求审批人补签一次。

## T07. 接口与页面合同

复用现有 Controller 和 API wrapper；下表是需要修改的合同，不创建平行业务入口。正式 URL 前缀以对应 Controller 的 RequestMapping 为准。

| 入口 | 新请求/响应合同 | 服务端约束 |
|---|---|---|
| 类别审批矩阵读取、预览、保存、导入导出 | actionType、routeVersion、signoffDepartmentIds、独立 approval/docControl 配置 | 会签仅 DEPT；缺配置报错；保存时并发版本检查 |
| `/route-preview` 和 readiness | actionType、目标身份、needTraining；响应 stageCode 序列、部门、当前负责人、blockers | 预览不创建任务和最终人员快照；提交不可直接信任预览结果 |
| 上传 `/submit` | 显式 needTraining、正式身份、幂等键、分发计划引用 | NEW 服务端选择 upload key；拒绝人员覆盖和任意 key |
| `/{id}/submit` 工作稿送审 | 增加必填 needTraining；移除新合同中的 selectedSignoffUserIds | 正式源关系区分 NEW 返工与 REVISION；幂等 hash 纳入培训值 |
| 撤回/驳回后重新送审 | 新轮 needTraining、修订内容/计划和新幂等键 | 不能复制旧人员快照或重复使用旧轮培训结果 |
| `/{id}/obsolete` | 原目标 ID、原因、幂等键及既有必要影响确认 | 服务端绑定 obsolete key；拒绝 needTraining 和人员覆盖 |
| 培训确认/电子及纸质签收 | 正式任务/人员记录 ID，服务端关联 workflowId/nodeInstanceId | 本人、当前节点、本轮证据和状态；不能修改其他人 |
| 文件详情/审批中心/历史 | workflowId、actionType、processDefinitionKey/Id、业务状态、当前阶段、逐部门快照、培训分支及分发进度 | 展示已保存事实；失败与条件跳过分别表达 |

新接口合同不默默接受并忽略废弃会签人员字段，提交了越权字段须报错。前端移除手选会签人；批准和文控配置仍由管理端治理。审批节点的查看权按冻结分配和任务授权，不能因为当前部门负责人变化剥夺原执行人的必要预览权；不扩大下载授权。

前端具体改造位置：上传页、文件详情内工作稿送审/升版/作废弹窗、类别管理矩阵组件、培训 task/mine/execution 页面、分发页面、审批中心适配和 `workflow.ts`。上传与升版控件都必须是 checkbox；表单重置默认 false，编辑未提交草稿回显其值，送审后只读。

## T08. 跨模块改造清单

| 模块 | 必须覆盖的代码边界 | 不能遗漏的行为 |
|---|---|---|
| DCC workflow | S01/S02/S08，RouteReadinessService，Controller/VO | 所有送审、返工、重提、工作稿入口，版本策略和幂等 |
| DCC 配置 | S03/S04/S05，ApprovalRouteAdminService，ApprovalMatrixSeedService，AdminFullConfigPackageService | 固定四阶段退出新合同，三动作配置唯一、导入导出一致 |
| System | S06/S07、DeptSaveReqVO/DeptService | 单负责人合同、租户/有效性、批量事务读取、并发变更 |
| DCC 任务与权限 | RouteSnapshot、TaskAssigneeSnapshot，ReviewMatrixAccessService，QueryService | 逐部门快照，现任负责人不替代旧任务执行人，历史可查 |
| DCC 签名 | S18、SignatureService、SignatureBindingService | 三动作 stageCode、每部门义务、签名意见/图片/本人验证 |
| BPM | S17，BpmTaskExternalSignatureGuard，BpmMessageServiceImpl，ApprovalModuleIntegrationDeclarations | 新 key 严格候选策略，禁止普通批准入口跳签名；消息和模块登记 |
| DCC 待办/事件 | S16/S19/S20，ControlledFileMapper 中 key 过滤 | 三 key 识别；作废 FORM_ACTION 归属；同一待办不被两个适配器重复收录 |
| 培训/分发 | S11-S14，PaperDistributionAckServiceImpl，相关 Mapper/状态过滤 | 待生效可办理、轮次隔离、完成才推进、拒绝迟到回执 |
| 生效/作废 | S10/S15、平台 ControlledContent 适配器、PendingActionGuard | 最后文控后才执行；旧正式版保护；状态/审计/统一受控候选同步 |
| Web | S07/S09/S19 及 T07 页面 | 五路径预览、无越权改人、三动作入口和错误反馈 |

实施时再次全仓定向检索旧 key、四阶段枚举、stageNo switch 和 selectedSignoffUserIds；逐个区分新流程运行入口、其他业务与历史只读展示，不能机械全局替换。DCC 对 BPM 能力通过现有 API/扩展接口调用，避免 BPM 反向依赖 DCC 实现类形成循环依赖。

## T09. 迁移、启用与历史

1. 只读盘点目标租户的路线、负责人、分发/培训参数、正式模型及 FormCenter 策略；列出所有旧 key 和旧 OBSOLETE 策略的在途实例。本设计文档不记录生产库查询；实施任务已记录获批本机 Docker MySQL 测试库的迁移执行及正式 Flowable 部署表只读核验，详见 `doc/tasks/20260921-dcc-three-workflows-implementation/`。该测试库证据不代表生产环境已盘点或部署。
2. 提供带 release-migration 元数据及完整依赖的增量 schema。新合同仅用于新实例，不把历史快照改写成部门会签，不给历史记录虚构 leader/task 时间。
3. 管理员通过正式配置界面明确维护各动作部门矩阵、批准人、文控及计划。岗位与部门不存在可靠一一对应，不自动按名称迁移；缺映射按类别/动作列为启用阻塞。
4. 三份 BPM 定义通过正式模型部署机制发布，保留独立 key、版本和内容指纹；禁止修改 ACT_GE_BYTEARRAY 冒充模型部署。FormCenter OBSOLETE 策略绑定目标 key，并校验不会直效绕过审批。
5. 采用 D07 的切换窗口：先停止目标范围旧流程的新发起，旧实例办结或经业务授权撤回，再切换新三流程与页面/接口合同。无法证明旧在途为零则阻止启用，不能自动取消实例。
6. 历史流程按原定义及已有快照只读展示，缺历史字段明确显示“未记录”，不从当前部门配置补齐。已存在但未送审的草稿在新合同下必须重新选定培训并通过配置校验。
7. 切换后阻止旧 submitWithoutApproval、手工 release/activate、旧 FormCenter UPLOAD 或“小版本检入直效”等入口绕过本次三动作治理；不改变明确不在范围的其他业务流程。
8. 新流程产生实例后不允许用简单程序回滚到不识别新 schema/key 的旧版本；故障时暂停新发起、保留证据并前向修复。所有真实 DB 写入、模型部署、服务重启和发布由实施阶段按当轮授权执行。

迁移验证至少覆盖首次/重复执行、完整 dependsOn 门禁、历史业务行零改写、租户隔离、有效配置唯一性与索引容量。Git 提交、远端发布和 E2E 都不是本次文档交付已执行事项。

## T10. 错误合同与实施批次

拟定错误语义如下；正式错误码数值由实现时在现有 ErrorCodeConstants 中分配，不能与已有编码冲突。

| 错误语义 | 用户提示要点 | 写入/推进结果 |
|---|---|---|
| WORKFLOW_DEFINITION_MISSING / MISMATCH | 某动作流程未配置、未部署或定义不匹配 | 不启动流程 |
| SIGNOFF_MATRIX_EMPTY / INVALID | 会签部门为空、重复或类型错误 | 不启用、不送审 |
| DEPT_LEADER_MISSING / MULTIPLE / INVALID | 明确部门及缺失、多值、停用、跨租户原因 | 本批任务零创建 |
| PARTICIPANT_NOT_QUALIFIED | 某人员缺岗位、权限或签名资格 | 不降级换人 |
| TRAINING_FLAG_REQUIRED / INVALID | 培训选项缺失或非法 | 不默认跳过 |
| DISTRIBUTION_PLAN_MISSING / TRAINING_RECIPIENT_EMPTY | 缺对应计划或培训对象 | 不跳过必经节点 |
| TASK_SNAPSHOT_MISMATCH / STALE_STAGE | 任务不属于本轮、操作者不符或阶段已终结 | 无签名/推进副作用 |
| IDEMPOTENCY_CONFLICT | 相同键对应不同动作、版本或选项 | 不创建第二实例 |
| FINALIZATION_FAILED | 保留实际失败原因和本次实例 | 不显示受控/作废成功，可授权重试 |

| 批次 | 范围 | 交付门禁 |
|---|---|---|
| P1 | T02-T04：动作配置、唯一负责人、逐部门任务身份、schema | 先 BDD/RED，负责人基数、冻结时点、同人多部门及事务回滚 GREEN |
| P2 | T01/T07/T08：三定义、所有发起入口、待办/签名/权限 | 三定义独立；会签全部通过；普通 BPM 入口不能跳过业务签名 |
| P3 | T05/T06：真实培训、分发、最后文控、生效/作废 | 五条正常路径和提前推进拒绝；待生效文件真实可读可办理 |
| P4 | T09/T10：迁移、历史、错误、并发、前端完整验收 | 定向回归、迁移门禁；获得当轮授权后才执行真实页面 E2E |

四批均为同一需求范围。文档已完成和开发期定向测试通过都不等于 P1-P4 的最终运行验收完成；实现不得以只交 P1/P2 或缺少真实 DB、正式 Flowable、真实页面 E2E、Git 收尾证据的状态宣称流程已可用。
