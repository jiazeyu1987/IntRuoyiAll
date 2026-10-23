# G03 当前证据与最小接入合同（SPEC_ONLY）

## A20已修类别矩阵入口来源合同

CategoryReviewMatrixTable.vue真实下游是CategoryMatrixDialog，原toCategoryVO从review摘要行拼装缺source/sort实际属性。BE/FE ReviewMatrixRow合同都没有source；本次不扩大BE projection，而是actual点击时读取既有正式getFileCategoryList→唯一categoryId匹配→完整saved CategoryVO open，source严格校验。错误/过期回执不open且本地el-alert显示，public矩阵实际保存策略不变。

专属5/5、G03三文件23/23；大静态审阅矩阵旧Dialog标签断言仍失败，隐藏列静态PASS。fulltypesRoot后续统一票。Table与专属测试请求归还，main custom申请接线继续按application-integration-hunks.md等待G01。

## 最新R03/preview合同

R03撤回前述A14整页leaf严格失败；正式upload-options现返回非leaf(null,null)、合法leaf(合法mode,null)、坏leaf(null,非空uploadBlocker)，字段为approvalSourceMode/uploadBlocker。仅能判断的配置问题逐项block，技术目录错误不catch/过滤。Response明确序列化null，API严格三形态，upload/file-type-options.ts直接保留source/blocker。G01独占upload/index.vue接实际禁选/选中guard与原因显示；正式submit仍严格校验选中type/category/mode/人员。

PreviewSourceContext与reserved SourceContext已在同一纯服务完成；预检无file/round，两个format隔离，bind只能使用实际分配file/round且所有共同身份等值。后续共享token/HMAC/DO/Workflow/Resolver/冻结与驳回实施的精确路径/锚点/最小字段/票据phase见application-integration-hunks.md，该文档覆盖初版预检只有方案的状态。

R03后端10/10、FE32/32；pure preview新RED5fail→GREEN21→REGRESSION23通过。本阶段仍非完整工程或运行验收；shared schema/旧test由Root V01 G05接，Maven无票。

## A10/A14当前正式分类管理与候选接线

2026-10-15已落实来源唯一CategoryDO.approvalSourceMode，DO/SaveReq/Resp与AdminService严格合法来源、新建管理弹框显式PUBLIC初值与管理员CUSTOM选择、旧值缺失报错。管理list/读/更新未知来源不会默认PUBLIC；已有import原字段保持，新import类别明确PUBLIC。

upload-options当前正式Controller委派TaxonomyAdminService.getUploadTaxonomyOptions()（接口签名已接），Impl只读CategoryDO唯一active类型绑定并投影Resp.approvalSourceMode；父路径明确null，所有leaf须唯一active category、mode合法，不按名称/缺matrix推断。原Controller submit权限及管理CRUD保留。FE fileTypeTaxonomies decoder同步严格flat/tree leaf无null来源。

正式迁移20261015_dcc_category_approval_source_mode.sql只增加category VARCHAR(32) NOT NULL无default字段，首次新增分支把既有category明确PUBLIC一次；已有列重放不UPDATE并拒不正确schema/unknown值。完整三文件dependsOn闭包政策门禁与6静态合同通过，未执行MySQL。

精确已实现路径见changed-files.md；BE私有真实当前管理/投影/Controller源码+H2 category端口回归8/8、FE当前定向29/29；这不能证明正式MyBatis/租户拦截器/GxP审计、SQL已执行或Workflow端到端通过。后续Root需合公共create_tables.sql来源列与既有旧类别fixture（详a10-projection-dependency.md），再发完整reactor唯一票。

完整上传页/DetailApplicationPanel/API/文件DO/Resolver仍他owner。本申请custom字段、源digest及主管HMAC、轮次/事务/驳回链仍按backend-minimal-signatures.md接入，不以现分类管理或纯service PASS宣称G03完成。

## A08最新交付（2026-10-15）

后端独立DccApplicationCustomMatrix.java / DccApplicationCustomMatrixService.java及DccApplicationCustomMatrixResolverTest.java已按A08实施；精确实际签名、来源/轮次/主管proof及目录资格接入义务见backend-minimal-signatures.md（覆盖下文初版仅方案签名）。私有当前源码Java17/JUnit RED11失败→GREEN11通过→REGRESSION16通过，0skip/abort；没有Maven/共享target/项目旧jar。

