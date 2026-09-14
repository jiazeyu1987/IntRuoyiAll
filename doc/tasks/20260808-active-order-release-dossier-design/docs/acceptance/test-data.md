# 活跃订单放行资料测试数据计划 V2

## Purpose and Scope

本文是 V2 的重点文档：定义如何制造能支撑放行成功路径的测试数据。测试数据不是“直接写一个双 100% 活跃订单”，而是制造一套符合产品、QA 文件、批记录表单、人员签名和历史列表要求的完整业务闭环数据。

## Evidence Reviewed

- 用户确认：双 100% 必须来自生产组长/PQC 组长报工历史和历史表单。
- 用户确认：PQC 数据必须符合产品 QA 文件约束；生产数据必须符合批记录表单约束。
- 用户确认：填写人、审核人、时间必须来自真实提交和审核签名。

## Required Test Data

- 测试租户：
  - 使用任务自有测试租户或已授权测试环境。
  - 禁止使用生产租户真实业务数据。
- 用户与签名：
  - 一线生产账号：负责生产提交和生产表单填写。
  - 生产组长账号：负责确认生产数据和申请放行。
  - 一线 PQC 账号：负责过程检验提交。
  - PQC 组长账号：负责复核 PQC 数据。
  - 生产负责人账号：负责最终放行。
  - 每个需要签名的账号必须有测试签名配置。
- 产品和路线：
  - 一个目标产品。
  - 产品绑定唯一 ACTIVE 工艺路线。
  - 路线包含至少一个生产工序和一个 PQC 检验工序。
  - 工序绑定正式批记录表单。
  - 产品/工序绑定 QA 文件或检验规程。
  - 配置正式损耗单和过程检验单承载。
- 设备和参数：
  - 生产工序所需设备存在并可被一线生产选择。
  - 批记录要求的设备参数有可提交值。
  - PQC 检验项目如要求设备，设备编号和设备归属必须符合 QA 文件。
- 生产历史：
  - 一线生产提交输出数量、设备、设备参数、损耗数量和损耗原因。
  - 生产提交后进入生产组长报工历史。
  - 对应生产历史表单可在生产组长历史表单列表查看。
  - 生产组长确认或审核通过，产生审核人和审核签名时间。
- PQC 历史：
  - 一线 PQC 按 QA 文件提交全部必检项目。
  - 每个项目包含检验方法、标准、实测值、判定和设备信息。
  - PQC 提交后进入 PQC 组长检验历史。
  - 对应 PQC 历史表单可在 PQC 组长历史表单列表查看。
  - PQC 组长复核通过，形成汇集明细和审核签名时间。
- 活跃订单：
  - 活跃订单与生产工单、产品、路线和批号对应。
  - 生产进度由生产历史自然汇总到 100%。
  - 检验进度由 PQC 历史自然汇总到 100%。


## Executable Fixture Contract

测试数据允许两种创建方式，但验收口径一致：

- 页面优先：能通过现有一线生产、生产组长、PQC、PQC 组长页面完成时，优先走真实页面。
- 正式领域服务 fixture：页面链路太长时，可以用任务自有 fixture 调用正式后端 service/API 创建数据；fixture 必须写入与页面提交相同的正式业务表、触发相同汇集/确认逻辑，并在页面历史列表中只读验证可见。
- 禁止方式：不得直接 update 活跃订单进度，不得只写申请表，不得只写 eDHR 批次，不得跳过生产组长/PQC 组长历史列表和历史表单。

fixture 必须先输出 M0 可执行入口清单，明确每类数据通过页面、正式领域 service 或正式 API 的哪一个入口创建；若入口不存在，记录 blocker，不能改用直接 SQL 或孤立结果表。

fixture 必须输出并记录以下 ID：

- `testPrefix`
- `tenantId`
- `productId`
- `routeId` / `routeVersionId`
- `workOrderId` / `workOrderCode`
- `activeOrderId`
- `productionSubmitEventId`
- `productionHistoryFormId`
- `productionLeaderConfirmId`
- `pqcTaskId`
- `pqcSubmissionId`
- `pqcHistoryFormId`
- `pqcLeaderReviewId`
- `pqcAggregateDetailIds`
- `lossSourceIds`
- `applicationId`
- `batchExecutionId`
- `releaseTransactionId`
- `releaseApprovalWorkTaskId`
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

执行 fixture 后必须先做页面只读断言：

