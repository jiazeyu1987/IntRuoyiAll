# 岗位需求分解矩阵全链路测试计划

## Test Strategy

本测试计划用于后续实现任务，不代表当前仓库已经具备计划中的测试类、前端脚本、账号、数据或运行态。每个里程碑必须遵循：

1. 先新增或扩展一个**可编译、可执行**且针对目标行为的测试。
2. 执行 RED，并确认失败原因是目标行为尚未满足。
3. 实现最小正式方案。
4. 执行 GREEN。
5. 执行相邻模块回归、真实用户路径 E2E 和任务数据清理。

缺测试类、缺 package script、No tests、缺数据库、缺账号、缺浏览器或服务未启动只能记录为 blocker，不算 RED。

## Coverage Contract

- 需求覆盖必须达到 `62/62`：`AC-M01` 至 `AC-M23`、`AC-D01` 至 `AC-D39` 各自拥有唯一 `TC-*`。
- 每个 `TC-*` 至少包含一个正向断言和一个失败/边界断言；只验证接口 200、数据库有记录或按钮可见不算完成。
- 每个用户可见写行为必须同时具备后端业务测试和真实 Playwright E2E。
- Schema、迁移、唯一键、并发、权限、租户、历史快照和性能不能仅由 E2E 证明，必须有对应低层测试。
- BDD 场景可以分组表达业务流程，但不得用 `AC-D01 至 AC-D08` 之类范围表达代替逐项测试。
- 代码覆盖率只能作为辅助指标，不能替代 62 项行为验收和关键失败分支。
- 详细 BDD、TDD、E2E 和测试数据方案位于：
  - `docs/acceptance/bdd-scenarios.md`
  - `docs/acceptance/tdd-plan.md`
  - `docs/acceptance/e2e-plan.md`
  - `docs/acceptance/test-data.md`

测试层级缩写：

- `DB`：schema、migration、唯一键、索引和持久化合同。
- `UT`：领域/服务单元测试。
- `API`：Controller、权限、租户和序列化合同。
- `UI`：前端静态合同、组件行为和 TypeScript。
- `E2E`：真实登录、菜单和 Playwright 用户路径。
- `SEC`：权限、职责隔离和租户边界。
- `CONC`：并发、乐观锁、唯一键和幂等。
- `SNAP`：历史快照、版本和审计。
- `PERF`：分页、索引、查询计数和 N+1。

## BDD Scenarios

