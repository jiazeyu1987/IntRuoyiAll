# 任务：跨模块租户与 scope 风险只读审计

## 任务目标

只读检查系统其他模块是否存在类似跨租户、跨站点、无 scope 读取或全局 key 绕过隔离的问题，重点审计展厅模块和 DCC 模块；本轮不修改生产代码。

## 前置任务检查

- `20260530-dcc-delete-parent-folder` 当前存在未提交后端改动，已按项目规则标记 Blocked；本轮审计不继续实现、不提交该任务改动。
- 当前仓库存在多项无关 dirty/untracked 文件；本轮只新增/更新审计任务文档，不回退、不混入源码变更。

## 审计场景

- CHECK: 展厅公开读取 scope -> Given release、document、asset 可被公开访问 / When 搜索无 `siteKey + stage` 或无租户上下文的读取入口 / Then 标记是否可能跨站点或跨租户读取。
- CHECK: DCC 文件与目录 scope -> Given DCC 受控文件、目录、下载、预览、OnlyOffice、迁移任务可访问 / When 搜索仅按 id、fileId、path、objectKey 等全局键读取/删除/下载 / Then 标记是否带租户或权限校验。
- CHECK: 其他模块相似模式 -> Given 后端模块存在公开接口、文件接口、下载接口、上传接口或租户忽略注解 / When 横向扫描相关代码 / Then 输出潜在问题清单和建议，不改代码。

## 里程碑

- [x] M1：创建只读审计任务记录。
- [x] M2：检查展厅模块残余跨 scope 风险。
- [x] M3：检查 DCC 模块跨租户/全局 key 风险。
- [x] M4：横向抽查其他模块类似风险。
- [x] M5：汇总发现、证据和建议。

## 预期验证

- 使用 `rg` 和定向文件阅读形成证据链。
- 区分已修复、未确认、确认风险、需产品决策的问题。
- 不修改生产代码。

## Current Status

completed

## 审计结论

- 展厅公开 release/current、manifest、document、asset 的旧无 scope 入口已改为 fail-fast，未再发现旧 release 读接口可跨租户读取。
- 展厅遗留 display 匿名接口仍存在 scope 边界缺口：`/showroom/display/hall/{hallId}` 与 `/showroom/display/narration` 是 `@PermitAll`，但不带 `siteKey + stage`，依赖当前租户上下文和资源 id 读取。
- 展厅版本中心存在更高风险：`ShowroomVersionBundleDO` 是 `@TenantIgnore`，`ShowroomVersionBundleMapper` 按 `targetType + targetId + revisionId` 查询时未过滤 tenant；`ShowroomVersionCenterController` 对 COMPANY 类型读访问没有额外租户校验。
- DCC 主流程未发现和旧 DMR/DCC 迁移完全相同的直接跨租户读链路；文件列表、详情、预览、下载、培训、分发、NAS 任务均有权限或任务归属检查，NAS 调度按租户执行。
- DCC OnlyOffice 两个匿名文件读取入口使用 `@TenantIgnore`，token payload 只有 `resourceType/resourceId/expiresAt`，不包含 tenantId；属于跨租户边界的潜在风险点。
- infra 文件模块本身是全局文件表与公开下载模型：`FileDO` 为 `@TenantIgnore`，`/infra/file/{configId}/get/**` 为 `@PermitAll + @TenantIgnore`；上层只要暴露文件 URL，就等于 bearer URL 访问，不再有租户校验。

## 建议

- 展厅优先处理：下线或改造 `/showroom/display/*` 遗留匿名接口，使公开读取统一走 `siteKey + stage` scope；版本中心所有 `@TenantIgnore` 表读取补齐 tenant 过滤或在 `scopeResolver.executeInTenant` 内执行并显式校验 target tenant。
- DCC 优先处理：OnlyOffice token payload 增加 tenantId，并在匿名读取端恢复或重建 tenant 上下文后再读取受控文件和 infra 文件。
- infra 文件策略需产品/安全决策：如果 DCC 文件不允许 bearer URL 公共访问，受控文件不应直接暴露 `/infra/file/.../get/...`，应通过 DCC 鉴权下载/预览代理访问。
