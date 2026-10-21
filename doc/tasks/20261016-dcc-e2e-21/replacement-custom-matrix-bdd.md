# E2E-21 换版矩阵来源只读定位

结论：当前直接RED位于后端换版属性投影，不是只有前端重算丢失，也没有证据支持源稿从未保存。未读DB、未解析响应正文；持久化判断由新context UI回读和源码事务路径共同支持。完整字段原值（canonical/digest/contextKey）不在DOM，未猜测。

## 固定原对象

ACTIVE A/1 `2054545668044084230`；master `2054545668044079202`；WORKING/currentDraft `2054545668044084236`；原BPM `572617a1-cac8-11f1-a035-b082e25ec548`。未换类型/改变更方式，未保存新配置、未创建替代申请、未提交B/1；browser/runner已关闭。

## 动作与自然请求

2026-10-18 16:58:26 +08 UI显示本次会签部门C21部门、批准人HISTORY、文控DOCCTRL、立即生效、无需培训。16:58:57选择换版；16:59:15出现原矩阵缺失提示，17:00:57确认提交disabled。DOM原文及自然path/status已存replacement-matrix-provenance-evidence.json。

- 选择前GET `.../4236/working-attributes` HTTP200；真实POST `.../route-preview` 一次 HTTP200；GET `.../4230/revision-options` HTTP200。路径中的4236/4230在JSON证据中保存完整ID。
- 选择后GET `.../4236/replacement-attributes` HTTP200，以及GET精确文件HTTP200；之后route-preview请求为0。HTTP200仅传输佐证，错误及禁用以DOM为准。
- 本轮PUT working-attributes为0；未点击保存草稿、为本次升版准备配置或预览所选正文。此前一次route-preview是工作稿初次显示时自动发出，不能冒充换版配置保存或新preview成功。

## 来源与丢失位置

1. `DccControlledFileQueryServiceImpl.java:853-894`：检入插入next后initialize、inheritPrepared，readInheritedCustomDraft取得绑定到next的自定义配置；显式set并UPDATE三列draftApprovalSourceMode/draftApplicationCustomMatrixJson/draftApplicationContextKey。少配置即抛错，事务回滚。它不是只在返回对象赋值。
2. `DccApplicationExecutionContractService.java:268-285`：验证source canonical及来源轮次；当source已有BPM时验证冻结route；以target自己的REVISION属性轮次、上下文key、prepared定义生成canonical。不能把原UPLOAD矩阵上下文原封不动塞给REVISION。当前A源配置已五签名冻结，4236初次UI可见对应人选。
3. `DccControlledFileWorkflowServiceImpl.java:760-777` 的普通working读取进入APPLICATION_CUSTOM，readCustomDraft读取已保存JSON并验证canonical，返回五个custom字段；前端`displaySavedCustom:456-492`要求contextKey/round/digest完整并生成route-preview，之后才显示原配置。真实UI已走通。
4. 同文件`:792-824` replacement读取却不走custom分支，调用短构造；`DccWorkingApplicationAttributes.java:21-25`将approvalSourceMode/applicationCustomMatrix/applicationContextKey/attributeRound/applicationCustomMatrixDigest五字段写成null。这是源码可直接确定的投影丢失点。
5. `DetailApplicationPanel.vue:635-660`在换版选择后先清snapshot并等待专用读取；mode为null未进入displaySavedCustom，又因公共draftApprovalMatrix为空且无新公共基线资格抛出真实提示。因此前端清空是切换读取机制的一环，根因证据指向专用读取输出遗漏；仅保留旧前端snapshot会掩盖上下文问题。
6. `DccControlledFileRevisionServiceImpl.java:295-298,341-365`正式候选创建先清旧custom来源，再initialize + readInheritedCustomDraft重新绑定并UPDATE。这条链还未被本例实际执行，不将其写为GREEN，也不把清空三字段单独判为bug：后面的重绑是必要配对步骤。

## Root最小修复与RED设计

建议首分配WorkflowServiceImpl的replacement属性来源投影及对应现有测试，先核对replacement上下文应复用selected稿的已保存配置读取，再由RevisionService在正式target重绑。暂未发现需要改Query或ExecutionContract持久化源头的直接证据。18若走相同自定义REPLACEMENT读取可能同受影响，尚未实际复现18。

1. 服务RED（待Root执行，未跑Maven）：构造已批准的APPLICATION_CUSTOM ACTIVE，实际检入工作稿；普通与replacement只读返回都必须包含源模式、相同已保存矩阵人选、source draft自己的contextKey/round/digest/精确定义；读操作前后源稿字段及快照行数不变。当前replacement五字段null应明确失败。可扩展现有DccSelectedWorkingAttributesDatabaseTest，而不只mock最终响应。
2. 来源安全RED：wrong tenant/master/baseline/requester、篡改canonical/context/round/definition digest必须拒绝；source配置缺失不能读取当前公共矩阵或从ACTIVE凭空补齐；有不可提交原因时仍可只读返回合法已保存配置，但canSubmit=false。
3. 正式候选RED：通过真实service submit REPLACEMENT后，B/1冻结所选差异正文、source4236、baseline4230；target custom canonical绑定target与新属性轮次，源稿canonical不变；部门/HISTORY/DOCCTRL与选定配置一致。校验失败完整回滚，不能只清三字段遗留半成品。此项覆盖RevisionService既有copy/rebind。
4. 前端RED：同一工作稿准备→选REPLACEMENT后正确保留回读配置，不弹公共矩阵缺失；等待专用读结束再决定可提交；不自动调用save、不写旧稿、不以旧snapshot兜底。
5. 真实复验：Root更新运行态后fresh READER只读原4230/4236，选择B/1、核对配置、再正式提交与五签名；仍使用原对象继续版本链。

本轮只做只读源码定位和设计，未修改生产或测试源码，未运行Maven/服务/数据库。真实UI RED已发生，所列新增单测仅设计，不能称单测RED/GREEN。

## Root分配后的首个实现门禁

Given 复用DccCustomRejectedCheckinResubmitTest的真实custom已提交来源和差异正文检入，source/working均保留自己的矩阵上下文。
When 对同一working分别走真实Controller GET working-attributes与replacement-attributes。
Then 两个返回都保留五custom字段、准备执行定义和已保存部门/生效/培训/属性选择，且source/working/属性快照没有读写变化。首个精确测试：DccCustomRejectedCheckinResubmitTest#replacementHttpReadPreservesSavedCustomProjectionWithoutWrites。

阶段：仅写行为测试，冻结待Root独占Maven执行有效RED；生产readReplacementApplicationAttributes尚未修改。

## 有效RED与最小GREEN分配

Root执行e2e18-green-21-projection-red：本方法1test/1failure/0error，replacement精确5项custom字段均null；其余字段及读取无写未失败。这是有效来源投影RED，之前null序列化预检失败仍单独保留。

授权最小GREEN仅readReplacementApplicationAttributes中公共基线分支前加入APPLICATION_CUSTOM读取：保留replacement自己的owner/baseline/资格与unavailable；以selected稿自己的reserved context/round/canonical验证返回完整投影，不调用普通working GET、不写源。Root独占Maven验证及后续运行态。
