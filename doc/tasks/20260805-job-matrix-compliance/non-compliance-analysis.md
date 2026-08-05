# 岗位需求分解矩阵不符合项分析

## 结论摘要

- 输入矩阵：`C:\Users\BJB110\Desktop\3\岗位需求分解矩阵.xlsx`。
- 读取范围：`岗位需求分解矩阵!A5:D27` 共 23 条主流程需求；`衍生需求!A5:D43` 共 39 条衍生需求；合计 62 条。
- 当前系统 canonical 证据显示：M0-M5 来源门禁已验收，RRM-BLK-001..032 已 `RESOLVED_VERIFIED`，最近一次授权 RRM 运行态 `real:check` 已无 SOURCE / ENV / RUNTIME blocker。本轮当前 shell 没有 `RRM_*` 环境变量，只能跑出 ENV blocker-only 的 check 产物，不能代表 canonical full real E2E。
- 当前系统仍处于 M6：全量真实 E2E 和逐 AC 验收尚未全部完成；其中 AC-M01 的后端候选准入硬门禁、前端静态合同和 RRM action 接入已补齐，AC-M04 的加入、冲突、跨角色只读、错误角色拒绝、最终清理和并发门禁已有 PASS 证据，但 62 项 AC 仍未达到 `ACCEPTED`。
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
| AC-M01 | 5 | 计划排产员 / 生产班组长 | 确认生产订单 | 代码级门禁与 RRM action 已接入，真实 E2E 未验收 | 后端已要求工单 `CONFIRMED` 且存在 Kingdee 同步记录 `sourceFid/sourceBillNo`，缺正式 ERP 身份会返回 `BLOCKED_ERP_SYNC_RECORD_MISSING` 并不可选，批量加入也会 fail-fast；前端已补齐“缺 ERP 正式订单”原因码和不可选静态合同；RRM 真实流程脚本已在生产组长加入活跃订单前接入 `scheduleOrderErpCandidateAdmission`。仍缺正式 RRM 环境下真实页面跑通、按正式 ID/编号查询、未确认/缺正式 ID/越权样本排除、任务数据清理和 AC 级 `ACCEPTED` 证据。 |
| AC-M02 | 6 | 生产班组长 | 填写调拨申请单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 ERP 调拨申请同步后可追溯，且 MES 无创建/编辑入口、缺正式来源时阻塞。 |
| AC-M03 | 7 | 系统 | 同步 ERP 候选数据 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明订单、调拨、发货、批次按正式 ID 幂等同步，且重复、乱序或冲突来源不生成重复事实。 |
| AC-M04 | 8 | 生产班组长 | 加入活跃订单池 | 动作通过但未 AC 验收 | 真实页面加入、冲突路线拒绝、跨角色只读、错误角色写入拒绝、最终清理和后端重复/并发/移出路径均已有 PASS/GREEN 证据；但当前仍属于 `E2E_COVERAGE`，尚未完成 AC 级完整失败路径、权限/只读 breadth、清理-readiness 和全量 M6 coverage 准出。 |
| AC-M05 | 9 | 仓库 | 生成调拨单并发货 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明 ERP 发货后可读取物料、数量、批次和状态，且未发货、部分发货和无正式 ID 不显示完整。 |
| AC-M06 | 10 | 物料员 | 核对并解包到线边仓 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明实物核对结果和批次形成追溯证据，且缺失、不一致或越权核对进入明确缺项。 |
| AC-M07 | 11 | 生产班组长 | 关联调拨单 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明一个订单可关联多调拨、多物料和多批次，且重复关联幂等、错误订单/租户/数量被拒绝。 |
| AC-M08 | 12 | 生产班组长 | 订单开工检查 | 不完全符合 | 尚未达到 `ACCEPTED`；需证明开工检查逐项展示结果、来源和阻塞原因，且缺项不标记就绪、不自动创建异常。 |
| AC-M09 | 13 | QA | 维护检验规程 | 不完全符合 | 已有 QA 规程入口/局部证据，但尚未完整验收；需证明完整规程可发布并生成不可变版本，缺首检/巡检/末检规则或冲突时发布失败。 |
| AC-M10 | 14 | 生产员工 | 按 SOP 生产 | 代码级已修复，仍待全量 AC 验收 | 已补 `20260805-ac-m10-sop-production-fact-reporting`：生产模式入口使用设备账号授权工序列表，不依赖 PQC 活跃订单；生产预校验不再要求订单上下文；缺 SOP/模板和越权工序由后端目标 JUnit 覆盖 fail-fast；正式一体提交补齐后端必填工序池幂等键。仍需在 M6 全量真实 E2E 中完成 AC 级页面验收和清理-readiness。 |
| AC-M11 | 15 | 生产员工 | 生产报工 | 不完全符合（数量/损耗边界代码级已修复） | 已补 `20260805-ac-m11-production-quantity-validation`：生产提交服务端拒绝负数产出、负数损耗、损耗大于产出，拆分器不再用 0 合格数截断掩盖异常。AC-M11 尚未达到 `ACCEPTED`；仍需证明人员、设备、参数、原因、签名完整保存，且缺必填、设备不可用、签名不一致、原始事实覆盖等场景被拒绝。 |
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
| AC-D03 | 7 | 生产班组长 | 维护不良原因 | 代码级已补，页面只读预检通过，仍未完整验收 | 业务已确认不再维护“不良原因”主数据；本轮已补 PQC 手动不良说明输入、提交字段、失败必填校验和 rawPayload 快照，并通过真实页面只读预检证明 PQC 填写页可见且手动输入控件可录入。仍需写入型真实 E2E/详情回读证明系统不依赖固定不良原因列表、不要求 PQC 组长维护原因、手输内容可追溯且历史记录不被后续修改覆盖。 |
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
- RRM 真实流程脚本已补入 AC-M01 动作：生产组长先进入 `/mes/pro/schedule-order`，点击真实“同步工单”页签，再用 `admission-diff` 按正式生产订单编号核验候选，同时要求 `BLOCKED_ERP_SYNC_RECORD_MISSING` 行保持 `selectable=false`。
- 已有 RED/GREEN 证据：目标后端 Maven 测试从 3 个预期业务失败变为 70 tests 全部通过；前端静态合同、RRM AC-M01 action 静态合同、脚本 `node --check` 和 `pnpm ts:check` 已通过。

