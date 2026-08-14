# PRD

## Goal

生产组长对由真实生产/PQC 历史形成双 100% 的活跃订单手动申请放行后，系统原子生成正式批记录、正式过程检验单和正式损耗单，完成性与 release precheck 通过后创建唯一 `RELEASE_APPROVE` 待办，由生产负责人真实页面放行或驳回。

## Scope

- 硬化现有申请接口与前端入口。
- 实现并接入三类正式资料 writer。
- 修正双 100%、来源 hash、签名证据、幂等和事务边界。
- 实现完成性检查。
- 补齐逐工序“一线生产元素 -> 批记录表单单元格/重复行”的对应关系配置；配置阶段只保存对应关系，不生成或回写批记录数据。
- 建立正式 fixture manifest 和真实页面 E2E。
- 集成验证三类资料、审计、签名、待办和最终放行事务。

## Non-Goals

- 不新增平行审批流或第二套负责人配置。
- 不增加必填 `generatedDocuments[]`。
- 不持久化 PRECHECKING/GENERATING 瞬态。
- 不用后台扫描替代手动申请。
- 不把 formBindings、默认 MAIN 或工序开始配置替代逐工序 `MAIN` 批记录；PI template 28 和 LOSS template 25 仅按明确路线绑定作为各自正式目标，不能替代其 PQC/QA 或生产损耗来源。
- 不在批记录单元格链接页面、一线生产提交动作或配置保存动作中生成正式批记录数据；正式数据只在生产组长点击“申请放行”时按最终历史一次性生成。
- 不把重复行数量全局写死为 4 行；重复行数量以当前工序目标表单实际结构和用户选择的重复行组为准。
- 不把配置阶段的中间提交数量差异、复核人时间推导作为 blocker；申请放行时系统基于已完成的正式生产历史生成资料。
- 不在本任务执行 Git commit/push 或操作远程服务器。

## User or System Scenarios

- 真实历史成功生成三类资料并进入负责人待审批。
- 缺正式来源、QA、映射、签名或负责人时无副作用阻塞。
- 同来源快照重复申请返回原对象。
- 来源正式修正后新 hash 重新申请。
- 负责人查看资料并批准或驳回。
- 工艺人员或管理员在批记录单元格链接页面选择某一道工序、一线生产来源元素和该工序批记录表单中的目标单元格；可选择目标重复行组，系统仅保存对应关系。
- 同一工序多次一线生产提交时，申请放行生成资料按提交顺序写入该工序已配置的下一条未使用重复行；若前序提交已经满足该工序完成数量，后续重复行保持空白。

## Functional Requirements

- FR-01 后端重新计算正式双 100%，不信任前端显示值。
- FR-02 BatchRecordWriter 只读逐工序 BATCH 正式绑定并写当前 batch/task execution。
- FR-03 ProcessInspectionWriter 只读 CONFIRMED PQC 汇集和 DCC 项目同工序最新 PUBLISHED QA 版本，并写入路线明确绑定的传统报表或 template 28 FormCenter 正式目标。
- FR-04 LossReportWriter 对账 feedback 与已签名事件正损耗明细，并写入路线明确绑定的传统报表或 template 25 FormCenter 正式目标。
- FR-05 三类 writer 输出 execution、field audit、source value hash、signature evidence 和 blockers。
- FR-06 完成性检查通过后才能 precheck/submitForApproval。
- FR-07 `AO_RELEASE_SOURCE_V1` canonical hash 覆盖来源值、QA、映射、签名和负责人规则。
- FR-08 请求/业务双幂等且并发唯一键不产生重复对象。
- FR-09 生成事务失败全部回滚，确定性 blocker 独立持久化，基础设施异常继续抛出。
- FR-10 前端展示定位 blocker，写成功刷新失败分层处理。
- FR-11 fixture 必须进入生产/PQC 历史列表和历史表单，申请与负责人处理走真实页面。
- FR-12 批记录单元格链接必须以“当前工序”为边界，目标表单只能是该工序已绑定的批记录表单；不同工序的一线生产 UI 分别链接到各自工序的批记录表单。
- FR-13 一线生产来源元素不得只限于数量，必须覆盖该工序 UI 中可提交的正式元素，包括产出/损耗/合格数量、设备选择、设备参数、勾选项、操作人、提交时间及后续明确纳入的一线生产字段。
- FR-14 目标重复行由用户在链接页面按表单实际结构选择，系统保存重复行组和组内目标单元格；每张表单、每道工序可有不同重复行数量，不使用全局固定行数。
- FR-15 链接配置只保存 source-to-cell 规则，不创建 batch execution、FormCenter instance、field audit、签名证据或三类放行资料；生产组长点击“申请放行”时，后端才按最终生产历史和链接规则生成数据。
- FR-16 申请放行生成批记录时，按一线生产提交顺序把每次提交写入下一条未使用重复行；若累计完成数量已满足该工序，剩余重复行不写入。

