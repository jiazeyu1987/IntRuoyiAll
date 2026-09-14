# DCC 源文件独占与 SHA-256 证据治理 PRD

## Goal

为测试环境中冻结边界内的每条有效 DCC 受控文件记录建立可验证的源文件证据：一条受控记录对应一个独占源文件记录，源文件正文可读取，SHA-256 可重算且一致，治理前后来源、操作者、时间和规则均可追溯。

无法获得可靠证据的记录必须进入明确的 `BLOCKED` 状态，不得被猜测、默认补齐或静默排除。治理完成后重跑 Windchill 迁移全量只读盘点，以新的 `AUTO_MAP` 和 blocker 明细决定下一阶段，而不是直接开始 Revision / Iteration 回填。

## Scope

### 第一阶段范围

1. 以授权业务租户、受控文件 ID 上限和全局引用核验边界冻结只读盘点，统一“有效受控记录”为 `dcc_controlled_file.deleted = 0` 且在冻结边界内的记录。
2. 逐条核验受控记录的 `source_file_id`、infra 文件记录、文件存储定位、正文可读性、SHA-256 和现有 ownership。
3. 生成版本化治理清单，将记录确定性分类为 `READY` 或 `BLOCKED`，保存稳定原因码、快照摘要和规则版本。
4. 对满足前置的独占源文件建立 ownership；对共享源文件组内每条有效受控记录创建字节一致的独立源文件副本，再建立 ownership。
5. 保存治理前源文件、治理后独占源文件、存储定位快照、SHA-256、处理动作、执行人、执行时间、状态和失败信息。
6. 支持有界、可暂停、可续跑且幂等的执行；源文件 ID、文件状态或正文哈希发生漂移时停止该记录并报告 blocker。
7. 执行 postflight，确认独占、哈希和影响隔离；随后重跑完整 Windchill 迁移只读盘点。

### 目标对象

- 当前测试库最新冻结边界内的有效 `dcc_controlled_file` 记录。
- 这些记录引用的全局 `infra_file`、现有 `dcc_controlled_file_source_ownership` 和 `dcc_controlled_file_source_migration` 证据。
- 当前盘点已知的缺失 ownership/hash 记录、共享源文件组和软删除源文件引用；实际范围以写入前最新只读盘点为准。

### Target Users

- 文控管理员：查看盘点汇总、下载治理清单、确认可执行批次、查看 blocker 和完成结果。
- 数据治理执行人：在明确授权和维护窗口内运行有界批次、暂停、续跑并处理失败。
- 审计/测试人员：从受控记录追溯原始源文件、独占副本、哈希和治理事件，并独立重算结果。
- Windchill 迁移负责人：使用重跑盘点结果判断能否产生确定性迁移候选。

## Non-Goals

- 不在本任务中创建或回填 Master / Revision / Iteration、Checkout、发布快照等 Windchill 第一阶段目标表。
- 不在本任务中治理项目、分类叶子、文件编码、平台 ACTIVE、空 Master、重复版本、正式指针或当前检出异常。
- 不恢复、删除或重建无法证明来源的历史正文；软删除或物理缺失源文件在没有正式证据时保持 `BLOCKED`。
- 不依据文件名、目录、版本号、创建时间、发布文件、最新版本或内容相似度推断源文件归属。
- 不改写审批流程、电子签名、发布文件、盖章文件、分发、培训、打印和访问历史。
- 不建设生产环境发布、安全加固、灾备或容量方案；当前仅测试环境。
- 不默认建设新的前端管理页面；第一阶段可复用/扩展管理 API 和治理报告，页面需要另立范围。
- 不承诺治理后 `AUTO_MAP` 必然大于 0；其他迁移 blocker 仍由完整盘点判定。

## Evidence Reviewed

