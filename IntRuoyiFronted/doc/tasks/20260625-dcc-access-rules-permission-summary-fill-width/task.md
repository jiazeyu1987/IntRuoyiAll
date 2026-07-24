# 任务：DCC 访问规则权限摘要横向铺满

## 任务目标

在不改变 DCC 访问规则真实接口、字段绑定和保存行为的前提下，把“权限摘要”单元格从左侧紧凑堆叠样式调整为横向平铺样式，使启用状态、查看、预览、下载在整段可用宽度内均匀展开。

- 只调整前端展示结构和样式，不改后端接口、请求参数和响应结构。
- 保留 `row.active / row.canQuery / row.canPreview / row.canDownload` 的真实绑定。
- 保留现有新增、删除、保存规则逻辑，不引入 mock、fallback 或静默降级。

## 当前状态

status: completed

## 上一相关任务检查

- 已检查同页上一任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260625-dcc-access-rules-hide-change-reason-column\task.md`，状态为 `completed`，允许继续本次样式调整。
- 已检查近期未完成任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260625-dcc-review-matrix-owner-role-triangle-alignment\task.md`，状态为 `blocked`，阻塞原因为用户切换到更高优先级需求，当前不与本任务冲突。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 页面表格和控件必须保持 IntPP 运维台风格，使用紧凑、可扫描的横向布局，不做无关重构。
  - 本次仅允许修改访问规则页展示层、静态测试和本任务文档；接口契约、后端行为、真实数据源保持不变。
  - 不得用 mock 数据、占位文案、fallback 或静默吞错掩盖真实问题。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过稳定布局类和静态断言约束权限摘要的横向铺满结构，避免后续样式回退。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 权限摘要横向铺满单元格 -> Given 管理员打开 DCC 访问规则页 When 查看权限摘要列 Then 启用状态、查看、预览、下载应在单元格横向可用宽度内平铺展示，不再只聚集在左侧。`
- `BDD: 权限摘要继续绑定真实权限字段 -> Given 管理员切换启用、查看、预览、下载开关 When 保存规则 Then 页面继续提交原有真实字段，不新增展示专用假数据。`

## 里程碑

1. M1：创建任务文档并记录前置门禁。`DONE`
2. M2：补静态 RED 断言，锁定横向铺满布局合同。`PENDING`
3. M3：实现权限摘要横向平铺样式。`PENDING`
4. M4：执行静态 GREEN 验证并完成收尾。`PENDING`

## 预期验证

- `node tests/e2e/dcc-access-rule-permission-summary-static.spec.js`
- `node --check tests/e2e/dcc-access-rule-permission-summary-static.spec.js`


## ??????

- `node tests/e2e/dcc-access-rule-permission-summary-static.spec.js`?PASS
- `node --check tests/e2e/dcc-access-rule-permission-summary-static.spec.js`?PASS
