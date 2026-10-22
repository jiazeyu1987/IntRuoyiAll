# G25 历史原名占用设计与实施范围提案

2026-10-03；状态 `design_ready_for_root_review_not_implemented`。当前只写本提案/既有任务记录，不改生产Java/正式SQL，不运行Maven/DB/服务/types/build/Git。Root仍是本轮数据库唯一执行Owner；后台Owner只负责39份正文只读核验工具。用户已确认历史口径，但本文没有把新增DDL/39证据登记当作已授权或已执行。

## 目标与已确认事实

用户决定见主管理 `g25-user-authorization.json`：保留历史文件、名称、版本、签名；核实全部旧原名继续占用；未来NEW exact重复拒绝，不自动改名/合并/删除。其rawSHA `fd257e2e41e760ee375a5fa5cf08ef1db452bca6f835abb75de52fa6ece9eaa9`。这不是质量签名时间或依据，不能补造QA审批记录；名称身份配置审计与另25项质量批准登记保持不同事实。

G23真实25 active legacy claims、25 Master、39版本/39源文件，观察为**30条Master↔完整名称边、13个完整UTF-8原名**。旧19个跨Master同名claim及3个Master多版本不同名在新用户口径下均属于可保留的历史多对多关系，**不能继续把历史同名本身当作激活blocker**。缺源metadata/hash/租户关联或正文核验失败仍是blocker。G23的6 metadata候选也不等于bytes已验证。

基线facts rawSHA `8c17e87327b31160e86be9d4c9424670c9a5e8097c1d909c8382f021819a416b`、1 SELECT；旧protected schema rawSHA `cf6a94375b95720637d7b75a471e508420ffa9cc6b399c4438851237d4fc66fa`。最终数量必须由登记时fresh exact scope再核，不能把25/39/30/13硬编码为通用产品规则或截断新数据。

## 采用的存储模型：3张独立sidecar表

建议新增单独前向迁移 `IntRuoyiBackend/sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql`，正式metadata dependsOn `20260930_dcc_c_revision_identity` 与 `20260811_dcc_source_ownership`（两者当前正式SQL已核对；保留所需obsolete_time/retain_until列由C提供，20年是当前正式Java策略，不制造不存在的retention迁移）。**不改已冻结19SQL、不重跑base/catalog、不UPDATE旧File/Master/claim，不新增业务seed。** 只DDL，实际配置INSERT由另审查登记事务执行。

| 新表 | 核心字段与约束 | 用途 |
| --- | --- | --- |
| `dcc_legacy_source_name_scope` | `id/tenant_id` bigint；`scope_id` varchar64 ascii_bin；`manifest_sha256/facts_sha256/bytes_receipt_sha256/user_decision_sha256/scope_identity_sha256` char64 ascii_bin；`status` PREPARED/VERIFIED/INVALID；claim/version/sourceFile/edge/name计数；真实verified_at/activated_at/actor/reason/request_id；unique tenant+scope_id/manifest | 冻结一个明确legacy集合及完整核验回执，验证原scope相同且全覆盖才VERIFIED。状态及失败理由都不能默认VERIFIED |
| `dcc_legacy_source_name_evidence` | `tenant_id/verification_scope_id/legacy_claim_id/legacy_master_id/controlled_file_id/source_file_id/config_id` bigint（verification_scope_id引用scope表id，外部scope_id仍string）；`version_no`及native BPM字符串仅原事实；`source_original_file_name` varchar256 utf8mb4、**`source_name_key` varbinary1024 STORED完整binary生成键**；expected/actual sourceSHA char64 ascii_bin、expected/actual size bigint、metadata identity/preimage/hash/proofRow SHA；`bytes_status` MATCH；`obsolete_time/retain_until/released_time`仅生命周期真实事件；unique scope+claim+controlledFile；index tenant+source_name_key+master | 每个实际版本的一行不可变核验证据（本scope39）。同Master多个原名、同原名多个旧Master都可存在。`SELECT DISTINCT tenant,name_key,master`正式投影出30条关系，不对tenant+name建立此表唯一约束 |
| `dcc_source_name_reservation` | `id/tenant_id` bigint；完整原名string+varbinary1024 key；**unique tenant+source_name_key**；`reservation_kind` LEGACY_GROUP/MODERN/NONE；legacy scope或modern claim/Master引用、generation与active状态；真实审计/释放时间 | 每个exactname一个全局占用/锁行（本scope13 legacy groups），LEGACY_GROUP的多个owner由evidence关系查询；MODERN只能有真实一个现代claim。并发新建和legacy激活共用同一exactkey，避免跨表check-then-insert放出重复 |

