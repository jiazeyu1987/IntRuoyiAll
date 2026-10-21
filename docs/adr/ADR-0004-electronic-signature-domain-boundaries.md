# ADR-0004：以独立电子签名领域和业务适配器收口模块化单体

## Status

Accepted for architecture design / APPROVED_DESIGN。日期：2026-10-10。用户已同意“模块化单体＋独立电子签名领域＋业务适配器”总体方向，并要求主 Agent 在五条满足后放行；主 Agent 已完成两轮内容审查并批准，见[审查记录](../system/electronic-signature-architecture-review.md)。本次批准覆盖架构与迁移设计，未启动实现、运行测试或发布；作者没有自行放行。

本ADR细化ADR-0003领域边界，替代强制所有临时员工转系统账户的决策及DCC承载公共授权/图片的现状。ADR-0003另有合同的受控外部时间、HMAC/MFA/WORM等长期控制目标保留；当前能力严格按M5本地SHA-256/服务器钟/创建审计范围声明，不宣布长期目标已满足，也不凭本ADR把外部时间/HSM/WORM新增为V3签署前置。临时档案保留本人密码、安全生命周期及作用域授权。20261006 M5/R10修复已设计批准但未实施；其V2协议/消费合同保持，稳定主体另属拟新增V3。

## Context

当前已有独立 yudao-module-signature；其业务模块依赖仅 system（`IntRuoyiBackend/yudao-module-signature/pom.xml:19`），但实现直接 import system.service.gxpaudit，并在业务事务取 ledger 后认证（`IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java:18`、`:77`、`:102`）。subject adapters 主要解 Base64 内容，不能以类名证明业务授权边界已经完整。

DCC workflow 有任务和 readiness 校验后才签署/推进业务（`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java:1111`、`:1124`、`:1134`）；BPM reviewTask 先签后 provider.review 且处于事务中（`IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/ApprovalCenterServiceImpl.java:126`、`:150`、`:158`）。MES 当前实际员工允许 SYSTEM_USER/MES_EMPLOYEE_PROFILE，但公共授权仍调用 DCC，档案认证以 Runnable 传入（`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java:9`、`:868`、`:896`）。

生产提交当前先签后生成 feedback/event（`IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java:173`、`:186`、`:199`）；稳定主体需要正式提交身份，不能改用订单 ID 或全内容 hash。用户要求保留合法无订单/批执行生产提交，不可因迁移新增账号/订单前置。eDHR 活跃订单主链和三条独立表单链仍由业务所有。

业务下游越过内核边界：偏差/NCR 直接 Mapper，timeline 直接查统一表并解 identity JSON；治理 UNION 旧领域表尚不是统一账本；BPM 公共图 provider 由 DCC 实现。准确入口与下游证据见架构文件 §2，不把目标当现状。

## Decision Drivers

- 业务继续拥有动作资格、当前对象版本、权威内容和状态机，浏览器不得绕过业务入口。
- 正式签署只有一个不可变事实源；业务投影 ID 与统一 signatureId 明确分离。
- 主体稳定、内容独立、多个动作/提交可区分；身份域与设备 operator 不混淆。
- 本地签名、业务推进、绑定及统一审计严格同事务，凭据失败证据独立持久且锁序一致。
- 公共授权与图片归签名领域；历史协议、历史姓名/图/链接不改写，不允许失败fallback或长期双写。
- 现有 M5 V2 修复必须可独立实施，长期协议不得迫使其全部重做。

## Options Considered

|选项|收益|决定及依据|
|---|---|---|
|继续 DCC 为公共签名中心|少迁文件|拒绝：MES/BPM依赖DCC资格/图片，领域所有权错位，难形成统一typed消费|
|通用浏览器写接口统一业务动作|接口少|拒绝：内核必须理解全业务状态，易绕过对象/实际员工授权|
|独立远程签名微服务|部署独立|本阶段拒绝：本地事务要求强，带来网络/分布式一致性成本，无当前必要性|
|模块化单体、独立signature领域、业务adapter|保留业务入口、统一安全和事实，同一数据库事务可落地|选择：与已有模块及用户同意方向一致；以public API/SPI限制源码边界|
|新旧长期双写或失败转旧内核|短期表面回退方便|拒绝：双事实源及不确定成功，违背no-fallback和历史可验要求|
|生产主体直接用订单/内容hash|无需创建业务身份|拒绝：一单多提交合并或内容改变身份，无订单合法路径被阻断|