- `docs/product/dcc-windchill-version-phase1-prd.md`
- `docs/system/dcc-windchill-version-phase1-migration-inventory.md`
- `docs/system/dcc-windchill-version-phase1-data-model.md`
- `docs/system/dcc-windchill-version-phase1-backend-api-design.md`
- `doc/tasks/20260904-dcc-windchill-version-test-db-inventory/inventory-report.md`
- `IntRuoyiBackend/sql/mysql/20260811_dcc_source_ownership.sql`
- 当前 source ownership/migration 服务、Mapper、管理接口及其单元测试、schema 测试。

## User or System Scenarios

### Scenario S-01：可读且未共享的源文件

Given 一条有效受控记录拥有非空源文件引用，文件记录有效、正文可读、未被其他有效受控记录引用，且没有冲突 ownership，When 系统完成只读核验并经批准执行，Then 系统可保留该源文件 ID、写入正文 SHA-256 和独占 ownership，并记录完整来源证据。

### Scenario S-02：多个受控记录共享一个源文件

Given 一个全局 `infra_file` 源文件被冻结任务范围内两个或更多有效受控记录引用，When 系统形成治理计划，Then 按全局 `source_file_id` 聚合整个共享组；系统还必须在受控数据库读取权限下检查冻结范围外是否有其它租户的有效引用。若发现范围外引用，整个共享组进入 `BLOCKED/SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE`，不能按租户分别认领。只有全局引用核验通过时，执行才允许为组内每条有效受控记录创建新的字节一致独立副本，原共享源文件仅作为来源证据保留。

### Scenario S-03：源文件引用缺失或文件不可用

Given 受控记录没有 `source_file_id`，或全局源文件记录不存在、已软删除、定位字段不完整、正文不可读，When 系统盘点，Then 该记录进入 `BLOCKED`，返回稳定原因和可操作说明，不写受控记录指针或伪造 hash。`infra_file` 没有 `tenant_id`，不把源文件租户不一致作为直接校验条件。

### Scenario S-04：已有 ownership 但证据无效

Given 已有 ownership 行，但其 source_file_id 与受控记录不一致、文件不可用、正文重算 hash 不一致或源文件同时归属其他记录，When 系统核验，Then 该记录不得计为“已治理”，必须进入对应 `BLOCKED`，保留原行供后续更正设计处理。

### Scenario S-05：执行前发生漂移

Given 治理清单中的记录已是 `READY`，但执行时源文件 ID、文件状态、存储定位或正文 SHA-256 与冻结快照不同，When 系统尝试提交，Then 本条处理失败关闭并转为 `BLOCKED` 或明确的漂移失败状态，不创建 ownership、不切换指针。

### Scenario S-06：执行中断后续跑

Given 一个批次在若干记录完成后因明确错误停止，When 执行人使用同一治理清单和任务键续跑，Then 已完成记录不重复复制或改写，未完成记录从其证据状态继续；不同清单内容复用同一任务键必须冲突。

### Scenario S-07：治理完成后重新评估迁移

Given 所有可处理记录完成且剩余记录均有 blocker，When 系统执行 source postflight 和完整 Windchill 迁移只读盘点，Then 报告新的有效 ownership/hash 数量、`AUTO_MAP` 数量和其他剩余 blocker，且不自动启动 Revision / Iteration 迁移。

## Functional Requirements

### FR-01 冻结只读盘点

1. 每次治理必须先记录环境、数据库、授权业务租户范围、统计时间、`snapshotMaxControlledFileId`、有效受控记录总数和规则版本，不记录凭据或文件正文。
2. 盘点查询必须为只读；受控记录统计使用相同授权业务租户、`deleted = 0` 与冻结 ID 上限口径，全局 `source_file_id` 引用关系必须使用受控数据库读取权限在冻结范围内完整核验。
3. 写入前必须重新盘点；先前报告中的 18,065、43、7 等数字只能用于对比，不能直接作为写入清单。

### FR-02 逐条证据核验

