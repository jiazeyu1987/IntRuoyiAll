# 文件唯一归属与接入方式

以下路径以仓库根目录为基准。BE=`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc`；FE=`IntRuoyiFronted/src/views/dcc/controlled-file`。测试按生产文件归属同线程；共享测试另列统筹。

## 1. 唯一归属表

| 文件或目录 | Owner | 其他线程的接入方式 |
|---|---|---|
| BE/service/file/DccControlledFileWorkflowServiceImpl.java及对应业务接口 | A | B/C/D提供正式服务方法及接入说明，不自行改此文件 |
| BE/service/file/DccControlledFileFinalizationServiceImpl.java、ObsoleteServiceImpl.java、listener/* | A | C提供占用/版本服务；D提供事件消费者；A负责事务调用 |
| BE/service/file/DccControlledFileObsoleteFormEffectExecutor.java、BE/approval/DccApprovalTaskAdapter.java、FE/detail/approval-actions.ts | A | 统一FormCenter、审批中心和详情办理语义，不能快速办理绕过指派 |
| BE/service/route/*、FE/routes/*、FE/categories/components/CategoryMatrixDialog.vue | A | B文件类型变动先给映射合同，不能另改路线规则 |
| DCC流程BPMN相关SQL、BPM候选/会签行为变更 | A提出、主管理登记 | BPM跨模块改动先列影响与测试，不复制旧seed覆盖全部定义 |
| BE/service/projectcode/*、productcatalog/*、对应controller与project对象 | B | 负责人、默认属性、项目文件夹身份输出经G1冻结 |
| FE/basic-data/components/ProjectCodeTabPanel.vue、ProductCatalogTabPanel.vue、ProjectFileTemplateEditor.vue、basic-data/file-type-taxonomy/* | B | 共享选择器嵌入由B调用D提供组件，不让D改这些页面 |
| BE/service/category/DccFileTypeTaxonomyAdminServiceImpl.java及类型controller | B | A审批绑定映射通过合同对接 |
| BE/service/file/DccControlledFileVersionPolicy*.java、DccControlledFileVersion*.java、DccWindchillVersionNumber.java | C | A/D只调用统一策略；不得另写版本算法 |
| BE/service/file/DccControlledFileQueryServiceImpl.java、NameClaimService.java、名称claim Mapper | C | D提交查询投影方案由C或主管理接入；A提供权限/生命周期投影，不各自改Query大文件 |
| BE/service/file/DccControlledFileRelatedFileService*.java、DccPublicationFollowup*.java、DccRelatedFileImpactAssessment*.java及各自controller/关系对象 | D | A保存会签安排、发事件；B提供目录和负责人，不另改关系逻辑 |
| 新增项目引用服务、controller、对象、Mapper及独立文件选择器组件 | D | G1核准命名；UI公共页嵌入由主管理 |
| FE/upload/index.vue、upload/submitter.ts、detail/index.vue、browser/index.vue | 主管理 | A/B/C/D写独立组件/服务与integration-notes，主管理唯一接线 |
| FE/shared/lifecycle.ts、shared/approval.ts、shared/ControlledFileBasicInfoPanel.vue、公共导航 | 主管理 | A定义状态，主管理唯一写入共用映射；各模块提props/显示需求，不重复改公共文案 |
| BE/dal/dataobject/file/DccControlledFileDO.java、MasterDO.java、公共文件Resp/Submit/Approve/Iteration等VO、ControlledFileController.java | 主管理 | 各模块提交字段/API差异方案，统一落地；不可用未经登记的重复DTO绕开 |
| IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts与公共type | 主管理 | 新模块独立API wrapper可由模块维护，公共字段统一接入 |
| IntRuoyiFronted/src/api/dcc/controlledFile/projectCodes.ts、projectProductRequests.ts | B | D使用B公开项目目录及权限合同 |
| 历史迁移、新增迁移编号与依赖、公共测试fixture、数据库结构入口 | 主管理审批 | 模块只新增其登记的迁移文件，不并行修改同一个历史SQL或重写历史业务行 |
| 本任务包、需求HTML、主管理Review报告 | 主管理 | 模块在自身任务目录提出需求/接口变更，不直接改业务基线 |

## 2. 防冲突规则

2026-10-03 G12实际交付归属：Root临时完成引用明细FE五文件及两个独立新文件/测试，后台两个conditional scheduler/recovery及独立context测试，最新明确范围见主任务G12；正式工具无新Agent续派回执时，不将旧completed Agent记运行。detail_closure G11共用selector两源码及已交付测试保持保护。后续backend_closure正式接手批准负责人Java/VO/schema/签名合同，detail_closure正式接批准弹框和workflow/api，Root停止对应生产并写；原负责人功能当前仍未实施。最终Root负责Review、统一验证/运行/本地Git，未验证中间状态不合入int_qms。

2026-10-03 G10：upload_closure 接 UI-02 文件列表直接关联和 UI-05 引用明细前端，独占 ProjectBrowserPanel/project-browser/DccProjectReferences、必要独立弹框及 relations.ts 新只读 wrapper/专属测试；复用 DetailRelationsPanel，不改该组件。detail_closure 接 UI-03，共用 DccFileSelector.vue、独立项目目录loader及选择器测试，必要所属 DetailRelationsPanel 接线，不写browser/upload/basic-data父页。backend_closure 本轮只读复核 Root G09 UI-06 核对步骤和迁移/运行准备，不改前端，若发现问题先报 Root；Java/Maven 后续变更仍需 Root 分派。Root保留需求/总 Review、统一验证、运行和Git；Root G09 ProductCatalog/helper/test 已交付并保持保护。Agent是否实际执行须以正式工具回执证明。

2026-10-03 G07：backend_closure 独占生命周期多受控版本生效、默认最新受控 SQL 过滤及对应后端测试/Maven、暂停 job 注册准备；upload_closure 独占 ProjectBrowserPanel.vue、project-browser.ts、DccProjectReferences.vue、project-reference-contract.ts 及浏览/引用专属测试，接默认筛选、文件名颜色和固定引用正文入口。detail_closure 独占 applicationRead.ts 及其测试，提供 ProjectBrowserOptions，其他公共入口仅只读审查、发现缺口先交 Root 分派。Root 独占需求/合同/总 Review、迁移依赖包、统一前端类型和构建、运行及最终 Git。原四工作树保持交付来源只读。跨引用正文权限复用现有精确 selectedControlledFileId 的 relation-permissions 轻量合同，不新增平行权限接口或猜测 source project 权限。

2026-10-03 G04：backend_closure 正式续跑，独占全部本批后端/Maven；detail_closure 正式启动，独占 detail/revision/relations详情父组件、workflow.ts/applicationRead.ts 和所属测试；upload_closure 正式启动，独占 upload/signoff-departments/submitter 和上传测试。Root 已将本批部门预检/控件及轮次/权限读接口成果移交，不再并写上述源码。上传 shared workflow.ts 类型调整由 detail_closure 接入，不能两个前端 Owner 同改公共接口。Root负责主文档、Review、跨模块验证和最终本地Git；原四树只读。

2026-10-03 G03：backend_closure 已正式启动，唯一负责整合树后端源码、共享 Controller/VO/Query/Mapper/schema 及 Maven，包括完整项目浏览、失败原目标号返工和审核人员配置。Root 作废 readiness 修复已移交，其210次回归证据保留，Root 不并写这些生产文件。前端后续分派以主任务 G03 和正式子 Agent 回执为准；未启动的分派不能记为运行中。Root 保留合同、Review、统一前端类型/构建、运行环境及最终本地 Git 整合。

本轮并行实际接手补记：backend_public_repair替代因工具协议错误退出的backend_public_integration；browser_project_integration在browser之外收口upload/index.vue、upload/submitter.ts和上传测试，Root停止这些源码写入，仅Review/合同/最终验证。detail仍唯一写workflow.ts/applicationRead.ts，共享字段由Agent直接消息协调。

2026-10-02 用户明确改为主管理带三个子Agent并行修复；只在现存整合worktree，原ABCD手动worktree只读。backend_public_integration接后端Controller/VO/placement与实际创建检入候选接线；detail_workflow_integration唯一修改公共detail和workflow.ts/applicationRead.ts，负责申请/指派/下发；browser_project_integration唯一修改公共browser及独立项目组件；Root暂保upload/submitter及主管理文档。每个子Agent独立任务记录，跨字段先消息协调，不创建独立手动线程或定时。此前Owner定义用于原worker交付，本轮整合写权限按此限定分配。

2026-10-02 H08草稿桥接接续：A负责新增单一内部DccWorkingApplicationDraftInitializer（MANDATORY事务），Workflow的prepareApplicationDraft改为委托此服务；C可在自身Query检入实际insert后调用该内部服务，但须等A交付签名/实现并由主管理冻结同步后再接线。该服务不得注入Query或Workflow，不新增HTTP、轮次表或属性表。真实未送审直接前驱调用B inheritReservedDraftToNewApplication；已提交返工仍用真实BPM来源fork；完全新申请才读取当前项目默认。所有权限、来源、锁顺序、重放与晚失败回滚须真实组合验明，不能假接口代替。本次不授权任何worker定时任务或跨Owner代改。

2026-10-01 CC-2小范围接续例外：B可在自身worktree实现Root DccApplicationRoundService的reserveDraft/requireDraft/bindReservedDraft及所属测试、独立前向迁移/H2fixture；C可登记SubmitIterationReqVO/既有前端提交类型的effectiveDate。具体语义和验证见continuation-contract-2.md，主管理统一接收/同步。不得据此改A Workflow或公共上传/详情/浏览页；其他归属不变。

2026-10-02 H04/H05真实公开NEW创建组合例外：A可小范围修改DccControlledFileMasterMapper.selectByNewLogicalIdentity及同逻辑身份锁定方法的deleted列谓词为已有deleted=0数值合同，保留tenant/project/type/number/锁/身份约束；当前H2 first error为旧b'0'语法，不以动态方言fallback/替身/换库绕开。只处理被真实创建路径命中的方法，交Root精确diff和公开创建/晚失败回滚结果，其他共享Mapper/模型/公共页归属不变。

- 独立worktree不消除合并冲突。Owner表约束的是同一文件的修改责任，不是电脑目录。
- 需要改别人的文件时，先交“文件、方法锚点、必要原因、最小差异、验收影响”。主管理选择由Owner接入或在明确阶段重新分配归属，并更新本表。
- 不用新增第二套同义公共对象、双接口、fallback或兼容逻辑来规避文件归属。
- 每次整合前核对基线指纹和最新Owner修改；不得用git checkout/reset覆盖别人改动。
- 公共大页面改动应采用独立组件接入；组件的上传票据、申请快照、版本选择必须来自同一正式上下文，不另复制状态。

## 3. SQL和测试命名

建议任务id：`20260930-dcc-a-workflow`、`20260930-dcc-b-project`、`20260930-dcc-c-version`、`20260930-dcc-d-relations`。新增SQL由主管理登记A/B/C/D顺序及dependsOn，不假设日期排序就能表达真实依赖。

新增测试类采用模块独立名前缀，如DccWorkflow…、DccProjectAttributes…、DccRevision…、DccProjectReference…。共用fixture和公共DO断言由主管理管理。现有测试断言旧业务时，保留原失败事实并按最新合同更新，不能删掉断言来制造GREEN。
