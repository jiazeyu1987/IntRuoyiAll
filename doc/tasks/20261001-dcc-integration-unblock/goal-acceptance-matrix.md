# DCC 持续目标验收矩阵

2026-10-03。以需求HTML v1.6、共同合同和最新用户确认为准。下表是主管理逐项验收面，不把代码存在、旧报告标题、离线绿测试或模块 ready_for_closeout 当作完整目标完成。原四 worktree 已收集交付来源；后续代码在既有整合树。以下路径均从仓库根目录解析。


## G44 当前实际状态（2026-10-04，覆盖历史pending说明）

最新需求v1.6共27AC，源码SHA2b854c484b5374ef3ef23680d8a3fa998fcee0eec1ad0ea4d39b558ad3f3f9ad。已确认项目批准创建同事务仅授选定启用leader一条USER/OWNER。G39源库与隔离19升级／新增3表首重／原7历史保护、3缺失原件恢复后39GET全MATCH；G43开发质量前置已取消，实际26审计配置first26repeat0／qualityversion0；历史名称登记25claims-39evidence-13name reservations与独立全部字段hash旧7保持，真实Gxp当前账号审计身份匹配。上述不再是pending/QAblocker；维护parenttimeout1不冒CLI0，独立实际commitproof保留。

当前同源最新Jar a574f1e945b8bf78ed13dad69baafc20ee886ddc11551aa861164dc9932feabb在registeredslot6后端PID12928/48067healthUP，前端42328/8067在线，QuartzautoJobs off。业务27AC尚未完成实际闭环：真实UI已登录、进入DCC产品目录、新建申请窗口，配置项目审核人成功且再次新建显示真实审核人；任务标识文件夹模板通过UI新增并在模板库显示启用／未使用。当前正在UI填写任务项目申请，尚无项目审核／批准、文件上传／电子签名／受控下发或版本/关联/引用/作废PASS。旧下表“正在接线”等文字是历史工程阶段，请引用最新G35/G39/G43独立Review与当前真实UI收据，不据此重开已闭软件问题。

下一主流程：真实页面完成项目申请审核批准和OWNER/folder→项目文件模板/真实类型路线及账号签名前置→上传/会签/批准owner/培训双链/文控受控/下发→版本重提与关联引用作废→真实分钟生效任务及剩余必要负向/详情→最终测试/本地提交合入int_qms/合入后验证。不要求再次质量批准，不扩正式WORM/上线任务，其他签名者真实资料及职责不伪造。完整Goal保持in_progress。

## 证据层级

- **当前代码检查**：核对实际调用、身份和权限边界，不能证明运行库或真实页面可用。
- **离线验证**：已执行有效单元/隔离H2/Flowable、真实转译SFC行为、类型和构建；只能覆盖测试确实走到的范围。
- **真实页面验收**：Playwright登录AGENTS测试环境，使用带主任务标识的自有对象，所有被验收业务操作必须在页面完成。每步以页面DOM/结果确认；自然网络仅辅助，不用API或DB代办。
- **合入证据**：本地提交、来源祖先、int_qms目标提交、合入后检查。无此证据不能宣告已融合。

## 业务验收清单

