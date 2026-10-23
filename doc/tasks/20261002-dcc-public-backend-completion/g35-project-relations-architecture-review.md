# G35 项目、目录、关联与引用主线架构复核

2026-10-04；状态：SOURCE_REVIEWED_ONE_CONFIRMED_MAINLINE_GAP。仅只读复核正式源码、最终需求27项及已有 A/B/C/D 交付说明，新增本报告；未执行数据库、浏览器、Maven、Git、服务操作，也未修改生产文件。当前 worktree HEAD 文件指向 `codex/20261001-dcc-integration`；未运行 Git。

结论：已配置正式访问规则的项目，其目录、申请属性、关联、引用和取消引用的公共调用链都已接入。**新建项目批准完成后，没有初始化任何项目访问规则，也没有可达的首次配置入口，导致后续目录/上传主线断开。** 这是一项确认的源码缺口，不是实际页面复现或运行验收结果。

## 一项确认的主线缺口

### G35-BD-01：新建项目批准后不能通过公共页面进入项目和配置其权限

业务触发：从产品目录新建项目、选择正式项目负责人、目录模板及三组默认属性，完成审核和批准，然后进入该项目目录上传文件。

正式创建链已经调用：`ProductCatalogTabPanel.vue#submitProjectProductRequest` → `projectProductRequests.ts#createDccProjectProductRequest` → `DccProjectProductCreateController#create` → `DccProjectProductCreateServiceImpl#createRequest/createPendingRequest`；批准后调用 `DccProjectProductCreateWriteService#writeApprovedRequest`。后者在事务内创建 ENABLE 项目，保存 `projectLeaderUserId/defaultAttributesJson`，生成目录，创建产品和项目产品关系，将申请置为 COMPLETED。

但这个写入方法不创建 `dcc_project_access_rule`。全模块 `src/main` 的正式权限规则写调用仅有 `DccProjectAccessServiceImpl#replaceProjectAccessRules`，由 `DccProjectCodeController` 的 PUT `/{id}/access-rules` 调用，没有项目批准后的调用或事件消费者。选定唯一负责人字段与 OWNER/EDIT/VIEW 授权是独立合同；当前实现也没有把 leader 或 admin 自动当项目 reader。

后续公共页面全都使用受项目规则约束的读取：

| 后续入口 | 实际调用 | 新项目规则为空时的结果 |
|---|---|---|
| 项目代码基础数据列表 | `ProjectCodeTabPanel#getList` → `getProjectCodePage` → GET `/dcc/project-codes/page` → `getReadableProjectCodePage` → `listReadableProjectIds` | 该项目不在列表内 |
| 项目目录/引用入口 | `ProjectBrowserPanel` → `ProjectBrowserState#loadProjects/selectProject` → 同一项目 page/detail，随后 GET folders | 无法选择项目或读取目录 |
| 通过产品链接打开项目 | `ProductCatalogTabPanel#openLinkedProjectCode` → `/mdm/project-code?projectCodeId=...` → `ProjectCodeTabPanel` 路由详情同步 → GET `/dcc/project-codes/{id}` → `getReadableProjectCode` | `assertProjectViewerOrAbove` 拒绝，不能打开项目详情 |
| 现有项目权限编辑 | `ProjectCodeTabPanel#openProjectAccessRules` | 只接受已读列表 row 或已读 `selectedProjectCode`；没有输入正式新项目申请身份并首次配置的独立入口 |
| 上传选择项目 | 公共 project page/defaults/folders/正式创建前置 | 选不到新项目；即使只提供 ID，正式 viewer/editor 与分类守卫仍拒绝 |

申请完成记录只展示状态、冻结负责人、模板和属性等事实，没有打开首次项目访问配置的按钮。产品链接最终仍进入同一受 reader 规则保护的详情，不能解开这一循环。类别管理权限、菜单 query/update、超级管理员 permission 和 `scope:all` 都不替代 `dcc_project_access_rule`；`scope:all` 只去掉既有 assignment 硬范围，正式项目规则仍必需。

精确影响：新建并批准的项目、负责人和生成目录可以已存在于数据库，但它们无法自然进入后续公共主线。已有带合法访问规则的项目不受这项缺口影响。项目创建独立测试验证对象/目录/审计保存，其中 `DccProjectAccessService` 被隔离为 Mockito bean；这类测试不能证明创建后正式 reader 接通。

