# Goal

按已通过独立评审的一线 PQC DCC-QA 数据链路设计包完成 14 个开发任务，并由主管 Agent 按依赖波次调度、评审、验证和 fast-forward 合并到 int_main。最终一线 PQC 应从全部有效活跃订单开始，以 activeOrderId 选择订单，读取订单锁定的 DCC/QA 版本快照，展示 QA 规程自有工序和检验项目，并叠加该订单的 PQC 任务状态与执行入口。

# Scope

- 覆盖 C00、DF01 至 DF11、INT12、VAL13。
- 以 doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design/ 为权威设计基线。
- 复用当前系统已有 MES、DCC、QA、PQC、active order、frontline context、Vue 页面和测试入口。
- 主管最多同时运行 3 个工作子 Agent，严格按 Wave 0 至 Wave 9 推进。
- 每个任务必须独立分支、独立 worktree、任务自有提交、主管评审、独立验证和 fast-forward 合并。
- 前端真实流程必须用 Playwright 和真实测试数据验证；API 只可作为最终只读辅助核验。

# Non-Goals

- 不 push、不部署、不操作远程服务器、不修改共享业务数据。
- 不创建 DCC 后端到 MES 的反向依赖。
- 不创建 DCC-QA 绑定表、QA item-type 表或 active-order PQC context 状态机。
- 不把 QA 规程与产品、路线版本、MES 工序名称、MES 工序编码、路线工序数量做存在性校验。
- 不用 current QA、今天的路线关系、产品编号或名称推算历史订单的锁定 QA 版本。
- 不用 fallback、兼容分支、吞异常、默认成功、空列表成功或 mock 数据掩盖缺失配置。
- VAL13 只做独立验收，不直接修改生产代码。

# User or System Scenarios

## Scenario 1: 一线选择活跃订单后看到 QA 工序列表

Given 系统存在有效活跃订单，且该订单已锁定 dccProjectCodeId、qaRegulationId、qaRegulationVersionId。
When 一线用户打开 PQC 订单选择页并选择 activeOrderId。
Then 页面按锁定 QA 版本展示 QA 自有工序和检验项目。
And PQC 任务状态只作为叠加信息展示，不能决定工序或项目是否显示。

## Scenario 2: QA 规程只通过 DCC 项目代码关联

Given 生产路线通过正式关系定位到唯一 DCC 项目代码。
When 系统读取该 DCC 项目代码对应的 QA 规程。
Then 系统直接使用 QA 表中的 dccProjectCodeId 关系。
And 不按产品编号、路线版本或 MES 工序推算 QA。

## Scenario 3: 历史订单读取锁定 QA 版本

Given 订单创建时已锁定 QA 发布版本。
And 后续 DCC 项目代码被禁用或 QA current 版本发生变化。
When 一线用户再次查看该订单 PQC 工序列表。
Then 系统读取订单锁定版本，PUBLISHED 或 RETIRED 时仍可查看和执行。

## Scenario 4: 上午巡检和下午巡检独立展示与提交

Given QA 版本包含 FIRST、PATROL_AM、PATROL_PM、FINAL 四个正式 rule key。
When 系统为订单生成和展示 PQC task options。
Then PATROL_AM 和 PATROL_PM 即使共用 inspectionType=PATROL 项目，也必须分别生成、排序、展示和提交。

## Scenario 5: 主管按波次合并任务

Given 前置波次任务尚未通过主管评审、独立验证并合入 int_main。
When 后续波次任务看似可以开发。
Then 主管不得启动后续波次，也不得用后继任务填满并发。

# Functional Requirements

