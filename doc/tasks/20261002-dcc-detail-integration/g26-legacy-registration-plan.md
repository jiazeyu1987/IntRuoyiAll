# G26 历史原名登记与配置审计最小执行合同

2026-10-03；当前 `prepared_for_root_review_not_executed`。本轮只交付核心Java/独立空表DDL/隔离回归。内部注册service/严格sealed factory已经实现，无REST/Runner、无实际激活SQL；Root真实39对象未全MATCH时禁止生成可执行登记DML。

## 核心实现和实际库边界

`20261003_dcc_legacy_source_name_occupancy.sql`只新增3空表，不参加冻结19SQL、不修改旧File/Master/claim/签名。完整dependsOn闭包由独立review证明8项policy PASS；Root实际MySQL8首次/重跑仍未执行。所有表必须实际InnoDB DYNAMIC且page size16K、完整binary索引1040bytes可用；IF NOT EXISTS遇同名错shape必须由exact postflight拒绝，不能把幂等skip当成功。

Root实际确认tenant1旧25claim新增project/leaf/normalizedNumber三字段全部NULL；19个Master正式三字段齐全、6个不齐。登记不回填这些字段、不猜项目/编号，6个旧Master仍须按原C正式前置拒绝新升版/检入。未来任意其他scope若旧claim已有非NULL完整编号、或旧claim原名非NULL，不使用本批迁移/登记合同：记录BLOCKED并另行具体设计，不放宽现代unique。

## 必需的独立配置审计操作（源码已实现／实际策略尚未批准及登记）

| 字段 | 提案 |
| --- | --- |
| operationId | dcc.controlled-file.legacy-name-occupancy.activate |
| sourceType | SERVICE_METHOD |
| sourceLocator | cn.iocoder.yudao.module.dcc.service.file.DccLegacySourceNameRegistrationService#activateVerifiedScope |
| domain / subjectType / actionType | DCC / DCC_LEGACY_SOURCE_NAME_SCOPE / ACTIVATE |
| reasonPolicy / signaturePolicy | REQUIRED_CATEGORY_AND_TEXT / NOT_REQUIRED |
| statePolicy / retentionClass | ABSENT_TO_PRESENT / GXP_CONTROLLED_DOCUMENT |
| testIds | LEGACY-ACT-01、LEGACY-ACT-02、LEGACY-ACT-03 |
| owner / applicability | dcc-owner / GXP |

该sourceLocator现有正式内部方法和literal GxpWriteOperation annotation。原冻结策略实测coverage RED（新operation未注册）；独立prospective候选实测GREEN，operations34/annotations12/reportSHA3acca207c875de851a1e645dec083a82311bdccd98523f1d48465e728d9e394d。原policy原始bytes未修改。原25待质量批准操作均不是这次scope登记语义，不能借publish/projectconfig或原质量批准ref作为该操作成功。RootReview批准有限writer实现后，新增真实method+GxpWriteOperation，并据正式来源重生成sourcecoverage与候选policy hash/范围；不修改原25清单来假装已批准。本轮实际QA回答“尚未批准”，所有质量生效登记保持阻塞。

NOT_REQUIRED仅指该技术登记本身不伪造电子签名；策略生效仍按现有部署规则取得真实质量电子签名。名称历史口径用户决定、质量策略批准、技术配置执行是独立事实。

## 合法writer与真实执行身份

本轮按Root分配仅实现内部注册service和server-onlysealed artifact factory，**没有Runner/REST/token plumbing**。未来执行入口由Root另审；必须使用已有真实认证上下文，缺上下文立即拒绝。服务重查实际启用同tenant账号、真实doc_control和dcc:controlled-file:update权限、LoginUser账号/昵称一致；不构造admin、SYSTEM_ACTOR、不在manifest携带凭据。构造JdbcTemplate和环境查询helper均使用正式同一DataSource，Spring物理事务统一；不连另库。

`GxpAuditService.append`在相同物理事务、真实LoginUser上下文中登记该operation，返回的正式eventId/sequence/hash写入执行回执；不直接INSERT系统GXP事件或手工编造哈希链。策略未登记、未获真实质量生效依据、append失败均rollback全部scope/evidence/registry。迁移ledger只记录DDL，不能代替名称登记配置审计。

## 输入封存（schemaVersion=1）

manifest必须由Root实际采集工具从protected facts+bytes回执生成；不接受客户端JSON声称MATCH或boolean：

- database/serverUuid/tenantId均字符串；当前固定ruoyi-vue-pro、真实UUID、tenant1。
- scopeId、正式用户历史口径回执SHA、facts/query/collector/receipt rawSHA；actual39GET output/receipt/reader/tool rawSHA及真实exit0、source_bytes_verified、39exact一一MATCH。
- claimIds/masterIds/versionIds/sourceFileIds完整精确Long字符串集合；由冻结ID集合计算claim/version/source/edge/name计数，禁止自报计数。
- 每版冻结File/Master/claim/infra_file及ownership/ticket实际关联原预像（包括NULL）、nativeBPM/versionID、完整UTF8原名bytes、sourceSHA/size/config/path及存储storage/endpoint/bucket/region/enablePathStyleAccess定位；凭据不进manifest/日志/回执。
- Root实际启用操作者身份依据、reason、requestId；实际 verifiedAt/activationAt，仅当前技术执行时间，不能借作QA签名时间。
- 各protected输入文件hash都以真实原始bytes重算；集合hash为按精确ID排序的canonicalJSON UTF8，不trim/lower名字、不去后缀。SHA字母统一lowerhex。

