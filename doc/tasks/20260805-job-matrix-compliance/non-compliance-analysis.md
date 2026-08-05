# 岗位需求分解矩阵不符合项分析

## 结论摘要

- 输入矩阵：`C:\Users\BJB110\Desktop\3\岗位需求分解矩阵.xlsx`。
- 读取范围：`岗位需求分解矩阵!A5:D27` 共 23 条主流程需求；`衍生需求!A5:D43` 共 39 条衍生需求；合计 62 条。
- 当前系统 canonical 证据显示：M0-M5 来源门禁已验收，RRM-BLK-001..032 已 `RESOLVED_VERIFIED`，最近一次授权 RRM 运行态 `real:check` 已无 SOURCE / ENV / RUNTIME blocker。本轮当前 shell 没有 `RRM_*` 环境变量，只能跑出 ENV blocker-only 的 check 产物，不能代表 canonical full real E2E。
- 当前系统仍处于 M6：全量真实 E2E 和逐 AC 验收尚未全部完成；其中 AC-M01 的后端候选准入硬门禁和前端静态合同已补齐，AC-M04 的加入、冲突、跨角色只读、错误角色拒绝、最终清理和并发门禁已有 PASS 证据，但 62 项 AC 仍未达到 `ACCEPTED`。
- 因此，本次结论为：当前系统对 62 条岗位需求均为“部分具备基础/局部证据，但未达到可声明符合的验收状态”，均记录为不符合项。

## 判定口径

1. 主表第 5-27 行依序映射为 `AC-M01` 至 `AC-M23`；衍生需求第 5-43 行依序映射为 `AC-D01` 至 `AC-D39`。
2. “符合”必须同时具备：正式数据源/代码链路、真实页面成功路径、失败路径、权限或只读隔离、必要的并发/性能/SNAPSHOT/清理证明，并在验收矩阵中达到 `ACCEPTED`。
3. 已有 SOURCE gate 或局部 action evidence 不等于完整符合；只要 AC 仍处于 `E2E_COVERAGE` 或 M6 未验收，即判定为不完全符合。
4. 本分析不使用 mock、默认成功、fallback、API-only 或口头假设替代验收证据。
5. 2026-08-05 业务口径修正：`AC-D03` 不再要求生产班组长或 PQC 组长维护“不良原因”主数据；出现不良时由 PQC 在检验/复核记录中手动输入不良说明或原因，系统需保存原始手输内容并进入后续追溯。

## 当前系统证据

- `blocker-inventory.md` 记录 M1-M5 已关闭：activeOrderId、生产系数快照、QA/PQC、调拨/放行来源、工艺路线三类配置分离均已验证，RRM-BLK-001..032 状态为 `RESOLVED_VERIFIED`。
- `task-state.json` 记录当前里程碑为 `M6`，M6 全量真实 E2E 仍为 `STRUCTURED_BLOCKED`；AC-M04 已有 `activeOrderCleanupCompleted=PASS`，但 62 项 AC 仍需各自完成 AC 级真实页面动作、失败路径、权限隔离、只读核验、清理-readiness 和最终验收。
- `verification-report.md` 记录当前 M6 已有 6 个 phase evidence、20 个 action evidence、2 个 gate evidence，且无 failed action/gate；但仍明确说明尚未完成 62 AC 的完整验收，不能将矩阵标记为全部完成。
- `test-plan.md` 的 Coverage Contract 要求覆盖 `62/62`：`AC-M01` 至 `AC-M23`、`AC-D01` 至 `AC-D39` 各自拥有唯一测试用例。

## 主流程逐条分析

