# G34 基础资料完成审查（AC-18／类型／审批矩阵CRUD）

范围：最终HTML第4、5、6流程与AC-18。分支codex/20261001-dcc-integration；本轮有限静态审查，无浏览器/DB/API/Maven/Git。本报告核入口是否接线及具体错误，不把已有单元测试名字当真实页面验收。Root另授权矩阵后端有限修复，实施证据随后追加，G33冻结不动。

## 已接通的链路

| 业务 | 前端→API→Controller→Service | 已有验证及真实状态 |
| --- | --- | --- |
| 模板新增/编辑/未用删除 | ProductCatalogTabPanel421或ProjectCodeTabPanel825“文件夹模板库”→FolderTemplateLibraryEditor.save/removeTemplate→projectAttributes.ts get/save/deleteFolderTemplate→DccFolderTemplateController `/dcc/folder-templates` PUT/DELETE→DccFolderTemplateService.save/delete | 仅dcc:project-code:update权限，无审批实例；结构及changeReason/审计事务完整；used模板可修改/停用但不能删除。DccFolderTemplateServiceTest核独立项目snapshot、历史保护、权限/审计rollback。真实有权/无权两个账号页面验收未执行。 |
| 项目非空目录删除保护 | ProjectCodeTabPanel826 ProjectFolderTreePanel→ProjectFolderEditor.openDelete/remove、二次确认→deleteProjectFolder API→项目文件夹controller→DccProjectFolderMaintenanceService.delete | 同project/category权限与editor/owner，project→folder锁；child/placement/reference存在拒绝，logical delete不级联文件。DccProjectFolderDeletionCombinationTest已有引用先锁/删除先锁/取消引用后删除/审计失败rollback。真实页面拒删非空尚无E2E。 |
| 文件类型CRUD/已用停用 | basic-data/file-type-taxonomy/index.vue openForm/submitForm/handleDelete（active启停、delete二次确认）→fileTypes API→DccFileTypeTaxonomyController `/dcc/file-type-taxonomies`→DccFileTypeTaxonomyAdminServiceImpl | 删除有child/category/file/projectTemplateItem使用的类型被拒；更新active=false保留ID/映射，resolveActivePath新申请明确拒失效路径。DccFileTypeTaxonomyAdminServiceImplTest.usedTaxonomyCanDisableWithoutDeletingTemplateHistory等已覆盖服务；未真实UI。 |
| 正式三动作审批路线CRUD | controlled-file/routes/index.vue＋RouteForm.vue“动作类型”→routes API→DccApprovalRouteController→DccApprovalRouteAdminServiceImpl.saveRoute/deleteRoute | actionType明确NEW/REVISION/OBSOLETE，单动作版本号、只停用sameaction已生效版本；正式DccControlledFileApprovalRouteAssigneeResolver按action exact读取，缺某动作不会借其它动作。已有DccApprovalRouteAdminServiceImplTest。真实UI CRUD＋提交采用新配置未验收。 |

`selectLatestActiveByCategoryId`本身**已经**通过Mapper.isLegacyActionType过滤传统LEGACY，不是任意动作最新。因此getApprovalMatrix/getReviewMatrixRows/userlookup这几处单个传统读取不应被重复当作新缺陷修改。真实用途：审阅矩阵用于传统LEGACY管理及当前review-matrix访问能力投影；正式routes页独立管理新三动作审批。需要把两页用途说清，不能直接隐藏旧按钮冒功能完成。

## P1：审阅矩阵旧维护误停用正式三动作

触发：某category已启用NEW/REVISION/OBSOLETE正式路线，从categories/index“审阅矩阵”表点击新增/编辑→CategoryMatrixDialog.submitForm保存，或点击删除确认。

证据链：

