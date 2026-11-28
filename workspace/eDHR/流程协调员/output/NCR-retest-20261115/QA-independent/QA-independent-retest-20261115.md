# 不合格评审独立定向复测（2026-11-15）

## 1、结论及边界
- 117治理详情正常打开对应评审，明确只读，无处置表单/按钮，无“缺少详情路径”。QA签名64314详情成功加载；核验仅本地摘要与时间值，创建审计停用，不计全面签名链通过。
- 生产、检验复核签名均报“签名证据与正式记录不一致”。虽未出现旧“班组活跃订单不存在”，仍不能判签名有效或周期关联通过。
- 115、116治理详情均报“eDHR 不合格评审单不存在”。旧返工档案、新旧周期及签名关联受阻，不能推断删除/重置原因或直接判追溯修复失败。
- 独立任务7196e22d-3264-44cb-b75c-f2b46a7565c0及a2237b27-650b-415f-8e06-cfcc095ddfba；旧追加4572已停止，不覆盖QA-existing。没有新建、复制、重置、处置、签名、通知重发、API/数据库业务操作、权限或服务变更，不扩展为六轮重测或上市放行。
- 环境localhost:8081，测试1，yingtaigroupadmin，昵称管理员，QA签名账号9908100249。已读取适用AGENTS、岗位指南和测试1记录。旧线程登录页后本次以当前控件正常登录；全部历史场景为模拟，不称真人亲签/真实质量批准。截图不含凭据。
- 本报告北京时间2026-11-15 14:11后整理，业务时间保持页面原值。

## 2、正式评审菜单和历史线索
- 正式不合格评审菜单：无筛选全部页刷新后仅BHGSP-202611-00000008，已关闭/作废，冻结08:40:49；进行中为空。112—116未在列表看到，仅能报告未见，不能断言删除。证据06。
- 完整工单/批次：`SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891`。本次没有历史独立周期ID，不把评审/订单号或意见编号当作周期通过。
- 历史交接115=0606/申请325，116=0607→0608/申请326，117=0609/申请327；其中未获页面结构化印证部分仍为历史线索。复制0601→0610上游已完成，本次未重复。

## 3、签名查询实测
- 正式列表→117详情：批记录已作废，5000，15工序。生产记录→粗洗，08:30:32，5000/损耗0，操作员模拟记录张飞红；复核张飞红“签名时间未记录”。点击复核签名，完整错误“签名证据与正式记录不一致”。证据02。
- 过程检验→原始提交→清洗→巡检第1轮：2026-11-15、80件、损耗1，模拟提交张飞红08:30:33；复核同样时间未记录。点击返回同一完整错误。证据03。未取得失配维度，不直接推断签名本体无效，也未覆盖全部工序/轮次。
- 操作记录QA处置：管理员/9908100249，08:50:26，成功；意见含评审117、1009200609、申请327、附件SIM-NCR-L-VOID-QA117-yingtaigroupadmin.txt。创建者朱利江/9908101028，08:40:49，未签名。
- 处置签名64314：QA_DISPOSITION、管理员/9908100249、签署08:50:27、原签时姓名管理员、含义QA不合格评审处置、时区Asia/Shanghai、正常指定人员身份确认方式。意见与117/0609/327相符，未单列租户ID/周期ID/正式记录ID，不能计这些维度全面通过。证据04。
- 展开核验：原记录完整性VALID；本次“摘要与时间核验通过（审计已停用）”；本地摘要/时间值一致；创建审计“已停用（本地开发）”；范围“本地摘要与签署时间值（创建审计已停用）”；服务器时钟、未绑定外部可信时间；原因GXP_AUDIT_DEVELOPMENT_DISABLED。操作日志与签名相差1秒，只保留原值。证据05。

## 4、治理正常入口
- 系统管理→电子签名→签名记录，动作QA_DISPOSITION筛选9条，117—109；112—117均有历史行，显示已校验，不等于本次完整重验。116技术信息UNIFIED-64313。
- 117行详情：`/mes/pro/feedback/edhr-nonconformance-review?reviewId=117&from=signature-governance`。标题评审117、单号00000008、完整订单相符、已关闭/作废、冻结08:40:49、签名#64314、完成08:50:26。明确不能再次处置和当前仅供查看，仅返回列表，无处置能力。无缺少详情路径。证据08。
- 116、115各行正常详情，参数reviewId=116/115，均实际报“eDHR 不合格评审单不存在”。未构造URL代替正常入口，不计对应只读详情通过。证据09、11。
- 116预览完整错误：“电子签名 PDF 导出失败: 该历史签名未保存签署时的图片快照，不能使用当前签名图片代替”。未补图或替换证据。证据10。
- 112—114只验证治理历史行及意见，未逐个打开原单/完整核验，不由115/116外推它们也不存在。

