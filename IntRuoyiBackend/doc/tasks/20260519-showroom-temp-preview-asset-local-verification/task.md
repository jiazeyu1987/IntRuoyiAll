# 任务：展厅临时预览图资产本地验证

## 目标

在用户已明确批准的前提下，使用当前已生成的展厅截图作为临时预览图资产，完成 `showroom_preview_asset_version` 的真实发布落库，并让前台 `/showroom/display/home` 的 `previewImageUrl` 不再为空，作为本地验证用途。

## 范围

- 仅用于本地验证，不作为长期业务方案。
- 使用当前生成的展厅截图作为临时预览图资产来源。
- 通过真实文件服务或等价正式数据路径，使 preview 资产可被前台读取。
- 验证 `/showroom/display/home` 的 `hallEntries.previewImageUrl` 不再为空。

## 非目标

- 不重做前台页面。
- 不修改前台路由。
- 不引入 mock 或静默降级。
- 不把临时验证资产当成最终生产素材。

## 前置确认

- 用户已经批准“仅用于本地验证的临时方案”。
- 前台路由与三端壳子已完成收敛并通过 reviewer 把关。
- 当前 blocker 已定位为运行库缺少 `showroom_preview_asset_version` 与图片文件。

## 里程碑

- [x] M1: 创建任务记录并确认临时方案范围。
- [x] M2: 将临时截图上传到正式文件存储并获取 file id。
- [x] M3: 创建并发布 preview asset live 记录。
- [x] M4: 验证 `/showroom/display/home` 的 `previewImageUrl` 已非空。
- [x] M5: 更新执行日志并完成收尾。

## 预期验证

- `showroom_preview_asset_version` 中存在 live 记录。
- `showroom/display/home` 返回的 hallEntries 带有非空 `previewImageUrl`。
- 前台首页图片墙不再全部显示“未发布预览图”。

## 当前状态

Completed.

## 验证结果

- PASS: 通过真实登录调用 `/admin-api/infra/file/upload` 上传临时截图，得到 `infra_file.id = 2272`
- PASS: 本地运行库已写入 `8` 条 `target_type = HALL` 的 `PUBLISHED` preview asset 记录
- PASS: 发现并修复了运行时旧 jar 与 `previewImageUrl` 私有文件地址问题
- PASS: `showroom_narration_version` 本地运行库缺失的 `voice` 列已补齐，避免前台讲解读取在真实验证时继续报 SQL 错
- PASS: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: 重启 48081 后，`GET /showroom/display/home` 返回的 `8` 个 `hallEntries.previewImageUrl` 均为非空公共读取路径

## 本地验证说明

- 当前 `8` 个展厅临时共用同一张已批准复用的截图文件 `02-screen-default-entry.png` 作为本地验证 preview 资产。
- 该方案仅用于本地验证，不应视为最终正式展厅素材方案。

## Cleanup Keep

- `doc/tasks/20260519-showroom-temp-preview-asset-local-verification/task.md`
- `doc/tasks/20260519-showroom-temp-preview-asset-local-verification/execution-log.md`
