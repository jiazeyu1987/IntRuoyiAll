# G44 项目/产品创建与上传身份主线审查

Status: ready_for_closeout — 有限只读架构审查，等待 Root 业务身份合同决定；本轮无生产/测试修改、Maven、DB/API/浏览器/Token/服务/Git动作。不把该阶段“准备完成”当全业务验收。

## 已证实 P1：创建完成没有上传所需正式产品身份

Root真实页面：项目270来自已批准并完成申请6，DCC ProductCatalog及ProjectProductRelation ACTIVE存在；上传技术调研报告(DHF类别)submit在 Workflow.prepareSubmitContext:2274-2276 因解析product.id为null拒绝，未创建文件/BPM。

源码完整链：

- HTML v1.6 `docs/product/dcc-final-requirements.html:123/131/133/413(AC-01)` 要求联合申请审核批准后“项目和产品创建成功”，目录和OWNER在同事务生成且“项目产品可用”。
- `DccProjectProductCreateWriteService#writeApprovedRequest:59-132` 插dcc_project_code、OWNER、文件夹、dcc_product_catalog和dcc_project_product_relation，再设置COMPLETED。项目builder不设置productMasterId；没有调用正式MdmProductApi。relation仅projectCodeId/productCatalogId，没有MDM ID。
- `DccControlledFileWorkflowServiceImpl#resolveDccProductFromProjectCode:2829-2844` 只project.productMasterId→`MdmProductApi.getEnabledDccProduct`；无该ID严格UNBOUND，不读取DCC catalog。
- DHF/DMR `isProductBoundCategory:2337-2340` 强制正式product ID；因此该联合创建产物无法满足当前正确上传校验，COMPLETED不是业务可用证明。

不得把catalogId放productMasterId（不同表/身份），不得MDM失效时fallback目录，不得取消productbound守卫或回填猜身份。这是目前真实主流程阻断，应先统一写入/读取合同。

## 另一个已证实显示/接线差异

`upload/index.vue#applyDccProjectCodeProductNumber:1484-1490` 总把productMasterId清null，产品编码显示project.projectCode。既有正式GET `/dcc/controlled-files/project-product`/`previewProjectProduct:627-636` 可返回MDM产品ID、dccProductCode/name和source PRODUCT_MASTER/UNBOUND，但当前upload页面未调用它。

即使后端已有正式MDM项目，前端展示仍会把项目代码显示为产品编号。HTML创建明确项目代码、产品编码两字段，并未规定它们相同。后端实际提交重新按project解真实MDM，所以不直接信任前端null；但用户看到的productCode与最终正式文件productCode可能不一致。最小应以正式project-product投影回显，代码/display不从projectCode猜。保持Long字符串，取消/异步更换项目不能携旧产品。

## 当前已有正式创建/绑定能力，不能当现成修复完成

`DccProductOnboardingServiceImpl#approveRequest:77-116` 在自己的@Transactional中调用 `resolveEnabledProductForApproval:140-162`：如已选Master则正式getEnabled；否则用MdmProductApi.createProduct创建MDM，然后读getEnabled，再在新project设置productMasterId。正式ApiImpl:37-43委托唯一MdmProductService，并非平行产品表。

但这个onboarding同时创建另一个项目；已有270不能再以同projectCode建档，validateProjectCodeAvailable拒重名。它也没有新合同folder/defaultattrs/OWNER完整接线，不能用此旧入口替代已确认联合创建。ProductCatalog编辑只改展示行，无MDM字段。项目维护后台update DTO确可写productMasterId，但当前ProjectCodeTabPanel项目维护form只有项目文字等，没有绑定MDM选择；“产品建档”另一个dialog的MDM选择用于创建新项目，并非绑定既存项目。故当前无法宣称已有正式UI一步可修复270，且不能用API/DB承担真实验收动作。

## 关键未确认业务合同：产品编码与14位DCC编号

最终HTML v1.6:123仅产品编码/名称/分类；联合ReqVO只productCode/productName/classification，没有dccProductCode，也无productMasterId。shared-contract没有新增MDM/14位编号规则。

正式MDM创建必填productCode/nameCn，category可从classification清楚映射，nameEn/modelSpecification可空；没有生产方/注册人等额外强制字段。MDM还独立保留dccProductCode，`MdmProductCodePolicy:13-14`/`MdmProductServiceImpl#validateEnabledDccProduct:121-129` 要求用于DCC的它为14位字母数字。联合申请的productCode仅非空可128chars；Root本次真实task长编码正常获批，不能截、补零、hash造14位或把projectCode当它。

因此这是新流程和历史正式产品协议尚未统一，HTML未规定14位，不能说用户漏填已明确字段，也不能擅自把14位加成已确认需求。

