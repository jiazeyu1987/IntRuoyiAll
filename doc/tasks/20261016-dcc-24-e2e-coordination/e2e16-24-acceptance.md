# E2E16–24 当前验收

来源：`../20261015-dcc-six-thread-followup-review/e2e-test-cases.md` 原16–24行；不缩小参数分支，不用准备成功替代业务完成。

当前状态：in_progress。E2E-17与E2E-24已按2026-10-20用户确认的业务范围更新为PASS，详见各自报告；E17同人多部门情形移出范围且未执行，E24音视频仅作附件，不纳入正文工具/翻页/搜索门槛，WAV/WEBM附件专项准入与权限未验证。其他用例仍按下表和各自报告记录，不据此宣称整组通过。原历史执行状态、线程协调及维护窗口记录保留如下。

|用例|必须证明的原流程|当前业务证据|
|---|---|---|
|16|延迟并培训，负责人O组织、原作者U上传，培训后U确认及签名/时间校验|原4228/A1已ACTIVE，记录U/原BPM及签名913/受控时间一致、3类校验和终态正文已复核；严格计划时刻前独立日常观察缺失，保留边界|
|17|未受控/待生效不下发；生效后全部所选部门人员逐人签名、范围隔离|2026-10-20用户确认每人仅归属一个部门，同人多部门参数移出本版本范围（未执行，不记PASS）。修订后整例业务PASS：原4234/A1三名不同接收人真实UI逐签、待签3→2→1→0，范围外人员排除；R4五项定向Maven GREEN。均为继承证据，本轮未重跑。[当前范围结论与历史决议记录](../20261016-dcc-e2e-17/verification-report.md)|
|18|选草稿升版、变更说明及多个附件、完整流程、新旧附件/培训记录隔离|原PARTIAL/REPLACEMENT完整业务2参数PASS：A1→A2/4252→B1/4256，各自正文与附件/培训逐个实际预览、上传人/时间/BPM正确；15+13trace脱敏。R28在22原S/L签名后仍复现，实际reading-session/stop拒绝已定位，原通知修复撤回|
|19|项目文件夹目录、名称编号搜索当前/授权项目、版本与权限|23项真实UI PASS、Root接受；当前项目跨文件夹入口已修，定向20项GREEN，原错误与邻近旧harness失败留存|
|20|同链有效/待生效/草稿，各正文入口及撤权后重新读取|原27参数真实UI PASS；原4231 ACTIVE、4245仍未来延迟待生效，新4251 WORKING草稿，前端正文及撤权边界均验证|
|21|审批/审批历史/版本历史固定正文，草稿标识及入口权限|原10参数真实UI及Root review PASS，root-integrated-result.json；九个trace、A→B→A、重开及三账号拒绝均核验|
|22|版本详情事实、固定关联及引用项目去重明细、补关联/整改记录|原Main4249 ACTIVE、原Rel1/后补Rel2冻结及A1正文通过；原Rel1工作稿4257实际换版为4258/B1，S/L已成功、C待办保留；源B1终态后固定性/引用去重/整改等未完成|
|23|三种附件查看下载权限、新旧培训记录、取消/连续点击/离开页面|原六权限参数业务PASS，A/B各两附件及培训记录正确，原交互负例保留；历史负例为正式历史搜索无可读行，直达地址不计稳定拒绝通过|
|24|PDF/Office/图片/文本等实际正文及工具、水印/计时、重开原件不变|按用户确认范围整例正文PASS，依据既有正文证据及七缺失格式续验收；音视频仅作附件，WAV/WEBM正文能力不属E24门槛。音视频附件专项准入/权限未验证，不能由本结论推定PASS。详见E24当前验证报告|

上表为2026-10-18 21:20原线程/当前证据核对进度；局部通过、完整接受及证据限制分别标明，运行进程存活不等于验收通过。

执行约束：原线程、gpt-6.1-sol/high；仅本例真实UI账号/权限/数据；业务通过依据DOM和真实文件，不解析响应JSON代替断言。API/DB不执行业务。Root独占Maven、共享运行包与48061重启；生产文件须根因证据、BDD及有效RED后由Root分配唯一写者。禁止新子Agent、Git写、共享历史数据修改、兜底/吞异常/兼容补丁及无关审计。

发现问题逐条记入本文件并向用户打印：触发证据→根因→源头最小改动→原流程复验。旧BLOCKED不能直接沿用作当前结论。

## 本轮发现