| AC | 矩阵行 | 岗位/角色 | 需求动作 | 当前判断 | 不符合项 |
|---|---:|---|---|---|---|
| AC-M01 | 5 | 计划排产员 / 生产班组长 | 确认生产订单 | 代码级门禁已补齐，真实 E2E 未验收 | 后端已要求工单 `CONFIRMED` 且存在 Kingdee 同步记录 `sourceFid/sourceBillNo`，缺正式 ERP 身份会返回 `BLOCKED_ERP_SYNC_RECORD_MISSING` 并不可选，批量加入也会 fail-fast；前端已补齐“缺 ERP 正式订单”原因码和不可选静态合同。仍缺真实页面按正式 ID/编号查询、未确认/缺正式 ID/越权样本排除、任务数据清理和 AC 级 `ACCEPTED` 证据。 |
| AC-M02 | 6 | 生产班组长 | 填写调拨申请单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 ERP 调拨申请同步后可追溯，且 MES 无创建/编辑入口、缺正式来源时阻塞。 |
| AC-M03 | 7 | 系统 | 同步 ERP 候选数据 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明订单、调拨、发货、批次按正式 ID 幂等同步，且重复、乱序或冲突来源不生成重复事实。 |
| AC-M04 | 8 | 生产班组长 | 加入活跃订单池 | 动作通过但未 AC 验收 | 真实页面加入、冲突路线拒绝、跨角色只读、错误角色写入拒绝、最终清理和后端重复/并发/移出路径均已有 PASS/GREEN 证据；但当前仍属于 `E2E_COVERAGE`，尚未完成 AC 级完整失败路径、权限/只读 breadth、清理-readiness 和全量 M6 coverage 准出。 |
| AC-M05 | 9 | 仓库 | 生成调拨单并发货 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 ERP 发货后可读取物料、数量、批次和状态，且未发货、部分发货和无正式 ID 不显示完整。 |
| AC-M06 | 10 | 物料员 | 核对并解包到线边仓 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明实物核对结果和批次形成追溯证据，且缺失、不一致或越权核对进入明确缺项。 |
| AC-M07 | 11 | 生产班组长 | 关联调拨单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明一个订单可关联多调拨、多物料和多批次，且重复关联幂等、错误订单/租户/数量被拒绝。 |
| AC-M08 | 12 | 生产班组长 | 订单开工检查 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明开工检查逐项展示结果、来源和阻塞原因，且缺项不标记就绪、不自动创建异常。 |
| AC-M09 | 13 | QA | 维护检验规程 | 不完全符合 | 已有 QA 规程入口/局部证据，但尚未完整验收；需证明完整规程可发布并生成不可变版本，缺首检/巡检/末检规则或冲突时发布失败。 |
| AC-M10 | 14 | 生产员工 | 按 SOP 生产 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明未选订单/任务仍可按 SOP 进入工序事实报工，且缺工序/SOP 或越权工序时阻塞。 |
| AC-M11 | 15 | 生产员工 | 生产报工 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明人员、设备、参数、数量、损耗、原因、签名完整保存，且缺必填、设备不可用、签名不一致时拒绝且原始事实不覆盖。 |
| AC-M12 | 16 | PQC 检验员 | 执行首检 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明首检按发布规程固定数量生成并逐件提交，且无规程、重复任务或数量不符时阻塞。 |
| AC-M13 | 17 | PQC 检验员 | 执行上午巡检 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明上午巡检保存日期、班次、轮次并向上取整，且 `301×5%` 非 `16`、跨天重复或轮次冲突时失败。 |
| AC-M14 | 18 | PQC 检验员 | 执行下午巡检 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明下午巡检与上午任务身份分离，且错班次、错日期或复用上午任务时失败。 |
| AC-M15 | 19 | PQC 检验员 | 执行末检 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明适用时生成末检、不适用时保存明确依据，且未显式配置、错误跳过或错误阻塞放行时失败。 |
| AC-M16 | 20 | 生产班组长 | 确认员工报工 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明通过进入分配、退回保留原因和原始提交，且重复确认、覆盖原始记录或退回后继续分配被拒绝。 |
| AC-M17 | 21 | 生产班组长 | 分配报工到生产订单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 FIFO 建议和手工调整只分配活跃订单且总量守恒，且非活跃、工序不匹配、超额、重复事件和版本冲突被拒绝。 |
| AC-M18 | 22 | 系统 | 更新生产订单进度 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明进度按工序目标量更新、ERP 产品数量不变，且系数缺失/非正数、超目标或并发更新被阻塞。 |
| AC-M19 | 23 | 系统 | 写入工序批记录表单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明全部已确认报工按策略汇总到正式批记录，且代表事件丢数、无聚合策略、缺正式绑定或重复回填被阻塞。 |
| AC-M20 | 24 | PQC 组长 | 确认 PQC 检验单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 PQC 组长可确认或退回并保留记录，且未确认/退回不算完成、自我确认和并发确认被拒绝。 |
| AC-M21 | 25 | 系统 | 汇集过程检验记录 | 不完全符合 | 已有局部过程检验汇集证据，但尚未完整验收；需证明只汇集最终已确认修订并可追溯任务/轮次/版本，且未确认、旧修订、重复汇集或跨租户数据被排除。 |
| AC-M22 | 26 | 系统 | 检查批记录完整性 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明所有正式来源完整时预检通过，且缺批记录、PQC、调拨、签名或有阻塞异常时不可放行。 |
| AC-M23 | 27 | 放行负责人 | 审核并放行生产订单 | 不完全符合 | 已有放行只读/准备类局部证据，但尚未完整验收；需证明放行负责人可签名放行或退回并审计，且越权、重复放行、签名缺失或预检未过时拒绝。 |

## 衍生需求逐条分析

