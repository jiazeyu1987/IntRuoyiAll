# DCC 日常草稿正文入口验证

## Native最后fixture跟进（最新，待Root重跑；direct-new合同未关闭）
- Root两DB正式复跑：W08 100/100 PASS已冻结；Native77项68PASS/3failure/6error。本线程仅写Native completion，不重跑、不改W08或已绿类。
- 已补nativePreview既有工作稿controlledFileId20，按既有准备的V1/v5读取原矩阵/主管proof，而非错误地走无文件ID的新建V2选择；不伪造ready/blocker/proof。
- 本suite接真实DccControlledFileAttachmentServiceImpl，外部rows明确为空，真实服务按tenant/file/完整空事实计算正式manifest；nativeSubmitRequest在事务读取该manifest并填写expected IDs/hash。保留缺主管/坏proof等原assertThrows和继承用例，不把它们改为接受前置附件异常。
- **尚未关闭的具体fixture合同：**父类ordinaryDirectNewUploadUsesSamePreviewProofAndActualInsertSnapshotFreezeAndExactBpm直接构造的submit请求硬编码PUBLIC_MATRIX_NATIVE_V1+publicId(v5)，而生产preparePublicPreview对无文件ID新建正式验证PUBLIC_MATRIX_NATIVE_V2。简单传既有file20得到的token也不能合法用于新建null-file身份；不能把该两种proof互换。要准确迁移此场景必须同步它的创建预览、精确V2图和submit输入；本轮“不可改父类/继承用例”边界下未改此测试或生产，需要Root明确分配该场景fixture迁移，不能记为PASS。
- Diff r3-native-manifest-fixture.diff，当前Native与12份保护Java SHA见r3-native-manifest-fixture-handback.json。静态保留所有@Test/参数源、负向thenThrow/assertThrows；7核心+W08+Query/CurrentSource+两父fixture原字节无漂移。实际Native77重跑仍由Root唯一执行，当前未声称剩余9项已通过。

## 两DB fixture error修复交Root（最新，待Root仅复跑两类）
- Root15类正式结果565执行/0failure/109error/0skip：Query272、CurrentSource27及其余11类全部通过；W08的32个error、Native77个category初始化error是本次剩余范围，证据Root r2-be-regression-initial-result.json。核心84和已绿13类不重复执行。
- W08：显式连接真实round/route/master服务与仅77/文件20的硬范围；从真实引擎taskId/assignee77/定义/绑定BPM与属性轮次构造APPROVAL_TASK，编制99按本人真实申请构造AUTHORING；由现有canonical公共矩阵派生及真实Workflow冻结方法记录四节点路线，不mock Guard/成功验证。metadata签发后固定issuedBodyContext供binary/Office复用，不重算成新轮次，不改变PENDING状态。
- W08原拒绝/无bytes/身份/改派/错误轮次断言保留。嵌套Office主体重入按现有精确合同检查IllegalStateException及BODY_READ_SCOPE_ALREADY_BOUND；尚在运行的旧轮次被当前正式resolver拒绝时按ROUND_FLOW_ACTIVE_HISTORICAL_SOURCE_NOT_RECORDED检查，不放宽为任意异常。旧CONTROLLED_PREVIEW常量断言迁为同一issuedBodyContext.purpose精确值，仍断言原始BPM。
- Native：本suite局部复制原公共资源初始化，仅分类builder增加明确approvalSourceMode PUBLIC_MATRIX，父类/schema/default不动；通过本类nativeSources顺序调用局部公共初始化及super.nativeSources保持原先顺序。completion专属初始化和清理按TestInfo声明类隔离，不删父测试/不skip。静态对比局部公共初始化去掉这一字段后与父原方法逐字一致。
- 精确diff和两文件SHA256见r3-db-fixture-fix.diff / r3-db-fixture-fix-handback.json；测试注解和参数化来源不变，7核心+Query/CurrentSource指纹稳定。未运行Maven/私有Java，未改生产/Guard/其他owner/前端，等待Root正式结果。

