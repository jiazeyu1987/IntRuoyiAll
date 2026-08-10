# 活跃订单放行资料生成 V3 多 Agent 开发方案

## 最终版入口

V3 保留为过程方案和审查证据；后续实现以 `v4-final-agent-development-plan.md` 为唯一开发入口。

## V3 目标

V3 将“生产组长双 100% 活跃订单申请生成正式放行资料”拆成 6 个可独立开发、独立验证的子 agent 任务，并由 1 个主 agent 统一冻结共享契约、集成 6 个结果、执行系统级真实数据测试和最终验收。

本方案仍继承 V2 的第一版目标：优先跑通真实业务链路，限制能不增加就不增加；但不能为了跑通而直接改进度、制造签名、使用默认成功、用 `formBindings` / 默认 `MAIN` 替代正式批记录，或绕过生产组长/PQC 组长历史数据。后续自动扫描可直接检索默认 MAIN，用于确认未把默认槽位当正式批记录来源。

本修正版按当前代码现状收敛：前端申请入口、后端申请接口、申请记录、eDHR 批次创建、`submitForApproval` 和生产批记录回填已有基础，A1/A2/A3 不再按从零开发理解；正式开发前由主 agent 在 M0 冻结当前接口字段、响应结构、blocker DTO、生成器端口和 fixture manifest。

## 总体拆分原则

- 6 个子 agent 可以在主 agent 冻结共享契约后并行开发，每个 agent 只依赖共享接口、共享 DTO、共享测试数据契约，不依赖其它子 agent 的未完成实现。
- 每个子 agent 必须有独立验收：单元测试、静态合同、组件测试或局部集成测试必须能证明本 agent 的输出正确。
- 主 agent 不代替子 agent 写业务实现；主 agent 负责契约冻结、范围冲突检查、合并顺序、运行时注入检查、系统级测试和最终放行判断。
- 后端运行时不因为拆成 6 个 agent 而拆成 6 套业务主流程；第一版仍以一个申请编排入口为核心，内部通过正式接口/组件完成来源校验、映射、写入和放行待办创建。
- 子 agent 允许在测试中使用显式 test double 验证本 agent 的调用边界；生产代码禁止 no-op 实现、默认成功实现或缺实现时静默通过。
- 所有写入型 E2E 必须使用测试租户、任务自有测试数据、真实前端路径；API 只能用于最终状态核验或只读辅助检查。

## V3 共享验收标准

- AC-V3-01：活跃订单的生产进度 100% 和检验进度 100% 必须能追溯到生产组长/PQC 组长历史列表和历史表单下的正式数据。
- AC-V3-02：生产组长在活跃订单行点击“申请放行”后，后端重新校验真实来源，而不是只相信前端或列表进度字段。
- AC-V3-03：系统创建或复用当前订单/产品/路线/批号对应的正式 eDHR 批次执行。
- AC-V3-04：正式批记录字段来自一线生产提交、生产历史表单、设备参数、损耗明细和生产组长确认。
- AC-V3-05：正式过程检验单字段来自一线 PQC 提交、PQC 历史表单、QA 文件项目约束、过程检验汇集明细和 PQC 组长复核。
- AC-V3-06：正式损耗单字段来自生产损耗历史、损耗原因、工序、产品、批号和生产组长确认。
- AC-V3-07：填写人、审核人、签名时间、确认时间必须来自真实提交/审核记录，禁止用当前登录人或当前时间默认填充。
- AC-V3-08：资料完成性检查通过后，只创建或复用生产负责人放行待办，不由生产组长申请动作直接放行。
- AC-V3-09：缺少正式来源、正式模板、字段映射、QA 约束、签名、负责人或完成性字段时，申请返回 blocker 且不生成假资料。
- AC-V3-10：重复申请同一订单同一来源快照必须幂等，返回同一批次执行、正式资料和负责人待办。
- AC-V3-11：测试数据不能直接写活跃订单进度字段，必须通过生产/PQC 正式历史数据自然形成双 100%。
- AC-V3-12：最终真实 E2E 必须覆盖生产组长申请、资料生成、生产负责人放行或驳回，并检查批记录、检验单和损耗单已填写完成。

## 主 Agent 职责

### 主 Agent 开发前门禁

主 agent 在任何子 agent 开发前先完成 M0 契约冻结：

