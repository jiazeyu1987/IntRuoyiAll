# G39 项目负责人初始 OWNER 独立审查

2026-10-04；状态：NO_OPEN_FINDINGS_IN_DELEGATED_MAINLINE_SCOPE。本次独立审查未发现需要返修的新 P1/P2。G35-BD-01 的新项目创建后缺访问规则问题已在所审源码中修复；不宣称实际页面验收、部署或最终合入完成。

审查范围严格为冻结 manifest 的4个生产文件和1个专属测试，及其既有 Controller、事务、Mapper 调用证据。已读 AGENTS/task-closeout，沿用现有 backend task，只新增本报告。未改源码/测试、未运行 Maven、未访问数据库或对象服务、未操作浏览器、Git或服务。

## 具体核验

| 边界 | 实际源码与测试依据 | 结论 |
|---|---|---|
| 创建主线实际接通 | `DccProjectProductCreateWriteService#writeApprovedRequest` 创建项目后，真实注入的 `DccProjectAccessService` 调用 `initializeApprovedProjectLeaderOwner`，之后才生成目录/产品/关系/COMPLETED及完成审计 | 不是未调用 helper；新增规则与生成资产同事务 |
| 同一物理事务 | writer 为 `@Transactional(rollbackFor=Exception.class)`；另一个 Spring service 的初始化方法是 `Propagation.MANDATORY`，没有 REQUIRES_NEW 或自调用绕过；`DccProjectProductAuditService#append` 同样 MANDATORY，正式 GxpAuditService append 以 REQUIRED 加入原事务 | 规则不能在独立事务先提交；专属组合使用真实服务和Mapper，失败后查询资产全空。BaseDbUnitTest 本身没有包住每项测试的 @Transactional，因此不是测试总回滚掩盖结果 |
| 选定负责人准确且仍可用 | initializer锁精确项目；核当前tenant、项目ENABLE、保存的projectLeaderUserId等于入参、正式目录用户ID相等/同tenant/status0，reason有效 | 不从负责人文本、申请人、审核人或admin推断授权主体；停用/外租户在组合测试中拒绝且没有残留规则 |
| 唯一初始规则、不覆旧配置 | initializer要求当前项目正式规则列表为空，仅创建一行 tenant/project/USER/selectedLeader/OWNER/active=true，并检查insert=1且返回真实ID | 非空规则明确失败，不delete/replace/upsert；新项目有唯一初始规则，而“所有OWNER都是引用负责人”没有被引入 |
| 没有未授权公共初始化入口 | 全dcc src/main的初始化方法引用仅interface、impl及writer唯一caller；Controller没有初始化映射。批准/重试仍经过既有project-code:update权限和正式批准身份/状态检查 | 方法是内部服务端口，不能经新增HTTP给现有项目授OWNER；没有放宽reader或认证旁路 |
| 完成审计含真实ACL | `DccProjectProductAuditService#snapshot` 在COMPLETED分支读取已生成project/product/relation/folders，并新增实际projectAccessRules列表，按当前tenant筛选；writer完成状态持久化后调用原 `dcc.project-product.complete` ledger | 审计保存当时实际规则而非推导字段或今日以后配置；未新增假质量批准/审计策略；专属成功测试查真实ledger的afterState包含projectAccessRules/OWNER |
| 晚失败回滚 | 专属组合模拟实际审计append失败、目录insert异常、OWNER insert0、关系insert0、完成update0；各次调用writer真实代理后读取project/product/folder/relation/ACL/ledger均空，原request仍WRITING且generatedProjectId/completedTime为空 | 没有留下看不见的completed项目或独立ACL。公开wrapper失败后走既有WRITE_FAILED记录，与本次生成事务回滚是不同事实，不被写成成功 |
| COMPLETED重放保护 | writer先拒绝status非WRITING，再进行所有资产/规则动作。专属测试完成后把正式规则改为VIEW，再重放writer | 保留既有STATUS_INVALID拒绝，规则字节和ledger计数不变；没有重新初始化、覆盖后续规则或假成功返回 |

初始化方法是新增内部public接口方法；真正执行路径只有已授权创建writer。MANDATORY负责事务约束，审批/身份/状态由已有上层公共调用校验。本审查没有把MANDATORY当作账号权限，也没有要求它改为独立公共授权功能。

## 保存的验证证据

manifest `doc/tasks/20261002-dcc-detail-integration/g39-owner-delivery-fingerprints.json` 的当前SHA与verification receipt所指值相等，全部5资产bytes/SHA吻合；GREEN和regression保存日志SHA也吻合。本 Agent 未重跑这些测试。

实际保存日志：8类154项、0失败/错误/跳过、BUILD SUCCESS，与receipt的Maven exit0一致。其中7项 `DccApprovedProjectLeaderOwnerTest` 使用真实 writer/AccessService/FolderReader/Mapper/H2/GxpAuditService，外部账号、角色与菜单等目录端口明确隔离；未mock AccessService放行。成功项证明选定leader可按正式reader取得新项目目录，申请人/审核人/批准人没有自动授权；其它项证明上表失败回滚和完成重放。有效RED是原writer完成资产但无ACL，leader读取目录 ACCESS_DENIED；初次夹具失败单独留证，不被算作业务RED。

7项不是对所有生产边界逐项真实页面验证；初始化“已有规则不空拒绝”及MANDATORY约束本次以代码/真实调用链审查，不夸大为另有专门运行用例。运行Jar需Root按最新源码重新包装核对；现有源码结论不能替代运行来源证据。

## 四个生产源码指纹

| 文件 | SHA-256 |
|---|---|
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessService.java | 2298d18acdcc7fd688c60e8b4ec7b6d3e3ff837000f8af67bc3923202c4c32f3 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java | db1c822c7242b8c91ddba1aadbd42d26ac5747e0db1f23d3192c4c678aa95192 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateWriteService.java | ede03e01218342d39c79255cd78d28a441f50a40a41d87ed8f48dfb731faa879 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductAuditService.java | b1a9b73970e444b7226d41d58da2729fd11f304ab5df86c2483e416dd9f42f44 |

专属测试SHA：`8fd2fe52eb777c0b6924098765be79f16ea8f7553a746ed19e457912ec64cde9`；manifestSHA：`f0d435ee7acd9ccde8a7b10bf53c11f3a41db3e872e22068af95032bd06e6609`。源码或manifest变更后重新核对，不复用本报告结论覆盖新版本。

## 已完成对象执行的事实边界

本轮只读核已存在安全JSON收据，没有重新GET/PUT。Root保护目录 `g39-object-recovery/receipt.json` 为RECOVERY_PASS、exit0、acceptedUniqueKeys3，其SHA为 `b1dcacd763c47d7a7275537a6eeb0b59aea382954e9821f0c5bbc08ae0d7b327`；`g39-all39-source-bytes/receipt.json` 为SOURCE_BYTES_VERIFIED、readerExit0、objectsRead39/matches39、无DB/对象写，其SHA为 `bb3f2ff2fdaf0d77219f884f771ad6222b54c7d13a267d4e15f88f9fc03e686f`。对象执行已完成，无须继续准备或重做恢复。

上述对象收据不证明项目OWNER页面路径，也不构成实际质量批准。本报告未更改任何授权状态、其他Owner文件或master任务状态。文档结构与4源码指纹检查后交Root收口。
