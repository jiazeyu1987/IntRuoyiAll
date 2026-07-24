# Execution Log: DCC 岗位列表切换到 IntAuth 来源前端

BDD: DCC 岗位分配页展示 IntAuth 岗位主数据 -> Given DCC 后端返回来自 IntAuth 的岗位列表, When 用户打开 DCC 岗位分配页, Then 页面展示该岗位列表并允许继续维护每个岗位的分配信息。

BDD: DCC 其他依赖岗位列表的页面继续可用 -> Given DCC 文件类别和审批路线页面依赖同一岗位列表, When 它们请求岗位数据, Then 页面仍然使用更新后的岗位来源正常渲染岗位选项。

RED: pending -> 当前前端尚未确认并锁定“DCC 岗位列表来自 IntAuth 来源”的失败用例。