收口边界由 Root 决定：必须使用已有正式项目规则和明确的授权事实。可以在已批准申请的完成上下文接入首次访问配置，或经明确业务合同将被选定负责人登记为正式规则主体；本次审查不自动选择 OWNER/EDIT、不授予申请人或 creator 默认权限，不引入认证旁路。**已配置项目规则允许上传，并不等于所有 OWNER 都是引用负责人；D 的引用检查仍必须保存唯一 leader。**

建议有效组合验证：Given 使用真实项目创建写入、真实 ProjectAccessService/Mapper、正式项目 reader 及目录服务且没有预先插规则，When 批准后按已确认的首次授权合同继续页面主线，Then 正式负责人/实际上传人可读取该新项目及模板生成目录，并能进入合法上传；验证授权主体和 accessLevel 来自明确合同，不能以 mock 许可或后台直接补库代替。

## 已接通的主线调用及边界

下列是本次沿实际 caller 确认的源码接通证据，均不是实际运行 PASS。

| 需求部分 | 公共页面 → 正式服务链 | 复核结果 |
|---|---|---|
| 项目默认属性与模板生成（AC-01） | ProductCatalog 创建表单、正式账号/模板/属性确认 → project-product 公共 API → frozen reviewer/request → `writeApprovedRequest` → `DccFolderTemplateService#generate` | 保存字段、审核身份及目录生成有实际 caller；目录使用冻结模板结构而非今日模板。唯一未接通是上面的首次项目访问规则 |
| 项目目录读取与维护（AC-15/18） | 公共 ProjectBrowser/ProjectFolderTreePanel → `getProjectFolders` → `DccFolderTemplateService#readProjectFolders`；独立 folder editor →正式 maintenance Controller/service | 正式逻辑 folder 与 NAS directory 不混用；已有授权项目可取得真实空目录；不以文件列表反推目录 |
| 上传属性、位置与关联（AC-02/04/06） | upload/index 挂 `ProjectApplicationAttributes`，选择项目默认、编辑实际值与确认 → submitter 发送 actual、projectFolderId、relatedControlledFileIds →正式工作稿/提交 →`DccPublicUploadPlacementService#create`、B placement bind、A draft initializer、B snapshot bridge、关联绑定 | 不是孤立组件；属性、逻辑位置与正文创建在正式事务内接入。请求不信任客户端 defaultSource，服务保存服务器项目来源及实际值两份快照 |
| 升版与返工属性（AC-03/04/11/27） | detail/index 挂 `DetailApplicationPanel`；选定正文读取 working/replacement attrs，提交命令 actual → C candidate →`DccWorkingApplicationDraftInitializer`→ B reserved snapshot；A `freezeApplicationAttributes` 绑定真实 BPM 并冻结 | 新升版从项目默认创建工作稿；已保存工作稿/返工复制其来源与实际值，不重读今日项目覆盖历史；所选较早正文 ID 与实际保存值有正式 caller |
| 作废独立属性（AC-03/20） | detail/index 挂 `DetailObsoleteApplication` →项目默认与本次 actual →`obsoleteControlledFile`→ `DccControlledFileObsoleteServiceImpl` 绑定 OBSOLETE round、begin/save/freeze | 作废快照独立于原 UPLOAD/REVISION 历史，原受控版本属性不由此改写 |
| 属性历史（AC-04/27） | detail/index 挂 `DetailApplicationHistory` → `ApplicationEvidencePanel` → application-rounds/application-evidence → `DccControlledFileQueryServiceImpl#getApplicationEvidence` | 以真实 file/type/BPM 的 round link 查 frozen defaultSource/actual 与同轮签名；没有用当前项目默认填历史 |
| 上传与已存在文件关联（AC-06/14） | 上传 `DccFileSelector purpose=relations` 仅保存待提交选择；正式 A 绑定历史快照。项目列表关联按钮 → `ProjectFileRelationsDialog`→`DetailRelationsPanel`→current/history/replace API →`DccControlledFileRelatedFileServiceImpl` | 待提交选择与正式当前集合没有混写；公共轻量关联入口有实际 caller，详情权限读取与正文权限分别校验 |
| 当前关联最新受控、历史固定版（AC-12/14） | `getCurrentRelationView/listCurrentRelatedFiles` → stable relatedMasterId →`DccLatestControlledFileResolverImpl#resolveLatest`；history → stored relatedControlledFileId/snapshot | 当前使用 `Master.latestControlledFileId`，没有 currentActive fallback；未来受控版返回 pendingEffect。历史接口读取固定 file/snapshot，前端 history pane不走最新选择器 |
| A 受控事件 → D 当前关系/整改（AC-12/13） | `DccControlledFileLifecycleService` 保存 CONTROLLED event →同步 `DccRelationControlledEventConsumer` → `DccRelationRemediationService#recordControlled` | 在原受控事务核对正式 event/file/Master/round；当前 source 集合提升为实际最新受控源，ACTIVATED 不代替 CONTROLLED；安排来自指派而不是引用或默认 requester |
| 引用与取消（AC-16/17） | browser/index 实挂 `ProjectBrowserPanel`→ `DccProjectReferences` callbacks→relations wrapper→`DccProjectReferenceController`→`DccProjectReferenceService`→B `DccProjectReferenceAuthorityImpl` | D 两个写入口都调用 B `DccProjectLeaderService#assertProjectLeader` 及真实 folder 归属，非任意 OWNER/admin；cancel 只删精确 reference，保留源文件及关联 |
| 引用固定版和计数（AC-16/17） | reference row 保存 selectedControlledFileId；读/预览/追溯重新查该固定版；`DccRelationStore#referenceProjectCount` | 按 DISTINCT project 计数，不按 folder；固定引用版与当前关联自动最新分开，未擅自采用待定“引用自动跟版”建议 |
| 引用不授编辑/检出（AC-10/16/17） | saved reference 只提供固定版正文浏览/只读追溯，后端 read permissions不写授权；普通检出/检入/升版走 Query/Revision 正式 guard | 引用写 service 只写 reference 与审计，不新增 project access/分类权限；源文件编辑仍校验源项目 EDIT/OWNER、分类 UPLOAD、requester/基线资格及持锁 actor |

