# G06 最小端到端合同与阶段接入清单

## A08 已实现服务与后续调用顺序

2026-10-15 A08只授UploadService、AttachmentService/Impl、CheckinReqVO及两个专属测试。以下正式方法已TDD实现，原SPEC_ONLY中其余公共合同仍是待核准设计。

```java
void bindCheckinAttachments(Long actorId, Long baseFileId, Long workingFileId,
    List<DccControlledFileAttachmentUploadTicketReqVO> tickets);
void freezeOrdinaryAttachments(Long actorId, Long selectedFileId, Long candidateFileId,
    List<Long> expectedOrdinaryAttachmentIds);
```

- Query后续需在同一checkin事务创建新WORKING、完成合法草稿/位置事实后，且**释放有效checkout之前**调用bindCheckinAttachments。传明确完整清单（[]为无附件，null拒绝）。服务校验有序的独立session票据，all resolve→all markBound→真实bindAttachments，票据要求AVAILABLE，禁止旧BOUND票据重绑定。
- 服务要求base和target同tenant/Master/project/category/type/fileNumber/directory；target.predecessor=base、requester=检出人、未送审WORKING、target沿用base的formal身份且版本递增。目录真实projectFolder不是ControlledFileDO字段，由原PublicUploadPlacementService权威记录校验；未捏造DO字段。
- 控制器多附件字段已存在，但Query未接入。因此目前传票据不能冒称已保存。后续Query精确重放仍须按BOUND票据比对完整清单，不能走新bind命令再次绑定。
- Revision后续在insert并准备target、inherit正式位置后、同一事务内调用freezeOrdinaryAttachments。target为正式候选WORKING，selectedIteration/predecessor都指精确source；PARTIAL/INITIAL只允许所选稿requester，REPLACEMENT走正式project OWNER（可非原附件上传人）。
- 正式冻结读取所选稿全部ATTACHMENT；传入expectedIds必须完整、有序、唯一，无推断。每行正式用途/版本/tenant/上传人/时间/order/hash/物理身份校验；TRAINING_RECORD不复制。当前实现始终经SourceOwnership创建并校验独立文件副本，不依赖未经证明的存储可变性假设。副本每创建一项立刻enlist到caller事务回滚清理，第二项或晚阶段失败全部清理。
- 新target附件保持原uploadedBy/username/nickname/uploadedAt/hash/order；applicationProcessInstanceId暂空，正常Workflow启动新BPM后现有bindApplicationRound写入新轮次。source行及已BOUND源票据不变，不给OWNER伪造上传事实。已冻结但未BPM时精确重放校验全清单与字节，零新增；BPM已启动后的提交重放仍由Workflow外层既有幂等入口处理。
- 当前未修改正式submitIteration期望字段或payloadHash；完整清单确认、expectedOrdinaryAttachmentIds与server manifest幂等仍待G01/G03公共合同/Root授权。
- UploadService新增CHECKIN+ATTACHMENT分支仍调用原validateSourceUploadContext；新增正式位置门禁只作用于该分支，没有改变SOURCE/DRAWING/NEW_UPLOAD/训练分支。每附件独立client session→dcc-checkin:<baseId>:<hash32>→ATTACHMENT slot。多票不得同槽。
- private javac只编译本阶段4个生产、列明票据/SourceOwnership/事务/Mapper依赖及测试；账户/权限/位置/物理客户端端口使用隔离mock，实际目标附件与ticket由H2/Mapper持久化。尚不能证明Query/Revision/Workflow集成或真实页面。

Root阶段review通过后再依文件归还授权整体Query/browser/Revision/Workflow/API/confirmation TDD，当前方法未被真实用户链路接线。

## 当前阶段

以下为第一阶段SPEC_ONLY历史设计；已实现范围以上方A08为准，其余下列逐文件申请仍未授。第一阶段基线分支 int_qms，HEAD 822c31fc0856516cf59ba9cd84bf2bc7f455d17f；实际观察源码含并行未提交改动，以 source-baseline.json为历史观察、a08-before/after.json为本阶段指纹。

## 当前源码事实

