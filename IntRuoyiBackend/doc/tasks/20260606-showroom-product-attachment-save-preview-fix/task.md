# 任务：展厅产品附件详情 URL 响应补齐

## 任务目标

为展厅产品基础附件详情响应补齐正式文件访问 URL，使前端基础信息弹框在上传后、保存后和再次打开时都能展示并点击查看图片、视频、文本附件。

## Previous Task Check

- 已检查同仓库前序任务：`doc/tasks/20260606-showroom-product-management-e2e/task.md`。
- 处理：该任务已标记 `blocked`，阻塞原因是基础弹框打开时缺公司信息；本任务只修复附件响应 URL 与前端保存/查看回归，不处理公司信息前置条件。

## BDD 场景

- BDD: 产品附件详情返回文件 URL -> Given 产品 revision 包含图片、视频或文本附件 / When 后台查询产品详情 / Then 每个附件响应都包含 `/admin-api/infra/file/{configId}/get/{path}` 形式的正式文件 URL。
- BDD: 缺附件文件直接失败 -> Given 产品 revision 引用的附件 fileId 不存在 / When 后台查询产品详情 / Then 后端直接返回文件不存在错误，不伪造空 URL。

## 里程碑

- [x] M1：检查前置任务状态并创建任务文档。
- [x] M2：新增 RED 后端回归测试。
- [x] M3：实现附件响应 URL。
- [x] M4：运行目标测试并记录证据。

## 预期验证

- `mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeProductMaterialMatrixTest test`
- `git diff --check -- yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom doc/tasks/20260606-showroom-product-attachment-save-preview-fix`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺文件时沿用 `fileUrl` 的 fail-fast 行为。
- `是否从根因和长期维护角度解决`：是。附件详情响应直接携带正式文件 URL，前端无需猜测或拼接。
- `是否存在临时补丁或绕过`：否。不改变受保护文件配置，不使用 mock URL 作为运行逻辑。

## Current Status

completed

## 进展记录

- 2026-06-06：已确认后端 RED，`ProductAttachmentRespVO` 当前不包含 `url`，详情附件无法提供前端可点击预览地址。
- 2026-06-06：已实现详情附件 URL 响应，缺失 `fileId` 对应文件时沿用 `SHOWROOM_TARGET_NOT_FOUND` fail-fast；定向后端测试和 diff 检查已通过。
- 2026-06-06：复核定向后端测试通过，前端真实 Playwright 路径已确认保存后重开详情返回附件 `url`。
