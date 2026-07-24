# Execution Log - 20260625-dcc-basic-data-crud-query

- BDD: 用户可新增项目代码 -> Given 用户进入 DCC 基础数据页并拥有维护权限 When 点击新增并填写必填项 Then 列表出现新增的项目代码记录。
- BDD: 用户可编辑项目代码 -> Given 列表已有项目代码记录 When 用户打开编辑弹窗并修改字段 Then 列表与详情展示更新后的值。
- BDD: 未被引用的项目代码可删除 -> Given 某条项目代码未被受控文件引用 When 用户确认删除 Then 该记录从列表消失。
- BDD: 已被引用的项目代码禁止删除 -> Given 某条项目代码已被受控文件引用 When 用户尝试删除 Then 系统明确报错并保留该记录。
- BDD: 用户可按筛选条件查询 -> Given 列表存在不同项目名称/项目代码/类别/状态的数据 When 用户输入筛选条件查询 Then 只返回符合条件的记录。
- BDD: 主编码继续统一显示无 -> Given 用户查看主表行数据 When 渲染主编码列 Then 该列仍统一显示 无。
- RED: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> FAIL, projectCodes.ts 缺少 /dcc/project-codes/create 静态合同
- GREEN: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> PASS
