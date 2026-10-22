# G15 当前公共流程只读审查

2026-10-03，整合树 `codex/20261001-dcc-integration`。依据最终需求 HTML v1.4、Root主任务 goal-acceptance-matrix/g12-review/g13-review、现有生产源码及最新用户确认。未运行真实 E2E、服务、数据库、Maven、全量类型/构建或 Git。G16 的已授权检入修复另有有效 RED/GREEN，本报告不重复扩大修改范围。

Root主任务报告位于 int_qms 根树 `doc/tasks/20261001-dcc-integration-unblock/`，本整合树没有这些主任务文档；本次只读跨树读取，没有改 Root 文档。主矩阵 W08“未实施”、G4“Docker许可待定”、提醒参数“未定”属于历史快照：当前 W08 已有服务/Picker/冻结展示，G14父handler异步修复已报告；用户最新确认 Asia/Shanghai、提前7天工作台提醒、每分钟生效检查，且授权Root恢复既有Docker依赖。源码、离线证据与实际部署仍分开。

## 十二流程当前映射

以下路径从仓库根解析；方法/组件名是可重新定位锚点，避免误用并行编辑后的旧行号。

| HTML流程 / Goal项 | 当前可定位的公共入口及正式链路 | 权限/身份/失败边界及真实验收前置 |
|---|---|---|
| flow-01 上传 / U01–U03 | FE `upload/index.vue` `submitForm`、`freezeUploadApplicationDraft`、`captureUploadSubmissionContext`、`confirmUploadApplication`；`upload/submitter.ts` `buildSubmitPayload`；BE `DccControlledFileUploadServiceImpl.uploadPreviewFile`、`DccControlledFileWorkflowServiceImpl.prepareSubmitContext`、NameClaim | 项目/逻辑folder独立于NAS directory，完整源名以实际Multipart/ticket校验；默认来源/actual分开，真实部门和批准人，确认取消零申请。需页面准备项目默认、独立目录、至少三级taxonomy→正式类别、项目文件模板、该类别NAS提交目录、真实矩阵/签名/存储；预览本身是真临时文件写入，不能称取消“零存储写”。 |
| flow-02 浏览 / B01–B02 | `browser/index.vue` 挂 `ProjectBrowserPanel`；`project-browser.ts.loadFiles`；`applicationRead.loadDccProjectBrowserPage`；详情 `DetailApplicationHistory`/版本表及基本信息 | 默认latest受控，ALL/WORKING/审批/历史实际状态查询；browserScope与selectorScope分离，名称不授正文，Long不转Number。点击行ID与当时正文一致；强详情独立授权。需至少一个当前受控、未来受控、工作稿和历史任务自有样本；空DOM不算该场景PASS。 |
| flow-03 项目/产品 / P01 U02 | `basic-data/product-catalog/index.vue` 挂 ProductCatalogTabPanel；`openProjectProductDialog`、`submitProjectProductRequest`/`confirmProjectProductApplication`、`handleProjectProductAction`；ProjectReviewerConfiguration | G09信息核对已存在，不能重复旧“无二次确认”。创建正式enabled leader/模板/三组属性，带出配置审核人；同一申请审核身份固定；缺/停用配置阻断。需真实非admin审核账号、approve职责、create/update权限，保存/审核/批准必须页面完成。遗留ProjectCode维护/旧onboarding不是本次正式新建流程替代。 |
| flow-04 模板/目录 / F01–F02 | ProjectCodeTabPanel 的 DCC基础条目抽屉挂 `ProjectFolderTreePanel`、`ProjectAttributeConfigurationDialog`、`FolderTemplateLibraryEditor`；ProductCatalog新建窗口也有模板库按钮 | 模板未用才删除、用过停用；已有项目目录独立。项目目录delete检查子目录/placement/reference，所选folder/project精确绑定。需正式project query/update及新增任务模板，可先页面造空目录再验删除边界。不能把旧NAS目录替代逻辑文件夹。 |
| flow-05 类型 / T01 | `basic-data/file-type-taxonomy/index.vue` 新增一级/新增下级/编辑/删除/检查对应类别；FileTypeCategoryMappingDialog | active类型至少三级供上传，类别映射正式唯一；已引用/停用历史保护。需category:manage与叶子实际类别、NAS绑定，不只taxonomy名称可见。 |
| flow-06 矩阵 / M01 | `routes/index.vue` handleCreateRoute/RouteForm；RouteForm `currentExpectedStageNos`、`validateRouteStageNos`、`submitForm`；positions页面分配角色 | NEW/REVISION会签DEPT→批准→文控，OBSOLETE两节点；模板描述已当前主线，LEGACY明确历史四阶段不是当前默认。会签部门可改且selected部门参与真实readiness。需启用部门leader、审批角色实际users或岗位、系统岗位、角色permission、签名授权图片；缺一项正式预检不ready。 |
| flow-07 关联 / R01–R02 | ProjectBrowserPanel `dcc-project-file-relations`→ProjectFileRelationsDialog(P15轻量)→DetailRelationsPanel/DccFileRelations→replaceCurrentRelations；upload关联只改本次选择 | G10已补列表关联，名称级授权能走轻量projection，不先读强详情；current取latest，history原snapshot，不互盖；乐观版本/幂等保存。需两项目受控源、目标及一个旧审批snapshot，权限撤销/晚响应必须页面验证。 |
| flow-08 选择器 / R01 | DccFileSelector `SelectorProjectDirectoryState`、`selectProject`、`directoryClick`、`scopeChanged`、`confirm`；项目分页和跨项目左树已挂 | G11已补跨项目树，不能重复UI-03旧缺口。源身份固定、自Master关联禁选，已选跨页保留，取消零写，正文按候选canPreview。需另外授权项目/空folder与同名不同IDfolder，不能猜ID或用全局搜索替代目录点击。 |
| flow-09 审批/受控/下发 / W01–W08 O01 | approval-center模块办理→详情DetailSignoffAssignment、ApprovalFileOwnerPicker、培训文件上传、submitActionDialog；WorkflowDistributionPanel；workbench挂PendingWorkflowDistributionList；Obsolete独立申请 | 指派与会签各有真实签名、same leader两个dept两义务；owner在native MATRIX_APPROVAL同签名选择、无额外权限；G14旧异步窗口已修不重复列未关。需真实四种职责账号、正确签名密码、线下培训文件、盖章存储/OnlyOffice依赖、通知接收人及正确本轮BPM。未来受控与生效、下发分开；提醒缺配置明确error。作废只会签批准，20年保留。 |
| flow-10 检出/检入/正式升版 / V01–V03 | `browser/index.vue` storage表 handleCheckout/handleCheckin/submitCheckin；工作版 handleSubmitWorkingIteration→detail DetailApplicationPanel选择正文→PARTIAL/REPLACEMENT | G16删除公开MAJOR检入，payload固定MINOR，返回同Master同正式基线工作小版/释放锁才报结果；源/配套PDF会话准确，late response不串。后端validateWorkingCheckin已MINOR-only。正式升版独立所选工作正文，失败重提同目标号并留每file/BPM/signature。注意项目panel本身无checkout按钮，当前动作在同公共页storage分支，须确认该任务文件能通过真实存储目录定位，不可API替代。 |
| flow-11 引用 / Q01 | 项目folder DccProjectReferences chooser/create；保存行正文/readonly trace；DccReferenceUsageDialog usage-page/分页 | G08固定selectedControlledFileId，P15 source/folder/body、独立强详情授权；G12使用明细已挂源行/保存引用行，全局distinct count/visible/restricted保留。leader限定，不按OWNER/admin冒充，正文不由引用授予。需目标正式leader账号、两目录同项目与另项目，受限账号和later源版/作废样本。 |
| flow-12 取消引用 / Q01 | DccProjectReferences prepareCancel/cancelConfirmed→cancelProjectReference exact project/folder/master/reference | 二次确认取消保留，真实失败保入口；成功仅精确引用删、count刷新、最后引用source字体复原。子目录含引用删除阻断。需真实leader与非leader两上下文，不把super_admin当leader；同项目多目录计1和取消最后一项目由DOM明细核对。 |

