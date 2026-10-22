# HTML v1.6 最终需求差异矩阵（G67有限审查）

状态：read_only_review_complete，2026-10-06。唯一业务标准为`docs/product/dcc-final-requirements.html`的12流程及27AC；本稿只新增文档，不构建、测试、Git、UI、DB或生产编辑。审核时G67通知FE资产已冻结，G67关联提交快照BE尚在开发。**主链已真实跑通，但新换版申请漏冻结当前关联是必须修复的主流程缺陷；关联整改收件和混合日期提醒还未取得最终页面证明。**源码、定向测试、真实页面分别记录，历史待验记录由最新证据覆盖，不累计各轮重复测试数量。

路径简称：F=`IntRuoyiFronted/src/views/dcc/controlled-file/`；R=`doc/tasks/20261001-dcc-integration-unblock/`；B=`doc/tasks/20261002-dcc-public-browser/`；J=`doc/tasks/20261002-dcc-public-backend-completion/`。证据具体操作与当前指纹详见B/g66-frontend-completion-proof-audit.md、G63/G64r2/G67 manifests和R主verification-report。旧G66稿的属性/名称待验项已被R/g66-project-attributes-real-progress.json、g64-name-occupancy-final-ui-proof.json覆盖。

## 12条流程

| 流程 | 源码与正式入口 | 当前定向证明 | 当前真实页面 / 差异 |
|---|---|---|---|
| 01 上传 | F/upload:index→逻辑folder、启用type、实际原名、属性、关联、培训及二确认；submitter冻结copy/ticket。 | 属性payload、无模板type/唯一map、NEW省略NAS、取消/late/错误helper；G64r2 17项及精确name码。 | G55项目271/folder2/type13新原件、CE→FDA、取消留输入、两确认、六签名受控下发；G56非空关联+training。G64跨project272原作废名真实拒且正确20年提示。满足主线。 |
| 02 浏览 | F/browser ProjectBrowserPanel目录/global search/版本范围；view正文门禁，detail只读档案history、项目与产品分别来自正式字段。 | selector实际目录/Long/late；viewer与metadata实际renderer/handler禁止只读写；G63回归含readonly。 | G55真实PDF canvas，G56目录/搜索/history、引用只读trace；独立Doc4033无SOURCE权限不能预览。已证阳性与部分权限边界，不外推所有搜索负向排列。 |
| 03 项目产品 | ProductCatalogTabPanel创建→configured reviewer→批准，后台ProjectReviewerConfiguration/属性模板；BE同事务初始化正式负责人USER/OWNER。 | 审核配置当前13XML、OWNER初始化/回滚/COMPLETED保护及正式productidentity tests；无缺值admin猜派。 | request7完成271/614；8审核拒→9、9批准拒→10完成272/615。更改review配置后旧申请冻结身份为定向，未独立UI全验。 |
| 04 文件夹 | FolderTemplateLibraryEditor直接权限保存；ProjectBrowserPanel复用ProjectFolderEditor维护真实逻辑folder，非空删除后端保护。 | template权限/结构/历史保护及publicfolder actualhandler/取消late、无NAS代理。 | G56 folder3创建/改名/软删历史保；模板各权限/非空删除排列为定向，未全UI。无审批节点新增。 |
| 05 文件类型 | file-type-taxonomy管理正式CRUD/启用及唯一category映射，上传独立选择启用类型完整路径。 | G47 type SFC renderer/非法map不取first、使用中配置历史保护。 | type13独立上传真实成功；停用/删除已使用类型的完整UI负向尚未执行，源码未确认缺陷。 |
| 06 审批矩阵 | categories审阅矩阵→CategoryMatrixDialog；按NEW/REVISION/OBSOLETE正式route保存执行名单，CategoryPermissionRulesTab不无签名改REVIEW/APPROVE。 | G34矩阵同锁/回滚及删改不跨动作停用；actualroutepreflight blocker展示阶段/人；G58独立TRAINING_RECORD。 | 正式native三类型路由已实际办理；全CRUD/配置变更冻结/缺负责人排列未全UI。不借其它action配置。 |
| 07 关联文件 | ProjectBrowserPanel关联→ProjectFileRelationsDialog，detail当前latest与审批snapshot分开。 | current boolfalse0GET/true严metadata、相关target精确版本；G64 CONTROL事件16XML。 | current4032 A2待生效 vs frozen4026 A1真实；**新B1申请漏带当前4028关联，见R01。** |
| 08 关联窗口 | DccFileSelector顶部source、左project/folder、右global，同源tenant/Long校验，确认才存。 | 实际SFC跨项目同名folder定位/搜索/late；upload source变/外tenant拒。 | G56非空选择与冻结真实；窗口接线满足，后续新申请冻结环节R01未关闭。 |
| 09 审批 | detail currentfile+BPM/task/assignee/review→原指派child与独立签名；批准选owner；培训仅线下记录；文控审核后受控，下发独立；作废两节点。 | G63 23+类型检查；G64 native拒绝120原XML；G61独立training资格及readonly；G67通知17+全types/build。 | 独立Doc通知2088/4033队列1→record→0，受控待生效；三拒4035/37/39和第四4041受控下发；独立作废424/425/426结束。**REV整改选择受R01阻；G67收件新按钮尚未真点击。** |
| 10 检出/检入/升版 | 正常storage下拉选actualversion→检出/本人检入；baseline详情PARTIAL/REPLACEMENT使用server target和所选body；实际变更事实/attempt/BPM历史。 | 锁/来源ticket/MINOR/错误receipt、版本策略进位与target复用、savedbody属性与source；BE同事务控制。 | 两次A1小版、较早A1-1→A2及未来生效；三次A3失败→第四同A3成功。换版4043 B1已送审，**R01关联快照空**，尚未受控全链。A9进位真实UI/跨人抢锁仍待。G65拒选ACTIVE为正确guard，非缺陷。 |
| 11 引用 | 目标projectleader→selector reference→正式reason/save，橙色class+文字+项目去重数；不自动grantedit。 | 原13真实HTTP/H2 leader变更、非leaderadmin拒/锁/回滚/obsolete追溯；referenceusage精确page身份。 | target272/folder4真实0→1；class/render橙色依据，不虚报截图像素色。多个不同leader正负UI未全验。引用跟随最新未确认。 |
| 12 取消引用 | 同targetleader/上下文→确认dialog→exactproject/folder/master/reference取消，source/history/其他关系不删。 | 同13XML lastprojectcount、foreign/非leader、auditrollback，取消0写。 | 二确认实际1→0/入口移除/sourceclass恢复；多文件夹/多人竞争UI未全验。 |

