# Frontend Feature Evidence

## Feature

- Feature: DCC 项目代码基础数据页新增“产品建档申请”闭环入口，支持选择已有 MDM 产品或填写新产品信息，提交申请后审批通过生成 DCC 项目代码并绑定 MDM 产品。
- Non-goals: 不新增真实 BPM 编排占位成功，不用 mock 数据替代 MDM 产品列表，不隐藏后端失败原因。

## Acceptance

- Acceptance: 页面有“产品建档申请”按钮和弹窗。
- Acceptance: 弹窗包含 MDM 产品选择、DCC 产品编号、产品中文名、目标项目名称、目标项目代码等字段。
- Acceptance: 前端 API 契约包含创建申请和审批通过接口。
- Acceptance: 提交申请后记录申请 ID，审批按钮只在有申请 ID 后启用。
- Acceptance: API 错误不被空 catch 吞掉，由现有请求/消息机制暴露。

## BDD

- BDD: 页面入口暴露建档申请失败原因 -> Given 用户在 DCC 项目代码基础数据页发起产品建档 / When 后端因重复编码、缺必填或禁用 MDM 产品拒绝请求 / Then 页面必须展示真实失败原因，不吞掉错误或默认成功。
- BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 用户已提交产品建档申请 / When 用户点击审批通过 / Then 前端调用审批接口并刷新项目代码列表。

## RED

- RED: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` -> FAIL, expected reason: 实现前缺少 `package.json` 静态脚本、产品建档 API 类型、页面入口、申请/审批按钮和 MDM 选择字段。

## GREEN

- GREEN: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` -> PASS, exit code 0。
- GREEN: `pnpm ts:check` -> PASS, exit code 0。

## Verification

- Verification: 静态契约测试检查 `package.json` 暴露 `e2e:dcc:project-code-product-onboarding:static`。
- Verification: 静态契约测试检查 `projectCodes.ts` 包含 `DccProductOnboardingCreateReqVO`、`DccProductOnboardingRespVO`、`createProductOnboardingRequest`、`approveProductOnboardingRequest` 和后端 URL。
- Verification: 静态契约测试检查 `ProjectCodeTabPanel.vue` 包含 `data-testid="dcc-product-onboarding-open"`、`submit`、`approve`、`getProductSimpleList`、`productMasterId`、`dccProductCode`、`productNameCn` 和提示文案。
- Verification: TypeScript relaxed project check 通过，确认新增 API 类型和 Vue 代码可编译。

## Blockers

- Blockers: 未执行真实浏览器写入型 E2E；缺少已确认的本机前后端运行态、登录态、测试租户/账号和可清理的任务自有 MDM/DCC 测试数据。
