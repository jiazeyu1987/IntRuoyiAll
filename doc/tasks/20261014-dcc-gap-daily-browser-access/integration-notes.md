# G07 正式入口授权设计（供 Root 接入审查）

## 最新归并状态

R06当前owner已把新延迟状态纳入expectsPreviewArtifact/missingreason；G07新 `DccControlledFileBodyReadCurrentSourceTest` 已私有当前源23/23PASS（sourcehashstable），含Word实际OFFICE+真实签名URL、图纸当次PDF、source变化/错fileID/旧event、账号停用/撤权/ignore、actualprovider默认DAILY、实际历史task。Query当前blob7fa123ccbc5778df650c3b424da614767f5e46b0，仅只读编译，G07未碰owner源码。

Root M09候选请至少收本新增测试类，当前发现数23，不是旧270补名。Root已计划旧Querywhole/Controller/Guard/OfficeToken/Context等联合正式-am；只有实际freshXML/exit可称正式通过。本线程真实服务与token/provider执行，infra协调器/持久事件/BPM/HTTPviewerHMAC端口仍mock，边界详见review-ready/verification-report，不混成运行验收。以下未投影旧差异均保留历史，当前已GREEN。

## M04后当前source接入状态更正

行为复核新增实际缺陷：当前source guard接入本身正确，但metadata `expectsPreviewArtifact`未包含PENDING_DELAYED_EFFECTIVE_CONFIRM，源件投影被提前跳过。真实新用例普通source及drawingPDF两FAIL；Office新独立用例expectedOFFICE却actualPDF。当前source护栏Office正向及引用变化/旧eventround/purpose反向四场景PASS。详见review-ready顶部；Query最小需要expects状态/专用缺件reason，由当前owner串行接。不得再仅看source分支存在认定新确认正文闭环。

当前只读Query blob `c80c8dbb2d176613ee404e275b68b989b112a710` 已包含 `PENDING_DELAYED_EFFECTIVE_CONFIRM` 在正规AUTHORING/APPROVAL_TASK/HISTORY且同精确round下只取sourceFileId的映射；Office callback也已包含 `token.infraFileId == resolveBinaryFileId(file, PREVIEW)` 比对。这两条旧待接入事项现为“源码已接、G07新独立当前source行为验证待运行”，不是仍未实现，也不能仅按静态存在关闭。

新增 `DccControlledFileBodyReadCurrentSourceTest` 六场景实际服务/真实Office签名token合同已编写，M05仍归Root独占时不读取其变动target运行。下文“尚无映射/建议增加”等文字保留历史设计依据，当前状态以本节和新运行receipt为准。Query/QueryTest仍属G06，G07仅只读，不另owner越权。

## 2026-10-15 C03 / A05 / A07 / A09 当前接入回执

正式 helper 已实现：`IntRuoyiFronted/src/api/dcc/controlledFile/bodyReadContext.ts`，唯一 export 为 `DccBodyReadContext`、`normalizeDccBodyReadContext`、`serializeDccBodyReadContext`、`readDccBodyReadContextRoute`、`buildDccBodyReadQuery`、`buildDccBodyReadHeaders`。Transport 固定 metadata `params.bodyReadContext` 和 binary header `X-DCC-Body-Read-Context`，两者同一 canonical UTF-8 JSON base64url；字段 `intent/controlledFileId/expectedVersionNo/applicationType/bpmRound/taskId`，受 intent 严格限制。DAILY 仅 intent+controlledFileId，办理/历史保留 raw version；未知/空白/附加/混合字段抛出。

Root 请立即转 G01 执行 workflow.ts 的 `workflow-wrapper-hunks.md`：metadata 第三参数 `bodyReadContext?: DccBodyReadContext`，binary 第四参数同类型；非 distribution 前先规范化一次，metadata 调用 `buildDccBodyReadQuery(id, context)`，binary 原有 token/event/trace headers 合入 `buildDccBodyReadHeaders(id, context)`；metadata 缺省加载时透传同 context。distribution 与 context 同时非 undefined 明确拒绝，既有下发参数/响应校验保留。common/filePreview 的 DCC 分支已实际调用新签名，所以 wrapper 未接前是明示编译/接入依赖，不能忽略参数冒充闭环。本线程未修改 workflow.ts。

实际 FE RED：9 FAIL，source丢上下文/跨文件不拒绝/BPM仅ID挂载，以及新helper缺失分类；GREEN：9 PASS；回归（本任务+BPM identity/通知+G09 viewer）26 PASS。初次扩展G09失败是新import未声明的测试端口，已通过复用已有filePreview source builder避免不必要新viewer依赖，G09工具12项全部重新运行通过。G09真实PDF3项另需其 `G09_CANVAS_MODULE`，当前缺依赖环境变量，不能计PASS。

A07后私有源码 RED：重新编译授权 Query/guard/interface/Office token/actor 与 Query测试及自有JUnit runner到任务目录，4/4真实业务断言失败（应拒绝未抛）。只读旧target依赖与本地Maven jars，未写target、未运行Maven，不代表reactor验收。命令 `& doc/tasks/20261014-dcc-gap-daily-browser-access/run-private-tests.ps1 -Phase source-red`；exit1，源码前后hash稳定。后端实现和成功办理测试继续推进。

A09接收browser/index.vue仅context接线与RevisionOptions.Iteration可空只读createTime/changeDescription投影；完成review后交还G06。其他页面/模型归属仍保护。

### G01 新确认节点：待正式 source 合同接入（已发现精确缺口）

目前读取 G01 真实源码：`DccDelayedEffectiveConfirmationService#requireTask` 使用 `PENDING_DELAYED_EFFECTIVE_CONFIRM` / `DELAYED_EFFECTIVE_CONFIRM`，原 requester、精确task/process/definition/round与前序真实签名被独立校验。G07 的 APPROVAL_TASK generic守卫复用服务器 `resolveRoundFlowContract`，无需另造授权endpoint，但 `Query#resolvePreviewReferenceId` 尚无此状态，会返回null，导致正式确认任务正文不可读。请 G01 给该状态已审批源件的唯一引用/是否盖章件合同；建议与现有 PENDING_APPLICANT_TRAINING_RECORD 同样只取本次 sourceFileId/sourceSha256，无原件/现行版fallback，但尚未收到她正式转交，不把猜测写生产。

G01 新 PUBLIC_MATRIX_NATIVE_V2 的 supervisor 元信息读也需 `DetailAuthorizationGuard#isActivePublicBound` 支持正式V2；这由G07在本轮Guard范围内接，保留精确 bound/task 身份。当前 C03 source最终regression覆盖V1，V2需要G01定义及新Binding source稳定后Root reactor核验。

C04：Query正文/附件metadata已从经过正式access服务核验的 `accessResult.issuedAt` 转UTCoffset String `previewRegisteredAt`，distribution复用正文映射。字段由G09新增，Query自有私有编译包含当前VO源码；watermarktimestamp/本地时间不作为补值。viewer toolbar hunk待G09交付，未擅自替代。

### 当前串行接管后的复审请求

Query已以 `13797566779c6f562a5c335c60a2a8013eddcdbc` 归还Root→G06，不再写。后续Root回授时请补独立回调source一致性：在 `readOnlyOfficePreviewBinary` 取得所选File并验证token/evidence后，显式要求 `tokenPayload.infraFileId == resolveBinaryFileId(file, PREVIEW)`，拒绝正文引用在签发后被变更的callback，而不只让BusinessFileAccess去证明原infra ID仍属于该对象。当前HTTP binary重读正式版本source，Office token具有同context/版本/事件绑定，尚无这条显式实际source等值断言。此项是本轮review主动发现的待修差异，不能把它隐藏为已完全闭环。

