# Execution Log: DCC 文件类别 E2E 历史链物理清理

BDD: 正式文件类别列表不应再混入旧 E2E 类别链 -> Given 当前租户已经拥有 48 条正式 IntAuth 导入文件类别 / When 旧 `E2E` 文件类别链被物理清理 / Then `GET /dcc/file-categories` 与真实前端 `DCC文件类别` 页面都不再出现 `DCC_RUNTIME_CATEGORY`。

BDD: 旧 E2E 类别链删除时必须一起删除其测试文件历史 -> Given `DCC_RUNTIME_CATEGORY` 仍挂有测试目录、审批路线、受控文件、路线快照和盖章记录 / When 执行物理清理 / Then 这些测试历史引用必须与类别一起删除，而不能留下悬挂残留。

RED: read-only pre-delete inspection showed the old E2E chain still existed in live MySQL: category `900201`, directory `900002`, route `900401`, 6 `dcc_controlled_file` rows, 6 route-snapshot rows, 4 stamp rows, and 4 directory-access-rule rows.

GREEN: one transaction physically deleted the full E2E chain and post-delete checks showed `runtime_category_rows=0` while the current tenant still retained `48` active formal IntAuth-derived categories.