### 还差什么

- 真实 E2E 尚未完成：脚本已接入真实页面动作，但当前 shell 缺少 RRM 真实运行所需 URL、租户、角色账号、电子签名、任务订单/路线/调拨/规程等环境变量，还没有在正式 RRM 环境跑出 PASS。
- 样本数据未闭合：还需要任务自有的 ERP 已同步正式订单、未确认订单、缺正式 ID/编号订单、越权或跨租户订单，分别用于成功路径和失败路径。
- AC 级验收未闭合：当前证据属于代码级 GREEN 和前端静态合同，不能替代 M6 要求的真实页面 action evidence、权限隔离、清理-readiness 和 coverage ledger。
- 相邻静态回归存在非本项 blocker：`smart-scheduling-smoke-real-flow-static.spec.js` 仍卡在 `autoSchedulePublishResult` 标记缺失；`pnpm e2e:role-requirement-matrix:preflight:static` 当前已 PASS，不再阻塞 AC-M01 静态准出。

### 建议执行顺序

1. 准备任务自有 ERP 同步样本：一条 `CONFIRMED + sourceFid/sourceBillNo` 完整订单，一条未确认订单，一条缺正式 ERP 身份订单，一条越权或跨租户订单。
2. 在正式 RRM shell 中补齐 `RRM_FRONTEND_URL/RRM_BACKEND_URL`、租户、六类角色账号、电子签名 JSON、订单/路线/调拨/规程 ID，并确认本机前后端运行态、菜单权限和计划排产员/生产班组长真实入口。
3. 通过已接入的 `scheduleOrderErpCandidateAdmission` 动作执行候选查询：用正式 ERP ID/编号查到正向订单，并记录 action evidence；确认未确认、缺正式 ID/编号、越权订单不出现在候选或显示明确阻塞原因。
4. 复跑 AC-M01 目标后端 JUnit、前端静态合同、RRM AC-M01 action 静态合同、`pnpm ts:check` 和 M6 `real:check/full real E2E`，把 coverage ledger 中 AC-M01 从代码级 GREEN 提升到 `ACCEPTED`。
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
| 5 | 数量/损耗主边界已代码级修复，损耗分项合计仍待补充。 | `20260805-ac-m11-production-quantity-validation` 已在 `MesProFrontlineFeedbackSubmitServiceImpl` 增加输出数量必须大于 0、损耗数量不能小于 0、损耗不能大于输出的服务端校验；`MesProFrontlineFeedbackPayloadSplitter` 已移除 `.max(BigDecimal.ZERO)` 截断。 | 负数产出、负数损耗和损耗大于产出已 fail-fast；但工废/料废/其他废品合计是否等于损耗、损耗原因联动仍未闭合，AC-M11 不能因此整体通过。 |
| 6 | 签名完整性只证明“人员 ID 一致”，未证明签名快照/授权有效性完整保存。 | 生产提交请求只有 `signatureId`、`signatureEmployeeId`，没有 `signatureSnapshot`；PQC VO 反而有 `signatureSnapshot`；生产提交 adapter 只把 `signatureId` 和 `signatureUserId` 传给工序池，未设置 `signatureSnapshot`。 | 只能证明 `actualEmployeeId == signatureEmployeeId` 和签名 ID 唯一，不能证明签名图像/授权版本/签名时点快照不可漂移。 |
| 7 | 设备不可用拒绝链路仍需补后端负向证明。 | 运行态配置会过滤 enabled 且 `DEVICE_STATUS_ENABLED` 的团队设备；但提交授权主要校验候选工序、设备/工作站匹配、人员绑定、模板匹配。工作站岗位路线候选源只校验工作站启用和机器存在，未在已读提交链路中看到对“已禁用/报修设备”的最终状态复核。 | 若前端缓存或恶意请求提交旧设备 ID，需要证明后端能拒绝不可用设备；当前证据不足以声明满足“设备不可用时拒绝”。 |
| 8 | 现有测试仍偏结构/Happy Path，缺 AC-M11 负向和原始事实不覆盖证明。 | `MesP0ProductionExecutionSchemaContractTest` 主要断言字段存在；`p0-production-execution-loop-real.e2e.js` 预置工单/任务/工作站/设备/签名 ID；`role-requirement-matrix-real-flow.e2e.js` 的 `productionEmployeeEntry` 只覆盖入口加载并关联 `AC-M10/AC-M11`。 | 未覆盖缺必填、设备不可用、签名不一致、参数越界、损耗原因缺失、重复提交不覆盖原始事实等 AC-M11 准出条件。 |

