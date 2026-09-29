# eDHR 反查、活跃订单详情与不合格审批融合静态审计

## 结论与证据边界

融合后的代码会影响主流程，影响不限于查询页面。本次确认 4 项逻辑缺陷，另有 3 项依赖历史数据、编号范围或存储配置的衔接问题，以及 2 项尚不能判定为正常业务故障的风险。已逐项区分，不能把它们都描述为“所有订单都会卡死”。

最应优先处理的是：待处置不合格评审与订单版本升级互相冲突；反查对 MySQL JSON 存储前后不同文本错误验哈希。前者可卡住返工及后续生产；后者可令已上市批次的反查整体不可用，但没有证据证明会撤销上市放行或删除历史记录。

方法：主 Agent 与两个子 Agent 分别审查正式来源写入/反查、前端共享详情、NCR 生命周期，再由主 Agent 核对关键分支及去重。参考 HEAD 为 `895272ee0`；对照 `840b9ef0d`、`800b93a91`、`0ece51c1c`、`8c50567aa`、`7f1d74e60`。最终依据为当前工作区源码，提交标题仅用于定位，不将后续修复或既有缺陷一概归为融合新引入。

本轮没有修改业务代码，没有运行业务测试、构建、E2E、数据库操作或发布。下述是静态因果判断；尚未确认现场历史数据、数据库实际 DDL、存储配置及负载。既有 607 项测试通过不能替代这些场景的验证。跳过 AOCI。

## 主流程影响范围

ERP 同步 → 工艺路线与 QA 正式版本 → 生产/PQC 组长设人员 → 加入活跃订单 → 一线生产/PQC 提交 → 组长复核及双 100% → 完工回执 → PQC 生产放行 → 上传资料 → 上市放行 → 系统历史追溯。

- 反查接口本身为读取；但其配套修改进入了完工快照生成、事件/审核事实冻结和回执校验，不能仅看查询 Controller 就断言与生产无关。
- 不合格评审会冻结工单，处置会修改批次、放行申请、待办及返工执行身份，需要与版本升级、移除等生命周期操作共同检查。
- 共享订单详情既展示证据，也承载资料读取和操作；主详情正确不代表附件一定属于当前订单。
- 未发现本次反查读取直接改写 ERP 同步、人员设置或上市放行结果的证据。上市放行后的历史追溯仍是系统行为，不增加人工归档负责人或 PDF 归档前提。

## 一、明确的逻辑缺陷

| 编号 | 优先级与归因 | 触发操作 | 结果与恢复边界 |
|---|---|---|---|
| EDHR-FUSION-01 | P1，NCR 与既有生命周期融合遗漏 | 先创建待处置 NCR，再批准订单版本升级，随后 QA 选择返工 | 原订单已经移除，返工仍要求原身份，处置回滚；评审及工单持续冻结，新订单也不能正常生产。批准后的升级没有普通撤销/原身份重加恢复路径 |
| EDHR-FUSION-02 | P1，反查新增校验 | 正常完工回执存入仓库定义的 MySQL JSON 列，再查询反查 | 内容未变，仅数据库规范化 JSON 文本，就可能报正式来源哈希冲突；候选范围中一笔命中即可阻断整次反查 |
| EDHR-FUSION-03 | P2，NCR 文件名处理新增 | QA 上传或查看 `100%.pdf` 等合法文件名 | 反复 URL 解码抛异常，处置请求未发出或材料详情显示失败；待处置评审仍冻结 |
| EDHR-FUSION-04 | P2，既有共享组件问题被融合复用扩大 | A 附件请求未结束就切换 B，A 最后返回 | B 主详情显示 A 的资料；可能误看材料，删除则被后端归属校验拒绝。没有证据证明会错删 |

### EDHR-FUSION-01：NCR 冻结没有覆盖版本升级和移除

**触发条件**：正式活跃订单尚无放行申请，工艺或 QA 有可升级版本。QA 创建 NCR 后，订单的 `activeStatus` 仍可参与版本升级。

**代码链**：

1. S01 `createFromActiveOrder` 附近（210–252 行）把评审绑定原 `activeOrderId`，冻结工单。
2. S02 `preview`（142–175 行）检查新版及放行申请；提交和 `applyApprovedUpgrade` 没有检查 NCR。审批通过（317–362 行）移除旧身份并为同工单创建新订单。
3. S03 `removePendingVersionUpgradeOrder`（340–350 行）持久化 `REMOVED/VERSION_UPGRADED`。
4. S01 返工处置（424–425 行）仍传原身份给 S04 `start`；S03 `retireForRework`（141–150 行）仅接受 ACTIVE/CLOSED 与 ACTIVE/COMPLETED/RELEASED，更新为 0，S04 抛 `REWORK_SOURCE_STATE_CHANGED`。
5. 整个处置事务回滚，评审和冻结保留。S01 `ensureWorkOrderNotFrozen` / `ensurePqcSubmissionNotFrozen`（567–602 行）继续阻止同工单生产/PQC。

