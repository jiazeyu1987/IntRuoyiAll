# 执行日志：四份材料上传和放行门禁

## 用户意图

专项只做代码审计、需求澄清和开发文档设计。四份材料口径已确认正确；需要设计四节点任务、附件版本/hash、上传人、成功条件、缺件、替换、重复、版本变化和并发规则，并要求所有放行入口复用同一放行材料硬门禁。复核明确：活跃订单点击“完成”触发流程 4 拥有的单一回填事务，流程 5 提供损耗判定，流程 6 建批，流程 7 映射追溯，流程 9 负责多入口前置，流程 10 最终放行，流程 11 测试/迁移总门禁；本任务不创建批次、不负责回填、不拥有最终放行状态。

## 规则与证据读取

- 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/experience-index.md`、`docs/product/production-role-system-operations.md`、`docs/backend-development.md` 的“活跃订单申请放行资料必须只使用正式来源”章节、`docs/frontend-development.md` 和 `docs/e2e-rules.md`。
- 已读取系统设计文档结构和 BDD/TDD 验收计划结构；本任务输出聚合在五份任务文档中，不创建项目级系统设计文件。

## BDD

BDD: 完成节点原子回填 -> Given 活跃订单双进度达到 100% 且用户点击“完成” / When 流程 4 在同一业务事务内回填批记录、过程检验单和损耗分支 / Then 三类回填全部成功才返回完成成功；任一失败则整体完成/回填失败且不得进入建批。

BDD: 无实际损耗不生成损耗单 -> Given 完成节点判定无实际损耗 / When 完成事务提交 / Then 不创建或写入损耗单，只保留 NO_LOSS 适用性事实供流程 7 映射。

BDD: 实际损耗必须有损耗单 -> Given 完成节点存在实际损耗 / When 完成事务提交 / Then 必须创建并回填损耗单；损耗单失败则整体完成失败且不得建批。

BDD: 材料替换必须重检 -> Given 四份材料已生成当前 manifest / When 任一节点完成新版本替换或来源/hash 变化 / Then 批次材料状态变为 `MATERIALS_RECHECK_REQUIRED`，旧 manifest 不得提交放行。

BDD: 材料为空不阻塞合法建批 -> Given 活跃订单完成节点的三类回填已在同一事务成功 / When 流程 6/9 创建或复用批次 / Then 建批成功进入 MATERIALS_PENDING，材料 gate 不被调用。

BDD: 四份材料齐套才可进入最终放行 -> Given 已存在批次且四节点当前版本均已完成 / When 任一合法放行入口预检 / Then 统一 gate 冻结 manifest 并只允许进入流程 10。

BDD: 缺件、旧版本和 hash 不一致必须阻塞 -> Given 四节点任一缺件、未完成、版本变化或 hash 不一致 / When 预检或提交 / Then 返回明确 blocker，不默认成功。

BDD: 多入口复用同一门禁 -> Given 批次详情、管理者代表前置和其它合法放行入口均可触发最终放行 / When 任一材料不齐 / Then 所有入口均阻塞并返回同一门禁结论，建批入口不在此范围。

BDD: pre-release 来源映射先于材料门禁 -> Given 批次已由流程 6/9 合法创建或复用但流程 7 尚未冻结生产工单、领料单、批记录、过程检验和适用损耗/`NO_LOSS` Origin/TraceLink / When 任一放行入口执行预检 / Then 返回跨线程稳定码 `TRACE_MAPPING_BLOCKED`，不读取四材料齐套结果，也不进入流程 10。

BDD: post-release 追溯在最终放行后执行 -> Given 流程 7 pre-release 已冻结来源映射且流程 8 返回 `MATERIALS_READY` / When 流程 10 唯一最终放行成功 / Then 流程 7 post-release 消费 release 与四材料 manifest 完成追溯；若追溯失败不得伪造放行成功。

BDD: 门禁不拥有最终放行 -> Given 四材料 gate 返回 MATERIALS_READY / When 流程 10 尚未签名批准 / Then 不写最终放行状态，必须继续走流程 10。

## 只读代码审计

- `MesProEdhrBatchExecutionServiceImpl`：四个节点常量和固定节点顺序存在；节点完成要求附件非空；预上传包含 token、文件元数据和 SHA-256；附件记录包含版本/hash 链和操作者审计字段。
- `MesProductionReleaseReportStageInitializerImpl`：报告阶段要求四个冻结节点、四个唯一节点类型和冻结负责人候选。
- `MesProductionReleaseReportNodePortImpl`：报告上传准备和节点完成均通过批次执行服务转发。
- `MesPqcReleaseDossierPortImpl`：正式来源和 `formBindings` 不可替代正式批记录绑定的 fail-fast 逻辑存在；活跃订单“完成”及同一事务三类回填属于流程修复 4（流程修复 5 提供损耗规则），批次创建/复用属于流程修复 6/9，不由本任务拥有。
- `EdhrReleaseDossierRequirementSetting.vue`：四个资料开关默认 false，且允许分别关闭；这是旧配置/界面遗留事实。服务端必须固定四节点，旧配置只能只读兼容或迁移，若仍影响运行时门禁则代码符合性为 FAIL。

## 跨线程职责复核

- 流程修复 4：拥有活跃订单“完成”唯一节点及原子回填事务，负责双 100%、批记录、过程检验单和实际损耗单/NO_LOSS 事实的整体成功或失败。
- 流程修复 5：只提供条件损耗判定与事实规则；不拆成独立回填节点，实际损耗分支必须在流程 4 完成事务内创建/回填损耗单，无损耗分支只允许 NO_LOSS 事实。
- 流程修复 6：回填成功后的批次执行创建/复用。
- 流程修复 7：分两阶段负责映射与追溯；pre-release 在材料上传前冻结 Origin/TraceLink，post-release 在流程 10 成功后消费 release 和 manifest。
- 流程修复 9：多入口前置、状态所有者、幂等和追溯合同；建批不受四材料 gate 阻塞。
- 流程修复 10：最终放行角色、签名、批准和最终状态。
- 流程修复 11：测试、回归、迁移、回滚总门禁。

## RED/GREEN/REGRESSION 证据

RED: NOT RUN（本任务只做文档审计与设计；后续实现线程需单独建立并运行定向合同测试，至少覆盖完成事务原子回填、无损耗/实际损耗分支和流程 7 pre-release 映射缺失先于四材料 gate 阻塞）。

GREEN: 文档结构检查 -> PASS，五份正式文档已生成并完成一致性核验；设计顺序为流程 7 pre-release 映射冻结 -> 四材料 gate -> 流程 10 最终放行 -> 流程 7 post-release 追溯；这不是生产代码 GREEN 证据。

REGRESSION: NOT RUN（后续实现线程必须按 `test-plan.md` 覆盖完成事务失败不建批、无损耗/实际损耗分支、流程 7 pre/post 两阶段、四节点、所有放行入口、幂等/并发、活跃订单来源和跨线程链路）。

## Blocker

1. 当前缺少证据证明服务端已经固定要求四节点并隔离旧配置运行时影响；旧配置只能作为只读兼容或迁移 blocker，不能参与放行决策。
2. 只读代码已证明节点级上传与审计基础，但尚未证明所有放行入口在运行态统一调用同一四材料 gate；需要后续实现和静态/单元/真实页面证据。
3. 当前未启动服务、未运行写入型 E2E，无法验证真实租户、负责人候选、对象存储和清理闭环。
4. `rg` 读取到 `target_corrupt_m4_20260802_1327` 下存在“文件或目录损坏”只读噪声；该构建产物未纳入代码结论，后续测试前需按本地运行规则处理或明确排除。

## 迁移/回滚记录

本任务不执行迁移或回滚。设计要求先盘点四节点正式来源，再以可证明的附件元数据迁移版本 1；无法证明的历史数据保持 blocker，禁止用旧附件或名称匹配补齐。

## 当前状态

completed

本任务范围内的审计、设计、结构核验和 closeout 已完成。完成节点单一事务回填、损耗分支和失败不建批约束，以及流程 7 pre-release 映射、流程 8 材料 gate、流程 10 最终放行、流程 7 post-release 追溯的顺序已冻结。生产实现、配置迁移、生产测试、回归和写入型 E2E 均为 NOT RUN，并作为后续实现 blocker 记录。

## 修订与收尾验证

- 已核对五份文档均包含流程 4/5/6/7/9/10/11 职责、完成节点单一事务回填和损耗分支、流程 7 pre-release 映射先于材料 gate、流程 7 post-release 位于流程 10 成功后、先建批后上传的时序、四材料固定口径和门禁不拥有最终放行状态。
- 文档结构核验 -> PASS；完成事务/损耗分支设计已同步五份文档；生产 GREEN -> NOT RUN。
- `task-closeout-cleanup` preview/apply -> PASS；五份正式文档保留，删除项为零。
- 最终旧词扫描 -> PASS；五份文档统一使用 `MATERIALS_RECHECK_REQUIRED` 和 `TRACE_MAPPING_BLOCKED`，历史未定义状态、错误码和笔误均无匹配。
