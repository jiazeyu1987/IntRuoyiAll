# G07 公共 UI 闭环只读审查

2026-10-03；整合树 codex/20261001-dcc-integration。依据 docs/product/dcc-final-requirements.html v1.4（flow-01..12）和主目录 doc/tasks/20261001-dcc-integration-unblock/goal-acceptance-matrix.md。主目录矩阵从 C:/IntRuoyiAll-int_main 只读取得，报告中的源码路径均相对仓库根；没有把主目录文件复制成当前整合树新矩阵。

本报告是当前源码与已执行定向组件证据审查，不是实库、真实页面或最终合入证据。未运行服务、数据库、E2E、Git提交或全量type/build。浏览/引用源码当前由upload_closure收口，下列锚点在其本批文件基础上读取；Root最终构建后如源码变化须重新核查。

## 原12项完整覆盖

| HTML原项 | 矩阵映射 | 当前公共入口与正式数据锚点 | 当前判断 |
|---|---|---|---|
| flow-01 上传 | U01/U02/U03 | IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：selectedSignoffDepartmentIds控件、handleFileChange/route-readiness、confirmUploadApplication、handleSubmit；同目录submitter.ts；后台upload-preview检查实际MultipartFile.originalFilename及最终NameClaim事务 | 项目/逻辑目录/类型/部门/原文件/日期/初始号/三属性/关联/培训/二次确认都有公共链路，早期全名判重使用正式上传端点P14。当前界面证据仍离线；关联跨项目左树见UI-03。 |
| flow-02 浏览 | B01/B02 | browser/index.vue:5挂ProjectBrowserPanel；browser/ProjectBrowserPanel.vue:版本范围select、previewRow/openOperationRow；browser/project-browser.ts:loadFiles；API applicationRead.ts:loadDccProjectBrowserPage；detail/index.vue:openHistoryDetail | G07默认最新受控、明确全部/工作/审批/历史状态通过服务器SQL page/count；每个点击ID精确保留、正文受canPreview控制。名称可见行的操作面板仍依赖强详情，见UI-02；引用行没有浏览入口，见UI-01。 |
| flow-03 项目/产品创建 | P01/U02 | basic-data/components/ProductCatalogTabPanel.vue:391负责人账号、417目录模板、425三属性、44审核配置、submitProjectProductRequest、handleProjectProductAction；ProjectReviewerConfiguration.vue；ProjectCodeTabPanel.vue:1073挂ProjectAttributeConfigurationDialog | 已配置审核身份带出/冻结、重提、三属性和模板都挂公共入口；负责人是系统账号，不猜admin。提交确认层次见UI-06；真实菜单/账号/审核/批准创建仍需页面验收。 |
| flow-04 文件夹模板/目录维护 | F01/F02 | ProductCatalogTabPanel.vue:540/ProjectCodeTabPanel.vue:1074挂FolderTemplateLibraryEditor；ProjectCodeTabPanel.vue:826挂ProjectFolderTreePanel；ProjectFolderTreePanel.vue:6/15/16新增/编辑/删除；ProjectFolderEditor.vue:openDelete/save | 模板树、原因、权限及删除二次确认有实际入口，项目目录读取保留空目录。维护入口目前在项目代码详情，项目浏览树仅浏览；若要求直接目录侧边维护需Root确认入口等价。服务限制、模板独立/引用中目录保护需真实页面验证。 |
| flow-05 文件类型 | T01 | basic-data/file-type-taxonomy/index.vue:handleForm/submitForm/handleDelete；FileTypeCategoryMappingDialog.vue；api/dcc/controlledFile/fileTypeTaxonomies.ts | 新增/修改/停用/删除及类别映射链存在，删除二次确认；仍须真实绑定矩阵/已用类型禁止删页验收。无本批实际新缺陷证据，不凭组件存在关闭T01。 |
| flow-06 审批矩阵 | M01 | routes/index.vue:handleCreateRoute/保存候选集合/derive preview；upload/index.vue:refreshRouteReadiness；DetailApplicationPanel.vue:open；DetailObsoleteApplication.vue准备部门/批准人；workflow.ts:previewControlledFileRoute五参数 | 本次部门替换后再解析、正式MATRIX_APPROVAL批准人及Long字段已接；培训文控职责不从旧候选文字推断。冻结路线/停用历史和真实多部门人选需页面验证。 |
| flow-07 建立/取消关联 | R01/R02 | detail/index.vue:543挂DetailRelationsPanel；DetailRelationsPanel.vue:load/persistCurrent；relations/DccFileRelations.vue:editor.save；api/relations.ts:listCurrentRelations/listHistoricalRelations/replaceCurrentRelations | 当前实际source校验Master/租户/项目，旧源只读、幂等/rowVersion通过正式服务。项目文件行缺直接关联入口且名称授权用户到不了强详情，见UI-02；目前无法称原点击关联路径闭环。 |
| flow-08 统一关联窗口 | R01/R02 | upload/index.vue:630挂DccFileSelector；DetailRelationsPanel.vue:source/目录加载；relations/DccFileSelector.vue:directoryClick/global/page/select/confirm | 顶部实际源、全局跨项目搜索、分页、选择保留、取消零保存、自关联禁止都接正式身份；左树仅来源/目标单项目，未能切换其它项目目录，见UI-03。 |
| flow-09 上传/升版/作废审批 | W01..W07/O01 | detail/index.vue:isSignoffTask/approvalProcessInstanceId/DetailSignoffAssignment、培训ticket上下文、WorkflowDistributionPanel；DetailSignoffAssignment.vue:saveAssignment；DetailApplicationHistory.vue:selectRound；workbench/index.vue:103挂PendingWorkflowDistributionList | 任务本地身份、指派/实际会签分别签名、整改同载荷、文控培训上传、正式下发确认已有入口。W07旧矩阵“未挂列表”已过时：当前已挂workbench。作废审批默认属性轮次错位见UI-04；自动生效job注册/提醒配置/盖章存储/通知接收UI无真实验收，不能以类存在关闭W02/W05/W06/W07。 |
| flow-10 版本/升版 | V01/V02/V03 | browser/index.vue:handleCheckout/handleCheckin/canSubmitWorkingIteration/handleSubmitWorkingIteration；DetailApplicationPanel.vue:selectIntent/selectIteration/submitRevision；revision/DccRevisionPanel.vue；detail/index.vue本版变更事实/尝试/前驱/BPM；Backend history逐行processInstanceId | 合法较早正文ID、真实PARTIAL/REPLACEMENT、P13OWNER他人稿独立换版、CE/FDA原snapshot及失败同号尝试身份已接。检入/真实正文比对和连续失败重提页面需验收；不可把离线18组件测试视为V01..3真实通过。 |
| flow-11 引用 | Q01/B02 | ProjectBrowserPanel.vue:54挂DccProjectReferences；browser/project-browser.ts:canReference/createReferences/mapReferences/refreshUsage；DccProjectReferences.vue:createSelected；DccReferenceBadge.vue | 正式目标leader、橙色文件名、来源标签、distinct-project count、精确选定版本未自动跟最新已接。引用条目缺正文/属性追溯入口见UI-01；计数无引用项目/文件夹/版本明细入口见UI-05；源项目详情补读为权限组合待验证项。 |
| flow-12 取消引用 | Q01 | DccProjectReferences.vue:prepareCancel/cancelConfirmed；browser/project-browser.ts:cancelReference/referencesChanged；后端DccProjectReferenceService.cancel | 原项目/目录/master/reference精确身份、确认原因、失败保留和计数刷新链存在；橙色源文件最后计数归零由G07 7项SFC用例证明，真实多目录distinct计数/他人禁止仍须页面验收。 |

