# 执行日志：修复展厅发布租户边界风险

- BDD: 不同租户相同快照发布不能互相阻塞 -> Given 租户 1 与租户 122 拥有相同展厅快照 / When 两个租户在同一秒发布到各自站点 scope / Then 两个发布应生成不同 releaseId 并都能成功入库。
- BDD: 公开 manifest 必须带站点 scope -> Given 已发布的 scoped 展厅 release / When 调用旧 `/showroom/release/{releaseId}/manifest` / Then 返回站点选择器缺失错误；When 调用 scoped `/showroom/sites/{siteKey}/stages/{stage}/release/{releaseId}/manifest` / Then 返回该 scope 的 manifest。
- BDD: 公开 asset 必须带站点 scope -> Given 已发布的 scoped 展厅资产 / When 调用旧 `/showroom/assets/{assetId}/{contentHash}` / Then 返回站点选择器缺失错误；When 调用 scoped asset 路径 / Then 只读取该 scope 的资产。
- PRECHECK: 审计任务 `20260530-showroom-dcc-tenant-risk-audit` 已完成并提交 `3168a9fe56`；当前存在 unrelated dirty changes，本任务只处理展厅 release 租户边界。
- RED: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomReleasePublishScopeStateMachineTest,ShowroomReleaseManifestApiTest,ShowroomReleaseAssetApiTest,ShowroomReleaseDocumentApiTest,ShowroomReleaseManifestConditionalRequestTest,ShowroomReleaseAssetConditionalRequestTest,ShowroomReleaseAssetErrorSemanticsTest,ShowroomReleaseAssetFailureJsonTest,ShowroomReleaseGoneSemanticsTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，预期原因：`scopedReleaseIdShouldIncludeSiteStageWhenSnapshotAndTimestampMatch` 第二次发布触发 `uk_showroom_release_id` 重复，旧 manifest/document/asset 路径仍返回 200 而不是 `SHOWROOM_SITE_SELECTOR_REQUIRED`。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomReleasePublishScopeStateMachineTest,ShowroomReleaseManifestApiTest,ShowroomReleaseAssetApiTest,ShowroomReleaseDocumentApiTest,ShowroomReleaseManifestConditionalRequestTest,ShowroomReleaseAssetConditionalRequestTest,ShowroomReleaseAssetErrorSemanticsTest,ShowroomReleaseAssetFailureJsonTest,ShowroomReleaseGoneSemanticsTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，16 tests，0 failures，0 errors。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomReleaseManifestQueryServiceTest,ShowroomReleaseDocumentErrorSemanticsTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleaseWebsiteIndexAssemblyTest,ShowroomReleasePurgeServiceTest,ShowroomLegacyWebsiteConfigProjectionApiTest,ShowroomLegacyWebsiteConfigConditionalRequestTest,ShowroomTenantIsolationRegressionTest,ShowroomReleaseCurrentApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，16 tests，0 failures，0 errors。
- BUILD: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests clean package` -> PASS，生成 `yudao-server/target/yudao-server.jar`。
- DEPLOY: `script/deploy/publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync` -> FAIL，环境原因：Docker Hub `maven:3.9.9-eclipse-temurin-21` 元数据拉取超时；本地 jar、前端和 website 构建均已成功。
- DEPLOY: 测试服远端基于当前 `intruoyi-backend` 镜像构建 jar-only 新镜像 `intruoyi-backend:20260530_showroom_release_scope_fix_1735`，更新 `/opt/intruoyi/runtime/.env` 的 `IMAGE_TAG` 并 `docker compose up -d backend` -> PASS；未同步数据库，未同步 MinIO。
- GREEN: `GET http://172.30.30.58:48081/actuator/health` -> 200，`{"status":"UP"}`。
- GREEN: 测试服 scoped `current` -> 200；scoped manifest -> 200；legacy manifest -> 400。
- GREEN: 测试服 scoped document `.json` -> 200；legacy document `.json` -> 400。
- GREEN: 测试服 scoped asset -> 200；legacy asset -> 400。
- BLOCKED: 新任务 `20260530-dcc-delete-parent-folder` 已被用户要求立即执行 -> 当前任务暂停，避免将展厅 release 租户边界未完成实现混入 DCC 删除父文件夹交付。
