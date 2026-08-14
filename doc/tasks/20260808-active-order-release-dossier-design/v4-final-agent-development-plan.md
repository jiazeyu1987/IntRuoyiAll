# 活跃订单放行资料生成最终版开发方案 V4

## 版本定位

V4 是本任务的最终开发入口。V2/V3 作为需求、审查和演进证据保留；后续实现按 V4 执行。V4 的目标不是重新设计一套 MES/eDHR 流程，而是在现有系统上补齐最小缺口，让生产组长在双 100% 活跃订单上手动申请后，系统基于真实历史数据生成正式批记录、正式过程检验单、正式损耗单，并提交生产负责人放行。

## 最终目标

- 生产组长活跃订单显示生产进度 100% 且检验进度 100%。
- 双 100% 必须能追溯到生产组长/PQC 组长历史列表和历史表单下的正式数据。
- 生产组长点击“申请放行”后，后端重新校验来源，不信任前端进度字段。
- 后端创建或复用正式 eDHR 批次执行。
- 后端写入三类正式资料：批记录、过程检验单、损耗单。
- 填写人、审核人、签名时间、确认时间来自真实提交/审核记录。
- 资料完成性通过后，只创建或复用生产负责人 `RELEASE_APPROVE` 待办，不直接放行。
- 生产负责人在正式放行入口完成放行或驳回。

## 最小可交付边界

### 必须实现

- 申请接口成功路径真实调用三类资料 writer。
- 批记录 writer 复用现有 `MesTeamLeaderBatchRecordBackfillServiceImpl` 或等价正式 writer。
- 过程检验单 writer 从 PQC 汇集明细和 QA 文件约束写正式过程检验单。
- 损耗单 writer 从生产损耗历史和损耗原因写正式损耗单。
- 完成性检查在放行待办创建前执行。
- 真实 fixture 能通过历史列表、历史表单、活跃订单、正式资料和负责人待办验证全链路。

### 首版不做

- 不新增平行审批流。
- 不新增第二套生产负责人配置；优先复用 `RELEASE_APPROVE`。
- 不新增必填 `generatedDocuments[]` 响应字段。
- 不强制持久化 `PRECHECKING` / `GENERATING` 瞬态状态。
- 不默认新增字段映射快照表；只有现有字段审计、PQC 汇集、损耗来源和操作审计不足时再评估。
- 不用自动后台扫描替代生产组长手动申请。

## 不可妥协约束

- 禁止直接更新活跃订单进度制造双 100%。
- 禁止伪造签名、用当前登录人或当前时间填正式资料签名字段。
- 禁止用 `formBindings`、默认 `MAIN`、工序开始配置或空值替代逐工序正式批记录绑定。
- 禁止用 raw payload 或状态字段冒充正式过程检验单。
- 禁止只有负责人待办而没有三类正式资料 writer 执行证据。
- 禁止资料不完整时创建负责人放行待办。
- 禁止 API-only 代替生产组长申请和生产负责人放行真实页面路径。

## 当前系统复用清单

| 能力 | 当前状态 | V4 使用方式 |
| --- | --- | --- |
| 生产组长申请按钮/API wrapper | 已存在 | A1 只做硬化和契约对齐 |
| `POST /active-order/release/apply` | 已存在 | A2 复用并补三类 writer 编排 |
| 申请记录和幂等 | 已存在基础 | A2 保持 `idempotencyKey` + source snapshot 双幂等 |
| eDHR 批次创建 | 已存在 | A2 复用 open/create 能力 |
| `submitForApproval` | 已存在 | A2 完成性通过后调用 |
| 生产批记录回填 | 已有 `MesTeamLeaderBatchRecordBackfillServiceImpl` | A3 接入申请编排，不重写第二套 |
| PQC 汇集明细 | 已存在 aggregate detail | A4 从结构化汇集写正式过程检验单 |
| 损耗来源字段 | 生产报工已有损耗字段 | A5 写正式损耗单并做完成性检查 |