## 有具体源码依据的未闭环项

### G07-UI-01 引用行没有浏览/详情/历史追溯入口（明确源码缺口）

依据flow-11要求“浏览时展示该文件版本自身实际属性”，源作废保留追溯入口。DccProjectReferences.vue模板只有文件名span、版本、badge和leader取消；没有正文/浏览/详情handler。其ReferenceRow selectedVersion甚至只Pick fileName/versionNo/status/pendingEffect。ProjectBrowserPanel.vue将openPreview传给引用选择器，却没有将已保存reference的实际selectedControlledFileId接浏览动作。

可核对路径：负责人引用受控A/1到目录→回到此文件夹引用表→没有动作能打开此保存版本正文、属性或原签核；源后来到A/2/作废也没有历史追溯按钮。不是后端拒绝后降级，而是入口不存在。下一步由Root给browser/reference Owner分派：基于持久selectedVersion/selectedControlledFileId和P15名称/预览正式权限接浏览/只读详情；不得改成Master.latest或取得源编辑资格。

### G07-UI-02 项目列表关联入口与名称权限路径不闭环（明确源码缺口/授权组合风险）

flow-07直接要求“文件列表点击关联”。ProjectBrowserPanel.vue:48操作列仅正文和操作面板，缺关联按钮；openOperationRow→/dcc/controlled-file/detail/{id}。detail/index.vue:loadData首个getControlledFile强详情成功后才有fileDetail，543的DetailRelationsPanel也依赖fileDetail。P15后台已有真实DetailAuthorizationGuard测试证明名称可见但详情拒绝、relation-permissions仍可成功；当前UI没有直接用这条轻量能力入口。

