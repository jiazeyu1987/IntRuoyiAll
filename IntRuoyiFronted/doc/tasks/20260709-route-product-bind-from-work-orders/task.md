# Task: 生产订单补齐工艺路线关联产品按钮

## 任务目标

- 在工艺路线详情“关联产品”页签表格下方左侧新增按钮 `从生产订单补齐产品`。
- 点击后调用后端批量补齐接口，将生产订单中产品名称等于当前工艺路线名称的产品编号补齐到当前路线关联产品。
- 保持现有关联产品列表、手动新增、复制、编辑、删除行为不变。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，命令不使用 `&&`，中文读写显式 UTF-8。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell、前端页面样式、BDD/TDD 和项目防错门禁。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，按钮沿用当前 Element Plus 操作按钮样式，不做无关重设计。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，前端只暴露正式后端能力并直接展示接口结果或错误。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 点击按钮补齐产品 -> Given 用户在工艺路线详情关联产品页签 / When 点击从生产订单补齐产品并确认 / Then 前端调用补齐接口、展示新增和已存在数量并刷新列表。
- BDD: 接口失败直接暴露 -> Given 后端返回无匹配或冲突错误 / When 用户点击补齐 / Then 前端不吞异常、不伪造成功，保留接口错误提示。

## 里程碑

- [x] M1：创建任务记录并读取经验门禁。
- [x] M2：补前端 RED 静态契约测试。
- [x] M3：实现 API 类型和页面按钮。
- [x] M4：运行前端目标验证并更新证据。
- [ ] M5：执行 closeout preview 并隔离提交本任务改动。

## 预期验证

- `node tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js`
- `pnpm.cmd exec eslint src/views/mes/pro/route/RouteProductList.vue src/api/mes/pro/route/product/index.ts tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js --format stylish`

## 当前状态

VERIFIED：关联产品页签表格下方已新增 `从生产订单补齐产品` 按钮，API 类型与页面行为通过静态契约测试和 ESLint。待完成 closeout preview 与隔离提交。

## Cleanup Keep

- `doc/tasks/20260709-route-product-bind-from-work-orders/frontend-feature-evidence.md`