## Decision

1. 保持 Java 17/Spring Boot/Maven 模块化单体；signature 不源码依赖 DCC/MES/BPM/showroom，仅依赖 system 公共身份安全/GxP API、infra 公共文件 API 及 framework。system 不依赖 signature。adapter/provider 的运行时回调通过公共 SPI 装配，不形成反向源码依赖。
2. 保留业务 HTTP Controller、权限、任务及前置条件。业务 adapter 在锁内调用真实领域授权、读取权威对象/版本/文件，不能只解码客户端或上游拼出的内容。内核没有任意 signer/object/hash 的浏览器写接口。
3. 公共签名授权/策略、图片版本管理迁 signature；SYSTEM_USER 凭据仍 system 所有，MES_EMPLOYEE_PROFILE 凭据仍 MES 所有。system.api 定义 provider/纯策略，两个域各实现精确身份校验，替换 Runnable；临时档案不要求系统账号，设备 operator 不是 signer。
4. 签名、业务状态、统一绑定、创建与业务 GxP 审计同一 DataSource/事务管理器提交。无事务 façade 先读成功回执，首次请求才独立认证；业务 executor 严格 ledger→身份→公共授权→业务主体→图片/绑定锁序，所有成功写 MANDATORY/REQUIRED。最外层调用者一并拆分，NEVER 检测误入事务；不只把 kernel 改 REQUIRES_NEW。Flowable若不参与同一事务阻断路径。
5. 独立认证只锁身份，失败计数/安全事件同 REQUIRES_NEW 提交后返回 outcome，调用方才抛错误；不得在外层持身份或业务锁时再独立认证。锁内重新检查 enabled/lifecycle/期限/资格/generation/version，沿用 M5 MySQL防ABA保护。失败事件不取GxP ledger，不被业务回滚吞掉。
6. V3 用 tenant+subjectType+stableSubjectId+subjectVersion；businessContent/hash 独立。动作及 occurrenceId 区分同主体多动作/多次执行，requestKey 区分重试与新提交。生产新增本域正式 submission 身份，在同事务以明确 MySQL 自增先插 UNSIGNED 记录获得ID，再签、绑定、改 SIGNED；失败全回滚，正式读不暴露 UNSIGNED，不能提交半成品。现有 createSubmitEvent 仍在签署和核验后，不循环提前调用，不假设现有 Snowflake/事件预分配能力；无订单关联可空且不造假对象。订单不作为签名主体身份或通用签署前置。
7. 统一正式记录是唯一不可变签署事实源。业务保存 unifiedSignatureId，投影自身 projectionId 单列。旧DCC ID/HMAC保留领域工件及历史路由；旧表停止正式新写。新版本/撤销/替代只追加证据，旧正文/摘要/时间不UPDATE。缺明确统一关联不猜，不因相同hash/数字合并。
8. 业务只能用 public typed evidence/query/binding verification；删除业务 import signature.dal、跨表SQL和内部JSON_EXTRACT。历史格式的固定解码封装在公开读合同与本域历史codec中，不泄漏内核envelope给应用service，不按当前业务资料重建历史。
9. 治理读模型归 signature，区分 sourceRef 与 unifiedSignatureId、explicit link 与 LEGACY_UNLINKED。公共目录先做租户/角色/业务scope过滤再COUNT/page/export，路由由业务SPI注册。统一BPM事实即使无旧表也可进入新目录；旧globalId/领域URL是只读alias，缺关联保留缺失，不能伪造VALID。
10. 正式事实及本地outbox同事务，通知与目录同步采用至少一次投递+eventId幂等。worker失败10次后FAILED/告警，原eventId人工重放；列表返回水位、详情由正式receipt读取。异常不回退旧UNION、不返回空成功、不宣称通知送达。
11. 公共图片署时固定imageId/version/fileId/hash等快照，后续只能读原版本，换图/停用只影响未来。缺失/损坏明确失败，无图合法动作NOT_REQUIRED，历史缺图LEGACY_MISSING。图片不等于完整电子签名，带图PDF合同不自动降为文字PDF。
12. M5 V1/V2协议与创建来源登记保持；其V2六元数据/28键evidence/14键command和MES17段subject不改。稳定主体独立V3先读后写，每域通过全部consumer/原子性/身份测试后切换，删除该域旧writer。历史按明确版本只读解析是协议支持，绝非新算法失败后降级旧算法。