## Root核心GREEN及三fixture交还（最新）
- Root正式-am核心GREEN84/84（SourceBinding67+Token17），0F/E/skip，freshXML/process归档Root r2-be-green-result.json；7交还Java指纹无漂移。本线程未重跑。
- Root接管Query大文件前，其普通正文source-bound stub/captor、双入口never及部分FileDO事实已应用；接管后本线程不再写入/回滚该文件，后续由Root继续适配。
- 另外三份：BodyReadCurrentSourceTest已迁移普通source-bound prepare/verify及精确tuple，原never保留并加新入口never，helper size显式对应原fixture bytes；W08SupervisorReadDatabaseTest保留旧prepareAccess并补普通新stub/captor及现有三字节source size；NativeUploadCompletionDatabaseTest经核对仅真实分发入口且来源事实已完整，保持逐字节不变。
- 精确差异见r3-fixture-three.diff，三文件指纹见r3-fixture-three-handback.json。原assert调用顺序/数量66、109、94均保持，测试注解和thenThrow数量不变，7核心文件无漂移。仅静态核验，没有执行编译/Maven；当前W08原审批上下文/入口及分发目的均未改写，不以此宣称相邻回归PASS。

## R3 GREEN候选交Root（最新，尚未执行GREEN）

Root review指出正式infra_file.name/type允许NULL，已移除初版source tuple对描述字段的无依据非空要求。正式依据为IntRuoyiBackend/sql/mysql/ruoyi-vue-pro.sql:242起的infra_file定义及FileDO；现有resolvePreviewFileName按业务文件名解析显示，previewKind允许按扩展名判断。签名tuple保存name/contentType原值（包括null/空字符串），不填显示默认值；id/configId/path与size有效性校验保留。已补nullable正常签发/同源binary+reading，以及nonnull→null负向。原先写成必填的描述约束以此节修正为准。

新增public `prepareSourceBoundAccess` 明确保留 `@Transactional(rollbackFor = Exception.class)`；source缺失或新issue/verify失败不会转legacy分支。

Root正式-am RED已完成：DccPreviewSourceBindingTest原33项执行，24业务断言失败、0ERROR/0SKIP、9不变源/撤权对照通过；1865个DCC Java输入0漂移，所有失败均为expected ServiceException but nothing thrown。证据引用[Root r2-be-red-result.json](../20261015-dcc-six-thread-followup-review/r2-be-red-result.json)，本线程未改写该证据。

随后按Root授权实现最小GREEN候选，当前5份生产变更：
- `service/token/DccPreviewSourceIdentity.java`：新增typed immutable来源tuple，精确保存infraFileId/configId/path/name/contentType/size，不做归一化或默认补值；name/contentType按正式schema允许null。FileDO必须存在且id/configId/path/size有效；不读bytes，不伪造hash。
- `service/token/DccViewerTokenPayload.java`：新增签名内typed previewSource claim；schema和前端字段不变。
- `service/token/DccViewerTokenService.java`：独立issueSourceBound/verifySourceBound，保留签名/期限/tenant/user/file/version/event/purpose/nonce/id校验，强制精确来源相等；无绑定旧token拒绝，新token亦不能走省略来源检查的legacy verify。
- `service/preview/DccControlledPreviewAccessService.java`：新增prepareSourceBoundAccess，复用现有真实event/watermark/audit生产链并签发来源绑定token；原prepareAccess仅用于原独立用途。新签发异常直接传播，无legacy重试。
- `service/file/DccControlledFileQueryServiceImpl.java`：普通正文metadata使用新签发入口；binary解析当前完整来源后按同claim复核，并用该不可变快照config/path/name/type读取/响应，verify后不再重新解析来源；assertPreviewReadingAccess强制同来源verify，继续保留账号/任务/租户/版本/轮次/业务文件授权。

上述路径均位于IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/。没有改reading Service/Controller、详情reader、Guard、签名审批、schema或前端。分发/附件/统一存储仍使用各自原用途入口；这是显式业务分支，不是source-bound失败后降级。

专属测试已冻结，静态计数（未执行）：
- `DccPreviewSourceBindingTest`：67项。保留Root已运行原33项；另22项reading同源/换源/旧无绑定token binary+reading拒绝/撤权及签发异常传播，Root schema review后再补8项name/type从nonnull变null拒绝、4项nullable描述同源正常metadata/binary/reading。
- `DccViewerTokenSourceBoundTest`：17项。真实HMAC+JSON record roundtrip、tuple六字段分别变化（含2项nonnull→null）、3项合法nullable描述roundtrip、claim篡改、签名有效但缺source、source token禁止legacy verify、到期与缺正式定位事实拒绝。
- 本线程没有运行Maven/私有Java/Node/types/build；不能把84项静态计数写成PASS。Root可先运行这两类，再按其票执行Query/preview/token/Office/reading相邻回归。