当前Root39读取35MATCH+4缺正文，必须输出 `BLOCKED_MISSING_SOURCE_BYTES` 并**不生成登记SQL/不PREPARED入库**。恢复后Root必须重新真实GET全39+fresh metadata/原scope，旧35结果不冒称新39全部通过。

## 首次与重放事务合同

1. 核实际源库/UUID、writer排除窗口、备份及新3表exact schema；读取并锁本scope所有legacy Master/claim（按tenant/master/claimID稳定排序），然后按原名binary键排序锁registry；从正式事实重建冻结全部ID集和原预像，任何extra/missing/change拒绝。
2. 拒绝旧claim原名或完整正式编号非NULL、未知source/config、缺任何MATCH、SHA/size/locator不符、现代claim/registry已占旧名。历史多Master同名及同Master多名合法保留全部，不去重owner边。
3. 首次INSERT scope PREPARED（真实计数/SHA/actor/reason），每实际版本INSERT evidence（当前39），各原名INSERT统一registry LEGACY_GROUP（当前13）；INSERT只写这3新表。
4. 在锁内重跑正式 `countUnresolvedNames`所需全部覆盖/源identity校验（临时PREPARED不参与正常业务），将scope一行由PREPARED、exactmanifest、exactpreimage条件转换VERIFIED；实际rowcount1。此UPDATE只新scope，不更新旧claim/File/Master。
5. 调正式GxpAuditService.append；任何失败传播，整个事务rollback；commit后独立只读核旧25claim/39File/25Master/签名原hash不变、新scope计数/关系/registry/审计回执正确。
6. 重放同tenant+scopeId：重算原manifest、每行payload及scope原完整identity，必须全相同且VERIFIED，并且现在live源身份仍相同；返回原正式回执，零新增/零更新。scopeId相同manifest不同、部分配置、现有PREPARED/INVALID、不全覆盖均拒绝，不自动补齐/覆盖/删除。

内部activation service和实际Spring/Gxp kernel测试已加入；测试替代存储/用户目录/环境读端口，不声称真实39已读取或真实数据库已登记。首次和重放/审计失败结果见最终回归回执。`activeLegacyReservationAndNewShareTheRealExactRegistryLock`证明现有row锁互斥；不能冒称“缺key首次INSERT的实际activation↔NEW完整竞争”。writer后续需真实事务测试LEGACY-ACT-01全部验证/旧零写、02审计失败全回滚、03同scope原回执重放与ABA/source drift拒绝，以及首INSERT并发唯一竞争。

## 20年释放与公开NEW编号复用限制

Core按实际obsoleteTime.plusYears(20)保留；同名多个owner每个证据分别保留，未知真实作废时间永久占用。仅当本Master所有版本终态、无执行指针、现代claim也已到期，才释放自身legacy边；最后未释放owner归零才registry NONE，新modernName可同stable行generation+1取得。原旧NULLclaim始终零写；modernclaim按原正式生命周期。

服务/mapper隔离测试用预先存在的第二Master验证reservation/claim层名称和原编号释放；**不等于公开新建复用已闭环**。既有C `Workflow.loadOrCreateMaster`对existing formalidentity NEW拒绝，Master唯一仍永久保留。Root待审两个设计：

- 依法释放后复用原Master空链：历史版本/签名仍归原Master，但新的“逻辑文件”是否允许继承相同Master需要业务确认；需authoritative pointers完全空、全部20年到期、version/revision/编号新系列明确，不可假装NEW绕guard。
- 新增正式活跃身份registry并前向唯一约束改为仅活跃占用：旧Master保持原值/历史，需独立更广DDL/生命周期事务/并发/审计及具体数据库授权；绝不直接删现代唯一或UPDATE历史。

本批均不实施。当前已确认的20年内原名/原编号继续占用被严格执行；到期后公开业务如何重新建档由Root单独收口。

## 当前真实执行边界（2026-10-03）

- 原19升级Root已独立PASS；新3表未实际执行。
- 源正文35/39MATCH、4缺对象；对象恢复用户回复尚未批准。不得恢复对象或开放完整scope。
- 审计策略尚未批准；不登记新26规则或任何虚构质量记录。
- 注册loader使用单次bounded读buffers哈希与严格JSON duplicate/trailing校验；facts claim/master/version/storage/ownership集合一致，bytes实际完成时间导出verifiedAt，历史用户决定语义真实核验。
- 官方GxpAuditService真实内核在isolatedtests下执行策略、真实LoginUser、序号/事件hash、幂等、事务回滚；Root仍须审核最终tests及源码指纹。Maven当前状态不作为PASS，结束后正式记录。