完整class runtime输出为结构节点而非ResolvedRoute/公共DO；不提供routeId/sourceNodeId，public/custom SOURCE明确区分，canonical/hash不被认作签名或合法类别来源的证明。正式当前目录与readiness、组织主管及HMAC仍由后续Resolver/Workflow接入复用，不在纯服务用模拟资格骗PASS。

当前相邻FE回归60项59通过/1失败：G01新V2预览与旧IC16测试V1期待不一致；本组件11项通过。该owner测试G03未改，Root接线时需纳入G01合同回归。以上是独立软件阶段，完整其他管理入口、实际申请保存/冻结/驳回、公共矩阵零写事务验证仍待授。

## C01最新冻结（覆盖下文初版建议）

- Root已冻结唯一正式类别approvalSourceMode=PUBLIC_MATRIX/APPLICATION_CUSTOM，管理端显式维护；不按名称/缺配置/客户端mode推断。
- applicationCustomMatrix中的approverUserId与documentControlUserId各为一个明确账号；初版“文控岗位选择”建议已被C01替代，不再作为待决定或实施方向。
- 2026-10-14本阶段只实现独立upload/CustomApplicationMatrixEditor.vue，配合专属真实SFC测试；没有共享页面/API/Java接线。部门字段继续唯一selectedSignoffDepartmentIds。
- 独立后端域文件的精确路径及最小签名见backend-minimal-signatures.md；等待M01回收后Root授权，不仅给计划停工，前端有限授权实现已完成。

组件合同：props approvalSourceMode、modelValue（applicationCustomMatrix或null）、selectedSignoffDepartmentIds、已有启用departments/users目录、loading/directoryError/disabled；emit两个update事件，不调用网络。expose validate(SAVE|SUBMIT)只校验本地结构/必填/当前目录，不证明正式启用/岗位/阶段权限/签名授权/图片预检已完成，接入方必须再调用正式后端预检及提交。草稿GET应提供两个字段的明确null或保存值；未知/不完整对象具体报错，不填默认账号。

## Evidence boundary

2026-10-14 在 int_qms、HEAD 822c31fc0856516cf59ba9cd84bf2bc7f455d17f 上只读复核当前共享工作区。下列结论为本轮源码推导及独立 Node 测试证据，没有真实页面、服务、实库或 Java 运行验收。历史报告和历史迁移不作为已实现证明。转交时必须重读 G01/G04 改后的代码。

## 现状

|链路|本轮源码锚点|结论|
|---|---|---|
|类型候选|IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：uploadFileTypeOptions；file-type-options.ts：buildUploadFileTypePaths|仅启用正式 leaf 且路径至少三级；前端没有本次矩阵来源字段或其他分支。是否已有符合要求的正式其他 leaf 属运行数据，未读取实库，不能判定不存在。|
|类型身份|IntRuoyiFronted/src/api/dcc/controlledFile/fileTypeTaxonomies.ts：DccFileTypeTaxonomyUploadOption；IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyAdminServiceImpl.java：resolveActiveCategoryId / validateCategoryMapping|按精确 taxonomy 解析唯一启用类别；无正式 APPLICATION_CUSTOM 标记。不能按中文名称、虚拟负数 ID 或第一条类别猜其他。|
|批准人 UI|IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：dcc-upload-matrix-approvers|只读展示从矩阵带出的批准账号，没有本次自定义批准人/文控职责编辑控件。会签部门已有编辑控件。|
|上传载荷|IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts：UploadFormDraft / buildUploadPayload|字段包含会签/下发部门与公共矩阵期待，不承载本次批准人/文控职责。保存草稿仍调用 requireApprovalMatrixSelection。|
|草稿读写|IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts：WorkingApplicationSelection / WorkingApplicationAttributes；BE DccWorkingApplicationSaveReqVO / DccWorkingApplicationAttributes|没有本次自定义配置。BE saveWorkingApplication 仅保存公共矩阵基线、部门、培训、生效方式及说明；readWorkingApplicationAttributes 固定按公共 canonical 读取 draftApprovalMatrixJson。|
|公共预检/送审|BE DccControlledFileWorkflowServiceImpl：previewRoute / submitControlledFile / submitWorkingIteration|普通流程使用 approvalMatrixBaselines 与 resolvePublicNative；目前无法提供无公共矩阵的正式自定义来源。|
|正式主管|BE DccControlledFileApprovalRouteAssigneeResolver：attachPublicNative；DccApprovalMatrixTokenService：issueNativeSupervisor|组织来源已存在，但要求 sourceApprovalMatrix 非空，凭证绑定公共矩阵 digest 和精确准备定义。自定义来源不能伪造公共 ID 绕过。|
|矩阵派生|BE service/route/DccPublicMatrixNativeDerivation：project|只派生 raw 公共 SIGNOFF/APPROVAL/DOC_CONTROL；不是本次配置入口。G04正独占修改会签可删规则，本轮读到的版本仍有 mandatory 限制，不能干扰或据此报告G04未修复。|
|人员预检|BE DccControlledFileRouteReadinessService：evaluateFrozenRoute / evaluateResolvedRoute|已有租户、账号启用、岗位、阶段权限、签名授权及图片就绪校验，可复用；自定义来源编译后还须验证选中部门与负责人。|
|驳回与冻结|BE DccControlledFileWorkflowServiceImpl：readNativeReworkRoute / nativeReworkComparison；Resolver：restoreFrozenRoute|当前重提能沿用公共冻结节点并检验前驱/精确定义；快照恢复仍严格要求 LEGACY 公共矩阵身份。不能直接把自定义 JSON 放进公共 baseline 或借用旧签名。|
|历史其他|IntRuoyiBackend/sql/mysql/20260526_dcc_other_template_category.sql|历史 NAS 其他分类复制公共治理配置；与本轮“其他本次自定义、公共矩阵零变化”不是同一合同。不修改或执行该历史迁移。|

