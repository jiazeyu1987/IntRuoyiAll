# Execution Log

## Bug Summary

- 公司页面进入或查看公司历史时出现 `SHOWROOM_TARGET_NOT_FOUND: unknown company field development_history_en`。

## Expected Behavior

- 公司历史和审批差异预览在遇到合法的公司英文字段时，应返回稳定标签而不是抛未知字段异常。

## Reproduction

- 合同回归入口：`mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 失败信息：`SHOWROOM_TARGET_NOT_FOUND: unknown company field development_history_en`

## BDD Scenarios

- BDD: company history should render bilingual company revision fields -> Given 公司 revision 历史里包含 `development_history_en` 等合法英文扩展字段 When 后端生成公司历史 diffItems Then 系统必须返回稳定字段标签而不是抛出 unknown company field

## Root Cause

- `ShowroomFieldDisplaySupport.fieldLabel("COMPANY", fieldCode)` 只收录了公司中文主字段，没有覆盖 `development_history_en`、`park_introduction_en` 等合法公司英文字段。
- 公司历史 / 审批差异预览在构造 `diffItems.label` 时会直接调用该映射，因此一旦 revision 审计中出现公司英文字段，就会被误判成 unknown field 并 fail-fast。

## TDD Evidence

- BDD: company history should render bilingual company revision fields -> Given 公司 revision 历史里包含 `development_history_en` 等合法英文扩展字段 When 后端生成公司历史 diffItems Then 系统必须返回稳定字段标签而不是抛出 unknown company field
- RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`ShowroomFieldDisplaySupport.fieldLabel("COMPANY", "development_history_en")` 抛出 `unknown company field`
- GREEN: 最小修复 -> 为公司合法 `_en` 字段补齐中文标签映射，采用 `xxx(英文)` 风格，与产品英文字段标签风格一致
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS (`65` tests)

## Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomCompanyFieldLabelContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Risk and Regression Scope

- 修复只影响 `ShowroomFieldDisplaySupport` 的公司字段中文标签映射。
- 不改变真正未知字段的异常语义；仅把已存在于 schema、持久化与前端合同中的公司 `_en` 字段从误判中剔除。
- 未修改前端页面、工作流状态机或产品字段映射。

## Blockers

- 当前仓库存在其它未提交的 showroom / dcc / infra 在途改动；本次只提交字段标签修复相关文件，不混入其它任务产物。