### AC-M11 第二轮补充缺口

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 9 | 记录本“原始条目”创建后仍是草稿，未在生产提交链路中提交/锁定。 | `createOriginalEntry` 只调用 `recordbookService.createEntry`；`createEntry` 新增 entry 时状态为 `ENTRY_STATUS_DRAFT`、版本为 `1`，生产提交链路没有继续调用 `recordbookService.submit`；而 `saveDraft` 会在草稿状态下直接更新 `entryContentJson`。 | 不能证明记录本中的原始事实“不可覆盖”；只要有草稿编辑权限，原始条目仍存在被后续保存草稿覆盖的路径。 |
| 10 | 记录本幂等键命中旧条目时直接返回旧 entry，未校验本次 payload 与旧 payload 是否一致。 | `createEntry` 用 `recordbookId + idempotencyKey` 查询旧 entry，命中后直接 `return toEntryResp(existing, ...)`，没有比对 `entryContentJson`、设备参数或 rawPayload。 | 如果前次提交在创建记录本后、创建工序池事件前失败，重试携带不同事实但相同记录本幂等键，系统会复用旧记录本事实而不是显式拒绝冲突。 |
| 11 | 工序池事件幂等查询维度与数据库唯一约束不一致。 | 查询 `selectSubmitByIdempotency` 包含 `deviceId`、`workstationId`；迁移唯一键 `uk_mes_pro_process_pool_event_idem` 只包含租户、事件类型、工单、路线工序、工序、实际员工、幂等键、删除标记。 | 同一幂等键但设备/工作站不同的请求，查询可能找不到旧事件，插入时再触发数据库唯一冲突；缺少明确的“原始事实冲突拒绝且不覆盖”业务错误证明。 |
| 12 | 工序池修订会更新事件主表 `rawPayload` 为补正后的 payload。 | `MesProcessPoolEventRevisionServiceImpl.updateOriginalRecord` 会先保存 revision 的 `beforePayload/afterPayload`，随后 `eventMapper.updateById(...setRawPayload(reqBO.getAfterPayload()))` 覆盖事件主表 raw payload。 | 有修订链可追溯，但事件主表的“原始 payload”会变成补正后值；依赖 event 主表的后续批记录回填、时间线或详情不能直接证明首次原始事实未覆盖。 |
| 13 | 生产提交损耗主关系已补服务端校验，原因与分项仍未验收。 | `MesProFrontlineFeedbackSubmitServiceImpl.validateProductionQuantity` 已在授权、幂等查询和写入前拒绝 `output <= 0`、`loss < 0`、`loss > output`；目标 JUnit 覆盖非法数量不触发授权、正式报工、记录本或工序池。 | 数量/损耗主关系不再进入正式报工；但 `lossQuantity` 与工废/料废/其他废品合计、损耗原因必填和来源校验仍需后续修复。 |

### AC-M11 修复进展：数量/损耗边界切片

- `20260805-ac-m11-production-quantity-validation` 已完成代码级修复：服务端提交校验新增 `PRO_FRONTLINE_FEEDBACK_QUANTITY_INVALID`，并在拆分前拒绝负数产出、负数损耗、损耗大于产出。
- `MesProFrontlineFeedbackPayloadSplitter` 不再使用 `.max(BigDecimal.ZERO)` 截断合格数量；合法场景仍按 `qualifiedQuantity = outputQuantity - lossQuantity` 写入。
- `MesProFrontlineFeedbackSubmitServiceTest` 新增负向回归，证明非法数量在授权、幂等查询、正式报工、记录本和工序池写入前 fail-fast。
- 本进展只覆盖数量/损耗主边界；AC-M11 仍保留设备参数、结构化原因、签名快照、原始事实不可覆盖、回读追溯等未闭合项。

