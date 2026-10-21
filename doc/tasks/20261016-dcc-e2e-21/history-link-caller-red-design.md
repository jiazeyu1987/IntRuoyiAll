# HISTORY真实已办入口：直接调用链与首个行为RED设计

结论：本轮真实已办缺上下文的首个确定丢失点在DccApprovalTaskAdapter的history link producer；前端caller保留缺失的query，不是先前推测的“前端把完整context丢掉”。不改Query/Test（18独占），不改其他生产或测试，不跑Maven。

## 实际调用链

1. `IntRuoyiBackend/.../dcc/approval/DccApprovalTaskAdapter.java:249` pageDone读取HistoricTaskInstance；`:353-389` toSummary(HistoricTaskInstance,HistoricProcessInstance)已有精确file/businessKey、sourceTaskId和processInstanceId，但detailQuery调用无参approvalCenterViewerDetailQuery()。`:490-495` helper仅put(viewer,1)/put(from,approval-center)，没有正文身份。不是BpmNativeApprovalTaskProvider：它按DCC定义排除，当前行module/source为DCC/DCC_CONTROLLED_FILE_TASK。
2. `IntRuoyiFronted/src/views/approval-center/index.vue:240-246`真实“查看”按钮@click=openModuleDetail(row)。`:988-996` openModuleDetail选择resolveDecisionDetailRoute/Query后router.push(resolveDccApprovalDetailLocation(...))。`:964-974` DCC没有decisionDetailRoute时直接取detailRoute/detailQuery。
3. 同文件`:930-957` resolveDccApprovalDetailLocation：TODO办理分支补process/task；DONE进入非办理分支，只展开nextQuery并加viewer/from；没有删除已有bodyReadContext，也不生成缺失context。因此实际后端只给viewer/from会原样落成已观察URL。
4. 详情`detail-read-context.ts:25-48`以及`detail/index.vue:5322-5356`：无bodyReadContext和process/task便没有历史轮次，最终读DAILY。from=approval-center只是入口标记，不能授予历史上下文。

## 真实记录

HISTORY本人从A1已办批准行查看。A文件4230，version A/1，原BPM572617a1-cac8-11f1-a035-b082e25ec548，批准task6c134fef-cacc-11f1-a035-b082e25ec548；当前B4244。正式URL仅`/dcc/controlled-file/detail/2054545668044084230?viewer=1&from=approval-center`，与生产helper输出一致。截图/原行/自然路径见historical-body-readonly-evidence.json。上述完整数字均来自原真实UIcheckpoint，未重建。

## 最小行为RED（设计，尚未编写或执行）

首选精确方法：`DccApprovalTaskAdapterTest#pageDoneCarriesExactApprovalHistoryBodyContextForRetainedOldVersion`。复用该类现有pageDone fixture：合法未删除的已完成历史A1文件、历史task/process，当前master/文件另有B1仅作不同版本背景；stub旧任务的精确process definition/application type事实，不用title或当前版本推断。调用adapter.page(DONE)，从真实summary.detailQuery取bodyReadContext，用现有DccControlledFileBodyReadContext.fromWire(fileId,wire)解码。断言intent=APPROVAL_HISTORY、controlledFileId=A、expectedVersionNo=A/1、applicationType=UPLOAD（此历史上传实例）、bpmRound=旧BPM、taskId=旧历史task；路由仍指向A，不取B；无任何写调用。当前query无bodyReadContext应在此精确失败，不能以空字符串、默认DAILY或缺版本旧数据做假RED。

不先重写共享fixture：现有pageDoneUsesDeletedControlledFileSnapshotForHistoricalSummary测试保留删除记录列表/按钮禁用边界；不为了新正例放开deleted或补猜legacy版本。现有测试对deleted query等于viewer/from的断言不是合法未删除历史阅读契约，不应直接改成授权。

紧贴前端的单一接线回归可后续复用现有AST执行方式（dcc-approval-center-signoff-entry.test.cjs风格），从真正approval-center SFC提取上述caller/builder，输入后端的完整合法DCC历史query，执行openModuleDetail并捕获router.push；再用既有readDccBodyReadContextRoute解码，确认完整历史context未丢失。不手写一份builder替代真实函数，不新增通用路由框架。当前前端会保留合法context，这个保护测试预期本来能绿，不应当成修复源头RED。

关键接线约束：已编码bodyReadContext中携带round/task/applicationType。不要同时另加顶层processInstanceId/taskId/applicationType，`resolveDetailReadSelection`明确拒绝encoded与plain两套身份混用。前述初步文档“带历史process/task”指它们存在于精确context，并非要求重复query。

## 最小后续分配建议

Root先分配DccApprovalTaskAdapter + 其现有AdapterTest的一个合法历史summary行为RED，确认失败后再决定在producer输出规范wire；Java已有DccControlledFileBodyReadContext.wire/fromWire可复用，不需要新增DTO或前端兜底。应用类型须来自该历史轮次的正式定义/绑定（特别作废轮次不能用file.changeType强推），身份欠缺应明确不可打开正文，不能降级DAILY。初轮测试只覆盖此次UPLOAD历史触发；现有TODO/删除/旧数据列表为邻近回归。

本例查看矩阵缺失是另一夹具事实，不由此routing修复放宽任何权限；历史读权限链由Root串行处理。当前B1实际业务已完整生效，但固定正文整例未PASS，三类拒绝仍NOT_EXECUTED。所有runner已关闭、无新写入。

## Root首个测试分配

Given 合法未删除A/1保留文件（OBSOLETE且published存在），旧历史UPLOAD定义、精确historic task/round，后继B1为不同文件；When 真实adapter.page(DONE)生成summary；Then wire解码为APPROVAL_HISTORY/旧A身份/A1/UPLOAD/旧round/task，正文入口只含规范wire而非混入另一套plain上下文，且不写任何文件或调用workflow。只新增DccApprovalTaskAdapterTest#pageDoneCarriesExactApprovalHistoryBodyContextForRetainedOldVersion；待Root Maven有效RED前生产冻结。

## Root有效RED后的最小GREEN

Root compiled RED1test/1failure/0error，唯一bodyReadContext=null，XML e2e21-23-compiled-red-DccApprovalTaskAdapterTest.xml保留。仅Adapter合法DONE生成wire；UPLOAD/REVISION/两类正式OBSOLETE key映射其历史类型，未知legacy/external或不完整身份没有正文详情路由；deleted沿用旧不可打开投影。补必要helper分支测试保证OBSOLETE不取file.changeType及不完整来源不默认DAILY。Root独占Maven。

## Root共享caller审查新增明确LEGACY分支RED

Guard.requireExactBodyRound已有LEGACY_APPROVAL显式合法族：同file.processInstanceId/历史businessKey/requester，NEW→UPLOAD、REVISION→REVISION。原helper将此族整体不可读是回归，不能扩为未知fallback。仅新增pageDoneCarriesExactLegacyApprovalHistoryBodyContextForItsBoundFile测试，真实adapter.page(DONE)，分别NEW/REVISION合法绑定并解码对应类型；生产冻结待Root有效RED。当前unknown测试中的LEGACY样本未绑定file.processInstanceId，仍应不可读，不代表合法legacy未知。Root首轮Adapter29PASS/唯一unused-stub ERROR只作测试维护，不列产品RED。