- 冻结接口契约：申请接口、列表状态字段、blocker DTO、生成结果 DTO、来源追溯 DTO、状态枚举。
- 冻结代码边界：每个子 agent 的 write scope、测试 scope 和禁止触碰范围。
- 冻结测试数据契约：任务自有测试标识、产品/路线/工序/QA 文件/批记录模板/设备/人员/签名/历史表单的最小组合。
- 冻结验收命令：后端聚焦单测、前端静态合同/类型检查、Playwright 真实 E2E、只读数据库/API 复核。
- 冻结失败规则：缺任何正式链路时 fail fast，不能降级为默认成功。
- 冻结现有系统复用边界：A1 复用现有按钮/API wrapper，A2 复用现有申请接口和 eDHR 放行待办，A3 复用 `MesTeamLeaderBatchRecordBackfillServiceImpl`；只有缺口部分新增实现。

### 主 Agent 集成职责

- 检查 6 个子 agent 的实现是否只修改自己的 write scope。
- 检查共享接口没有被任一子 agent 私自改名、放宽字段、添加 fallback 或吞异常。
- 合并后运行跨 agent 运行时契约复验：接口实现、Spring 注入、构造器参数、调用顺序、前端 API wrapper、权限码、菜单入口和 E2E 脚本入口。
- 执行系统级真实数据测试：先制造正式来源数据，再从生产组长页面点击申请，最后由生产负责人页面完成放行或驳回。
- 审核最终资料：批记录、过程检验单、损耗单均完成，且每个关键字段能回溯到来源记录、人员、签名时间和工序。

### 主 Agent 不做

- 不把子 agent 的单测通过等同于系统完成。
- 不跳过真实页面路径。
- 不用 API-only 生成放行资料。
- 不用空实现补齐缺失的子 agent 结果。
- 不在缺负责人、缺模板、缺 QA 文件、缺签名时继续生成待办。

## 共享接口和数据契约

### 状态枚举

- `NOT_READY`：未达到双 100%，或双 100% 背后的正式来源不完整。
- `READY_TO_APPLY`：生产/PQC 正式来源均可追溯，允许申请。
- `PRECHECKING`：后端执行来源、模板、映射、签名和负责人预检；首版可作为运行时瞬态，不强制持久化。
- `BLOCKED`：缺少正式来源、映射、签名、负责人或完成性字段。
- `GENERATING`：创建/复用批次执行并写正式资料；首版可作为运行时瞬态，不强制持久化。
- `PENDING_RELEASE_APPROVAL`：资料完成并推送生产负责人放行。
- `RELEASED`：生产负责人已放行。
- `REJECTED`：生产负责人已驳回。

### 申请接口契约

- `POST /mes/pro/process-pool/team-leader/active-order/release/apply`
- 请求字段：`activeOrderId`、必填 `idempotencyKey`、可选 `applyRemark`；首版不新增 `clientRequestId` 别名。
- 响应字段：`applicationId`、`activeOrderId`、`workOrderId`、`workOrderCode`、`status`、`statusName`、`batchExecutionId`、`releaseTransactionId`、`releaseApprovalWorkTaskId`、`dossierSummary`、`blockers[]`、`appliedAt`。
- `sourceSnapshotHash` 放在 `dossierSummary.sourceSnapshotHash`；首版不要求新增顶层 `sourceSnapshotHash`。
- `generatedDocuments[]` 不作为首版申请接口前置字段；正式资料对象 ID 由只读复核接口、字段审计、批次执行任务或 fixture manifest 核验，后续需要前端直接展示时再扩展。
- 权限：生产组长申请权限；生产负责人放行仍走现有放行权限。
- 请求幂等：`tenantId + activeOrderId + idempotencyKey`。
- 业务幂等：`tenantId + activeOrderId + workOrderId + routeVersionId + sourceSnapshotHash`，不能使用前端随机值作为唯一业务幂等依据。

### Blocker 契约

每个 blocker 首版沿用当前 VO 字段，并允许补充字段级定位：