- FR-01 主管必须读取 AGENTS.md、触发规则、设计包、评审证据、Git 状态、worktree 状态和端口脚本状态；设计评审未通过时停止开发。
- FR-02 主管最多同时运行 3 个工作子 Agent；平台容量不足时必须先报告实际容量。
- FR-03 任务必须按 Wave 0 C00；Wave 1 DF01/DF02/DF03/DF05；Wave 2 DF04；Wave 3 DF06；Wave 4 DF07；Wave 5 DF08；Wave 6 DF09；Wave 7 DF10/DF11；Wave 8 INT12；Wave 9 VAL13 执行。
- FR-04 每个实现任务必须从包含全部前置任务的最新 int_main 创建独立分支和 D:\IntRuoyiWorktree\ 下的独立 worktree。
- FR-05 每个工作子 Agent 必须收到通用业务背景、对应任务文档、共享接口合同、前置任务和已冻结接口、文件所有权和禁止修改范围、验收命令。
- FR-06 工作子 Agent 只能修改任务文档明确归属的文件和自己的任务记录；不得自行合并 int_main 或删除 worktree。
- FR-07 每个任务必须先记录 Given/When/Then 场景，再运行并记录真实 RED，随后实现最小正式方案并记录 GREEN 和回归验证。
- FR-08 C00 必须交付并验证 schema preflight、后继 migration、backfill、postflight、rollback 和机器可读报告；缺失批准清单或历史歧义时阻塞。
- FR-09 DF01 必须返回全部 deleted=0 且 active_status=ACTIVE 的 active-order，不按 PQC task 状态过滤，不按 workOrderId + routeId 去重。
- FR-10 DF02 必须按 activeOrderId 读取订单已确定路线追溯摘要，只用于定位 DCC 和展示审计，不作为 QA 工序校验。
- FR-11 DF03 必须实现路线到 DCC 项目代码的正式关系管理，保持同租户、唯一当前绑定和历史 version 单调递增。
- FR-12 DF04 必须校验订单路线对应唯一、启用、同租户 DCC 项目代码；缺失、重复、禁用或跨租户均失败。
- FR-13 DF05 必须复用 QA 表 dccProjectCodeId 作为唯一正式关系，修正 QA/DCC 管理前端和批量状态展示，不新增 DCC 侧绑定表。
- FR-14 DF06 必须在新建 active order 的同一事务中锁定 dccProjectCodeId、qaRegulationId、qaRegulationVersionId，并按启用 rule 生成任务；removed 重新激活保留原快照和任务历史。
- FR-15 DF07 必须按订单锁定版本读取 QA 自有工序，支持 PUBLISHED/RETIRED 历史版本，不调用管理端 current 查询或 DCC 当前启用校验。
- FR-16 DF08 必须按 qaProcessId + itemCode 聚合业务检验项目，保留上下限、单位、精度、设备选项、适用检验类型和来源字段。
- FR-17 DF09 必须按 activeOrderId + regulationVersionId + qaProcessId + inspectionRuleKey 叠加任务状态、task options 和订单级生产提交事件候选；任务不得过滤外层 QA 工序和项目。
- FR-18 DF10 必须返回专用一线 PQC response assembler，不改变生产模式路线工序响应模型。
- FR-19 DF11 必须对齐后端 DTO、稳定排序和状态显示，确保页面展示 QA 工序、项目、规则任务和状态。
- FR-20 INT12 必须打通 controller、页面、人员切换、设备选择、提交、签名、resultType、幂等和事件归属链路。
- FR-21 VAL13 必须由未参与实现的独立 Agent 执行静态合同、JUnit、真实页面和只读 API 验收；发现问题退回对应任务，不直接修生产代码。
- FR-22 每个任务合并前主管必须亲自检查完整 diff、提交范围、任务文档、接口统一性、当前系统复用、过度设计风险和 RED/GREEN 真实性。
- FR-23 评审通过后，主管必须先把最新 int_main 合入任务分支并重跑验证，再仅用 fast-forward 合并回 int_main；冲突、无法 fast-forward 或不明并发修改即停止。

# Non-Functional Requirements

- NFR-01 所有缺失前置条件必须 fail fast，不得返回 mock、空成功、默认成功或降级结果。
- NFR-02 每个任务必须保留分支、worktree、提交号、验证命令、RED/GREEN 证据、评审结论和合并状态。
- NFR-03 优先复用当前系统 service、mapper、VO、页面和测试入口；不得新增重复模型或过度抽象。
- NFR-04 所有 ID 查询必须在当前租户上下文执行；跨租户 ID 与不存在 ID 使用同一非法引用语义。
- NFR-05 不得修改共享业务数据；写类型验证必须使用确认的测试租户、账号和任务自有数据。
- NFR-06 不得 git add -A、不得回滚无关变更、不得强制合并、不得半合并前后端。
- NFR-07 所有任务服务启动必须使用独立端口槽位，且不占用 int_main 的 8081/48081。
- NFR-08 测试通过必须来自实际命令输出或真实页面行为，不接受子 Agent 自报或文档声称替代测试。