## 5、档案、交接、通知
- 正式历史追溯无筛选1条EDHRB-1794702701662，同完整批次，开始08:27:21，已作废。详情正常打开`/mes/pro/feedback/edhr-batch-execution/active-order-detail?batchExecutionId=900000001336&from=/mes/pro/feedback/edhr-batch-history`。未看到116旧/0608新周期；未点击反查关联批次，不称全库无档案。证据12。
- 本人工作台没有当前待办，隐藏0；历史交接8条均已完成，描述仍为创建时待QA处置。以下为本次可见定位，不证明对应源单仍存在：

|评审线索|原订单|交接任务ID|场景|
|---|---|---|---|
|117|1009200609|2121765330785501184|负责人作废|
|116|1009200607|2121754258313674752|负责人返工|
|115|1009200606|2121744427376803840|负责人让步|
|114线索|1009200605|2121733225577807872|组长作废|
|113线索|1009200603|2121719353353535488|组长返工|
|112线索|1009200602|2121704673788657664|组长让步|

- 116通知回执：候选用户9908100249，已发送、尝试1、失败原因空，发送时间原值1794700610000。站内信有8条未读，含115—117及组长三场景；116时间07:56:50。原通知实际可见，不可说未发送，也不证明新的返工下游通知发送。未阅读/全部阅读/重发。证据14、15。
- 116交接“查看原轮次”实际落到我的站内信列表，没有可见原档案及新旧周期关联。URL：`http://localhost:8081/user/profile?activeOrderId=1009200607&reviewId=116&handoffTaskId=2121754258313674752&roundId=116&handoffType=QA_REVIEW&tab=notifyMessage&handoffReadOnly=1&handoffResult=completed`。隐藏DOM不当详情打开。证据15。

## 6、可交下游对象及前置
- 在制工单2行，产品按压式球囊扩充压力泵/IDI，组长张飞红，数量5000、生产0%/检验0%、在制、暂无待办责任人。自身组长页暂无负责工艺路线，不代表全局无活跃订单。证据13。
- 新副本`SIM-COPY-SIM-COPY-SIM-COPY-CODX-PQC-20260804383b66f02f9c4d98600`，加入13:05:42，与上游0610时间相符，但表不显示活跃ID，未独立证实0610ID。旧行`SIM-COPY-SIM-COPY-CODX-PQC-20260807-SP-WO-04842aa87dbebad1cb9c3`，加入04:03:43。
- 两行可交生产组长确认正式生产入口，不是保留115/116让步返工实例或放行待办。让步需115/0606源单、原周期、关闭后正式生产入口；返工需116/0607旧档案、0608新周期及责任交接。当前前置未满足，不自行重建或用副本替代原追溯验收。

## 7、UI问题
- 较高：签名错误缺签名ID、评审/订单/周期/正式记录与失配维度；复核时间未记录，难定位。应展示追溯上下文，区分未采集/错配/权限，不强行判通过。
- 较高：治理源单不存在后空列表出现新建、丢失原签名上下文；116查看原轮次只落消息列表，需核实已完成交接档案入口。
- 中：已校验概览没有审计停用/仅摘要时间的范围提示；长意见撑满整屏、操作需滚动；重复标签(2)(3)(4)易混单，应摘要折叠、固定身份操作、标签显示评审号。证据07。
- 中：正式列表详情与治理详情信息层级不一致，后者#64314仅文本。建议统一评审摘要、签名核验和旧周期入口。
- 低：通知时间原始毫秒换行，完成交接仍突出旧待QA处置文案；应显示业务时区可读日期、区分原通知与当前完成状态。

## 8、未覆盖及交还
- 未覆盖112—114源单/完整核验、全部生产检验签名、115/116源单不可用原因、116旧档案/0608新周期、结构化租户周期验证、后续生产重做/双100%/PQC上市放行/新通知；不计通过，也不判修复全部失败。
- 内置浏览器承接旧页面，不能证明与其他岗位隔离；按共用资源交还。不关闭、不登出，保留测试1/管理员，当前我的站内信及上述116定位URL，本次后不再操作。
- 绝对证据目录：`D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent`。01—15均本次业务截图；02/03错误、04/05核验、08只读、09/11源单错误、10快照缺失、13在制、14/15通知及原轮次落地。

## 9、关键截图绝对路径
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\02-production-review-signature-mismatch.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\03-inspection-review-signature-mismatch.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\05-QA64314-full-validation.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\08-governance117-readonly-detail.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\09-governance116-source-missing.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\11-governance115-source-missing.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\13-in-process-two-orders.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\14-rework-handoff-notification-receipt.png`
- `D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\eDHR\流程协调员\output\NCR-retest-20261115\QA-independent\15-rework-original-round-lands-on-message-list.png`