## M0 契约冻结

M0 必须先完成，未通过不得启动 6 个子 agent 并行开发。

### M0-01 接口契约

- 请求：`activeOrderId`、必填 `idempotencyKey`、可选 `applyRemark`。
- 响应：`applicationId`、`activeOrderId`、`workOrderId`、`workOrderCode`、`status`、`statusName`、`batchExecutionId`、`releaseTransactionId`、`releaseApprovalWorkTaskId`、`dossierSummary`、`blockers[]`、`appliedAt`。
- `sourceSnapshotHash` 位于 `dossierSummary.sourceSnapshotHash`。
- blocker 首版字段：`blockerType`、`objectType`、`objectId`、`objectCode`、`reason`、`suggestion`，可选 `routeProcessId`、`processId`、`fieldCode`、`cellKey`。
- 不新增 `clientRequestId`。
- 不把 `generatedDocuments[]` 作为首版必填响应。

### M0-02 writer 端口契约

A2 只依赖三个最小 writer 边界，生产代码不得用 no-op 实现：

- `BatchRecordWriter`：输入活跃订单、批次执行、生产来源，输出批记录 execution/审计证据。
- `ProcessInspectionWriter`：输入活跃订单、批次执行、PQC 汇集明细、QA 文件版本，输出正式过程检验单对象/审计证据。
- `LossReportWriter`：输入活跃订单、批次执行、损耗来源和损耗模板，输出正式损耗单对象/审计证据。

writer 可以是接口、内部组件或现有服务适配器；不得为拆分而新增大型抽象层。

### M0-03 fixture manifest 契约

成功 fixture 必须输出：

- `testPrefix`
- `tenantId`
- `productId`
- `routeId` / `routeVersionId`
- `workOrderId` / `workOrderCode`
- `activeOrderId`
- `productionSubmitEventIds`
- `productionHistoryFormIds`
- `productionLeaderConfirmIds`
- `pqcTaskIds`
- `pqcSubmissionIds`
- `pqcHistoryFormIds`
- `pqcLeaderReviewIds`
- `pqcAggregateDetailIds`
- `lossSourceIds`
- `batchRecordExecutionIds`
- `processInspectionFormIds`
- `lossReportFormIds`
- `sourceFormIds`
- `sourceEventIds`
- `sourceValueHashes`
- `expectedFillerUserIds`
- `expectedReviewerUserIds`
- `expectedSignatureTimes`
- `signatureEvidenceCount`
- `applicationId`
- `batchExecutionId`
- `releaseTransactionId`
- `releaseApprovalWorkTaskId`

### M0-04 运行时顺序契约

A2 申请编排顺序固定为：

1. 校验申请人是活跃订单生产组长。
2. 读取活跃订单、工单、产品、路线、工序快照。
3. 校验生产/PQC 历史来源形成双 100%。
4. 生成 source snapshot hash。
5. 创建或复用 eDHR 批次执行。
6. 调用 A3 批记录 writer。
7. 调用 A4 过程检验单 writer。
8. 调用 A5 损耗单 writer。
9. 执行完成性检查。
10. 执行 release precheck。
11. 调用 `submitForApproval` 创建或复用生产负责人待办。

## 6 个子 Agent 最终拆分

| Agent | 最终任务 | 主要交付 | 独立验收 |
| --- | --- | --- | --- |
| A1 | 前端入口硬化 | 复用按钮/API wrapper，修状态和 blocker 展示 | 静态合同 + 类型检查 |
| A2 | 申请编排硬化 | 接入 A3/A4/A5 writer，修幂等和事务顺序 | JUnit + 静态合同 |
| A3 | 批记录回填复用 | 复用现有批记录回填并接入 A2 | 单测 + 字段审计复核 |
| A4 | 过程检验单 writer | QA/PQC 汇集到正式过程检验单 | 单测 + QA 约束负测 |
| A5 | 损耗单 writer 与完成性 | 损耗来源到正式损耗单，完成性门禁 | 单测 + 完成性负测 |
| A6 | 测试数据与真实 E2E | fixture manifest，真实页面路径 | fixture 自检 + Playwright |

