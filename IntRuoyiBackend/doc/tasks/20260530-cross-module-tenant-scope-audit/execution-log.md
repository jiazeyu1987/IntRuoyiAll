# 执行日志：跨模块租户与 scope 风险只读审计

- PRECHECK: `20260530-dcc-delete-parent-folder` 仍为 In progress，已标记 Blocked 以避免和本轮只读审计混入。
- CHECK: 本轮只读审计，不修改展厅、DCC 或其他模块生产代码。
- CHECK: 展厅 release 公开读接口 -> `ShowroomReleaseManifestQueryService#getCurrentResponse/getManifestResponse/getDocumentResponse/getAssetResponse` 的无 scope overload 返回 `SHOWROOM_SITE_SELECTOR_REQUIRED`；带 scope 路径使用 `tenantId + siteKey + stage` 查询 pointer/release/document/asset。
- CHECK: 展厅 legacy display -> `ShowroomDisplayController` 的 `/hall/{hallId}`、`/narration`、`/website-config` 是 `@PermitAll`；`displayWebsiteConfig()` 现在会走 legacy projector 的 `siteSelectorRequired()`，但 hall/narration 仍不带 siteKey/stage。
- CHECK: 展厅内容服务 -> `ShowroomPersistentContentService` 对 company/product/hall 的 `selectById` 后有 `requireTenant`；这一路未发现直接跨租户读取。
- CHECK: 展厅版本中心 -> `ShowroomVersionBundleDO` 使用 `@TenantIgnore`，`ShowroomVersionBundleMapper#selectByTargetAndRevision/selectListByTarget` 未带 tenant 条件；`ShowroomVersionCenterController#requireReadAccess` 对 COMPANY 类型直接返回，风险确认。
- CHECK: DCC DO/SQL -> DCC 业务 DO 多为 `BaseDO`，但未加 `@TenantIgnore`；测试 DDL 有 `tenant_id`，常规 SQL 预期由租户插件加 tenant 条件。
- CHECK: DCC 主文件入口 -> 列表、详情、预览、下载由 `DccControlledFileQueryServiceImpl` 做 category/directory 权限检查；未发现无权限直接读取。
- CHECK: DCC NAS 任务 -> controller 有 submit/directory/category 权限，`requireOwnedTask` 校验 operatorUserId，调度器遍历 tenant 并用 `TenantUtils.execute` 执行。
- CHECK: DCC OnlyOffice -> `DccControlledFileController` 的 upload-preview/controlled-file OnlyOffice 文件读取为 `@TenantIgnore + @PermitAll`；`DccOnlyOfficePreviewTokenService` token 未包含 tenantId，列为潜在跨租户边界风险。
- CHECK: 横向扫描 -> 发现 infra 文件 `FileDO` 为 `@TenantIgnore`，公开下载接口为 `@PermitAll + @TenantIgnore`；其他 pay/sms/auth 类公开入口主要是外部回调或认证路径，未纳入本轮重点风险。
- RESULT: 只读审计完成；未修改生产代码。
