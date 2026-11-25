# P03：生产记录损耗0、正式损耗明细1——独立只读调查

调查日期：2026-11-23。对象：881MO085822／1009200660／首笔粗洗327089；对照周期1009200661、1009200662、1009200663。

## 续查结论：已取得原始持久化证据，定位本笔触发条件

本节为用户要求“定位到问题再告诉我”后的新增调查，取代下文初次调查中关于未查库、缺事件JSON、无法确认空物料分支的当前状态表述；下文保留初次调查过程，不代表本次仍缺这些证据。

**问题已定位：正式损耗1件已保存。粗洗工序冻结配置没有输出物料，本笔因此是合法的“工序级报工”，物料明细为空。生产表却在物料明细为空时直接写损耗0，忽略已保存的工序损耗，行合计与表合计随之错误。**

本次通过现有本地配置连接同一本地MySQL，在显式READ ONLY一致性事务内只执行SELECT，结束ROLLBACK。未执行数据库业务写入或业务API；没有补报、重签、修改配置权限、修代码或重启服务。仅新增本线程证据脚本/JSON并更新此记录。

可复核证据：

- 原始业务字段摘录：`D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\P03-loss-readonly-evidence-20261123.json`。
- 只读提取脚本：`D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\P03-loss-readonly-evidence-20261123.py`。凭据仅在内存中读取现有配置，不写入证据；未导出完整请求、签名或凭据。
- 脚本校验通过：事件327089确属租户186、订单881MO085822、批号K202510048、潘多灵、2026-11-23 00:31:08；完成25、损耗1、物料明细空。
- 事件根JSON中`lossQuantity=1`、`lossDetails=[SYSTEM_OTHER_LOSS/1]`、`materialDetails=[]`；不存在`rawPayload`嵌套层。排除本笔因原JSON层级读错而漏物料的解释。
- 对应正式报工8164，`mes_pro_feedback_material`无物料行。事件内冻结配置与持久化工序快照1009209877的配置完全相同；`outputMaterialIds=[]`，事件内配置SHA-256校验通过。因此该空物料不是“填写过但没保存”的证据，而是符合本工序正式配置的提交模式。
- 数量片段8039为OUTPUT25；8040为LOSS1。损耗既在事件JSON中，也有正式LOSS片段，不能说损耗未保存。
- 327089只有一条分配，原周期1009200660、25件、版本1，仍为CURRENT。没有跨单拆分。
- 对照327091的分配历史：原26在版本2变为源22、目标1009200661为4，该事件损耗0。与本笔损耗1无关。

四单对照也取得正式事件数据：

| 订单 | 原周期 | 首笔非零损耗事件 | 完成数量 | 正式损耗 | materialDetails | 冻结输出物料 |
|---|---|---|---:|---:|---|---|
| 881MO085822 | 1009200660 | 327089 | 25 | 1 | 空 | 空 |
| 881MO089518 | 1009200661 | 327092 | 1 | 1 | 空 | 空 |
| 881MO094070 | 1009200663 | 327095 | 1 | 1 | 空 | 空 |
| 881MO085999 | 1009200662 | 327119 | 105 | 1 | 空 | 空 |

对应源码链路未发生字段层级转换：详情SQL取事件`raw_payload`根层；服务分别解析`materialDetails`和`lossDetails`；控制器同时输出两者。`ActiveOrderSubmissionDetailPanel.vue`当前3099行判断物料列表、3122行在空物料分支固定`lossQuantity: 0`、3123行总数加0；2862行的正式损耗明细则独立读取`submission.lossDetails`。这与原图25/0/25及明细1逐项吻合。行号因并行改动较初次记录增加2行，以本节当前读取行号为准。

修复建议限定为显示契约：明确支持没有输出物料的工序级报工，以已保存的工序损耗显示行及合计。对本笔，完成25＋损耗1应显示总数量26。不要新增物料配置来绕过显示错误，不改正式损耗1，也不重复补报或签署。

核验边界：本次拿到了事件/数量片段/分配/冻结配置的实际只读证据；未重新登录页面、未捕获实时详情HTTP响应、未对当时运行包做哈希比对，不声称完成E2E复测或修复验收。本笔持久化数据的具体空物料触发条件已证实，原始截图和当前实际展示分支相互印证。

## 结论与证据强度

