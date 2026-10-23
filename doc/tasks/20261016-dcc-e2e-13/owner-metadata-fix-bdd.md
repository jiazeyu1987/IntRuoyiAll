# E2E-13 原培训入口元数据读取修复

## Current Status

blocked — Root已确认47项适用GREEN并部署；O原入口metadata及无培训上传控件PASS。正文/完整申请证据权限不在本例声明中。代码继续冻结，Root review及工程收尾待办。

## BDD

Given 当前原生上传文件处于PENDING_APPLICANT_TRAINING_RECORD，正式申请映射与文件当前BPM精确一致，负责人O的批准签名/本轮绑定有效，O不是原申请人或历史审批人。

When O从正式workbench入口读取同文件同BPM的详情，task为空。

Then 元数据使用已有query.getControlledFile(actor,id)及其权限投影，保留当前绑定轮次的属性与签名；不调用APPROVAL_HISTORY正文守卫。正文读取仍单独走requireBodyRead，元数据准入不授正文或TRAINING_RECORD上传职责。

## 首个RED

selector：DccSelectedDetailReadServiceTest#waitingTrainingOwnerReadsBoundApplicationMetadataInsteadOfApprovalHistoryBody

直接调用真实reader.readApplication，历史正文guard为非历史审批人的O抛出training owner is not historical approver；预置元数据query返回精确文件版本。当前代码应直接落在该历史错误，而不是因缺少新字段/测试装配失败。

## 后续验证边界

Root给出有效RED后再改唯一分配的DccSelectedDetailReadService.java；随后补足原上传人、其他人员、错误轮次、显式任务/历史及元数据不提升正文权限的负例。复用现有HTTP、offlineEntry与正文guard测试，不新增接口或泛化上下文。

## 已实现及冻结交接

- 唯一新增metadata准入：task=null、isNativeWaitingFile、当前BPM与唯一正式映射完全相同、offlineTraining.canReadProgress通过；读取已有两参数query投影。
- 投影必须保持文件ID、版本和当前轮次，签名摘要只保留所选BPM；显式read与普通任务/历史路径仍调用原requireBodyRead。
- 补U、无职责、错轮次、有task、非原生/非等待、错文件/版本/投影轮次、独立正文拒绝及HTTP无task入口；没有修改ProgressService、Controller、正文guard或前端。
- GREEN selector：DccSelectedDetailReadServiceTest,DccSelectedDetailReadHttpTest。原正文guard/offlineEntry/W06/daily projection回归selector见owner-metadata-green-handoff.json。
- 三个生产/直接测试文件已冻结，hash见交接JSON。Root已执行适用47项GREEN（Service20/HTTP4/Guard19/当前W06职责4）；旧offlineEntry37夹具错误另列未修复。