- `blockerType`：稳定机器码，例如 `MISSING_BATCH_RECORD_BINDING`、`MISSING_PQC_AGGREGATE_DETAIL`。
- `objectType`：缺失对象类型，例如 `ROUTE_PROCESS`、`QA_ITEM`、`SIGNATURE`、`RESPONSIBLE_USER`。
- `objectId`：能定位的正式对象 ID；确实没有对象时为空但必须说明原因。
- `objectCode`：对象编码或业务单号。
- `reason`：用户可理解的原因。
- `suggestion`：建议处理入口，例如补 QA 文件、补批记录绑定、补签名、补负责人配置。
- 可选 `routeProcessId/processId`：相关工序。
- 可选 `fieldCode/cellKey`：相关目标字段；只有能定位到具体字段时才要求返回。

### 来源追溯契约

每个正式资料字段至少能追溯：

- `sourceType`：`PRODUCTION_SUBMIT`、`PRODUCTION_LEADER_CONFIRM`、`PQC_SUBMIT`、`PQC_LEADER_REVIEW`、`PQC_AGGREGATE_DETAIL`、`LOSS_DETAIL`。
- `sourceId`：来源记录 ID。
- `sourceFormId/sourceEventId`：历史表单或工序池事件 ID。
- `routeProcessId/processId`：工序身份。
- `submitUserId/reviewUserId`：填写/审核主体。
- `submitSignatureTime/reviewSignatureTime`：签名/确认时间。
- `sourceSnapshotHash`：申请时来源快照 hash。

## 子 Agent 任务图

所有子 agent 只依赖主 agent 的 M0 共享契约；子 agent 之间不直接互相等待。最终运行时集成由主 agent 完成。

| Agent | 任务 | 可独立开发 | 可独立验证 | 最终集成依赖 |
| --- | --- | --- | --- | --- |
| A1 | 活跃订单前端入口硬化 | 是 | 是，使用冻结 API schema 和前端静态合同 | A2 后端真实接口 |
| A2 | 后端申请编排硬化与生成器集成 | 是 | 是，使用显式 test double 验证生成器端口 | A3/A4/A5 真实生成器 |
| A3 | 复用生产历史到批记录回填能力 | 是 | 是，使用生产历史 fixture 和批记录模板 fixture | A2 调用端口 |
| A4 | PQC 历史到过程检验单映射 | 是 | 是，使用 QA/PQC fixture 和检验模板 fixture | A2 调用端口 |
| A5 | 损耗单与资料完成性检查 | 是 | 是，使用损耗 fixture 和正式资料检查 fixture | A2 放行待办前调用 |
| A6 | 测试数据与真实 E2E | 是 | 是，先验证 fixture 自身可见性 | A1-A5 完成后的系统测试 |

## A1 子 Agent：活跃订单前端入口硬化

### 开发任务

- 复用现有生产组长活跃订单列表“申请放行”按钮、状态标签、行级 loading 和 blocker 入口；仅补缺口，不重复新建入口。
- 对齐当前活跃订单列表字段：`releaseApplicationStatus`、`releaseApplicationBlockerSummary`、`releaseApprovalWorkTaskId`；如需新增 `releaseApplicationId` 或 `batchExecutionId`，必须由 M0 契约统一后再改。
- 复用前端 API wrapper `applyTeamLeaderActiveOrderRelease(data)`，请求使用 `idempotencyKey`。
- 点击前展示确认框，说明系统将基于已填写历史数据生成正式批记录、过程检验单和损耗单，并推送生产负责人放行。
- 申请成功后刷新当前行状态；写请求成功但列表刷新失败时，必须提示“申请已提交，但列表刷新失败”，不能让用户误以为申请失败并重复提交。

### 非范围

- 不生成正式资料。
- 不判断字段级映射是否完整。
- 不替代后端双 100% 校验。
- 不直接跳转生产负责人放行。

### 独立验收任务

- 前端静态合同：按钮文案、点击处理器、API wrapper、`idempotencyKey`、权限码、状态标签和 blocker 展示均存在。
- 组件/页面合同：`READY_TO_APPLY` 时按钮可点击；`NOT_READY/BLOCKED/PENDING_RELEASE_APPROVAL/RELEASED` 时展示对应状态。
- 失败合同：后端返回 blocker 时弹出或展开具体 blocker；写成功刷新失败时不恢复成可重复申请的草稿状态。
- 禁止项扫描：前端不得根据进度字段直接显示“已生成成功”，不得本地构造 `batchExecutionId` 或 `releaseApprovalWorkTaskId`。

