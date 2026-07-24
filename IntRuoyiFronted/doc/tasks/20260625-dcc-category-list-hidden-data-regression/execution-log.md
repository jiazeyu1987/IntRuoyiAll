# DCC 文件类别列表数据被默认隐藏回归修复执行日志

BDD: 类别列表显示真实类别主数据 -> Given 后端返回真实 DCC 文件类别列表 / When 用户打开 DCC 文件类别 的 类别列表 页签 / Then 页面必须显示真实类别主数据，而不能因审批摘要为空直接隐藏整行。

BDD: 审批摘要为空时显式显示占位 -> Given 某个文件类别尚未配置审核或批准岗位 / When 用户查看该类别行的审批摘要 / Then 摘要列应显示 '-' 占位，而不是把类别从列表默认移除。

BDD: 查询条件只影响查询字段 -> Given 用户输入类别编码、类别名称或启用状态筛选 / When 类别列表重新计算可见行 / Then 只按查询条件过滤，不再附带审批摘要是否存在的隐式过滤。

GREEN: experience-preflight -> PASS，本机真实只读验证仅涉及登录与页面查看，已先确认 `http://localhost:8081/login?redirect=/index` 可访问并按 `docs/login-access.md` 使用测试租户最小登录路径。

RED: node tests/e2e/dcc-category-governance-summary-static.spec.js -> FAIL, 旧实现仍把类别列表表格绑定到 `visibleCategories`，测试找不到要求的 `:data="filteredCategories"`，证明审批摘要过滤仍在生效。

GREEN: node tests/e2e/dcc-category-governance-summary-static.spec.js -> PASS

GREEN: node tests/e2e/dcc-review-matrix-tab-static.spec.js -> PASS

GREEN: readonly-db-diagnosis -> PASS，本机库 `dcc_file_category` 现有 `172` 条有效类别，测试租户 `tenant_id=122` 现有 `64` 条有效类别，但仅 `5` 条带审批岗位摘要；问题确认为前端默认过滤回归而非数据删除。