### AC-M11 第三轮补充缺口

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 14 | 前端生产报工提交契约缺少后端必填的工序池幂等键。 | 后端 `MesProFrontlineFeedbackSubmitReqVO.processPoolSubmissionIdempotencyKey` 标为 `@NotBlank`，`MesProFrontlineFeedbackSubmitServiceImpl.validateSubmitContext` 会缺失即拒绝；但前端 `ProFrontlineFeedbackSubmitReqVO` 接口和 `buildFrontlineFormalSubmitPayload` 返回对象都没有该字段，只给记录本 `idempotencyKey` 默认 `frontline-submit-${signatureId}`。 | 真实生产报工路径存在前后端契约断裂；即使页面填完人员、设备、参数、数量、损耗和签名，也可能因缺后端必填幂等键直接失败，无法证明完整保存。 |
| 15 | 生产电子签名未校验签名主数据、授权状态或签名快照。 | 生产提交 VO 只有 `signatureId`、`signatureEmployeeId`，没有 `signatureSnapshot`；PQC 提交 VO 有 `signatureSnapshot`；`MesFrontlineSubmitAuthorizationServiceImpl` 只校验实际员工等于签名员工、设备/工作站/模板匹配；`MesProcessPoolEventServiceImpl` 只校验签名 ID 为正、签名用户等于实际员工、签名 ID 未重复，未看到签名服务、DCC 授权、图像或有效状态读取。 | 只能拒绝“签名人与实际员工不一致”和“签名 ID 重复”，不能证明签名缺失、过期、未授权、伪造 ID 或签名图像漂移时拒绝并保存原始签名事实。 |
| 16 | 设备参数提交仍是显示名分组和自由 Map，空值可被省略。 | 运行态参数 VO 只有 `parameterCode/name/unit/lowerLimit/upperLimit/defaultValue/valueType`，没有 `required` 标志；前端 `equipmentParameters` 用 `device.label` 作为外层 key，`buildProductionDeviceParameterPayload` 过滤掉 `undefined`；记录本 payload 的 `equipmentParameters` 是自由 `Map<String,Object>`；后端 splitter 只把嵌套参数展开进 rawPayload。 | 参数身份依赖前端显示名，重复设备名、设备改名或空参数都缺少稳定 ID/必填校验；不能证明“参数完整保存，缺必填或参数异常时拒绝”。 |
| 17 | 原始 payload 以客户端提交内容为起点，不是服务端白名单重建。 | `MesProFrontlineFeedbackSubmitServiceImpl` 只要求 `rawPayload` 非空；`MesProFrontlineFeedbackPayloadSplitter.buildProcessPoolRawPayload` 先 `payload.putAll(reqVO.getRawPayload())`，再覆盖 `outputQuantity/lossQuantity/equipmentParameters` 等少数字段。 | 客户端伪造的展示字段、人员/设备文案或额外事实可能进入事件 rawPayload；系统没有证明最终原始事实完全来自服务端认证上下文和正式规则。 |
| 18 | 损耗/不良原因身份不是稳定结构化快照。 | 前端 `configuredDefectReasons` 用 `reason.reasonCode || String(reason.reasonId)` 作为 draft key，提交时只把 `defects: { ...productionDefectDraft }` 放进记录本 `entryContent`；生产报工 VO、记录本 VO 和工序池 BO 均没有 `reasonId/reasonType/reasonName` 等结构化原因字段。 | 原因代码重命名、原因禁用、跨工序原因或同 code 冲突时缺少后端可验证身份；不能证明损耗原因完整保存、缺原因拒绝或历史原因不随配置漂移。 |
| 19 | 实际人员只以用户 ID 参与校验和保存，缺少报工时人员快照。 | `listEmployeeCandidates` 通过工作站岗位取系统用户并过滤启用状态，`requireBoundEmployee` 只按 `candidate.userId == actualEmployeeId` 判断；事件和报工链路保存 `actualEmployeeId/feedbackUserId/signatureUserId`，未见员工姓名、编码、岗位、班组、候选来源或绑定快照进入生产提交事实。 | 后续用户姓名、岗位、班组或启用状态变化后，历史报工难以证明当时人员范围和人员事实；不能完整满足“人员完整保存且历史原始事实不覆盖”。 |