### 验收通过标准

- 覆盖 AC-V3-02、AC-V3-09、AC-V3-10 的前端部分。
- A1 只改前端入口、API wrapper 和相关前端测试。
- 主 agent 可用真实后端接口替换测试 payload 后，无需改 A1 业务逻辑。

## A2 子 Agent：后端申请编排硬化与生成器集成

### 开发任务

- 复用并硬化现有申请接口、权限校验、请求校验、事务边界、申请记录和状态流转。
- 基于真实来源快照计算 `sourceSnapshotHash`，同一业务快照重复申请返回既有结果。
- 编排调用 A3/A4/A5 暴露的正式生成端口：批记录生成、过程检验单生成、损耗单生成、完成性检查。
- 创建或复用正式 eDHR 批次执行。
- 编排顺序固定为：来源校验 -> 创建/复用批次执行 -> A3/A4/A5 写正式资料 -> 完成性检查 -> release precheck -> `submitForApproval` 创建或复用生产负责人放行待办。
- 任一来源、模板、签名、负责人或生成端口返回 blocker 时，事务不得留下不完整正式资料。
- 成功路径 `signatureEvidenceCount` 必须大于 0，且来自来源提交/审核签名证据。

### 非范围

- 不实现生产字段映射。
- 不实现 PQC 字段映射。
- 不定义损耗字段细节。
- 不新增复杂审批流。

### 独立验收任务

- 后端单测：双 100% 来源预检通过时按顺序调用三个生成端口和完成性检查。
- 后端单测：生成器返回 blocker 时申请状态为 `BLOCKED`，不创建负责人待办。
- 后端单测：缺 `RELEASE_APPROVE` 负责人时返回负责人 blocker。
- 后端单测：重复申请返回同一申请记录、批次执行 ID 和待办 ID。
- 后端单测：生产组长申请不会直接把状态置为 `RELEASED`。
- 后端单测：申请成功前必须证明 A3/A4/A5 真实 writer 已执行，不能只有资料摘要或 release task。
- 后端静态合同：Controller/VO/前端 TS 统一使用 `idempotencyKey`、`blockerType/reason/suggestion` 和 `dossierSummary.sourceSnapshotHash`。
- 静态合同：生产代码中不存在 no-op 生成器、默认成功生成器、吞异常继续放行逻辑。

### 验收通过标准

- 覆盖 AC-V3-02、AC-V3-03、AC-V3-08、AC-V3-09、AC-V3-10。
- A2 可在 A3/A4/A5 未完成时通过显式 test double 完成独立验证；集成时必须替换为真实实现，缺实现即编译或启动失败。

## A3 子 Agent：复用生产历史到正式批记录回填能力

### 开发任务

- 复用 `MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess(...)` 作为生产历史到正式批记录的首选回填能力。
- 按 `activeOrderId -> workOrderId -> routeProcessId/processId -> batchRecordReportId` 定位工序正式批记录表单绑定，不重复定义第二套批记录来源规则。
- 读取一线生产提交、生产历史表单、设备、设备参数、数量、损耗明细和生产组长确认。
- 校验生产数据符合批记录表单必填字段、设备选择和设备参数要求。
- 将填写人设置为一线生产提交人，审核人设置为生产组长确认人。
- 将填写时间设置为一线生产提交签名时间，审核时间设置为生产组长确认签名时间。
- 写入正式批次执行中的正式批记录表单，并记录字段来源追溯。
- 将 A3 结果接入 A2 申请编排；申请成功路径必须能从批次执行或字段审计中只读复核回填结果。

### 非范围

- 不读取 PQC 汇集。
- 不创建生产负责人待办。
- 不用 `formBindings`、默认 `MAIN` 或工序开始配置补批记录表单。

### 独立验收任务

- 后端单测：存在生产提交、历史表单、正式批记录绑定、设备参数和组长确认时，生成正式批记录。
- 后端单测：缺正式批记录绑定时返回 `MISSING_BATCH_RECORD_BINDING` blocker。
- 后端单测：缺设备必填参数时返回字段级 blocker。
- 后端单测：填写人/审核人/签名时间来自来源记录，不来自当前登录人或当前时间。
- 后端单测：来源追溯包含生产提交事件 ID、历史表单 ID、生产组长确认 ID 和 source hash。
- 后端集成测试：A2 申请成功时调用现有批记录回填服务或等价正式 writer，并产生可复核字段审计。