- 生产组长报工历史能按 `testPrefix` 找到记录。
- 生产组长历史表单列表能打开目标生产表单。
- PQC 组长检验历史能按 `testPrefix` 找到记录。
- PQC 组长历史表单列表能打开目标 PQC 表单。
- 活跃订单列表显示生产进度 100% 和检验进度 100%。
- 成功申请后只读核验三类正式 writer 均执行：批记录、过程检验单、损耗单均有目标对象 ID 和字段/来源证据。
- 成功申请后 `signatureEvidenceCount > 0`，且填写人、审核人、签名时间等于 fixture manifest。

## Test Data Creation Procedure

1. 创建任务前缀：`E2E-AO-REL-<日期>-<序号>`。
2. 选择或创建目标产品，并确认 QA 文件和批记录表单已配置。
3. 创建任务自有生产工单和活跃订单，并记录 `workOrderId`、`workOrderCode`、`activeOrderId`。
4. 用一线生产账号提交生产数据：
   - 选择正确工序。
   - 选择符合工序要求的设备。
   - 填写批记录表单要求的设备参数。
   - 填写生产数量、损耗数量和损耗原因。
   - 完成一线生产签名。
5. 用生产组长账号确认生产数据：
   - 在报工历史找到目标记录。
   - 在历史表单列表打开目标表单。
   - 审核/确认通过并签名。
6. 用一线 PQC 账号提交过程检验：
   - 选择同一活跃订单和工序。
   - 按 QA 文件填写全部检验项目。
   - 选择 QA 要求的设备或确认无需设备。
   - 填写实测值和判定。
   - 完成一线 PQC 签名。
7. 用 PQC 组长账号复核：
   - 在检验历史找到目标记录。
   - 在历史表单列表打开目标表单。
   - 复核通过并签名。
   - 确认汇集明细已生成。
8. 只读检查历史列表和活跃订单：
   - 生产组长报工历史命中目标记录。
   - 生产组长历史表单列表可打开目标表单。
   - PQC 组长检验历史命中目标记录。
   - PQC 组长历史表单列表可打开目标表单。
   - 活跃订单生产进度为 100%。
   - 活跃订单检验进度为 100%。
   - 两个进度均能追溯到上述历史数据。
9. 生产组长申请放行。
10. 生产负责人放行。

## Mapping Verification Data

- 批记录映射核验：
  - `sourceSubmitUserId == batchRecord.fillerUserId`
  - `productionLeaderConfirmUserId == batchRecord.reviewerUserId`
  - `sourceSubmitSignedAt == batchRecord.filledAt`
  - `productionLeaderConfirmedAt == batchRecord.reviewedAt`
  - 设备和设备参数值一致。
- 过程检验单映射核验：
  - `pqcInspectorUserId == inspectionForm.fillerUserId`
  - `pqcLeaderReviewUserId == inspectionForm.reviewerUserId`
  - QA 文件要求项目全部存在。
  - 实测值、标准、判定一致。
- 损耗单映射核验：
  - 损耗数量合计一致。
  - 损耗原因一致。
  - 工序、产品、批号一致。
  - 填写/审核人员和时间来自生产历史。
- writer 执行核验：
  - 批记录 writer 产生 `batchRecordExecutionIds` 或字段审计。
  - 过程检验 writer 产生 `processInspectionFormIds` 或正式检验单记录。
  - 损耗 writer 产生 `lossReportFormIds` 或正式损耗单记录。
  - 不能只用 `releaseApprovalWorkTaskId` 证明资料已生成。

## Negative Test Data

- 缺生产历史表单：验证双 100% 来源 blocker。
- 缺 PQC 汇集明细：验证过程检验 blocker。
- PQC 缺 QA 必填项目：验证 QA 约束 blocker。
- 生产缺设备参数：验证批记录约束 blocker。
- 缺生产组长确认签名：验证审核签名 blocker。
- 缺生产负责人配置：验证待办 blocker。

## Reset Procedure

- 所有测试数据带任务前缀。
- 清理只处理任务自有生产工单、活跃订单、申请记录和可安全撤销的测试数据。
- 正式签名、审计和已放行记录如不可删除，应记录保留清单，不硬删。
- 清理后只读确认无 active 待办和无未闭环测试活跃订单残留。

## Data Ownership

- 共享产品、路线、QA 文件和模板只读复用，除非任务明确创建专用副本。
- 生产/PQC 历史数据归当前测试任务所有。
- 签名密码只通过环境变量或安全配置提供，不写入日志。
- 生成资料和放行审计保留可追溯 ID。

## Test Blockers

- TB-01 无法创建或定位符合产品约束的 QA 文件和批记录表单。
- TB-02 无法创建生产/PQC 历史数据并进入对应历史列表和历史表单列表。
- TB-03 无法获取签名配置。
- TB-04 无法安全清理任务自有数据。
- TB-05 无法证明双 100% 来源，不得执行成功路径验收。
