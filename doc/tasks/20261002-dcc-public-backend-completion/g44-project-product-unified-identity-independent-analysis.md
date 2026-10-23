# G44 项目创建产品身份与上传的独立分析

2026-10-04；结论：CONFIRMED_CREATED_PRODUCT_IDENTITY_NOT_CONNECTED_TO_UPLOAD。Root已通过真实页面完成申请6→项目270/产品目录/ACTIVE关系，但上传在产品绑定分类的正式guard拒绝；本报告只读正式代码分析原因与最小边界，不改源码/测试，不运行Maven、DB、API、浏览器、服务或Git。

## 确认的合同断点

`DccProjectProductCreateWriteService#writeApprovedRequest` 正式创建的是 `dcc_project_code`、`dcc_product_catalog` 和 `dcc_project_product_relation`：关系保存requestId/projectCodeId/productCatalogId/ACTIVE，项目保存leader/默认属性/目录等；它没有创建MDM product，也不填project.productMasterId。这里的产品编码、名称、分类来自用户已批准申请，不是无产品或假数据。

上传 `DccControlledFileWorkflowServiceImpl#resolveDccProductFromProjectCode` 只接受project.productMasterId指向的 `MdmProductApi#getEnabledDccProduct`。为空时返回null identity；`prepareSubmitContext` 对 `DCC_FVM_DHF_`/`DCC_FVM_DMR_` 分类要求 `dccProduct.id != null`。因此用户刚批准创建的正式DCC产品未被接入，`productMasterId=null` 会在生成正式文件前失败；项目270真实File F1行数0与此断点一致，但本Agent未查询实际库。

前端当前 `applyDccProjectCodeProductNumber` 更把selectedProject.projectCode直接显示为productCode、productMasterId置null。既有正式 `/dcc/controlled-files/project-product` API虽然可读真实project product，当前页面不调用它；它也只返回PRODUCT_MASTER/UNBOUND及MDM身份，无法表示已批准DCC catalog。当前源码检索未找到名为 `resolveProjectProductIdentity` 的既有方法；若Owner要以此命名新增唯一正式resolver，不应把它当已存在合同或复制第二套fallback。

前端 `validateDccProjectProductCode` 当前仅对产品必需分类核非空项目代码，和后端需要有效MDM产品身份不一致；不能继续用前端绿预检证明可提交。服务端必须是身份权威，页面显示应来自同一正式投影。

## 现有两条产品创建合同

| 体系 | 当前正式identity | 使用条件 |
|---|---|---|
| 新DCC联合项目/产品申请 | 已完成request + ACTIVE project-product relation +正式catalog行；catalog产品编码/名称/分类为用户申请值 | 当前链缺唯一正式上传resolver，不能把catalog.id放进productMasterId或假称MDM已创建 |
| 旧MDM产品/onboarding | project.productMasterId → MdmProductApi/getEnabledDccProduct；产品主数据有productCode及独立dccProductCode | ENABLE且dccProductCode为14位字母数字；已绑定失效或错误MDM应明确失败，不尝试另一个catalog成功 |

`DccProductOnboardingServiceImpl#resolveEnabledProductForApproval` 已存在真实MDM创建/绑定流程：申请有productMasterId时取正式MDM；没有时用用户产品编码、DCC产品编号、中文名等创建MDM，再把其真实ID写project.productMasterId。但这条旧入口未带新联合申请的负责人账号/三属性/目录合同，不能让用户改走它来掩盖新主线缺口。

## MDM真正必填字段（不猜制造默认）

`MdmProductSaveReqVO` bean validation必填仅 `productCode`、`nameCn`。正式service `validateAndNormalize` 也要求二者并校验唯一；nameEn/modelSpecification/category为可空，status取既有有效ENABLE合同。**没有制造方式、规格、注册主体等必须填默认值的要求。** 不应为此次接通偷增这些用户未要求字段或填“其他/不适用”。