**同根因的移除分支**：S05 `removeActiveOrder`（1825–1837 行）也没有 NCR 门禁；无放行申请、无报工的新单可被冻结后移除，变成 `REMOVED/REMOVED`，同样使返工失败。普通移除在符合唯一历史记录及当前配置前提时可重新激活原 ID（2270–2358 行）；但会清理运行历史并重建快照，不能当成无副作用恢复。版本升级产生的旧身份明确排除在重加恢复之外。

**修改方向**：在移除、版本升级预览/提交/批准应用处统一核对阻断性 NCR 与工单冻结，写入时在一致锁边界复核，覆盖“升级已申请后才出现冻结”的竞争情况。已经形成冲突的数据需要明确恢复方案，不能改成让步来掩盖原本应返工的质量结论。

**验收场景**：Given 待处置 NCR；When 组长移除或申请/应用版本升级；Then 原身份、证据和冻结状态保持一致，并明确拒绝冲突动作。另覆盖正常无 NCR 升级、未批准取消后正常返工。

### EDHR-FUSION-02：反查把数据库 JSON 文本变化误判为证据变化

**代码链**：S06 `prepare`（165–202 行）先对内存序列化 JSON 计算 `SHA256(SHA256(sourceJson) + "|" + lossJson)`；S07 的 `formal_source_snapshot_json`（16 行）和 `loss_condition_facts_json`（31 行）是原生 JSON 列。S08 `validate`（60–62 行）从数据库读出后直接对字符串重新计算相同公式。