可核对路径：仅名称授权账号在全局/项目目录看见row，正文被禁用，点操作面板会进入强详情读取；无法办理/查看有权限的名称级关联，尽管P15接口正式允许读取。下一步需要独立轻量关联drawer/路由或正式可读操作投影；不得用canPreview简单代替详情资格，也不能放宽getControlledFile鉴权。

### G07-UI-03 统一选择器左侧不能切换其它项目目录（明确源码缺口）

flow-08左侧要求切换项目、展开文件夹。DetailRelationsPanel.vue:103只读actual.projectId一棵树；upload/index.vue:1661仅map当前上传项目；ProjectBrowserPanel.vue给引用/操作selector的directories是当前state.project一棵树。DccFileSelector.vue自己只接受directories并渲染el-tree，没有项目分页/选择装载。

跨项目全局搜索已能用，但这不能代替左侧跨项目目录路径。下一步Root决定共用正式名称授权项目/目录loader及归属；先选一项其它项目目录行为RED，再统一给selector接。不要把来源NAS目录或数字相等当逻辑文件夹。

### G07-UI-04 作废审批默认显示原上传/升版属性轮次（可确定的父组件绑定缺口）

detail/index.vue:542的DetailApplicationHistory primary-bpm-round固定fileDetail.processInstanceId。3058 approvalProcessInstanceId则优先route.query.processInstanceId，作废流程是另一真实FormCenter BPM。DetailApplicationHistory.vue只在primaryBpmRound对应正式mapping时自动选轮次。

可核对路径：原受控文件 native BPM=U，作废申请 BPM=O且actual不同；以审批入口processInstanceId=O打开同file，指派/任务在O，属性Panel仍自动选U。手动选择O可看对，但进入审批自动显示的申请属性与正在办理轮次不一致。下一步Root分派detail：用实际办理approvalProcessInstanceId选正式application-rounds映射（只读详情仍native默认），实际route变更/晚响应测试RED→GREEN，不从签名文本或definition猜类型。

### G07-UI-05 引用计数缺项目/文件夹/所用版本明细（明确源码缺口）

flow-11写明引用项目数可打开明细查看项目、文件夹、所用版本。DccReferenceBadge.vue为span与计数字样；ProjectBrowserPanel.vue只有usage(masterId/referenceProjectCount/referenced)，getProjectReferenceUsage响应也只有这些字段。无点击、明细表或正式明细加载合同。当前distinct数值对不等于完整明细闭环。

下一步Root分派D/backend先交名称授权且不可越租户的正式usage明细投影，再给浏览Owner接只读drawer；按project去重显示数，同时保留每folder/reference/选定版身份。

### G07-UI-06 项目产品新申请缺独立信息核对阶段（与原文逐步流程需Root确认）