| AC | 矩阵行 | 岗位/角色 | 需求动作 | 当前判断 | 不符合项 |
|---|---:|---|---|---|---|
| AC-D01 | 5 | 生产班组长 | 添加本班组员工 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明新员工加入班组并用于后续绑定，且重复、跨租户或无权限添加被拒绝。 |
| AC-D02 | 6 | 生产班组长 | 禁用本班组员工 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明禁用员工不再用于新报工且历史保留，且已禁用仍可新选或历史人员被清空时失败。 |
| AC-D03 | 7 | 生产班组长 | 维护不良原因 | 业务口径已调整，仍未完整验收 | 业务已确认不再维护“不良原因”主数据，PQC 发现不良时手动输入即可；后续需把矩阵/测试计划同步为“PQC 手动录入不良说明并保存快照”，并证明系统不依赖固定不良原因列表、不要求 PQC 组长维护原因、手输内容可追溯且历史记录不被后续修改覆盖。 |
| AC-D04 | 8 | 生产班组长 | 维护损耗原因 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明损耗原因按工序配置并进入报工，且固定前端列表、禁用原因或跨工序原因被拒绝。 |
| AC-D05 | 9 | 生产班组长 | 绑定工序可用设备 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明只能从正式设备台账绑定工序设备，且独立创建设备、重复绑定或跨租户设备被拒绝。 |
| AC-D06 | 10 | 生产班组长 | 设备报修或禁用后的可选控制 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明报修/禁用设备不用于新报工、恢复后可选，且不可用设备仍可提交或历史设备消失时失败。 |
| AC-D07 | 11 | 生产班组长 | 维护设备参数上下限 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明参数上下限、单位和默认值按设备/工序保存，且下限大于上限、默认值越界或单位冲突时拒绝。 |
| AC-D08 | 12 | 生产班组长 | 超限参数复核提醒 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明超限值被标记并提醒复核、原值保留，且系统静默改值、吞提醒或错误判定正常时失败。 |
| AC-D09 | 13 | 生产班组长 | 配置负责范围 | 不完全符合 | 已有局部权限隔离证据，但尚未完整验收；需证明员工、工序、工作站、产线、设备、订单范围均生效，且仅前端隐藏、后端越权返回或范围串租户时失败。 |
| AC-D10 | 14 | 生产班组长 | 班组基础维护审计 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明维护操作记录操作人、时间、前后值和范围，且缺审计、审计可改写或前后值不完整时失败。 |
| AC-D11 | 15 | 生产班组长 | 退回员工报工并保留修订记录 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明原始、退回原因和补正提交可串联追溯，且覆盖原始、断裂修订链或复用旧签名时失败。 |
| AC-D12 | 16 | 生产班组长 | 班组日结与未完成提醒 | 不完全符合 | 已有日结/性能类局部证据，但尚未完整验收；需证明日结逐项展示未分配、未确认、缺项和次日延续，且漏项、重复项、分页错误或跨范围数据时失败。 |
| AC-D13 | 17 | 生产班组长 | 只读查看 PQC 状态 | 不完全符合 | 已有局部只读/权限证据，但尚未完整验收；需证明生产组长可只读查看 PQC 状态，且页面或 API 允许填写/确认 PQC 时失败。 |
| AC-D14 | 18 | 生产班组长 | 查看批记录进度 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明按正式逐工序批记录绑定显示进度，且缺绑定时读取 `formBindings`、`MAIN` 或工序开始即失败。 |
| AC-D15 | 19 | QA | 按产品/路线/版本/工序维护检验规程 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明产品/路线版本/工序可发布独立规程，且错路线、错版本、重复有效规程或跨租户发布被拒绝。 |
| AC-D16 | 20 | QA | 查看工序基础信息 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 QA 页面显示正式 SOP、系数和批记录绑定，且显示旧版本、缺来源或用表单槽位替代时失败。 |
| AC-D17 | 21 | QA | 配置检验项目和标准 | 不完全符合 | 已有 PQC 按规程渲染局部证据，但尚未完整验收；需证明 PQC 完全按规程项目、方法、工具和标准渲染，且固定项目、默认合格或缺关键项仍可提交时失败。 |
| AC-D18 | 22 | QA | 配置首检规则 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明首检固定数量和项目可配置并带入任务，且数量非正、缺项目或发布后任务未带出时失败。 |
| AC-D19 | 23 | QA | 配置上午/下午巡检规则 | 不完全符合 | 已有局部规程渲染证据，但尚未完整验收；需证明上午/下午规则独立、比例向上取整，且两轮混用、比例越界或 `301×5%` 不为 `16` 时失败。 |
| AC-D20 | 24 | QA | 配置末检规则 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明末检需要/不适用显式保存并参与放行，且未配置被当不适用或适用任务未生成时失败。 |
| AC-D21 | 25 | QA | 保存草稿校验 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明草稿缺字段或规则冲突时返回字段级错误，且通用成功、吞异常或无效草稿进入发布时失败。 |
| AC-D22 | 26 | QA | 检查检验规程完整性 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明完整性检查逐产品/路线版本/工序列缺失，且漏检规则、错误来源或缺项仍显示完整时失败。 |
| AC-D23 | 27 | QA | 发布或启用规程版本 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明发布版本不可改写、新任务用新版本，且原地修改发布版、历史任务漂移或双有效版本时失败。 |
| AC-D24 | 28 | PQC 检验员 | 选择活跃订单和路线工序 | 不完全符合 | 已有局部活跃订单/PQC 页面证据，但尚未完整验收；需证明只能选择统一活跃订单及正式路线工序，且终止订单、旧活跃来源或缺路线时阻塞。 |
| AC-D25 | 29 | PQC 检验员 | 选择实际 PQC 人员 | 不完全符合 | 已有实际检验人选择局部证据，但尚未完整验收；需证明共享账号下保存实际 PQC 人员，且默认登录人冒充、无人员范围或跨租户人员被拒绝。 |
| AC-D26 | 30 | PQC 检验员 | 电子签名提交 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明签名人与实际检验人一致并保存快照，且签名缺失、身份不一致、过期或复用签名时失败。 |
| AC-D27 | 31 | PQC 检验员 | 逐件填写检验明细 | 不完全符合 | 已有逐件数量和性能类局部证据，但尚未完整验收；需证明计划数量对应完整可还原逐件明细，且少件、多件、重复序号、N+1 或仅整批结果时失败。 |
| AC-D28 | 32 | PQC 检验员 | 填写不合格、损耗和失败原因 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明不合格/损耗数量、原因和说明参与判定，且数量不守恒、缺原因、负数或默认合格时失败。 |
| AC-D29 | 33 | PQC 检验员 | 生成工序池 PQC 事件 | 不完全符合 | 已有正式提交生成事件局部证据，但尚未完整验收；需证明提交生成可追溯 PQC 事件并进入待办，且重复提交生成双事件、无任务身份或待办不可见时失败。 |
| AC-D30 | 34 | PQC 检验员 | 处理 PQC 组长退回 | 不完全符合 | 已有退回补正修订链局部证据，但尚未完整验收；需证明退回原因、原提交、补正和新签名形成修订链，且覆盖原提交、旧修订参与完成或无新签名时失败。 |
| AC-D31 | 35 | PQC 检验员 | 缺失前置条件阻塞 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明缺订单、路线、规程、人员或签名逐项 fail fast，且默认订单/人员/数量/合格或占位成功即失败。 |
| AC-D32 | 36 | PQC 组长 | 筛选待确认检验提交 | 不完全符合 | 已有筛选分页局部证据，但尚未完整验收；需证明多条件筛选准确且分页总数正确，且条件串扰、重复行、总数漂移或越权数据时失败。 |
| AC-D33 | 37 | PQC 组长 | 查看 PQC 提交详情 | 不完全符合 | 已有详情追溯/权限阻断局部证据，但尚未完整验收；需证明详情展示逐件明细、原因、签名、版本和原 payload，且缺字段、显示错误修订或跨租户详情时失败。 |
| AC-D34 | 38 | PQC 组长 | 确认或退回检验提交 | 不完全符合 | 已有复核负向局部证据，但尚未完整验收；需证明确认/退回留痕、退回不参与完成，且重复终态、未签名确认、退回仍汇集或并发双成功时失败。 |
| AC-D35 | 39 | PQC 组长 | 确认人与实际检验人隔离 | 不完全符合 | 已有自我确认阻断局部证据，但尚未完整验收；需证明后端阻塞确认人等于实际检验人，且只在前端隐藏或共享账号绕过时失败。 |
| AC-D36 | 40 | PQC 组长 | 跟进质量异常 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明不合格形成独立质量异常并有责任/解除条件，且进入生产异常流、无责任人或无法解除时失败。 |
| AC-D37 | 41 | PQC 组长 | 确认过程检验记录汇集 | 不完全符合 | 已有过程检验汇集只读局部证据，但尚未完整验收；需证明组长确认后过程检验汇集可见，且未确认/退回/旧修订参与或重复汇集时失败。 |
| AC-D38 | 42 | PQC 组长 | 过程检验闭环和日结提醒 | 不完全符合 | 已有日结/性能类局部证据，但尚未完整验收；需证明日结提示未提交、未确认、退回、不合格和放行影响，且漏项、重复项、范围越界或分页总数错误时失败。 |
| AC-D39 | 43 | 系统 | 衍生配置不改写历史数据 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明修改配置后历史报工/PQC/批记录保持原快照，且历史显示新值、缺版本或 legacy 猜填参与新判断时失败。 |