### Root fixture精确交接
Root若跑以下旧类，需要按新正式入口适配fixture，保留全部原业务断言；本线程未擅自修改：
1. `service/file/DccControlledFileQueryServiceTest.java`：setUp约388行普通正文`previewAccessService.prepareAccess` stub/captor转`prepareSourceBoundAccess(request, source)`，约404行普通binary token stub/verify转`verifySourceBound(token, expected, source)`；metadata/binary实际FileDO fixture补真实configId/path/size（尤其原size为空的helper），按有内容夹具的实际长度设置，不能靠产品默认值过关；name/type按实际值保留null，不新增必填。附件/分发的旧入口断言保留。
2. `service/file/DccControlledFileBodyReadCurrentSourceTest.java`：previews普通metadata stub转prepareSourceBoundAccess，binary/file helper补size；reading对token verify的断言转verifySourceBound并断言具体来源tuple。保持改派/停用/错误round、Office源件精确身份及零bytes/零新event断言。
3. 如Root扩大到`service/file/DccW08SupervisorReadDatabaseTest.java` / `DccNativeUploadCompletionDatabaseTest.java`，其normal metadata prepare stub/captor与FileDO完整事实同样需适配，不能将此fixture变化冒成新业务RED。
4. `DccControlledPreviewAccessServiceTest`、`DccG09PreviewRegisteredAtTest`、`DccPreviewIssuedRoundTransactionTest`中直接测试原prepareAccess的案例、统一ONLINE_FILE/分发/附件/OnlyOffice专有token与reading Service mock Query端口保持原合同，不应批量换新入口。

最小stub规则：只迁移普通正文调用点，request/expected原有每条断言保留，第二/第三参数增加typed source事实断言；原过期/篡改/不匹配的thenThrow必须随普通正文verify迁移到verifySourceBound，不能另加默认成功stub掩盖。拒绝前置的never().prepareAccess/verify应同时检查新方法未调用，不删除旧零调用断言。reading Service的assertPreviewReadingAccess接口未变，不需改该owner宿主。

### 边界与冻结
来源tuple证明服务器FileDO身份及位置/描述事实未变，不能证明相同config/path下对象存储bytes未被原位替换；FileDO没有hash字段，不冒用DCC sourceSha256作为发布件hash。metadata不额外读bytes。Office callback保留其独立现有signed infraFileId合同，本次仅将其普通viewer token与reading接入full tuple，不改Office专有token/回调协议。

生产与测试指纹冻结在r3-green-handback.json；旧r3-red-test-handback.json保存原RED33测试指纹含义。等待Root唯一Maven GREEN与相邻回归结果，当前R3尚未标已验证关闭。

最终只执行静态结构/契约检查及自有生产git diff --check，均通过；5生产+2专属测试无尾部空白，已冻结，不再写Java等待Root回执。Root建议先验证 `DccPreviewSourceBindingTest,DccViewerTokenSourceBoundTest`（静态67+17，未执行），再按上述最小清单适配原fixture并运行相邻类。

## R3 Root授权后测试先行交还（最新，尚未执行RED）

**已冻结交Root立即执行正式-am RED：DccPreviewSourceBindingTest，静态33项，测试SHA256为0d78faa4c04184b8490f9a102ca426bd3954c6cb1bc4772bd841fb7fd45e3454。8个生产输入仍无变化；本线程等待Root回执前不再改测试或生产。**

Root已唯一授权本线程R3普通正文来源绑定的Query metadata/binary/assertPreviewReadingAccess、preview access、token新合同及专属测试。当前只新增独立测试类 `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPreviewSourceBindingTest.java`，未修改任何生产代码。所有调用均为现有公开Query API，没有为缺失新API制造编译RED；实际编译/执行仍由Root唯一Maven确认，本线程未运行Maven/私有Java/Node/types/build。

|方法|参数展开预期数量|目的|
|---|---:|---|
|unchangedSourceReadsOnlyTheMetadataSelectedFile|4|DAILY/APPROVAL_TASK × PDF/IMAGE，不变来源读取F1成功，证明fixture真实可读|
|sameVersionRepointedArtifactCannotReadF2WithF1Metadata|4|同版本published/source指针F1→F2，必须来源冲突且零storage读取|
|sameFileIdChangedConfigCannotReadNewLocationWithOldMetadata|4|同infraFileId的configId变化拒绝|
|sameFileIdChangedPathCannotReadNewLocationWithOldMetadata|4|同infraFileId的path变化拒绝|
|sameFileIdChangedDescriptorCannotReuseOldMetadata|12|上述4场景 × name/type/size变化拒绝|
|currentPermissionRevocationStillDeniesBeforeAnyStorage|5|DAILY/任务账号停用、硬范围撤销，另任务改派，继续明确ACCESS_DENIED|