| 场景 | Given / When / Then | 覆盖验收 |
|---|---|---|
| BDD-01 ERP 候选进入统一活跃订单 | Given ERP 候选订单和唯一正式路线；When 组长加入活跃订单；Then 生产、PQC、批记录和放行读取同一 activeOrderId | `AC-M01`、`AC-M03`、`AC-M04` |
| BDD-02 多调拨与开工检查 | Given 分批发货、补料、退料和多批次；When 关联并检查；Then 逐物料计算覆盖、展示缺项且不自动创建异常 | `AC-M02`、`AC-M05`、`AC-M06`、`AC-M07`、`AC-M08` |
| BDD-03 班组配置只绑定正式设备 | Given 正式设备台账和员工/原因配置；When 维护班组配置；Then 新业务只使用启用配置且历史快照不变 | `AC-D01`、`AC-D02`、`AC-D03`、`AC-D04`、`AC-D05`、`AC-D06`、`AC-D07`、`AC-D08` |
| BDD-04 工序事实优先报工 | Given 尚未确定订单归属；When 员工提交工序、人员、设备、参数、数量、损耗和签名；Then 原始事实成功保存且不可覆盖 | `AC-M10`、`AC-M11` |
| BDD-05 退回修订与系数分配 | Given 原始报工和多个活跃订单；When 退回补正并分配；Then 修订链完整、分配总量一致、目标量使用生产系数 | `AC-M16`、`AC-M17`、`AC-M18`、`AC-D11` |
| BDD-06 确定性批记录回填 | Given 多员工、多设备、多次报工共同完成工序；When 达到目标量；Then 全部事实按正式映射聚合并幂等回填 | `AC-M19` |
| BDD-07 QA 规程版本生命周期 | Given 产品、路线版本和工序；When QA 配置、校验并发布；Then 缺项阻塞、发布不可变、历史保留版本 | `AC-M09`、`AC-D15`、`AC-D16`、`AC-D17`、`AC-D18`、`AC-D19`、`AC-D20`、`AC-D21`、`AC-D22`、`AC-D23` |
| BDD-08 首检与巡检任务计算 | Given 活跃订单和发布规程；When 按日期/班次生成任务；Then 首检固定、巡检向上取整、末检按适用性处理 | `AC-M12`、`AC-M13`、`AC-M14`、`AC-M15` |
| BDD-09 PQC 逐件检验与签名 | Given PQC 任务和实际人员；When 逐件填写并签名；Then 保存任务上下文、逐件明细、原因和签名且不依赖生产事件 | `AC-D24`、`AC-D25`、`AC-D26`、`AC-D27`、`AC-D28`、`AC-D29` |
| BDD-10 PQC 复核职责隔离与补正 | Given 待确认 PQC 提交；When 组长确认、退回或自我确认；Then 合法确认、退回修订和自我确认阻塞正确 | `AC-M20`、`AC-D30`、`AC-D32`、`AC-D33`、`AC-D34`、`AC-D35` |
| BDD-11 过程检验和质量异常 | Given 已确认合格/不合格提交；When 汇集过程检验；Then 仅最终修订汇集，不合格形成独立质量异常 | `AC-M21`、`AC-D36`、`AC-D37` |
| BDD-12 批记录完整性和放行 | Given 调拨、生产、PQC、批记录、异常和签名来源；When 预检并审核；Then 缺项阻塞，全部通过后才可签名放行 | `AC-M22`、`AC-M23` |
| BDD-13 日结、范围、只读和快照 | Given 多角色、未闭环事项和配置变更；When 查看范围/日结/历史；Then 后端隔离权限、逐项提示并保持历史快照 | `AC-D09`、`AC-D10`、`AC-D12`、`AC-D13`、`AC-D38`、`AC-D39` |
| BDD-14 缺少正式前置条件 | Given 任一正式订单、路线、系数、规程、人员、签名、调拨或批记录绑定缺失；When 提交；Then fail fast 且无默认补齐 | `AC-D31` 及全部跨切面失败路径 |
| BDD-15 并发与幂等 | Given 同一业务对象的两个并发请求；When 同时提交；Then 最多一个转换成功且无超额、重复回填/汇集/放行 | M1-M6 并发门禁 |
| BDD-16 三类工艺路线配置互不替代 | Given 工序开始、正式批记录绑定和 `formBindings` 同时存在；When 分别展示和执行；Then 三条链路只读取各自来源 | `AC-D14` 及正式批记录跨切面门禁 |

## Acceptance Test Coverage Matrix

每行是一个不可省略的验收测试单元。实施时允许把多个 `TC-*` 放入同一测试类或 E2E 脚本，但测试报告和任务日志必须能按 `TC-*`/`AC-*` 单独定位结果。