Viewer已按G09正式hunk接入清理/成功登记、media元素真实load与generation/src、Office同URLopened。只有画面成功显示（本地blob为“未记录”）；success metadata在decoder层必须有正式offset time，unavailable保持reason并无时间。父页订阅Office `error({documentUrl,message})`，已测试stale/currentURL与clear；子组件当前未emit，Root需转G09补最小错误event。实际端口行为验证不冒充真实服务。

## 当前结论与证据边界

本机分支实测为 `int_qms`。本轮读取当前源码，不复用旧 PASS。G07 原审计对工作稿准确，但对待生效应收窄：Query 的 `canReadBinary` 当前已经直接拒绝 `CONTROLLED_PENDING_EFFECTIVE`；问题是正文接口没有日常与办理上下文，`WORKING` / `REJECTED` / `READY_TO_PUBLISH` 申请人仍可由普通正文接口读入。

- `DccControlledFileController#getPreviewMetadata` 和 `previewControlledFile` 只有文件 ID，以及独立的下发义务参数；普通读没有编制/精确审批任务/历史轮次字段。
- `DccControlledFileQueryServiceImpl#getPreviewMetadata` 使用 `canReadBinary`，后者对工作稿和待发布申请人放行；`readPreviewBinary` 同样复核这条权限。
- `DccControlledFileDetailAuthorizationGuard#isAllowed` 是元信息/详情权限，有 requester、生命周期管理、培训进度等授权；不能把它直接当作正文业务入口证明。
- `DccControlledFileQueryServiceTest#readPreviewFile_workingRequesterReadsSourceBinary` 明确把普通正文接口读取 WORKING 源稿作为旧行为；本线程未执行该测试，不能称其 PASS。
- `filePreview.ts` 的 DCC source 只有 ID/下发身份；BPM 包装页也只给 ProtectedPdfViewer 传 ID；URL 的 `from`/`handling` 只决定前端展示，不是正文授权。

## 最小正式合同

正文默认入口是 DAILY。不接受 `from`、Referer、`canPreview`、布尔 `isApproval` 或客户端角色作为权限证明。调用方选择读意图和精确业务键，服务器验证后才建立一次只读作用域；租户和操作者只取安全上下文。

建议增加唯一 `DccControlledFileBodyReadContext`（Java 与 TypeScript 同形），作为严格判别联合：

|意图|请求事实|服务器必需证明|
|---|---|---|
|DAILY|文件 ID；不接收任务/轮次|文件必须为 ACTIVE，实际激活事实存在；现有文件硬范围、项目 READ、正文阅读权限全部通过。工作稿、审批中、待发布、待生效等一律拒绝，即使 requester/admin 也不例外。|
|AUTHORING|精确文件 ID、expectedVersionNo；已送审稿可带 applicationType/bpmRound|当前启用登录用户等于所选稿 requester；同租户、未删除；文件硬范围、项目 EDIT/OWNER 与 READ、类别 UPLOAD、所选稿源件身份；未送审稿使用已有正式工作稿/编制合同，已送审稿使用绑定流程与申请人证明。不能给同 Master 下其他稿默认放行。|
|APPROVAL_TASK|文件 ID、expectedVersionNo、applicationType、bpmRound、taskId|服务器读取该 task；未挂起且任务 assignee 为当前用户；task 的租户/流程/定义与服务器精确绑定轮次、版本业务对象一致。复用 DccBoundWorkflowContractResolver / 正式轮次和既有 task 授权，不以路线候选人、历史参与者或其他轮次当前任务代替。|
|APPROVAL_HISTORY|文件 ID、expectedVersionNo、applicationType、bpmRound、历史 taskId（参与人入口）|正式持久化轮次映射、DccApplicationHistoryGuard 和真实 BPM 历史共同证明精确文件/版本；历史 task 的租户/流程/定义/assignee 必须一致。申请人历史入口可凭真实流程 startUserId；仅元信息访问者不能取得草稿正文。|
|VERSION_HISTORY|精确文件 ID、expectedVersionNo|仅既有保留正式正文状态，所选版本必须在正式 Master 链中且未删除，同租户；保留原有项目/文件硬范围/正文权限与作废权限。读所选 publishedFileId，不换 currentActiveControlledFileId。不能用它读 WORKING 或待生效。|
|TRAINING_APPLICATION|文件 ID、expectedVersionNo、applicationType、bpmRound|仅保留现有合法培训业务读：服务器轮次绑定及 `offlineTraining.canReadTrainingBody`；日常上下文不能继承培训身份放行。该分支是保留相邻办理链路，不改变 G01/G02 培训流程。|

