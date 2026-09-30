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

- 独立worktree不消除合并冲突。Owner表约束的是同一文件的修改责任，不是电脑目录。
- 需要改别人的文件时，先交“文件、方法锚点、必要原因、最小差异、验收影响”。主管理选择由Owner接入或在明确阶段重新分配归属，并更新本表。
- 不用新增第二套同义公共对象、双接口、fallback或兼容逻辑来规避文件归属。
- 每次整合前核对基线指纹和最新Owner修改；不得用git checkout/reset覆盖别人改动。
- 公共大页面改动应采用独立组件接入；组件的上传票据、申请快照、版本选择必须来自同一正式上下文，不另复制状态。

## 3. SQL和测试命名

建议任务id：`20260930-dcc-a-workflow`、`20260930-dcc-b-project`、`20260930-dcc-c-version`、`20260930-dcc-d-relations`。新增SQL由主管理登记A/B/C/D顺序及dependsOn，不假设日期排序就能表达真实依赖。

新增测试类采用模块独立名前缀，如DccWorkflow…、DccProjectAttributes…、DccRevision…、DccProjectReference…。共用fixture和公共DO断言由主管理管理。现有测试断言旧业务时，保留原失败事实并按最新合同更新，不能删掉断言来制造GREEN。