- CategoryReviewMatrixTable.vue103–122新增/编辑/删除按钮，CategoryMatrixDialog.vue表单无actionType；fileCategories.ts405保存、445删除 `/dcc/file-categories/{id}/matrix`。
- DccFileCategoryController.java300/308分别调用matrixAdminService.saveApprovalMatrix/deleteApprovalMatrix。
- DccCategoryApprovalMatrixAdminServiceImpl.persistApprovalMatrix1080起route未显式actionType；SQL实际既有column NOT NULL DEFAULT LEGACY，因此新route为传统范围；1087起却遍历samecategory全部active旧route，不限LEGACY，全部停用。deleteApprovalMatrix262起也不限actionType。
- 正式resolver.resolveRoute约113按selectLatestActiveByCategoryIdAndActionType读取，缺route抛CONTROLLED_FILE_ROUTE_NOT_CONFIGURED无fallback。故旧审阅保存一次能破坏后续上传/升版/作废全部申请。
- getActiveMatrixPositionIdsByCategoryIds约207批量查询只category+active，不限LEGACY且不核生效时间，可能把正式actionroute当传统矩阵投影。单个selectLatestActiveByCategoryId正确，不应一起改成新fallback。

现有MatrixAdminServiceTest.deleteApprovalMatrix_deactivatesActiveRouteAndRemovesReviewApproveRules只创建传统route，没混入3个typedroute，因此证明旧删行为却不能证明动作范围隔离。需要真实H2有效RED：保存/导入/删除LEGACY后typedroute active/版本/node/snapshot保持原payload、exactresolver仍可选；批量read不从typed推断；并发同scope版本唯一和latefailurerollback。

最小修复合同（Root批准）：此旧endpoint用途**显式LEGACY**，不添加新平行流程、不借任一动作fallback。category锁内版本仅LEGACY；save/import/delete只传统同category范围；传统批量projection只LEGACY且已生效；保持正式三动作及历史snapshot/权限记录。新actions仍由正式routes页管理，UI明确用途。若历史实际NULL/空action，先核正式20260921迁移及库现状，既有传统协议不等于擅自回填或新增降级。涉及生产Owner文件：MatrixAdminServiceImpl、必要category/routeMapper及定向tests；VO/Controller无需先扩actionType，正式typedresolver/SQL迁移/旧历史/G33不改。

## 验收脚本缺口

G29/G30/G31九模式仅project-product、upload-submit、native-task、training、version、relations、reference、distribution、obsolete。project-product选择已有folderTemplateLabel，但不维护模板；九模式没有模板权限拒绝、非空目录删除、类型CRUD停用、矩阵/routes CRUD。其准备回执明确actualBrowserInvocations=0；目前覆盖代码不等于AC-18完成。

仓库旧`tests/e2e/dcc-review-matrix-tab-real.e2e.js`有UI配置矩阵，但固定另一个测试租户/aoteman/category907178、部分直接fetchJson调用matrix/effective-preview，不能直接用于本轮芋道源码/admin任务自有资料验收，也不覆盖与三动作路线隔离。`dcc-project-folders-unit.spec.js`仅VM转译组件/树状态；文件名带e2e目录不代表实际页面。

最小新增真实页面scenario：

1. **template-permission**：任务自有模板，具有update权限账号新建/编辑保存→刷新与新项目snapshot；无权限现有账号按钮不可办且前端不能保存，不临时猜/授角色；确认无审批任务新增。仅页面动作，DB/API只读核验。
2. **folder-nonempty**：任务project＋页面新child/实际文件placement/reference分别让删除二次确认后报已使用并保留目录/文件；任务空目录可删除；已取消引用边界用正式取消按钮，不APIseed。
3. **taxonomy-used-disable**：页面新建任务类型→映射/创建真实任务申请采用该类型→删除被拒→编辑停用→旧记录/映射仍可见且新upload候选不可选；另未使用leaf删除成功。
4. **matrix-action-isolation**：routes页创建同category三动作、保存传统审阅矩阵并刷新→三正式route状态不变；修改/导入/删除传统矩阵后typedpreview/真实新upload/升版/作废仍用正确各动作；单typed修改仅改变未来该动作，历史snapshot不变。需要本P1代码先修后执行。

应做独立`basic-data`真实DOM脚本/contract，不能硬塞已有九模式去修改已封存其语义。当前只给具体scenario缺口，本轮未编写/执行脚本、更无真实业务PASS。