## 端到端最小合同（待 Root 冻结）

### 1. 正式其他身份和来源

- 建议由正式类别配置返回明确 approvalSourceMode=PUBLIC_MATRIX 或 APPLICATION_CUSTOM，并随 upload-options 提供；后端按租户、taxonomy 与类别真实记录复核。来源模式不是用户绕过权限的开关。
- 其他仍是唯一启用正式 taxonomy leaf + category；项目/文件夹/上传权限与既有归档链不变。不把“未配置公共矩阵”自动解释为其他。
- 不从名称 contains(其他)、历史 NAS code 前缀或任意客户端 mode 推断资格。Root 须冻结本轮其他正式节点身份和来源字段的存放位置；如当前无正式节点，由正式管理路径补齐，不在申请提交时建公共配置。
- executionContract 沿用 G01 冻结的正式模型；source mode 只表达人员来源，不增加 BPM 分支、跳主管或改文控后培训/延迟节点。

### 2. 单一申请资源承载配置

建议在现有 preview/create-working/working-attributes/submit/submit-iteration 的 DTO 内增加 applicationCustomMatrix；名称待 Root 确认，禁止另造 /custom-submit 等平行 API。

最小配置包含：formatVersion、会签部门 IDs、一个明确批准账号 ID、一个明确文控职责 ID。所有 Long 在前端用字符串。会签仍取最终 selectedSignoffDepartmentIds 作为唯一来源，不在两个字段中各存一份可分歧部门表；如果 canonical 包含部门，它必须由该正式字段生成并严格等值验证。下发部门独立。

文控职责建议选择正式 DCC 审批岗位，并通过既有岗位配置解析人员；该选择不能授予新权限或修改公共岗位成员。若 Root 确认业务实际要求直接选择文控账号，应改为明确 USER 来源合同后再实施，不以无岗位时回退账号兜底。当前其他职责入口及审批策略未定义，不能自行提供任意 ANY/ALL、比例、多组或新增流程节点编辑器。

草稿允许合法但未填写完整的字段保存为 null/空选择，并准确回显未填事实，不能保存后用当前默认补齐。格式/身份违法仍拒绝。提交严格要求会签非空、批准人和文控职责完整，并复核全部运行人员；保存成功本身不送审。

### 3. 持久化与精确身份