## 当前具体缺口与风险

1. **G15-R01 运行前置尚未由真实页面验证。** 当前已核验port registry指向int_qms slot6 8067/48067，本Agent未连接这两个服务也未操作Docker。配置 `dcc.workflow.zone-id=Asia/Shanghai`、`reminder-lead-days=7` 与 `dccControlledFileActivationJob` 每分钟实际Quartz记录/运行日志需Root部署并只读核对；源码DccWorkflowDatePolicy故意不默认。用户许可恢复既有Docker依赖不等于migration/schema已执行。
2. **G15-R02 新项目的目录模板与“项目文件模板”是不同前置。** ProductCatalog创建携folderTemplate，ProjectFileTemplateEditor另配taxonomy叶子+显示文件名。upload可选文件严格依赖后者和类别NAS绑定。页面创建项目成功不能直接证明上传候选/文件类型齐全；E2E必须依次进入DCC基础条目维护项目文件模板，不能空模板后API代建。
3. **G15-R03 账号职责与签名是实际阻塞候选。** AGENTS的admin标签只确认登录身份，不证明doc_control、electronic_signature_admin、部门leader/系统岗位、业务review/approve/distribute。治理页真实 `getRoles.includes('electronic_signature_admin')` 与permission限制授权页；每个审批人还需本人在“我的签名”上传真实签名图片且启用。不能用admin审批代替指定审核人，不能生成假签名图或虚构账号/ID。
4. **G15-R04 项目配置负责人控件Long精度尚有局部风险。** `ProjectAttributeConfigurationDialog` 当前leaderId `ref<number>()`、users UserVO.id:number，保存直接传入但没有referenceIdentity/安全数字归一；其新建ProductCatalog/批准Picker已有严格Long。实际服务可能返回大Long string，本控件此时读/回显数值类型有不一致风险，需要独立页面/类型组合验证。只读推导，未做真实复现，也未修改该Owner。
5. **G15-R05 项目审核配置UI角色门禁与正式服务需组合核验。** ProjectReviewerConfiguration.canConfigure使用checkRole('doc_control')，而通用helper会把super_admin视作任意角色；正式后端仍需doc_control身份。可能按钮出现但保存被拒；这是权限组合风险不是后端被绕过证据。workbench显式真实doc_control已与此区别处理。验收必须真实doc_control账号配置，另测super_admin-only拒绝。
6. **G15-R06 项目浏览操作panel里没有检出/检入直达动作。** 公共storage分支具备动作且G16已修，detail只有引导而未发现checkout handler。flow-10从项目选定具体文件能否自然找到storage行（并保留同版本）仍需真实路径核验；没有可达定位时应报入口缺口，不能手工构造API/虚拟行。
7. **初始A/3、多字母修订及过期日期口径不冒充已确定。** HTML D09是建议，当前上传revision/1规则不作为无条件缺陷；未来所有正式局部/换版按统一service。过去日期DccWorkflowDatePolicy.requireReviewDate真实阻断；不得给脚本补今天/旧数据假成功。法律证书有效期字样与文控版本术语分开，G16未修改无关页面。

