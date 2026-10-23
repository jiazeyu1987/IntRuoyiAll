# E12 PUBLIC 四项附加边界：已有证据映射与最小计划

PREPARE/HOLD。四项本轮 PUBLIC 均仍缺精确验证，不能由正式重提31项或阶段修复33步关闭。没有错密码用例。当前仅离线查阅本例局部合同、fixture、runner和既有真实UI记录，没有新浏览器或业务动作。

## 原合同与逐项映射

高层合同为 doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md:38，E2E-12“部门指派与全部会签”。四项附加验收的准确名称/动作来自本例 rerun-20261020-r2-round2/handoff.json:132 起及 add-boundaries.cjs:13/18/21/24，并非高层表中新增四个独立原用例。

|准确原名称|原动作及通过标准|已有 CUSTOM 真实UI|已有 PUBLIC 真实UI／本轮结论|
|---|---|---|---|
|assignment-missing-selection|L未选择本部门会签人即点“签名确认指派”；显示“请从当前本部门正式候选目录选择会签人”，不完成指派。|cal3 step402空值→403提交→404提示→405读取，PASS。|原四轮NOT_RUN。最新continuation1 step8/24只验证初始选择为空；到step14/30前已经选择人员并输入密码，不能证明空选提交拦截。本轮仍缺。|
|assignment-missing-password|L已选有效本部门人员、已填意见但密码为空，点“签名确认指派”；显示“请输入当前账号密码完成指派签名”，仍停留指派区。|cal3 step406–411，PASS。|本轮NOT_RUN。但历史R9存在PUBLIC真实证据：public-r9-02 step55空密码提交，step57真实内联DOM显示该提示；step56误按弹窗读取ERROR保留。该旧运行Jar为478fff77…，不作当前709e78bd…本轮通过。最新continuation1 step13/29先secret、14/30才提交，没有此负向动作。|
|signature-cancel no advancement|C1打开“审核通过”签名框，点击“取消”；框关闭，页面仍0/2已完成、不推进。|cal3 step439–444，PASS。|本轮PUBLIC段没有签名框取消动作。主管签名、正常会签和最终批准待办均不替代取消验证；仍缺。|
|signoff-missing-password|C1打开会签签名框，填写意见，密码为空点“确认签名”；显示“请输入登录密码完成电子签名”，不完成会签。|cal3 step445–449，PASS。|本轮正常会签均先secret再确认（continuation1 step43→44、55→56），没有空密码确认或对应提示；仍缺。|

cal3 均指 runs/20261019-r3/rerun-20261020-r2-round2-cal3/session.json，CUSTOMPOS 对象4349。R9原始记录为 runs/20261019-r3/public-r9-02/session.json，对象4265。最新continuation1指 runs/20261019-r3/resubmit-20261020-1740-continuation1/session.json。记录编号完整前缀均为205454566804408。

已局部核对：cal3 PUBLIC步骤1–336、checkin-same-name-20261020-1658-business PUBLIC整段、resubmit首次PUBLIC整段及continuation1步骤1–73；均无三条负向提示DOM、无签名取消动作。continuation2步骤93只是PUBLIC最终历史读取，修复33步只读对象是CUSTOM4381，不是这四项。add-boundaries.cjs:27 的“PUBLIC checked on resubmission”只是当时计划注释，不是已经执行的证明。既有31项/33步PASS保留其原范围。

## 自有对象清单与最后已保存UI节点

仅查本例现有fixture/清单及最后UI，不做全仓或服务器状态审计：