- 建议新增独立 draft_custom_matrix_json；保持 draft_approval_matrix_json 只表示历史/维护类型公共基线。custom canonical 不冒充 DCC_APPROVAL_MATRIX_BASELINE_V2，不制造 routeId/sourceNodeId。
- canonical 由服务端生成，绑定 tenant、project、category、taxonomy、requester、精确 controlledFileId、applicationType、保留 attributeRound、配置序号/并发期待、contextKey；初次未分配 fileId 的预检使用本次 context，创建后重新绑定真实 ID。
- 提交将 custom canonical/digest 与运行节点一同冻结到现有 route snapshot/runtime JSON，明确 source kind=APPLICATION_CUSTOM；属性轮次及 BPM 轮次关联沿用正式 round-link。相同 file 不同轮次不得互读。
- 修改 custom 输入立即使旧预检/确认/payload selection 失效；晚到响应不得覆盖新项目/类型/申请/弹框 generation。切换项目/类型对已修改配置走现有确认，取消保留填写。
- 直接创建和工作稿提交的 payload hash 必须包含本次 custom canonical；同键同载荷回读，改批准人/文控/部门或借用其他上下文的票据必须冲突，不能重建 BPM。

### 4. 主管、会签与现有模型

- 自定义来源先编译成既有 MATRIX_REVIEW/MATRIX_APPROVAL/DOC_CONTROL_REVIEW 语义，再接正式组织主管 SUPERVISOR_REVIEW；文控后节点完全取 G01 的已冻结定义。
- MATRIX_REVIEW 仍为部门义务，负责人真实签名指派本部门会签人，所有最终部门都需通过；不在上传阶段自动选择具体会签人。批准配置最小为单明确 USER；文控职责的执行规则须沿既定合同，不能选不了就跳过。
- 应泛化现有 NativeSupervisorProof 的来源摘要绑定为明确 public/custom tagged source，同时验证配置 digest、部门集、精确定义与 context。这是同一正式票据设施的扩展，不做第二套主管或申请 API。
- 自定义路径不能读取/修改公共 route/node，不能因公共矩阵有新版而弹公共变更确认或更新原保存配置。维护类型保持 G04 的可删会签规则及原批准策略。

### 5. 保存、恢复、驳回与人员校验

- 草稿 GET 返回实际 custom JSON + 来源身份/轮次，DetailApplicationPanel 使用它初始化并提供校验、确认摘要。恢复默认“属性”不能覆盖保存矩阵；恢复草稿应读真正保存值。
- 驳回重提从真实前驱申请/版本/attributeRound 冻结配置继承；核对 tenant、Master、项目、类型、来源状态、终结 BPM、精确准备定义及 lineage。创建新轮次配置，不复制前轮签名/已完成状态。
- 不主动改类型时沿用原 custom，即便公共配置变化。主动改类型的行为需 Root 结合尚未收到的第6项冻结规则，当前只提供显式拒绝未知变更的方案，不暗改类型。
- 预检及正式提交各自复核部门存在/启用、负责人及本人所属、批准/文控账号存在启用且同租户、岗位、当前阶段权限、真实签名授权与有效图片。提供阶段/人员/部门具体错误。
- 无主管具体报正式组织来源问题；空会签精确“请至少选择一个会签部门”；无批准人“请选择本次批准人”；文控缺失“请选择本次文控职责”；失效给“本次批准人/文控职责人员/会签部门负责人 + 姓名/ID + 原因”。错误码最终沿 Root 统一合同，不新增猜测默认原因。
- 在当前既定项目→Master→排序File锁顺序下，配置、票据、节点、轮次、附件关联及 BPM 启动同事务；晚失败全部回滚，再次合法请求可完整重试。

## 真实 RED / GREEN / REGRESSION 方案

