# G56 — HTML v1.6流程1–8及指定验收项独立核对

Status: ready_for_root_review。审查起点为Root提供的main/int_qms HEAD8090e7193及G55最新真实收据。唯一仓库C:/IntRuoyiAll-int_main，所有路径下文为仓库相对路径；用户本轮要求提交推送后继续完整核对缺口，Git/真实环境由Root负责。本Agent只读业务链并获有限授权修正文侧栏1个组件两处展示；没有DB/API/浏览器/服务/Git/Maven/fulltypes/build动作。没有将“未实测”写成代码失败，也不把主线一个admin兼任外推为独立角色验收。

真实依据：`doc/tasks/20261001-dcc-integration-unblock/g55-real-mainflow-root-review.json`、`g55-final-business-delivery.md`。实际申请7完成、项目271/产品614/负责人OWNER；真实上传独立type13和不在预设名单的原件到folder2；默认CE与本次FDA分别保留、取消确认保输入；两部门指派/会签、批准负责人、跳过未选培训、文控审核、受控生效及电子下发；审核驳回8→9、批准驳回9→10的待办/通知/原历史保留；正文canvas非空、项目/产品分开、离开详情0空ID读。该收据明确distinctActorsTested=false、trainingSelected=false、completeHtmlE2EClaim=false。这里不重复执行这些动作。

## 需求与正式实现链