<!-- ACCEPTANCE_TEST_MATRIX_START -->
| AC | BDD | 测试用例 | 必测层级 | 正向断言 | 失败/边界断言 |
|---|---|---|---|---|---|
| AC-M01 | BDD-01 | TC-M01 | API、E2E | ERP 已确认订单可按正式 ID/编号查询 | 未确认、缺正式 ID 或越权订单不进入候选 |
| AC-M02 | BDD-02 | TC-M02 | API、E2E | ERP 调拨申请同步后可追溯 | MES 无创建/编辑入口，缺正式来源时阻塞 |
| AC-M03 | BDD-01 | TC-M03 | DB、UT、API | 订单、调拨、发货、批次按正式 ID 幂等同步 | 重复、乱序或冲突来源不生成重复事实 |
| AC-M04 | BDD-01 | TC-M04 | DB、UT、API、E2E、CONC | 各角色读取同一 `activeOrderId` | 重复加入、冲突路线和并发加入最多一个成功 |
| AC-M05 | BDD-02 | TC-M05 | API、E2E | ERP 发货后可读取物料、数量、批次和状态 | 未发货、部分发货和无正式 ID 不得显示完整 |
| AC-M06 | BDD-02 | TC-M06 | UT、API、UI、E2E | 实物核对结果和批次形成追溯证据 | 缺失、不一致或越权核对进入明确缺项 |
| AC-M07 | BDD-02 | TC-M07 | DB、UT、API、E2E、CONC | 一个订单可关联多调拨、多物料和多批次 | 重复关联幂等，错误订单/租户/数量被拒绝 |
| AC-M08 | BDD-02 | TC-M08 | UT、API、UI、E2E | 开工检查逐项展示结果、来源和阻塞原因 | 缺项不标记就绪且不自动创建异常 |
| AC-M09 | BDD-07 | TC-M09 | DB、UT、API、UI、E2E | 完整规程可发布并生成不可变版本 | 缺首检/巡检/末检规则或冲突时发布失败 |
| AC-M10 | BDD-04 | TC-M10 | UT、API、UI、E2E | 未选订单/任务仍可按 SOP 进入工序事实报工 | 缺工序/SOP 或越权工序时阻塞 |
| AC-M11 | BDD-04 | TC-M11 | DB、UT、API、UI、E2E | 人员、设备、参数、数量、损耗、原因、签名完整保存 | 缺必填、设备不可用、签名不一致时拒绝且原始事实不覆盖 |
| AC-M12 | BDD-08 | TC-M12 | UT、API、UI、E2E | 首检按发布规程固定数量生成并逐件提交 | 无规程、重复任务或数量不符时阻塞 |
| AC-M13 | BDD-08 | TC-M13 | UT、API、UI、E2E | 上午巡检保存日期、班次、轮次并向上取整 | `301×5%` 非 `16`、跨天重复或轮次冲突时失败 |
| AC-M14 | BDD-08 | TC-M14 | UT、API、UI、E2E | 下午巡检与上午任务身份分离 | 错班次、错日期或复用上午任务时失败 |
| AC-M15 | BDD-08 | TC-M15 | UT、API、UI、E2E | 适用时生成末检，不适用时保存明确依据 | 未显式配置、错误跳过或错误阻塞放行时失败 |
| AC-M16 | BDD-05 | TC-M16 | UT、API、UI、E2E、CONC | 通过进入分配，退回保留原因和原始提交 | 重复确认、覆盖原始记录或退回后继续分配被拒绝 |
| AC-M17 | BDD-05 | TC-M17 | UT、API、UI、E2E、CONC | FIFO 建议和手工调整只分配活跃订单且总量守恒 | 非活跃、工序不匹配、超额、重复事件和版本冲突被拒绝 |
| AC-M18 | BDD-05 | TC-M18 | UT、API、E2E、CONC | 进度按工序目标量更新，ERP 产品数量不变 | 系数缺失/非正数、超目标或并发更新被阻塞 |
| AC-M19 | BDD-06 | TC-M19 | DB、UT、API、E2E、CONC | 全部已确认报工按策略汇总到正式批记录 | 代表事件丢数、无聚合策略、缺正式绑定或重复回填被阻塞 |
| AC-M20 | BDD-10 | TC-M20 | UT、API、UI、E2E、SEC、CONC | PQC 组长可确认或退回并保留记录 | 未确认/退回不算完成，自我确认和并发确认被拒绝 |
| AC-M21 | BDD-11 | TC-M21 | DB、UT、API、E2E、CONC | 只汇集最终已确认修订并可追溯任务/轮次/版本 | 未确认、旧修订、重复汇集或跨租户数据被排除 |
| AC-M22 | BDD-12 | TC-M22 | UT、API、UI、E2E | 所有正式来源完整时预检通过 | 缺批记录、PQC、调拨、签名或有阻塞异常时不可放行 |
| AC-M23 | BDD-12 | TC-M23 | UT、API、UI、E2E、SEC、CONC | 放行负责人可签名放行或退回并审计 | 越权、重复放行、签名缺失或预检未过时拒绝 |
| AC-D01 | BDD-03 | TC-D01 | DB、UT、API、UI、E2E | 新员工加入班组并用于后续绑定 | 重复、跨租户或无权限添加被拒绝 |
| AC-D02 | BDD-03 | TC-D02 | UT、API、UI、E2E、SNAP | 禁用员工不再用于新报工且历史保留 | 已禁用仍可新选或历史人员被清空时失败 |
| AC-D03 | BDD-03 | TC-D03 | DB、UT、API、UI、E2E、SNAP | 新报工只显示当前工序启用的不良原因 | 禁用/跨工序原因可选或历史原因被改写时失败 |
| AC-D04 | BDD-03 | TC-D04 | DB、UT、API、UI、E2E、SNAP | 损耗原因按工序配置并进入报工 | 固定前端列表、禁用原因或跨工序原因被拒绝 |
| AC-D05 | BDD-03 | TC-D05 | DB、UT、API、UI、E2E | 只能从正式设备台账绑定工序设备 | 独立创建设备、重复绑定或跨租户设备被拒绝 |
| AC-D06 | BDD-03 | TC-D06 | UT、API、UI、E2E、SNAP | 报修/禁用设备不用于新报工，恢复后可选 | 不可用设备仍可提交或历史设备消失时失败 |
| AC-D07 | BDD-03 | TC-D07 | DB、UT、API、UI、E2E | 参数上下限、单位和默认值按设备/工序保存 | 下限大于上限、默认值越界或单位冲突时拒绝 |
| AC-D08 | BDD-03 | TC-D08 | UT、API、UI、E2E、SNAP | 超限值被标记并提醒复核，原值保留 | 系统静默改值、吞提醒或错误判定正常时失败 |
| AC-D09 | BDD-13 | TC-D09 | DB、UT、API、UI、E2E、SEC | 员工、工序、工作站、产线、设备、订单范围均生效 | 仅前端隐藏、后端越权返回或范围串租户时失败 |
| AC-D10 | BDD-13 | TC-D10 | DB、UT、API、E2E、SNAP | 维护操作记录操作人、时间、前后值和范围 | 缺审计、审计可改写或前后值不完整时失败 |
| AC-D11 | BDD-05 | TC-D11 | DB、UT、API、UI、E2E、SNAP | 原始、退回原因和补正提交可串联追溯 | 覆盖原始、断裂修订链或复用旧签名时失败 |
| AC-D12 | BDD-13 | TC-D12 | UT、API、UI、E2E、PERF | 日结逐项展示未分配、未确认、缺项和次日延续 | 漏项、重复项、分页错误或跨范围数据时失败 |
| AC-D13 | BDD-13 | TC-D13 | UT、API、UI、E2E、SEC | 生产组长可只读查看 PQC 状态 | 页面或 API 允许填写/确认 PQC 时失败 |
| AC-D14 | BDD-16 | TC-D14 | UT、API、UI、E2E | 按正式逐工序批记录绑定显示进度 | 缺绑定时读取 `formBindings`、`MAIN` 或工序开始即失败 |
| AC-D15 | BDD-07 | TC-D15 | DB、UT、API、UI、E2E | 产品/路线版本/工序可发布独立规程 | 错路线、错版本、重复有效规程或跨租户发布被拒绝 |
| AC-D16 | BDD-07 | TC-D16 | UT、API、UI、E2E | QA 页面显示正式 SOP、系数和批记录绑定 | 显示旧版本、缺来源或用表单槽位替代时失败 |
| AC-D17 | BDD-07 | TC-D17 | DB、UT、API、UI、E2E | PQC 完全按规程项目、方法、工具和标准渲染 | 固定项目、默认合格或缺关键项仍可提交时失败 |
| AC-D18 | BDD-07 | TC-D18 | UT、API、UI、E2E | 首检固定数量和项目可配置并带入任务 | 数量非正、缺项目或发布后任务未带出时失败 |
| AC-D19 | BDD-07 | TC-D19 | UT、API、UI、E2E | 上午/下午规则独立，比例向上取整 | 两轮混用、比例越界或 `301×5%` 不为 `16` 时失败 |
| AC-D20 | BDD-07 | TC-D20 | UT、API、UI、E2E | 末检需要/不适用显式保存并参与放行 | 未配置被当不适用或适用任务未生成时失败 |
| AC-D21 | BDD-07 | TC-D21 | UT、API、UI、E2E | 草稿缺字段或规则冲突时返回字段级错误 | 通用成功、吞异常或无效草稿进入发布时失败 |
| AC-D22 | BDD-07 | TC-D22 | UT、API、UI、E2E | 完整性检查逐产品/路线版本/工序列缺失 | 漏检规则、错误来源或缺项仍显示完整时失败 |
| AC-D23 | BDD-07 | TC-D23 | DB、UT、API、UI、E2E、SNAP | 发布版本不可改写，新任务用新版本 | 原地修改发布版、历史任务漂移或双有效版本时失败 |
| AC-D24 | BDD-09 | TC-D24 | UT、API、UI、E2E | 只能选择统一活跃订单及正式路线工序 | 终止订单、旧活跃来源或缺路线时阻塞 |
| AC-D25 | BDD-09 | TC-D25 | UT、API、UI、E2E、SEC | 共享账号下保存实际 PQC 人员 | 默认登录人冒充、无人员范围或跨租户人员被拒绝 |
| AC-D26 | BDD-09 | TC-D26 | UT、API、UI、E2E、SEC、SNAP | 签名人与实际检验人一致并保存快照 | 签名缺失、身份不一致、过期或复用签名时失败 |
| AC-D27 | BDD-09 | TC-D27 | DB、UT、API、UI、E2E、PERF | 计划数量对应完整可还原逐件明细 | 少件、多件、重复序号、N+1 或仅整批结果时失败 |
| AC-D28 | BDD-09 | TC-D28 | UT、API、UI、E2E | 不合格/损耗数量、原因和说明参与判定 | 数量不守恒、缺原因、负数或默认合格时失败 |
| AC-D29 | BDD-09 | TC-D29 | DB、UT、API、UI、E2E、CONC | 提交生成可追溯 PQC 事件并进入待办 | 重复提交生成双事件、无任务身份或待办不可见时失败 |
| AC-D30 | BDD-10 | TC-D30 | DB、UT、API、UI、E2E、SNAP | 退回原因、原提交、补正和新签名形成修订链 | 覆盖原提交、旧修订参与完成或无新签名时失败 |
| AC-D31 | BDD-14 | TC-D31 | UT、API、UI、E2E | 缺订单、路线、规程、人员或签名逐项 fail fast | 默认订单/人员/数量/合格或占位成功即失败 |
| AC-D32 | BDD-10 | TC-D32 | UT、API、UI、E2E、PERF | 多条件筛选准确且分页总数正确 | 条件串扰、重复行、总数漂移或越权数据时失败 |
| AC-D33 | BDD-10 | TC-D33 | UT、API、UI、E2E | 详情展示逐件明细、原因、签名、版本和原 payload | 缺字段、显示错误修订或跨租户详情时失败 |
| AC-D34 | BDD-10 | TC-D34 | UT、API、UI、E2E、CONC | 确认/退回留痕，退回不参与完成 | 重复终态、未签名确认、退回仍汇集或并发双成功时失败 |
| AC-D35 | BDD-10 | TC-D35 | UT、API、UI、E2E、SEC | 后端阻塞确认人等于实际检验人 | 只在前端隐藏或共享账号绕过时失败 |
| AC-D36 | BDD-11 | TC-D36 | DB、UT、API、UI、E2E | 不合格形成独立质量异常并有责任/解除条件 | 进入生产异常流、无责任人或无法解除时失败 |
| AC-D37 | BDD-11 | TC-D37 | DB、UT、API、UI、E2E、CONC | 组长确认后过程检验汇集可见 | 未确认/退回/旧修订参与或重复汇集时失败 |
| AC-D38 | BDD-13 | TC-D38 | UT、API、UI、E2E、PERF | 日结提示未提交、未确认、退回、不合格和放行影响 | 漏项、重复项、范围越界或分页总数错误时失败 |
| AC-D39 | BDD-13 | TC-D39 | DB、UT、API、UI、E2E、SNAP | 修改配置后历史报工/PQC/批记录保持原快照 | 历史显示新值、缺版本或 legacy 猜填参与新判断时失败 |
<!-- ACCEPTANCE_TEST_MATRIX_END -->