## 前置页面可操作性地图

| 前置 | 源码可操作入口 | 待真实页面核验 |
|---|---|---|
| 启用用户/角色/岗位/部门负责人 | system/user UserForm岗位，role分配，system/dept负责人；DCC positions新增角色/分配用户或systemPost | 当前测试账号及每个职责account真实存在、启用、同租户、角色permission/部门leader一致。禁止把声明/目录行存在当正式授权。 |
| 电子签名授权/图片 | signature-governance authorizations→DccSignatureAuthorizationsPane；my-signature→SignatureGovernanceMySignaturePane上传/启用，要求变更原因 | 管理员真实角色、签名者本人登录、图片已启用/hash及签名授权未过期，错误密码实际零推进。 |
| 模板库/项目模板/类型 | FolderTemplateLibraryEditor，ProjectFileTemplateEditor，file-type-taxonomy映射，Category绑定NAS目录 | 两类模板分别配置，至少三级有效类型映射唯一真实类别，NAS最后一级绑定并有存储权限。 |
| 项目/产品/审核配置 | ProductCatalog正式新建+确认，ProjectReviewerConfiguration，records页审核/批准；ProjectCode详情默认+folder | 配置审核不是固定admin，缺配置显示真error；审核/批准UI实际产生正式项目/产品和独立目录。 |
| 三动作矩阵与签名候选 | routes RouteForm NEW/REVISION/OBSOLETE，positions与dept目录 | 会签DEPT、批准POSITION或USER来源实际resolve，文控职责/岗位/签名齐全；上传改变部门后的ready与执行一致。 |
| 提醒/生效调度/对象存储 | DccWorkflowDatePolicy、ActivationJob；infra job与job log正式页；MinIO/实际PDF转换配置由Root | 七天/时区/每分钟job真实生效；存储/转换失败不受控；UI能观察实际待生效切换及下发后移出。 |

## 脚本准备结论

本任务提供一个无需生产变更的正式Playwright脚本，范围为前置可达性、浏览筛选/关联与明细取消、上传完整确认取消、引用窗口取消、工作台提醒只读。运行时由Root填真实菜单链/credentials/任务自有项目和文件名；没有默认密码/账号/业务ID，没有API/SQL/fetch/mock。

完整写入12流程与审批/生效/通知仍按配套计划逐步通过真实页面准备和办理；此脚本的局部PASS不代表整体目标、迁移、调度、盖章或合入通过。每步trace从登录成功后开始，不收集登录密码输入。尚未运行实际脚本；静态准备核验单独记录。

G19 更新：G15-R05 前端checkRole扩大文控角色风险已经在Root授权单一组件范围内修复。ProjectReviewerConfiguration现直接核对正式userStore真实doc_control+update，读/确认/写后身份代际守卫覆盖换账号/失权/关闭/迟到。真实helper/SFC RED5→受影响32PASS，组件lint0/0，证据在execution-log G19。原风险依据保留，不继续作为当前未修复缺口；真实登录/后台角色组合仍需Root页面验收。