- R28最终收敛（09:40）：A、D的DOM瞬时事件均真实抓到拒绝；D正常页面GET失败采集为空，旧GET诊断不能证明根因。直接调用链确认task-action-readiness为POST，validateTaskAction会在已流转任务上读取并拒绝；前端refreshTaskActionReadiness已按弹框generation/context/visible处理错误，但API未接管全局通知。撤回此前三个GET的可选参数/详情接线及预览GET的通知改动，保留历史RED/GREEN和失败UI说明，最终仅修审批条件预检专用读取的一项通知选项。新测试执行真实API和弹框handler，要求失败仍reject、当前弹框错误可见、过期弹框不影响新状态。原签名均已成功，不重放；该最小修复后在后续原签名继续严格记录瞬时DOM事件。
- R28复验边界（09:10）：B1主管S签名后稳定页无迟到提示，但实际C会签返回内容仍短暂含拒绝；此前1500ms之后的零通知检查漏掉瞬时消息，已将回归状态修正为未关闭，保留失败截图。原时点服务日志两次bodyDenied，C实际页面仍启动预览metadata读取；getControlledFilePreviewMetadata尚走默认全局通知，而其所有实际预览调用方均已有按当前请求显示的错误状态。新增该实际getter拒绝/精确正文上下文RED后，仅为该专用读取启用既有UI接管选项。只读访问日志诊断未查到记录，不将空日志当无失败证据，不读取请求参数或响应正文。
- R28根因（07:30）：S签名成功并返回已无待办的审批中心，截图右上仍为ElNotification.error；该通知由axios业务错误拦截器生成。旧详情getControlledFile/纸质记录/权限说明读取未声明既有ignoreErrorMessage选项，通知先于reloadAll及loadAccessExplanationOnly的isCurrentDetailLoad判断。BDD：Given 旧文件任务读取尚未结束且已经返回收件箱，When 读取拒绝，Then API仍reject、服务端资格仍拒绝，由已有页面上下文决定错误展示；当前页面同样拒绝仍明确显示错误，其他调用默认通知不变。只为这三个GET增加可选既有请求选项，详情读取显式接管展示，不改axios、审批动作、角色或后端授权；先执行实际API与详情函数的RED。
- R27根因修正（06:42）：实际刷新后服务器按route中的旧4240重新定位A1，无法仅靠预览按钮的同Master判断保持所选草稿。保留第一次预览局部RED/GREEN及未通过的实际回归，不计问题关闭。新增完整submitCheckin行为RED：业务写入成功并merge后，没有一次导航到返回的Working身份（routes0，expected1）。最终只在已知同Master的正式编制浏览检入成功后，沿既有路由同步守卫将workingFileId更新为返回ID，保留筛选条件；openPreview恢复原精确ID判断，日常边界不放宽。原Office4253已保存，后续用该工作稿的一次真实备注修订验证成功后导航与预览，不能重放原4240的检入。
- R1624-27（编制浏览检入后选中新草稿仍发送DAILY）：原Office4240检入一次已生成4253/WORKING；正式编制浏览route仍锚定原4240及同Master，列表merge已真实选4253，但openPreview只按workingFileId等于选中ID判AUTHORING，实际popup对4253携带DAILY并被正常拒绝。从“填写审批申请”→详情预览同4253实际AUTHORING已读取草稿及阅读时间。BDD：Given 正式编制入口和真实列表确认同一Master的检入后继草稿，When 预览所选后继版本，Then 使用所选精确ID/版本的AUTHORING；普通日常入口、不同Master、非工作状态或缺列表身份仍不切换意图。只改该私有预览入口判断，不改后端阅读守卫或令所有草稿可读；先执行实际SFC openPreview及正式上下文模块的RED。

- R1624-28待定位（签名成功后额外无权提示）：18原A2/4252主管S及实际会签C均实际签名成功并进入下一任务，返回审批中心后仍显示Current user cannot access this controlled file；两次真实截图和下一任务已留存。尚未证明是迟到原详情读取还是其他调用，不能放宽签名后权限或吞所有错误来消掉提示。
- R1624-26同类项目入口（2026-10-19 05:49）：原存储全域真实请求已在b9f7b31a返回1177ms，名称QUERY与内容拒绝保持。18/U从项目浏览切全部版本时仍timeout30000；实际线程栈getProjectBrowserPage:460→isWithinAssignedFileScope→hasAnyPermissions确认另一候选循环同因。BDD：Given Project浏览真实SQL产生多个项目/状态候选，When 同一请求逐候选授权，Then 本请求只取一次分配范围、项目可读集合、目录集合及电子收件集合，原count/page/正文限制保持；下一请求分配或项目撤权立即生效。仅复用已存在的批量接口和私有判断，不改权限服务/SQL候选范围/默认状态。先补现有ProjectBrowserScopeIntegrationTest的真实H2 SQL+Query行为RED。