### 验收通过标准

- 覆盖 AC-V3-04、AC-V3-07、AC-V3-09。
- A3 输出的批记录生成结果可被 A2 汇总到同一个批次执行。

## A4 子 Agent：PQC 历史到正式过程检验单映射

### 开发任务

- 按产品、路线工序和 QA 文件定位检验项目、方法、标准、上下限、设备要求和判定规则。
- 读取一线 PQC 提交、PQC 历史表单、逐件明细、过程检验汇集明细和 PQC 组长复核。
- 校验 PQC 数据符合当前产品/工序 QA 文件约束。
- 将填写人设置为一线 PQC 检验员，审核人设置为 PQC 组长。
- 将填写时间设置为 PQC 提交签名时间，审核时间设置为 PQC 组长复核签名时间。
- 写入正式过程检验单，并记录字段来源追溯。

### 非范围

- 不从 raw payload 直接拼过程检验单。
- 不用状态字段替代结构化汇集明细。
- 不处理生产批记录和损耗单。

### 独立验收任务

- 后端单测：QA 文件项目和 PQC 汇集明细完整时，生成正式过程检验单。
- 后端单测：PQC 任务已复核但缺结构化汇集明细时返回 `MISSING_PQC_AGGREGATE_DETAIL`。
- 后端单测：检验项目超上下限或方法/设备不符合 QA 文件时返回 QA blocker。
- 后端单测：填写人/审核人/签名时间来自 PQC 提交和 PQC 组长复核。
- 后端单测：禁止用 raw payload、默认空项目或状态字段冒充正式检验明细。

### 验收通过标准

- 覆盖 AC-V3-05、AC-V3-07、AC-V3-09。
- A4 输出的过程检验单生成结果可被 A2 汇总到同一个批次执行。

## A5 子 Agent：损耗单与资料完成性检查

### 开发任务

- 从生产报工、损耗字段、损耗原因明细、工序、产品、批号和生产组长确认读取损耗来源。
- 按正式损耗单模板和字段映射写入损耗数量、损耗原因、工序、产品、批号、处理说明、填写人、审核人和签名时间。
- 对无损耗订单，仅在正式模板支持“无损耗确认”口径时生成无损耗确认；否则返回 blocker。
- 实现资料完成性检查：批记录、过程检验单、损耗单必填字段、签名、审核、来源追溯、模板和负责人均完整后，才允许 A2 创建放行待办。

### 非范围

- 不创建活跃订单前端入口。
- 不直接放行。
- 不因为损耗为 0 自动跳过损耗单，除非模板正式支持。

### 独立验收任务

- 后端单测：存在损耗明细时生成正式损耗单，并追溯到生产提交和组长确认。
- 后端单测：损耗为 0 且模板支持无损耗确认时生成确认项。
- 后端单测：损耗为 0 但模板不支持无损耗确认时返回 blocker。
- 后端单测：任一正式资料缺必填字段、签名或来源追溯时，完成性检查失败。
- 后端单测：完成性检查失败时，不允许创建负责人放行待办。

### 验收通过标准

- 覆盖 AC-V3-06、AC-V3-07、AC-V3-08、AC-V3-09。
- A5 输出的完成性结果是 A2 提交生产负责人放行前的最后硬门禁。

## A6 子 Agent：测试数据与真实 E2E

### 开发任务

- 建立任务自有 fixture 场景，例如 `REL-V3-<timestamp>`，制造一个可追溯的产品、路线、工序、QA 文件、批记录表单、设备、人员、签名和活跃订单组合。
- 测试数据必须先走正式来源链路：一线生产提交、生产组长确认、一线 PQC 提交、PQC 组长复核、PQC 汇集、损耗明细和活跃订单汇总。
- fixture 可以优先走真实页面；若页面路径过长，可走正式领域服务/API，但必须写入同一批正式业务表，并在真实页面历史列表和历史表单中验证可见。
- 输出 fixture manifest：`activeOrderId`、`workOrderId`、`productId`、`routeId`、`routeProcessIds`、`batchRecordFormIds`、`qaRegulationVersionId`、`productionSubmitEventIds`、`pqcTaskIds`、`pqcAggregateDetailIds`、`lossDetailIds`、`expectedSubmitUsers`、`expectedReviewUsers`、`expectedSignatureTimes`。
- fixture manifest 还必须输出目标资料对象和证据：`batchRecordExecutionIds`、`processInspectionFormIds`、`lossReportFormIds`、`sourceFormIds`、`sourceEventIds`、`sourceValueHashes`、`signatureEvidenceCount`、`releaseApprovalWorkTaskId`。
- 编写 Playwright 真实 E2E：生产组长登录 -> 活跃订单列表 -> 点击申请放行 -> 查看状态 -> 生产负责人登录 -> 放行或驳回。

