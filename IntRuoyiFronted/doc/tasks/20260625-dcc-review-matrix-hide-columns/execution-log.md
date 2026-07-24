# 执行日志：审阅矩阵页签改名并隐藏三列

BDD: 审阅矩阵页签改名 -> Given 用户进入类别页 When 查看顶部页签 Then 原 DCC审阅矩阵 页签显示为 审阅矩阵。

BDD: 审阅矩阵总览隐藏冗余列 -> Given 用户进入审阅矩阵列表 When 查看表头 Then 不再显示 可查阅主体、待审预览主体、下载规则、当前状态/风险、当前版本、生效时间、备注 列。

BDD: 审阅矩阵仅保留精简查询 -> Given 用户进入审阅矩阵列表 When 查看查询区 Then 只保留 类别编码、类别名称、查询、重置、刷新列表、按人反查。

BDD: 审阅矩阵核心操作保留 -> Given 用户进入审阅矩阵列表 When 查看操作区 Then 编辑、删除、预览能力保持不变。

BDD: 查看矩阵仅保留精简查询 -> Given 用户进入查看矩阵列表 When 查看查询区 Then 只保留 类别编码、类别名称、查询、重置、按人反查。

BDD: 查看矩阵隐藏启用状态列 -> Given 用户进入查看矩阵列表 When 查看表头 Then 不再显示 启用状态 列。

RED: node tests/e2e/dcc-review-matrix-tab-static.spec.js -> FAIL, 旧静态契约仍要求页签文案为 `DCC审阅矩阵` 且仍允许三列存在。

RED: node tests/e2e/dcc-category-governance-summary-static.spec.js -> FAIL, 旧类别页静态契约仍要求 `DCC审阅矩阵` 页签名称。

GREEN: node tests/e2e/dcc-review-matrix-hide-columns-static.spec.js -> PASS

RED: node tests/e2e/dcc-review-matrix-tab-static.spec.js -> FAIL, 旧静态契约仍要求已删除风险摘要布局的 `flex-wrap: nowrap;` 样式。

GREEN: node tests/e2e/dcc-review-matrix-tab-static.spec.js -> PASS

GREEN: node tests/e2e/dcc-category-governance-summary-static.spec.js -> PASS

RED: node tests/e2e/dcc-review-matrix-hide-columns-static.spec.js -> FAIL, 删除风险列后组件样式仍残留 `matrix-risk-list` 旧 class。

GREEN: node tests/e2e/dcc-review-matrix-hide-columns-static.spec.js -> PASS