- R1624-26（存储目录候选循环重复读取已取得的分配范围）：2026-10-19 05:09原NoAccess存储全域请求真实超时；05:10线程栈确认listControlledFileBrowserCandidates→canAccessQuery→isWithinAssignedFileScope→resolveActiveAssignedControlledFileIds→hasAnyPermissions→临时角色SQL。同一方法入口已取得本请求activeAssignedControlledFileIds，但null表示无限制时仍逐候选重新取。BDD：Given 单请求已确定分配范围及目录资格，When 逐候选核对名称查询权限，Then 复用本请求已确定的分配判断；下一独立请求重新读取，撤权立即反映。Root只给私有候选判断透传已知布尔值；不改公共权限服务、权限含义、全局缓存、版本聚合和筛选顺序。先在已有真实Query目录授权/撤权测试加入两次请求的精确权限调用次数取得RED。

- 23脚本预期纠正：原NoAccess自有目录配置明确QUERY=true、PREVIEW=false、DOWNLOAD=false。正式项目和存储入口均可显示名称元数据，不能据“存在行”判断附件泄露；旧零行断言不符合该已保存配置。保留失败记录，后续须真正进入本人只读详情及历史，确认明确拒绝且无正文/附件/下载，不能单靠无DOM称拒绝通过。
- R1624-22完整链第三处状态遗漏（2026-10-19 03:03）：新包29a6下同一原A2源件预览200、页面明确“已上传DCC241016C20-A.pdf”；一次CHECKIN返回业务错误，事务回滚。根因栈AttachmentServiceImpl.bindCheckinAttachments:74，附件绑定校验的检入来源状态也漏CONTROLLED_PENDING_EFFECTIVE，即使本次普通附件为空也必须走该真实事务校验。Root只读确认仍仅4231/4241/4242/4245，无新工作稿，原A2锁保留；不盲重传/重提。BDD：Given 真实private H2受控待生效基线及合法本人检出，When 经完整Query检入事务生成小版本，Then 通过同一附件绑定、属性初始化和原子落位，生成WORKING而源状态/执行指针不提前生效；原错锁/身份/跨版本及普通待审限制保留。新增完整数据库RED后再补这一个来源状态，禁止跳过空附件校验消错。

- R1624-25（23历史详情辅助读链仍要求作废权限）：修复R24后A1追溯query保持，实际已进入4233/A1，但详情为空；后端两次栈为PaperDistributionAckServiceImpl.getPaperDistributionRecords:75。该只读接口仍调用shared detail guard.isAllowed，未接通新增canReadRetainedVersion，导致同一VIEW身份Query主记录已可读而辅助纸质记录拒绝。rg已核对guard.isAllowed直接生产调用方仅Query元数据与纸质记录读取；不改纸质签收/回收或作废写校验。BDD：Given 合法本租户/项目/分配/固定Master及VIEW、无OBSOLETE的旧A1阅读人，When 读取共用详情资格与纸质下发分区，Then 与所选旧版本读取一致，真无纸质记录时真实空列表；无VIEW仍拒。新增真实guard+纸质读取服务RED后，只在历史状态接通同一已验证读取判断，不能吞异常返回伪空数据。

- R1624-24（23真实历史读复验发现迟到列表回调覆盖详情路由）：新包下ViewOnly本人从正式“追溯”进入B1，A1/已作废历史行已真实出现；短暂停留后route从traceability=1变为detail/B1?pageNo=1&pageSize=20&scope=global，记录页容器消失。两次同入口复现，未操作切换身份。直接源码browser/index.vue.reloadBrowserListAndCommitState在getList之后调用syncRouteFromBrowserState，后者无所属页面检查，使用已变为详情的route.path与列表query执行replace。BDD：Given 列表请求未完且已进入固定详情/审批上下文，When 列表迟到完成，Then 不改详情路径及其上下文；当前列表仍按原规则同步自己的分页/筛选。先执行真实SFC函数与迟到caller的行为RED，Root只增加所属列表页面守卫，不改历史权限/详情默认上下文/通用路由框架。

- R1624-22原检出UI GREEN后续源件校验缺口（2026-10-19 01:35）：原A2/4245已由Manager通过真实前端取得本人锁，01:29:22及检入入口可见；随后同名PDF的CHECKIN预览400，未提交检入、未新建草稿。日志精确落WorkflowServiceImpl.validateSourceUploadContext:999，其状态集同样漏CONTROLLED_PENDING_EFFECTIVE；不是文件名错误或缺UPLOAD权限。BDD：Given 已按既定受控基线规则成功检出的本人/同租户/同类别待生效A2，When 同一CHECKIN上下文预览源件，Then 准许进入正常预览；错锁人、错基线、跨租户及待审状态仍拒绝。先在现有WorkflowServiceImplTest补目标RED及控制，不改断言骗过、不重放已成功检出，保留原待生效时间。