### 非范围

- 不实现生产业务逻辑。
- 不直接写活跃订单进度字段。
- 不用 API-only 代替真实页面申请和负责人放行。
- 不使用生产租户、admin 基线数据或不可清理的真实业务记录。

### 独立验收任务

- Fixture 自检：生产组长报工历史列表可见生产提交和生产组长确认。
- Fixture 自检：生产历史表单可见对应工序、设备、参数、填写人、审核人和签名时间。
- Fixture 自检：PQC 组长历史列表可见 PQC 提交、复核和汇集明细。
- Fixture 自检：PQC 历史表单可见 QA 文件要求的项目、结果、判定、填写人、审核人和签名时间。
- Fixture 自检：活跃订单由上述来源自然显示生产进度 100% 和检验进度 100%。
- E2E 自检：脚本入口、测试账号、测试租户、签名配置、前后端运行态和浏览器可执行文件均存在。

### 验收通过标准

- 覆盖 AC-V3-01、AC-V3-11、AC-V3-12。
- A6 的 fixture 是最终集成 E2E 的唯一成功路径数据来源。

## 主 Agent 集成方案

### 集成顺序

1. 契约复核：主 agent 复查 API schema、状态枚举、blocker DTO、来源追溯 DTO、生成器接口和 fixture manifest 是否与 V3 一致。
2. 子 agent 证据复核：逐个检查 A1-A6 的独立验收报告、测试命令、失败修复记录和未解决 blocker。
3. 范围冲突复核：检查各 agent 是否只修改自己的 write scope；共享文件只能由主 agent 契约阶段或明确集成阶段修改。
4. 后端集成：将 A3/A4/A5 的真实 writer 接入 A2 编排服务，运行 Spring 注入、构造器参数、事务和幂等测试。
5. 前端集成：将 A1 的页面入口接入 A2 真实接口，运行 API wrapper、权限码、状态展示和类型检查。
6. Fixture 集成：运行 A6 fixture，先验证历史列表和历史表单可见，再验证活跃订单双 100%。
7. 真实 E2E：Playwright 走生产组长申请、资料生成、生产负责人放行/驳回。
8. 只读复核：用只读 API/DB 查询批次执行、批记录、过程检验单、损耗单、来源追溯、待办和放行状态。
9. writer 执行复核：确认 A3/A4/A5 均写入正式资料，成功路径 `signatureEvidenceCount > 0`，且填写/审核人与签名时间等于 fixture manifest。

### 集成测试用例