|链路|当前锚点|本轮复核结论|
|---|---|---|
|检入上传入口|BE service/file/DccControlledFileUploadServiceImpl.java，uploadPreviewFile，CHECKIN 分支约112行|在执行正式上下文校验后，仍明确拒绝 ATTACHMENT；不是单纯前端控件缺失。|
|检入权限|BE service/file/DccControlledFileWorkflowServiceImpl.java，validateSourceUploadContext，约840行|校验 category UPLOAD、租户/类别/版本、有效检出记录及实际检出人、项目 EDIT/OWNER；必须保留。|
|检入请求|BE controller/admin/file/vo/DccControlledFileCheckinReqVO.java；FE workflow.ts ControlledFileCheckinReqVO|只有源票据、图纸PDF票据、会话、修改说明/备注/培训；无普通附件清单。|
|检入保存|BE service/file/DccControlledFileQueryServiceImpl.java，doCheckinControlledFile，约704行|创建 WORKING、初始化草稿事实、继承归档/关联、绑定源票据并释放检出；没有普通附件持久化。|
|检入页面|FE browser/index.vue，检入弹框约1005行、submitCheckin约2130行|只提供一个源文件与图纸配套PDF；普通多附件没有可操作入口与请求载荷。|
|正式升版|BE service/file/DccControlledFileRevisionServiceImpl.java，freezeCandidate约275行、payloadHash约373行|复制所选工作正文和图纸、创建新正式候选；不冻结普通附件。target.trainingRecordFileId 明确清空，保持此边界。|
|工作稿送审|BE service/file/DccControlledFileWorkflowServiceImpl.java，submitWorkingIteration约916行|正式候选创建后启动BPM，bindApplicationRound只为已有目标附件写首次轮次；它不会把所选工作稿附件带到目标。|
|确认框|FE detail/DetailApplicationPanel.vue，confirmation约629行、submitRevision约665行|读取 selectedApplicationFile，展示正文/基线/目标和申请配置，但不列附件。已执行行为 RED，见验证报告。|
|上传页另一条入口|FE upload/index.vue，handleAttachmentFileChange/resolveReadyAttachmentUploads/handleSubmit；submitter.ts buildUploadPayload；BE Workflow resolveSubmitFiles|已有多附件上传状态、确认摘要和attachmentUploadTickets，逐项使用独立会话；不能笼统宣称所有升版上传都没有附件。本阶段只是只读复核，不抢占G01/G03文件。|
|正式附件服务|BE service/file/DccControlledFileAttachmentServiceImpl.java，bindAttachments约113行、bindApplicationRound约178行|在MANDATORY事务中要求ATTACHMENT票据已BOUND至本版本，上传人等于该版本requester；保存hash/真实上传人/上传时间及首次BPM轮次。可复用检入绑定；不能拿工作稿BOUND票据再次绑定到正式目标。|
|详情历史读取|BE Query toBrowserRespVO约3408行；FE detail/ControlledFileAttachments.vue、attachment-facts.ts|按精确file.id读取附件并检验attachment.controlledFileId；培训用途、上传人、轮次和时间分别展示。版本隔离已具备基础，未验证同一版本多申请轮次过滤。|
|票据槽位|BE service/upload/DccUploadTicketServiceImpl.java，resolveReusableActiveTicket/findActiveUploadSlotRows；20260811_dcc_upload_slot_idempotency.sql|一个租户/上传人/session/purpose仅有一个活跃槽。多个普通附件不能共用同一个session。|

BE 指 IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/；FE 指 IntRuoyiFronted/src/views/dcc/controlled-file/，workflow.ts 全路径为 IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts。行号仅本次观察定位，正式交还后须按方法重新定位并重算指纹。

## 提议的唯一正式合同

