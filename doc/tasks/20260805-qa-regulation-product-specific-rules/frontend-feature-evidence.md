# Frontend Feature Evidence

## Feature Goal

- QA 检验规则跟随 DCC 项目正式绑定的 MDM 产品，不同产品拥有独立规则。
- 将当前压力泵规程、规则和检验项目登记给 `IDI` 正式绑定的 `productMasterId`，而不是让项目代码直接成为规则状态 key。

## Non-Goals

- 不修改 QA 草稿保存、发布、项目配置状态或工艺路线后端接口。
- 不新增产品名称推断、默认产品、mock 数据、localStorage 持久化或兼容 fallback。
- 不把缺少 `productMasterId` 的项目当作任一已有产品。

## Requirements And Acceptance

- AC-QA-PRODUCT-RULE-1：选择不同产品时，页面规则不得互相继承。
- AC-QA-PRODUCT-RULE-2：切回原产品时，恢复该产品在当前页面会话中的规程字段、检验规则和检验项目草稿。
- AC-QA-PRODUCT-RULE-3：两个 DCC 项目绑定同一 `productMasterId` 时，共用同一份产品规则草稿。
- AC-QA-PRODUCT-RULE-4：当前压力泵规则只通过 `IDI` 的正式产品绑定登记；缺产品绑定时清空并阻塞保存。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Regression contract: `IntRuoyiFronted/tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`

## API Contracts And Data States

- DCC 候选继续使用 `getProjectCodePage()`，正式产品身份来自 `DccProjectCodeRespVO.productMasterId`。
- QA 保存 payload 继续使用 `buildQaRegulationSavePayload()` 中的正式 `productId`。
- 新增页面内 `Map<number, QaProductRuleDraftSnapshot>`，保存产品级规程字段、检验规则和检验项目。
- 未配置产品加载显式空白规则模板；压力泵产品加载既有压力泵模板；缺产品绑定时 active product ID 为空且保存继续显示正式阻塞提示。

## BDD Scenarios

- BDD: 压力泵规则只属于正式绑定产品 -> Given `IDI` 正式绑定压力泵产品, When QA 选择该项目, Then 页面按其 `productMasterId` 加载压力泵规则。
- BDD: 不同产品规则互不串用 -> Given 产品 A 的规则已编辑, When 切换到产品 B, Then 产品 B 显示独立空白或既有规则。
- BDD: 切回产品恢复草稿 -> Given 产品 A 已编辑后切换到产品 B, When 再选择产品 A, Then 恢复产品 A 的规程字段、规则和检验项目。
- BDD: 同产品跨项目入口复用 -> Given 两个 DCC 项目绑定同一产品, When 在两个入口间切换, Then 使用同一产品草稿。
- BDD: 缺产品绑定时阻塞 -> Given DCC 项目没有 `productMasterId`, When QA 选择该项目, Then 清空规则且保存被既有门禁阻塞。

## RED And GREEN

- RED: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> FAIL, expected reason: old page has no product-owned draft snapshot or product-keyed cache.
- GREEN: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.

## Responsive Accessibility Loading Empty Error Permission

- 未修改现有响应式布局、Element Plus 控件、路由或权限。
- 保留 DCC 项目候选加载、错误和重试状态。
- 未配置产品使用可编辑的空白规则结构，不显示其它产品数据。
- 缺少正式产品绑定时不隐藏错误、不生成默认成功状态，保存仍显示“未绑定 MDM 产品”提示。

## Verification Path

- 聚焦静态合同覆盖产品切换状态所有权和禁止直接项目代码选规则。
- 三个相邻 QA 静态合同覆盖页面入口、末检适用性和版本发布标题栏未回归。
- `pnpm ts:check` 覆盖 Vue/TypeScript 类型正确性。
- 本任务未启动前后端服务，未将静态合同记录为真实 Playwright E2E。

## Blockers

- 无任务自有 blocker。