flow-03写“点击确认→核对申请信息→点击提交申请”。ProductCatalogTabPanel.vue:submitProjectProductRequest验证、配置重读后直接create/resubmit，没有ElMessageBox/单独确认状态。表单显示审核人/模板预览，但footer点击提交即写。与上传正式二次确认不同。建议Root确认是否必须按原文补独立核对；如果是，给basic-data Owner分派，完整冻结负责人/产品/模板/默认属性/审核人及取消零写测试。此项不擅自扩写basic-data。

## 已修复/已挂接与仍待运行的边界

- G05/G06详情现有current/history关联、申请精确round列表、尝试/前驱/BPM、原工作稿CE/FDA读取、显式OWNER换版保留。未把这批成果回滚成旧报告的“未接”。
- PendingWorkflowDistributionList在workbench/index.vue:103已实际挂接，不再列为未挂组件。日期/提醒读取与下发按钮挂公共workbench；最终job配置、提醒提前量、真实文控账号/收件人权限和API错误仍需Root页面验收。
- FolderTemplateLibraryEditor/ProjectFolderTreePanel/ProjectAttributeConfigurationDialog/ProjectReviewerConfiguration在公共basic-data父页均找到挂接。菜单真实配置和无权限组合未验收，不用存在性当PASS。
- Native会签taskDefinitionKey=MATRIX_REVIEW与后台DccWorkflowSignoffAssignmentService一致；assigned只是指派事实，真实会签另有密码签名。未发现需要把ASSIGN猜成独立taskDefinitionKey的证据。
- 强源码风险但还未做组合实际复现：ProjectBrowserState.mapReferences为sourceProjectName补getProjectDiscovery(sourceProjectId)，可能比reference名称授权要求更强；应以目标可读/源名称可读但源project query/scoped拒绝组合验证，不把此推导直接写成已经失败。
- 多个未来受控版的生效链、调度注册、迁移/配置、真实存储/盖章/通知、最终int_qms融合仍是goal-acceptance-matrix G2/G4/G5门禁，本Agent无新增运行证据；Root继续处理。

## 本批wrapper与定向证据

applicationRead.ts loadDccProjectBrowserPage第三参数ProjectBrowserOptions：latestVersionOnly默认true，status为DccProjectBrowserStatus正式联合；显式false+精确status下发到服务器；返回status或latest身份失配明确报错。selectorScope接口未变。新wrapper行为RED为默认false!==true；GREEN dcc-detail-closure16/16，既有公共browser model10/10、SFC7/7 PASS；保持page/total不前端过滤聚合。修改范围只共享API/专属测试/任务报告，browser/引用仅只读。

Root下一步应优先UI-04（审批读错本轮属性），随后UI-01/UI-02（无入口/错误权限链）、UI-03/UI-05（完整选择/明细路径）；UI-06先核对原文门禁。以上都是仍可分配的具体工作，不把本审查报告标为整套completed。

## 2026-10-03 G08更新（只关闭UI-04本批源码/单元缺口）

G07-UI-04已按Root分派修复：DetailApplicationHistory父参数由file.native改成applicationRoundSelection实际办理上下文；显式route BPM/task需既有TaskApi真实响应核验后才选正式mapping，未核验/错误/文件变更立即清旧证据。普通详情仍native默认可明确查历史；办理态锁实际轮次，缺/重复/外来mapping显式报错。后台HistoryGuard现有协议足够，无新增API支持请求。RED6/6→GREEN10/10，受影响详情/训练44/44回归、两个静态接线合同、两个SFC编译、3源码lint0/0。真实页面/Root最终type/build未执行，不能把此条写成整体G2/G3通过。其它UI-01/02/03/05/06保持报告原审查边界等待Root分配。

## G11 UI-03更新

按Root本轮唯一范围已修复：共享DccFileSelector左侧实际项目搜索/服务器分页→任意授权项目→逻辑folder→精确selector查询，Source/已选/引用目标固定；无folder只等用户选folder或明确global。晚目录/页响应及关闭/unmount失效；错误局部可见global可用。新真实wrapper/SFC RED3→GREEN7，受影响25+18+14回归、2源码lint通过。没有改各Owner父页或后台、没有实际页面证明；整体门禁仍由Root统一。
