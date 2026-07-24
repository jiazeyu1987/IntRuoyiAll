# Execution Log：DCC 项目别名映射识别

- BDD: 已确认文件名别名优先 -> Given 文件名命中已确认项目别名但不包含标准项目名称 / When 执行基础信息识别 / Then 使用别名绑定的 DCC 项目代码主键，识别方式记录为别名规则，且不调用 Codex。
- BDD: 已确认目录别名优先于标准目录规则 -> Given 文件名未命中且目录路径命中已确认项目别名 / When 执行基础信息识别 / Then 使用目录别名绑定的 DCC 项目代码主键，并记录目录命中证据。
- BDD: 未确认或禁用别名不得生效 -> Given 文件名或目录仅命中待确认/禁用别名 / When 执行基础信息识别 / Then 不使用该别名，继续执行既有文件名、目录、Codex 链路。
- BDD: 识别记录可追溯别名 -> Given 文件通过别名识别成功 / When 导出或查看识别记录 / Then 可看到别名 ID、别名文本和命中来源。

- RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: `DccProjectCodeAliasMappingDO` / `DccProjectCodeAliasMappingMapper` 不存在，当前实现缺少持久化别名映射层。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex,DccBaseSchemaTest#mysqlSchemaShouldSupportDccProjectCodeAliasMappingRecognition" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests, 0 failures。
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，7 passed。
- GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_containsSharedLedgerRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test, 0 failures。
- GREEN: `experience-preflight` -> PASS，真实数据 E2E 前已读取 PowerShell 编码规则、经验索引、登录预检要求和 Playwright 运行约束；本轮仅使用测试租户真实路径，不使用 mock、接口绕过或静默 fallback。

- GREEN: `real-data-e2e -> login-preflight` -> PASS，本机真实前端 `http://localhost:8081`、后端 `http://localhost:48081`、测试租户 `测试租户/aoteman` 登录进入 `/index`。
- GREEN: `real-data-e2e -> schema-precondition` -> PASS，本地测试库补齐已存在正式迁移 `20260703_dcc_project_code_alias_mapping.sql` 与 `20260526_dcc_electronic_signature_hardening.sql`，用于匹配当前代码运行态。
- GREEN: `real-data-e2e -> single-file-alias-recognition` -> PASS，Playwright 真实打开 `/dcc/controlled-file/detail/2054545668044051057`，点击“识别基础信息”，POST `/admin-api/dcc/controlled-files/2054545668044051057/recognize-project-code` 返回 `code=0`，`recognitionMethod=FILE_NAME_ALIAS`、`dccProjectCodeId=1`、`projectName=PTCA球囊扩张导管`、`projectCode=PTCABC`、`matchedProjectAliasId=2`。
- GREEN: `real-data-e2e -> ledger-db-verification` -> PASS，`dcc_controlled_file.id=2054545668044051057` 已更新为 `dcc_project_code_id=1`、`product_code=PTCABC`、`product_name=PTCA球囊扩张导管`；识别账本 `dcc_controlled_file_recognition_record.id=209` 记录 `BASIC_INFO / FILE_NAME_ALIAS / SUCCESS / matched_project_alias_text=empty.docx / matched_project_alias_source=FILENAME`。
- NOTE: `real-data-e2e -> rule-alignment-observation` -> 当前实现的“文件名别名匹配”读取的是底层 `infra_file.name`，本次真实文件的底层源文件名为 `empty.docx`，而文件查阅页显示的受控文件名原为 `codex-dr-20260610184136.docx`；因此 `codex-dr` 别名未命中，补充 `empty.docx` 测试别名后命中。若正式规则要求使用文件查阅页展示文件名/受控文件名，应后续调整识别输入源。