| 测试 ID | 场景 | 前置数据 | 操作 | 期望结果 |
| --- | --- | --- | --- | --- |
| IT-V3-01 | Fixture 来源可见性 | A6 生成任务自有 fixture | 分别登录生产组长、PQC 组长查看历史列表和历史表单 | 生产/PQC 历史、表单、签名、审核和工序均可见 |
| IT-V3-02 | 双 100% 申请成功 | IT-V3-01 通过 | 生产组长在活跃订单点击申请放行 | 返回 `PENDING_RELEASE_APPROVAL`，生成批次执行和负责人待办 |
| IT-V3-03 | 正式批记录完成性 | IT-V3-02 通过 | 只读打开正式批记录 | 必填字段、设备参数、填写人、审核人、签名时间完整且可追溯 |
| IT-V3-04 | 正式过程检验单完成性 | IT-V3-02 通过 | 只读打开正式过程检验单 | QA 项目、结果、判定、填写人、审核人、签名时间完整且可追溯 |
| IT-V3-05 | 正式损耗单完成性 | IT-V3-02 通过 | 只读打开正式损耗单 | 损耗数量/原因或无损耗确认、填写人、审核人、签名时间完整且可追溯 |
| IT-V3-06 | 生产负责人放行 | IT-V3-02 通过 | 生产负责人在正式放行入口审批 | 可放行或驳回，生产组长申请不会直接放行 |
| IT-V3-07 | 幂等重复申请 | IT-V3-02 通过 | 生产组长重复点击申请 | 返回同一 applicationId、batchExecutionId、releaseApprovalWorkTaskId |
| IT-V3-08 | 缺批记录绑定 blocker | 移除或构造缺正式批记录绑定的测试路线 | 点击申请放行 | 返回 blocker，不创建假批记录、不创建放行待办 |
| IT-V3-09 | 缺 PQC 汇集 blocker | 构造 PQC 已提交但未汇集明细的测试订单 | 点击申请放行 | 返回 blocker，不用 raw payload 或状态字段冒充过程检验单 |
| IT-V3-10 | 签名来源校验 | 正常 fixture | 比对正式资料字段和 fixture manifest | 填写/审核主体和时间等于来源记录，不等于当前申请时间 |
| IT-V3-11 | 禁止直接进度造数 | 正常 fixture + 代码/SQL 静态检查 | 扫描 fixture 和实现 | 未直接更新活跃订单进度字段制造成功 |
| IT-V3-12 | 禁止 `formBindings` 替代批记录 | 正常 fixture + 静态/运行时复核 | 扫描批记录来源和生成结果 | 批记录来自工序设置正式批记录绑定，不来自 `formBindings` 或默认 `MAIN` |
| IT-V3-13 | 接口契约一致性 | 当前后端 VO + 前端 TS 类型 | 静态扫描请求/响应/blocker 字段 | 统一使用 `idempotencyKey`、`dossierSummary.sourceSnapshotHash`、`blockerType/reason/suggestion` |
| IT-V3-14 | 三类 writer 执行证明 | IT-V3-02 通过 | 只读核验批记录、过程检验单、损耗单目标对象和审计 | A3/A4/A5 均执行，不能只有 release task |
| IT-V3-15 | 签名证据数量 | 正常 fixture | 比对申请摘要和 manifest | `signatureEvidenceCount > 0`，签名主体/时间均来自来源 |

### 推荐验证命令

实际命令以当前 `package.json`、Maven 模块和测试类名称为准；主 agent 必须先确认脚本和测试文件存在，缺失时记录为前置 blocker，不得把命令缺失写成业务失败。

- 后端聚焦测试：`mvn -pl yudao-module-mes/yudao-module-mes-biz -am "-Dtest=MesActiveOrderRelease*Test,MesReleaseDossier*Test" test`
- 后端静态合同：扫描 no-op 生成器、默认成功、吞异常继续放行、直接改进度、`formBindings` / 默认 `MAIN` 替代批记录。
- 前后端契约静态合同：扫描 `idempotencyKey`、`dossierSummary.sourceSnapshotHash`、`blockerType/reason/suggestion`，确认没有重新引入 `clientRequestId` 或首版未实现的 `generatedDocuments[]` 必填依赖。
- 前端类型检查：以 `IntRuoyiFronted/package.json` 实际脚本为准运行 TypeScript/静态合同。
- E2E 入口检查：确认 Playwright spec 文件和测试命令存在。
- 真实 E2E：运行生产组长申请和生产负责人放行路径 spec。
- 空白和编码：`git diff --check -- doc/tasks/20260808-active-order-release-dossier-design`，并用 UTF-8 读取所有 Markdown。

## 测试数据制造设计

### 测试数据最小组合

- 产品：一个任务自有产品，包含稳定产品编码和批号规则。
- 路线：至少 2 个正式工序，包含路线版本和工序设置。
- 批记录表单：每个需映射工序都有正式批记录表单绑定，字段包含设备、设备参数、数量、填写人、审核人、签名时间。
- QA 文件：对应产品和工序有正式 QA 文件/检验规程，项目包含方法、标准、上下限、设备要求和判定规则。
- 设备：生产设备和 PQC 检验设备均符合工序/QA 要求。
- 人员：一线生产、一线 PQC、生产组长、PQC 组长、生产负责人均有测试账号、岗位/责任范围和电子签名配置。
- 生产历史：一线生产提交、生产历史表单、工序池事件、数量片段、生产组长确认。
- PQC 历史：一线 PQC 提交、逐件明细、PQC 历史表单、PQC 组长复核、过程检验汇集明细。
- 损耗历史：至少覆盖一条有损耗样本；另设一条无损耗样本用于模板支持时验证。
- 活跃订单：由上述生产/PQC 历史汇总形成双 100%，不能直接写进度字段。
- 可执行入口清单：M0 必须明确每类测试数据使用页面路径、正式领域 service 或正式 API 的哪一个入口创建，并证明写入同一批正式业务表。