## 27条验收

`源满足`表示核到正式实现，不含正在开发的修复；`定向`指已有当前原证据，无本轮重跑。`页面部分`列剩余明确边界，不能当全项PASS。

| AC | 源码 | 当前定向证明 | 真实页面结论 / 尚未证实 |
|---|---|---|---|
| 01 配置审核人/创建/OWNER | 满足 | configured snapshot、OWNER同事务/失败/COMPLETED，G66 reviewer13 | request7/10完成，formal项目目录可用；配置换人冻结及初始化失败UI仅定向。 |
| 02 上传actual不改default | 满足 | frozen actualcopy/defaultSource，SFC CE/FDA | G55默认CE、本次FDA及会签/批准真实，项目默认保持。 |
| 03 新升版/作废默认不同于原文件 | 满足 | 新controlled工作稿初始化currentdefault；saved/返工copy原申请；30属性XML | G66改OTHER/Y后新upload/作废dialog读OTHER，旧history/workingCE保；新revision已有草稿CE正确，不当缺陷。新controlled首工作稿读取改后default的完整UI单独排列仍未全验。 |
| 04 default改变不覆draft/inflight/history | 满足 | reserved draft、snapshot freeze、inflight/history实际服务30XML | G66 4042旧saved CE、4041history CE保、新uploadOTHER，恢复CE；正在运行流程随配置变化专项UI为定向边界。 |
| 05 OTHER/N/A/转移条件 | 满足 | state/Fields真实互斥/条件/长度，30属性XML | r12/297仅NA、302OTHER缺/306转移缺均0PUT、308合法OTHER+Y、r13/022恢复；已有实际条件证明。 |
| 06 上传关联/培训/两确认 | 满足 | actualSFC cancel留值/同snapshot+文件ticket、新关联冻结 | G55cancel+二确认；G56关联+training正常送审。 |
| 07 指派本人及其他会签人 | 满足 | actualtask/department/canAssign+原指派/签名分离 | 本人已实测；真正另签名人指派/签名正常UI待，非源码缺陷。 |
| 08 三节点reject/文控无签名 | 满足 | native CAS/actionkey严格/回滚，120XML；FE password+reason | 4035会签拒、4037批准拒、4039文控拒，r12/165空密码0写；旧版保，各新轮重会签。 |
| 09 培训与非培训主顺序 | 满足 | training receiveTask/独立TRAINING_RECORD、正式身份/记录推进 | G55无培训/G56培训上传；G61独立Doc线下记录无SOURCE权→审核；G64 revision无training完整。升版选training组合未单独UI全验。 |
| 10 一人锁、别人看到、本人检入 | 满足 | Master锁/同源guard/actor投影/错人拒/MINOR | 正常已有working/failed版本检入、释放锁实际；两独立账号合法竞争尚待。 |
| 11 局部/A9进位/换版 | 满足 | formal policy/selectedbody/intent，不本地算target | PARTIAL A2/A3真实，B1 REPLACEMENT已确认送审；A9→B1与B1受控后完整UI待，R01影响当前B1。 |
| 12 控制成功/失败、未来生效 | 满足 | G64control16含late外层rollback、pending/shared/最新/旧执行 | A2提前受控oldA1 ACTIVE；自然上海零点A2 ACTIVE/A1 OBSOLETE；受控失败无通知由事务证明，未破坏实库故意失败。 |
| 13 所选关联整改/受控才通知 | **真实缺陷R01未关** | control16只证既有已冻结关系情景，不能覆盖后来改current而旧working空的提交；G67 BE有效RED开发中 | 新4043 B1真实指派整改列表空，无法选4028；受控后本人实际消息及期限待R01修后复验。 |
| 14 当前latest vs冻结旧审阅 | 读取满足；新提交冻结R01 | current/history分离/rowVersion/controlled事件 | currentA2待生效/frozenA1真实。下一申请应冻结当时current关系，当前R01需修，旧history不改。 |
| 15 目录/搜索/history/权限正文 | 满足 | actualselector/trace/readonly/独立binarygate | 非空canvas、搜索/历史/只读trace已实；无SOURCE权Doc只看待办，未grant正文。完整多身份搜索排列仅定向。 |
| 16 仅目标leader引用/橙色/去重 | 满足 | NameRef13原XML实际leader/非leaderadmin/同tenant | 目标272引用0→1/class+文字实际；其他leader/成员UI未全验。 |
| 17 leader取消/count/source恢复 | 满足 | lastproject/targetedcancel/rollback/关系保，原13XML | 真二确认1→0/入口移除/源class恢复，多目录/账号排列未全验。 |
| 18 template直权限、类型/非空保护 | 满足 | template/usedtype/folderblock、publicfolderhandlers | folder3真实CRUD；模板各权限/usedtype非空删除完整UI未全验。 |
| 19 下发/接收/纸件 | 已确认下发满足；纸职责未定 | formal日期/record/recipient/二确认、不改生效日期 | 4026/4028/4041电子下发真实；已下发消提醒。纸件/逐人签收不冒实测，D06未定。 |
| 20 选择器作废两节点/独立属性 | 满足 | pendingcontrolled资格/原BPM事实/签名/共享事务 | 4033正常selector操作→原todo指派424/会签425/批准426完成OBSOLETE；原training/history保。入口当pending、批准时已自然ACTIVE，pending全效果为定向不冒UI。 |
| 21 完全同名及case/extensions | 满足 | NameClaim binary比较/同源重放、名称74XML | 原作废名跨project272真实拒且r12/268正确提示；sop/SOP/后缀三变体成功仅定向。 |
| 22 实际变更类型所有版本 | 满足 | stored intent/source/body、historical own row及policy | A2/A3 PARTIAL实际；4043 B1 REPLACEMENT申请已建立，受控后history以及进位B1局部区别尚待完整UI。 |
| 23 受控/工作/在途/历史术语 | 满足已核链 | G58display20、G62completion32、G63readonly/active scopes | 工作A1-1不当受控/历史、A2pending、原生旧版OBSOLETE完成卡真实；非全仓文字扫到即结论。 |
| 24 作废20年占原编号/名称 | 满足 | exactMaster自链例外、保留date/name/number原XML | 4033保至2046-10-06、原名跨project实际拒；另一NEW用原编号单独UI未全验。 |
| 25 预设晚于control、到期原子切换 | 满足 | native/shared pending→active及低旧控obsolete、失败rollback | A2 control10/5/effective10/6分别，自然分钟job切换，签名与original dates保；A3当日受控/生效同时间真实。 |
| 26 Shanghai/7天/混合排序/已处置 | 满足 | datepolicy inclusive+7、server日期/id排序，FE坏序/FUTURE拒/错误null | 分钟5625自然激活及次分钟0真实；B1 10/13边界/混合未下发列表及办理后移除的完整UI仍待。 |
| 27 连续失败同目标与独立历史 | 满足 | exactretry policy及50真实H2案例、history ID/type/BPM严匹配 | A3三失败4035/4037/4039→第四4041 A3 attempt4成功/下发，427–440各签名/BPM独立，旧A2在新生效前执行。A2只是HTML例子，不另设编号限制。 |