- R1624-23（集成回归发现附件能力投影重复查管理权限）：2026-10-19 Root全Query274项仅1项失败，原每请求1次目录管理资格实际调用3次；332项核心合计331PASS，检出2/Adapter31/历史18/附件下载7均PASS。直接堆栈证明新readAttachmentProjection未透传toRespVO已计算的管理资格，对每个文件再次走无参canReadBinary取权限。BDD：Given 分页已经取得当前请求管理资格，When 投影所选版本附件下载能力，Then 复用该资格而保持原附件VIEW/DOWNLOAD独立判断；直接metadata/binary/download请求仍重新取当前资格，不使用跨请求缓存。保留原verify(times1)断言，Root只给附件读取私有helper透传本次值，不改权限规则、主正文策略或测试次数。

- R1624-22（20待生效基线检出SQL漏状态）：恢复同包后原A2/4245页面允许检出，正式一次动作返回“已由本人检出”但无锁/检入入口。Root核对VersionPolicy.isControlledBaseline/hasControlledBodyStatus明确包含CONTROLLED_PENDING_EFFECTIVE，Query.canCheckoutLockedVersion也允许；Mapper.checkoutByIdAndTenantWhenAvailable的原子UPDATE状态集漏该状态，claimed=0又复用已检出文案。只修源SQL白名单与既定资格一致，不假设锁已存在或改日期生效来绕过。20唯一先在现有WorkingSubmissionConditionTest补真实MyBatis/H2允许基线→UPDATE1/原锁回读、二次/已锁/跨租户/删除/待审0的RED；Root执行后才准Mapper单状态最小GREEN，不改表/迁移/Query/版本规则或共用错误码。

- R1624-21（22负责人指派缺后补关联采集入口）：原Main4249/BPM1842903f/当前L任务3481664b，只有指派/意见/密码/签名按钮，无后补选择操作；后端VO、DccWorkflowSignoffAssignmentService同一签名事务已支持supplementalRelatedControlledFileIds。22唯一先补真实组件handler/项目固定版本选择行为RED；优先只DetailSignoffAssignment复用已有selector/slot并入现有请求，不新组件/DTO/API/权限或重建Main。实际RED后Root才准生产GREEN，原上传Rel1及revision整改字段必须保留。

- R1624-19（21已办正文上下文丢失，根因已定位，测试先行）：DccApprovalTaskAdapter的pageDone summary已有原file/历史task/process，但approvalCenterViewerDetailQuery只输出viewer/from。真实HISTORY已办点击因此落DAILY；前端caller保留已有query，没有删除context。21唯一先补现有AdapterTest合法旧A1精确APPROVAL_HISTORY wire行为RED，Root执行后才准Adapter最小GREEN。类型须取历史轮次的正式定义/绑定，不能用当前file.changeType猜作废历史；保留deleted/TODO/身份不足不开放边界。
- R1624-20（运行态异常退出，已找到Windows事件）：22:54发现48061无监听且原PID34440不存在，故20/24登录500及17迟到个人首页500不能算账号/业务权限拒绝。Windows Application Error事件1000记录22:37:51 java.exe、进程0x8688=34440、模块ntdll.dll、异常0xc0000374；不是已证明的业务代码异常或OOM，不改源码/权限猜消错。Root保存22份文件全列摘要、1个在途任务及暂停5627，22:55以同200656/hash264efbd4…和同私有配置恢复PID14864，不加载待验源改动；恢复health/UI及原状态复核后再放行。

R1624-18原流程GREEN：原R1/R2/R3真实签收时间22:26:58/22:27:16/22:27:33，原U独立DOM回读逐次2/1/0待签，三人原义务不互相替代。原signed0失败仍保存；晚于进程退出的个人首页截图明确invalid，不作为主链证据。Root已接受逐次原DOM及规范签收状态，目标表视觉截图仍补齐；组织重叠参数边界保留。

R1624-13后续源码GREEN：Root实际57项全部PASS（13浏览、17真实签收事务、22绑定、5ack消费者），新增电子名单一次读取/撤权行为有效RED已保存；该源码尚未部署，恢复服务仍旧200656，不能把57项代替原global性能UI。Query/Test归属已从18冻结转给23仅历史读测试；历史读真实当前B正控与A行/附件门禁RED完成后才生产GREEN，不并改。

