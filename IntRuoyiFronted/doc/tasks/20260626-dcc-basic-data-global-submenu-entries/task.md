# 任务：DCC 基础数据迁入全局基础数据子入口

## 任务目标

- 将 `项目代码` 与 `产品目录` 从 `src/views/dcc/controlled-file/basic-data/index.vue` 的页面内 tab 拆成两个独立页面入口。
- 让左侧全局 `基础数据` 菜单下显示 DCC 的两个子入口，而不是进入同页后再切 tab。
- 保持 `项目代码` 页面既有能力和 `产品目录` 页面只读筛选表格能力。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个 frontend 相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-basic-data-product-catalog-tab\task.md`
- 状态：`COMPLETED`
- 处理：上一个前端任务完成了同页双 tab 版本；当前用户要求调整为全局基础数据子入口，因此在既有实现上继续演进。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 页面、工具栏、表格沿用 IntPP 运维工作台风格，不新增营销化布局。
  - 菜单入口、路由跳转、详情回跳必须和动态菜单结构一致，不保留同页 tab 兜底。
  - 未验证真实菜单前，不得把静态源码存在视为用户可达。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。移除页面内 `项目代码/产品目录` 切换壳，不保留双入口并行。
- `是否从根因和长期维护角度解决`：是。把两个面板提升为独立页面，并配合动态菜单子路由建模。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 项目代码和产品目录拆成两个独立页面 -> Given 用户从全局基础数据菜单进入 DCC 子入口 / When 页面渲染 / Then 当前页面只显示对应业务内容，不再显示内部切换 tab。`
- `BDD: 项目代码详情跳转仍可用 -> Given 用户从其他 DCC 页面打开项目代码详情 / When 跳转到项目代码页面 / Then 页面保留 projectCodeId 详情抽屉行为。`
- `BDD: 产品目录页面直接加载只读表格 -> Given 用户进入 DCC产品目录 页面 / When 页面首次加载 / Then 直接展示产品目录筛选和列表。`

## 里程碑

1. M1：建立前端任务台账并补 RED 静态契约。`COMPLETED`
2. M2：实现独立页面、路由与跳转调整。`COMPLETED`
3. M3：运行静态验证、类型检查与真实菜单可见性校验。`COMPLETED`

## 预期验证

- `node tests/e2e/dcc-basic-data-global-submenu-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