| HTML范围/AC | 当前正式链（真实handler→wrapper→后端） | 已证明与剩余验收 |
| --- | --- | --- |
| 流程1：项目/文件夹/类型/真实原件，AC-02 | `upload/index.vue.handleProjectCodeChange` → `ProjectApplicationAttributes.selectProject` / `getProjectDefaults`；`handleFileTypeTaxonomyChange` → fileType upload-options/唯一active-category；`handleFileChange` → SOURCE preview/ticket；`submitter.buildSubmitPayload` → `submitControlledFile` → `DccControlledFileWorkflowServiceImpl` / `DccProjectFolderStorageService` | G55实际项目产品来源、独立类型、非模板名单PDF、仅projectFolderId、内部映射/位置一致已通过。大小写/后缀判重、另一租户/无权正文、切项目dirty确认、图纸PDF配对属于仍需正式专项验收，不恢复预设名单或第二NAS规则 |
| 流程1/13：三组属性，AC-02/05 | `ProjectAttributesFields` → `state.changeMarkets/changeTransfer/validateAttributes`；submit实际属性 → `DccProjectApplicationSnapshotService.saveReservedDraft/freeze`、正式属性服务 | 上传CE默认/FDA实际分离及会签所见同值已有真实证据；NA互斥、OTHER说明、转移Y必填/转N清目标前后端合同存在，AC-05各种条件实际操作未覆盖，不是源码未实现 |
| 流程1二次确认，AC-06 | `submitForm` → `confirmUploadApplication` / `buildUploadConfirmationSummary` → 同context复核 → actual uploadSubmitterService.submit | 实际取消保留输入→再次确认→创建会签已通过。关联/勾选培训均非此次实际选择，完整组合仍待验 |
| 流程2目录与全局搜索，AC-15 | `browser/index.vue` project模式 → `ProjectBrowserPanel` / `ProjectBrowserState.selectProject/selectFolder/search/loadFiles` → `loadDccProjectBrowserPage` → `/dcc/controlled-files/browser-page` → `DccControlledFileQueryServiceImpl.getControlledFileBrowserPage` | 默认LATEST_CONTROLLED，Global/PROJECT_FOLDER明确分支；状态/历史筛选和pagination/late response守卫接通。G55实测项目folder列表和正文，未实测全局检索与旧版/无内容权限账号矩阵 |
| 流程2历史与正文，AC-15 | `detail/index.vue.openHistoryDetail`及列表versionView → 精确selected ID详情；正文`ProjectBrowserPanel.preview`→正式viewer路由；QueryService `canReadBinary`、ProtectedPdfViewer正式预览授权链 | 当前A/1正文canvas、同ID离开详情0空ID请求真实通过。历史已作废/未来待生效、选定历史正文及相应冻结属性需要组合验收。正文侧栏缺少部分HTML字段/完整档案入口的明确源码缺口列下文G56-F03 |
| 流程3项目产品创建/审核配置，AC-01 | `ProductCatalogTabPanel.submitProjectProductRequest` / `ProjectReviewerConfiguration.save` → `projectProductRequests` → `DccProjectProductCreateServiceImpl.createRequest/review/approve/resubmit` / `DccProjectReviewerConfigurationService.assertFrozenReviewer` → WriteService | 审核配置只正式启用账号，提交快照与旧审核人不随新配置替换。Owner初始化在`DccProjectProductCreateWriteService.createWrite`同项目/产品/目录事务调用`initializeApprovedProjectLeaderOwner`。G55创建/待办/双驳回重提真实通过；独立R/L及提交后改审核配置、失败回滚需验，不能默认admin审核规则 |
| 流程3b默认属性维护 | `ProjectCodeTabPanel`正式属性编辑 → projectAttributes API → 属性审计/快照服务 | 当前默认与实际快照隔离源码接通；默认后改+在途/草稿/历史不被反写的真实AC-04仍待验，不在本指定AC范围冒PASS |
| 流程4模板维护，AC-18 | `FolderTemplateLibraryEditor.open/choose/save/removeTemplate` → `/dcc/folder-templates` → `DccFolderTemplateController` query/update权限 → `DccFolderTemplateService.save/delete` | 权限确认后直接保存、无BPM/审核节点；validated树、版本history/Gxp、everUsed删除拒、旧项目目录独立有代码。G55项目用模板生成文件夹已实测，模板有/无权限编辑及已使用停用/删未用未实测 |
| 流程4c项目文件夹，AC-18 | `ProjectCodeTabPanel`详情挂`ProjectFolderTreePanel`→`ProjectFolderEditor.save/remove` → projectfolders PUT/DELETE → `DccProjectFolderMaintenanceService` | 原正式接口/editor能增改空目录，owner/editor及permission、同tenant/parent环、子目录/位置/引用删除保护和二次确认接通。**公共项目目录ProjectBrowserPanel未挂这些维护入口**，与HTML指定操作路径不齐，列G56-F02；不是重新开发后端CRUD |
| 流程5文件类型，AC-18 | `basic-data/file-type-taxonomy/index.vue.submitForm/handleDelete` → taxonomy create/update/delete及启用状态 → `DccFileTypeTaxonomyController` / `DccFileTypeTaxonomyAdminServiceImpl` | active路径/leaf与唯一activecategory分开解析；被category/file/template使用禁止删、可停用保identity/history。G55读取启用类型成功；维护/使用保护实际正负账号未实测 |
| 流程6动作矩阵 | `routes/RouteForm.submitForm` → `saveApprovalRoute(categoryId,{actionType,nodes})` / route DELETE → `DccApprovalRouteController` / AdminService / `DccActionApprovalRoutePolicy` | NEW/REVISION明确3节点配置，OBSOLETE2、LEGACY独立4；部门/批准人来自正式候选、ALL/ANY按规则、实际routeReadiness挡缺岗位/签名而非自动换admin。提交snapshot与obligations保存本次名单。G55两部门实际链通过；矩阵新增改删/配置更换不影响旧任务的专项操作待验 |
| 流程7关联当前/历史 | 上传`openUploadRelations/persistUploadRelations`暂存 → actualsubmit selected IDs；项目列表`openRelationsRow`→ProjectFileRelationsDialog→DetailRelationsPanel → `listCurrentRelations/listHistoricalRelations/replaceCurrentRelations` → FileRelationsController/关系服务 | 当前以`DccLatestControlledFileResolverImpl`读Master.latestControlledFileId，无currentActive替代；历史精确版本快照另读，权限/自身/重复/expectedVersion冲突守卫接通。G55未做真实关联；旧非受控source无最新受控会准确报缺受控，不可用GET写currentset补成功 |
| 流程8选择窗口 | `DccFileSelector`顶部source，`SelectorProjectDirectoryState`实际项目目录，`FileSelectorState` → current context、分页目录/全局查询、selectedMaster去重；preview检查canPreview，confirm save按purpose | 三种relations/reference/operation用途分开，培训在主申请表而非窗口；目录/全局搜索分页不丢selection、取消不保存、权限重新核链存在。多个项目/同目标两处勾选/取消/无正文账号仍待真实验收 |
| AC-19/26 文控按日期下发/提醒 | workbench→`PendingWorkflowDistributionList.refresh` → `getPendingWorkflowDistribution` → workflow-lifecycle pending API → LifecycleService `pendingDistribution`；办理→原`WorkflowDistributionPanel`真实二次确认→DistributionService | 已受控且distributedTime为空、按effectiveDate/ID升序，提醒筛到config截止、前端拒无序/重复/未控数据。G55真实电子下发、保持预设日期和distributedTime通过；接收签收/纸件处置未办，多日期排序/7天边界/已处置排除仍待验。真实请求/保存错误不能虚报成功，但没有独立“下发审批驳回/业务失败”节点 |
| AC-26 已确认配置 | `DccWorkflowDatePolicy`强制zoneId/reminderLeadDays，`DccControlledFileActivationJob`调用dueVersions/activateDue；Root已准备Shanghai/7天/每分钟配置 | 代码有准确政策与job入口，不能将源码存在当本次每分钟真实调度已验。当前本轮日期同日，未观察未来日期自动切换；Root实际job注册/启用与日期边界另核。本报告保已确认Shanghai/7天/分钟，不重新变成待讨论 |
| AC-23统一“受控版本” | 上传/selector/version范围、pendingDistribution和当前详情以受控/工作/审批/历史分开 | scoped源码未发现“有效版本/最新有效”用于文件版本；“当前有效项目代码”是项目概念不是版本。正文侧栏currentActive误label本次修为“当前执行受控版本”；这不新增latest受控投影，也不把未生效受控版当可执行 |

## 仅确认的源码缺口与有限处理

