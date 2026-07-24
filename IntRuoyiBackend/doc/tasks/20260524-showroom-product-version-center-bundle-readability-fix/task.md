# 任务：修复产品版本中心无可读 bundle 报错

## Goal

- 定位并处理 `展厅 -> 产品 -> 版本中心` 点击后报错 `SHOWROOM_VERSION_CENTER_NOT_READY: no readable version bundle exists` 的问题。
- 明确这是代码回归还是本地运行库缺少 `showroom_version_bundle` / backfill 前置条件。
- 若属于代码缺陷，则在不引入 fallback 的前提下完成最小修复并补齐回归测试；若属于数据前置条件缺失，则失败快报并给出精确阻塞说明。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\release\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\release\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\**`

## Non-Scope

- 不顺手改 `yudao-ui-admin-vue3` 前端布局、路由或 toast 行为，除非定位证明问题根因在前端合同消费。
- 不用 mock 数据、兜底 bundle 或静默跳过缺失 revision 来掩盖历史版本不可读问题。
- 不直接改测试服/正式服环境。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-prompt-template-garbled-text-fix\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，不阻塞本次版本中心报错排查与修复。

## Milestones

- [x] M1：建立任务文档与执行日志，记录 BDD 场景、预期验证与前序任务状态。
- [x] M2：复现产品版本中心报错，确认目标产品、接口路径和本地 runtime 日志。
- [x] M3：核查 `showroom_version_bundle` / 发布历史 / backfill 状态，区分代码缺陷与数据阻塞。
- [x] M4：若属于代码缺陷，先补 RED 回归测试，再做最小修复并跑 GREEN。
- [x] M5：更新任务文档、执行日志和最终验证结论，明确剩余风险或阻塞。

## Expected Verification

- 真实接口复现：
  - `POST http://127.0.0.1:48081/admin-api/system/auth/login` with `tenant-id=1`, valid admin account
  - `GET http://127.0.0.1:48081/admin-api/showroom/version-center/history?targetType=PRODUCT&targetId=<id>`
- 定向后端测试：
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 若涉及 SQL / backfill 合同：
  - `python -m pytest script/tests/test_showroom_version_center_sql.py -q`

## Current Status

- Completed on 2026-05-24.
- 已完成代码修复：
  - 已为产品直发、产品 Excel 导入复用发布链路、产品审批发布、产品封面批任务补齐 `showroom_version_bundle` 落库；
  - 已把 0 bundle 场景下的版本中心错误改为显式带出缺失 revision 列表，而不再只返回泛化的 `no readable version bundle exists`。
- 已完成验证：
  - `ShowroomVersionCenterServiceTest` -> PASS
  - `ShowroomHttpApiIntegrationTest#publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval` -> PASS
  - `ShowroomProductCoverBatchTaskServiceTest` -> PASS
  - 本地 `48081` 已重启到新 jar：`D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-runtime-control-20260524-165527.jar`
  - 已执行 `sql/showroom/20260523_showroom_version_center_backfill.sql`，当前运行库 `showroom_version_bundle` 计数为 `COMPANY=2 / PRODUCT=471`
- 已完成运行库 fallback 数据回填：
  - 按用户明确许可，已用 revision `2551` 的现有 preview / 中英文 narration 为 `1181, 1233, 1234, 1256, 1326, 1331, 1343, 1346, 1377, 2367, 2549` 补造历史媒体与 bundle；
  - 所有 fallback 数据均使用 `creator/updater = fallback-version-center-backfill-20260524` 标记；
  - 已提供回滚脚本：
    `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\scripts\rollback-local-product-version-center-fallback-backfill.sql`
- 当前结果：
  - 本地 `product_id=1` 版本中心已恢复可打开；
  - Playwright 真实点击链路已验证可见 `12` 条历史版本，并可切到 `V32`。

## Risks / Blockers

- 本轮历史数据修复是用户明确批准的 fallback 数据方案，只适用于当前本地运行库。
- fallback 来源统一为 revision `2551` 的现有 preview / 中英文 narration，不代表真实历史内容。
- 若后续需要恢复权威历史数据，可先执行回滚脚本删除 `fallback-version-center-backfill-20260524` 标记数据，再按真实历史媒体重建。

## Final Verification Result

- `POST http://127.0.0.1:48081/admin-api/system/auth/login` with `tenant-id=1 / admin / admin123` -> PASS
- `GET http://127.0.0.1:48081/admin-api/showroom/version-center/history?targetType=PRODUCT&targetId=1` before fallback backfill -> FAIL-FAST，返回 `SHOWROOM_VERSION_CENTER_NOT_READY: published revisions missing readable bundle [2549, 2367, 1377, 1346, 1343, 1331, 1326, 1256, 1234, 1233, 1181]`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\scripts\apply-local-product-version-center-fallback-backfill.py` -> PASS
- `GET http://127.0.0.1:48081/admin-api/showroom/version-center/history?targetType=PRODUCT&targetId=1` after fallback backfill -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-product-version-center-fallback open http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fproduct`
  + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\scripts\verify-product-version-center-fallback.mjs` -> PASS，真实登录后点击 `product_001` 的 `版本中心`，页面打开 `12` 条历史版本并成功切换到 `V32`
