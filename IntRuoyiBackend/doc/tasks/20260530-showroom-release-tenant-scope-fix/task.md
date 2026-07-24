# 任务：修复展厅发布租户边界风险

## 任务目标

修复展厅模块审计发现的租户边界问题：不同租户在同一时间发布相同展厅快照时不能因为全局 `release_id` 互相阻塞；公开发布内容读取不能继续依赖无站点上下文的全局公开路径。

## 前置任务检查

- 上一个任务 `20260530-showroom-dcc-tenant-risk-audit` 已完成并提交：`3168a9fe56 任务: 完成展厅DCC租户风险只读审计`。
- 当前仓库存在与本任务无关的未提交改动；本任务不回退、不提交这些改动。

## BDD 场景

- BDD: 不同租户相同快照发布不能互相阻塞 -> Given 租户 1 与租户 122 拥有相同展厅快照 / When 两个租户在同一秒发布到各自站点 scope / Then 两个发布应生成不同 releaseId 并都能成功入库。
- BDD: 公开 manifest 必须带站点 scope -> Given 已发布的 scoped 展厅 release / When 调用旧 `/showroom/release/{releaseId}/manifest` / Then 返回站点选择器缺失错误；When 调用 scoped `/showroom/sites/{siteKey}/stages/{stage}/release/{releaseId}/manifest` / Then 返回该 scope 的 manifest。
- BDD: 公开 asset 必须带站点 scope -> Given 已发布的 scoped 展厅资产 / When 调用旧 `/showroom/assets/{assetId}/{contentHash}` / Then 返回站点选择器缺失错误；When 调用 scoped asset 路径 / Then 只读取该 scope 的资产。

## 里程碑

- [x] M1：建立任务文档并记录 BDD 场景。
- [x] M2：补充失败回归测试，覆盖跨租户 releaseId 与旧公开路径拒绝行为。
- [x] M3：最小实现 releaseId scope 化与旧公开路径收口。
- [x] M4：运行 targeted 回归验证并记录 RED/GREEN。
- [x] M5：部署到测试服并完成公开接口烟测。
- [x] M6：执行收尾清理预览、提交本任务变更。

## 预期验证

- `ShowroomTenantIsolationRegressionTest` 或等价测试证明同秒同快照跨租户发布不会撞 `release_id`。
- release manifest/document/asset 的旧 unscoped controller 路径不再绕过站点 scope。
- scoped 公共接口继续可读取发布内容。
- 不修改 DCC 模块。

## 验证结果

- RED：新增回归测试后，旧公开 manifest/document/asset 路径仍返回 200，且同秒同快照第二个 scoped 发布触发 `uk_showroom_release_id` 重复，符合预期失败。
- GREEN：`mvn -pl yudao-module-showroom -am '-Dtest=ShowroomReleasePublishScopeStateMachineTest,ShowroomReleaseManifestApiTest,ShowroomReleaseAssetApiTest,ShowroomReleaseDocumentApiTest,ShowroomReleaseManifestConditionalRequestTest,ShowroomReleaseAssetConditionalRequestTest,ShowroomReleaseAssetErrorSemanticsTest,ShowroomReleaseAssetFailureJsonTest,ShowroomReleaseGoneSemanticsTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` 通过，16 tests。
- REGRESSION：`mvn -pl yudao-module-showroom -am '-Dtest=ShowroomReleaseManifestQueryServiceTest,ShowroomReleaseDocumentErrorSemanticsTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleaseWebsiteIndexAssemblyTest,ShowroomReleasePurgeServiceTest,ShowroomLegacyWebsiteConfigProjectionApiTest,ShowroomLegacyWebsiteConfigConditionalRequestTest,ShowroomTenantIsolationRegressionTest,ShowroomReleaseCurrentApiTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` 通过，16 tests。
- BUILD：`mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests clean package` 通过，生成 `yudao-server/target/yudao-server.jar`。
- DEPLOY：先运行 `script/deploy/publish-int-ruoyi.ps1 -Environment test -SkipDatabaseSync -SkipMinioSync`，本地 jar、前端和 website 构建均成功；Docker 构建阶段因 Docker Hub `maven:3.9.9-eclipse-temurin-21` 元数据拉取超时失败。随后在测试服基于当前后端镜像构建 jar-only 新镜像 `intruoyi-backend:20260530_showroom_release_scope_fix_1735` 并重启 backend，未同步数据库或 MinIO。
- TEST SERVER：`http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`；scoped `current`、manifest、document、asset 均返回 200；旧 manifest、document、asset 路径均返回 400。

## Cleanup Keep

- `doc/tasks/20260530-showroom-release-tenant-scope-fix/bug-regression-evidence.md`
- `doc/tasks/20260530-showroom-release-tenant-scope-fix/backend-api-evidence.md`
- `doc/tasks/20260530-showroom-release-tenant-scope-fix/database-schema-evidence.md`
- `doc/tasks/20260530-showroom-release-tenant-scope-fix/security-review.md`

## Current Status

completed