- R1624-18（17签收绑定策略漏交付）：正文已沿原流程GREEN后，R1签收失败，U回读仍0签3待签；22:14 Root只读确认源/副本均缺dcc.distribution.recipient.signature-bind。源码绑定方法真实append及正式YAML:161均已登记，完整ack链仅依次涉及signature receipt创建和该绑定，两者在同一事务；不新增业务动作或改历史签名。BDD：Given 原三接收人/273/4234/A1及未签状态保持，When 第三个精确test-only策略备份、副本首次/重复后应用本机源库，Then 正式canonical一致、原策略逐行摘要保持，仅新增这一配置；原R1随后真实签名与U计数才是业务GREEN。没有放宽签名、禁审计或直接修改收件人状态。

- R1624-13原流程复验限制（21:34）：新200656包下18原storage/global页面仍出现30秒超时，一页同关键词能正常返回原A1，另一页列表失效；请求内权限复用单测GREEN不足以关闭性能缺陷。18保存同一自然请求参数/时序，并在真实UI在途时只读采34440栈定位剩余直接热点；不加timeout、不改页数或权限，原检入/升版继续走可用正式入口。

  21:47直接新证据：global-natural-request-window.json确认13:39:52.679Z发出普通browser-page，仅page1/20、原keyword、latestVersionOnly=true，无项目/文件夹限定；30.009秒取消。在途6秒栈唯一DCC线程落在countActiveElectronicRecipientAccess→canAccessBrowseScope→canAccessQuery候选循环。正式Mapper已存在同tenant/user/medium/status/deleted条件的selectActiveElectronicControlledFileIdsByUserId，故只复用本请求集合，保留ACTIVE与原资格门禁。BDD：Given 多ACTIVE候选、仅精确电子下发授权集合可读，When 一次真实browser-page及下一次撤权后请求，Then 每次只读一次集合、无逐文件count、仅授权行可见且撤权立即生效。18唯一先测试RED，Root实际Maven后才准Query最小GREEN；不改Mapper/SQL、keyword聚合时序或其他端点。
- R1624-17（21旧A1历史正文拒绝，待直接归因）：4244/B1已从4236真实换版并完整签名；VERSION_HISTORY打开原4230/A1显示已作废但拒绝正文，无PDF。21仅追精确actor/原上下文/项目与VIEW资格及状态守卫，对比当前B1、原TASK/HISTORY。先保留现场和BDD/有效RED，禁止扩权或改成当前正文来消错。
- 20原“延迟选择/确认立即”观察已复核（21:34）：原4242稳定加载后单选、revision投影、确认摘要一致为DELAYED，保存重开仍DELAYED，实际提交原链A2/4245。当前证据不支持生产缺陷；Root撤回未经调用链证实的“refreshPanelMatrix会写effectiveMode”线索（该方法实际只更新路线/批准人），不据此改生产。异步初始化旧观察保留，不冒称原原因已确定。
- R1624-16原UI GREEN（21:28）：原R1本人原通知/4234/A1实际PDF画布及唯一正文文字搜索成功、水印含R1；自然GET200 Content-Type application/pdf且必需水印头存在。body-opened配置修复已沿原触发流程验证，不改响应头协议或VIEW。R15三个实际签收及U逐次计数仍继续。

