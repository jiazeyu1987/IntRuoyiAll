# Execution Log - 20260625-dcc-basic-data-main-code-doc-control-order

- BDD: 文控为纯数字时按数字升序返回 -> Given 项目代码列表包含 2/10/30 等文控值 When 查询分页 Then 结果按 2、10、30 的数字顺序返回，而不是按 id 倒序或字符串顺序返回。
- BDD: 非数字文控排在数字文控之后 -> Given 项目代码列表同时包含纯数字和 A-1 等非纯数字文控 When 查询分页 Then 纯数字文控先按数字升序返回，非数字文控排在其后。
- BDD: 导出与分页复用同一默认顺序 -> Given 项目代码导出走同一分页查询 When 导出列表 Then 导出顺序与分页默认顺序一致。
- RED: mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest#pageAndExportShouldOrderByNumericDocControlNoAscendingBeforeNonNumeric" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，当前顺序为 `10, 2`，未按数字升序输出。
- GREEN: mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest#pageAndExportShouldOrderByNumericDocControlNoAscendingBeforeNonNumeric" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS