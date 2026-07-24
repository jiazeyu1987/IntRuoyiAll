# Execution Log：DCC 基础数据迁入全局基础数据子入口（前端）

BDD: 项目代码和产品目录拆成两个独立页面 -> Given 用户从全局基础数据菜单进入 DCC 子入口 / When 页面渲染 / Then 当前页面只显示对应业务内容，不再显示内部切换 tab。
BDD: 项目代码详情跳转仍可用 -> Given 用户从其他 DCC 页面打开项目代码详情 / When 跳转到项目代码页面 / Then 页面保留 projectCodeId 详情抽屉行为。
BDD: 产品目录页面直接加载只读表格 -> Given 用户进入 DCC产品目录 页面 / When 页面首次加载 / Then 直接展示产品目录筛选和列表。

INFO: task-created -> 前端任务文档已创建，准备补 DCC 基础数据全局子入口 RED 静态契约。
RED: node tests/e2e/dcc-basic-data-global-submenu-static.spec.js -> FAIL, 缺少项目代码独立页面文件，旧实现仍是页内 tab。
RED: node tests/e2e/dcc-basic-data-product-catalog-static.spec.js -> FAIL, 缺少产品目录独立页面文件，旧实现仍是页内 tab。
GREEN: node tests/e2e/dcc-basic-data-global-submenu-static.spec.js -> PASS
GREEN: node tests/e2e/dcc-basic-data-product-catalog-static.spec.js -> PASS
GREEN: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> PASS
GREEN: node tests/e2e/dcc-project-code-recognition-static.spec.js -> PASS
GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS
INFO: implementation-complete -> 已新增 `project-code/index.vue`、`product-catalog/index.vue` 独立页面，并将旧 `/dcc/controlled-file/basic-data` 跳转统一改为 `/mdm/project-code`。
GREEN: experience-preflight -> PASS, 已按登录与真实 E2E 门禁复核本机入口、测试租户与目标菜单路径，允许执行真实浏览器可见性验收。
GREEN: real-browser submenu verification -> PASS, 测试租户真实登录后，`/mdm/project-code` 与 `/mdm/product-catalog` 页面均可从左侧全局基础数据菜单路径打开，产品目录页关键表头 `数据来源` 可见。