## 必须修复的具体主流程差异

**R01（已真实复现，修复中）**：先有saved working4042，再通过当前关联窗口把4041的current relation保存为4028，随后选4042提交REPLACEMENT→4043。current关系非空，但4042/4043审批关系表空，普通会签指派“整改关联”无数据。复现R/g67-replacement-relation-snapshot-real-failure.json（r14/012→028→035）。源码锚点为`DccControlledFileRevisionServiceImpl`新候选阶段`relatedFileService.inheritRelatedFiles(selected.getId(), target.getId())`，`DccControlledFileRelatedFileServiceImpl`的history继承入口；FE`DetailSignoffAssignment.vue`确实读该新candidate历史，所以不能靠前端塞当前列表或SQL补历史。最小修复是**每次新正式REVISION候选按同事务、正式latestbaseline/currentset身份冻结当时current关系**，空/非空都准确；原selectedbody/default/actual及submitted history不覆，同key重放不刷新旧snapshot。J/g67-current-relations-freeze-bdd.md已有有效RED，当前未见finalfreeze/包/新UI收据，不能记已修。验收须从相同旧working自然新提交，能指派所选4028与期限，控制前0任务/通知、控制后只所选本人收到一次消息，旧历史仍空。

**N01（源码已修，非当前开放源码缺陷）**：原整改通知没有安全可点来源入口，G67 FE已用正式templateCode+Longtuple+deadline/sameorigin精确URL接原消息详情；17定向、lint、全types75657/build84592均0，2生产资产冻结。必须在R01解决、B1受控生成实际消息后由本人正常详情点“查看来源受控申请”，核source/relatedMaster/期限和原binary权限；不把原16XML或消息API诊断当页面点击通过。