所有外部Long传string、DB bigint；basename保持大小写、扩展名、尾空格，按UTF-8完整binary键相等，不LOWER/TRIM/去扩展/按prefix/hash做唯一。1024byte键加tenant8byte在MySQL8 InnoDB范围内；实现时仍需用真正旧schema/newDDL首次及重跑合同核对nullable/charset/collation/generated/index顺序。

原 `dcc_controlled_file_name_claim.uk_dcc_c_source_name` **不删除或放宽**。现代NEW仍依赖它作最终唯一性约束，新registry还把现代和legacy namespace原子统一。legacy同名多Master只存在evidence关系中，旧claim保持source_original_file_name=NULL及所有旧值，不能向现代unique表插15个同名alias来模拟保留。

registry是正式占用数据，不是绕过历史guard的开关。只有VERIFIED scope、实际匹配的完整evidence且registry关系正确时legacy才resolved；空表、PREPARED、任意缺项/错误状态不放行。

## 正式门禁与三种动作意图

复用 `DccControlledFileNameClaimService`，新增独立 `DccLegacySourceNameOccupancyService`与mapper，不能在upload controller/page吞countUnresolved错误、使用测试租户或全局allowLegacy boolean。

`countUnresolvedNames(tenant)`的正式含义升级为“active NULL-name claim且缺可核验的完整legacy resolution”。可新增diagnostic `countRawNullNameClaims`显示仍25，但业务guard继续使用**正式unresolved判定**。SQL应NOT EXISTS对应VERIFIED scope coverage：该claim同tenant Master和冻结版本源identity仍匹配、所有scope version evidence完整、expected/actual SHA与size相等、对应exact registry仍保留/或依法已释放状态、scope/hash无漂移；未知legacy claim不在scope仍计unresolved。不可只看一条MATCH、一个Master名字或count39而忽略主键集合。

源identity匹配限定不可变原名/sourceFile/config/size/sourceSHA/版本所属关系；正常业务状态、实际作废时间变化不能让已核验源名证据失效。版本新增不加入旧scope或重写旧bundle，新增现代正文按其正常正式源/claim规则处理；若旧scope内正文被替换/源identity变，明确invalidate并阻止新建，不能偷偷复用旧body证据。

三种**服务器实际流程推导**的reservation context：

| 意图 | 来源及必须事实 | 旧占用结果 |
| --- | --- | --- |
| NEW_LOGICAL_FILE | WorkflowServiceImpl当前`changeType=NEW && controlledUploadSubmit`，正式项目/leaf/number/newMaster/source ticket/调用者权限 | 任意有效LEGACY_GROUP占用均拒绝，**即使请求Master等于某旧owner也不能新建**；现代otherMaster占用拒绝；新registry行及现代claim同事务登记 |
| SAME_NEW_DRAFT_REPLAY | preflight现有exact scoped SOURCE BOUND ticket+actor+unsent NEW draft+same Master/source/body/sourceOriginalName、无native BPM/受控baseline | 只重放本现代草稿原claim，不产生新逻辑文件/新占用；不能凭sameMaster或同session字符串承认历史占用 |
| EXISTING_MASTER_REVISION | 既有RevisionService/Query checkin真实已授权Master、selectedIterationId/baselineId、本版真实sourceFileId与核验source name/hash关系；现有C版本/项目/leaf/number/申请人OWNER权限已通过 | 对**自己这个旧Master、实际所选版本/正文**的VERIFIED旧占用可继续使用，历史同名还有其它旧Master不误拒；不授予其它Master新建资格。多个旧名字按selected evidence精确取，不任选latest名字 |

建议typed command `DccNameReservationContext(intent,masterId,selectedControlledFileId,sourceFileId,sourceSha256,projectId,leafId,number,ticketBinding)`；只由现有服务器正式路径构造，controller的route/任意flag不能决定intent。`claimIdentity`保留现代职责，但原六参数没有足够事实区分NEW/REVISION，需显式命名入口/内部command验证；不能为了兼容旧调用默认猜REVISION。