# Dependencies and Constraints

- 设计放行依赖：.review-fix-loop/runs/20260812T001009Z-e59e06/ 已通过；旧失败运行仅保留为历史证据。
- 代码基线依赖：每个任务从最新 int_main 创建，且前置任务已合入 int_main。
- 目录依赖：所有 worktree 必须位于 D:\IntRuoyiWorktree\。
- 端口依赖：服务启动前必须运行 scripts\runtime\reserve-worktree-slot.ps1。
- 规则依赖：后端、前端、数据库、E2E、登录、运行态、Git、PowerShell 操作必须先读取对应 docs\*.md 触发规则。
- 业务依赖：QA 与生产路线唯一交点是 DCC 项目代码；QA 工序不与 MES 工序建立映射。
- 数据依赖：压力泵验收基线为三个活跃订单均可见，锁定 DCC 项目代码 ID，返回 8 个 QA 工序和 18 个聚合业务检验项目。

# Acceptance Criteria

- AC-01 启动门禁通过：主管已确认 AGENTS.md、触发规则、设计通过证据、Git int_main 状态、现有并发修改、worktree 根目录和端口脚本；任何前置缺失均记录阻塞并停止开发。
- AC-02 并发规则满足：任一时刻工作子 Agent 数量不超过 3；平台容量不足时有明确报告；波次任务少于 3 个时没有启动后继波次填充并发。
- AC-03 波次依赖严格：C00、DF01-DF11、INT12、VAL13 均按规定 Wave 顺序推进，后继任务只在全部前置合入 int_main 后启动。
- AC-04 任务隔离完整：每个实现任务都有独立分支、独立 D:\IntRuoyiWorktree\ worktree、任务自有文档和提交，且工作子 Agent 未越权。
- AC-05 BDD/TDD 证据完整：每个实现任务均记录 BDD、真实 RED、GREEN、回归验证和对应命令结果；缺失证据的任务不得合并。
- AC-06 业务链路正确：一线从全部有效活跃订单选择 activeOrderId 后，按订单锁定的 DCC/QA 版本读取 QA 自有工序和项目，并叠加 PQC 任务状态；PQC task 不过滤工序或项目。
- AC-07 QA-DCC 边界正确：QA 规程只通过 DCC 项目代码关联；没有产品、路线版本、MES 工序存在性校验，没有 DCC 侧 QA 绑定表或后端反向依赖。
- AC-08 规则任务正确：FIRST、PATROL_AM、PATROL_PM、FINAL 四个 rule key 是正式身份；上午巡检和下午巡检分别生成、展示、排序和提交，不能按 inspectionType=PATROL 合并。
- AC-09 历史锁定正确：新订单只锁定 DCC 当前启用且 current PUBLISHED QA 版本；旧订单读取其锁定 PUBLISHED/RETIRED 版本，DCC 后续禁用或 current 变更不影响旧订单查看执行。
- AC-10 提交闭环正确：PQC 提交基于任务行锁、PENDING 到 SUBMITTED 的 CAS、CanonicalPqcSubmissionV1 hash、唯一正式 event 和实际填写人签名；相同内容幂等返回原回执，冲突内容零写入拒绝。
- AC-11 主管评审通过：每个任务合并前主管已复核完整 diff、提交范围、任务文档、接口一致性、系统复用、过度设计风险和 no-fallback 约束。
- AC-12 独立验证通过：每个任务有独立测试或主管重跑验证证据；DF01-DF11 全部合并后 INT12 全链路通过；INT12 合并后 VAL13 独立验收通过。
- AC-13 合并方式正确：所有通过任务均先吸收最新 int_main 并重跑验证，再 fast-forward 合并；没有冲突强合、半合并、force、push 或部署。
- AC-14 最终收尾完整：最终报告列出 14 个任务的分支、worktree、提交号、验证结果、合并状态、风险或阻塞；任务 worktree 已按规则清理；未执行 push、部署、远程服务器操作或共享业务数据修改。