- R1624-01（测试配置，原保存流程GREEN）：19的类别T1保存真实提示 `Controlled file category lifecycle stage is invalid: DCC241016C19L2`。证据为19的 `runs/config-actor-2026-10-18T03-00-45-166Z/result.json`。Root源码核对 `DccFileCategoryAdminServiceImpl.resolveLifecycleStageFromTaxonomyStage`：严格由分类路径第二层语义推导正式生命周期；本例L2名字没有业务语义。仅指派19从正式UI修本例自有L2为策划语义，保持正式校验，不改生产/共享分类。`runs/config-actor-2026-10-18T03-10-13-376Z/result.json`已由Root读取：T1自然页面精确回读已存在并跳过重建，T2真实保存及页面行回读成功，目录913890不变，两者路径第二层为本例策划节点，无原生命周期错误。仅配置恢复GREEN，19目录搜索业务仍待完成。
- R1624-02（过期测试门禁，待脚本纠正）：17/23旧主脚本仍硬要求共享 `roleFixtureVerified=true`，与早已授权的独立本例manifest模式冲突。Root要求只更新本例脚本验证真实自有角色/fixture和当前运行身份，禁止把共享false伪改true；不是产品缺陷，也不是业务PASS。
- R1624-03（测试工具，待续跑）：22/23/24新增DPAPI缓存使用Windows PowerShell的SecureString cmdlet，当前继承模块路径导致 `Microsoft.PowerShell.Security` 加载失败/信任交互，在业务启动前终止。Root要求复用已有纯.NET `System.Security.ProtectedData` CurrentUser实现（stdin、隐藏进程、不回显），不修改机器策略/信任或QMS产品；旧身份无安全凭据时不猜密码，不重置。24已恢复真实角色页面，其余待真实续跑核验；不计业务PASS。
- 范围检查11:32：Root对已跟踪生产脏文件的路径/长度/mtime与本轮冻结基线比对，无新增生产修改。19在Root提醒后停止新增权限菜单，已从正式页面确认可复用review/approve权限；按现有精确权限授予本例角色，不新增同义菜单。16–24仍处账号/配置/业务数据准备及本人核验，全部原业务分支继续，尚无整例PASS。
- R1624-04（并行测试产物风险，已收口）：11:37实际进程核对发现22旧19432/新37296均运行同一个fixture-ui.cjs，23旧46860/新31636均运行同一个ui-session.cjs。Root要求精确核实旧handle/浏览器用途并收尾弃用runner，禁止仅因观察超时再启动或覆盖同一manifest。22通过原handle正常finish旧浏览器/trace，23确认旧handle失效且无浏览器子进程后精确结束旧进程；Root11:53实际核对仅22 PID37296、23 PID31636各一个。没有据此判定业务动作失败或强停活流程。
- R1624-05（19测试资格/项目配置，原流程待复验）：首份PDF选择后未见预览；原页面进一步给出上传人缺岗位及P1缺正式OWNER规则。历史申请负责人文本不等于正式权限；只补本例实际岗位和项目授权，保留拒绝现场，不放宽服务端校验、不重复上传。证据在19同一resume run的result.json及checkpoint，Root12:22读回已定位本例精确角色菜单前置，后续真实上传仍待执行。普通shell命令同时观察到40–77秒启动/调度延迟，两种profile探针均有延迟，尚无根因结论，不把环境猜测写成产品缺陷或已修复。
- R1624-06（真实格式入口缺口，未修复）：24实际选择PNG正文被“仅Office/PDF/图纸源件”拒绝。Root直接读取submitter.ts及DccControlledFileUploadTypePolicy，两端同样窄白名单，Workflow提交也使用后者；与已存在的IMAGE/TEXT/VIDEO/AUDIO正文渲染能力未接通。直接下游Finalization对非PDF统一OnlyOffice转换，并强制要求盖章产物，故不能只扩白名单、伪造盖章身份或把媒体伪转PDF。已指派24保留同一UI各格式拒绝、补最小真实模块RED设计，生产暂冻结；Root继续决定可完整验证的最小修复。PDF/Office业务继续，不将未支持参数计PASS。
- R1624-05归因纠正（13:45只读实证）：Root此前把Config列表不可见推断为旧P1/P2缺正式规则，证据不足，该推断撤回。实际286/287及新306/307均已有正确USER OWNER=910357（规则106/107/149/150，启用、未删除、无限期）；新请求43/44负责人910357且COMPLETED。运行Jar内AccessServiceImpl及CreateWriteService均包含初始化方法/调用，且字节与当前target一致。不得再重建项目或写规则来处理该拒绝；继续核对实际登录身份、请求projectId及拒绝读取来源。此前新fixture保留，不把它或旧规则宣称为本轮修复结果。
- R1624-07（19拒绝原因误报，已修复并经原页面验证）：同轮13:27:32后端实际栈位于UploadServiceImpl.validatePreviewCategory:279的类别UPLOAD拒绝；该分支复用DCC_PROJECT_ACCESS_DENIED（1080000341，文案为Current user is not the DCC project owner），导致正确OWNER被误报。类别UPLOAD需本例正式UI核对/补齐；诊断只修这一明确拒绝点，保留权限门禁和原拒绝码，避免扩展组织/项目管理流程。

BDD R1624-07：Given 用户类别UPLOAD无授权且项目OWNER与该条件独立；When 实际uploadPreviewFile校验该类别；Then 在任何文件/票据保存前拒绝，明确提示该类别上传权限不足，不声称OWNER缺失。已有允许类别/真实PDF成功与项目实际拒绝路径保持。19为UploadServiceImpl及UploadApiTest唯一写者：先增加真实服务行为RED，Root运行定向Maven；有效RED后再批准一处最小诊断改动，随后GREEN和原UI资格补齐复验。

R1624-07 RED：Root实际Maven `DccControlledFileUploadApiTest#deniedCategorySourceUploadReportsCategoryPermissionBeforeCreatingResources`，exit1，1test/1failure/0error/0skip。独立保存的 `e2e19-category-upload-diagnostic-red-surefire.xml` 证实唯一失败为具体类别权限消息与旧OWNER消息不等，原拒绝码、零源流读取和零文件/票据调用均通过。已批准19一处局部消息GREEN，待回归。真实UI原因消除另已见证：19补本例T1/T2作者UPLOAD并回读后，同项目306、同原PDF预览HTTP200且页面显示文件，不改OWNER、不代表整例19通过。

