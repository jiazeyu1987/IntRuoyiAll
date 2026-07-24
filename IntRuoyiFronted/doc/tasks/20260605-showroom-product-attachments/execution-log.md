# 执行日志：后台展厅产品基础附件管理

- BDD: 基础弹框可管理附件 -> Given 可编辑产品基础弹框打开 / When 用户上传图片、视频、文本附件 / Then 列表展示文件名、类型、大小、排序和删除操作。
- BDD: 保存和发布携带附件 -> Given 基础弹框附件列表已编辑 / When 保存草稿或提交发布 / Then 请求体必须包含排序后的 `attachments`。
- BDD: 只读产品不能编辑附件 -> Given 产品基础弹框处于不可编辑状态 / When 附件资料区显示 / Then 上传、删除和排序控件不可用。

## TDD 记录

- RED: `node scripts/showroom-product-attachments.test.mjs` -> FAIL，4 assertions failed，预期原因：API 上传方法、基础弹框附件区、附件 payload 与只读禁用逻辑尚未实现。
- GREEN: `node scripts/showroom-product-attachments.test.mjs` -> PASS，4 tests passed。
- REGRESSION: `node scripts/showroom-product-attachments.test.mjs` -> PASS，2026-06-05 20:46，4 tests passed。
- REGRESSION: `pnpm ts:check` -> PASS，2026-06-05 20:48。