## Strict TDD Workflow

每个 `TC-*` 必须独立执行以下循环：

1. `BDD_APPROVED`：确认对应 Given/When/Then、正式来源和测试数据。
2. `TEST_ADDED`：创建可执行测试，确认测试运行器发现且 tests run 大于 `0`。
3. `RED_VALID`：生产代码未修改，测试因表格中的目标断言失败。
4. `GREEN`：实现最小正式方案后重跑同一命令通过。
5. `REFACTOR`：清除重复、fallback、默认值和跨层业务规则。
6. `REGRESSION`：运行相邻模块、权限、租户、并发、快照或性能回归。
7. `E2E`：适用行为通过真实页面路径和只读 API 核验。
8. `ACCEPTED`：日志记录 `AC-*`、`TC-*`、命令、业务 ID、结果和清理证据。

禁止把多个失败原因塞入一个 RED；必须拆成可定位的测试。禁止使用不同命令或更弱断言把 RED 改写成 GREEN。

## RED

### RED Rules

- RED 命令执行前，测试类/脚本必须已创建且能被测试运行器发现。
- RED 预期原因必须写成业务断言，例如“PQC 仍读取旧活跃订单来源”，不能写“测试类不存在”。
- 一个 RED 只证明一个最小行为差距；多个失败原因必须拆分。
- 所有 RED 证据记录到后续实现任务的 `execution-log.md`：
  `RED: <command> -> FAIL, <expected reason>`。