1. 每条记录必须核验受控文件存在且有效、source_file_id 非空、全局文件记录存在且有效、config/path 可解析、正文可读取；不得因为 `infra_file` 无 `tenant_id` 而伪造租户一致结论。
2. 系统必须对实际正文计算小写 64 位 SHA-256；不能使用文件名、路径、大小、已有任意 hash 文本或 published/stamped 文件替代源文件正文哈希。
3. 系统必须核验现有 ownership 的 controlled_file_id、source_file_id、origin_source_file_id、source_sha256 和当前正文是否一致。
4. 核验输出必须包含原始定位证据的不可变快照摘要，使审计人员能知道哈希对应哪份文件记录；不得在报告中嵌入正文。

### FR-03 候选分类和 blocker

1. 只有全部自动核验通过的记录才能进入 `READY`；`READY` 不等同于已完成治理。
2. 至少提供以下稳定 blocker 原因：`SOURCE_REFERENCE_MISSING`、`SOURCE_REFERENCE_NOT_IN_GLOBAL_INDEX`、`SOURCE_RECORD_MISSING`、`SOURCE_RECORD_DELETED`、`SOURCE_LOCATION_INCOMPLETE`、`SOURCE_CONTENT_UNREADABLE`、`SOURCE_HASH_MISMATCH`、`OWNERSHIP_POINTER_MISMATCH`、`OWNERSHIP_CONFLICT`、`SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE`、`SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE`、`SNAPSHOT_DRIFTED`。不设置“源文件租户不一致”原因，因为 `infra_file` 是全局表且没有 `tenant_id`。
3. `BLOCKED` 必须保存受控文件 ID、租户、原 source_file_id、原因码、脱敏说明、发现时间和规则版本。
4. 不得用默认源文件、最新文件、文件名相似度或共享组第一条记录自动消除 blocker。
5. 不可判定的业务证据使用 `BLOCKED`；意外技术故障使用明确失败状态。两者不得混为“已完成”或静默跳过。

### FR-04 治理清单确认

1. 治理清单必须包含 schema 版本、规则版本、冻结边界、逐条动作、预期旧 source_file_id、盘点 SHA-256、共享组标识、状态、生成时间和清单 SHA-256。
2. 写批次只接受状态为已确认且清单 SHA-256 与当前内容一致的治理清单。
3. 清单中的 `BLOCKED` 记录不得进入写批次；清单外或超过冻结边界的新记录不得被本次批次处理。

### FR-05 独占源文件治理

1. 非共享且未被有效 ownership 占用的可读源文件，可以保留原 source_file_id 并建立 ownership；“非共享”必须由受控数据库读取权限下的全局引用核验得出。
2. 共享组必须按冻结任务范围内的全局 `source_file_id` 聚合。若数据库读取权限确认冻结范围外存在其它租户有效引用，整组进入 `SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE`，不得按租户分别认领或复制其中一部分。
3. 全局引用核验只有在受控数据库读取权限下执行，不扩大普通业务用户或业务租户的文件可见性；权限不可用时进入 `SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE`。
4. 每份副本必须从冻结清单指定的源正文创建；复制后重新读取并计算 SHA-256，只有与盘点源 SHA-256 相等才能提交。
5. 对复制场景，受控记录 source_file_id 必须指向新独占文件，ownership 的 source_file_id 指向同一新文件，origin_source_file_id 保留治理前全局共享文件 ID。
6. 对不复制场景，ownership 的 source_file_id 和 origin_source_file_id 均指向原源文件，并保存同一正文哈希。
7. 每个有效受控记录至多一个有效 ownership；完成全局引用核验后，每个治理后源文件在全局有效引用集合中至多属于一个有效受控记录。若现有表约束只覆盖租户，必须在设计阶段补足跨租户防复用方案。

### FR-06 原子提交和漂移保护