上下文输入必须拒绝混合字段、空白/非唯一参数、未知意图与无法证明的轮次。AUTHORING 的确切状态集合和工作稿证明直接复用当前办理规则，不能因新增读入口扩大检出/检入/送审权限。

## 签发、二进制与 Office 必须同一证明

1. Metadata 在读取正文文件、签发 access event/viewer token 前验证上述合同，按精确 ID/版本选源稿、当前图纸 PDF、盖章稿或保留正式件；不回退原件/当前版本。
2. 把规范化并已核验的上下文做 SHA-256 后 base64url 编码，purpose 使用短前缀加 43 字符摘要（总长小于现有 `purpose varchar(64)`）。不要把整个 JSON/任务 ID 拼入 64 字符字段。access event 的现有 `processInstanceId` 保存已证明轮次；日常为空。
3. HTTP binary 仍带原 token/event/trace 身份，并带同形 context；服务器重新读当前授权、重算 context purpose，验证 token 的 tenant/user/file/version/purpose/event/nonce/TTL 与数据库 event/trace 精确匹配。普通 context 不能重放编制 token；变更 task/轮次/版本/用户均失败。
4. OnlyOffice 的签名 payload 追加已证明 bodyReadContext（或等效严格字段），callback 从已验签 payload 恢复 context 后重新核验业务授权。`DccVerifiedOfficeReadActor` 当前仅允许 CONTROLLED_PREVIEW 且绑定当前 BPM，需要扩展为支持精确所选轮次的只读证明；不能伪造或替换登录安全上下文。
5. `assertBusinessFileAccess(PREVIEW/ONLYOFFICE_PREVIEW)` 必须消费同一个已验证作用域；裸 fileId/通用预览不存在该作用域时保持 DAILY，避免业务文件 Guard 绕过。下发专属 context 保留，拒绝与其他 context 混用。
6. 浏览列表/搜索 canPreview 只投影 DAILY 决策；详情、编制、审批和历史区域使用其真实 context 投影，不把日常值直接覆盖合法办理。不能只隐藏按钮。

## 请求 Root 转交的精确文件清单

