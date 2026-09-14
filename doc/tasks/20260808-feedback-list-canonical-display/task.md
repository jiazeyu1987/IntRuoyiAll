# 报工列表正式字段显示修复

## Task Goal

修复报工管理“报工”列表视觉上像空表的问题：接口已返回正式报工行，但非导入来源的正式报工没有 `excel*` 展示字段，页面不能只绑定 Excel 字段导致产品、工序、人员和时间为空。

## Milestones

- [x] 复现并记录接口有数据但页面关键列为空的现象
- [x] 增加正式字段显示静态回归合同
- [x] 修改报工列表展示逻辑，按正式行数据形态显示可见业务字段
- [x] 运行定向静态合同、类型检查和真实页面只读复验
- [x] 更新验证报告

## Expected Verification

- `node tests/e2e/mes-feedback-list-canonical-display-static.spec.js`
- `node tests/e2e/mes-feedback-list-excel-columns-static.spec.js`
- `node tests/e2e/mes-pro-feedback-unified-list-template-static.spec.js`
- `pnpm ts:check`
- 真实 Playwright 只读打开 `/mes/pro/feedback?tab=feedback`，确认列表请求 `total > 0` 且页面可见正式字段。

## Current Status

completed - 报工列表已按导入快照字段 + 正式 canonical 字段显示，定向静态合同、类型检查和真实页面只读复验均通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次不是错误降级，而是同一正式报工列表需要展示两类正式行形态：导入来源快照字段和普通正式报工字段。
- `是否从根因和长期维护角度解决`：是；修复展示绑定只覆盖导入字段、未覆盖正式报工 canonical 字段的根因。
- `是否存在临时补丁或绕过`：否；不造假数据、不隐藏空态、不改数据库。

## Applicable Gates

- `bug-regression-fix-loop`：先复现用户可见缺陷并用回归合同锁定，再做最小修复。
- `frontend-feature-delivery`：前端行为变更需记录 BDD、RED/GREEN 和真实页面路径。
- `docs/frontend-development.md#用户可见描述与内部编码隔离门禁`：用户可见字段必须来自正式展示字段或正式业务字段，不能用编码占位掩盖缺数据。