1. 提交前必须重读受控记录、源文件记录和正文 hash，并与清单中的旧 source_file_id、文件状态、定位摘要和 SHA-256 比较。
2. 任一字段漂移必须阻止该记录提交，不得把新状态自动吸收到旧清单。
3. source_file_id 更新、ownership 写入和迁移证据完成必须原子提交；任一环节失败不得留下“指针已切换但无 ownership”或“ownership 已完成但指针不一致”。
4. original_file_id、published_file_id、stamped_file_id、审批、签名及其他历史关系不得被本事务修改。
5. 文件副本创建后、数据库事务提交前失败时，必须把副本标为可追踪的待清理对象；清理失败必须显式报错，不能吞掉。

### FR-07 批次、幂等和续跑

1. 单批大小必须有明确上限，默认不超过现有 200 条能力，并支持维护窗口内暂停。
2. 每个执行请求必须有稳定任务键和请求摘要。同键同摘要返回原处理结果；同键不同摘要返回冲突。
3. 已 `COMPLETED` 的记录再次执行不得重复复制、修改认领时间或生成第二条 ownership。
4. 已存在 `COPY_VERIFIED` 副本的重试只能在重新读取副本且 hash、状态、来源关系和全局 source_file_id 引用核验均符合清单时复用；不能以文件租户字段作为校验项，否则阻塞，不能重新猜测副本。
5. 一个记录失败后不得把该记录计为已处理成功；批次返回已完成、已阻塞、失败和剩余数量，并明确停止位置或继续策略。

### FR-08 审计和查询

1. 文控管理员必须能按授权业务租户、冻结边界、状态、blocker 原因、全局共享组和受控文件 ID 查询明细与汇总；全局引用的其它租户明细只允许在受控数据库审计结果中以脱敏计数/标识呈现，不扩大业务租户可见性。
2. 每条完成记录必须显示治理前 source_file_id、治理后 source_file_id、origin_source_file_id、SHA-256、动作、执行人、执行时间、规则版本和清单 hash。
3. 所有关键失败和漂移必须留痕；审计写入失败时对应记录不得返回成功。
4. 查询和导出不得包含文件正文、访问令牌、密码或连接凭据。

### FR-09 Postflight 与 Windchill 重跑门禁

1. source postflight 必须逐条重新读取治理后的源正文并计算 SHA-256，核对 ownership 与当前 source_file_id。
2. postflight 必须证明：一个有效受控记录至多一个有效 ownership；一个独占源文件至多一个有效 owner；所有 `COMPLETED` 记录 hash 一致；共享引用在治理完成集合中为零。
3. postflight 必须核对 original/published/stamped 指针以及审批、签名等既有证据没有被本任务改写。
4. source postflight 通过后，必须按最新冻结边界重跑完整 Windchill 迁移只读盘点，并输出 `AUTO_MAP` 与全部剩余 blocker。
5. 只有完整盘点中相关源文件门禁通过的记录才可进入后续迁移候选；本任务不得自动启动下一阶段。

## Non-Functional Requirements

- **一致性**：受控记录指针、ownership 和迁移证据必须在事务成功后相互一致。
- **可追溯性**：任意 `COMPLETED` 记录可从受控记录追到治理前源、治理后源、正文哈希、治理规则和执行事件。
- **确定性**：相同冻结数据和规则版本产生相同 `READY / BLOCKED` 分类；不得依赖无稳定顺序的查询结果决定业务归属。
- **幂等性**：中断重试不能产生第二份有效 ownership 或无法解释的重复副本。
- **租户隔离**：受控记录的盘点、复制、认领、查询和导出均限定当前授权业务租户；全局 `infra_file` 引用核验只能由受控数据库读取权限完成，发现冻结范围外其它租户有效引用时整组阻塞，不能扩大业务租户可见性。
- **可恢复性**：执行以小批次推进，保存完成游标和每条证据；失败时能够从最后一个明确状态续跑，不把失败报告成成功。
- **负载可控**：实施前报告待读取字节数、预计复制字节数和批次上限；实际执行不得未经评审占满运行服务或对象存储。
- **可测试性**：核心分类、哈希、共享复制、漂移、幂等、事务失败和 postflight 必须可以用自动化测试重复验证。
- **兼容边界**：治理只改变 source_file_id 及其正式 ownership/migration 证据，不改变用户当前正式文件读取和历史审计语义。

