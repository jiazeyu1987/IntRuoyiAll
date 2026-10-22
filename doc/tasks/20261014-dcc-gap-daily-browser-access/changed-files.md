# G07 本线程改动清单

## M04后增量

- 新增 `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileBodyReadCurrentSourceTest.java`，改前不存在：六个正规延迟source/当前图纸/实际Office签名及source变化/event回放护栏行为场景；M05期间暂不运行。
- 本增量未修改Query/QueryTest、detail/browser或其他已归还共享文件；只读记录当前Querysource新护栏，并更新自有报告状态。

## 2026-10-15 当前实现与单一归属

|文件（仓库相对路径）|本线程最小差异|移交状态|
|---|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java|只读正文context scope、既有resource overload、event/token/Office proof、日常投影、C04登记时间、Iteration两可空只读字段；保留原checkin|整文件已归还Root→G06，hash137975...cdbc，停止写入|
|同目录 DccControlledFileQueryService.java|metadata/binary正规context重载，原默认DAILY|G07|
|同目录 DccControlledFileDetailAuthorizationGuard.java|正文与metadata分离；申请人/Master/当前task/历史/培训/启用租户scope；V1/V2 current bound|G07|
|同目录 DccControlledFileBodyReadContext.java（新增）|严格Java联合/规范wire/hash；无授权结果字段|G07|
|同目录 DccOnlyOfficePreviewTokenService.java、DccVerifiedOfficeReadActor.java|context随签名payload，hash/版本/同actor证据；callback只读actor，安全上下文不伪造|G07|
|同目录 DccControlledFileRevisionOptions.java|Iteration新增createTime/changeDescription可空只读字段|A09投影已交付|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java|仅正文metadata/binary解析与同下发互斥；不改审批/上传写接口|G07|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java|G07真实服务BDD；旧preview成功用例迁到正规的scope/login/event/Master/round，2旧节点事实补task/signedAt/对应历史|G07专属|
|同测试目录 DccControlledFileDetailAuthorizationGuardTest.java、DccOnlyOfficePreviewTokenServiceTest.java|任务/历史/草稿/待生效/培训/签名token行为回归|G07专属|
|同测试目录 DccControlledFileBodyReadContextTest.java、DccControlledFileBodyReadHttpContractTest.java（新增）|canonical/type/重复键/跨文件/UTF8，真实Controller两资源transport|G07专属|
|同测试目录 DccApplicationReadHttpContractTest.java|仅Iteration构造新增null字段|A09必要相邻构造|
|IntRuoyiFronted/src/api/dcc/controlledFile/bodyReadContext.ts（新增）|同形严格意图、正规wire/query/header、未知/混合字段拒绝|G07|
|IntRuoyiFronted/src/api/common/filePreview.ts|仅DCC正文source context透传及独立distribution拒绝混用|G07|
|IntRuoyiFronted/src/views/dcc/controlled-file/shared/viewer-navigation.ts、view/presentation.ts|正文导航保留规范context；from仍仅导航文案|G07|
|IntRuoyiFronted/src/views/bpm/processInstance/detail/dcc-business-identity.ts、index.vue|BPM所选文件/raw版/task-round source，不切当前todo，迟到metadata保护|G07|
|IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue|只日常DAILY/专门精确working AUTHORING，两个openPreview传所选版本|已归还Root→G06，停止写入|
|IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue|两正文source、精确任务/历史/编制/培训导航，保护null-roundselection和rawversion|已归还Root→G01，停止写入|
|IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue|source身份校验，C04成功画面时间/清理/媒体load/Officeerror订阅|G07；子组件error event尚待G09|
|IntRuoyiFronted/tests/unit/dcc-g07-body-read-context.test.cjs、dcc-g07-preview-registration.test.cjs（新增）|实际helper/source/wrapper/SFC/navigation行为，非字符串验收|G07专属|

本线程未写 workflow.ts/applicationRead.ts/PreviewMetadataRespVO/PreviewAccess/AccessAudit/OnlyOffice子组件；这些变更由对应owner负责。相对HEAD diff含历史并行改动，不能全部算本线程成果。

下面“后端”段仅保留第一阶段历史状态；当前状态以上表为准。

## 后端

|文件|本线程差异|状态|
|---|---|---|
|IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java|仅新增 g07_dailyMetadataMustNotIssueBodyTokenEvenForRequester 与 g07_dailyBinaryMustNotReadWorkingBodyWithOrdinaryViewerEvidence；未改公共 fixture/原测试|已写，未运行，Root 冻结后未再修改|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java|无本线程改动|等待真实 RED 与端到端授权|
|IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileDetailAuthorizationGuard.java|无本线程改动|等待真实 RED 与端到端授权|

测试文件在任务开始前已含并行脏改动；Git 对 HEAD 的总差异不是 G07 的差异，不能全部归属本线程。

## 自有记录

- doc/tasks/20261014-dcc-gap-daily-browser-access/task.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/execution-log.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/integration-notes.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/review-ready.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/changed-files.md
- doc/tasks/20261014-dcc-gap-daily-browser-access/verification-report.md

未写任何未授予共享生产文件。