### AC-M11 第四轮补充缺口

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 20 | 一线可选设备和提交授权设备来自不同设备来源。 | 运行态设备列表 `MesFrontlineRuntimeConfigServiceImpl.toDeviceOptions` 读取 `MesProcessPoolTeamProcessDeviceDO.deviceId -> MesProcessPoolTeamDeviceDO.id`；前端 `configuredDeviceCards` 使用 `runtimeConfig.devices[].deviceId` 作为 key；但 `MesFrontlineWorkstationPostRouteBindingSource.putBinding` 使用 `MesDvMachineryDO.id` 生成授权候选设备，`MesFrontlineSubmitAuthorizationServiceImpl` 又要求提交 `deviceId` 等于候选 `process.deviceId`。 | 页面选的是班组本地设备 ID，后端授权比的是工作站台账机械 ID；二者不一致时会误拒绝，或者只有 ID 偶然相同时才通过，不能证明“设备完整保存且设备不可用/不匹配时稳定拒绝”。 |
| 21 | 单一正式设备 ID 与记录本/字段值中的多设备事实不一致。 | `buildFrontlineFormalSubmitPayload` 的 `processPoolContext.deviceId` 只写 `formalContext.deviceId`；但 `equipmentParameters` 和 `buildProductionFieldValues()[DEVICE_PARAMETERS]` 对 `visibleDeviceCards` 全量生成，`buildProductionFieldValues()[DEVICE]` 还把多个设备 label 用 `、` 拼接。 | 正式事件上下文只表示一个设备，记录本和 raw/fieldValues 却可能保存多个设备和多组参数；后续追溯无法明确哪个设备才是本次报工设备，不能满足设备事实一致性。 |
| 22 | 工序池幂等命中旧事件时不比对本次签名、数量和 rawPayload。 | `MesProcessPoolEventServiceImpl.createEvent` 在生产提交时先 `findExistingSubmitEvent`，命中后直接返回旧 `processPoolEventId`；`findExistingSubmitEvent` 只按幂等键、订单、路线工序、工序、实际员工、设备账号、设备、工作站查询，`toSubmitEventResult` 只返回旧 `feedbackId/recordbookEntryId/recordbookEventId/processPoolEventId`。 | 同一幂等键和上下文下，若本次提交换了签名、数量、参数或原因，系统会复用旧事件而不是显式拒绝“幂等键冲突但事实不同”，仍不能证明原始事实不覆盖/不混淆。 |
| 23 | 损耗数量被创建为可用数量分片，FIFO 消耗未按 `OUTPUT` 过滤。 | `MesProcessPoolSubmitEventServiceImpl.buildQuantityFragments` 同时创建 `OUTPUT` 和 `LOSS` 分片，`MesProcessPoolEventServiceImpl.createQuantityFragments` 统一设置 `allocationStatus=AVAILABLE`；`MesTeamLeaderReportConfirmationServiceImpl.persistFifoConsumptionIfRequired` 读取该事件全部分片后直接 `map(this::toAllocatableFragment)`，`toAllocatableFragment` 不保留也不校验 `sourceQuantityType`。 | 损耗分片和产出分片进入同一可分配池，代码层无法证明损耗永远不会进入 FIFO、订单分配或批记录回填；这与“数量、损耗守恒并原因完整”要求冲突。 |
| 24 | 生产执行追溯包没有覆盖参数、原因、原始 payload 和签名快照。 | `MesTeamLeaderTraceServiceImpl.buildSubmitSection` 只放 `processPoolEventId/feedbackId/recordbookEntryId/actualEmployeeId/deviceId/workstationId/signatureId` 等 ID；`closureEvidence` 的 answers 覆盖 `who/device/process/quantity/quality/signature/review/batchRecord`，其中 signature 只列提交/PQC/复核签名 ID，检索未见 `equipmentParameters`、`defects/reason`、`rawPayload`、`signatureSnapshot` 输出。 | 即使底层 event/recordbook 里有部分字段，当前只读追溯接口也不能一站式证明人员、设备、参数、数量、损耗、原因、签名完整保存；验收所需事实仍缺回读证据。 |