这里默认来源与实际值的边界按现有 server draft 合同判断：页面预览 defaults 不允许伪造为已保存来源；正式 File 创建时立即 `prepareDraft` 保存来源，后续 ordinary save 只改 actual，送审 `submitReservedDraft` 冻结并绑定真实 BPM。没有把未保存页面表单当已保存草稿或宣称实际库快照已验明。

## 原四个交付与当前整合的关系

读取原 A/B/C/D integration-notes、B/D verification 中的接口归属与历史待接入说明，并对照现有整合 caller。B 历史“公共属性组件尚未挂载”、C 历史“缺唯一 draft initializer”、D 历史“公共关联/引用待父页面接线”等，不能用作当前断开证据：上述当前源码已实际挂接。原交付中的独立测试、依赖接收或 task blocked 标题也不证明公共业务运行完成。

最终27项中，本报告聚焦 AC-01 至 AC-06 的项目/属性接线、AC-10 至 AC-18 的相关模块边界、AC-20/27 的独立快照与历史；审批完整顺序、签名执行、日期调度、全名判重、真实 E2E/迁移/审计执行由 Root 及其他 Owner 复核。未扩展负向账号矩阵、页面文案或待定权限规则。

## 审查限制与文档验证

唯一确认的主线源码缺口为 G35-BD-01。除此之外，本次范围未确认新的孤立接口或跨模块语义矛盾；这不表示27项全部完成，更不表示正式环境成功。

本次没有真实账号、页面、数据库行或运行服务证据。Root 最新 Jar PASS 是包装证据，不改变新项目权限缺口。报告新增前沿用现有 backend task、读取 task-closeout 规则，未改该 task 状态或其他 Agent 文件。结构验证应核对每个锚点存在、G35-BD-01 触发与下游阻断完整、源码指纹仍相等；报告纳入 Cleanup Keep 由 Root 统一处理。

## 审查时源码指纹

原始 bytes SHA-256；仅标识本次读取的版本，不声明整合树整体被冻结。下游修复后应依据新源码重新判断 G35-BD-01。

