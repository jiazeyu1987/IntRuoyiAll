# Execution Log - 20260625-dcc-basic-data-main-code-doc-control-order

- BDD: 主表隐藏三列并新增主编码占位 -> Given 用户进入 DCC 基础数据页 When 查看主列表表头 Then 主表显示 文控/主编码/项目名称/项目代码/类别/项目组负责人/更新时间/关联文档，且不再显示 委托生产/项目工程师/状态。
- BDD: 主编码当前统一显示无 -> Given 用户查看任意 DCC 基础数据主表行 When 渲染主编码列 Then 该列统一显示 无。
- BDD: 详情和导入预览保持原合同 -> Given 用户打开详情抽屉或导入预览 When 查看字段 Then 存放位置/优先级/委托生产/项目工程师/状态等既有详情或导入合同保持不变。
- RED: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> FAIL，主表仍显示 委托生产/项目工程师/状态，且不存在 主编码 列合同。
- GREEN: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> PASS