| ID | 已确认需求及实际操作 | 必查代码/离线验证 | 真实页面与最终判据 | 当前缺口/进度 |
|---|---|---|---|---|
| U01 | 上传页选项目→逻辑目录→类型→部门→文件→日期/编号/初始版→关联→培训选择→二次确认 | FE upload/index.vue、submitter.ts、project-attributes；BE public placement、Workflow；真实A创建/B位置/GXP回滚、上传父handler测试 | 自有项目及目录、实际文件、全部确认汇总；取消保输入零申请；确认进入会签；目录登记与源文件对应 | G04部门接线已29前端/206后端定向通过；上传Agent复核完整确认/批准人；尚无真实UI |
| U02 | 项目3组默认属性带入，允许修改；默认来源和actual分别保存 | ProjectAttributes/state、B snapshot与round mapping、WorkingDraftInitializer；SelectedIterationDatabase组合 | 项目CE默认、上传actual FDA、后改项目NMPA；草稿/在途/历史仍显示当时CE/FDA；恢复原草稿默认明确 | 保存/返工组合已交付；详情选工作正文来源缺口正在修复 |
| U03 | 不同项目禁止完整文件名大小写完全一致的新上传；大小写/扩展名不同允许 | NameClaimService、binary source_name_key、RevisionExactNameTest、实际ticket sourceOriginalFileName | 两项目提交SOP.pdf第二次拒绝；sop.pdf/SOP.PDF/SOP.docx独立；不得用模板名/受控PDF判重 | Multipart/preflight竞态及exact binary测试已过；实际库25历史claim需原完整名核验，否则新上传failClosed；19迁移不回填，真实页面待验 |
| B01 | 项目目录选项目/逻辑文件夹及全局搜索→浏览，名称权限与正文权限分离 | ProjectBrowserScopeMapper/Query、selector独立合同、ProjectBrowserPanel、browserScope wrapper/SFC | 最新受控默认展示；可按状态/版本方式找工作稿、审批中和历史；空目录保留；所点版本和正文权限准确 | G07默认/全版本/真实status接线已交付；G08 latest历史作废指针SQL修复274回归已核验；类型最终组合和真实UI待验 |
| B02 | 浏览/详情显示版本、3组属性、受控/预设生效/实际生效/下发、签核、关联及引用 | DetailBasic/Facts/Evidence/RevisionHistory；准确BPM/Long | 所看文件/版本、实际变更类型、独立失败尝试及前驱/BPM均可追踪；无正文权限可见名不可读正文 | 详情Agent接尝试/BPM/历史申请；整页真实验证待做 |
| P01 | 新建项目及产品→名称/代码/负责人账号/产品/分类/备注/模板/3属性→核对→提交→审核→批准 | ProductCatalogTabPanel、ProjectProductCreateService、ReviewerConfig/快照/统一账本 | 页面配置正式审核账号、新建/重提带出；非admin审核人收到并可审核；缺/停用配置拒绝；改配置不接管旧申请 | G09完整安全核对弹框/取消零写/配置与上下文漂移守卫16定向、最终131组合及types/build过；真实页面账号配置/审核批准仍待验 |
| F01 | 创建项目选择文件夹模板；模板库新增/改/删只做权限确认 | FolderTemplateLibraryEditor、FolderTemplateService、Formal config审计；B组件/目录测试 | 模板对新增项目形成独立目录，修改模板不改变已有项目；未授权不能维护；无额外审批 | B模块交付有证据，最新入口Review需复核；真实UI待做 |
| F02 | 项目文件夹新增/改/删及空目录/逻辑位置 | ProjectFolderTreePanel/Editor、placement、FolderDeletionCombination | 创建目录、重命名、移动/删除限制准确；非本项目/引用中目录拒绝，空目录可浏览 | 位置实际创建/检入继承通过；真实UI待做 |
| T01 | 文件类型新增/改/删及绑定审批矩阵 | taxonomy页面/CategoryMapping、taxonomy service/route policy | 正式类型管理、关联/停用历史保护，上传类型带出正确规则 | 既有模块基础，仍须当前必要回归及真实页面证据 |
| M01 | 审批矩阵创建/改/删；本次部门可增删 | routes/CategoryMatrix、ActionRoutePolicy、RouteReadiness；G04 selected-dept HTTP/实际resolver | 默认部门/批准人带出；删增后的实际参与人预检与流程一致；修改配置不改变已冻结路线 | G04修复已确认；行政说明中旧培训/分发顺序文案需统一 |
| R01 | 项目文件列表/详情关联按钮→顶部源文件、左项目目录、右全局搜索→确认 | DccFileSelector/CurrentRelations/RelationEditor、latest resolver/乐观锁/幂等 | 跨项目待生效latest可选，源Master自关联拒绝；取消不保存；改当前关系历史不变 | G10直接关联/同一选择窗口与G11跨项目左树已实现，143最终组合/types/build过；名称/正文/历史权限仍分开，真实UI待验 |
| R02 | 当前关联始终最新受控，历史只读申请时快照 | RelatedFileService latest vs snapshot、DetailRelationsPanel；D关系运行时测试 | 目标新版提前受控后当前看到新版待生效；源历史仍旧版本名/编号；正文按实际所选版本权限 | 当前/历史Parent正在收口；正文只读权限不能靠详情管理权限升级 |
| W01 | 部门负责人指派（可自己）并电子签名→实际会签每部门义务签名 | SignoffAssignment/Task-local obligation、Flowable multi-instance、SignoffAssignmentContext | 两部门同leader需分别指派与会签；指派不等于通过；错误密码零推进；外人/错轮次拒绝 | 已有15指派及多轮BPM证据，实际页面待做 |
| W02 | 升版指派关联整改负责人/限期；受控成功才通知，失败不通知 | relationArrangements同签名payload、CONTROLLED event/outbox、Remediation事务测试 | 不选则不建任务；选择准确账号/期限，主文件受控后接收任务，未生效仍通知；审批阻断无通知 | A/D真实组合已交付；外部通知/收件人UI仍需验收 |
| W03 | 批准签名→可选培训；文控上传本次线下文件即可完成培训 | native流程/BPMN、upload TRAINING_RECORD/session/file/round、Workflow submitTraining | 需培训有文控上传入口，非文控不能上传；任意旧ticket/错file/round拒绝；未勾选直接文控审核 | 业务代码存在且模块测试通过；真实页面ticket/下一任务仍未验证 |
| W04 | 文控审核查看冻结正文/基础信息/前序证据并签名；驳回回申请前重新全流程 | Workflow taskReadiness、Finalization/签名绑定、审批中心adapter | 表单显示完整前序证据，密码失败无推进；驳回新申请保留历史，重新会签 | Root作废预检210回归已过；上传/升版端到端待做 |
| W08 | 上传/升版批准人在批准弹框中选择文件负责人，不新增节点；作废不适用 | 正式ApproveTaskReqVO/Workflow、签名证据/前向schema、详情批准弹框 | 启用同租户账号、同一批准签名绑定file/BPM/版本；失败零推进，历史保留原人选，不自动默认申请人 | G13公开HTTP/HMAC/统一签名/真实Flowable560回归及G14弹框修复已通过；同一批准选择负责人、失败回滚/重批准新投影/旧签名保留已Review；实库schema与真实页面待验 |
| W05 | 系统受控章、记录controlledTime，受控立即形成正式受控事实；生效日期可未来 | Finalization→completeControl、stamp真实生成失败拒绝、WorkflowLifecycleTransaction | 真实文件有受控章；受控成功成为latest但旧版仍可执行，技术失败不旧版废/不通知 | H2/存储隔离边界明确；真实盖章/存储未验证 |
| W06 | 到新版本生效日，新版执行与旧版自动作废同事务；保留20年 | Lifecycle activateLocked/ActivationJob、retain plusYears20、并发幂等/回滚测试 | 当前执行定位切换，旧正文/名称编号/签核保留；刷新状态准确；取消失败新版不切换 | G07三个受控版链/真实GxP/并发/回滚368回归已核验；暂停调度迁移已准备，频率/实际Quartz注册运行尚未验 |
| W07 | 文控按预设日期升序提醒/处置/下发，技术失败准确报错 | pendingDistribution接口/日期策略/WorkflowDistributionPanel、Reminder API tests | 待处理列表显示日期和提醒；已下发不再提醒；收件人/方式/二次确认实际保存，待生效提示不变 | G06 PendingWorkflowDistributionList已挂工作台、实际角色/权限及导航39回归过；提醒值待配置，真实UI待验 |
| V01 | 选择具体文件检出，别人不能检出，显示检出人；检入生成横杠小版本 | Checkout/Query真实Master锁、ownership/hash/audit、CheckinDraftInitialization/Placement | 两账号争抢准确拒绝；检入实际文件生成A/1-1、-2，继承属性/位置/正文准确 | C/A组合已多次通过；真实并发及ticket页面待做 |
| V02 | 所选合法较早工作小版→局部/换版→审批；A/9局部B/1、换版升字母 | VersionPolicy/RevisionSourceSelection、selected-body冻结；revision组件18 | 所选较早正文实际被使用；B/1显示真实PARTIAL或REPLACEMENT；不可由字母推导 | 服务已过，详情选body来源正在行为修复 |
| V03 | 失败后仍申请原目标号，独立保留每次失败file/BPM/body/actual/signatures | ReworkPolicy attempt/predecessor/generated unique key；RejectedRevisionRetry H2/Flowable | A/2失败两次第三次仍A/2；可逐次打开原记录和签名；同时不同候选拒绝，同键精确回读 | 后端实际组合50执行含继承；详情尝试/前驱/BPM展示正在接线 |
| Q01 | 仅目标项目正式负责人引用/取消；引用橙色、distinct-project count及使用明细 | ProjectAccess唯一leader、DReference/controller、ProjectBrowserReferences/UsageDialog | 负责人引用到目录、其他OWNER/admin不允许；一项目多目录计1；二次确认取消精确记录，源不删除，最后取消颜色/数目复原；明细固定所用版本/授权项目与分页 | G08固定引用浏览/独立只读追溯已收口；G12 usage前后端已接线、最终154组合/types/build过，global/visible计数/受限和正文权限分开；真实UI待验 |
| O01 | 受控选择器→具体操作面板→作废，独立属性；会签→批准后结束 | ObsoleteService/FormCenter BPM/ObsoleteEvidenceGuard/retention | 全部动作在页面；批准后作废，无培训/文控审核/受控/下发新节点；历史仍可看20年 | G04作废部门/Readiness修复，历史轮次Parent正在收口；真实动作待验 |