### M1 RED

先增加可编译的权威来源、schema 和迁移合同测试，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderAuthorityServiceTest,MesActiveOrderMigrationContractTest,MesActiveOrderSchemaTest,MesFrontlinePqcActiveOrderAuthorityTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

预期行为型失败：

- PQC 仍通过 `mes_pro_process_pool` 判断活跃订单。
- 同一订单被多个组长加入后得到多个跨角色身份。
- 开放订单缺正式路线版本时迁移未阻塞。
- 活跃订单状态转换缺少唯一键/版本冲突保护。

### M2 RED

优先扩展现有测试，使其断言新行为：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineProductionFactSubmitServiceTest,MesTeamLeaderSubmissionRevisionServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordAggregationServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static
pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static
```

预期行为型失败：

- 原始生产报工仍强制订单、任务或工作站。
- 订单数量 `300`、生产系数 `3.0` 时目标量仍为 `300` 而非 `900`。
- 多次报工完成后只回填代表事件，遗漏其他人员、设备或参数。
- 两个并发确认可造成超额或重复回填。

### M3 RED

先新增前端静态测试文件和对应 `package.json` script，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest,MesQaInspectionRegulationPublishServiceTest,MesPqcTaskGenerationServiceTest,MesFrontlinePqcContextServiceTest,MesPqcSubmissionServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-qa-regulation:static
pnpm --dir IntRuoyiFronted test e2e:role-matrix-pqc-dynamic-form:static
```