1. 复用现有 upload-preview、DccUploadTicketService 和 ATTACHMENT 用途；不建立第二套上传或附件写接口。CHECKIN 上下文允许普通附件，但保留现有正式授权。每项附件独立生成客户端会话，服务端返回 dcc-checkin:<精确baseId>:<会话hash>；源文件/图纸仍共享其现有源会话。
2. 检入请求增加 `attachmentUploadTickets: [{uploadTicket, sessionId}]`，直接复用 DccControlledFileAttachmentUploadTicketReqVO。[]是明确无新附件；未读清单/缺项不得伪装成[]。每个票据独立校验本租户、实际检出人、category、ATTACHMENT用途、baseId前缀、AVAILABLE且未过期，以及正式存储身份。重复票据拒绝，不按排序取第一项。
3. doCheckin在现有项目→Master/File锁顺序下先解析完整清单，再创建next；在同一事务内markBound每个普通票据至next，再调用真实bindAttachments(next, files)。附件存储失败、行数不是1、第二项异常、草稿/归档/审计/释放检出失败均传播回滚。只新增本次明确选择的普通附件；不自动从旧申请/基线补清单，不复制历史培训。
4. 现有检入要求正文或备注真实变化。G06不擅自增加“仅附件变更即可检入”的产品规则；附件伴随正常检入保存。若要开放仅附件变更，Root须另行核准其无变化判断和审计语义。
5. 新工作稿附件已有精确controlledFileId，BPM轮次暂空，表示已保存而未送审。DetailApplicationPanel读取所选工作稿真实清单，明确普通附件与培训附件；缺清单为读取错误，不显示无附件。confirmation列普通附件完整名称及数量，确认后仍核对所选fileId、完整有序附件身份和原有配置fingerprint。
6. 提议 submitIteration 增加 `expectedOrdinaryAttachmentIds: Long[]`，前端使用精确字符串ID，空清单显式[]；服务端在锁定所选工作稿后对照全部普通附件的有序ID。客户端不能提供上传人/时间/hash事实。未提供、重复、丢失精度、属于其它版本、顺序或集合不同均明确拒绝，不凭当前版补齐。两个正式调用点均在DetailApplicationPanel，应同时更新初始送审与升版送审。
7. AttachmentService提供正式的所选普通附件冻结命令（建议freezeOrdinaryForCandidate）。服务端验证租户、source/target同Master、target.selectedIterationControlledFileId、事务和正式候选；仅复制所选清单ATTACHMENT行到target的新附件行，保留真实上传人/时间/hash/名称/order，applicationProcessInstanceId置空，随后现有bindApplicationRound填写新BPM轮次。原工作稿行及其票据不改；不将BOUND工作票据重绑target，也不伪造新上传事实。OWNER可为非原上传人，冻结不能错误要求OWNER=原附件上传人。现有候选selectedIterationControlledFileId与冻结manifest共同保留来源链，无需先新增迁移。
8. 冻结前验证每项正式存储身份、可读性和SHA；若复用不可变storageFileId，必须由现有文件服务事实证明其不可变且禁止源附件删除破坏目标引用。若无法证明，Root需核准附件物理独立副本及其事务回滚清理范围；不能只复制metadata后宣称版本字节已隔离。本阶段未宣称该存储条件成立。
9. Checkin精确重放不仅核对源票据/备注/培训/图纸，也须逐项resolveBoundFile(ATTACHMENT, existing.id)，对照existing保存清单的storage/name/size/hash/order。同源票据换附件清单不能返回旧成功；相同完整载荷零新增回读。该设计可用已绑定票据与现有附件行比对，不先添加checkout schema。
10. 正式候选creation payloadHash须包含服务端从所选工作稿读取的普通附件manifest（有序attachmentId/storageId/hash/名称/大小/上传事实），submit payloadHash包含期望有序ID。候选插入/第二附件冻结/BPM持久化/轮次绑定/平台记录任一晚失败，目标和所有新附件/BPM/票据事实一起回滚；已保存工作稿清单保持不变；同键相同完整载荷重试成功，同键变清单拒绝。
11. 取消确认仅保留填写与已保存工作附件；不调用submitIteration、不启动BPM、不产生审批签名。不能将“检入已保存”的文件伪称为已送审；刷新失败保持当前已有诚实提示。
12. 版本附件读取继续用精确id。若页面明确选择历史BPM轮次，则普通与培训清单分别按对应applicationProcessInstanceId冻结身份读取，不能覆盖为当前轮次；未收到共享applicationRead/详情轮次授权前，此部分仅提出待接入范围，不能称全部历史验收完成。

## 精确文件转交申请