经主 Agent 第二轮复审通过的具体合同：

- 两摘要分离：businessContentHash仅覆盖公开`{schemaId,data,before,after,diff}`规范化内容，adapter及consumer可以重算/匹配；既有contentHash覆盖完整签署envelope。receipt/query/ExpectedBinding明确两字段。V1/V2先按M5验证原完整证据、时间和唯一创建审计，再从严格固定历史codec派生业务hash，标DERIVED_VERIFIED_LEGACY；不改任何历史摘要，不用剥离副本冒充完整验真。
- 两阶段adapter：先锁对象/授权/冻结业务内容与imageRequirement，core一次捕获图片并结合session单次时间/身份，再finalizeArtifacts构造DCC HMAC等领域工件。core核对主体/版本/身份/内容/图/时间不变；工件不含尚未生成的统一signatureId/evidenceHash/auditId，不能另采图/时间。
- V3重放输入使用RequestIntentHash（原command_hash列），全部入口统一排除后生成ID/occurrence。已有task/node定位在intentLocator保护；新生产/PQC等正式记录ID在事务内生成后才形成ResolvedSigningCommandHash（新增列）。后者保护intent到解析动作映射并进入完整envelope；重放只用原intent协议与回执，不重新生成ID或当前资料。
- 凭据SPI及SignerKey/CredentialAttempt/AuthenticationOutcome/AuthenticatedSigner/LockedSigner归system.api.signaturecredential，无signature DTO引用；SigningSession/subject/receipt/evidence/FrozenSigningFacts归signature.api；GxpAuditExpectation/registry归system.api.gxpaudit。单向编译依赖不会因DTO形成system→signature环。
- V3 EvidenceHashInput及完整envelope固定清单见架构§10。先生成business/intent/resolved/content/evidence各hash，再一次INSERT取得signatureId，随后以signatureId+hash写唯一创建审计；signatureId/auditId/eventId等后生成ID不进入签署hash、不回写正式事实。V3精确source为ElectronicSignatureV3TransactionExecutor.executeAuthenticated、SERVICE_METHOD、signature.record.create、ELECTRONIC_SIGNATURE_RECORDED、审计schema2；registry/机器登记同步新增，不改M5 V1/V2登记，三代交叉source拒绝。
- 原失败锁迁移不用max/求和猜合并。停止旧writer后冻结两来源各自计数/窗口/锁/策略/事件；只有可确证逐尝试去重的完整集合才按明确滚动窗口重放。任何有效窗口证据不完整的身份拒绝新认证/签署，保留两来源分别生效，直到所有可证明原窗口/锁自然到期或证据审核关闭；不清零、不缩锁、不调用旧writer。未知截止/锁原因继续隔离，不擅定默认期限。
- 治理采用最终一致cursor，水位不充当MVCC，不承诺跨页冻结集合；每页重新授权、totalAsOf为本次值。导出开始的一次一致读取得独立recordKey集合形成manifest，逐项再核当前权限，失败不跳项。domain为SYSTEM_USER/MES_EMPLOYEE_PROFILE枚举，只有tenant/signer/operator的ID为规范十进制字符串。

公共授权/图片原DO没有tenantId；迁移由保留的原SYSTEM_USER主记录或一致明确历史证据确定租户，缺失/冲突隔离，不归默认租户。原imageId/version/hash引用保留映射。密码失败安全状态按上述逐尝试证据/双来源窗口规则转凭据域并留存，不缩锁；行政锁归公共授权，未知原因锁保持拒绝直到核实。现有DCC投影无统一FK，新写原子显式绑定，历史零确证UNLINKED、多候选AMBIGUOUS，不按相似时间/hash猜。