### AC-M11 第五轮补充缺口

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 25 | 一线一体提交之外仍有后台创建/更新/提交正式报工通道，未强制设备、参数、原因、签名事实。 | `MesProFeedbackController` 同时暴露 `/create`、`/update`、`/submit` 和 `/approve`；这些接口使用 `MesProFeedbackSaveReqVO`，该 VO 只有工单、任务、工作站、数量、报工人、审批人、备注等字段，没有 `deviceId`、`equipmentParameters`、`rawPayload`、`signatureId/signatureSnapshot` 或结构化损耗原因；`FeedbackForm.vue` 也通过 `createFeedback/updateFeedback/submitFeedback` 走这条旧链路。 | 具备 `mes:pro-feedback:create/update` 权限的后台路径可以生成正式生产报工，但无法满足 AC-M11 对设备、参数、损耗原因和电子签名完整保存的要求；一线链路补强不能代表全系统报工入口合规。 |
| 26 | 第三方/直接报工 Excel 导入会创建或提交正式报工，但不会采集 AC-M11 所需完整事实。 | `MesProFeedbackImportRecordServiceImpl.attributeImportRecordAllocation` 构造 `MesProFeedbackSaveReqVO` 后调用 `createFeedbackWithScheduleSnapshot`，并把 `laborScrap/materialScrap/otherScrap` 置为 0；`ThirdPartyFeedbackImportServiceImpl.importDirectWorkReportWorkbook` 同样构造 `MesProFeedbackSaveReqVO`、创建正式报工并调用 `submitFeedback(feedbackId, true)`；两条路径均未写入设备参数、签名快照或结构化原因。 | 导入数据可进入正式报工和进度闭环，但不能证明“人员、设备、参数、数量、损耗、原因、签名完整保存”；把损耗字段默认置 0 还可能把源文件未提供的事实误写成业务事实。 |
| 27 | 正式报工草稿可被更新或删除，缺少原始事实版本/修订保护。 | `MesProFeedbackServiceImpl.updateFeedback` 只要求报工处于 `PREPARE`，随后用 `feedbackMapper.updateById(updateObj)` 覆盖字段；`deleteFeedback` 同样只校验 `PREPARE` 后 `deleteById`；该主表没有 raw/original snapshot 或 revision 链。 | 对后台创建或导入生成的草稿正式报工，提交前事实可被覆盖或删除；这与“原始事实不覆盖”的验收要求冲突，尤其是在报工正式化前缺少可审计的第一次事实快照。 |
| 28 | 标准报工详情、列表和导出响应不回显 AC-M11 的完整事实。 | `MesProFeedbackRespVO` 主要包含工作站、工艺路线、工序、工单、任务、产品、数量、报工人、审批人、状态、备注和导入来源字段；未包含设备、设备参数、原始 payload、签名、签名快照、结构化损耗/不良原因；`MesProFeedbackController.get/page/export-excel` 均使用该 VO。 | 即使底层某些扩展表保存了局部事实，标准生产报工页面和导出无法作为验收回读证据；用户看不到也无法核对设备、参数、原因和签名是否完整保存。 |
| 29 | 生产签名没有与具体事实 payload 形成不可拆分绑定。 | `MesProFrontlineFeedbackSubmitRespVO` 只返回 `feedbackId/recordbookEntryId/recordbookEventId/processPoolEventId`；`MesProProcessPoolEventDO` 保存 `rawPayload`、`signatureId`、`signatureUserId`、`signatureSnapshot` 字段但没有 payload hash/digest；生产适配器 `MesProcessPoolSubmitEventServiceImpl.toCreateEventReq` 只设置 `rawPayload/signatureId/signatureUserId`，未设置 `signatureSnapshot`，也未把签名绑定到 canonical payload。 | 现有代码只能证明“某签名 ID 与某事件 ID 同时保存”，不能证明签名时锁定的就是人员、设备、参数、数量、损耗、原因那一组原始事实；后续修订、回填或详情展示时缺少防篡改绑定证据。 |
| 30 | 必填事实容器只校验非空对象，未校验空内容或必要键。 | `MesProFrontlineRecordbookPayloadReqVO.equipmentParameters` 只是 `@NotNull`，不是 `@NotEmpty`；`MesProFrontlineFeedbackSubmitReqVO.rawPayload` 也是 `@NotNull`；`MesProFrontlineFeedbackSubmitServiceImpl.validateSubmitContext` 只检查 `rawPayload == null`；拆分器直接把 `equipmentParameters/rawPayload` 写入记录本和工序池；工序池只要求序列化后的 `rawPayload` 字符串非空。 | 空 `{}` 的设备参数、缺关键字段的 rawPayload 或缺参数明细的记录本内容仍缺后端统一 fail-fast 证明；不能满足“缺必填时拒绝且原始事实完整保存”。 |
| 31 | 现有测试明确把越界设备参数作为“保留而不拒绝”的预期。 | `MesProFrontlineFeedbackRawLimitBypassTest.shouldPreserveRawOutOfLimitEquipmentValuesWithoutClippingOrRejecting` 构造 `temperature=10`、`pressure=50` 的 out-of-limit 参数，并断言记录本和工序池事件继续保存这些值；没有对应“越界拒绝/复核提醒”的绿色验收。 | 这直接证明当前代码口径是“越界值原样保存”，而 AC-M11 需要证明参数完整且异常条件可被拒绝或受控处理；当前测试反而锁定了不拒绝行为。 |

### AC-M11 第六轮补充缺口

