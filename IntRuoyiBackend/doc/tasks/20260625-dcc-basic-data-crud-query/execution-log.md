# Execution Log - 20260625-dcc-basic-data-crud-query

- BDD: 可创建项目代码 -> Given 管理员提交合法项目代码请求 When 调用创建接口 Then 返回新记录编号且分页可查询到该记录。
- BDD: 可更新项目代码 -> Given 已存在项目代码记录 When 管理员提交更新请求 Then 详情与分页返回更新后的字段值。
- BDD: 未被引用项目代码可删除 -> Given 某条项目代码未被受控文件引用 When 调用删除接口 Then 该记录被删除。
- BDD: 被引用项目代码禁止删除 -> Given 某条项目代码已被 dcc_controlled_file 引用 When 调用删除接口 Then 系统明确报错并保留该记录。
- BDD: 分页筛选支持项目名称项目代码类别状态 -> Given 列表存在多条不同属性项目代码 When 按条件分页查询 Then 仅返回符合条件的记录。
- BDD: 菜单权限种子补齐维护权限 -> Given DCC 基础数据菜单存在 When 同步菜单权限种子 Then create/update/delete 权限同时存在于同一菜单下。
- RED: mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest,DccProjectCodeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 控制器/服务尚未声明 createProjectCode、updateProjectCode、deleteProjectCode
- GREEN: mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest,DccProjectCodeControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS
