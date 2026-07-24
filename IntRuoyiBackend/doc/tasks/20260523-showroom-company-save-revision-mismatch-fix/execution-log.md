# Execution Log

## Bug Summary

- 保存公司信息时，后端抛出 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch`，导致保存失败。

## Expected Behavior

- 当 live 公司讲解仍指向旧 revision 的同一公司讲解版本时，保存公司基础信息不应因该历史 source revision 绑定而失败。

## BDD Scenarios

- BDD: company save keeps valid live narration version across revision rotation -> Given 公司当前 revision 已切换且 live 公司中文讲解仍指向同一公司的旧 revision 版本 When 管理端保存公司信息 Then 保存成功并保留现有 live 公司中文讲解版本

## TDD Evidence

- Reproduction: 发布一版带 live 公司中英讲解的公司信息后，再直接保存公司新 revision，不重新生成讲解；随后 live 公司讲解的 `sourceRevisionId` 仍指向旧 revision，继续触发 `SHOWROOM_TARGET_NOT_FOUND: live company ZH narration source revision mismatch`。
- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision test` -> FAIL, 新公司 revision 已发布，但 live 公司讲解 `sourceRevisionId` 仍停留在旧 revision，断言未通过。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision test` -> PASS
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test` -> PASS (`62` tests)

## Root Cause

- `publishCompany` 只创建并发布了新的公司 revision，没有同步迁移当前 live 公司讲解版本。
- 后续读取链路仍会把 live 公司讲解当作“必须属于当前 company revision”的资产来校验，因此旧 `sourceRevisionId` 被判定为 mismatch 并直接报错。

## Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#companyPublishShouldCarryForwardLiveNarrationsWhenSavingNewRevision test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test`

## Risk and Regression Scope

- 本次修复仅影响 `ShowroomApiRuntime.publishCompany` 的公司发布链路。
- 修复会在公司保存发布时，把当前 live 的中英文公司讲解复制为新 revision 的已发布版本，并复用原有脚本、音频、时长和 voice。
- 未修改产品发布链路、preview asset 逻辑、审批流或 release 组装逻辑。

## Blockers

- 无。
