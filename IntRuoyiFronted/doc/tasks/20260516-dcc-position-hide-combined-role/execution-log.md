BDD: 岗位分配主列表隐藏合并岗位 -> Given `DCC岗位分配` 主列表已能显示 `部门负责人` 与 `部门授权代表` / When 页面渲染岗位数据 / Then 不再显示 `编制部门负责人或授权代表` 这条合并岗位 / And 两条拆分岗位保持可见。
RED: pre-change live page evidence -> FAIL, the real `DCC岗位分配` page still showed the combined role `编制部门负责人或授权代表` alongside `部门负责人 / 部门授权代表`.

GREEN: real positions-page verification -> PASS, the live page returned `rowCount=32`, `hasDeptOwner=true`, `hasAuthRep=true`, and `hasCombined=false` after filtering out `source=INTAUTH:19` in the positions-page view model.