MySQL 对 JSON 存储/输出进行规范化，包括空白及对象键顺序，无法保证还原写入前字符串。该行为已经核对 [MySQL 官方 JSON 说明](https://dev.mysql.com/doc/refman/8.0/en/json.html)。因此，即使字段值完全相同，两次字节哈希也会不同。仓库未检索到把这两列迁移为文本的 SQL；实际部署 DDL 本轮未读取。

S09 `validateCommonChains` / `toChainContext`（718–754 行）先对锚点和所有候选调用上述校验，目录、查询和证据入口均受影响。后续 `MesTeamLeaderActiveOrderCompletionReceiptHash` 对 JSON 做规范化，不能补救前一处已经抛出的异常。

**影响**：反查不可用、误报来源冲突。正常历史列表和上市状态不是同一个接口，不能据此说“上市没有成功”或“系统没有归档”。内存 Mockito mapper 不模拟 JSON 数据库往返，不能证明此处正确。

**修改方向**：明确持久化后稳定的规范摘要协议并让写入与读取一致；已有不可变回执的哈希、来源绑定必须按受控方案处理，不能直接重算覆盖正式审计证据，也不能取消所有完整性验证。

**验收场景**：Given 真实 writer 生成的完整回执；When 经 MySQL JSON 列保存并重新读取；Then 内容相同反查成功，修改实际字段仍明确拒绝。必须验证数据库往返，不能只在内存里传同一个字符串。

### EDHR-FUSION-03：合法百分号文件名使不合格处置失败

S10 `decodeFileName`（358–372 行）循环调用 `decodeURIComponent`，直到文本不变。`100%25.pdf` 第一轮变成 `100%.pdf`，下一轮抛 `URIError`；正式文件名本就是 `100%.pdf` 时第一轮即失败。`+` 也被按表单编码处理成空格，可能改变合法路径文件名。

S10 `resolveReviewMaterialName`（399–400 行）对服务端原始名称再次解码；`buildReviewMaterials`、上传事件和 `handleDispose`（409–414、544–557、583–599 行）共用该逻辑。S01（1126 行）正式材料保存的是 `FileDO.name`，没有约定它是 URL 编码字符串。共享 UploadFile 的回显也存在相同循环。

**影响**：某些合法资料无法用于让步/返工/作废，评审保持待处置；已保存的材料可能不能正常显示。新上传可改名后重传，但不能要求删除历史审计材料来解决回显错误。

**修改方向**：正式文件名直接展示；URL 路径解码按明确编码协议处理，避免对已解码字符串重复猜解码。与通用上传组件同时核对。

**验收场景**：Given 中文、百分号、加号和已编码名称；When 上传、处置、重新打开详情；Then 原名准确、请求成功、无 URIError，已有材料无需删除重传。

### EDHR-FUSION-04：共享详情附件请求缺少订单隔离

S11 `loadDetail`（115–143 行）有主请求序列保护；S12 `loadDossierFiles`（3937–3959 行）没有保护。其 watcher（4121–4127 行）在订单/申请变化后发起新请求，旧请求成功、失败及 finally 仍可覆盖当前文件、错误和 loading。

从反查结果打开另一批次会进入同一路由的不同 query。若旧订单附件返回较晚，主详情已经是 B，而附件来自 A。预览按行内 fileId 打开；删除使用当前订单加旧附件 ID，服务端材料归属校验会拒绝该不匹配组合，因此不认定错删或越权。

**修改方向**：用订单、申请身份及请求序列隔离附件响应；切换时清除旧内容，旧请求的错误和 finally 也不得修改新页面。

**验收场景**：Given A 的资料请求比 B 慢；When 从 A 切到 B；Then 无论 A 成功或失败，最终只显示 B 的资料与状态。

## 二、需要核实现场条件的衔接问题

这些不是“当前部署已经发生”的断言，但触发后果可以从源码推导。

### EDHR-FUSION-C01：融合前已完工、尚未申请放行的旧回执无法继续重放

- **前提**：存在反查快照扩展前生成的完工回执，尚未建立放行申请。
- **依据**：融合前 S06 的 `canonicalSourceSeed` 没有 `productionFacts`，当前 711 行会增加该字段。S13 `matches`（23–37 行）比较整个新旧来源对象，不能相等；`completeForRelease` 会调用既有回执重放分支，随后报幂等冲突。
- **证据强度**：现有 `MesProEdhrFrozenProductionWriterReaderR3Test.legacyReceiptReplayDoesNotManufactureNewFrozenEvidence` 明确断言拒绝缺少该字段的旧回执。这是有意保护不可变证据的行为，问题在于存量衔接没有正常继续路径，不能把“拒绝伪造历史”本身当成错误。未发现本轮相关数据迁移，未核实现场是否存在这种中间态。
- **影响**：受影响旧单无法建立首次放行申请；重新点击无效。新版本从头完成的新单不属于本项；已有申请重放也不应笼统归入。
- **处理/验收**：先只读盘点该类旧单，再决定受控升级方案或明确业务恢复流程；不能悄悄用今天的数据补成历史冻结证据。Given 真实旧回执且无申请；When 升级后申请；Then 按批准的存量策略继续或有可执行恢复路径，不停留在无法处理的通用幂等冲突。

### EDHR-FUSION-C02：合法大 Long 编号被完工重放当作非法关联

- **前提**：回填记录 ID 达到 `9007199254740991` 或更大。默认 MySQL 自增新库通常不会很快达到，本轮没有现场编号范围证据。
- **依据**：项目 `NumberSerializer`（19–36 行）将这类 Long 写为十进制字符串，生产 Jackson 配置也注册到 JsonUtils；S13 `normalizeCompletions`（52–55 行）却只接受 `isIntegralNumber()`。S08 的身份校验已接受正式十进制字符串，两处合同不一致。
- **影响**：真实、未改变的回填关联在重复完工或后续首次申请时被拒绝。本项位于融合后的重放修复代码，不笼统归因于反查最初提交。
- **处理/验收**：精确支持项目规定的数字/十进制字符串身份，不用浮点转换；使用生产 ObjectMapper 覆盖边界以下、边界值、大 ID、伪造/溢出字符串，验证同来源重放成功、错误关联拒绝。

### EDHR-FUSION-C03：若使用对象存储 URL，NCR 合法上传材料仍不能用于处置

- **前提**：主文件配置使用直接返回 S3/MinIO 对象地址的存储客户端，URL 不含 `/admin-api/infra/file/{configId}/get/`；本轮未读取现场存储配置。
- **依据**：通用上传返回 `FileServiceImpl.createFile` 的客户端 URL，`S3FileClient` 支持对象/签名 URL；S01 `parseAdminFileAccessLocation`（1190–1209 行）只接受本地代理地址形状，其他地址报材料必填错误。所有处置都需要材料解析。
- **影响**：上传成功但让步、返工、作废均不能完成，冻结持续；在同一配置下重复上传无效。该限制在融合前已有，不能计作本次新增。
- **处理/验收**：资料关联使用正式文件 ID，并验证文件记录、归属和授权；避免通过 URL 外形推导身份。Given 项目正式支持的不同存储客户端；When 上传并提交 NCR 处置；Then 同一正式文件身份能被正确校验，跨文件/无权限请求仍拒绝。

## 三、待验证风险，不作为已确认主流程 BUG

| 编号 | 代码事实 | 目前限制 | 后续验证 |
|---|---|---|---|
| EDHR-FUSION-R01 | S11 `resolveDetailQuery` 优先使用 activeOrderId，而反查锚点继续取 batchExecutionId；后端也优先前者 | 手工构造冲突双 ID 链接会产生 A 详情/B 反查；已核对的正常跳转只传一种身份，未发现正常操作生成冲突链接 | 明确参数二选一或验证二者正式关联；覆盖冲突链接被明确拒绝 |
| EDHR-FUSION-R02 | S09 `loadReleasedCandidates`（565 行起）扫描范围内全部已上市候选；`calculateCatalogSnapshot`（504 行起）逐批次读取各来源，查询/证据入口会再次计算 | 结果页大小不限制这些读取；大历史量可能慢/超时，但未做负载验证，不能说已经使生产服务不可用 | 用真实历史量与并发测延迟、查询数、内存；优化需保留查询完整性，不能静默截断结果或跳过异常批次 |

## 四、与历史待完善列表的关系

此前 EDHR-FLOW-07、16～21 仍单独记录在 `docs/bugs/20260928-edhr-cycle-reaudit.md`。本轮没有新的修复证据关闭它们，不重复编号、不重复计入上面 4 项。

| 既有编号 | 已登记卡点 |
|---|---|
| 07 | 单独完工后 ERP 修改批次编码，回执与实时来源冲突 |
| 16 | 旧详情预检创建失败事务，占用随后 PQC 正式初始化 |
| 17 | 旧资料节点被上市审批锁阻断；不是所有上市放行必失败 |
| 18 | 旧资料保存把多个负责人的待保存节点一起检查权限 |
| 19 | 回执已存在时重建/移除再加入清掉来源，旧回执仍占身份 |
| 20 | 多物料同损耗原因初次提交合法，更正时被当成全局重复 |
| 21 | 当前“其他上传”没有传放行申请 ID，跨角色列表/上传/预览受阻 |

本轮再次看到 21 的调用缺口，它可能遮蔽附件竞态，但不会消除竞态；生产组长具备两单读取权限时仍可能触发 FUSION-04。01 的 NCR 返工源状态冲突与旧 19 的回执遗留不是同一根因。

## 五、优先顺序和未判为 BUG 的行为

1. 优先修复 FUSION-01、02：分别影响返工闭环及正常反查。
2. 修复 FUSION-03、04，并核实 C01 是否有受影响存量；C03 若现场使用对象存储则提升为立即处理。
3. C02 按正式序列化合同补齐；R01、R02 做契约/负载验证后决定实施范围。

待处置 NCR 阻止报工/放行属于业务保护；作废后只读属于正确终态；来源缺失不伪造历史属于证据要求。NOWAIT 锁冲突回滚可重试，不能仅凭存在 NOWAIT 认定永久死锁。后台继续校验文件归属，不能把前端错显示直接写成越权或错删。

## 源码索引

所有路径相对仓库根；行号用于导航，代码变化后按方法名重新核对。B = `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/`。

| 锚点 | 路径 |
|---|---|
| S01 | B + `service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java` |
| S02 | B + `service/pro/processpool/team/MesTeamLeaderActiveOrderVersionUpgradeServiceImpl.java` |
| S03 | B + `dal/mysql/pro/processpool/team/MesProcessPoolActiveOrderMapper.java` |
| S04 | B + `service/pro/processpool/team/MesActiveOrderReworkCycleService.java` |
| S05 | B + `service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java` |
| S06 | B + `service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionBackfillPortImpl.java` |
| S07 | `IntRuoyiBackend/sql/mysql/20260822_mes_process_pool_active_order_completion_receipt.sql` |
| S08 | B + `service/pro/batchrecord/MesProEdhrReverseTraceReceiptValidator.java` |
| S09 | B + `service/pro/batchrecord/MesProEdhrReverseTraceServiceImpl.java` |
| S10 | `IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue` |
| S11 | `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue` |
| S12 | `IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue` |
| S13 | B + `service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionSourceEvidence.java` |

补充正式协议来源：

- `IntRuoyiBackend/yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/util/json/databind/NumberSerializer.java`
- `IntRuoyiBackend/yudao-framework/yudao-spring-boot-starter-web/src/main/java/cn/iocoder/yudao/framework/jackson/config/YudaoJacksonAutoConfiguration.java`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/service/file/FileServiceImpl.java`
- `IntRuoyiBackend/yudao-module-infra/src/main/java/cn/iocoder/yudao/module/infra/framework/file/core/client/s3/S3FileClient.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFrozenProductionWriterReaderR3Test.java`

逐项修复完成的标准是对应行为验证通过；本报告的文档检查不代表业务已验收。