|用例|真实调用与失败点（接线前 RED）|GREEN / 回归断言|
|---|---|---|
|FE载荷/草稿|隔离 Node 编译执行现有 submitter.buildWorkingDraftPayload、buildSubmitPayload、applicationRead GET/POST；配置在当前模型/载荷中丢失，而不是检查字符串是否存在|精确传递/恢复自定义配置、null不补值、lossy Long/错source拒绝；不增加写API。|
|FE页面行为|编译实际 upload/DetailApplicationPanel handler；选择其他、改批准人、保存/确认、取消、切类型以及异步旧响应|实际来源切换、已保存值回显、确认摘要一致；取消零提交，旧响应/旧预检不授权新配置。|
|BE预检/提交|以 DccNativeUploadSupervisorWorkflowTest 的 H2 + 注册正式Flowable模型方法为参考新增独立 G03 test；真实 Workflow.previewRoute/createWorkingControlledFile/saveWorkingApplication/readWorkingApplicationAttributes/submitWorkingIteration，不 mock Workflow/Resolver/配置持久层|当前 public-only 路径无法无公共矩阵提交而 RED；后续精确开始主管任务、部门义务、批准/文控正确。外部人员目录与存储可作为显式隔离端口，不能称线上验收。|
|公共零变化|同一隔离 DB 记录公共 route/node 全字段快照；custom预检、保存、提交、驳回重提前后逐行比较；同时验证公共 Mapper没有insert/update/delete|空公共来源也能走合法custom；所有公共行完全不变，维护类型相邻规则保留。|
|必填/可用性|参数化真实预检及直接正式提交：空会签、缺主管、缺批准/文控、停用/不存在/跨租户账号、转部门负责人、缺岗位/权限/签名授权/图片|具体reasonCode/消息与阶段/账号，custom、节点、签名、BPM数量及状态全部零推进；加载后人员变化仍拒绝。|
|驳回沿用|真实批准/驳回形成前驱，合法检入目标及新轮次；之后更改公共矩阵，再走当前正式重提方法|沿精确冻结custom，新轮次人员复核；旧签名不复用；历史读原轮；错租户/Master/类型/轮次/定义/lineage拒绝。|
|事务与重放|TransactionTemplate中启动前晚失败/节点写入后失败；重放同键、异键、同键变批准/文控/部门；并发配置版本期待|晚失败持久表与BPM均回滚，可重试；同载荷只一实例，不同载荷冲突；不能用 mock 成功代替隔离事务。|

暂定 Maven 命令（仅 Root 签发唯一验证票后执行）：

```powershell
mvn -pl yudao-module-dcc -am test "-Dtest=DccApplicationCustomMatrixWorkflowTest,DccApplicationCustomMatrixResolverTest,DccNativeUploadSupervisorWorkflowTest,DccPublicMatrixNativeDerivationTest,DccWorkflowAttributesIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false"
```

其中两个 G03 类为待授权新增，不声称当前存在或已运行。公共 H2 create_tables.sql 只能 Root 合入授权迁移差异，不能通过借旧 schema/target 绕过。最终 Java 隔离结果也不能冒称 E2E/运行验收。

## 精确文件转交请求

所有下列生产文件目前均未写、未获 G03 写授权。相对路径稳定，不依赖本机盘符。每项必要差异与验收影响如下。

|文件|锚点 / 最小必要差异|依赖 / 验收影响|
|---|---|---|
|IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue|uploadFileTypeOptions / refreshRouteReadiness / freezeUploadApplicationDraft；其他控件、状态失效、custom校验/摘要|G12→G01交还后由Root转交；否则无真实新建入口。|
|IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts|UploadFormDraft / buildUploadPayload / buildUploadConfirmationSummary；单合同载荷/摘要|G01交还；漏传配置则无法保存与送审。|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue|open / selectIteration / saveAttributes / refreshPanelMatrix / submitInitial / submitRevision；custom保存/恢复/重提|G01交还；不修改详情父页。|
|IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts|UploadApprovalMatrixState / requireApprovalMatrixSelection；public/custom来源判别、ticket/输入失效|Root登记转交；不得复制状态机或伪造public基线。|
|IntRuoyiFronted/src/views/dcc/controlled-file/upload/signoff-departments.ts|resolvedUploadApprovers / resolvedUploadApplicationPeople；支持custom来源的只读显示，保留G04会签规则|G04交还；只有必要变动，不并改组织身份算法。|
|IntRuoyiFronted/src/views/dcc/controlled-file/upload/file-type-options.ts|UploadFileTypePath / buildUploadFileTypePaths；透传正式来源模式|Root转交；其他身份不靠名称推断。|
|IntRuoyiFronted/src/api/dcc/controlledFile/fileTypeTaxonomies.ts|DccFileTypeTaxonomyUploadOption；正式来源投影|Root保留共享API；需批准正式标记来源。|
|IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts|Submit/Iteration/Preview/Readiness/ApprovalSelection类型与请求验证；增加同资源custom合同|Root/G01冻结后转交；不新增平行URL。|
|IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts|WorkingApplicationAttributes / Selection / GET验证 / save；精确实际值与轮次|Root/G01冻结后转交；无current默认回填。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java|previewRoute / createWorkingControlledFileIdempotently / save/readWorkingApplication / submitWorkingIteration / readNativeReworkRoute / persistApprovalRouteSnapshots / insertControlledFile|G01正式申请合同交还；single source分派、事务、payload hash、精确冻结/重提。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java|ResolvedRoute / attachPublicNative / restoreFrozenRoute；tagged来源与同主管/运行节点|G04交还；不得改变维护类型批准规则。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalMatrixTokenService.java|NativeSupervisorProof / issueNativeSupervisor / requireRecordedNativeSupervisor；绑定custom来源digest|Root转交；不伪造public ID或绕过组织凭证。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkingApplicationAttributes.java|record字段；返回custom实际配置及精确来源|Root/G01共享合同；无恢复入口无法验收草稿。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccWorkingApplicationSaveReqVO.java|正式同资源DTO；custom结构与阶段校验|Root/G01合同转交。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java|正式NEW载荷；custom来源和完整性|Root/G01合同转交。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java|正式工作稿/重提载荷；custom来源和完整性|Root/G01合同转交。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRoutePreviewReqVO.java|预检输入；custom当前配置及期待|Root/G01合同转交。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRouteReadinessRespVO.java|预检输出；tagged配置+digest/凭证，不占public routeId|Root/G01合同转交。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java|新增独立custom draft持久字段，来源mode|Root共享DO/迁移；禁止把custom塞入public JSON。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkingApplicationDraftInitializer.java|initialize / ApplicationProvenance；只在经验证前驱与正式保留轮次继承配置|Root/B合同授权；若由Workflow完成继承则无需改此文件，实施前锁定一个位置。|