BE 路径前缀：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/`。

|文件|方法/锚点|最小必要差异及验收影响|
|---|---|---|
|service/file/DccControlledFileQueryServiceImpl.java（已有授权）|getPreviewMetadata、readPreviewBinary、readOnlyOfficePreviewBinary、canReadBinary、assertBusinessFileAccess、toBrowserRespVO、getControlledFileSelectorPage|上下文作用域与统一正文授权，分别处理投影、签发和二进制；保持版本选择策略。|
|service/file/DccControlledFileDetailAuthorizationGuard.java（已有授权）|hasAuthenticatedActor、hasCompilerScope、requireAuthenticatedCurrentReadBound|新增正文入口校验，不全局改 isAllowed；复核精确 task/历史 actor。|
|service/file/DccControlledFileBodyReadContext.java（建议新增，未授权）|严格判别字段与规范化 purpose|唯一正式上下文，避免另造平行 API。|
|service/file/DccControlledFileQueryService.java（未授权）|getPreviewMetadata、readPreviewFile 的 context 重载|保留默认 DAILY；添加显式 context 入口给 Controller，原办理写接口不变。|
|controller/admin/file/DccControlledFileController.java（未授权）|getPreviewMetadata、previewControlledFile|严格解析读意图和业务键，拒绝混合下发 context；转交 Query。|
|service/file/DccOnlyOfficePreviewTokenService.java（未授权）|issueControlledFile、isSupportedControlledPurpose、PreviewTokenPayload|允许经过严格校验的正文 context purpose，签名携带 context，回调复核；不把 prefix 检查当授权。|
|service/file/DccVerifiedOfficeReadActor.java（未授权）|构造校验、matches|保留只读 actor 与 token/event/trace 绑定，允许精确历史轮次，不覆盖安全主体。|

FE 接线顺序必须避开 G09/G10/G08 当前 owner；如尚未转交，交 Root/现 owner 逐锚点接入。

|完整路径|锚点|最小必要差异及验收影响|
|---|---|---|
|IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts|getControlledFilePreviewMetadata、previewControlledFileWithWatermark|仅 DCC 正文 wrapper 增加严格 context，metadata/binary 同一请求身份，不根据 from 推断权限。|
|IntRuoyiFronted/src/api/common/filePreview.ts|OnlineFilePreviewSource 的 DCC_CONTROLLED_FILE 分支、buildDccControlledFilePreviewSource、getOnlineFilePreviewMetadata、previewOnlineFileWithWatermark|source 携带同形正文 context；附件与其他业务 source 不改。|
|IntRuoyiFronted/src/views/bpm/processInstance/detail/index.vue|bpm-dcc-approval-file-preview 的 ProtectedPdfViewer|用真实 processInstance/todoTask 或明确历史任务建立 previewSource；目前只有 ID。|
|IntRuoyiFronted/src/views/bpm/processInstance/detail/dcc-business-identity.ts|resolveDccTaskHandlingLocation|传精确业务键用于选择入口，并仍由服务器校验；不把 routing 来源当授权。|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue|viewerMode 正文、dcc-approval-upload-file-preview、版本历史导航与 approvalProgressScope|日常用 DAILY，编制用 AUTHORING，任务/历史带精确 task/round；导航保留同 ID/版本。该文件由 G10 等阶段 owner 接线，不并写。|
|IntRuoyiFronted/src/views/dcc/controlled-file/shared/viewer-navigation.ts|openControlledFileViewer|显式传正规 context，保留返回路径；from 只用于导航文案。|
|IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue|handleView/正文导航（当前 openViewer 路径）|普通目录/搜索保持 DAILY，专门工作稿编制入口显式 AUTHORING；检入完成仍导航精确新稿。|
|IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue|resolvedOnlinePreviewSource、props.previewSource|已有 previewSource 可直接透传，优先由父页完成；若必须增加 props，则由 G09 owner 接入。|

## 真实 RED / GREEN / REGRESSION

已新增 `DccControlledFileQueryServiceTest#g07_dailyMetadataMustNotIssueBodyTokenEvenForRequester`（WORKING/REJECTED/READY_TO_PUBLISH）及 `g07_dailyBinaryMustNotReadWorkingBodyWithOrdinaryViewerEvidence`。两者直接调用当前生产服务并检查 token 签发/正文存储端口零调用，不是字符串测试。预期前者返回 metadata、后者读取正文而导致断言失败；必须由 Root 唯一 Maven 票实际执行才算 RED。

待 context 获准后增加真正服务测试：同一 requester 的 DAILY 拒绝与 AUTHORING 成功、审批精确 task 成功/错 task 或轮次拒绝、APPROVAL_HISTORY 保留所选版本、待生效 DAILY 拒绝（含 manager/requester）、Office callback 重放/撤权/跨租户拒绝、下发与培训相邻回归。旧 `workingRequesterReadsSourceBinary` 等应转成正规 AUTHORING context 以保留业务契约，不能直接删成功断言。

当前未运行任何 Maven/JUnit、E2E、服务或真实库验收。Root 正在持有 G12 M01 RED 票，本线程 BE 生产/测试写入已冻结。