| 序号 | 不符合项 | 代码证据 | 影响 |
|---:|---|---|---|
| 32 | 生产组长确认报工只兜底校验数量、PQC 绑定和分配，不校验 AC-M11 完整事实。 | `MesTeamLeaderReportConfirmationServiceImpl.confirmSubmission` 读取事件后只调用 `extractSubmittedQuantity`、`validatePqcQualityGate`、`validateAndPrepareLines` 和 FIFO/分配逻辑；`validateEvent` 只要求路线工序和工序存在；测试 `MesTeamLeaderReportConfirmationServiceTest.event` 构造的生产提交事件只有 `eventType/workOrderId/routeId/routeProcessId/processId/actualEmployeeId/rawPayload/serverSubmitTime`，缺 `deviceId/workstationId/signatureId/signatureSnapshot/equipmentParameters/reason` 仍能进入确认成功用例。 | 下游“确认/分配成功”不能反推生产报工事实完整；一个只含 `outputQuantity` 的事件也可能通过组长确认链路，无法满足验收要求。 |
| 33 | 组长确认数量只读取客户端 rawPayload 的 `outputQuantity`，未与正式报工主表或数量分片交叉校验。 | `extractSubmittedQuantity` 只解析 `event.getRawPayload()` 中的 `outputQuantity`，确认和分配都以该值为 `submittedQuantity`；未见与 `MesProFeedbackDO.feedbackQuantity`、`MesProProcessPoolQuantityFragmentDO.totalQuantity` 或记录本原始条目进行一致性校验。 | 如果 rawPayload 被客户端伪造、修订覆盖或与正式报工/分片不一致，组长确认可能按错误数量分配和闭环，不能证明“数量、损耗、原始事实不覆盖”。 |
| 34 | 批记录自动回填只按已配置字段映射取值，不承担 AC-M11 完整性门禁。 | `MesTeamLeaderBatchRecordBackfillServiceImpl.backfill` 只要求存在启用的 cell-link rules，然后逐条 `toChange`；`sourceValue` 只在某条规则要求的 `sourceFieldCode` 缺失时失败。若规则未配置设备参数、损耗原因、签名快照或人员快照字段，则回填仍可只写入数量/压力等局部字段。 | 正式批记录回填成功不能证明所有报工事实已保存和回读；验收需要的人员、设备、参数、数量、损耗、原因、签名可能缺项但不阻塞。 |
| 35 | 批记录回填读取的是当前事件 rawPayload，不读取首次记录本事件或修订前 payload。 | `MesTeamLeaderBatchRecordBackfillServiceImpl.sourceValue` 通过 `sourceEventMap` 取事件，再由 `rawPayload(sourceEvent)` 解析当前 `event.rawPayload`；没有使用记录本 `ENTRY_CREATE` 事件快照、工序池 revision 的 `beforePayload` 或首次提交 payload hash。 | 一旦事件主表 rawPayload 被修订服务更新，后续批记录回填会使用补正后的当前值而非首次原始事实；“原始事实不覆盖”仍缺 downstream 证明。 |

### AC-M11 当前复核结论（2026-08-05）

当前结论：`仍不符合 / 尚未达到 ACCEPTED`。本轮当前代码已修掉部分旧缺口，但仍不能完整证明“人员、设备、参数、数量、损耗、原因、签名完整保存，且缺必填、设备不可用、签名不一致时拒绝且原始事实不覆盖”。

| 状态 | 复核项 | 当前代码证据 | 判断 |
|---|---|---|---|
| 已局部补齐 | 前端一线提交契约补入工序池幂等键。 | 前端 `ProFrontlineFeedbackSubmitReqVO` 已包含 `processPoolSubmissionIdempotencyKey`；`FrontlineFixedTemplatePanel.vue.buildFrontlineFormalSubmitPayload` 会写入该字段。 | 第三轮第 14 项的“前端缺后端必填幂等键”已不再成立。 |
| 已局部补齐 | 损耗数量边界已有后端 fail-fast。 | `MesProFrontlineFeedbackSubmitServiceImpl.validateProductionQuantity` 已拒绝 `outputQuantity <= 0`、`lossQuantity < 0`、`lossQuantity > outputQuantity`。 | 第一/第二轮中“损耗大于产出被截断为 0”的核心风险已降低。 |
| 已局部补齐 | 损耗原因已有结构化 ID 和快照校验。 | `MesFrontlineLossReasonValidatorImpl.requireEnabledLossReason` 在 `lossQuantity > 0` 时要求 `lossReasonId`，并校验原因启用、类型为 `LOSS`、归属当前 `routeProcessId`；拆分器写入 `lossReasonId/code/name` 快照。 | 第三轮中“损耗原因完全无结构化身份”的结论已有改善。 |
| 仍不符合 | 生产签名仍缺提交时签名快照和 payload 绑定。 | 生产 `MesProFrontlineFeedbackSubmitReqVO` 仍只有 `signatureId/signatureEmployeeId/rawPayload`，没有 `signatureSnapshot`；`MesProcessPoolSubmitEventServiceImpl.toCreateEventReq` 未设置 `signatureSnapshot`，也无 payload hash/digest。 | 不能证明签名时锁定了人员、设备、参数、数量、损耗、原因这一组原始事实。 |
| 仍不符合 | 设备参数仍是自由 Map，缺后端规则级校验。 | `MesProFrontlineRecordbookPayloadReqVO.equipmentParameters` 仍为 `Map<String,Object>` 且只 `@NotNull`；前端仍按设备 label 分组并过滤 `undefined` 参数；未见后端按参数编码、类型、单位、上下限、必填逐项校验。 | 不能证明“参数完整保存、缺必填或越界时拒绝”。 |
| 仍不符合 | rawPayload 仍以客户端内容为起点。 | `MesProFrontlineFeedbackPayloadSplitter.buildProcessPoolRawPayload` 仍先 `payload.putAll(reqVO.getRawPayload())`，再覆盖少量服务端字段。 | 客户端伪造字段仍可能进入原始事实，缺少服务端白名单重建和签名前 canonical payload。 |
| 仍不符合 | 后台创建/更新/导入正式报工仍绕过一线完整事实链路。 | `MesProFeedbackController` 仍暴露 `/create`、`/update`、`/submit`、直接导入接口；`FeedbackForm.vue` 仍走 `createFeedback/updateFeedback/submitFeedback`；这些 VO/表单没有设备参数和生产签名快照。 | 全系统“生产报工”不能只按一线一体提交判断合规。 |
| 仍不符合 | 原始事实仍有覆盖链路。 | 记录本 `createEntry` 仍创建 `ENTRY_STATUS_DRAFT`，`saveDraft` 可覆盖草稿内容；工序池修订 `updateOriginalRecord` 仍用 `afterPayload` 更新事件主表 `rawPayload`。 | 不能证明原始事实不覆盖，只能证明部分 revision/diff 可追溯。 |
| 仍不符合 | 下游确认/批记录回填不兜底校验完整事实。 | 组长确认只从 `rawPayload.outputQuantity` 提取数量并做 PQC/分配校验；批记录回填只按 cell-link rule 读取当前 event `rawPayload` 中配置过的字段。 | 组长确认、批记录回填成功不能反推 AC-M11 完整事实已保存。 |
## AC-D03 手动不良说明专项核验