Root需先明确唯一产品身份选择（不涉及执行权限审批）：

1. 如果全DCC统一MDM：产品编码仍业务productCode；明确一个独立“DCC产品编号”输入/规则(现行14alnum)，或用户明确产品编码本身即此编号且同步修改产品编码校验。推荐独立字段保留现有两编码语义，避免扭曲用户原产品编码。批准时用正式MDM API创建并将真实MasterID写项目，DCC catalog作为同一产品的目录投影。
2. 如果已批准DCC catalog本身就是此业务的正式产品身份：需明确它与已有MDM是两种显式来源，以统一Typed产品投影而非fallback；MDM声明存在却失效必须拒绝。这个选择涉及受控文件产品引用与既有消费者，范围更大，不应为270临时硬塞相同ID或新增平行表。

本审查不替Root/用户选择，也不改守卫；在既有唯一MDM目标下推荐方案1复用正式MDM服务。新增字段和14位规定必须先确认，不能先实现猜测。

## 方案1最小可Review修复（合同确认后）

- 联合申请增加明确dccProductCode输入/快照/详情(或已确认singlecode映射)。提交前用正式MdmProductCodePolicy校验唯一，冻结该编码，审核后不可由当前默认重解；若已有MDM选择也必须明确选择模式与匹配字段，不通过碰巧重复码偷偷复用。
- 联合Writer原批准同事务调用唯一MdmProductApi.createProduct(productCode, dccProductCode, nameCn=productName, category=classification, statusENABLE)，核实际getEnabledDccProduct；将返回MasterId写新project.productMasterId，保持原catalog/relation/OWNER/folder/completed/真实Gxp全部同事务。
- 任何MDM重复/invalid/zeroinsert/读回错、目录或OWNER或审计失败均整体回滚，现有WRITE_FAILED+明确重试继续；不会部分MDM残留。使用实际同DataSource/事务，不用另线程/REQUIRES_NEW。
- DCC目录表不用拿MDM ID代替自身id，关系沿用project→Master与project→catalog；完成审计快照补正式MasterID/业务编码产品事实。无需新增质量策略/审批节点/默认grant。
- 请求新增字段需独立前向migration ADD nullable dcc_product_code (以及需要时generated_product_master_id)到申请表；project.product_master_id/MDM现有表已存在，无历史DML/回填。旧在途/完成缺字段不猜值，不能默认变READY，后续显式恢复业务另行定义。若用户确认single existing productCode为MDM dccCode，是否可免schema须再核payload冻结/历史，不先宣称0migration。
- 批准人配置仍未确认，不为MDM创建偷偷给申请人/批准人额外角色菜单权限。服务内批准场景复用已有MDMAPI，与直接MDMController需mdm:product:create是不同已授权业务入口；Root确认其业务权限合同，无新自动grant。
- 前端上传调用正式投影，UNBOUND明确提示；两编码显示区分，正式submit仍服务器校验Project/MDM身份。

BDD/验证计划：

1. Given新联合申请明确MDM输入，When真实批准Writer→实际MdmService/Mapper→project/catalog/relation/OWNER/folders，Then同一产品Master身份启用，COMPLETED回执包含它，正式project-product/read准备身份可满足DHF上传守卫；不能只测试catalog insert。
2. Given正文/permission等其余合法，When公共上传Controller→Workflow使用该新project，Then实际Master被解析、申请创建/BPM继续（测试隔离外部BPM不冒E2E）；再由Root真实UI验收。
3. GivenMDM创建后任一晚期失败/auditfail，Then包括MDM在内全部回滚；明确WRITING/WRITE_FAILED和重试保留失败历史。
4. GivenMDM冲突/invalid14/缺新字段或指向disabledforeign产品，Then拒绝/零成功，不能fallbackcatalog；完成重放不再建MDM或改已有OWNER。
5. Given项目代码与产品编码不同，When选择项目/异步切换，Then正式产品回显正确，ID字符串不截断且UNBOUND不提示解析成功。

Migration静态首次/重复/fullclosure/history零DML，真实DDL待Root具体授权；本轮不执行。

## Flow03审核/批准/待办补核

已实现并接通：Controller reviewer-config GET/PUT(文控+update)，创建Service冻结当时审核人ID/name，Review用assertFrozenReviewer；ProductCatalogTabPanel:426-428展示本次审核人，:484-497只有冻结审核人按钮，handleProjectProductAction:1213又校验。不能继续报告“审核人固定admin未修”。