## Consequences

收益：业务资格与公共安全职责清晰，统一签署可独立查询/验真；DCC/MES/BPM不复制认证或图片生命周期；正确处理同对象多动作、选定员工、无订单生产及重放；旧记录不会随当前姓名/图片漂移。

成本：公共API及SPI调整、上游事务拆分、资格/图片所有权和权限切换、本域submission/revision/真实绑定、typed reader及异步治理目录均需实施和真实MySQL验证。新submission是业务身份，不是额外签名账本。保留历史codec和别名是必要维护负担，不能以代码整洁删去仍需验真的协议。治理列表有明确最终一致水位，不提供虚假即时覆盖。

风险：外层漏拆导致锁反序/认证计数回滚，Flowable资源不共事务，投影缺绑定，历史迁移误合并，权限先分页后过滤，文件被覆盖/删失，reader误拒合法V2/V3。每项由架构 §12 定向正负例及数据对账关闭；没有证据不能开放对应新写。

## Rejected Options

拒绝长期双写/运行时fallback、当前资料补历史、内容Base64作为新主体身份、强制临时工系统化、用统一VALID替代业务权限/数量资格、业务直接读内核表/JSON、图片当完整签名、静态ADR通过当运行验收。DCC历史HMAC仅表达真实领域核验范围，本地SHA256+时间+创建审计不是外部不可抵赖认证。

## Verification and Operational Impact

具体五项矩阵、迁移L0..L5及删除条件见[长期架构](../system/electronic-signature-architecture.md)。必须沿业务入口→API→adapter/service→数据库→下游完成核查；验证身份/权限、成功重放/并发新键、多动作/内容revision、真实生成后全部consumer、事务回滚、安全事件持久/锁竞争、图历史/链接、各来源治理权限/分页/导出、outbox幂等/FAILED重放。

实施需任务隔离MySQL/Spring代理/触发器，H2/fixture不替代事务与正式写链证据。经用户授权E2E只用Playwright真实页面/账号/任务数据，API/DB不能承担验收动作。本轮只检查设计及ADR结构，不运行构建、业务测试、服务、DB写入或Git操作。

## Revisit or Rollback Conditions

- 未生成V3正式事实可回退该域代码但保留additive schema；首条V3之后只停新写并向前修复，回滚版本必须读V1/V2/V3，不恢复旧writer或双写。
- 出现同事务管理器缺失、无法生成权威稳定主体、权限scope无法表达、身份写方锁序不全、图历史文件不可保留，阻断该路径实施/开放，重新审查受影响设计；不得降级。
- 只有独立部署/外部可信签署服务成为明确需求时另立ADR，保留业务授权与证据合同，重新讨论分布式一致性；不预先引入远程框架。
- 灾难恢复只按独立授权恢复业务/签名/绑定/审计的一致备份，不能单表回退或清失败计数。旧协议仍有保留期内历史数据就不删除reader/历史alias。

## References and Owners

- 直接实施合同：[electronic-signature-architecture.md](../system/electronic-signature-architecture.md)，§2源码证据、§4DTO/SPI、§5主体、§6事务、§8公共所有权、§9读模型、§11迁移、§12五条验收。
- 既有方向：[ADR-0003](ADR-0003-unified-electronic-signature-kernel.md)；本ADR只细化边界并替代强制临时账户归并，不改写其历史批准事实。
- 已批准修复：`doc/tasks/20261006-electronic-signature-repair-design-review/repair-plan.md:3`；`review-report.md:77` 第四轮pass及`:99`当前决定；四份system设计仍是M5合同，不是已实现系统。
- 批准来源：当前任务用户同意总体方向，要求子Agent先写设计、主Agentreview且五条全部满足才放行。
- 所有者：signature负责公共签名/授权/图片/目录，system负责身份安全API/GxP，MES负责档案凭据及生产/PQC/偏差主体，DCC/BPM/showroom各负责业务授权与适配器，infra负责原文件版本存储。主Agent负责本设计放行，后续执行按用户授权范围。