## Business Rules

1. “证据完整”同时要求：受控记录有效、全局独占源文件记录有效、正文可读、SHA-256 重算一致、ownership 两端指针一致、全局引用核验完成、来源链和治理审计完整；不把不存在的 `infra_file.tenant_id` 当作条件。
2. ownership 行存在但任一证据条件失败时，状态仍为 `BLOCKED`，不能计入完成数。
3. 共享来源说明技术引用不独占，不代表任何一个受控记录天然拥有该全局源文件。共享组按冻结任务范围全局聚合；范围外存在其它租户有效引用或无法完成受控全局核验时整组 `BLOCKED`，只有核验通过后才默认全部复制，原文件仅保留为 origin 证据。
4. 两份正文 SHA-256 相同只证明内容相同，不证明业务归属相同；不能因为 hash 相同合并 ownership。
5. 对软删除、缺失或不可读正文，published/stamped/original 副本不能未经正式业务确认自动替代 source。
6. `BLOCKED` 是诚实的最终治理结果之一；任务完成可以包含 blocker，但不得声称这些记录已经可迁移。
7. 当前快照中的数字只用于基线对比；执行和验收均以最新冻结边界产生的数字为准。

## States and Transitions

```text
INVENTORIED
  -> READY                  自动证据核验全部通过
  -> BLOCKED                缺失、软删除、冲突、不可读或不可判定

READY
  -> COPY_VERIFIED          需要复制且副本字节哈希已验证
  -> COMPLETED              保留独占原源并原子写入证据
  -> BLOCKED                执行前发生可解释的数据漂移
  -> FAILED                 非业务歧义的技术执行失败

COPY_VERIFIED
  -> COMPLETED              原子切换指针并写入 ownership/迁移证据
  -> BLOCKED                副本或原始来源与清单漂移
  -> FAILED                 技术提交失败

FAILED
  -> READY                  原因已排除并重新只读核验、重新确认清单
  -> BLOCKED                复核后确认属于证据缺失或业务歧义

BLOCKED
  -> READY                  获得正式新证据，重新盘点并由新清单确认
```

禁止跳过 `READY` 从 `INVENTORIED` 直接写入 `COMPLETED`。`BLOCKED` 不得由重试次数或默认规则自动转为 `READY`。

## Dependencies and Constraints

- 依赖现有 infra 文件服务能够按 file record 读取正文并创建独立副本。
- 依赖 `dcc_controlled_file_source_ownership` 和 `dcc_controlled_file_source_migration` 的 schema；如果现有字段无法表达清单版本、blocker 原因或定位快照，需先形成 additive schema 设计及迁移测试，不能把信息塞入模糊 error_message。
- 依赖当前 DCC 受控记录和文件存储在维护窗口内可冻结或执行提交前漂移检查。
- 依赖文控角色、受控文件更新权限和业务租户上下文；全局 source_file_id 引用核验另依赖受控数据库读取权限，不能通过扩大业务租户可见性代替，不允许跨租户业务批次。
- 依赖写入前明确授权、运行态数据源核对、恢复点、对象存储空间/读取量评估和结果确认人。
- 实施必须遵守 BDD + strict TDD；生产代码变化必须先有 RED，再以 GREEN 验证。
- 真实 E2E 只有用户当轮明确授权时才能执行；未执行 E2E 不能用 API 或数据库查询冒充页面验收。

## Acceptance Criteria