现状：`DccProjectProductCreateServiceImpl#approve:205-216`/retry调用 `requireAdmin:242-245`，其标准为当前用户username恰等admin。FE批准按钮:502-515仅status PENDING_APPROVAL显示；controller批准需project-code:update，后端再admin规则。普通有update用户可看到按钮但实际拒绝，是当前UI资格显示差异；并未已实现可配置批准人。

HTML:138/D02明确“批准人配置与本人兼任限制未在本稿中确定”，所以固定admin是待确定业务项，不能称违反已确认可配置批准人或擅自换默认角色。审核配置已确认，批准配置尚未确认，两者不能合并。

HTML:131“审核人收到申请/批准人进入待办”。当前具体操作是ProductCatalogTabPanel:443-515申请记录列表内审核/批准；正式 `/dcc/project-product-requests/pending` returns状态列表，创建/审核/批准Service链未产生BPM task、通知或统一审批中心todo。因而字面流程呈现差异“申请记录办理，未接统一待办/通知”有明确源码依据，但是否必须统一待办/主动推送而非这个专属待审批列表，HTML未规定统一中心实现；作为待对齐界面/通知合同报告，不虚构未实现审核批准。

HTML Flow03不明确项目创建审核/批准电子签名，不能把文件主线的签名要求自动扩成新产品节点签名平台。本轮只报告现状。

## 证据边界与读时指纹

P1基于Root已真实UI失败+源级完整身份调用链；14位/批准配置属于未确定业务协议；其余前端/服务分析只读，没有执行测试/MDM创建/DB更新。旧申请6/项目270和先前记录不修改。

- docs/product/dcc-final-requirements.html · 88077 bytes · SHA256 `2b854c484b5374ef3ef23680d8a3fa998fcee0eec1ad0ea4d39b558ad3f3f9ad`
- docs/dcc-parallel-delivery/shared-contract.md · 8018 bytes · SHA256 `abc0bb9894a6c4473896c2dde64c45f570d66fb76140bdd701d95ed56274db4f`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateWriteService.java · 10132 bytes · SHA256 `ede03e01218342d39c79255cd78d28a441f50a40a41d87ed8f48dfb731faa879`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java · 18489 bytes · SHA256 `648857b1375995498f88bee42d9704399fcbadf0f11c1486f8b393ea4074a653`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/vo/productcreate/DccProjectProductCreateReqVO.java · 1720 bytes · SHA256 `7467ef0da596f47a6715fb79216d1ec4108b63c8d9b3416d1f189a60488796c7`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java · 213810 bytes · SHA256 `90ba908bc0191c28662a6e8967be7f10d6e61cfbdd874af59775d26f19f7f124`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/onboarding/DccProductOnboardingServiceImpl.java · 13510 bytes · SHA256 `29cf8219f54a3368ab31366ceee4c67fbc0925a4a3c8e02287bcecfc2e86b2a0`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/DccProjectCodeServiceImpl.java · 74596 bytes · SHA256 `4f999650e07efdda015ed451aa698e132d44c6f7c97af2d1861175ba92a118b8`
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectProductCreateController.java · 6616 bytes · SHA256 `06324ea122913383ae432c46458c165924807ad8f433ead606ab1184685cb67d`
- IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/service/product/MdmProductServiceImpl.java · 29569 bytes · SHA256 `56b00a12b702d5eaefbd82348e9a23caa0966a726d8bafefab1353a13fe0b90a`
- IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/api/product/MdmProductApiImpl.java · 2592 bytes · SHA256 `cfa79ad78ad542ee745eed5d48581d243be6a8b08b13a8391aa5978a5aa17799`
- IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/service/product/MdmProductCodePolicy.java · 447 bytes · SHA256 `47fbd4e72a229340a12fb4d41657f10e808de5085c6bd58f05c3861b3bb98193`
- IntRuoyiBackend/yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/controller/admin/product/vo/MdmProductSaveReqVO.java · 548 bytes · SHA256 `6c814d1fd6ca44fd98bda6898b5060b79f1f833b19302614b7747ecfc323bddf`
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue · 114594 bytes · SHA256 `651eb911f93f7f5cb6de5d7cc2d0425e40d4654528c6e3a0cdabf7b011343faa`
- IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue · 73123 bytes · SHA256 `315d9185e45f67e88a2a0e3400465ea057bff752ebdd67cb7b4ba83a575a39b2`
- IntRuoyiFronted/src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue · 152572 bytes · SHA256 `894f5b2d8f72ee7637f5bb6c29e56b9169dc76bb5e26aae5b863053d9354d851`
- IntRuoyiBackend/sql/mysql/20260920_dcc_project_product_create_approval.sql · 3616 bytes · SHA256 `2d60785f65c898218f2d925f6aed89a4d0b670e3aa266224c56f0cf040e6c8a7`
