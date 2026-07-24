# 任务：后台展厅产品基础附件管理

## 任务目标

在展厅产品管理的产品列表“基础”弹框中新增附件资料区，支持上传、展示、排序和删除图片、视频、文本附件；保存草稿和提交发布时将附件列表写入请求体；不可编辑状态只读展示。

## Previous Task Check

- 已检查同仓库未完成任务：`doc/tasks/20260605-dcc-product-name-recognition/task.md`。
- 处理：该任务已因用户切换当前优先级标记 `blocked`；本任务只修改展厅后台页面、展厅 API 类型、测试与本任务文档。

## BDD 场景

- BDD: 基础弹框可管理附件 -> Given 可编辑产品基础弹框打开 / When 用户上传图片、视频、文本附件 / Then 列表展示文件名、类型、大小、排序和删除操作。
- BDD: 保存和发布携带附件 -> Given 基础弹框附件列表已编辑 / When 保存草稿或提交发布 / Then 请求体必须包含排序后的 `attachments`。
- BDD: 只读产品不能编辑附件 -> Given 产品基础弹框处于不可编辑状态 / When 附件资料区显示 / Then 上传、删除和排序控件不可用。

## 里程碑

- [x] M1：检查前置任务状态，创建任务文档。
- [x] M2：新增 RED 前端静态/单元测试。
- [x] M3：实现 API 类型、上传方法和基础弹框 UI。
- [x] M4：运行验证、更新证据并收尾提交。

## 预期验证

- `node scripts/showroom-product-attachments.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260605-showroom-product-attachments/frontend-feature-evidence.md`
- `git diff --check -- src/api/showroom-admin src/views/showroom-admin doc/tasks/20260605-showroom-product-attachments`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。上传失败或接口失败直接显示错误，不构造本地假附件。
- `是否从根因和长期维护角度解决`：是。附件通过后端上传接口取得正式 `fileId`，并随产品 revision 保存。
- `是否存在临时补丁或绕过`：否。不使用普通下载兜底，不绕过产品发布流程。

## 当前状态

completed

## Current Status

completed

## 完成记录

- 扩展展厅后台 API 类型与 `uploadProductAttachment` 上传方法。
- 基础弹框新增“附件资料”区，支持图片/视频/文本类型上传、展示大小、排序、删除。
- 保存草稿与直接发布请求体包含排序后的 `attachments`；不可编辑 revision 下上传和行操作禁用。
- 验证通过：`node scripts/showroom-product-attachments.test.mjs`，4 tests passed。
- 验证通过：`pnpm ts:check`。