1. 已亲自查看09、10、11、13、15原图，并补看12、14。直接确认正常单同一时间00:31:08的生产行显示25／0／25、三个输出物料字段“未记录”，正式损耗明细及汇总却显示SYSTEM_OTHER_LOSS／1。09未露出表尾，不能将正常单合计0说成本次从09直接看到；该合计取自岗位回执。12、14直接露出另外两单损耗合计0。
2. 当前源码能确定：事件级损耗和物料级损耗是两个独立读取模型；生产记录不读取事件级lossDetails。无materials时固定输出0，合计再累加该0。不是合计加法误算。
3. 比“发现置零代码”进一步确定：当前后端明确允许冻结工序没有输出物料时，以materialDetails=[]提交工序级损耗；这一路径会正式保存lossDetails与LOSS数量片段，却不创建物料行。此类有效业务输入必然被当前生产表显示为0。尚未取得327089的原始响应、事件JSON或冻结会话证据，因此不能宣布本次已经证实走了这一合法空物料路径，更不能宣布物料保存失败。
4. 当前详情SQL直接读取事件根JSON；控制器明确输出materials和lossDetails，没有发现这两字段在控制器转换中被遗漏。跨单分配只提供submittedQuantity，当前展示链没有把lossDetails按比例变换为0。327091是岗位回执中的跨单源事件，与327089不同；不能用其22＋4分配解释327089的25／0／25。
5. 到达的确定层级：截图现象＋当前代码的提交/持久化设计/读取/展示契约缺陷；未到“327089运行数据逐层核验闭环”。源码与当时运行包一致性也未核验。无代码修复、业务写入、API调用、数据库查询、配置修改、服务操作或重复签署。

## 原始截图（全部为绝对路径）

| 文件 | 本次直接看到的事实 |
|---|---|
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\09-正常单记录损耗列0.png | 正常单粗洗00:31:08，输出物料编码/名称/规格均未记录，生产25、损耗0、总数25，复核人未签名。截图本身不展示事件ID；327089关联来自任务资料，时间/人员相符。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\10-正常单正式损耗明细1.png | 粗洗SYSTEM_OTHER_LOSS／其他损耗1，填写人潘多灵，00:31:08；汇总亦1。该裁剪图不显示订单号，与09关联依赖同组回执及相同时间人员。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\11-让步单正式损耗1.png | 881MO089518，粗洗正式损耗明细/汇总1，00:49:39。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\12-让步单粗洗合计7.png | 生产合计7、损耗合计0、总数量7；可见记录物料未记录。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\13-返工单正式损耗1.png | 881MO094070，原周期粗洗损耗明细/汇总1，00:54:11；不代表新周期0664。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\14-返工单粗洗合计3.png | 生产合计3、损耗合计0、总数量3；可见记录物料未记录。 |
| D:\ProjectPackage\IntRuoyi\IntRuoyiAll\workspace\协调部\edhr协调员\output\生产组长-四case-1123\screenshots\15-作废单正式损耗1.png | 881MO085999，粗洗损耗明细/汇总1，01:15:59。此图不显示生产行损耗及合计。 |

## 真实业务链与源码依据

下列均为当前工作区行号；不以源码替代事件实值。源码路径前缀为D:\ProjectPackage\IntRuoyi\IntRuoyiAll\。

### A. 一线生产填写与payload

- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue:5662`：有配置输出物料必须至少填写一项；无配置输出物料走工序数量校验。因此“无物料”并不自动等于保存失败。
- 同文件6720—6746：逐输出物料构建materialDetails，填写物料ID/编码/名称/规格、outputQuantity、lossQuantity及lossDetails；材料损耗来自材料草稿的原因数量求和。
- 同文件5698—5706、6562—6566：有物料时进度取各物料完成量最小值、损耗取物料损耗总和；无物料时读取工序草稿完成量和工序损耗原因。数量口径不同是明确设计，不可把多物料完成数简单视为成品翻倍。
- 同文件6378—6467：顶层materialDetails；feedbackPayload.outputQuantity/lossQuantity/lossDetails；rawPayload也由6795—6821把materialDetails、lossDetails置于根层。
- `IntRuoyiFronted/src/api/mes/pro/feedback/index.ts:1219`提交`/mes/pro/feedback/frontline/submit`。

### B. 接口授权、聚合与正式持久化

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/MesProFeedbackController.java:114`：正式提交要求登录；服务进一步校验设备账号、签署身份、冻结会话、所选活跃订单。
- `.../service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:120—159`：从冻结会话区分输入与输出物料；输出物料为空时materialSubmission=null，进入743—751校验，要求materialDetails为空，但仍校验并接受工序级损耗。
- 同文件480—508：有输出物料时按服务端验证结果聚合progressQuantity、totalLossQuantity、原因明细，并覆盖物料快照。754—762正式报工合计为完成＋损耗，合格数为进度数，不合格数为损耗数。
- 同文件207—236：创建正式feedback；仅materialSubmission非空才创建物料记录；之后创建正式事件和初始订单分配。这不是“所有报工必须存在物料表行”。
- 同文件787—803与`.../service/pro/feedback/frontline/MesProFeedbackMaterialServiceImpl.java:28、92—94`：有物料分支将物料完成量、损耗量和原因JSON写入`mes_pro_feedback_material`。
- `.../service/pro/feedback/frontline/MesProFrontlineFeedbackPayloadSplitter.java:117—142`：先展开reqVO.rawPayload，再显式在事件JSON根层写outputQuantity、lossQuantity、lossDetails和materialDetails。当前写入契约不是`rawPayload.materialDetails`二层。
- `.../service/pro/processpool/MesProcessPoolSubmitEventServiceImpl.java:97、109—142`：原JSON序列化进事件；完成量与损耗量分别生成OUTPUT和LOSS片段，LOSS片段携带原因明细。`MesProcessPoolEventServiceImpl.java:68—89、296`创建事件、保存rawPayload与数量片段，生产事件创建前检查工单冻结。
- 以上只能说明设计上保存何处；未查询327089对应feedback、物料表行或LOSS片段，不声明它们实际存在或不存在。

### C. 初始与跨单分配

- `.../service/pro/processpool/team/MesReportAllocationCommandService.java:187—250`：初始分配接收完成量，校验事件和活跃订单，将outputQuantity写allocatedQuantity，不把损耗加入完成量。
- `.../service/pro/processpool/team/MesReportAllocationQuantityFragmentService.java:58—60、227—233`：分配片段只读OUTPUT，不处理LOSS；不能据此说LOSS被分配算法扣成0。
- `.../service/pro/processpool/team/MesReportAllocationPoolQuantityService.java:19—25`：可分配池读取reportOutputQuantity。
- 岗位回执明确327091原26分为源22＋目标4；327089为另笔首报25。当前截图09复核尚未签名，损耗差异已存在。未取得两笔事件分配版本历史，故不把回执当成数据库审计结果。
- 额外静态边界：有跨单分配时，详情的submittedQuantity使用分配量，但materials中的outputQuantity及lossDetails仍取完整源事件。若有物料或有损耗的事件被拆给多单，可能出现物料数量与分配量不一致或损耗在多单重复展示；本次没有该运行实证，不能借P03直接扩展为另一项已证实缺陷。

### D. 详情接口、数据读取与输出

- `IntRuoyiFronted/src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue:133`调用详情；`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts:1141—1148`直接GET`/mes/pro/process-pool/team-leader/active-order/detail`，无物料/损耗字段改写。
- `.../controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:620—626`要求生产组长或在产订单查询权限。`.../service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java:173—181、222—225`进一步校验人员可读范围、活跃状态与上市资料读取条件。历史/作废/返工访问需使用正式历史入口，不能放宽状态来查。
- `IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml:57—66、100—111`：从CURRENT订单分配取得eventId及allocated_quantity，从对应PRODUCTION_SUBMIT事件直接取raw_payload作为originalPayloadJson；没有读取`mes_pro_feedback_material`，没有读取分配片段JSON，也没有进行损耗比例转换。
- `.../service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java:241—249、1840—1866`：逐行构建submission，submittedQuantity来自分配量；materials、lossDetails分开解析。
- 同文件1928—1951：读取事件根层lossDetails；1954—1990：只读根层materialDetails，缺失或空数组返回空；有物料时验证ID/编码/名称，直接读取其损耗；2062—2073仅解析JSON，不自动展开rawPayload。
- 同文件1254—1256展开rawPayload的是PQC原始项目读取，不能套用到生产物料读取。
- `.../controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:1266—1313`同时显式输出materials、lossDetails和材料lossQuantity；未见控制器漏映射。
- `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts:582—596`的submission接口只有lossDetails，没有事件级lossQuantity字段。即便事件根层有lossQuantity，当前详情模型也未将其直接提供给生产表。

### E. 两类显示和合计