## 最终目标门禁

| Gate | 必须证据 | 当前状态 |
|---|---|---|
| G1 四模块与Review收口 | 所有上述项有当前源码/必要有效测试，审查问题无已知未完成 | 未完成；本批子Agent并行推进 |
| G2 公共前后端闭环 | 真实页面完整路径及授权/失败行为；无独立组件未挂或旧入口错路由 | 未完成；不是“各模块编译通过” |
| G3 最终源码验证 | 最终测试/类型/lint/build及真实Playwright证据绑定相同源码/运行Jar | 后端560/11类、infra7项、当前前端150/16文件、正式types与lint已过；最新源码types/lint/build及Jar package已过，真实Playwright未运行 |
| G4 迁移/配置 | 严格19SQL及25当前依赖事实；真实MySQL首重演练/原库升级/运行配置 | 25只读前置已全部PASS；driver31离线及postflight12已复核，实库执行审批pending；另缺tenant1审计25策略与新版真实批准资料、25历史claim原全名核验，均不偷偷回填 |
| G5 本地int_qms合入 | 任务资产边界、来源提交、合并提交号、祖先验证/合入后回归/端口guard | 未执行；不推送origin为用户明确门禁 |
| G6 最终交付 | 修改范围/需求证据/提交号/迁移和运行验证边界完整 | 未到交付阶段，不标goal complete |