需要接线的真实调用点（当前源码）：`DccControlledFileWorkflowServiceImpl:2312` NEW；`DccControlledFileRevisionServiceImpl:117/:212` selected/baseline升版；`DccControlledFileQueryServiceImpl:693` checkin/retry。正式selected source/权限/版本/租户读取应在调用前已核验，service再核其关系。`preflightNewSourceName`仍NEW early gate，legacy完整核验后不同未占用名字返回正常流程，有效重复在正式存储/ticket创建前拒绝。

对旧File.source_original_file_name仍NULL的读取，允许**独立验证后的正式只读identity projection**精确查询本版evidence；这不是按infra/title兼容fallback。若现代字段非NULL却与VERIFIED evidence矛盾必须拒绝；没有本版证据也拒绝。不把值写回旧File。新合法版本可按其正式所选source生成自己的新字段。本文不放宽现有“缺项目/编号/负责人/选中工作正文权限”的C守卫；六个旧Master formal number不足不因legacy登记被补成新项目/编号。原旧版本无法办理的其它具体C前置需要单独报告，不能据此更改当前scope。

## 核验证据与激活流程

后台39bytes helper输出仅sourceFileId/status/actualSHA/actualLength/httpStatus/errorCode；不含名字/Master/config，**不能从stdout单独创建occupancy**。Root封存manifest应包含：

- fresh DB/container/UUID/version、fact/query rawSHA、25claim ID/25Master ID/39exactversion ID/39sourceFileId及原版tenant/source/meta/preimage identity；39source nameUTF8 bytes与metadata SHA；完整scope集合hash。
- 正式sourceSHA/expected size；实际GetObject39全部MATCH的actual SHA/length；fileId→metadata/config28/storage20/真实object address身份（地址/凭据仅保护目录，公开只opaque hash）；helper source/classes/output rawSHA、真实执行退出与采集时间、所有39exactID一一join，无extra/missing/duplicate。
- 用户历史口径decision引用/rawSHA、明确登记scope/reason/actor、备份与可review配置manifest；**不含或猜QA批准时间/质量签名依据**。

实现时服务读取受信审查artifact，或正式read-only source reader重新核验；请求DTO只能传artifact ID/expectedSHA，不能相信客户端`bytes_verified=true`/JSON中自称MATCH/客户端路径。当前Root protected JSON仅是待封存实际证据，不是任意上传文件可获得信任。激活前重新核metadata/claim/version原identity与manifest，变了明确BLOCKED；bytes的verified-at只证明该实际读取时刻，要有明确源对象变更保护/当前稳定窗口，不能宣称长期永久不变。

状态：PREPARED→验证完整且权限通过→VERIFIED。PREPARED evidence从不参与占用/resolved。激活以一个事务插入/验证39 evidence、13 registry占用及scope最终VERIFIED、配置audit/outbox；任何缺对象/MISMATCH/metadata候选/范围变动/冲突/审计失败整批rollback，25NULL仍unresolved。重复同manifest原rowHash全相同返回同配置回执，不增行、不更新已冻结payload；相同scopeID但不同manifest拒绝。13名字下所有旧Master关系依法保留，不因旧多对多冲突删除或筛掉某条。

Root G21 clone19演练现在进行中，**此新DDL/配置不参加那19首跑/重跑**。需独立完整migration policy closure、首次/重复、原业务零DML和新schema合同；现有19成功不代表本设计部署完成。

## 事务、并发及写入影响

现有modern claim unique与新增registry exact unique双守卫。NEW最终claim事务先校验unresolved/实际intent，再锁自身Master（既有规则）和exactname registry行；不存在行则正常唯一INSERT竞争，唯一冲突后读并核真实owner，禁止把所有DuplicateKey当同Master成功。modern claim INSERT与registry MODERN owner绑定同一个事务；只读preflight可能被另一合法新建抢先，最终save仍唯一冲突fail，不上传后默默改名字。

legacy激活锁实际scope legacy Master/claim身份（按tenant/master/claimID排序），再按exactname bytes顺序锁/创建13registry行；REVISION/保留/释放也按Master→name顺序，不引入反向tenant锁造成现有Master先锁路径死锁。所有field/rowcount/原值守卫在锁内重验；登记前若modern新claim已占观察旧名字，拒绝激活并报告冲突，不改其claim。数据库事务冲突要明确失败/回滚，不吞异常重试成成功。实现需用两线程门闩测activation↔NEW及NEW↔NEW竞争，不只验证SQL字符串。

| 阶段 | 允许写 | 明确不写 |
| --- | --- | --- |
| standalone migration | 3新表、generated/index、正式新migration ledger | 旧File/Master/claim值、正文、原ledger、菜单/历史seed |
| 审查prepare | 新scope/evidence草案与审查audit（若选择持久准备） | 旧claim原名、发布/签名/BPM |
| 39证据激活 | 新39evidence/13registry/scope状态、正式config audit/outbox | 旧25claim所有字段、旧39File、旧Master/签名/正文 |
| 未来NEW | 既有正式modern claim+新registry MODERN绑定、正常新文件业务 | 旧claim回填、旧Master继承/改名 |
| 未来合法REVISION | 新版本正常字段/证据，读取legacy own-source identity | 猜旧项目/编号/负责人或覆写旧version |
| 作废保留/依法释放 | legacy新sidecar生命周期/registry状态及audit；modern沿原claim服务 | 到期前释放、删除旧claim/历史evidence、自动job |

## 历史作废20年与同名多owner

继续复用 `DccObsoleteRetentionService.retainedUntil(obsoleteAt)=obsoleteAt.plusYears(20)`。`NameClaimService.retainObsoleteIdentity`若本Master属于VERIFIED legacy，登记**新的sidecar**真实obsoleteAt/retainUntil到该Master所有占用边，不为配置先写旧claim。现代路径沿现有claim保留。未提供实际作废事件/时间的历史owner持续占用，不以登记时间/今天/签名时间猜起算点。

`releaseExpiredIdentity`legacy分支必须同时核：真实作废时间且每owner期限已满、当前无ACTIVE/待生效/WORKING/在途BPM/未终止版本、所有同名group其他owner也已可释放、scope证据仍完整、正式授权触发。一个Master到期只释放自己sidecar边；其他Master仍占就不能把13group的该名字变空闲。最后owner依法释放才registry AVAILABLE，新NEW可在同一stable行generation后取得MODERN占用，历史evidence/审计仍保留，不删旧claim。新增job/BPM审批节点均不在此实现范围。

## API、事件与权限计划

建议新独立维护controller（不往普通upload DTO塞allowLegacy）：

- `GET /dcc/controlled-files/legacy-name-occupancy/reviews/{scopeId}`：只读coverage/blockers/精确Master关系，授权可见信息；未核验不标可用。Long string，完整保护地址/keys不公开。
- `POST .../prepare`：选择已封存actual证据artifact ID+SHA、scope/reason；读取/复核真实facts，形成manifest回执与blockers，不激活。是否直接采用task维护CLI取代REST由Root实施分配确定，但仍同service/权限/审计。
- `POST .../{scopeId}/activate`：manifest SHA、expected scope preimage、正式idempotency key、reason；严格当前tenant、实际启用actor、**真实doc_control角色 + existing dcc:controlled-file:update**（与已有source-governance门禁相同）双校验。super_admin无该真实业务角色不能因helper bypass取得资格；不自动授角色。明确授权配置操作产生配置审计，不伪造QA电子签名或借普通审批approve权限。

建议配置事件 `LEGACY_NAME_OCCUPANCY_PREPARED/ACTIVATED/INVALIDATED/RETAINED/RELEASED`，payload只scope/manifest/evidence IDs/old-new config摘要/reason/request/actor/真实clock，使用现有正式审计持久化及事务outbox（实现前确认现有GXP audit owner/event policy）；不触发通知/整改/新增BPM，不记录密码/凭据或把维护用户admin说成质量已签人。审计注册若需要新policy operation属于新的精确配置范围，不能夹进原25质量审批登记或默认质量依据已齐。

## Root待分配的具体生产文件与SQL清单

主要唯一NameClaim backend Owner（以下均在`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc`）：