**其余是有限补证，不直接认定源缺陷**：第二合法签名人/检出竞争（AC07/10）；A9进位及本轮B1受控后变更事实（AC11/22）；混合日期/7天临期/处置后移除（AC26）；配置换reviewer/inflight属性等前述细分。不要通过扩admin/权限、改时钟/参数/业务DB补写代验。Root当前已在安排这些实际路径，遇新失败再收窄开发。

## 证据时效及不属于业务缺陷的事项

### Root 后续真实复验更新（G68/G69）

G67 R01 已有3生产文件修复、55项定向回归和实际包来源复核；G67通知入口有17项/完整types/build证明，尚待新B/1真实受控后本人消息点击。不能把这些源码证明当作新关联整改真实闭环。

G68 真实撤回样本4043发现正常检出被拒、旧重提会跳目标。已最小接通真实原生取消历史/精确锁/新正文返工链，并隐藏旧动作；14项后端、16项前端定向回归通过。本机新包实际页面 r15/205 明确原目标和保历史提示、旧动作消失；r15/211显示本人检出4043，锁30真实ACTIVE。原撤回记录仍在。

G69 当前唯一该修正链已复现的新接线缺口：r15/214正常上传预览 HTTP400，服务端 Workflow.validateSourceUploadContext 的 CHECKIN 状态校验遗漏已验证的原生 WITHDRAWN；没有发生检入。r15/217准确提示上传失败并阻止提交，不属于检入事务失败。后端正对正式multipart/context做严格RED/GREEN，保G68取消历史与本人锁资格；Root后续复验需先显示上传成功，再检入与同目标重提。此更新替代上文关于仅R01开放的早期结论，不删除原阶段证据。