## 不符合项汇总

| 分类 | 数量 | 说明 |
|---|---:|---|
| 主流程 AC 未完全符合 | 23 | `AC-M01` 至 `AC-M23` 均未达到完整 `ACCEPTED`。 |
| 衍生需求 AC 未完全符合 | 39 | `AC-D01` 至 `AC-D39` 均未达到完整 `ACCEPTED`。 |
| SOURCE / ENV / RUNTIME blocker | 0 / 当前 shell 缺 RRM env | canonical 授权运行态已无 SOURCE / ENV / RUNTIME blocker；但本轮当前 shell 没有 `RRM_*`，不能刷新 full real E2E 产物。不符合主因仍是 62 项 AC 未达 `ACCEPTED`。 |
| M6 验收缺口 | 62 | 全部 AC 仍需完整真实页面成功路径、失败路径、权限/只读隔离、必要并发/性能/SNAPSHOT、清理闭环和最终验收。 |

## AC-M01 当前进度与下一步

### 已做到

- 后端 admission-diff 已把缺 ERP 正式同步身份的已确认工单标记为 `BLOCKED_ERP_SYNC_RECORD_MISSING`，并设置为不可勾选。
- 后端批量加入排产工单池已 fail-fast 拒绝两类数据：未达到 `CONFIRMED` 的生产工单，以及缺少 Kingdee 正式同步记录或 `sourceFid/sourceBillNo` 为空的工单。
- 错误码已补齐：`PRO_SCHEDULE_ORDER_WORK_ORDER_NOT_CONFIRMED` 表达未确认阻塞，`PRO_SCHEDULE_ORDER_WORK_ORDER_ERP_SYNC_REQUIRED` 表达缺 ERP 正式 ID/编号阻塞。
- 前端已补齐 `BLOCKED_ERP_SYNC_RECORD_MISSING: '缺 ERP 正式订单'`，并沿用后端 `selectable=true` 和 `READY_TO_ADMIT` 的行选择保护，静态合同已覆盖该原因码。
- 已有 RED/GREEN 证据：目标后端 Maven 测试从 3 个预期业务失败变为 70 tests 全部通过；前端静态合同、`node --check` 和 `pnpm ts:check` 已通过。

### 还差什么

- 真实 E2E 尚未完成：还没有通过真实页面证明计划排产员/生产班组长可以按正式 ERP ID/编号查询到已确认订单。
- 样本数据未闭合：还需要任务自有的 ERP 已同步正式订单、未确认订单、缺正式 ID/编号订单、越权或跨租户订单，分别用于成功路径和失败路径。
- AC 级验收未闭合：当前证据属于代码级 GREEN 和前端静态合同，不能替代 M6 要求的真实页面 action evidence、权限隔离、清理-readiness 和 coverage ledger。
- 相邻静态回归存在非本项历史 blocker：`smart-scheduling-smoke-real-flow-static.spec.js` 仍卡在 `autoSchedulePublishResult` 标记缺失，不能把该失败归因到 AC-M01。

### 建议执行顺序

