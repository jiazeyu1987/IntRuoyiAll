# 任务：产品主数据页面编码可变与日期展示

## 任务目标

在前端产品主数据页面中，将表格“更新时间”按 `YYYY-MM-DD` 显示，和后端产品主数据 Excel 导出格式保持一致；同时允许管理员编辑产品主数据 `产品编码`，因为展厅和 DCC 的稳定关联以产品主数据 ID 为准，产品编码后续会统一变更。

## Previous Task Check

- 前端上一相关任务：产品主数据页面已合入 `int_main`。
- 状态：页面存在，可在 `/mdm/product` 访问。
- 处理：本任务只调整更新时间展示格式，不改页面结构和接口契约。

## BDD 场景

- BDD: 产品主数据更新时间按年月日显示 -> Given 产品主数据列表返回 `updateTime` / When 管理员打开 `/mdm/product` / Then 表格更新时间只展示年月日，不展示时分秒。
- BDD: 产品编码可编辑 -> Given 管理员打开已有产品主数据编辑弹窗 / When 需要统一调整产品编码 / Then 产品编码输入框可编辑并随保存请求提交。

## Milestones

- [x] M1：建立任务文档和 BDD。
- [x] M2：补静态契约测试锁定 `dateFormatter2` 和产品编码可编辑。
- [x] M3：实现产品主数据表格更新时间格式化；编辑弹窗产品编码已为可编辑输入框并补测试锁定。
- [x] M4：运行前端验证并提交任务相关改动。

## Expected Verification

- `node tests/mdm-product-update-date-format-static.spec.mjs`
- `node tests/mdm-product-code-editable-static.spec.mjs`
- `pnpm ts:check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。使用项目已有 `dateFormatter2`，不新增页面私有日期格式化逻辑。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed: 产品主数据页面“更新时间”列已使用项目已有 `dateFormatter2` 按年月日显示；编辑弹窗产品编码保持可编辑输入框，并已用静态契约测试锁定。

## 最终验证结果

- `node tests/mdm-product-update-date-format-static.spec.mjs` -> PASS。
- `node tests/mdm-product-code-editable-static.spec.mjs` -> PASS。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

## Cleanup Keep

- doc/tasks/20260607-mdm-product-update-date-format/frontend-feature-evidence.md
