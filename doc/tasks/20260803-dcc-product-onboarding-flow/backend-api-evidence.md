# Backend API Evidence

## Scope

- Scope: DCC 产品建档申请后端闭环，覆盖管理后台 API、服务状态机、MDM 产品集成、DCC 项目代码生成、受控文件提交读取 MDM 绑定。
- Owned endpoints: `POST /dcc/product-onboarding-requests/create` and `POST /dcc/product-onboarding-requests/{id}/approve`。
- Owned services: `DccProductOnboardingServiceImpl`, `DccProjectCodeServiceImpl`, `DccControlledFileWorkflowServiceImpl`, `MdmProductApiImpl`。

## Contract

- Create contract: 创建申请只生成 `PENDING_APPROVAL` 申请单，不生成 DCC 项目代码；目标 `projectName + projectCode` 必须未存在且没有待审批申请。
- Approval contract: 审批通过必须校验申请存在且处于待审批，解析启用 MDM 产品或正式创建 MDM 产品，生成启用 DCC 项目代码并写入 `productMasterId`。
- Controlled-file contract: 已绑定 MDM 的 DCC 项目代码提交受控文件时，保存 MDM `productMasterId`、`dccProductCode` 和 `nameCn`；未绑定旧数据继续使用项目代码/项目名，不吞异常。
- Auth contract: 创建使用 `dcc:project-code:create`，审批使用 `dcc:project-code:update`。

## Validation

- 必填校验：目标项目名称、目标项目代码、产品中文名和 DCC 产品编号按服务校验；DCC 产品编号必须符合 14 位字母或数字规则。
- 状态校验：非待审批申请拒绝审批；重复项目代码或重复待审批申请拒绝创建和审批。
- MDM 校验：审批阶段通过 `getEnabledDccProduct` 确认启用 MDM 产品；无效、禁用或缺少 DCC 产品编号时 fail fast。
- Error behavior: 使用明确业务错误码，不返回默认成功，不吞掉 MDM 或项目代码冲突异常。

## BDD

- BDD: 产品建档申请生成待审批单 -> Given 一个产品尚未存在 DCC 项目代码 / When 用户提交包含 MDM 产品信息和目标 DCC 项目代码的建档申请 / Then 系统创建待审批申请 / And 不立即生成正式 DCC 项目代码。
- BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 产品建档申请处于待审批状态 / When 审批人审批通过 / Then 系统创建或绑定启用状态的 MDM 产品 / And 生成启用的 DCC 项目代码 / And DCC 项目代码记录 `productMasterId`。
- BDD: 重复 DCC 项目代码必须拒绝 -> Given DCC 项目代码已存在 / When 用户提交相同目标项目代码的建档申请 / Then 请求被拒绝。
- BDD: 禁用 MDM 产品不能被绑定 -> Given 目标 MDM 产品存在但状态为禁用 / When 用户审批通过建档申请 / Then 审批动作失败并且不生成 DCC 项目代码。
- BDD: 受控文件提交沿用 MDM 产品绑定 -> Given DCC 项目代码已由建档闭环生成并绑定 MDM 产品 / When 用户基于该项目代码提交受控文件 / Then 受控文件保存正式 MDM 来源。

## RED

- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 实现前缺少建档申请服务、审批生成 DCC 项目代码、MDM 创建/启用校验和受控文件 MDM 绑定读取。

## GREEN

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 106, Failures: 0, Errors: 0, Skipped: 0。
- GREEN: `mvn -pl yudao-module-dcc -am "-DskipTests" compile` -> PASS, BUILD SUCCESS。

## Verification

- Contract verification: `DccProductOnboardingServiceImplTest` 覆盖创建待审批、重复项目代码拒绝、审批生成项目代码并绑定 MDM、禁用/无效 MDM 拒绝。
- Integration boundary verification: `DccControlledFileWorkflowServiceImplTest#submitControlledFile_projectCodeWithMdmBindingPersistsMdmProduct` 覆盖受控文件提交保存 MDM `productMasterId`、DCC 产品编号和中文名。
- Observability touchpoints: 审批记录写入申请单状态、审批人、审批时间和生成项目代码 ID；受控文件提交记录 MDM 产品来源字段，可从文件详情响应返回。

## Blockers

- Blockers: 真实写入型 Playwright E2E 未执行，缺少已确认的本机前后端运行态、测试租户/账号和任务自有 MDM/DCC 测试数据；未用 API-only 或 mock 替代。