预期行为型失败：

- PQC 首检在没有最新生产事件时不能提交。
- 页面仍固定四个项目、`PATROL`、数量 `30` 或损耗 `1`。
- 规程发布后仍可原地修改。
- 上午/下午巡检未按日期、班次、轮次区分。
- PQC 组长可确认自己作为实际检验人的记录。

### M4 RED

先确认调拨、异常、返工、报废和库存的正式 source map，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderStartCheckServiceTest,MesQualityAbnormalServiceTest,MesPqcProcessInspectionAggregationTest,MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-transfer-start-check:static
pnpm --dir IntRuoyiFronted test e2e:edhr:release:check
```

预期行为型失败：

- 一个订单不能表达多调拨、分批、补料、退料或多批次。
- 开工检查只判断“有关联单”，未校验物料/数量/批次。
- 未确认 PQC 被错误算作过程检验完成。
- 放行仍只得到“来源未接入”占位项，无法读取真实生产/PQC/物料来源。

### M5 RED

先新增日结、范围、快照和静态合同测试，再运行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderDailyCloseServiceTest,MesPqcDailyCloseServiceTest,MesRoleMatrixReadModelServiceTest,MesRoleMatrixHistorySnapshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-daily-close-scope:static
```

预期行为型失败：

- 班组长仍可创建独立班组设备而不是绑定设备台账。
- 责任范围缺产线、设备或订单。
- 日结未列出退回未补正、未分配、未确认或批记录缺项。
- 修改配置后历史报工/PQC 显示新值。

## GREEN

### GREEN Rules

- 只实现令当前 RED 通过的最小正式方案。
- 不保留双读、默认值、兼容 shim、mock success 或异常吞噬。
- GREEN 后立即运行相邻回归；任何相邻回归失败都保持 milestone 未完成。
- 记录格式：
  `GREEN: <command> -> PASS`。

### Milestone GREEN Commands

M1、M2、M3、M4、M5 分别重跑对应 RED 命令并要求：

- tests run 大于 `0`。
- failures=`0`，errors=`0`。
- 新增 schema/contract 测试和行为测试同时通过。
- 前端静态合同通过后执行：