|PUBLIC对象|最后证据与状态|可直接做L指派边界？|
|---|---|---|
|4265／DCC241016C12R8PUB1|public-business-results-r6.json，public-r9-02 step126，独立批准待办。|否，已完成L与C。|
|4341／R2R2PUBLICPOS|round2 manifest，cal3 step112，独立批准待办。|否。|
|4346／R2R2PUBLICREJC1|原驳回且仍检出、原失败检入无后继；原始封存对象。|否，明确不触碰。|
|4348／R2R2PUBLICREJC2|round2 manifest，cal3 step334，已驳回。|否，不重开原PASS。|
|4375／SAME1650旧版|1658-business生成的旧驳回；后继4376/4379。|否，旧证据不改。|
|4376／SAME1650工作稿|已由唯一提交产生正式4379；工作记录本身无L任务。|否，不重复重提。|
|4379／SAME1650正式版|resubmit continuation2 step93，“待会签批准／当前阶段：批准”。|否，保持批准待办。|

CUSTOM4349/4352/4355及4380/4381是另一模式，不可代替PUBLIC。早期fixture-manifest.json也是APPLICATION_CUSTOM。最新L2已保存todo（resubmit continuation2 step32）显示“暂无审批任务”；L1最后详情为已完成本轮指派的CUSTOM4381，此后最终历史已到批准。没有从这些本例记录发现可确认仍待L指派的自有PUBLIC对象。

以上是最后已保存UI事实，不是本轮实时服务器盘点；没有声称现在绝对不存在其它待办。恢复窗口第一步仍须通过真实待办只读核对本例已知对象，不能为寻找对象泛扫别人的单据。

## 供Root review的最小续验计划（尚未执行）

1. 放行后先核对原关闭回执、运行指纹及真实身份，再从L1/L2正式待办检查本例已知PUBLIC对象。只有真实页面能同时确认任务自有PUBLIC、精确fileId/BPM、当前L待指派且可到达C1待办，才复用该对象，不更改其路线。若无可用对象，才创建恰好1份新的PUBLIC边界专用申请。已有对象表明新建的必要性在于两个L边界必须有尚未完成的正式指派，两个C边界必须有尚未完成的会签；原批准/驳回/工作稿不能逆转或借用。
2. 新建时仅复用既有项目285/文件夹21、PUBLIC类别908772、矩阵914030/v2、R3账号与已有本例PDF。新业务编号须页面查重。保持默认D1、仅补D2和独立批准人910521，不改任何类别、矩阵、角色或权限。真实上传最终确认一次；L2本人主管签名通过一次，到达L。没有新建账号、重置密码、修改签名图片或额外负例。
3. 在一个L本部门指派区依次完成空选提交提示、有效选择后空密码提交提示，均保存DOM/截图、相同fileId/BPM、仍未指派的状态及自然请求元数据。之后本人用原有效凭据完成该部门唯一一次正常指派。另一负责人仅完成使C1正式会签入口可达的正常指派一次；沿用已验证的D1→C1、D2→C2路线，不重复部分/全部通过或驳回分支。两次正常指派是到C1前置，不计新的边界PASS。
4. C1本人从真实待办打开此申请：先开签名框并取消，读回仍0/2且仍为会签；再打开，填意见但密码留空，点确认，读到原空密码提示且0/2不变，随后取消关闭。到此停止，保留C1/C2待会签，不执行任何C通过/驳回、独立批准或后续流程。
5. 只为上述4项保存新独立run、trace、截图与逐项DOM/无推进证据；仅被动method/path/status，签名/凭据输入段不录trace，隐私扫描、CRC及context/browser/runner闭合后补充结果。首次异常即停，历史错误不覆盖，不自动重试或从头重放。

凭据沿用 .runtime/int_qms/e2e12-r3/credentials.xml 的原DPAPI（现有launcher Import-Clixml→内存解密→进程环境），按已有账号阶段记录使用唯一已知有效值。无当前可用vault或真实身份不符即停止；不猜测、不测试错密码、不为了负例锁账号、不输出或落盘明文。当前本轮没有读取或解密vault。

当前Root已接受41508/48061、20976/8061、Jar709e78bd…及upload877479ff…/detail67a10e26…；这些是后续预检预期，不冒充本轮新运行验证。当前不改runner、gate、生产文件或配置，不启动UI，不执行Maven/构建/重启/DB/API/Git/清理。本次交付为四项证据缺口映射与条件计划，等待Root后续放行；不是新增E2E通过。
