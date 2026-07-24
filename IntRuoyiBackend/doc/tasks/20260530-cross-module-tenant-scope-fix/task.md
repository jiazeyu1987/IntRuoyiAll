# 任务：修复跨模块租户与 scope 边界风险

## 任务目标

修复跨模块租户与 scope 只读审计确认的问题，重点覆盖展厅模块和 DCC 模块，避免公开或弱鉴权入口绕过租户、站点或受控文件权限边界。

## 前置任务检查

- `20260530-cross-module-tenant-scope-audit` 已完成并提交，作为本轮修复输入。
- `20260530-dcc-delete-parent-folder` 已标记 Blocked；本轮不继续该任务，不混入目录删除功能改动。
- 当前工作区存在与本任务无关的 deploy 脚本和展厅图片未提交改动；本轮不回退、不暂存这些文件。

## BDD 场景

- BDD: 展厅版本中心按租户读取版本包 -> Given 两个租户存在相同 targetType/targetId/revisionId 的版本包 / When 当前租户查询版本中心历史或 mapper / Then 只能读到当前租户版本包。
- BDD: 展厅 legacy display 详情入口不得匿名公开 -> Given 匿名用户访问旧 `/showroom/display/hall/{id}` 或 `/showroom/display/narration` / When 请求没有 siteKey/stage / Then 旧详情入口不再以 `PermitAll` 暴露，公开展厅内容必须使用带 scope 的发布接口。
- BDD: DCC OnlyOffice 预览 token 绑定租户 -> Given 租户 1 生成 OnlyOffice token / When 租户上下文切换到租户 2 或匿名 OnlyOffice 服务读取 / Then 租户 2 上下文校验失败，匿名读取必须恢复到 token 内的租户上下文，不能漂移读取其他租户同 id 文件。
- BDD: DCC 受控文件响应不暴露 infra 公共下载 URL -> Given 用户查询受控文件详情或列表 / When 返回文件预览元数据 / Then 不返回可绕过 DCC 权限的 `/admin-api/infra/file/.../get/...` URL。

## 里程碑

- [x] M1：创建修复任务记录和 BDD 场景。
- [x] M2：补充 RED 测试覆盖展厅和 DCC 边界。
- [x] M3：修复展厅版本中心租户过滤和 legacy display scope。
- [x] M4：修复 DCC OnlyOffice token 租户绑定和受控文件 URL 暴露。
- [x] M5：运行 targeted regression，记录 GREEN 证据并提交。

## 预期验证

- targeted 展厅测试覆盖版本中心租户过滤与 display scope。
- targeted DCC 测试覆盖 OnlyOffice token 租户绑定与 URL 暴露。
- 不修改无关 deploy 脚本、图片产物或已阻塞的 DCC 删除父文件夹任务。

## Current Status

completed; targeted regression passed. Full `ShowroomHttpApiIntegrationTest` class still has unrelated existing workflow fixture failures and is not part of this boundary fix gate.

## Final Verification

- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccOnlyOfficePreviewTokenServiceTest,DccControlledFileQueryServiceTest,DccControlledFileUploadApiTest" test` -> PASS, 37 tests.
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomSchemaMapperContractTest#versionBundleMapperShouldScopeReadsByCurrentTenant,ShowroomHttpApiIntegrationTest#websiteConfigShouldBePublicWhileLegacyWebsiteDetailEndpointsAreRetired,ShowroomAppConfigCompanyFieldsContractTest#websiteConfigEndpointShouldRemainPublicWhileLegacyWebsiteDetailEndpointsAreRetired" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 targeted tests.
- GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 14 tests.