### G56-F01（P2，本批已修，待Root统一验证）正文侧栏项目与执行版本

原`shared/ControlledFileBasicInfoPanel.vue.formatDccProjectCodeLink`把productName/productCode当DCC项目链接文字，导航却真实dccProjectCodeId；同组件label“当前受控版本”读currentActiveVersionNo。QueryService.resolveCurrentActiveVersionNo只取ACTIVE执行版本，未来A/2受控待生效时A/1仍执行，不能把这个字段冒“最新受控版”。

Root授权的唯一组件修复现用已有正式projectName/projectCode、真实ID导航、unbound明确文字；产品栏目保持。label准确为“当前执行受控版本”，值不改，不新增字段/API。BDD与有效RED→GREEN记录`g56-preview-project-execution-version-bdd.md`，完整指纹在本批manifest；本Agent不称实际正文页面已HMR验收。

### G56-F02（P2，未改）项目目录维护入口未承接HTML步骤

`ProjectBrowserPanel.vue`只有项目查找、树选择、搜索/正文/关联/操作面板及引用，没有ProjectFolderEditor/新增编辑删除调用；已有正式维护组件只挂在`ProjectCodeTabPanel.vue`详情826行附近。HTML流程4c明确从项目目录选择项目新增/修改/删除。后端存在维护能力不能代替这个入口。

有限修复建议：在公共项目树选定project/folder后复用既有ProjectFolderEditor，不造第二CRUD、任意NAS树或默认OWNER；沿正式update权限与项目editor/owner后端守卫，成功后重读**当前同项目**目录。BDD Given合法项目权限和真实选中folder，When从项目目录增改/空删，Then准确同projectID保存/刷新；无权/非空拒，取消0写、切项目晚成功不污染。有效RED执行真实ProjectBrowserPanel handler/renderer与现wrapper；原FolderEditor及folder维护接口必要回归。独立账号的页面正负角色与非空删除由Root验。

### G56-F03（P2，未改）正文预览侧栏未显示完整浏览档案要求

`detail/index.vue` viewerMode实际只ProtectedPdfViewer+ControlledFileBasicInfoPanel；该共享侧栏目前没有受控日期/预设生效日期/下发日期、项目逻辑folder、文件负责人和冻结实际属性/历史入口，showInfoActions默认false且viewer caller未启用。这些事实/历史在同组件v-else的完整详情中存在，G55已证明完整详情与正文均可用，但用户从项目列表“正文”进入的页面未承接HTML流程2c完整浏览信息。

有限处理先让预览侧栏展示**已有正式字段**的项目folder/负责人/三日期，并提供现只读完整详情/历史入口；不把当前项目默认值当历史实际属性、不新增角色或让Name权限获正文。冻结属性/过程记录复用`DetailApplicationHistory`准确file/BPM映射，可按已有只读详情入口展示，避免复制新档案平台。BDD Given浏览待生效/历史ID且字段/冻结快照不同，When正文或历史入口打开，Then准确选定版本/三日期/实际属性；无内容权限仍拒正文。RED实际viewer caller与子树渲染，原detail/context/preview权限合同回归；Root真实旧版、待生效、无正文账号验。

### G56-F04（P2，未改）详情阶段进度仍用历史固定四阶段投影

`detail/index.vue.originalReleaseApprovalStages/displayStageProgressList`总按DOC_CONTROL_REVIEW→MATRIX_REVIEW→MATRIX_APPROVAL→DOC_CONTROL_APPROVAL四位置排列；`stageProgressList`实际来自正式snapshot/task，但map仍补不存在第四“文控批准”且把当前原生文控审核放在前面。Native NEW/REVISION主流程是会签→批准→可选培训→文控审核；OBSOLETE独立BPM两节点，不能用File旧key判它。实际签名提示已G55改，但进度条来源仍不一致。

有限修复建议：仅native当前**准确task/BPM定义及快照**采用其真实阶段顺序，LEGACY保持独立显示；不编造培训/作废后节点，不改审批引擎或读取另round。BDD Given native NEW/revision/obsolete真实round，When解析进度与当前阶段，Then只有适用节点且顺序真实；legacy合同不受影响。有效RED执行实际progress函数+渲染，回归现历史/context guard。Root/对应生命周期agent确认实际训练和独立作废scope后实施，不能仅改文本假成正确流程。

## 收口与剩余验收

除上述明确入口/展示缺口，本范围未发现需要把已通过四主方向推倒重写的主链缺陷。当前矩阵/模板/typeCRUD和关联读写的正式handler/API接通；未进行其真实权限/组合验收不能写“满足全部业务”。下一轮按主方向优先：Root复核G56-F01小改，随后接已有项目目录维护入口；再处理裸正文档案/阶段投影，最后安排管理权限、历史/关联、未来日期等实际组合。只读审查没有新增用户问题/配置猜值或fallback方案。

交付前保留准确源码锚点及关键rawSHA到本批finalmanifest；主收据的实际ID/时间/身份只证明其对应已执行案例。本报告不能替代其它agent对版本/作废/引用/审批核心的独立范围，也不声明完整HTML已E2E PASS。