R1624-07 GREEN/REGRESSION：整个DccControlledFileUploadApiTest实际36test/0failure/0error/0skip（14:51–14:53，exit0），独立green-surefire.xml已保存。生产仅UploadService第279行局部拒绝消息，原code及false条件不变。Root正在构建候选运行包并保留前序修复class字节核对，暂未重启/宣称运行态新消息；所有浏览器到安全checkpoint并关闭后才统一更新48061。

R1624-07运行态15:39已更新：全部9例runner及旧C20 Chrome31004实际退出后，仅停止已核归属旧18204/48061，启动25996/48061，Jar151101/SHA bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f。完整构建、前序关键类及本次UploadService字节、health UP、fresh正式UI登录均PASS；16/19原file/BPM/task/status/办理人重启后不变。依据category-upload-message-runtime-proof.json，已恢复原9线程、model gpt-6.1-sol/high。原消息负例页面复验委派19精确自有UPLOAD规则停用/原U拒绝/完整恢复，未恢复前不得继续主业务，不以类字节/登录替代该负例UI结论。

- R1624-10（24 Office上传超时，未证明根因或修复）：DOCX正式upload-preview客户端30秒超时，尚未进入OnlyOffice iframe；只读发现infra9198354931816/temp546已实际保存，uploader910473，AVAILABLE但已过15:05:13有效期，bound file NULL、cleanup ACTIVE，不能声称零保存或已清理。15:09及15:30运行线程无未完成DCC上传/业务请求。保留物理/票据残留不写库，恢复后本人新会话沿同DOCX原UI复验并取自然请求时序；此次服务重启仅部署诊断修复，不宣称解决Office超时。

R1624-07原UI闭环Root16:06已复核：19的category-message-2026-10-18T07-49-40-282Z结果包含原/停用/恢复三份精确规则DOM，作者独立新context，POST upload-preview自然400，visiblePreviewError为“当前用户没有该文件类别的上传权限”，submitRequests=[]；原规则全部字段恢复一致、启用true，两个脱敏trace保留。Root独立打开03-specific-category-denial-no-owner-message.png核对页面实际类别错误。该诊断修复已关闭，19完整目录/搜索业务仍未关闭。24同DOCX新轮自然HTTP200约12.6秒并已见两页，但后续脚本旧mount被iframe替换的定位问题单列；原30秒失败原因仍未证明，不能归为本次部署修复。

- R1624-11（签名后相邻阅读关闭请求，尚不归为详情刷新缺陷）：16的run-20261018-151101/artifacts/S-sign-adjacent-network-order.json记录S签名成功后reading-session/stop、待办查询，无旧详情GET；返回待办仍出现bodyDenied消息。后端stop目前复用当前正文授权，任务完成后原TASK能力终止；是否需要独立的阅读结束授权/计时收口仍待针对性设计，未放宽guard、未吞错误。原主管/指派/会签继续，辅助诊断不阻塞主链。
- R1624-12（21自定义换版属性投影丢失，待服务RED）：ACTIVE4230/master2054545668044079202，WORKING4236，原working GET能显示已保存APPLICATION_CUSTOM，选REPLACEMENT/B1后专用replacement GET丢五custom字段而提示矩阵基线缺失。21的replacement-custom-matrix-bdd.md及前后DOM证据定位Workflow.readReplacementApplicationAttributes短构造，与正常working的完整canonical读不同。Root只分配21对应Workflow投影和最贴近测试；不改前端旧snapshot兜底、Query持久化、公共矩阵或候选重绑。BDD：同一合法已保存custom工作稿两种属性读取均保存精确来源/轮次/上下文/人选，读取不写源；无合法来源仍拒绝，现有身份/正式B1重绑继续回归。先一个真实服务行为RED再最小GREEN。
- R1624-13（浏览列表目录授权重复读取，待性能行为RED）：Root17:07实际Thread.print三条浏览请求栈均为listControlledFileBrowserCandidates→canAccessQuery→hasAuthorizedDirectoryAccess→getAuthorizedDirectoryIds→matchesSubject，源码每候选重复读同用户全量目录规则及主体集合。18在培训后浏览30秒超时，Root只读4232证实已ACTIVE且培训附件9198354931846已绑定，禁止重绑。只分配18最贴近浏览测试：实际browser page多候选同accessType至多读一次、可见/拒绝结果不变，第二请求重新读支持撤权。候选生产仅QueryService浏览请求局部懒加载EnumMap，旧其他路径不跨请求缓存、不改权限/SQL/超时/DB池；测试RED前生产冻结。