以下为 Root 集成依赖，不请求当前阶段写入：

- 正式其他类型来源若存类别：IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/category/DccFileCategoryDO.java、controller/admin/category/vo/DccFileTypeTaxonomyRespVO.java、service/category/DccFileTypeTaxonomyAdminServiceImpl.java 与其管理/投影；Root选定唯一正式来源位置后才列新增字段/迁移，不能同时加两套mode。
- 数据库新增候选迁移 IntRuoyiBackend/sql/mysql/20261014_dcc_application_custom_matrix.sql（当前不存在，命名待Root核准）：仅来源标记、draft custom列及必要快照身份/序号；不插入公共route/node。不执行实库。
- IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql：Root负责合入上述增量H2 schema；禁止worker并写公共fixture。
- 精确历史展示可通过现有runtimeApprovalNodesJson带custom canonical；若现有响应丢弃它，先给Root精确Query/VO映射差异，再等待G07/G06 owner交还，不能并写Query。
- DccPublicMatrixNativeDerivation.java：复用 G04 既有节点验证规则即可，优先不改public派生算法；如新source需要通用runtime验证，由Root转交后仅提取通用验证，不改变公共策略。
- 主管签名服务、生命周期、BPMN、Controller URL原则上不需要G03改动；现有 DccControlledFileController 的 previewRoute/working-attributes直接转发正式DTO/record。若G01新图消费custom节点存在缺口，先提交具体合同差异给Root。

获授权后拟新增：service/file/DccApplicationCustomMatrixService.java（canonical/校验/编译服务，无Controller）；对应 DccApplicationCustomMatrixWorkflowTest.java / DccApplicationCustomMatrixResolverTest.java；FE tests/unit/dcc-upload-g03-custom-matrix.test.cjs。精确全路径由Root以以上现有目录登记；新增helper只服务正式申请链，不自带第二套接口。

## 依赖与待冻结决定

1. G01正式申请/精确执行定义/主管签名合同先冻结；转交upload、submitter、DetailApplicationPanel、Workflow及公共DTO。
2. G04会签规则与Resolver先review交还；不复用未冻结中间版本。
3. Root决定正式其他身份和source mode来源位置、文控职责采用正式岗位还是明确账号、单批准账号的既定执行规则；这些决定影响DTO/迁移/票据，不能仅做前端特判。
4. Root批准上述custom来源/草稿字段/精确冻结、轮次与重提合同，再授予具体生产范围、唯一Maven票。
5. G06后续接入附件不得丢custom配置/期待；G03交还时需给G06载荷及hash合同。当前没有任何文件归还可被解释为已授权共享生产写入。
