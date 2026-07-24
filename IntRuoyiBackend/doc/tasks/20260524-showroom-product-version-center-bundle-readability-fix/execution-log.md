# 执行日志：修复产品版本中心无可读 bundle 报错

BDD: 产品版本中心应在存在可读历史 bundle 时正常打开 -> Given 产品存在已发布 revision 且对应 readable version bundle 完整 / When 用户进入产品版本中心 / Then history 接口必须返回历史列表，而不是抛出 `SHOWROOM_VERSION_CENTER_NOT_READY: no readable version bundle exists`

BDD: 产品版本中心应在发布历史缺少 readable bundle 时失败快报 -> Given 产品存在已发布 revision 但运行库缺少对应 `showroom_version_bundle` 或 backfill 未完成 / When 用户进入产品版本中心 / Then 系统必须明确暴露缺失前置条件与受影响 revision，不得用当前 live 数据、mock 数据或空列表兜底

INFO: 已检查同仓前序任务 `20260524-showroom-prompt-template-garbled-text-fix` 状态为 Completed，可继续本任务。
INFO: 已通过代码搜索定位产品版本中心历史入口与后端抛错点：`ShowroomVersionCenterService.assertPublishedHistoryReadable(...)`。
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest,ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归断言暴露两个真实缺口：1) `publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval` 中新发布产品没有写入 `showroom_version_bundle`；2) `historyShouldReportMissingRevisionIdsWhenNoReadableBundleExists` 中 0 bundle 场景错误消息不包含缺失 revisionId。
INFO: 本地运行库 `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro` 实查 `product_id=1`：已发布 revision 为 `1181/1233/1234/1256/1326/1331/1343/1346/1377/2367/2549/2551`，`showroom_version_bundle` 对应产品记录为 `0`；仅 revision `2551` 具备 `1` 条中文讲解、`1` 条英文讲解、`1` 条 preview asset。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，0 bundle 场景现在会显式返回缺失 revisionId 列表。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，产品直发后会同步写入 `showroom_version_bundle`，bundle 引用的 preview / 中英文 narration 与新 revision 一致。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，产品封面批任务新增 bundle 持久化后未破坏既有后台任务重试/恢复逻辑。
INFO: 仓库自带 `script/deploy/restart-int-ruoyi-local.ps1 -Component backend` 在当前目录下调用 `mvn -pl yudao-server` 时找不到 reactor，已改为手工在 `ruoyi-vue-pro` 根目录执行 `mvn -pl yudao-server -am -DskipTests package`，并将本地 `48081` 重启到 `output/runtime/backend-runtime-control-20260524-165527.jar`。
GREEN: 执行 `sql/showroom/20260523_showroom_version_center_backfill.sql` 到本地 `23306/ruoyi-vue-pro` -> PASS，`showroom_version_bundle` 计数变为 `COMPANY=2 / PRODUCT=471`，其中 `product_id=1` 新增 `revision_id=2551` bundle 1 条。
BLOCKED: 真实 `GET /admin-api/showroom/version-center/history?targetType=PRODUCT&targetId=1` 仍 fail-fast -> `SHOWROOM_VERSION_CENTER_NOT_READY: published revisions missing readable bundle [2549, 2367, 1377, 1346, 1343, 1331, 1326, 1256, 1234, 1233, 1181]`，说明代码回归已解除，但运行库仍缺 11 条旧 published revision 的权威历史媒体。
INFO: 用户已明确批准仅在当前本地运行库内使用任意可用数据补齐历史媒体；本轮 fallback 数据统一以 `creator/updater=fallback-version-center-backfill-20260524` 标记，并提供独立回滚 SQL。
GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\scripts\apply-local-product-version-center-fallback-backfill.py` -> PASS，为 `1181,1233,1234,1256,1326,1331,1343,1346,1377,2367,2549` 各补 1 条 preview、2 条 narration、1 条 bundle，全部指向 template revision `2551` 媒体。
GREEN: 真实 `GET /admin-api/showroom/version-center/history?targetType=PRODUCT&targetId=1` after fallback backfill -> PASS，返回 `12` 条可读历史版本。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-product-version-center-fallback open http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fproduct` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-version-center-bundle-readability-fix\scripts\verify-product-version-center-fallback.mjs` -> PASS，真实登录主租户 `芋道源码 / admin / admin123` 后点击 `product_001` 的 `版本中心`，页面进入 `/showroom/product/version-center/1?revisionId=2549`，历史列表可见 `12` 条版本，点击 `V32` 后无页面级报错。