17:51定向执行结果：e2e18-21-browser-and-replacement-red实际2test/2failure/0error。18测试已先通过3个可见文件及无预览/下载越权结果，再因QUERY wanted1/actual4失败，构成有效性能RED；已批准18仅局部请求内懒加载缓存GREEN。21首失败在working GET可选null字段的JSON序列化比较，尚未到目标replacement，属测试断言形态问题，不能批准改生产；已要求按完整DTO领域值保留所有原字段断言后重跑。两份原XML分别保存为e2e18-directory-permission-red-surefire.xml与e2e21-projection-preflight-failure-surefire.xml，不抹除、不将编写或假失败称作产品RED。

18:15第二轮实际结果：e2e18-green-21-projection-red。18定向1test/0failure/0error，3个正确可见结果、无正文授权越权、两种访问集合各读一次及第二独立请求撤权全部通过；未部署。21修正序列化断言后1test/1failure/0error，精确5个custom投影字段为null，其他保存值及源/快照无写断言通过；此为有效产品RED，已批准仅replacement分支完整读取已保存custom来源。原XML分别独立保存green与replacement-custom-red；原对象4230/4236不动，鉴权不经普通requester GET替代。

18:50邻近回归实际101项：Browser12/0failure/0error，CustomRejected42/0failure/0error；SelectedWorking47中45通过/2error，均旧submit请求缺必填附件manifest，失败位置在附件安全门禁，与projection GET无关。原XML三类分别保存，未将整轮写成PASS；21只修两条测试请求复用现有a29ExpectedServerManifest，并允许父helper仅private→protected、方法体/端口/cleanup不变，生产附件检查保持，之后重跑47。

- R1624-14（19当前项目全文件夹搜索入口缺失）：原23参数21PASS/2BLOCKED，选P1未选单文件夹时名称/编号搜索均提示先选文件夹、browser请求0。现有backend GLOBAL允许精确projectId且mapper有项目等值过滤；缺口在前端State/UI/adapter限制。只分配19三个前端文件及实际State/adapter行为RED，保留同项目不同folder结果、跨项目拒绝及原全局/文件夹语义；未授权改backend/权限/SQL或只用旧通过项替代。
- R1624-15（17三人签收缺正式策略交付）：原R1/R2/R3真实签收均被signature.dcc.distribution-receipt.create策略缺失拒绝，0签/3待签。Root只读证实正式gxp-audit-policy.yaml已有integration-02精确项，原库与专用副本均0行，既有迁移未交付该新增项。BDD：仅test租户1精确补正式声明，保留OPTIONAL_TEXT/REQUIRED签名/PRESENT_TO_PRESENT，备份原策略全行、迁移依赖gate、副本首次/重复后才原库首次/重复，原行摘要不变；不签收、不改业务/历史审计/签名。Root新增精确test-only迁移及任务脚本复用既有备份/副本原语，源头修复后由原三接收人原UI逐签复验，不能直接改数据库收件状态。
- R1624-16（17下发正文缺水印头，已确认直接审计配置缺口）：原distribution273/recipient276/A1预览在返回binary前调用真实body-opened审计；正式YAML已登记，2026-10-18 20:55 Root只读确认源库/专用副本均无该operation行。成功写头分支共用，现有缺策略事务回归要求回滚readAt，禁止补假头或放宽审计。BDD：Given 原R1/文件4234/A1及权限不变且正式策略缺行，When 备份后将精确test-only策略在副本首次/重复验证再应用源库，Then 原策略逐行摘要保持、只新增该配置、不写业务/旧审计/签名，原通知须真实渲染正文后才能判GREEN。复用已审查修复原语，仅扩展一个固定body-opened入口；原UI复验未完成前不关闭问题。

- R1624-08（17测试前提与正式组织模型冲突，参数仍未验收）：`../20261016-dcc-e2e-17/business-issues.json` 的C17-ORG-OVERLAP记录UserForm单值deptId、AdminUserApi/Mapper只查直接部门成员、FirstEffectiveDistributionRosterSource按唯一直接部门取人且拒重复身份；父子部门同时选择也不产生重叠。Root禁止为构造该参数扩展员工多部门模型/复制身份/伪造名册，原参数独立保留未验收，其余全员/范围外/逐人签名/时序分支继续。
- R1624-09（16提交后导航/发现链路，未证明产品失败）：16提交等待超时、“我发起的”未找到及再次选择源件同名占用，不能据此断言未建档。Root只读实际file表已确认2054545668044084228，DCC241016C16DELAYED18/A1，project308/category908765/requester910476，14:17:45已建，PENDING_SUPERVISOR_REVIEW，BPM bc599afa-cabb-11f1-8473-b082e25ec548。已禁止重提/换名并通知S从原正式待办继续；原导航失败证据保留，不用DB执行审批。