- **AC-01 冻结口径一致**：Given 最新测试库待治理，When 执行只读盘点，Then 受控记录汇总与逐条明细使用同一授权业务租户、`deleted = 0`、冻结 ID 上限和规则版本，并对全局 `source_file_id` 做受控数据库范围核验，且不会写数据库或对象存储。
- **AC-02 独占源直接认领**：Given 有效受控记录的源文件有效、正文可读、未共享且无冲突 ownership，When 执行已确认治理动作，Then source_file_id 保持不变，ownership 两端指针一致，重算 SHA-256 等于保存值。
- **AC-03 共享组全部隔离**：Given 三条有效受控记录在冻结范围内共享同一全局 source_file_id，且受控数据库读取权限确认冻结范围外无其它租户有效引用，When 执行该共享组治理，Then 三条记录分别指向三个不同的新源文件，各副本 hash 与原正文相同，origin_source_file_id 均指向原共享文件，原共享文件不再作为三条记录的当前 source_file_id；若范围外存在有效引用，则整组改为 `BLOCKED/SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE` 且不复制。
- **AC-04 缺少引用阻塞**：Given 有效受控记录的 source_file_id 为空，When 盘点，Then 记录状态为 `BLOCKED/SOURCE_REFERENCE_MISSING`，不创建文件、ownership 或默认 hash。
- **AC-05 软删除阻塞**：Given source_file_id 指向已软删除的全局 infra 文件记录，When 盘点或执行，Then 记录状态为 `BLOCKED/SOURCE_RECORD_DELETED`，不得根据现有 ownership 或其他副本宣布完成。
- **AC-06 正文不可读阻塞**：Given 文件记录存在但正文无法读取，When 盘点，Then 记录状态为 `BLOCKED/SOURCE_CONTENT_UNREADABLE`，报告脱敏原因且无业务写入。
- **AC-07 无效 ownership 不计完成**：Given ownership 行的 source_file_id、受控记录指针或重算 hash 任一不一致，When 统计治理完成量，Then 该记录不计入有效 ownership，并以稳定原因进入 `BLOCKED`。
- **AC-08 禁止弱字段猜测**：Given 两个文件名、路径、版本号或 hash 相同但没有正式归属证据，When 生成治理清单，Then 系统不自动合并、替换或认领它们；全局 source_file_id 引用只能由受控数据库读取权限核验，不能扩大业务租户可见性，仅按各自受控记录和已验证来源处理或阻塞。
- **AC-09 清单确认门禁**：Given 治理清单未确认、schema/规则版本不匹配或清单 SHA-256 改变，When 请求运行批次，Then 请求失败且没有文件复制、指针更新或 ownership 写入。
- **AC-10 快照漂移失败关闭**：Given `READY` 后源文件 ID、记录状态、定位或正文 hash 改变，When 尝试提交，Then 该记录以 `SNAPSHOT_DRIFTED` 阻塞，source_file_id 和 ownership 保持提交前状态。
- **AC-11 原子性**：Given 副本创建成功但 source 指针、ownership 或迁移证据任一步写入失败，When 事务结束，Then 数据库中不存在部分成功组合，副本有明确待清理/清理结果，执行结果不为 `COMPLETED`。
- **AC-12 重复执行幂等**：Given 一条记录已 `COMPLETED`，When 使用同一任务键和请求摘要重试，Then 返回原结果，不创建新副本、不新增 ownership、不改写认领时间；同键不同摘要明确冲突。
- **AC-13 续跑可验证**：Given 批次处理若干条后停止，When 使用原确认清单续跑，Then 已完成条目保持不变，处理从未完成条目继续，结果计数能与逐条状态对账。
- **AC-14 租户隔离**：Given 当前业务清单只包含租户 A，但全局 source_file_id 引用核验发现租户 B 在冻结范围外有效引用同一源文件，When 盘点或执行，Then 该全局共享组以 `SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE` 阻塞；受控数据库只返回完成判定所需的脱敏引用结果，不扩大租户 A 的业务文件可见性，也不修改租户 B 数据。
- **AC-15 历史证据不变**：Given 受控记录已有 original/published/stamped 指针、审批、签名、分发、培训、打印和访问历史，When 完成源文件治理，Then 这些非 source 证据的主键、指针、版本标签和哈希与治理前快照一致。
- **AC-16 source postflight**：Given 一个治理批次完成，When 执行 postflight，Then 所有 `COMPLETED` 记录均满足当前 source_file_id = ownership.source_file_id、正文重算 hash = ownership.source_sha256，且完成集合中共享 source_file_id 数为 0。
- **AC-17 blocker 可解释**：Given 任一记录未完成，When 文控管理员查询或导出，Then 能看到受控文件 ID、业务租户、原 source_file_id、稳定 blocker/失败原因、发现时间、规则版本和下一步所需证据；若原因为范围外全局引用，只显示受控数据库核验所需的脱敏计数/标识，不出现空成功或静默遗漏。
- **AC-18 全量迁移盘点重跑**：Given source postflight 通过，When 重跑完整 Windchill 迁移只读盘点，Then 报告最新 `AUTO_MAP`、源文件门禁和其他 blocker 数量；系统不会自动创建 Revision / Iteration 或把 remaining blocker 标记为通过。
- **AC-19 当前基线问题可复现或解释**：Given 2026-09-04 报告曾记录 18,065 条缺失、43 个共享组和 7 条无效软删除引用，When 在新冻结边界重跑，Then 新报告逐项给出当前数量；与旧快照不同的数量必须由冻结边界或数据变化解释，不能直接沿用旧数字。
- **AC-20 零猜测完成定义**：Given 治理结束仍有软删除、缺失、不可读、冲突或无法完成受控全局引用核验的记录，When 生成最终报告，Then 这些记录保持 `BLOCKED` 且任务不得宣称“全部历史文件已治理”或“已具备 Revision / Iteration 迁移条件”。