### Fixture 执行顺序

1. 创建或定位任务自有产品、路线、工序、设备和人员。
2. 创建或定位正式 QA 文件和逐工序正式批记录表单绑定。
3. 通过正式页面或正式领域服务写入一线生产提交、设备参数、生产历史表单和生产组长确认。
4. 通过正式页面或正式领域服务写入一线 PQC 提交、逐件明细、PQC 历史表单、PQC 组长复核和过程检验汇集明细。
5. 写入生产损耗明细或正式无损耗确认来源。
6. 等待或触发正式汇总，让活跃订单列表自然显示生产进度 100% 和检验进度 100%。
7. 使用真实页面验证生产组长/PQC 组长历史列表和历史表单均能看到对应来源。
8. 输出 fixture manifest，供主 agent 和只读复核使用。

### Fixture 禁止事项

- 禁止直接 update 活跃订单进度字段。
- 禁止只写 `mes_pro_feedback` 而不写工序池事件、记录本 entry/event、数量片段和组长可见链路。
- 禁止只写 PQC 状态而不写结构化逐件明细和汇集明细。
- 禁止用 admin 视角证明生产组长/PQC 组长历史可见。
- 禁止用 API-only 申请放行替代生产组长页面点击。
- 禁止写入生产租户或不可清理的正式业务数据。

## 交付物和验收证据要求

每个子 agent 完成后必须交付：

- 变更文件清单。
- 独立测试命令和结果。
- 覆盖的 AC-V3 编号。
- 已知 blocker 或明确无 blocker。
- 证明未引入 fallback/默认成功/吞异常的静态或代码证据。
- 若涉及测试数据，必须交付 fixture manifest 和清理策略。

主 agent 最终完成后必须交付：

- 6 个子 agent 的验收汇总。
- 集成冲突和解决记录。
- 后端、前端、fixture、真实 E2E、只读复核证据。
- 生成的正式批次执行 ID、批记录/过程检验单/损耗单对象 ID、负责人待办 ID。
- 每类正式资料完成性检查结果。
- 所有 AC-V3 的覆盖矩阵。

## 最终完成门禁

V3 开发任务只有同时满足以下条件才能算完成：

- A1-A6 均完成独立开发和独立验收。
- 主 agent 完成集成，且不存在 Spring 注入、接口签名、前端 API wrapper、权限码、路由或状态枚举冲突。
- M0 契约冻结已完成，接口字段、blocker DTO、`dossierSummary.sourceSnapshotHash`、生成器端口和 fixture manifest 不再互相冲突。
- fixture 通过真实页面证明生产组长/PQC 组长历史列表和历史表单均可见。
- 生产组长真实页面申请成功生成正式批次执行、正式批记录、正式过程检验单和正式损耗单。
- 生产负责人真实页面收到待办并可放行或驳回。
- 只读复核证明 A3/A4/A5 真实 writer 已执行，成功路径 `signatureEvidenceCount > 0`。
- 缺来源、缺映射、缺签名、缺负责人等负向场景均返回 blocker，且不生成假资料。
- UTF-8、空白检查、后端聚焦测试、前端静态/类型检查和真实 E2E 均有证据。

## V3 相比 V2 的优化

- V2 说明“做什么”和“如何第一版少限制跑通”，V3 进一步说明“谁来做、怎么并行做、各自如何验收、最后如何集成验收”。
- V3 的拆分是开发协作拆分，不是运行时过度拆服务；后端首版仍保持一个申请编排入口。
- V3 明确 A6 测试数据为系统级成功路径的唯一来源，防止子 agent 用孤立单测或 API-only 冒充真实放行完成。
- V3 明确主 agent 的最终职责是集成和验收，不允许 6 个 agent 单独通过后跳过系统级真实链路。
