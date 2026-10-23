# LD01 / G46 批准项目产品到上传正式身份

Status: ready_for_closeout — LD01 backend frozen for Root review; actual schema/runtime not executed by agent. 工作区仅主仓 int_qms；legacy_occupancy_core 唯一后端/Java/Maven owner。Root 管理 Git/DB/运行/E2E；前端独立 owner，本轮不修改 FE。

用户明确 HTML 是业务需求，不能将旧 MDM 14 位编号强加新 DCC 产品创建。采用唯一服务器 resolver，显式 MDM_MASTER / DCC_CATALOG 来源；DCC_CATALOG 必须当前 tenant 的 COMPLETED 创建申请、ACTIVE项目产品关系及真实目录ID/code/name一致。已有MDM绑定失败绝不退目录。productMasterId仅真实MDM ID，新来源/provenance字段独立。

- Given已批准新DCC项目产品，When公共project-product预览及DHF上传解析身份，Then真实DCC目录编码/名称和稳定catalog/relation/request身份，不用项目代码代产品编码，无14位新必填。
- Given已声明MDM绑定但失效、modern创建证据缺失/不完整/跨tenant，When解析，Then拒绝，不fallback目录、不接受客户端ID/code伪造。
- Given正式上传/工作版本/升版，When保存/复制产品事实，Thensource和对应真实ID+申请来源及code/name冻结同事务；旧文件/申请/签名不回填。

字段设计：dcc_controlled_file仅新增nullable product_source varchar32、product_catalog_id bigint、product_relation_id bigint、product_create_request_id bigint。product_master_id保留MDM语义。正式preview/API含source/provenance身份，Long输出字符串。纯forward新增列SQL/依赖closure/历史0DML准备；Root审后实际执行，不在此agent连库。

验证：公共预览现态UNBOUND有效RED；真实H2 Mapper/批准Writer产物→resolver→正式preview/上传持久化GREEN，必要Workflow/Owner/Query相关回归，失败事务及身份来源有限负向。legacyMDM截图14规则只其显式来源保留，DCC业务不借它。

## 接口与实现收口

同一GET /dcc/controlled-files/project-product：source严格MDM_MASTER/DCC_CATALOG/UNBOUND。MDM_MASTER只有真实productMasterId；DCC_CATALOG的MasterId=NULL，catalog/relation/createRequest三个真实ID；UNBOUND所有product身份/code/name为空且仅未声明modern资料。projectCodeId与各Long均JSONstring。MDM错误不回DCC；客户端ID/code不参与解析，目录产品保持原业务编码不要求14位。

统一resolver用于上传、预览、当前允许metadata修正；文件保存四来源字段+code/name，query版本复制/currentread保持保存事实，正式revision现有BeanUtils复制保provenance，不当前默认覆盖。metadata切source显式清空另一套ID并核rowcount，原签名/属性/版本不改；现有metadata审计diff/snapshot包含真实来源。

真实目录表tenant_id已存在，但旧DO BaseDO省掉导致批准新catalog写tenant0。Root证实旧613，新增DO mappedtenant属性/Writer显式requiredtenant，只影响新插入；不修历史613，不OR0通过Proof。Proof包含物理tenant/状态/完整批准request反向ID/ACTIVErelation并用Java Objects.equals精确核原code/name，大小写/重音/尾空格漂移均拒；未匹配modern不变UNBOUND。

有效RED1是真实approvedWriter/H2/publicpreview仍UNBOUND；metadata另有效RED1项目码覆盖真实产品码。初期compile/Mockoverload/fixturetenant及FileNOTNULL/lambdacache失败全部分别保留，不称业务RED。最终467/6全0（G46继承7OWNER，前443/33等结果重复不相加），22:29:37完成；六JUnitXML和所有日志rawSHA写verificationreceipt。

准备SQL 20261004_dcc_product_identity_source只四nullable列，rawSHA049d60e2e8f4a7999ebc213cda77154fa78c315bff296f78be127462977a2760；fullclosure9PASS，executiononly新1，另外8只核事实不能重放。离线first/repeat守护合同与3变异拒PASS，不宣称真实MySQL执行。Root具体备份/隔离演练/来源零旧列摘要方案见runtimeplan。

本Agent源码仅MAIN int_qms，不写旧整合树/FE；没有真实DB/Redis/Token/service/package/Git/browser/E2E。RootReview后负责实际四列升级/打包/真正新建产品上传验收。