## Open Questions

- 按冻结任务范围全局聚合共享组、范围外其它租户引用即整组阻塞，以及“全部有效记录生成独立副本”的规则是否由设计评审确认；若不采纳，必须由业务明确哪条记录保留原源文件，不能按排序或按租户自动决定。
- 现有 schema 是否扩展清单版本、blocker 原因、定位快照和请求摘要字段，或新增专门治理批次/明细表；由系统设计阶段确定。
- 已有 7 条无效 ownership 的更正方式和原始证据保留方式尚待数据模型评审。
- 软删除文件是否有受控备份可恢复，以及谁有权确认替代证据，尚无输入；由于 `infra_file` 全局，不能通过租户字段补救。
- 正式写执行的维护窗口、恢复点、执行人、确认人和对象存储预算尚未确定。
- 是否需要新建可视化治理页面，需由产品另行确认；当前 PRD 不把页面作为实施前置。

## Product Blockers

- 未重新生成最新只读冻结清单并确认清单 hash 前，不得运行现有或新增的写批次接口。
- 未解决现有候选查询有效记录口径和无冻结边界问题前，不能用当前 `run` 接口直接进行全量治理。
- 未定义并验证 `BLOCKED` 原因、清单确认和执行幂等合同前，现有 `PENDING/COPY_VERIFIED/COMPLETED/FAILED` 状态不足以支持正式治理。
- 未确认按冻结任务范围全局聚合共享组、范围外其它租户引用即整组阻塞、存储增量和对象存储可用性前，共享组保持 blocker。
- 未具备受控数据库读取权限完成全局 source_file_id 引用核验前，相关记录保持 `SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE`，不得按当前业务租户分别认领。
- 源文件缺失、软删除、正文不可读或 ownership 冲突记录在取得可验证证据前保持 blocker，不能由程序自动修复。
- 即使源文件治理全部可处理项完成，身份、平台 ACTIVE、空 Master、重复版本、正式指针和检出等 Windchill 迁移 blocker 仍需后续独立任务治理。
