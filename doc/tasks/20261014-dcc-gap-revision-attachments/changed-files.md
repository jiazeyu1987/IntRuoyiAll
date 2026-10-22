# G06 改动文件

## 最终接收回执同步

本次仅更新自有task/verification/execution/review/清单及新增final-software-acceptance.json。只读核验最终有效47报告1008/1008、后端11865/前端2429输入0漂移，G06冻结源hash未变；没有生产/test写入或套件执行。所有软件已Root接收，唯一未授权门禁为Git收尾，Root统一清理与报告。

## M12接收后记录更新

本次仅更新task.md、review-ready.md、verification-report.md、本清单、execution-log.md及新增m12-software-acceptance.json。生产和测试仍与a34-frozen-sources.json相同，没有重跑任何套件。Root M12 G06全部软件域已接收，整体唯一45ERROR属G12独立fixture，G06不修改。

## 当前A34冻结差异（M11之后）

仅以下两生产文件和一个新专属测试；Root冻结后不再写源。源改前a34-before.json，最终a34-frozen-sources.json包含raw SHA、blob与6声明方法。

|路径|精确差异|
|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java|initializer/prepared后读取真实typed custom继承，源CUSTOM而缺结果拒绝；本地next同步3字段，并入原tenant/id/WORKING/nullBPM CAS，affected=1；public null不写custom。其它manifest/正文/只读资格保持。|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRevisionServiceImpl.java|INSERT前清空target旧custom三字段；仅真实custom来源用新Resource initializer建立target reserved round；typed重建后并入原CAS写3字段/本地target同步。保留原source与public语义。|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccCustomDraftInheritanceIntegrationTest.java|新增自包含6个真实Query/Revision/typed服务/initializer/round/SQL场景；直接BaseDbUnitTest，无继承业务tests；exact-file与lateCAS约束不放宽且finally清理。|

G03 ExecutionContractService/公共Workflow/VO/API/Panel没有本线程A34写入；G07 Guard和其它冻结资产未动。无新schema/迁移。任务runner仅增加A34选择与实际源码编译范围，输出仍私有目录。

## 已正式接收的M10/M11资产

9份后端生产（8 Java+1前向nullable SQL）与8份授权测试逐路径/指纹见m10-frozen-sources.json、m11-frozen-sources.json；M10→M11只改获准的测试真实root路径，生产无变。FileResp的ordinaryAttachmentManifestHash已阶段交G03，后续无并写。当前A34只在上述2生产位置产生新delta，不能把M11前66PASS当新delta正式验收。

以下按授权阶段保留历史详细文件清单，遇到旧“未授权/未接入”以当前review-ready及本节为准。

## A18 新授权旧fixture差异

- IntRuoyiFronted/tests/unit/dcc-browser-checkin-minor-only.test.cjs：仅添加显式ready空普通附件组件state/ref及真实模板CHECKIN context/key依赖，原65断言/20测试名完全保持。
- IntRuoyiFronted/tests/unit/dcc-browser-checkin-training-default.test.cjs：仅补run host的ready空附件state，原11断言/4测试名完全保持。

改前a18-before.json记录raw SHA与AST assertion，修改后a18-after.json记录fresh hash及不弱化结果；public API/Query/Revision/Workflow及生产前端未改。A18两旧fixturecanonical问题已关闭，其下A17“未转交”说明为历史记录。

## A15/A17 新增授权文件

|路径|实际差异|
|---|---|
|IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue|Root A17转交后接入附件组件/context/state、完整票据payload、any失败零写、保存结果清单核对、取消清理；不改G07正文context/G08草稿ID/helper|
|IntRuoyiFronted/src/views/dcc/controlled-file/browser/CheckinAttachmentsPanel.vue|Root A15授权新增真实multi-upload组件，复用原preview/cleanup APIs，独立session、失败保留、清理及late response隔离|
|IntRuoyiFronted/tests/unit/dcc-checkin-attachments-panel.test.cjs|12项真实Vue编译/挂载/handler/状态测试|
|IntRuoyiFronted/tests/unit/dcc-browser-checkin-attachments.test.cjs|7项实际父页submit/cancel行为测试|

browser改前hash在a17-before.json；新文件改前不存在。最新10份生产/测试hash在a17-after.json。没有写Query/Revision/Workflow/workflow.ts/共享DO/Resp/schema，也没有修改两份未转交历史fixture。后端A15只在原授权服务加强精确formal身份及原2专属测试，授权范围保持。

## A08 当前生产与测试差异

仅写以下4个生产文件与2个专属测试；改前hash已保存于a08-before.json，当前hash写入a08-after.json。共同旧脏改动不得计入本任务；原bindAttachments/bindTrainingRecord/bindApplicationRound/读取行为未改。

|仓库路径|本任务必要差异|
|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadServiceImpl.java|CHECKIN+ATTACHMENT允许，现有上下文授权保持，新增正式位置/版本/tenant门禁及其依赖|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentService.java|新增bindCheckinAttachments与freezeOrdinaryAttachments两个正式服务命令|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileAttachmentServiceImpl.java|仅新增两命令、其身份/权限/字节/重放helpers和正式依赖；初传/历史培训方法保持|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileCheckinReqVO.java|attachmentUploadTickets正式List及嵌套票据验证|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/upload/DccCheckinAttachmentUploadContextTest.java|新增8项实际UploadService/真实票据槽位/存储回滚隔离测试|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRevisionAttachmentsDatabaseTest.java|新增16项真实附件/票据/Mapper/H2事务/字节复制/回滚及重放测试；私有schema不改公共fixture|

没有写Query/Revision/Workflow/publicAPI/browser/AttachmentDO/Resp/sharedSchema。自有任务目录新增runner/launcher、before/after及阶段receipt/hash记录；classes/compile/test日志为任务临时验证产物，不应混入最终工程提交。

## 第一阶段历史记录

## 范围

唯一写入目录：doc/tasks/20261014-dcc-gap-revision-attachments/，共7份记录（6份Markdown和1份JSON）。生产变更为0；没有改现有相邻测试。检入/升版修复尚未实施。

|文件|性质|改前状态/指纹|
|---|---|---|
|task.md|新任务记录/BDD/约束/状态|新建，无改前hash|
|execution-log.md|新执行及证据日志|新建，无改前hash|
|source-baseline.json|33个只读文件的raw与UTF-8/LF SHA256|新建；文件字段含当前观察源码hash，不是本任务生产改动|
|integration-notes.md|实际链路、最小合同、精确转交清单及TDD方案|新建，无改前hash|
|verification-report.md|本轮真实命令、PASS/RED/未运行边界|新建，无改前hash|
|review-ready.md|第一阶段审查入口及文件转交请求|新建，无改前hash|

本次RED通过stdin执行，完整内容留在verification-report.md；未写临时脚本、未修改公共测试。当前共享脏工作区的其它改动均属于基线/并行资产，不归G06。
