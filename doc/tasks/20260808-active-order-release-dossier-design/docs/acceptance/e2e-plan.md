# 活跃订单放行资料 E2E 计划 V2

## Purpose and Scope

本文定义真实端到端验证。E2E 不允许只造一个双 100% 活跃订单，必须先制造或通过页面形成生产组长/PQC 组长历史数据和历史表单，再申请放行。

## Evidence Reviewed

- PRD V2。
- 测试数据计划 V2。
- 项目 E2E 真实用户路径规则。

## User Paths

- Path-01 准备生产历史：
  - 登录一线生产账号或使用等价正式 fixture。
  - 按目标产品工序提交生产数据、设备、设备参数、数量和损耗。
  - 登录生产组长账号确认生产数据。
  - 断言生产组长报工历史和历史表单列表出现目标记录。
- Path-02 准备 PQC 历史：
  - 登录一线 PQC 账号或使用等价正式 fixture。
  - 按该产品 QA 文件提交过程检验数据。
  - 登录 PQC 组长账号复核通过。
  - 断言 PQC 组长检验历史和历史表单列表出现目标记录。
- Path-03 活跃订单申请：
  - 登录生产组长账号。
  - 进入活跃订单列表。
  - 定位任务自有活跃订单。
  - 断言生产进度和检验进度均为 100%。
  - 点击“申请放行”并确认。
  - 断言状态变为“待负责人放行”。
- Path-04 生产负责人放行：
  - 登录生产负责人账号。
  - 打开放行待办。
  - 查看正式批记录、过程检验单、损耗单。
  - 电子签名放行。
  - 断言放行成功。

## Fixture Path Rules

- 若使用 fixture 造数，fixture 必须调用正式领域 service/API，而不是直接改进度或插入孤立结果。
- fixture 造数后，Playwright 仍必须打开生产组长/PQC 组长历史列表和历史表单确认数据可见。
- 申请放行和生产负责人放行必须通过真实页面执行，不能用 API-only 替代。

## Browser or Client Steps

1. 检查本地前后端运行态和登录账号。
2. 创建任务前缀，例如 `E2E-AO-REL-20260808-001`。
3. 通过正式页面或安全 fixture 创建产品/订单/活跃订单关联。
4. 用一线生产路径生成生产历史和生产历史表单。
5. 用生产组长路径确认生产历史。
6. 用一线 PQC 路径生成检验历史和 PQC 历史表单。
7. 用 PQC 组长路径复核并汇集。
8. 回到生产组长活跃订单列表申请放行。
9. 登录生产负责人处理待办。
10. 用只读 API 核验批次执行、正式表单和放行事务。

## API Verification

API 只用于最终只读核验：

- 活跃订单进度来源是否关联生产/PQC 历史。
- 生产组长报工历史目标记录是否存在。
- PQC 组长检验历史目标记录是否存在。
- 批记录字段审计是否能追溯生产历史。
- 过程检验单字段是否能追溯 PQC 汇集明细。
- 损耗单字段是否能追溯损耗明细。
- A3/A4/A5 writer 是否均产生正式目标对象或字段审计。
- `signatureEvidenceCount` 是否大于 0，且填写人、审核人、签名时间与 manifest 一致。
- 负责人待办是否唯一。
- 放行事务状态是否正确。

## Console and Log Checks

- 目标页面不能出现未处理 JS 异常。
- 目标申请接口不能 500。
- 后端日志不能包含签名密码或 token。
- E2E 证据必须记录测试前缀、账号角色、目标订单、activeOrderId、applicationId、batchExecutionId、releaseTransactionId、workTaskId。
- E2E 证据必须记录 `batchRecordExecutionIds`、`processInspectionFormIds`、`lossReportFormIds`、`signatureEvidenceCount` 和来源签名时间比对结果。

## Test Blockers

- TB-01 无法通过页面或正式 fixture 生成生产/PQC 历史数据。
- TB-02 无法看到生产组长/PQC 组长历史列表和历史表单中的目标记录。
- TB-03 缺目标产品 QA 文件或批记录表单。
- TB-04 缺生产负责人待办入口。
- TB-05 缺签名密码或测试账号。