## Non-Functional Requirements

- NFR-01 无 fallback、silent downgrade、mock success 或异常吞噬。
- NFR-02 所有来源、字段、签名和待办可审计追溯。
- NFR-03 数组排序、decimal、时间和 JSON canonical 结果稳定。
- NFR-04 日志/manifest 不含密码、token、签名口令。
- NFR-05 修改范围遵循现有 Spring/Vue/Element Plus/Playwright 模式。

## Dependencies and Constraints

- V4：`doc/tasks/20260808-active-order-release-dossier-design/v4-final-agent-development-plan.md`
- M0：`doc/tasks/20260809-active-order-release-dossier-m0/m0-contract-freeze.md`
- AC/BDD/TDD/E2E：设计任务 `docs/product` 与 `docs/acceptance`。
- 当前 eDHR batch execution、batch report execution、field audit、PQC aggregate、release transaction/work task。

## Acceptance Criteria

- AC-01 任务自有活跃订单生产 100% 可追溯至生产历史和历史表单。
- AC-02 检验 100% 可追溯至 PQC 历史、历史表单、CONFIRMED task 和 aggregate detail。
- AC-03 生产来源满足逐工序正式批记录绑定、设备、参数、数量、损耗和必填约束。
- AC-04 PQC 来源满足发布 QA 项目、方法、标准、上下限、设备和判定约束。
- AC-05 申请时后端重新校验双 100%。
- AC-06 创建或复用可追溯正式 batch execution。
- AC-07 批记录 execution/field audit 可追溯生产提交、确认、设备参数和签名时间。
- AC-08 过程检验 execution/field audit 可追溯 PQC 汇集、QA 版本、填写/审核签名。
- AC-09 损耗 execution/field audit 可追溯损耗总量、明细、原因、工序和签名。
- AC-10 填写人/审核人/签名时间严格等于来源记录，不用当前人/当前时间替代。
- AC-11 三类资料完成后才创建唯一 RELEASE_APPROVE 待办，申请状态为 PENDING_RELEASE_APPROVAL。
- AC-12 负责人真实页面可查看并批准/驳回，release transaction/event 正确。
- AC-13 缺正式来源、QA、映射、签名、模板或负责人时返回定位 blocker 且无部分资料/待办。
- AC-14 同来源快照重复申请不重复创建 batch、execution、audit、transaction 或 work task。
- AC-15 申请、来源 hash、映射、三 writer 证据、签名和待办均可审计。
- AC-16 批记录单元格链接页面可以在同一工序内把一线生产的数量、设备、参数、勾选项、提交人和提交时间等来源元素映射到该工序批记录表单的目标单元格。
- AC-17 用户保存链接关系后，数据库只新增或更新配置规则；批记录正式数据、审计、签名和放行资料数量不变化。
- AC-18 申请放行时，多次一线提交按顺序落到用户配置的重复行，未被实际提交使用的重复行保持空白；重复行数量来自表单结构和用户配置，不固定为 4。
- AC-19 申请放行前已由生产完成状态保证数量闭环；系统不得因配置阶段看不到最终数量一致性或复核人时间推导而阻塞对应关系配置。
