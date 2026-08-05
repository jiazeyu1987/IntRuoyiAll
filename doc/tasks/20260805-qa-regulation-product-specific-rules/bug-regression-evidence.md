# Bug Regression Evidence

## Bug Summary

- QA 规程页的检验规则由页面级单例 `qaInspectionTypeRules` 承载，切换 DCC 项目时不会按产品重置或恢复，导致压力泵规则可串入其它产品。
- 压力泵规程和检验项目还由 `projectCode === 'IDI'` 直接选择，没有先落到 DCC 正式绑定的 MDM 产品身份。

## Expected Behavior

- 检验规则、规程草稿字段和检验项目必须以 DCC `productMasterId` 为唯一状态 key。
- 当前 `IDI / 按压式球囊扩充压力泵` 的既有规则只登记给其正式绑定产品。
- 不同产品互不继承规则；同一产品从不同 DCC 项目入口进入时复用同一份草稿。
- DCC 项目缺少 `productMasterId` 时显示空白规则并由既有保存门禁阻塞。

## Reproduction

- Reproduction command: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- RED result: FAIL，首个失败为缺少 `QaProductRuleDraftSnapshot`，旧实现没有产品级规则草稿模型。

## Root Cause

- `applyDccProjectToQaDraft()` 只按 DCC 项目代码决定是否初始化压力泵模板。
- `qaInspectionTypeRules` 在页面生命周期内共享，切换产品时没有保存当前产品状态，也没有为目标产品加载独立状态。
- 规程字段和检验项目虽会重置，但规则数组不会同步重置，形成跨产品状态泄漏。

## Regression Test

- Added: `IntRuoyiFronted/tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- Coverage: 产品 ID keyed `Map`、切换前保存、切换后恢复、嵌套检验类型深拷贝、同产品复用、缺产品绑定清空、禁止项目代码直接选择当前规则。

## RED And GREEN

- RED: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> FAIL, expected reason: missing product-owned rule draft model.
- GREEN: `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS.

## Verification

- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS.
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS.
- `pnpm ts:check` -> PASS.
- Scoped `git diff --check` -> PASS；只有 Git 的 LF/CRLF 工作区提示，没有 whitespace error。

## Risk And Regression Scope

- 变更仅调整 QA 页面内的产品状态归属和切换逻辑，不修改后端保存/发布 API、路由、权限或正式工艺路线来源。
- 产品身份只使用 `productMasterId`；没有增加产品名称推断、默认产品、异常吞掉或备用数据源。

## Blockers

- 无任务自有 blocker。
