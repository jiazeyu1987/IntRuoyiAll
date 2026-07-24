# Execution Log - DCC项目代码关联文件数排序

BDD: 项目代码列表显示关联文件数 -> Given 项目代码 A 关联 2 个受控文件且项目代码 B 未关联文件, When 用户打开 DCC 项目代码列表, Then A 显示关联文件数 2 且 B 显示 0。
BDD: 项目代码列表按文件数排序 -> Given 多个项目代码拥有不同关联文件数, When 用户按关联文件数升序或降序排序, Then 后端分页结果按该数量正确排序且前端向列表接口传递排序参数。

STATUS: task-doc -> CREATED
RED: mvn -pl yudao-module-dcc -Dtest=DccProjectCodeServiceImplTest#pageShouldIncludeAssociatedFileCountAndSortByCount test -> FAIL, DccProjectCodePageReqVO#setFileCountSort and DccProjectCodeDO#getAssociatedFileCount missing.
RED: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> FAIL, table missing “关联文件数” column.
GREEN: mvn -pl yudao-module-dcc -Dtest=DccProjectCodeServiceImplTest#pageShouldIncludeAssociatedFileCountAndSortByCount test -> PASS.
GREEN: node tests/e2e/dcc-project-code-basic-data-static.spec.js -> PASS.
GREEN: associated-file-count-query -> PASS, changed from per-row count to one grouped count query for current filtered project-code set.
STATUS: implementation -> COMPLETED
GREEN: closeout-verification -> PASS, backend class regression + frontend static contract + evidence validation all passed.
STATUS: task -> COMPLETED