| 文件 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateWriteService.java | 2f2577ab5e49ee8064da871d37da2d8cf55beb9f472b5122e3bd0c6190efcb11 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java | 648857b1375995498f88bee42d9704399fcbadf0f11c1486f8b393ea4074a653 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java | 4f999650e07efdda015ed451aa698e132d44c6f7c97af2d1861175ba92a118b8 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectCodeController.java | b70278c405771be08bb1ab68c099c4973194d7b67a2b43d6c8bfb9aa8d860fb3 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java | 46007fa908119d3389e47ef6b4d6dc01e97e421fc2318a36bb4d550c606a6295 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/DccProjectLeaderService.java | 534c51ec04a11fde06ca34263eb1b04a0a5ce300328b8022ea21824f67ee8f91 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/DccProjectApplicationSnapshotService.java | dfd0181413b9cf30827e48b1ea516d90993b648a6700e055bfb036d2727695b3 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/DccProjectAttributesService.java | 3f1c2e618e7449d152cbffba5769c6d8d8c3615f08e1c198711c876c2ec2604e |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/folder/DccFolderTemplateService.java | f2a4e4412ccd3e018738d6e0f8e7028c39474d989a9dbcbb44ca1c0835143b3e |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkingApplicationDraftInitializer.java | 38282b3a36a512eb60c96e340867822349e6def1a84c7a1488be3e75b1c1318c |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicUploadPlacementService.java | 18bef00047fcef89f7f701d168e0e2147bab8a9b91d6f2e4fcac7b54d1400492 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRelatedFileServiceImpl.java | 52aefca10c657654ec3d87a6fd9131d602cf982ec4ff55afbe0ec9cb4d707759 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccLatestControlledFileResolverImpl.java | 6eca1595d875e721fc588f12a5e075b183e5a595ee61a714eebf0362fdb1a363 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccProjectReferenceAuthorityImpl.java | 1622fc7be0784105a78ef54f1c86fa17411649394b53596aa299c21fce3e56de |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccProjectReferenceService.java | 54af3d934cef1b3849282359e22c618ce468de9e9d0ea1e612b9023653b4f224 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationStore.java | 5efa8c81c1f9e2e38be9e22927ebe23e58af21b6112c1331170137e1cef0e0f6 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationRemediationService.java | 3fe9665a9cd6fe136636071a7779744cb34f7d3921d3a21065686410ec72e485 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccRelationControlledEventConsumer.java | 29167f9eed41c727bf88a503af0d30b3de110e0b9460f55d91043cb9904d986d |
| IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue | 315d9185e45f67e88a2a0e3400465ea057bff752ebdd67cb7b4ba83a575a39b2 |
| IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue | 894f5b2d8f72ee7637f5bb6c29e56b9169dc76bb5e26aae5b863053d9354d851 |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectBrowserPanel.vue | eca0a143d55e6b117372f29049c648b7027633390e5d711294b6ebbe8932921f |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/project-browser.ts | 491b1902c9835ae3a20b16baa766475de201c9c217ead0d5aae7292c18c6b7eb |
| IntRuoyiFronted/src/views/dcc/controlled-file/browser/ProjectFileRelationsDialog.vue | 9dac8a534747683c58cb63669b63df24b681cf45fe10f121d884e7a8081578de |
| IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue | d31ce1251326ae2121ca51afaef55fd918de6f7f1494e91ee2ad53dca341a184 |
| IntRuoyiFronted/src/views/dcc/controlled-file/relations/DccProjectReferences.vue | e96d7f77377015ea66c13bfea761dda8100311cf5e728947261cfd620a6ee5a0 |
| IntRuoyiFronted/src/views/dcc/controlled-file/relations/project-reference-contract.ts | e1728649f4504954f6d74823a42ffa1adb2e8550f9ff23eb3fedc834395677fd |
| IntRuoyiFronted/src/api/dcc/controlledFile/relations.ts | 5b405616c8d0e719efdd34faa598269c5f2bda5722a8ffd2c62687bfdccd10fd |
| docs/product/dcc-final-requirements.html | d6a62a4c4b9c1d191042fca4cfc0a66fee622622a1bdcecad48646bb3cd4f823 |
| docs/dcc-parallel-delivery/shared-contract.md | b33b8eb7fe52957a2623babcd0030ebcee4f8f048b81c27d2b3f4d534716a0c1 |