|完整仓库路径|必要差异/锚点|验收影响/先决依赖|
|---|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileCheckinReqVO.java|增加正式普通附件票据列表与逐项验证|检入载荷；Root公共VO授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadServiceImpl.java|uploadPreviewFile CHECKIN分支允许ATTACHMENT并保留validateSourceUploadContext|真实上传入口；Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java|doCheckin解析/全量绑定；matchesCheckinReplayPayload对照附件|检入/回滚/重放；G07交还，G08/G06浏览排期明确。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentService.java|增加事务内所选普通附件读取/冻结命令|唯一正式服务；Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentServiceImpl.java|逐项读取/验证与冻结target行，保持bindTrainingRecord及上传事实|非原上传人OWNER、历史培训隔离、晚失败；Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileAttachmentMapper.java|需要时增加明确tenant/source/ATTACHMENT FOR UPDATE查询|冻结清单锁读，不改变现有历史读取；Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRevisionServiceImpl.java|freezeCandidate及payloadHash保存普通附件manifest；覆盖initial candidate|正式目标与所选工作稿闭环；Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java|expectedOrdinaryAttachmentIds正式期望字段，空清单显式[]|防止未加载清单被当作无附件；G01/G03交还公共合同后Root授权。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java|submitWorkingIteration在创建目标前核对所选附件清单；目标完成bindApplicationRound|保留原审批/签名/权限/幂等；G01/G03交还后Root授权。|
|IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts|Checkin票据列表、SubmitIteration期望ID，契约校验|调用一致，无新API；G01/G03与Root公共API转交。|
|IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue|检入弹框多附件上传/移除/失败/未完成状态；submitCheckin/取消/切上下文清理|实际用户检入；G08归还或Root明确顺序。|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue|confirmation/submitRevision/submitInitial完整所选普通附件清单及确认fingerprint|取消零提交、清单不可串版；G01/G03交还后Root授权。|

暂不申请写 upload/index.vue、upload/submitter.ts、AttachmentRespVO、AttachmentDO、Controller及schema；现有上传链路和附件事实字段足以先设计闭环。该清单不含自动新增平行API。若历史轮次显示需扩展，先提交实际调用链与精确读模型文件/方法，不自行扩大。

专属新增测试拟为 BE service/file/DccRevisionAttachmentsDatabaseTest.java、service/upload/DccCheckinAttachmentUploadContextTest.java、FE tests/unit/dcc-checkin-attachments.test.cjs 与 dcc-revision-attachment-confirmation.test.cjs。测试fixture仅本任务隔离H2建表/数据；不改BaseDbUnitTest、公共create_tables.sql或其它线程测试。必须Root显式授权这些测试路径与Maven唯一票。

## RED/GREEN/REGRESSION 设计

- 已执行RED：当前实际confirmation/submitRevision输入两份已保存普通附件，断言两名称入确认；失败为未列名称（不是源码字符串查找）。同一运行取消控制用例PASS且零submit请求。此RED无生产修复GREEN。
- 后端入口RED：调用真实uploadPreviewFile，完整合法CHECKIN权限及检出上下文，ATTACHMENT期望返回正式票据；当前明确拒绝。同时测试错误检出人、跨租户/类别/版本、SOURCE/训练用途冒充，拒绝发生在存储和票据写之前。
- 检入事务RED：H2内存独立URL强制断言；真实Query、ticket、attachment mapper/service、draft initializer、Master/checkout、事务。两独立附件票据成功后查新版本两行及BOUND归属；当前模型及接线缺失会失败。第二行insert/markBound/初始化/审计晚失败，核对完整快照、检出锁保留且两票据AVAILABLE；同载荷重试一次成功。
- 正式升版RED：真实Revision+Workflow+附件冻结与Flowable内存引擎，构造所选工作稿两普通附件+历史培训；PARTIAL/REPLACEMENT及合法非上传人OWNER覆盖。查目标两普通行、新BPM轮次、所选旧行/历史训练不变；当前freezeCandidate缺附件会失败。第二冻结insert/BPM轮次update/recordSubmitted晚失败，全目标和候选BPM回滚，原工作稿清单不变。
- 前端行为RED：真实submitCheckin/上传状态函数和实际Vue弹框编译；两READY完整发送，UPLOADING/FAILED/缺响应/保存拒绝零成功，取消保留；迟到上传响应与切base/tenant/用户不污染新上下文。新状态注入名应按授权后实际生产函数绑定，不能造未接线helperPASS。
- 前端确认RED：实际DetailApplicationPanel传入两个附件、历史TRAINING、空/未返回清单、确认中切选版本/清单；完整显示，未返回和变更零写；取消保持填写。固定exact fileId；G08的草稿显示helper接线保留。
- REGRESSION：本阶段已跑35项相邻Node测试，生产授权后按新差异重跑附件、确认、检入权限/图纸/培训、精确版本、取消和重放相关测试。后端沿用DccCheckinDraftInitializationDatabaseTest、DccRevisionDatabaseTest、DccOrdinaryAttachmentFactsTest、DccRevisionRollbackSynchronizationTest定向范围，等待唯一票，不并跑公共target。
- 运行验收：尚无E2E授权；所有隔离单测均不得表述为真实页面、运行服务或真实数据库验收通过。
