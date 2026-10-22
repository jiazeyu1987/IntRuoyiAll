# G33 维护入口独立只读审查

2026-10-04；分支codex/20261001-dcc-integration。未改生产、未运行Maven/测试、服务、DB或Git。读时9文件rawSHA/bytes见`g33-independent-entry-read-fingerprints.json`；实现Owner并行在改源码，本报告只适用于该快照，不覆盖其文件或把后来源码当原结论。

## R01 [P1] 质量签名仅校验引用字符串，未验证真实签名

文件：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccLegacyMaintenanceGate.java`，`currentApprovedPolicy`（56–75）和`requireApprovedQualityFacts`（81–87）。

`qualitySignatureEvidenceReference`只过actualFact非空/非placeholder/长度；`qualityRoleBasis`也是文字，`qualityRoleId`只证明该批准账号拥有某个启用同tenant角色。后面未查询system_electronic_signature，也未调用正式签名验证API，没核证据tenant/actor/subject/policy version/content hash/status/signedAt/action。可证实触发：其它policy_version/26operation/currentschema事实齐备时，保护q把签名引用写成任意非空`QA-REFERENCE-ONLY`，当前代码仍继续检查并允许到activation；不存在签名记录不会成为本门禁拒绝理由。

这不符合`docs/system/gxp-audit-trail-config-security-deployment.md`“策略生效需要质量电子签名”。当前真实QA尚未批准，registry不存在会先拒绝；本发现针对未来资料齐备时不能用非空引用冒真实签名，不声称今日已经发生写入。

Root随后澄清正式边界：实际质量电子签名和职责依据可以是已由Root人工核实的外部资料，不保证全部在system_electronic_signature；此入口不负责新增QA审批功能。因此本发现精确收敛为**自由引用字符串没有绑定已审真实外部资料**，不要求新建质量签名记录或为外部签名强制指定系统API。

有限修复：quality artifact须引用实际签名/职责资料的protected descriptor与rawSHA，并有Root完成核实回执，明确绑定exactpolicy hash/coverage、批准person、签名time、职责依据。资料不能只用任意自由字符串冒充已核实。若资料来自系统签名才使用正式验证API；若外部资料，明确“人工审查的保护证据，入口核hash/身份绑定，不能自动鉴真”。不插入伪签名、不把admin当质量批准。负向验证在其它事实成立而未核实/缺hash/错person/time/policy绑定时拒绝零activation。

## R02 [P1] 临时Redis失败路径仍可能记录stdin access token

文件：上述目录`DccLegacyNameRegistrationMaintenanceCommand.java`，`requireSecretLoggersDisabled`（36–39），`execute`（24–30）；正式来源`IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/oauth2/OAuth2TokenServiceImpl.java`，`getAccessToken`（116–124）。

维护entry日志OFF清单覆盖token mapper/Redis DAO、Spring Redis/lettuce/MyBatis/Jdbc，但遗漏`cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenServiceImpl`。正式checkAccessToken调用getAccessToken，该方法Redis RuntimeException WARN直接把`accessToken({})`写到日志。可证实触发：其它清单logger均OFF、该service WARN仍enabled，维护stdin实际token进入formalAPI且Redis故障；先生成包含token的WARN，之后command抛出的safeexception不能清除已写日志。

有限修复：读取token前核该真实service logger也OFF（或正式secret-safe配置），验证Redis lookup异常的实际service路径captured日志不存在token；不为此改通用OAuth业务兼容逻辑、不猜额外包名。当前没有运行服务触发真实泄露，本审查指出明确可达代码路径。

## R03 [P1] schema receipt没有绑定真实完整首跑重跑合同

文件：`DccLegacyMaintenanceGate.java`，`verifyCurrentSchemaProof`（88–103）。

`validatedContractSha256`只核64hex；完成sourcejournal只核status/database/writeAttempted和steps.size=2，完全不检查两个step内容。两个`{}`也满足现有数组门禁。当前SHOW CREATE的3hash与保护proof内自给hash相同，仅证明当前表等于该声明，未绑定已审70columns/8micro/2generated/7CHECK合同。可证实触发：有3张InnoDB错shape表和相应migrationledger，提供其当前SHOW CREATE hash、自洽descriptor、任意64contractHash、标成功的steps[{},{}]，本方法不会因缺actual material/首跑重跑facts/旧行保护而拒绝。

有限修复：绑定Root已审正式固定合同SHA及严格G28/G29校验器完成journal/rawmaterials/首重跑等证据的保护验证回执；资料绑定必须覆盖exactsource UUID/迁移SQL/policy表结构hash，并拒emptySteps或与已审验证回执不同的sourcejournal。复用正式准备的proof protocol与Root实际完整校验结论，不要求维护entry重写DDLparser或通用迁移器。当前SHOWCREATE可继续作为运行前漂移核验，不能单独承担首次结构正确性的证明。

当前真实新表和正式journal不存在，仍会先拒绝；不声称当前数据库已经错建或activation已执行。

## 正确方向及边界

- `DccLegacyMaintenanceAuthAdapter.withVerifiedActor`（25–55）先要求实际DB exact access-token row，再调用正式checkAccessToken并核两份tenant/user/type/expiry/scopes及当前enabled用户username/nickname；formal API refresh fallback只有access-token row不存在才发生，该adapter先拒，因此方向正确。上下文finally恢复原SecurityContext/tenant/ignore，未制造admin用户/角色。
- `DccLegacyMaintenanceExecutor.execute`在真实Spring@Transactional内currentquality/schema核验→正式registration，预期同一DataSource/物理事务，正式registration再核doc_control/update与实际来源body/Gxpappend；没有另起业务节点。
- Command AtomicBoolean一次失败也不自动重试；stdin只在proof/qualityFacts/logger守卫之后读取，UTF8/长度严格，token字节清零，错误只输出稳定码没有rawcause。Token String只在本次内存作用域。
- Runner仅local-maintenance+enabled=true存在，成功receipt只scope/event/sequence/hash/replay，后close context；普通运行不触发。出错由Spring runner异常停止启动，不执行自动恢复/重试。
- 当前35/39正文proof必在strict factory被拒；“尚未批准”quality facts必被拒；新3表尚无正式schema receipt。现有Entry/Kernel/Gate tests均隔离fixtures/mocks，其中Kernel用真实Gxp内核但Gate是mock，不能当实际QA/schema/OAuth/MySQL运行PASS。

本报告3项发现已直接发Root。源码并行变更后需要按新hash重新检查关闭点；仅静态review，不伪称已复现真实DB/认证运行结果或整体完成。

Root有限范围澄清已纳入R01/R03：只做已有保护证据真实绑定和维护lane日志安全；不扩QA系统、DDLparser或权限平台。等待Owner源码稳定后按新指纹精确复核这三项。

## 修复后单次源码复核（2026-10-04）

结论：R01/R02/R03在Root指定的有限证据边界内**源码关闭**。这是源码审查，不是实际运行/质量批准/DDL验收；本Reviewer未跑Maven、Java测试、DB或服务。当前读时hash见`g33-independent-entry-closure-receipt.json`，保留最初缺陷快照以供追溯。

- **R01**：`requireApprovedQualityFacts`必须加载signedApprovalRecord和qualityAuthorityRecord保护descriptor/rawbytes/hash，Root reviewReceipt精确绑定policyVersion/policyHash/coverage、approvedBy/approvedAt/reference/role/basis/signatureReference和两份资料SHA，并要求真实来源question/message引用。`currentApprovedPolicy`在同一执行事务再次调用此门禁并核实际policy registry、批准人员/role和26operation当前内容。注释明确外部签名真实性/职责由Root人工核实，入口不自动鉴真；未扩大为QA新审批系统。Gate tests源码加入自由引用缺资料、不同person、signedbytes漂移负向；未把这些fixture看作实际批准。
- **R02**：Command实际token读取前先`requireSecretLoggersDisabled`：明确加入真实OAuth2TokenServiceImpl WARN路径，读取LoggingSystem实际logger configurations核保护namespace的effective子logger也OFF。然后`requireSafeMyBatisLogging(sqlSessions)`使用正式依赖注入SqlSessionFactory，核其真实Configuration.logImpl及实际OAuthAccessTokenMapper mapped statements的statementLog均Slf4jImpl，且必须至少存在真实statement，拒StdOut/autoselection/nonSlf4j或空mapper配置。不是mock返回“已关闭”来放行生产。Logging test源码覆盖service WARN、mapper child DEBUG overriding parent、StdOut/NoLogging和正式service实际Redis异常分支日志零token；测试还由Owner跑，Reviewer未执行。
- **R03**：固定contractSHA `f1ec2ec8207b34f0427d021f717c4859d6b71715fbdc436706263ed0fd7e9f0a`与R2validatorSHA `1560ee9ad223b4f65b6393428b84e949422fcc433ce92242285dd762d71c2f56`均与仓库正式准备材料当前rawbytes核对相符。完成journal必须exact迁移ID/SQLSHA/source、原beforedescriptor、first/repeat两个完整phase；material/postflight/oldafter/newledger/stdout/stderr descriptor都按rawSHA读取，exit0/source/SQLSHA/空stderr严格。Root重验回执绑定同journalrawSHA、固定contract/validator及SHOW CREATE hashes，再读当前SHOW CREATE拒漂移。有限入口依赖Root对严格R2validator真正重验完整语义后的保护结论，不重写DDLparser，也不假称此入口独立验证外部执行真实性。Gate tests源码拒任意contract、emptyphase、foreignDB、forgedvalidatorhash与旧wrongshape自洽receipt。

大snapshot门禁已由8MiB提升到**16MiB bounded**；实际9,423,066字节旧snapshot可读取，仍拒大于16MiB。`descriptor`现兼容正式G28/G29 `{path,sha256,bytes}`并严格核bytes整数及实际长度，不能因此接受其它未知字段。原statement/token字节清理、一次性执行、异常safe码、context恢复和same@Transactional保持。

复核中Gate发生一次小改（descriptor接受正式bytes字段并验证）从32d350...7616变到8c6726...1b3e，已再次只读检查并记录最新指纹；不覆盖Owner任何生产文件。当前真实QA仍尚未批准、source39仍不全MATCH、新schema执行证明未具备，维护入口继续拒绝实际登记。最后测试结果由Root/Owner核验，不在此源码结论中声称PASS。

## Final Gate r3 指纹复核

Root发现最终Gate与前次8c6726不同，Reviewer再次单次只读核R01/R03及descriptor16MiB。当前rawSHA04b771396b89495babdf5b132195b3fd505e6df2abf635820328ee718b2338b0；R01外部signed/authority资料SHA与Rootreview subject binding、R03fixed合同/validator和完整phase/保护验证回执binding全部保留。descriptor现在显式区分2字段/path+sha或3字段/另bytes；bytes必须JSON integral且可Long、非负并等实际读取长度，bounded16MiB仍严格。不是凭“format”猜无行为变化，以上方法再次源码核对。其余4生产类rawSHA与前次closure完全相同，无须广审。测试文件Owner改变且正在正式收口，receipt更新其当前hash仅作版本记录，Reviewer没有运行或重审Owner全测试，不将报告101PASS当自身测试结论。