1. 修改 `service/file/DccControlledFileNameClaimService.java`、`dal/mysql/file/DccControlledFileNameClaimMapper.java`，正式resolved guard及NEW/replay/revision/retention分支；原modern unique/error行为保留。
2. 新增 `service/file/DccLegacySourceNameOccupancyService.java`、`DccNameReservationContext.java`、`DccLegacyNameVerifiedScope.java`（command/结果/验证投影可按既有record拆分）；新增3个DO/mapper对应scope/evidence/reservation。
3. 必要最小接线修改 `service/file/DccControlledFileWorkflowServiceImpl.java`（NEW）、`DccControlledFileRevisionServiceImpl.java`（真正existing Master/selected source）、`DccControlledFileQueryServiceImpl.java`（checkin/resume），明确typed intent和readonly verified identity。三者并行Owner目前共享区，Root分配单一Owner后才能写。
4. 若选择正式REST，新建 `controller/admin/file/DccLegacyNameOccupancyController.java`及独立VO，复用当前tenant/auth和source-governance权限组合；无需前端改动或新增节点。Root维护执行先做可review准备，最终激活需明确范围。
5. 新独立 `sql/mysql/20261003_dcc_legacy_source_name_occupancy.sql`＋相邻formal metadata，测试MySQL/H2 schema新3表合同；不得写任何旧记录UPDATE/DML/acceptedEquivalent。
6. 真实auditor/事件适配文件的归属由Root核对现有审计policy后分配；不得擅改Root正在登记的25项质量事实或全局signature平台。

## BDD → 有效RED → GREEN计划

本轮仅设计，**没有生产实现或Java/Maven测试PASS**。下一Owner先新增真实service/mapper/事务测试，用当前生产复现RED，再GREEN；不要以新独立表/类存在关闭业务。

| Given / When / Then | 必要测试文件/边界 |
| --- | --- |
| 25NULL旧claim+39actual MATCH；激活完整scope；原legacy gate resolved并可不同名NEW，旧值/签名/version/master原hash完全不变 | `DccControlledFileNameClaimServiceTest`扩展、`DccLegacySourceNameOccupancyServiceTest`新；原service真实count guardRED，不是假allowLegacy |
| 同名多旧Master及本Master多源名；完整证据登记；30条关系/13组占用完整保留不unique冲突 | 新真实mapper/H2集成＋MySQL migration合同；历史同名不当未知 |
| 元数据6候选但无body receipt，或39少1/错SHA/size/租户/source/config/mismatched scope/重复ID；activate；零VERIFIED/零partial占用且新上传仍拒绝 | 新activation service事务测试，手工MATCH bool不得通过 |
| 任一旧名已被legacy group占；未来NEW带任意同/异Master；exact拒绝；case/ext不同当不同名字 | NameClaim真实preflight+claimIdentity行为及完整API流程，不mock唯一约束 |
| 合法REVISION选own Master实际旧version/source且既有C/权限通过；其它旧Master也同名；允许自己已有占用；foreign selected/new logic假sameMaster拒绝 | `DccControlledFileRevisionServiceTest`/Query checkin受影响测试；包含现代same draft replay与非法actor/跨tenant |
| 已登记legacy但旧Master formal project/leaf/number缺；新revision入口；按原C门禁拒绝而非补当前默认 | 受影响Revision/Query真实server guards；不能因占用已核验宣称所有旧文件都可编辑 |
| NEW↔NEW/NEW↔activation并发；exactkey争用；仅一合法占用，无半登记/错误sameMaster重试 | 真实事务同步门闩／MySQL首次重复验收；配合原modern unique约束 |
| 任一config audit失败；activate；scope/evidence/registry一起rollback，旧数据0写 | 真实事务边界；不是只check throw |
| actual作废不足20年/未知时间/尚有owner或工作稿/待生效/在途流程；release；继续occupied；全部owner满足才释放 | retention集成/边界20年calendar含闰日，复用现有retainedUntil；不新增自动job |
| 同manifest重放；返回同回执；不同manifest/晚fact/source drift；拒绝并维持旧配置不改 | 幂等/冻结scope/ABA类测试，old SHA不变 |
| schema first/repeat；新3表exactcolumn/nullability/collation/generated/index、旧业务/ledger全hash一致 | 新standalone migration policy+static RED/GREEN，再Root明确授权MySQL独立演练 |

后续真实页面由Root当轮明确E2E授权后走公开SOURCE上传→真实工作版本→申请链；API/DB只能只读核验，不能代替该业务动作。当前proposal的通过只表示可review设计，不代表gate已修复/原库已新增表/旧claim已resolved。

## 当前交付与剩余

可审查的新3表/三动作/API/事件/权限/事务/20年/TDD边界已具体；用户历史口径足够用于设计，不再把原19同名冲突要求“改历史”。缺的是39实际正文回执完成后的完整manifest与新DDL/配置的独立精确执行范围，由RootReview后给唯一Owner写生产。当前仍保留所有原G21/G23成果和历史零写。