## 运行与真实页面测试准备

补充生命周期 Review 场景：目前 completeControl 只比较 latest 版本递增，activateLocked 只作废 currentActive。应验证 A/1执行、A/2已受控未来生效、A/3又受控且生效日期较早/同日/较晚的完整链。较新版本生效后任何较早待生效版本不能随后重新执行或让ActivationJob永久异常；正式处理须保留20年历史与幂等/回滚，不能猜日期规则来绕开，也不能只验证两行版本链。当前这是源码推导风险，尚未作为实际复现或已修复结论。

使用注册int_qms slot6的8067/48067。共享Docker/既有本机MySQL23306与Redis26379恢复已获用户授权并完成；19项本机数据库升级/隔离恢复方案已准备并提出单独审批，未答不执行数据库写入。

获得可用真实环境后，先只读核对迁移/schema、正确Jar与数据源及配置，再执行Playwright真实页面。创建过程中的自有项目/产品/目录/类型/矩阵/账号/签名授权等前置资产也必须通过正式页面；不得用直接SQL/API代建。修改共享配置只在具体影响方案及适当授权范围内实施，并记录恢复计划。账号密码不写入验收文档。

测试顺序：项目/模板/审核配置→两项目上传及全名判重→无培训和有培训两条审批→未来受控与日期切换→检出检入/较早正文/局部及换版→连续失败同号重提→跨项目当前/历史关联与受控通知→引用/取消权限与计数→独立作废及历史。必要负向动作在任务自有数据上完成；不存在的页面入口属于功能缺口，不能API补齐。

日历时区、提醒提前量/渠道及其他未确认管理政策以现存正式配置或用户决定为准，缺值如实列出。生效模拟不得改全机时间或直接更新业务状态；运行测试必须用真实合法任务入口/已授权隔离运行配置。
