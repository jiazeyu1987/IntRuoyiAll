# Task: 生产订单补齐工艺路线关联产品

## 任务目标

- 在工艺路线关联产品中提供“从生产订单补齐产品”能力。
- 后端按当前工艺路线名称精确匹配生产订单产品名称，将匹配产品物料编号补齐到当前工艺路线关联产品。
- 仅修改本机业务源码、测试与任务文档；不操作服务器、不修改正式环境数据。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，命令不使用 `&&`，中文读写显式 UTF-8。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell、项目防错、前端页面样式与 BDD/TDD 门禁。
- 项目防错：生产工单和工艺路线产品关联必须基于真实表结构与业务链路，禁止 mock、默认成功或静默跳过。
- 前端样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，按钮保持现有操作台式紧凑按钮风格。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，新增后端正式批量补齐接口，前端只调用业务接口并展示结果。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 按路线名称补齐生产订单产品 -> Given 当前工艺路线名称等于生产订单产品名称 / When 用户点击从生产订单补齐产品 / Then 系统将匹配产品编号补齐到当前路线关联产品。
- BDD: 无匹配生产订单产品时失败 -> Given 没有生产订单产品名称等于当前工艺路线名称 / When 用户点击补齐 / Then 接口返回明确错误且不新增关联产品。
- BDD: 产品已绑定其他路线时失败 -> Given 匹配产品已关联其它工艺路线 / When 用户点击补齐 / Then 接口返回冲突产品编码且本次不部分写入。
- BDD: 已有关联产品不重复新增 -> Given 匹配产品已关联当前路线 / When 用户点击补齐 / Then 系统计入已存在数量并只新增缺失产品。

## 里程碑

- [x] M1：创建任务记录并读取经验门禁。
- [x] M2：补后端 RED 测试和前端静态 RED 测试。
- [x] M3：实现后端批量补齐接口与服务逻辑。
- [x] M4：实现前端按钮、API 类型和刷新提示。
- [x] M5：运行目标验证、更新证据、执行 closeout preview。
- [x] M6：按仓库隔离提交本任务改动。
- [x] M7：刷新本机 48081 后端运行包并验证接口映射生效。

## 预期验证

- `mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteProductBindFromWorkOrdersTest test`
- `node tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js`
- `pnpm.cmd exec eslint src/views/mes/pro/route/RouteProductList.vue src/api/mes/pro/route/product/index.ts tests/e2e/mes-pro-route-product-bind-from-work-orders-static.spec.js --format stylish`

## 当前状态

COMPLETED：后端批量补齐接口已实现并通过目标单测；前端按钮与 API 契约已通过静态测试和 ESLint；本机 48081 后端已重启到包含该接口的新运行包，未登录探测返回 `401 账号未登录`，不再返回“请求地址不存在”。

## Cleanup Keep

- `doc/tasks/20260709-route-product-bind-from-work-orders/backend-api-evidence.md`