独立签名人910328的旧培训角色配置带项目修正强制文件范围，不能把该身份的普通详情拒绝当作检出锁PASS。Root以正常任务自有角色/部门准备本阶段编制会签身份；查看矩阵和OWNER两个配置已交另一个 agent 唯一通过真实页面处理。旧独立培训证据仍属当时配置，本阶段资格及锁/会签另验，不放宽生产权限或授scope:all。

R/g64-three-rejection-rounds-real-proof.json + fourth/control + final/distribution已覆盖旧g61 AC08/27 NOT_RUN；R/g66-project-attributes-real-progress.json覆盖旧g66属性待验；R/g64-name-occupancy-final-ui-proof.json覆盖G64 helper `NEXT_AFTER_RUNTIME_RESTART`。G64原HTTP400只能诊断，页面正确占用提示才是完成证明；G65已有working拒绝检出ACTIVE是正确保护、并无ACTIVE锁，不再误记actor丢失。旧G54/G55空ID/项目显示/readonly/训练入口均已后继修复，不重开。正式cleanup工具缺失、曾经未质量批准的本机开发前置、旧worktree/git阶段记录是收尾/历史，不是HTML业务P1；开发质量前置已按用户撤销，不能重新阻塞。

来源审查边界：当前G63 Parent与G64 submitter、G58browser已与原manifest一致；G67通知FE2source/test及types/build在B/g67-relation-remediation-notification-fingerprints.json按精确SHA封存。G66属性30、审核13，G64拒绝120、名称引用74、CONTROL通知16的原XML/来源收据保留，不相加独立用例；原隔离signature port不是实际密码证据，真实Root签名另证。J/g67关联代码开发中的变化没有被旧包或旧测试自动覆盖。本审查未运行任何验证操作。

HTML D08期限单位/冲突协调/超期后续、D09 Z之后、D10引用跟最新、D06纸件办理与独立下发签名及20年期满处置仍带未定标签，不纳新平台或新审批完成条件；已确认关联始终最新、目标leader引用/取消、20年内占用、新版生效旧版作废不回退。主任务尚不能据本稿标全业务完成：先关闭R01并补本轮通知/提醒真实闭环，再由Root整体收尾。


主管理最终判定G73：上文最后开放G72已于8项后端/37项前端及types验证、同源包复核后关闭。真实普通910328从正常详情/选版浏览检出31，另一合法账号admin看本人持锁且检出/检入/撤销入口不可用；本人撤销后恢复检出，任务链ACTIVE锁0。当前没有已确认开放代码差异。完整12流程/27AC结论和有限E2E边界以 doc/tasks/20261001-dcc-integration-unblock/g73-final-html-business-alignment.md 及 g72-two-actor-checkout-real-proof.json 为准；不是把早期in_progress删除或冒充旧未验已验。