### 核验结论

| 核验项 | 当前判断 | 代码/证据依据 | 缺口 |
|---|---|---|---|
| 系统是否支持手动输入 | 页面只读预检通过，写入未验收 | `FrontlineFixedTemplatePanel.vue` 已新增 `data-pqc-defect-description` 手动文本框，`pqcDraft.defectDescription` 保存草稿；不合格或损耗时 `validatePqcDefectDescription()` 阻塞空说明提交。2026-08-05 Playwright 登录本机 `芋道源码/admin` 打开 `/mes/pro/feedback/edhr-batch-pqc-fill`，PQC 面板和手动输入控件可见；输入 `AC-D03只读预检手动输入-未提交` 后 value 可回读，且 `/pqc/submit` 写请求数为 0。 | 仍需任务自有测试数据的写入型真实 E2E，证明 PQC 检验员可按真实路径提交不合格说明。 |
| 是否保存原始输入快照 | 代码级支持 | `FrontlinePqcInspectionSubmitReqVO`、`MesFrontlinePqcSubmitReqVO`、`MesFrontlinePqcSubmitCommand` 均新增 `nonconformanceDescription`；前端 rawPayload.pqcDraft 保存 `defectDescription`；后端 `buildPqcInspectionEventRawPayload` 写入标准化 `nonconformanceDescription`。 | 仍需真实数据回读 event/PQC record rawPayload，证明页面详情能读到该字段。 |
| 是否能追溯到订单/工序/PQC 记录 | 代码级支持较完整 | 后端 rawPayload 同时保留 `workOrderId`、`routeId`、`routeProcessId`、`processId`、`pqcTaskId`、`regulationVersionId`、`pieceDetailCount` 和 `pqcItemDetails`；新增 JUnit 断言不良说明与 `workOrderId/routeProcessId/pqcTaskId` 同 payload。 | 仍需专项验收把“手动不良说明”与同一订单、工序、PQC event/detail 页面一起回读证明。 |
| 历史记录是否不会被后续修改覆盖 | 部分支持 | PQC record 的 `rawPayload` 在创建时写入，当前检索未发现更新该字段的服务路径；退回补正有 revision/diff 表记录 `beforePayload`、`afterPayload`、字段前后值和修订签名。 | event 表 `rawPayload` 会在修订服务中被更新为 `afterPayload`，时间线详情的 `originalPayloadJson` 读取的是当前 event `raw_payload`；因此现状更接近“有修订链可追溯”，不是严格的“原始详情永不被覆盖”。 |

### 结论口径

- `AC-D03` 不能按旧口径继续要求“生产班组长/PQC 组长维护不良原因主数据”；旧的 defect reason 目录能力不应作为新版 `AC-D03` 符合证据。
- 新口径应改为：PQC 在发现不良时手动录入不良说明/原因，系统保存原始手输文本快照，并可按订单、工序、PQC event/record 追溯。
- 当前系统已具备“手动不良说明字段 + 失败必填 + 进入 rawPayload + 订单/工序/PQC task 追溯身份”的代码级能力，并有静态合同、后端 JUnit、运行 Jar 字段检查和真实页面只读输入证据。
- 当前准确状态应调整为：`代码级已补，仍未完整验收`；缺少真实页面 E2E、详情回显和原始/修订不覆盖验收前，仍不能提升为 `ACCEPTED`。

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
| 8 | AC-D03 新口径 / AC-D28 | PQC 提交结构“不合格原因/说明”代码级字段已补，页面只读输入已证明，剩余为写入验收缺口。 | `MesFrontlinePqcSubmitReqVO`、`MesFrontlinePqcSubmitCommand` 和前端 `FrontlinePqcInspectionSubmitReqVO` 已新增 `nonconformanceDescription`；前端 rawPayload.pqcDraft 记录 `defectDescription`；后端 rawPayload 写入标准化说明；真实页面只读预检证明 `data-pqc-defect-description` 可见且可输入。 | 仍需真实 PQC 页面提交、PQC 组长详情/时间线回读和历史修订不覆盖证明，才能把 AC-D03 提升到 `ACCEPTED`。 |
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