```powershell
pnpm --dir IntRuoyiFronted ts:check
```

M2 相邻回归至少包含：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

M3/M4 相邻回归必须继续覆盖原生产组长测试、PQC 上下文、过程检验和 eDHR 放行测试。

## E2E

### E2E Preconditions

执行前必须按项目规则确认：

- 前端、后端端口与当前分支 profile 一致并且服务健康。
- 已读取 `docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md` 和 `docs/database-rules.md`。
- 测试租户、六类角色账号、权限、密码、电子签名、浏览器、数据库、Redis 均可用。
- ERP 已存在任务专用真实候选订单、调拨、发货和批次数据，或可通过正式 ERP/同步页面创建并同步。
- 正式路线、生产系数、SOP、正式逐工序批记录绑定和 QA 规程数据可通过真实页面维护。
- 缺任一前置时 E2E 标记 BLOCKED，不得用 API-only、SQL 直写、mock 或静态合同替代。

### Planned E2E Scripts

实施时新增：

- `IntRuoyiFronted/tests/e2e/role-requirement-matrix-real-flow.e2e.js`
- `e2e:role-requirement-matrix:real:check`
- `e2e:role-requirement-matrix:real`

语法和真实执行：

```powershell
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real
```

### E2E-01 Positive Main Flow

1. 计划/班组长账号通过正式页面确认任务专用 ERP 候选已同步。
2. 生产班组长登录，搜索订单并加入统一活跃订单。
3. 关联两个调拨/发货批次，验证物料覆盖和开工检查。
4. QA 登录，创建规程草稿，配置首检、上午/下午巡检、末检和项目，完成发布。
5. 生产员工登录，在不选择订单/任务的情况下提交工序事实和签名。
6. 生产班组长查看原始报工，退回一次；员工补正后重新签名。
7. 生产班组长确认并按 FIFO 建议/手工调整分配到活跃订单。
8. 验证 ERP 固定数量未变，系数 `1.0` 和 `3.0` 工序分别得到正确目标量。
9. PQC 检验员登录，在尚无依赖生产事件的情况下执行首检，随后执行上午/下午巡检和适用/不适用末检。
10. PQC 组长登录，确认合法提交并退回一笔补正；验证自我确认被阻塞。
11. 验证过程检验汇集、正式批记录多事件聚合、异常状态和日结。
12. 放行负责人登录，查看全部来源并签名放行。
13. API 只读核验 activeOrderId、目标量、批记录、PQC 任务、过程检验和放行状态。

### E2E-02 Missing Prerequisites

分别通过真实页面构造：

- 缺调拨/物料批次。
- 缺 SOP。
- 缺生产系数。
- 缺正式批记录绑定。
- 缺发布 QA 规程。
- 缺实际 PQC 人员或签名。
- 缺必需 PQC 任务确认。

每个场景必须断言具体缺项、来源和阻塞动作；不得只断言通用 toast。

### E2E-03 Role and Scope Isolation

- 生产员工不能维护班组、规程或放行。
- 生产班组长只能只读查看 PQC，不可提交/确认。
- QA 不可确认 PQC 或分配报工。
- PQC 检验员不可发布规程或放行。
- PQC 组长不可确认自己的实际检验提交。
- 放行负责人不可修改原始报工/PQC。
- 调整员工、工序、产线、设备和订单范围后，后端返回结果同步收窄。

### E2E-04 Historical Snapshot and Daily Close

1. 创建并完成一组报工/PQC。
2. 修改员工名称、原因、设备参数上限和 QA 规程版本。
3. 验证历史报工、PQC、签名和批记录仍显示旧快照。
4. 创建未分配、未确认、退回未补正和批记录缺项。
5. 验证班组日结和 PQC 日结按职责分别显示。

### E2E Evidence

证据至少记录：

- 前端/后端 URL。
- 测试租户和角色账号标签，不记录密码。
- 任务数据前缀。
- 订单、路线版本、路线工序、调拨、物料批次、规程版本、报工、PQC 任务、签名、批记录和放行 ID。
- 关键页面断言和最终只读 API 结果。
- 清理命令/页面路径和清理后计数。

## Test Data

所有数据必须属于一个已确认的非生产测试租户，建议前缀：`RRM-20260801-`。