## 子 Agent 详细任务

### A1 前端入口硬化

- 复用现有“申请放行”按钮和 `applyTeamLeaderActiveOrderRelease`。
- 确认请求只提交 `activeOrderId`、`idempotencyKey`、`applyRemark`。
- 确认 blocker 使用 `blockerType/reason/suggestion` 展示。
- 写成功但刷新失败时提示“申请已提交，但列表刷新失败”。
- 不在前端本地生成 `batchExecutionId`、`releaseApprovalWorkTaskId` 或资料成功状态。

### A2 申请编排硬化

- 保留现有申请接口和权限码。
- 复用当前申请记录、请求幂等和业务幂等。
- 在创建负责人待办前真实调用 A3/A4/A5。
- 任一 writer 返回 blocker 或抛正式缺失错误时，不创建负责人待办。
- 成功路径 `signatureEvidenceCount > 0`。
- 生产组长申请只进入 `PENDING_RELEASE_APPROVAL`，不得写成 `RELEASED`。

### A3 批记录回填复用

- 以 `MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess(...)` 为首选实现。
- 不新建第二套批记录绑定读取规则。
- 批记录来源只能来自逐工序正式批记录绑定。
- 填写人、审核人、签名时间来自生产提交和生产组长确认。
- 输出批记录 execution ID 或字段审计证据。

### A4 过程检验单 writer

- 读取 PQC 提交、PQC 历史表单、PQC 组长复核、PQC 汇集明细。
- 按产品/工序 QA 文件校验项目、方法、上下限、设备要求和判定规则。
- 写正式过程检验单，不能用 raw payload 或状态字段代替。
- 填写人是 PQC 检验员，审核人是 PQC 组长。
- 缺 QA 文件、缺汇集明细、缺检验项目或超限判定不一致时返回 blocker。

### A5 损耗单 writer 与完成性检查

- 从生产提交、损耗明细、损耗原因、工序、产品、批号和生产组长确认读取来源。
- 写正式损耗单或正式无损耗确认。
- 无损耗订单只有模板正式支持无损耗确认时才成功。
- 完成性检查覆盖三类资料必填字段、签名、审核、来源追溯、模板和负责人。
- A5 不创建负责人待办，只向 A2 返回完成性结果。

### A6 测试数据与真实 E2E

- 先输出 M0 可执行入口清单，说明每类数据走页面、正式领域 service 还是正式 API。
- fixture 不得直接 update 活跃订单进度。
- fixture 造数后必须登录生产组长/PQC 组长页面只读验证历史列表和历史表单可见。
- 生产组长申请和生产负责人放行必须走真实页面。
- 最终只读核验三类资料对象、字段审计、签名证据和放行事务。

## 正式资料映射矩阵

| 资料 | 来源 | 目标 | 必须验证 |
| --- | --- | --- | --- |
| 批记录 | 生产提交、生产历史表单、设备参数、生产组长确认 | 批记录 execution / 字段审计 | 设备、参数、数量、填写人、审核人、签名时间 |
| 过程检验单 | PQC 提交、PQC 历史表单、QA 文件、PQC 汇集、PQC 组长复核 | 正式过程检验单 | QA 项目、实测值、判定、填写人、审核人、签名时间 |
| 损耗单 | 生产损耗明细、损耗原因、工序、产品、批号、生产组长确认 | 正式损耗单 | 损耗数量、原因、无损耗确认、填写人、审核人、签名时间 |

## 测试数据最终方案

### 成功数据