预期总33项，24项来源变化负向、9项成功/撤权对照；这是静态参数计数，**不是执行结果**。Root建议命令（cwd IntRuoyiBackend，仅Root执行）：

```powershell
mvn -q -pl yudao-module-dcc -am "-Dtest=DccPreviewSourceBindingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

夹具真实执行Query、DetailAuthorizationGuard、DccControlledPreviewAccessService、DccControlledFileAccessAuditService、DccViewerTokenService和DccBusinessFileAccessProvider；event/trace/log Mapper、BPM读取、AdminUser/项目/目录/硬范围以及storage为mock。metadata先真实创建event/trace并以明确测试密钥签发HMAC token，binary原样使用该metadata的token/nonce/id。无mock verify成功，不继承Query大测试类，不执行任何数据库或真实服务写入。

来源事实：FileDO正式字段为id/configId/path/name/type/size，没有bytes hash；FileService.createFileRecord构造这些事实，moveFile可更新同ID的path。现阶段测试只证明服务器可见来源tuple变化，**不声称覆盖对象存储同路径原位bytes篡改**。DCC行sourceSha256仅为既有审批fixture必须的来源字段，不把它伪造成发布件或FileDO hash合同。typed源件/独立source-bound issue/verify、旧无绑定token拒绝及reading权限保留在Root真实RED后实现，当前尚无GREEN。

当前测试指纹与8个授权生产输入的未变核验见r3-red-test-handback.json。旧R3静态交接说明保留为发现历史；“不再修复”的旧阶段结论已被Root本次继续授权覆盖。

## 本次继续R3：普通预览来源绑定缺口（最新交接，尚未修复）

只读复核发现R3：普通PDF/图片等非Office预览的metadata access event 与 viewer token 绑定受控文件ID、版本、用户、事件和purpose，但没有绑定metadata时解析的预览infraFileId或bytes hash；binary阶段又按当前受控行指针重新解析并读取。若同一版本的sourceFileId/publishedFileId或FileDO bytes路径在两次请求之间变更，当前静态链只保证V/U/event/token仍匹配，不保证仍读取metadata当时的F1。Office token已有infraFileId绑定，不能替代普通预览合同。

代码链、BDD、RED计划和Root分配边界见[integration-notes.md](integration-notes.md)。本线程QueryImpl/Guard与并行reading/详情owner停写，未修改代码或测试宿主，未执行RED/Maven/Node。R3不是已验证运行越权，需Root先确认source pointer/hash不可变合同，再在唯一Maven票下做F1→F2负向测试；当前无GREEN或回归PASS。运行、MySQL、迁移、E2E、服务、remote、Git仍未验/未执行。

### R3 最终交接
- 精确触发：普通预览 metadata 已成功并签发 token/event，随后同一受控版本的 `sourceFileId`、`publishedFileId` 或对应 `infra_file` 内容身份变更，再请求 binary；token 未过期且租户、用户、版本、事件仍相同。
- 方法锚点：`DccControlledFileQueryServiceImpl.getPreviewMetadataInScope`、`DccControlledPreviewAccessService.prepareAccess`、`DccPreviewAccessRequest`/`DccPreviewAccessResult`、`DccControlledFileAccessEventDO`、`DccViewerTokenExpectedContext`、`DccControlledFileQueryServiceImpl.readPreviewBinary`、`resolveBinaryFileId`、`requireMatchingPreviewEvidence`。
- 现象依据：metadata 阶段选出的 preview `infraFileId`/hash 不进入普通 event/token；binary 阶段重新解析当前指针并读取，因此源码没有证明仍使用 metadata 阶段的来源 F1。Office token 已含并校验 `infraFileId`，属于不同合同。
- 权限边界：本线程只拥有日常 Query/Guard 静态 review；R3需要跨 Query、preview access、audit event、viewer token 合同及其测试，不能单方面修改共享 Java、schema 或并行 owner 文件。故此缺口保持未修复、未关闭。
- Root 专属 RED：冻结同版本 V、F1/F2 和可控 file pointer；metadata 取得普通 PDF/图片 token 后切换至 F2，再调用 binary；断言返回来源上下文冲突或仍读取 F1，并断言 F2 零 storage bytes。覆盖 PDF、图片、状态/账号撤权，以及 FileDO path/hash 变化。随后由 Root 决定绑定 `infraFileId`、hash 或两者，并执行唯一 Maven GREEN/回归。
- 当前指纹见[daily-entry-review-r3.json](daily-entry-review-r3.json)：9 个源码文件 raw SHA256 与规范化 UTF-8/LF SHA256，核验前后无漂移。

R3 交接完成。未修复，不得将其写成已解决；本轮到此结束。

## 本次继续：日常正文入口独立只读复核R2（最新）

基线int_qms / f06833f06。按本次委派逐项复查列表allowedActions、精确详情reader/Query窄context、metadata/binary、Office callback、reading start/heartbeat/stop/summary，重点为草稿/待生效、任务改派和账号停用。**本次未发现新的具体正文授权绕过，未修改生产或测试源码，未执行任何行为测试。** 下方125后端/181前端/完整类型均为上一轮有效证据，没有重写为本次新执行，也没有重复旧11/125套件。

|本次检查入口|具体源码依据|静态结论|
|---|---|---|
|普通目录、项目目录、搜索和版本选项|QueryServiceImpl:getControlledFileBrowserPage约432行、getProjectBrowserPage约476行→restrictDailyBodyProjection约2555行；restrictDailyPreviewAction同步allowedActions|非ACTIVE或无activatedTime的草稿/工作/待生效等根行与历史选项canPreview=false且无PREVIEW。专门workingFileId编制入口与日常入口分开。|
|精确DAILY详情|Controller:readControlledFileDetail→SelectedDetailReadService:authorizedProjection约55行→QueryServiceImpl:getControlledFile三参数约630行|reader传同一context，窄入口在投影前requireBodyRead，仅DAILY再收窄根行/历史选项；generic两参数get不变，历史遍历未绑定根ThreadLocal。F01保持关闭。|
|metadata/token|Controller:bodyReadRequest默认DAILY；Query:getPreviewMetadata→withBodyReadContext→Guard.requireBodyRead|日常非ACTIVE/无激活事实拒绝，发生在prepareAccess/token签发前；显式任务context仍按当前task/assignee授权。|
|binary|Query:readPreviewFile默认DAILY→withBodyReadContext→readPreviewBinary；requireMatchingPreviewEvidence与token.verify|先重新授权，再核对tenant/user/file/rawVersion/event/purpose/round/token nonce/id；旧token不是当前权限的替代品。|
|Office callback|Query:readOnlyOfficePreviewFile先验签再设签名tenant；readOnlyOfficePreviewBinary核对源件ID及事件→withVerifiedOfficeReadActor→readValidatedOnlyOfficePreviewBinary→canReadBinary|签名callback继续调用相同正文守卫；hasAuthenticatedActor通过AdminUserApi与DccAccessUserIdentity复核账号启用/同租户，APPROVAL_TASK仍读取当前受理人；无context旧token的普通分支显式要求ACTIVE并按DAILY检查。|
|reading start|ReadingSessionService:startInternal→authorize→Query.assertPreviewReadingAccess→withBodyReadContext；资源锁后再次authorize|DAILY草稿/待生效或已撤销任务权限在session写入前拒绝，不签发新token、不读正文bytes。|
|reading heartbeat/stop|ReadingSessionService:advance锁前/锁后authorize，随后requireSession|旧session还须绑定同context wire、event、user、tenant、file、version、nonce/id；重复stop也先重新授权，不能用会话状态绕过停用/改派。|
|reading summary|summaryInternal先authorize，再selectSummary|统计查询没有跳过正文守卫；按当前tenant/user/file/version汇总。ONLINE_FILE旁路要求独立INFRA_FILE版本/ONLINE_FILE_PREVIEW purpose及business provider授权，不能混入DCC正文event。|

撤权语义：Guard.requireBodyRead公共前置会复核认证主体、账号启用、租户与硬分配范围；APPROVAL_TASK另外检查task未挂起、仍在精确轮次且assignee为当前用户。故原任务权限在改派后不能继续借旧context/token使用。独立拥有的ACTIVE项目阅读权或合法APPROVAL_HISTORY/VERSION_HISTORY须按自己的context与范围判断，不能把任务改派解释成撤销一切历史/项目权限。

列表/普通metadata详情的动作投影与实际正文授权有明确区别：generic get仍服务管理/编制元数据，不把它冒称DAILY正文接口；列表allowedActions不携带token，也不能替代上述每次正文或计时鉴权。本次没有声称账号停用后所有旧页面按钮会即时消失，亦未把旧页面残留或合法历史入口当作已证明的正文越权。

本次只读涉及的14个具体源文件及raw/规范化UTF-8/LF SHA256见daily-entry-review-r2.json。未写Query、Controller、SelectedDetailReadService或其他owner文件/测试宿主；无新缺陷，故不新增RED/GREEN或修复代码。运行服务、真实数据库/迁移、并发时序、外部Office/存储及E2E仍未验；服务/remote/Git提交推送未执行。仅文档结构、锚点与源码指纹进行本次核验。

上轮Root已将临时票据归档并清理，当前长期证据统一引用[Root最终报告](../20261015-dcc-six-thread-followup-review/verification-report.md)与[归档verification-results.json](../20261015-dcc-six-thread-followup-review/verification-results.json)。不依赖已清理的backend-green-summary/fe-types-green-result等临时文件。

本次文档结构/锚点核验PASS，14份源码指纹复核稳定，行为测试新增执行数为0。canonical cleanup preview因工程blocked状态拒绝，实际为BLOCKED，未执行apply；本次仅保留文档及明确列入Cleanup Keep的R2指纹，没有临时测试产物。

## Root M01正式回执：R01/R02/F01软件验证关闭（上一轮有效证据）

Root上一轮在集成源码执行-am定向10类Maven，exit0、freshXML125/125通过，0失败/错误/跳过；本线程只读核对，不自行重跑或写生产。当前不存在待Maven、缺依赖或F01待分配阻塞。长期证据见[Root验证报告](../20261015-dcc-six-thread-followup-review/verification-report.md)、[统计与进程归档](../20261015-dcc-six-thread-followup-review/verification-results.json)及[执行记录](../20261015-dcc-six-thread-followup-review/execution-log.md)。

|本票测试范围|通过数|
|---|---:|
|ReadingSession|29|
|ReadingController|12|
|SelectedDaily|6|
|SelectedDetailService|7|
|SelectedDetailHTTP|3|
|DailyBrowser|11|
|BodyReadCurrent|27|
|DetailGuard|19|
|BodyContext|3|
|OfficeToken|8|
|合计（同一M01票）|125|

R01修复完整Duration边界；R02修复受控四路仅审批身份入口并保留精确业务/token校验；F01由详情owner增加窄context详情入口，reader传原context，根行guard后仅DAILY复用restrictDailyBodyProjection。只读确认generic两参数get保持原行为，未使用根ThreadLocal遍历历史。SelectedDaily6与DailyBrowser11证明精确DAILY详情及日常选项动作闭环，同票相邻正文授权测试通过。以上软件验证已关闭，未声称真实运行验收。

Root真实RED11中3断言失败、4实际旧权限拒绝已保存在[RED归档](../20261015-dcc-six-thread-followup-review/verification-results.json)的backendRed；Root回执3生产文件原始字节SHA恢复0mismatch。本线程没有执行RED恢复、修改生产或新增重复测试。

Root前端viewer82、详情99分别独立PASS，各0drift；最终Root记为其前端两组181项，不与125或历史115/77/344/26混加。完整类型在首次回执时GREEN复跑中，最终[类型GREEN归档](../20261015-dcc-six-thread-followup-review/verification-results.json)的completeVueTypecheck为exit0、0drift、2429 inputs，未自行运行types。签名摘要processInstanceId的可选string/null由Root如实补齐，未以any绕过。

F01接入后的只读原始字节SHA256：
- DccControlledFileQueryService.java：586afe2447ec37b8a8f288920814a41a039cac90a7c51dcfbc419aea6b4dd480
- DccControlledFileQueryServiceImpl.java：991da386c9e22722ad6c2218f783a0f963efc1dca84dcc40203454fc58abcb7c
- DccSelectedDetailReadService.java：a98b91cc1358edb420735592b4fdc82f34b56302668976b5c2af40bdd60178bc

运行服务、真实MySQL迁移/事务/并发、外部Office/存储、E2E与Git提交推送仍未验证或未执行。工程状态仅因本轮禁止Git提交推送保留blocked，不把它写成软件失败。下方历史待分配/待Maven描述均已被本节覆盖，旧指纹文件保留其原始检查点含义。

## F01移交状态（历史）
Root已接受F01并整体交精确详情owner，授权Query interface/Impl的窄context详情入口、SelectedDetailService与自有测试。约束为generic get不变、仅DAILY复用restrictDailyBodyProjection、不用根ThreadLocal遍历历史。本线程保持Query两文件停写，只读review，不新增重复实现或测试；DAILY详情最终补测由Root统一收口。

接收移交后的首次只读源码仍是旧reader→generic get，尚未观察到owner接入结果；此时不宣称F01已修复。下文“等待Root分配”为发现时历史，已被本节移交状态覆盖。跨端正文/token/session的DAILY拒绝review保留原有效范围，无新增测试PASS。

## 首次续派只读复核（历史，已由Root M01关闭）

2026-10-15，int_qms / f06833f06，源码已有reading-session后端与精确详情接口。本次仅只读源码review，未改生产/测试源码，未执行Maven、shared target、fulltypes、build、Node或E2E。Root所述7类后端115/115、10文件前端77/77仅为其上轮检查点，非本线程本轮执行；下方344/26保留为首轮历史证据，不表示新集成源码重新通过。

- 日常列表根行及版本选项仍统一执行restrictDailyBodyProjection，canPreview与PREVIEW动作一致。
- metadata/binary默认DAILY；withBodyReadContext先调用requireBodyRead，草稿/WORKING/待生效在token或bytes前拒绝。
- reading-session controlled start/heartbeat/stop/summary均先authorize→assertPreviewReadingAccess→withBodyReadContext→requireBodyRead；写入口锁前/锁后各复核，summary也不绕过。上下文purpose、version、event、task round、token nonce/id均复用已有校验，没有以计时接口签发新token或读取正文。线上权限/存储/真实时钟未验收。
- online/infra reading分支只接受INFRA_FILE版本与ONLINE_FILE_PREVIEW event，并重新走business provider；普通DCC正文event不能替换该身份。正式DCC文件经provider进入Query无上下文时仍按DAILY检查。
- 精确详情显式DAILY读取草稿会被guard拒绝；但发现F01：合法ACTIVE根行的DAILY详情投影经无context的query.getControlledFile后，其WORKING历史选项可重新获得canPreview/PREVIEW。原因、完整触发链、建议范围和BDD详integration-notes.md。此项是静态推导的响应投影不一致，未证实正文/token/计时越权。
- F01修复需精确详情service与Query接口协调，超出本线程仅QueryImpl/DailyBrowser专属测试归属；已记录integration note等待Root分配，未单方面改共享接口/Controller/Viewer/详情/reading授权方法。
- 交还源码指纹：followup-review-fingerprints.json，11个具体源文件原始SHA256与规范化UTF-8/LF SHA256。旧verification-summary.json保持原样，不改旧证据含义。

该历史检查点结论：正文/token/reading-session的DAILY草稿拒绝链静态闭合；精确DAILY详情F01当时待Root处理，现已由上方M01正式验证关闭。

本次文档结构与11文件指纹稳定核验PASS；canonical cleanup preview因当前blocked状态拒绝，结果为BLOCKED，未执行apply。未创建临时测试产物，不将首轮cleanup成功重复计为本次成功。

## 结论
以下为首轮检查点的实现与定向验证：日常列表原先只将 canPreview=false，浏览页却使用 allowedActions，申请人的工作稿因此仍可能显示预览按钮。首轮已同步移除根行和版本选项的 PREVIEW 动作。首轮后端正文日常拒绝、专门编制、审批任务和历史读取规则保留；以下回归数据仅对应当时源码，不覆盖本次新集成源码。

## 基线与改动文件
- 分支 int_qms，HEAD f06833f06。委派给出的 a3b8234ec 至 HEAD 仅任务文档变化，相关 DCC 源码无变化。
- 修改：IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java，锚点 restrictDailyBodyProjection / restrictDailyPreviewAction（约2542行）。根行和历史版本布尔权限与 allowedActions 同步，仅收窄 PREVIEW。
- 新增：IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccDailyBrowserBodyProjectionTest.java，11项真实Query投影测试。
- 新增本任务 task.md / execution-log.md / verification-report.md / verification-summary.json。
- 不包含并行 view/index.vue、两个 LOG_FILE_IS_UNDEFINED 和既存未跟踪历史资产。

## BDD / RED / GREEN
BDD 详 task.md。先增加测试、运行失败，再修改生产代码。

后端工作目录为仓库下 IntRuoyiBackend，RED/GREEN 完整命令：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccDailyBrowserBodyProjectionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

- RED：exit 1；11测试，10预期失败，0错误/跳过，26.434秒。草稿、工作稿、待生效、失效、作废及版本选项残留 PREVIEW；预期仅 CHECKIN/REVISE，实际还带 PREVIEW。
- GREEN：exit 0；11/11通过，0失败/错误/跳过，47.199秒，BUILD SUCCESS。
- 新增测试覆盖 immutable allowedActions、原有管理动作/锁/待办身份保留、ACTIVE原拒绝不得放宽、缺激活记录拒绝、未知历史身份拒绝、缺动作投影不得造权限。

## REGRESSION
后端完整命令（同一工作目录）：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccDailyBrowserBodyProjectionTest,DccControlledFileQueryServiceTest,DccControlledFileBodyReadCurrentSourceTest,DccControlledFileDetailAuthorizationGuardTest,DccControlledFileBodyReadContextTest,DccControlledFileBodyReadHttpContractTest,DccOnlyOfficePreviewTokenServiceTest,DccW08VersionBrowserPermissionsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

实际 exit 0 / BUILD SUCCESS / 16.020秒；2026-10-15 15:59:40（Asia/Shanghai）结束，344/344通过，0失败/错误/跳过。八类均存在，逐份核验 fresh JUnit XML 时间与计数，摘要和 SHA256 存 verification-summary.json。

|测试类|数量|范围|
|---|---:|---|
|DccDailyBrowserBodyProjectionTest|11|本次根行/版本选项修复|
|DccControlledFileQueryServiceTest|272|日常metadata/binary拒绝、申请人编制及task精确正文、版本/列表等既有查询|
|DccControlledFileBodyReadCurrentSourceTest|23|metadata、二进制、Office回调、infra授权守卫、精确历史及撤权|
|DccControlledFileDetailAuthorizationGuardTest|19|日常/编制/审批/历史/租户/账号身份|
|DccControlledFileBodyReadContextTest|3|正规上下文、严格字段与canonical wire|
|DccControlledFileBodyReadHttpContractTest|2|真实Controller MockMvc透传、默认DAILY及重复/混合输入拒绝|
|DccOnlyOfficePreviewTokenServiceTest|8|签发验签、上下文绑定|
|DccW08VersionBrowserPermissionsTest|6|待生效拒绝、独立下载权限及正式版本指针|

前端工作目录为 IntRuoyiFronted：

```powershell
node --test tests/unit/dcc-g07-body-read-context.test.cjs tests/unit/dcc-working-browser-navigation.test.cjs
```

exit 0，26/26通过，0失败/跳过。执行真实函数/模板抽取的开发测试，验证日常DAILY、编制AUTHORING、精确审批/历史上下文、所选版本与请求/路由保持，不是页面E2E。

## 静态审查与授权闭环
- 普通存储目录：getControlledFileBrowserPage 在非 workingBrowserScope 时调用 restrictDailyBodyProjection；browser/presentation.ts:getBrowserRowActionState 读取动作列表，现与布尔权限一致。
- 日常全局搜索/项目目录：getProjectBrowserPage 统一调用同一限制；ProjectBrowserPanel / ProjectFolderFileTable 读取 canPreview。非ACTIVE只保留元数据与管理入口。
- 精确编制：workingFileId + workingMasterId 为专门入口；保留AUTHORING，服务器 requireBodyRead 仍核验申请人、账号、硬分配范围、项目编辑权、类别权限、Master与轮次。申请人不能靠日常身份直接读草稿。
- 审批任务：APPROVAL_TASK 核验真实运行任务、当前受理人、版本、租户、流程定义与精确轮次；任务改派/停用即拒绝。
- 审批历史：APPROVAL_HISTORY 核验历史任务已办受理人，或精确流程发起人；仍检查项目/租户/范围，读取选中正文。VERSION_HISTORY 只允许带保留正式正文的ACTIVE/SUPERSEDED/OBSOLETE，不替换为当前正文。
- Controller metadata/binary 无上下文时默认DAILY；Query的withBodyReadContext在取token和正文前授权，Office callback与infra PREVIEW亦重新校验。前端from参数不授予权限。
- git diff --check 本任务源码通过。回归前后8个关键生产文件无漂移；正式结果仅覆盖该检查点。

## 未验证边界与 Root review
- 未执行真实页面E2E、服务启停、数据库写入、远端操作、提交、推送；本轮仅定向Maven/Node与静态审查，未运行全项目测试/前端构建。
- 后端依赖端口使用mock，实际业务授权服务与token实现按各测试范围执行；没有验证真实BPM图、外部Office服务、真实文件存储及部署后效果。MockMvc不等于运行服务器。
- 需要 Root review：建议核对这16行生产diff、日常根行/版本选项两个消费点，以及专门编制/审批/历史入口保留情况。无需再启用子Agent。软件验证通过不自动授予提交、推送或E2E权限。
- 工程最终状态 blocked：用户当轮明确禁止提交/推送，与项目 completed 门禁冲突；不把这项流程限制写成代码失败。

## Cleanup（首轮历史）
canonical cleanup preview/apply 在 ready_for_closeout 状态实际执行，均 exit 0，keep4 / delete6 / 无blocked或warning。已删除本任务临时日志、复制XML和source-before manifest；保留三份报告与 verification-summary.json。随后工程状态标为 blocked，仅因禁止提交/推送。文档结构与JSON计数验证PASS，未清理并行资产。