1. 准备任务自有 ERP 同步样本：一条 `CONFIRMED + sourceFid/sourceBillNo` 完整订单，一条未确认订单，一条缺正式 ERP 身份订单，一条越权或跨租户订单。
2. 启动并确认本机前后端运行态、登录账号、菜单权限和计划排产员/生产班组长真实入口。
3. 通过真实页面执行候选查询：用正式 ERP ID/编号查到正向订单，并截图/记录 action evidence；确认未确认、缺正式 ID/编号、越权订单不出现在候选或显示明确阻塞原因。
4. 复跑 AC-M01 目标后端 JUnit、前端静态合同、`pnpm ts:check` 和 M6 `real:check/full real E2E`，把 coverage ledger 中 AC-M01 从代码级 GREEN 提升到 `ACCEPTED`。
5. 清理任务样本或记录可重建夹具；若运行态、账号、菜单或样本缺失，保持 blocker，不得用 API-only 或静态测试冒充真实 E2E。

## AC-M04 当前进度与下一步

### 已做到

- `joinActiveOrder=PASS`：生产班组长已能通过真实页面把订单加入活跃订单池，并返回统一 `activeOrderId=12`。
- `activeOrderConflictRouteRejected=PASS`：冲突路线已在真实页面链路 fail-fast 拒绝，且不会先插入错误 active order。
- `activeOrderCrossRoleReadOnly=PASS`：PQC 检验员已能只读读取同一 `activeOrderId`，证明跨角色统一订单身份。
- `activeOrderUnauthorizedMutationBlocked=PASS`：专用错误角色 `aoteman` 写入被后端拒绝，返回 `403 / 没有该操作权限`。
- `activeOrderCleanupCompleted=PASS`：真实后端移出接口已清理 `activeOrderId=12`，回读 active order count 为 0。
- 并发/重复后端证据已 GREEN：重复加入、`DuplicateKeyException` 并发唯一键、冲突路线前置拒绝、显式 `ACTIVE -> REMOVED` 条件更新均已通过目标或相邻回归。

### 还差什么

- AC-M04 仍未从 `E2E_COVERAGE` 变成 `ACCEPTED`，原因是 M6 总验收仍要求每个 AC 都具备完整真实页面成功路径、失败路径、权限/只读 breadth、清理-readiness 和全量 coverage 准出记录。
- 当前 `result.json` 已被本轮缺少 `RRM_*` 环境的 `real:check` 覆盖为 ENV blocker-only 产物；它既不再包含旧 `activeOrderCleanupDeferred`，也没有 action/gate evidence。应以后续 `test-report.md` / `verification-report.md` 中 `activeOrderCleanupCompleted=PASS` 的 canonical 证据为准，并在有正式 RRM 环境时重新跑 full real E2E 刷新产物。

### 建议执行顺序

1. 在正式 RRM 环境变量齐备的 shell 中复跑 `real:check` 和 full real E2E，刷新 canonical `result.json`，确认 AC-M04 coverage row 不再引用 `activeOrderCleanupDeferred`。
2. 在 coverage ledger 中为 AC-M04 明确列出已满足项：成功加入、重复加入幂等/唯一键、冲突路线失败、错误角色拒绝、跨角色只读、最终清理、并发 proof。
3. 若 ledger 仍要求更多 breadth，补真实页面负向用例：非活跃/终止订单、无路线版本或缺正式路线、跨租户或越权订单、重复提交后的页面回读一致性。
4. 最后运行 `real:check`、full real E2E、`MesTeamLeaderActiveOrderServiceTest` 和相关静态合同，全部 PASS 后再把 AC-M04 从 `PASS_ACTION_NOT_ACCEPTED` 提升为 `ACCEPTED`。

## AC-M11 代码级补充复核

用户追问项：`尚未达到 ACCEPTED；需证明人员、设备、参数、数量、损耗、原因、签名完整保存，且缺必填、设备不可用、签名不一致时拒绝且原始事实不覆盖`。