1. 创建或定位任务自有产品。
2. 创建或定位 ACTIVE 路线和工序快照。
3. 绑定逐工序正式批记录表单。
4. 配置产品/工序 QA 文件。
5. 配置生产设备、PQC 设备和参数约束。
6. 准备一线生产、生产组长、一线 PQC、PQC 组长、生产负责人账号和签名。
7. 一线生产提交生产数据、设备参数和损耗明细。
8. 生产组长确认生产数据。
9. 一线 PQC 按 QA 文件提交检验数据。
10. PQC 组长复核并形成汇集明细。
11. 活跃订单自然显示双 100%。
12. 生产组长页面申请放行。
13. 生产负责人页面放行或驳回。

### 负向数据

- 缺正式批记录绑定。
- 缺生产历史表单。
- 缺设备必填参数。
- 缺 PQC 汇集明细。
- PQC 数据不符合 QA 文件。
- 缺损耗来源或损耗模板。
- 无损耗但模板不支持无损耗确认。
- 缺生产组长/PQC 组长签名。
- 缺生产负责人配置。
- 重复申请同一来源快照。

## 测试与验收命令

实际命令以仓库现有脚本为准，缺脚本时记录 blocker，不得改用假 PASS。

- 后端静态合同：`node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs`
- 后端聚焦单测：`mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderRelease*Test,MesReleaseDossier*Test" test`
- 前端静态合同：`node tests/e2e/team-leader-active-order-release-application-static.spec.js`
- 前端类型检查：按 `IntRuoyiFronted/package.json` 实际脚本执行。
- 真实 E2E：生产组长申请和生产负责人放行 Playwright spec。
- 文档验证：product/system/acceptance validator、UTF-8 读取、`git diff --check`。

## 最终集成验收矩阵

| ID | 验收项 | 通过条件 |
| --- | --- | --- |
| G-01 | M0 契约冻结 | 接口、DTO、writer、fixture manifest 无冲突 |
| G-02 | 来源可见性 | 生产组长/PQC 组长历史列表和历史表单可见 |
| G-03 | 双 100% 来源 | 活跃订单双 100% 可追溯到历史数据 |
| G-04 | 批记录生成 | 正式批记录字段和签名完整 |
| G-05 | 过程检验单生成 | QA 项目和 PQC 结果完整 |
| G-06 | 损耗单生成 | 损耗或无损耗确认完整 |
| G-07 | writer 证据 | A3/A4/A5 均有目标对象或字段审计 |
| G-08 | 签名证据 | `signatureEvidenceCount > 0` 且等于 manifest 来源 |
| G-09 | 放行待办 | 资料完成后才创建 `RELEASE_APPROVE` 待办 |
| G-10 | 负责人放行 | 生产负责人真实页面可放行或驳回 |
| G-11 | 幂等 | 同一来源快照重复申请返回同一对象 |
| G-12 | 负向 blocker | 缺来源/映射/签名/负责人时不生成假资料 |
| G-13 | 禁止替代来源 | 批记录不来自 `formBindings` 或默认 `MAIN` |
| G-14 | 禁止 API-only | 成功路径包含真实页面申请和负责人处理 |

## 开发顺序

1. M0 主 agent 冻结契约和 fixture manifest。
2. A2 写 RED：证明当前 apply 未调用 A3/A4/A5 writer。
3. A3 接入现有批记录回填，并让 A2 调用。
4. A4 实现过程检验单 writer。
5. A5 实现损耗单 writer 和完成性检查。
6. A2 串联 writer、完成性、release precheck、`submitForApproval`。
7. A1 对齐前端展示和静态合同。
8. A6 建 fixture 并证明历史列表/历史表单可见。
9. 主 agent 集成测试和真实 E2E。
10. 只读复核三类资料、签名、待办和放行状态。

## 最终结论

按 V4 开发可以达到目标，但完成判断只看最终集成证据，不看单个 agent 自称完成。若 A4/A5 writer、A2 writer 编排、fixture 页面可见性或真实 E2E 任一项缺失，本功能不得标记完成。