- `IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:2860—2877`：明细直接展开submission.lossDetails，汇总按工序＋原因累加quantity。
- 同文件2997—3018：物料行带编码/名称；3021—3035：事件行没有物料字段；459—461把空物料字段显示“未记录”。
- 同文件3090—3124：有materials读取material.lossQuantity；3081—3084缺失/无效时归0；无materials直接固定lossQuantity:0，总数为submittedQuantity＋0，完全不消费lossDetails。
- 同文件3137—3143：损耗合计只累加已经构造的行损耗，所以行0自然导致合计0；生产表总数量同样漏计此事件级损耗。
- 09三个物料字段均“未记录”与当前事件行分支高度吻合；当前服务有物料会要求编码名称非空。因此在运行包与源码一致、响应未被其他层改写的前提下，可进一步收窄为前端无materials分支。缺原始响应，不能把该条件推断写成无条件运行结论。

## 各候选原因的判定

| 候选 | 本次确定程度 | 还缺什么 |
|---|---|---|
| 物料未保存 | 未证实。合法无输出物料分支本就不创建物料行；页面“未记录”不能证明写入丢失 | 327089冻结会话OUTPUT清单、原始提交的materialDetails、feedback物料行 |
| 控制器未输出物料/损耗 | 当前转换代码可排除固定漏映射；不能排除运行版本或实际响应异常 | 当时/现正式详情响应、运行包版本 |
| rawPayload层级错误 | 当前生产写入与读取均为根层，未发现静态层级错配；PQC有解包逻辑不代表生产同样需要 | 327089事件raw_payload，特别是根层与rawPayload子层键分布 |
| 跨单分配投影遗漏 | 已确定详情是分配完成量＋原事件快照组合；没有查到把损耗按比例变0的步骤；跨单事件回执为327091 | 327089与327091分配CURRENT及版本审计、片段归属 |
| 前端缺物料/缺字段填0 | 当前源码确证；截图更支持无物料分支，具体输入未正式捕获 | 327089响应中的materials、lossDetails、submittedQuantity |
| 真正无损耗 | 与截图正式明细1相冲突，不能据生产表0成立 | 不应通过清零或补报来改变既有证据 |

## 最小补证集合（只读，不要求重做业务）

1. 经既有授权历史/详情页面加载1009200660，留存含327089的原始响应：eventId、submittedQuantity、materials完整列表、lossDetails、submittedAt；若原周期已退出活跃池，使用实际历史入口。不要调用写接口或通过重新提交制造响应。
2. 同租户327089事件只读摘录：feedback_source_id、raw_payload中的outputQuantity/lossQuantity/materialDetails/lossDetails与可能存在的rawPayload子层；OUTPUT/LOSS片段的类型、数量、来源事件。签名凭据、令牌等不采集。
3. 对应冻结会话OUTPUT材料清单、对应feedback的`mes_pro_feedback_material`行；由此区分“合法无材料”和“应该有但未保存”。若事件已有材料而响应没有，锁定读取/运行版本；若响应有材料而页面未记录，锁定前端或版本。
4. 327089和327091的分配行与变更历史：当前周期、allocatedQuantity、版本、有效状态；据此核实25与22＋4的关系。另三周期的首笔损耗事件ID需要正式证据，不能凭327089—327136区间猜测。
5. 当时前后端版本标识或构建信息。此次未访问运行页面、未调用接口、未查库；在现有本地回执中未找到上述事件级原始JSON。这是本次记录级结论的证据缺口，不是已确认服务无法访问。

## 最小修复建议（仅建议，未执行）

1. 首先补齐事件级损耗显示契约：无物料报工使用该事件正式损耗量/明细形成生产行损耗及总数，不把材料为空解释为无损耗。单独增加“缺字段取明细”的通用fallback会掩盖契约异常；应按明确的工序级与物料级模型分别处理。
2. 有物料必须使用正式物料快照中的损耗，缺值应暴露契约错误；不把整笔事件损耗重复摊到每个材料。保留正式原因明细1，不修改原签名事件、不补报、不清零。
3. 修复前明确被跨单拆分事件的损耗归属：当前详情混用了分配完成量和源事件快照。若327089原单原数量25且损耗1成立，生产行应呈25／1／26；这只是该前提下的期望，不代表其他拆分单可套用同一加法。
4. 后续授权修复后的集中验证至少覆盖：无物料非零损耗、有物料非零损耗、零损耗、多个材料、跨单源/目标与合计。真实前端复验既有记录，禁止重报或重签制造通过。

## 本次交付边界

只新增本线程记录；原问题报告和截图保持原样。本次是静态链路核对与原图复核，未运行自动化测试或E2E，未声称数据库保存完整或具体运行根因已最终闭环。代码行号均按本次读取的工作区版本。
