# 执行日志：DCC 基础数据主表隐藏存放位置和优先级

BDD: 主表隐藏存放位置和优先级 -> Given 用户进入 DCC 基础数据页 When 查看主列表表头 Then 不再显示 存放位置 与 优先级 两列。

BDD: 详情仍保留完整字段 -> Given 用户从主列表进入某条 DCC 基础数据详情 When 查看详情抽屉 Then 存放位置 与 优先级 仍作为条目详情字段可见。

BDD: 导入预览仍保留完整字段 -> Given 用户打开 DCC 基础数据导入预览 When 查看预览表头 Then 存放位置 与 优先级 仍保留在导入预览中，不改变导入合同。

RED: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> FAIL, 旧主列表仍保留 `存放位置` 列与 `优先级` 列，未满足本次精简要求。

GREEN: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> PASS
