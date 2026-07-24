# 执行日志

## BDD

- BDD: 文件名优先归类 -> Given 文件名命中项目 A 且目录路径命中项目 B / When 执行产品识别 / Then 最终使用文件名命中的项目 A，且不调用 Codex。
- BDD: 目录别名归类 -> Given 文件位于“81 一次性使用指引导管（三类） CEGCT/输入阶段”目录下且项目代码表存在 id=117、项目名称“一次性使用指引导管”、项目代码 CEGCT / When 执行产品识别 / Then 文件归到 id=117，识别方式为目录规则，识别记录保存命中证据。
- BDD: 文件类型分层 -> Given 文件位于 QMS documents、DMR 或 DHF 下 / When 执行产品识别 / Then 第一层分别为 QMS文档 或 技术文档；技术文档第二层按类别列表匹配，预留层为空。
- BDD: 导出可追溯 -> Given 文件已有成功或失败识别记录 / When 导出识别记录 / Then Excel 包含产品、识别方式、匹配证据、批量任务和文件类型 1-5 层。

## TDD

- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增文件类型断言缺少 DO 字段、迁移和实现。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增文件类型断言缺少 DO 字段、迁移和实现。
- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest,DccFileCategoryAdminServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 识别规则与 runtime schema 回归暴露文件名 shortcut、目录别名 matchText、短编码回退 Codex 和 recognition record 字段契约问题。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q` -> PASS, 8 passed。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest,DccFileCategoryAdminServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 76 tests。