但MDM的**DCC使用资格**另外要求 `dccProductCode`，`MdmProductCodePolicy`固定 `[A-Za-z0-9]{14}`；普通create允许dccProductCode空，但getEnabledDccProduct随后拒绝它。新联合申请只收普通productCode/productName/classification，没有单独dccProductCode，用户已合法提交的长任务产品/项目编码不满足14位合同。不能截断、hash、补零、按新ID生成14位号码，不把projectCode自动当dccProductCode，不取消旧MDM或截图路径的14位验证。

若业务坚持所有新DCC联合产品必须创建MDM并用于这一旧14位路线，唯一仍缺的明确输入/规则是：**DCC产品编号的14位真实来源（用户另填、选择已有MDM或批准编号生成政策）**。这属于产品编号合同，不是额外代码执行权限；本分析不重复权限确认，也不猜默认值。可以先按现有已确认DCC产品编码合同接通正向主线，避免因未要求的新编号字段改变用户已批准需求。

## 最小接通方向与约束

1. 使用一个server正式项目产品resolver/投影，让新联合申请的**已批准DCC catalog身份**和现有**MDM master身份**明确区分。先核project/tenant/ENABLE，依据真实关系/绑定事实解析其唯一来源，不根据显示名/编码巧合搜索或“MDM失败就catalog”fallback。
2. DCC来源核该tenant/project的唯一ACTIVE关系、真实COMPLETED申请及generatedProject/generatedCatalog IDs、catalog真实code/name/classification与批准事实。缺关系、歧义、deleted、错tenant、未批准或交叉身份都拒绝；不能只为`productMasterId=null`返回一个有名字对象。
3. 保留product-bound分类的“必须有正式有效产品”规则，改为验证准确**source-specificidentity**，不直接删guard或让任意nonnull字符串过。MDM来源继续验证真实master及14位dccCode；DCC来源使用用户批准的普通productCode，不把它塞入MDM dccCode字段或绕过老截图guard。
4. 现 `ResolvedDccProduct.id`/`productMasterId` 的语义是MDM ID。新产品源如果在VO/持久化暴露identity，使用明确source及独立catalogId/relation/request标识；MDM ID仍仅真实MDM值。File保存productCode/productName等版本快照要来自本次正式解析，历史不读今日关系代替，后续checkout/revision同链保留对应事实。具体新增字段/迁移归Owner/Root review，不能跨模块自行定义同名并行identity系统。
5. 既有project-product只读API、上传预检、正式submit及前端展示共用此合同。Long identity字符串投影明确，当前页不得继续用projectCode冒充产品编号；用户修改请求productCode不能改变server产品身份。
6. 回归必须用实际新项目联合创建→批准写入→真实formalproductresolver→上传prepare/create的组合，而不是只对private helper手工造DTO或mock合法MDM。证明已批准普通DCC产品可上传绑定分类；既有有效MDM路径保留、失效MDM不fallback、错relation/批准未完成拒绝、后期事务失败不留下File/属性/关系或成功审计。真实页面再用原项目270/任务自有正文验证，不能直接补其MDM FK绕过证据。

这不是新增平行产品系统：新DCC catalog及批准关系已经存在，缺的是它与公共上传的正式身份连接。如果Owner选择统一到MDM，则须核完整已确认输入/编号来源及实际创建事务；不能让MDM optional字段或旧14位合同被隐式降级。

## 审查依据与边界

读取：新项目产品WriteService及ReqVO、ProjectProductRelationDO/Mapper、DccProductCatalogDO；旧DccProductOnboardingService；MDM API/impl、SaveReqVO、ServiceImpl、CodePolicy；Workflow prepareSubmitContext/previewProjectProduct/product-bound guard/resolve/截图产品校验；前端upload applyDccProjectCodeProductNumber和submitter validation/API声明。

本报告为确认源码分析和修复建议，不是新实现Review或实际测试PASS。只列当前必填编号缺口，没有要求用户确认制造默认或重新授权开发。Root/legacy Owner选择并冻结合同后再按实际源码Review，原已完成项目/目录/OWNER/属性/签名和历史证据不被重建或删除。
