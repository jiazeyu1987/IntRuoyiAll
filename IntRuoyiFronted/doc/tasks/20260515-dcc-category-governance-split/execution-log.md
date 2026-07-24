# Execution Log: DCC 文件类别治理职责拆分

BDD: DCC 文件类别页只保留类别主数据与基础维护 -> Given 用户打开 `DCC文件类别` 页 / When 页面加载完成 / Then 页面只展示类别列表、筛选、类别基础信息与目录绑定，不再展示审批矩阵入口、审批路线、分发规则或培训规则。

BDD: DCC 审批路线页统一承载审核入口和路线预览 -> Given 用户打开 `DCC审批路线` 页 / When 页面加载完成 / Then 页面展示审批矩阵入口与派生路线预览，而不再把审核配置留在 `DCC文件类别` 页。

BDD: DCC 下发与培训页独立承载各自规则 -> Given 用户分别打开 `DCC下发` 与 `DCC培训` 页 / When 页面加载完成 / Then `DCC下发` 只展示分发部门规则，`DCC培训` 只展示培训部门规则，且两页都不再混入审批路线内容。

RED: pre-change `src/views/dcc/controlled-file/categories/index.vue` 同时承载审批矩阵入口、审批路线、分发规则和培训规则；而 `src/views/dcc/controlled-file/routes/index.vue` 只承载路线预览，职责未按用户要求拆开。

GREEN: real browser verification on `http://127.0.0.1:8081` shows:
- `DCC文件类别` page no longer contains `审批矩阵` / `审批路线列表` / `分发部门规则` / `培训部门规则`
- `DCC审批路线` page contains `审批矩阵` entry and `派生四层预览`
- `DCC下发` page contains `分发部门规则` only
- `DCC培训` page contains `培训部门规则` only

GREEN: post-split error fix -> `DCC文件类别` source no longer mounts `CategoryMatrixDialog`, and `DCC审批路线` skips preview requests when no active route exists, so the page no longer surfaces `Approval route does not exist` as an initial screen error.