从当前代码继续细查后，除“尚未达到 ACCEPTED”本身外，AC-M11 还存在以下可确认的不符合或未闭合风险：

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 1 | 报工仍强制依赖生产工单、生产任务、工作站，不满足“工序事实优先，订单归属后续分配”。 | 后端 `MesProFrontlineFeedbackPayloadReqVO` 将 `workstationId`、`workOrderId`、`taskId` 均标为必填；`MesProFrontlineProcessPoolContextReqVO` 同样要求 `workOrderId`、`taskId`、`workstationId`、`deviceId`；前端 `assertFrontlineFormalSubmitContext` 也把订单上下文、生产任务、工作站、设备作为必填；P0 真实 E2E 通过环境变量和 URL 预置这些 ID。 | 不能证明“未确定订单归属时先保存工序原始事实”；真实页面成功路径仍可能只是“有订单/任务上下文”的报工。 |
| 2 | 正式报工主记录没有承载 AC-M11 所要求的完整事实字段。 | `MesProFeedbackDO` 只有工作站、路线、工序、工单、任务、数量、报工人、状态、备注等字段，未见 `rawPayload`、`equipmentParameters`、`signatureId`、`signatureSnapshot`、结构化损耗/不良原因字段；完整事实被拆到记录本 entry 和工序池 event。 | 如果后续确认、分配、批记录回填读取正式报工主表，无法单表证明“人员、设备、参数、数量、损耗、原因、签名完整保存”。 |
| 3 | 设备参数服务端未按配置规则逐项校验。 | 运行态会下发参数 `lowerLimit`、`upperLimit`、`defaultValue`、`valueType`，但提交 VO 中 `equipmentParameters` 是自由 `Map<String,Object>`；`validateSubmitContext` 只校验大对象、签名和设备账号存在，未读取参数规则校验编码、单位、类型、上下限或必填。 | 缺参数、越界参数、伪造参数编码或类型错误仍缺少后端 fail-fast 证明；仅靠前端默认值/输入控件不能满足验收。 |
| 4 | 损耗原因/不良原因没有结构化提交与强制校验。 | 前端把 `defects` 放入 `recordbookPayload.entryContent`，后端提交 VO 和工序池 BO 没有 `lossReasonId`、`reasonCode` 等正式字段；运行态虽返回 enabled defect reasons，但提交服务未校验 `lossQuantity > 0` 时原因必须存在且来自当前工序配置。 | 只能证明“可能保存在 raw/entryContent”，不能证明“原因完整保存、禁用/跨工序原因拒绝、缺原因拒绝”。 |
| 5 | 数量守恒和损耗边界未 fail-fast。 | `lossQuantity` 仅 `@NotNull`，未见非负、`loss <= output`、损耗分项合计等服务端约束；拆分器将 `qualifiedQuantity = outputQuantity - lossQuantity` 后用 `.max(BigDecimal.ZERO)` 截断。 | 当损耗大于产出或分项不守恒时，系统可能生成 0 合格数而不是拒绝，掩盖原始异常事实。 |
| 6 | 签名完整性只证明“人员 ID 一致”，未证明签名快照/授权有效性完整保存。 | 生产提交请求只有 `signatureId`、`signatureEmployeeId`，没有 `signatureSnapshot`；PQC VO 反而有 `signatureSnapshot`；生产提交 adapter 只把 `signatureId` 和 `signatureUserId` 传给工序池，未设置 `signatureSnapshot`。 | 只能证明 `actualEmployeeId == signatureEmployeeId` 和签名 ID 唯一，不能证明签名图像/授权版本/签名时点快照不可漂移。 |
| 7 | 设备不可用拒绝链路仍需补后端负向证明。 | 运行态配置会过滤 enabled 且 `DEVICE_STATUS_ENABLED` 的团队设备；但提交授权主要校验候选工序、设备/工作站匹配、人员绑定、模板匹配。工作站岗位路线候选源只校验工作站启用和机器存在，未在已读提交链路中看到对“已禁用/报修设备”的最终状态复核。 | 若前端缓存或恶意请求提交旧设备 ID，需要证明后端能拒绝不可用设备；当前证据不足以声明满足“设备不可用时拒绝”。 |
| 8 | 现有测试仍偏结构/Happy Path，缺 AC-M11 负向和原始事实不覆盖证明。 | `MesP0ProductionExecutionSchemaContractTest` 主要断言字段存在；`p0-production-execution-loop-real.e2e.js` 预置工单/任务/工作站/设备/签名 ID；`role-requirement-matrix-real-flow.e2e.js` 的 `productionEmployeeEntry` 只覆盖入口加载并关联 `AC-M10/AC-M11`。 | 未覆盖缺必填、设备不可用、签名不一致、参数越界、损耗原因缺失、重复提交不覆盖原始事实等 AC-M11 准出条件。 |

## AC-D03 手动不良说明专项核验

### 核验结论

| 核验项 | 当前判断 | 代码/证据依据 | 缺口 |
|---|---|---|---|
| 系统是否支持手动输入 | 部分支持 | PQC 页面支持逐件手工录入数值、逐件选择“合格/不合格”，并可填写检验数量、损耗数量；`pqcDraft` 当前只有 `inspectionType`、`patrolRound`、`inspectionQuantity`、`scrapQuantity`。 | 未看到 PQC 不良说明/原因的专用文本输入字段；当前不能证明“出现不良时 PQC 手动输入不良原因/说明”。 |
| 是否保存原始输入快照 | 基础支持 | `MesFrontlinePqcSubmitReqVO.rawPayload` 必填；`buildPqcInspectionEventRawPayload` 先复制前端 `rawPayload`，再补充 `activeOrderId`、`pqcTaskId`、`workOrderId`、`routeProcessId`、`processId`、`pqcItemDetails`、`pieceDetailCount` 等；PQC event 和 PQC record 均写入 `rawPayload`。 | 如果前端没有传“不良说明/原因”字段，快照不会包含该业务输入；缺少专项测试证明手输文本进入 `rawPayload`。 |
| 是否能追溯到订单/工序/PQC 记录 | 基础支持较完整 | PQC 提交 VO、Command、event、PQC record 均包含 `workOrderId`、`routeId`、`routeProcessId`、`processId`、`pqcTaskId`、`productionSubmitEventId`；提交服务校验 PQC task 身份和当前订单/工序一致；时间线读模型返回 `originalPayloadJson`、工单、工序、PQC task 和 PQC 结果。 | 仍需专项验收把“手动不良说明”与同一订单、工序、PQC event/detail 页面一起回读证明。 |
| 历史记录是否不会被后续修改覆盖 | 部分支持 | PQC record 的 `rawPayload` 在创建时写入，当前检索未发现更新该字段的服务路径；退回补正有 revision/diff 表记录 `beforePayload`、`afterPayload`、字段前后值和修订签名。 | event 表 `rawPayload` 会在修订服务中被更新为 `afterPayload`，时间线详情的 `originalPayloadJson` 读取的是当前 event `raw_payload`；因此现状更接近“有修订链可追溯”，不是严格的“原始详情永不被覆盖”。 |

### 结论口径

- `AC-D03` 不能按旧口径继续要求“生产班组长/PQC 组长维护不良原因主数据”；旧的 defect reason 目录能力不应作为新版 `AC-D03` 符合证据。
- 新口径应改为：PQC 在发现不良时手动录入不良说明/原因，系统保存原始手输文本快照，并可按订单、工序、PQC event/record 追溯。
- 当前系统具备“PQC 提交、逐件明细、原始 payload、订单/工序/PQC 记录追溯、退回补正修订链”的基础能力，但缺少“手动不良说明字段 + 进入 rawPayload + 详情回显 + 原始/修订不覆盖”的专项验收证据。
- 当前准确状态应保持为：`业务口径已调整，仍未完整验收`，不能提升为 `ACCEPTED`。

### 建议补充验收用例

1. PQC 将某逐件检验项标记为不合格，手动输入“不良说明/原因”，正式提交后在 event `rawPayload`、PQC record `rawPayload` 和 PQC 组长详情中回读同一文本。
2. 回读详情必须同时显示 `workOrderId/workOrderCode`、`routeProcessId/processId`、`pqcTaskId`、`eventId`、`productionSubmitEventId`，证明手输内容绑定到对应订单、工序和 PQC 记录。
3. 修改后续配置或不良原因目录后，历史 PQC 详情仍显示当时手输文本，不被新配置、新文案或固定列表覆盖。
4. PQC 组长退回后，PQC 补正可形成 revision/diff；详情必须能区分首次提交原始文本和补正后文本，失败条件为只显示新值且无法追溯旧值。
5. 负向路径：没有填写不良说明却提交不合格、前端只允许固定列表、后端未保存手输字段、跨订单/跨工序详情可见、修订直接覆盖无 diff，均应失败。

## 2026-08-05 代码级继续审计：明确不符合项

用户追问项：`从系统代码分析来看，还有哪些不符合`。

本节只记录从当前系统代码直接能看出的结构性不符合或未闭合风险；不重复“62 项尚未 ACCEPTED”这一总体验收状态。

| 序号 | 涉及 AC | 代码级不符合/未闭合风险 | 代码证据 | 影响 |
|---:|---|---|---|---|
| 1 | AC-M08 | 订单加入活跃池时直接置为 `ACTIVE`，没有正式“开工检查/开工就绪”模型。 | `MesTeamLeaderActiveOrderServiceImpl.addActiveOrder` 创建 `MesProcessPoolActiveOrderDO` 后直接设置 `activeStatus/businessStatus = STATUS_ACTIVE`；仅通过 `requireMatchingScheduleOrder`、`insertProcessSnapshots` 等异常阻塞，不返回逐项检查结果、来源、缺项和阻塞原因。`MesProcessPoolTeamLeaderController` 当前提供加入/移出、异常上报、员工/设备维护等入口，未发现 start-check/readiness checklist 接口。 | 只能证明“加入时做了部分前置校验”，不能证明班组长开工前看到逐项结果、缺项不标记就绪、缺项不自动创建异常。 |
| 2 | AC-D01 / AC-D02 | 员工来源仍是班组长本地档案，不是正式 HR/用户主数据强约束。 | `MesTeamLeaderRuntimeConfigServiceImpl#createEmployee` 直接插入 `MesProcessPoolTeamEmployeeProfileDO`，`systemUserId` 可为空；`bindEmployeeToProcess` 只校验本地 profile 属于 leader 且 enabled；Controller 暴露 `/employee-profile/create` 和 `/process-employee-binding/save`。 | 不能证明“班组员工来自正式人员档案并可按正式身份追溯”；临时/本地员工仍可能进入后续报工绑定。 |
| 3 | AC-D05 / AC-D06 | 设备来源仍允许班组长本地创建设备，不是只能从正式设备台账绑定。 | `MesTeamLeaderRuntimeConfigServiceImpl#createDevice` 从请求的 `deviceCode/deviceName/deviceStatus` 直接创建 `MesProcessPoolTeamDeviceDO`；Controller 暴露 `/team-device/create`；`assertDeviceAvailable` 只校验本地 enabled 和本地状态 `ENABLED`。 | 设备可用性有局部校验，但源头不是正式设备台账，无法满足“只能从正式设备台账绑定工序设备”。 |
| 4 | AC-D07 / AC-D08 / AC-M11 | 设备参数上下限仅在维护配置时校验，提交报工时没有后端按上下限逐项判断、阻塞或生成复核提醒。 | `saveDeviceParameterRule` 与 `validateRange` 校验配置下限、上限、默认值；一线页面将 `equipmentParameters` 作为自由对象提交；当前检索未发现提交服务读取参数规则并判断 `lowerLimit/upperLimit` 的超限逻辑或“超限复核提醒”对象。 | 配置存在不等于运行时执行；越界值、伪造参数编码、缺参数或类型错误仍缺少后端 fail-fast/提醒证据。 |
| 5 | AC-M09 / AC-D15~D23 | QA 检验规程后端当前主要是只读发布版本查询，没有正式保存草稿、校验完整性、发布/启用接口。 | `MesQaInspectionRegulationController` 只有 `/published-version` 和 `/project-statuses` 两个 GET；`MesQaInspectionRegulationServiceImpl` 只读取最新已发布版本和产品配置状态；前端 `QaRegulationPage.vue` 明示“正式保存/发布接口未接入，本页调整仅用于前端规则预览和发布前检查，未写入后台”。 | 不能证明 QA 可维护、保存草稿、发布不可变版本；首检/巡检/末检规则的正式发布链路仍不完整。 |
| 6 | AC-M12~M15 / AC-D18~D20 | 未发现生产代码按 QA 规程生成 PQC 任务。 | 主代码检索 `MesPqcInspectionTaskDO.builder()`、`pqcInspectionTaskMapper.insert`、`create/generate Pqc task` 未发现正式生成服务；`MesPqcInspectionTaskMapper` 只提供查询 pending/list 和 `updateSubmittedIfPending`；PQC 提交服务要求已有 `pqcTaskId` 且任务为 `PENDING`。 | 首检、上午巡检、下午巡检、末检无法证明按发布规程自动生成任务；`301×5%` 向上取整、班次/轮次、首检固定数量等规则也缺少后端生成证据。 |
| 7 | AC-D27 / AC-D28 / AC-M12~M15 | PQC 数值型样本没有按标准上下限判定；前端还会用标准下限自动补齐空样本。 | 前端 `pqcInspectionItems` 把数值项默认值设为 `standardLowerLimit`，`getPqcStoredPieceValues` 会补齐到检验数量；`resolvePqcResult` 只在损耗数量大于 0 或逐件值等于“不合格”时失败，不比较数值上下限；后端 `resolvePieceJudgement` 同样只看值是否为“不合格”或总结果是否失败/成功。 | 缺失数值输入或越界数值可能被默认值/总结果掩盖，不能证明逐件明细按规程标准真实判定。 |
| 8 | AC-D03 新口径 / AC-D28 | PQC 提交结构缺少“不合格原因/说明”专用字段。 | `MesFrontlinePqcSubmitReqVO` 和 `MesFrontlinePqcSubmitCommand` 只有 `inspectionResult`、`itemResults`、`sampleValues`、`rawPayload` 等字段；`ItemResult` 只有项目、设备和样本值；前端 rawPayload 记录 `inspectionType/patrolRound/inspectionQuantity/scrapQuantity/pqcPieceValues`，未看到手输失败原因字段。 | 按新业务口径，PQC 不再维护固定不良原因主数据时，系统仍不能证明“不合格时手动输入说明并保存、回显、追溯”。 |
| 9 | AC-D03 旧口径迁移风险 | 系统仍保留班组长“不良原因”主数据维护和一线生产缺陷原因下发。 | Controller 暴露 `/defect-reason/create`；运行态配置仍返回 `defectReasons`；前端生产缺陷原因来自 runtime config。 | 如果矩阵口径已经改为 PQC 手动说明，则旧“不良原因目录”不能作为符合证据；还需确认不会要求 PQC 依赖固定原因列表。 |
| 10 | AC-D36 | PQC 不合格没有看到自动生成独立质量异常的正式链路。 | `MesWorkOrderAbnormalReportServiceImpl#markAndReport` 只根据生产组长手工请求创建 `MesProcessPoolWorkOrderAbnormalDO`；Controller 入口是 `/work-order/abnormal/report`；针对 `INSPECTION_RESULT_FAILURE` 的检索只落在 PQC 结果判定/事件校验，未发现从 PQC failure 自动 insert 异常。 | 不合格 PQC 结果可能只停留在 PQC event/record，不能证明形成独立质量异常、责任和解除条件。 |
| 11 | AC-M20 / AC-M21 / AC-M22 | PQC 组长确认后的任务状态闭环不完整：放行预检要求 `CONFIRMED`，但提交链路只把任务置为 `SUBMITTED`，组长确认聚合也未回写任务 `CONFIRMED`。 | `MesFrontlinePqcContextServiceImpl` 将 PQC task 从 `PENDING` 更新为 `SUBMITTED`；`MesTeamLeaderSubmissionReviewServiceImpl` 审核通过时仅调用 `aggregateApprovedPqcSubmission`；`MesPqcProcessInspectionAggregationServiceImpl` 只更新 PQC record 的过程检验聚合字段；`MesOrderReleaseCompletenessServiceImpl` 预检却过滤 `taskStatus != CONFIRMED` 并阻塞。 | 过程检验汇集与放行预检之间存在状态断点，可能导致已审核 PQC 仍无法满足放行预检，或需要额外未证明的状态转换。 |
| 12 | AC-M22 / AC-M23 | 放行预检在当前已读服务里主要覆盖 PQC、偏差、返工、报废、库存一致性，批记录逐工序完整性/签名完整性仍需另行证明。 | `MesOrderReleaseCompletenessServiceImpl` 检查 `evaluateInspectionResult`、`evaluateDeviationClosed`、`evaluateReworkClosed`、`evaluateScrapRecorded`、`evaluateInventoryConsistency` 等；本轮未在该服务中看到“所有正式批记录表单按逐工序绑定完整填写并签名”的直接检查。 | 如果批记录完整性在其它 release 服务实现，需要补明确证据；否则 AC-M22/M23 的“批记录完整性和放行前置”仍不闭合。 |

### 代码级优先整改顺序

1. 先补正式开工检查模型：用 checkItems 明确展示来源、状态、缺项和 blocker，开工就绪与活跃订单加入分离。
2. 把班组员工/设备的来源改为正式主数据绑定：禁止本地创建设备/临时员工冒充正式台账。
3. 补 QA 规程写入/草稿校验/发布链路，并从发布版本生成 PQC 任务。
4. 补 PQC 数值上下限判定、不合格手动说明、质量异常自动生成和 PQC task `CONFIRMED` 状态闭环。
5. 最后补放行预检对正式批记录逐工序完整性、签名和 PQC/异常闭环的一致性证明。

## 整改建议

- 继续按 M6 分 AC 切片推进，不得把 phase evidence 或单个 action evidence 等同于整项 AC 验收通过。
- 每个 AC 应补齐：真实页面成功路径、对应失败路径、后端权限隔离、只读核验、必要并发/性能/SNAPSHOT 证明、任务数据清理或可重建夹具证明。
- 维持 M0-M5 已关闭的正式来源约束，禁止重新引入默认值、fallback、`formBindings` 替代批记录绑定、API-only 验收或临时夹具成功。