| 数据 | 最小要求 |
|---|---|
| 角色 | 生产员工、生产组长、QA、PQC 检验员、PQC 组长、放行负责人各至少一名；另准备一名可作为实际 PQC 人员的 PQC 组长 |
| ERP 订单 | 固定产品数量 `300`，正式产品、交期、批次和唯一订单 ID |
| 路线 | 正式发布路线版本，至少两个工序；生产系数分别为 `1.0` 和 `3.0` |
| 三类配置 | 同一工序同时准备工序开始配置、正式批记录表单绑定和一个 `formBindings`，用于验证互不替代 |
| 调拨 | 至少两个调拨/发货批次，包含部分发货、补料、退料和多物料批次 |
| 设备 | 正式设备台账中启用、报修、禁用设备各一台 |
| 参数 | 一个正常值、一个超上限值、一个低于下限值 |
| 报工 | 多员工、多设备、多次报工；包含一次退回补正 |
| QA 规程 | 首检固定数量；上午/下午比例 `5%`；一个工序末检适用，一个工序末检不适用 |
| PQC | 首检、上午巡检、下午巡检、末检、退回补正、不合格和自我确认阻塞样本 |
| 签名 | 每个实际操作人可用的正式电子签名；签名人与实际人员不一致的失败样本 |
| 异常 | 生产异常和质量异常各一条，分别包含阻塞和非阻塞状态 |

禁止把账号密码、token、连接密钥或签名凭据写入任务文档或提交。

## Failure Paths

- ERP 候选未同步或正式订单不存在。
- 同一订单存在两条冲突正式路线或两套活跃来源。
- 重复加入、移出后继续分配、终止订单继续生成 PQC。
- 调拨只有单号无正式 ID，物料/数量/批次不覆盖，退料导致净数量不足。
- 生产报工缺工序、实际人员、台账设备、数量、原因或签名。
- 生产报工被强制要求订单/任务/工作站。
- 设备报修/禁用后仍可选；超限值被自动改写。
- 退回覆盖原始报工或补正没有修订链。
- 生产系数缺失、非正数、来源冲突或被默认为 `1`。
- FIFO/手工分配到非活跃订单、工序不匹配、总量不等、超额、重复事件。
- 多次报工批记录只取一个事件；聚合策略缺失仍继续写入。
- 正式批记录绑定缺失时改用 `formBindings`、`MAIN` 或工序开始配置。
- QA 规程缺项目/首检/上午/下午/末检明确配置仍能发布。
- 发布规程被原地修改，历史任务被新版本覆盖。
- PQC 仍依赖最新生产事件或复制生产设备/工作站。
- 页面仍写死项目、类型、数量、损耗或默认合格。
- 巡检数量未向上取整，上午/下午/跨天任务重复或污染旧任务。
- 实际 PQC 人员与签名人不一致。
- PQC 自我确认、退回记录参与完成、旧修订被重复汇集。
- 质量异常进入生产异常流或阻塞条件无责任人/解除条件。
- 班组长能修改 PQC，或权限只在前端隐藏未在后端校验。
- 日结遗漏未分配、未确认、退回未补正、不合格未处理或批记录缺项。
- 放行检查用“来源未接入”占位成功、缺项仍生成待办或重复放行。
- 配置修改改写历史报工/PQC/批记录。
- 一对多 JOIN 导致列表重复、分页总数错误或放行检查重复计数。

## Concurrency and Performance

- 使用数据库锁、唯一键和版本号验证同一业务对象并发转换。
- 报工分配并发必须证明累计量不超过目标量。
- PQC 提交/确认并发必须证明只有一个有效修订/确认。
- 批记录回填并发必须证明同一聚合版本只写一次。
- 放行并发必须证明同一事务只产生一个终态。
- 对活跃订单、组长待办、PQC 待办、日结和放行列表执行合理规模的分页/索引验证。
- 逐件明细加载执行查询计数或性能断言，阻止 N+1。

## Cleanup

- 真实 E2E 使用任务前缀和明确业务 ID。
- 优先通过真实页面撤销/关闭任务数据；API 只用于最终核验和项目规则允许的任务自有清理。
- 不删除非本任务业务记录、系统基线、生产租户数据或 append-only 审计/签名证据。
- 清理完成后记录各任务表/业务对象的剩余计数和无法删除但按治理规则保留的证据。
