# 执行日志：DCC 访问规则仅显示手动保存目录

- BDD: 手动保存目录列表只返回显式绑定目录 -> Given 多个目录存在访问规则且仅部分目录被标记为 access_rule_manually_bound / When 调用 access-rule-directories 接口 / Then 只返回被标记且当前仍有规则的目录路径摘要。
- BDD: 继承目录默认不进入手动保存列表 -> Given 新子目录从父目录克隆了访问规则但未经过访问规则页显式保存 / When 调用 access-rule-directories 接口 / Then 该子目录不得出现在左侧目录摘要列表中。
- BDD: 访问规则页显式保存会建立手动绑定 -> Given 目录当前存在或新建访问规则草稿 / When 调用 PUT /{id}/access-rules 成功 / Then 目录 access_rule_manually_bound 必须被写为 true。
- BDD: 删除整组规则会清除手动绑定 -> Given 某目录已被手动保存且存在访问规则 / When 调用 DELETE /{id}/access-rules / Then 该目录全部规则被删除且 access_rule_manually_bound 必须被写为 false。
- BDD: 访问规则引用不存在目录时仍需 fail-fast -> Given dcc_directory_access_rule 中存在指向不存在目录的记录 / When 调用 access-rule-directories 接口 / Then 服务直接抛出明确错误，不静默过滤。

- INFO: task-created -> 已创建后端任务文档，准备补 schema、service、controller 与 NAS 克隆目录的 RED 回归。
- RED: backend-targeted-tests -> FAIL，`mvn -pl yudao-module-dcc -am "-Dtest=DccDirectoryAdminServiceImplTest,DccControlledFileNasTransferServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 初次失败：`DccFileDirectoryDO` 缺少 `accessRuleManuallyBound` 字段、迁移文件 `sql/mysql/20260626_dcc_access_rule_manual_binding.sql` 不存在。
- RED: manual-binding-sql-contract -> FAIL，`python -X utf8 -m pytest script/tests/test_dcc_directory_access_rule_manual_binding_sql.py -q` 初次失败：缺少新增列 migration。
- RED: test-schema-bootstrap -> FAIL，补齐新字段后再次运行 Maven 失败：`src/test/resources/sql/create_tables.sql` 中 `DEFAULT b'0'` / `DEFAULT b'1'` 触发 H2 解析错误。
- RED: runtime-schema-coverage -> FAIL，`DccBaseSchemaTest` 报 `sql/mysql/20260515_dcc_runtime_schema_repair.sql` 缺少 `dcc_category_view_matrix_rule` 的幂等建表或列补丁覆盖。
- GREEN: backend-targeted-tests -> PASS，`mvn -pl yudao-module-dcc -am "-Dtest=DccDirectoryAdminServiceImplTest,DccControlledFileNasTransferServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过；验证 `listAccessRuleDirectories` 只返回 `access_rule_manually_bound=true` 且仍有规则的目录，`replaceAccessRules` 写 `true`，`deleteAccessRules` 写 `false`，自动克隆子目录默认 `false`。
- GREEN: manual-binding-sql-contract -> PASS，`python -X utf8 -m pytest script/tests/test_dcc_directory_access_rule_manual_binding_sql.py -q` 通过；验证 `sql/mysql/20260626_dcc_access_rule_manual_binding.sql` 使用 `information_schema.columns` 幂等守卫添加 `access_rule_manually_bound`，且未做历史回填。
- INFO: runtime-diagnosis -> 用户反馈页面仍显示大量目录后，复核本机运行态发现 `48081` 仍使用旧 runtime jar，且本机数据库缺少 `dcc_file_directory.access_rule_manually_bound` 列，导致页面继续暴露旧“按有规则即展示目录”的行为。
- RED: local-runtime-schema-guard -> FAIL，`docker exec int-ruoyi-mysql mysql ... SHOW COLUMNS FROM dcc_file_directory LIKE 'access_rule_manually_bound';` 返回空，证明本机后端重启脚本尚未把 `20260626_dcc_access_rule_manual_binding.sql` 纳入自动补库链路。
- GREEN: local-restart-schema-guard-test -> PASS，`python -X utf8 -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` 通过；新增约束确保 `restart-int-ruoyi-local.ps1` 在本机启动后端前自动探测并执行 `20260626_dcc_access_rule_manual_binding.sql`。
- GREEN: local-runtime-refresh -> PASS，`powershell -NoProfile -ExecutionPolicy Bypass -File ruoyi-vue-pro\\script\\deploy\\restart-int-ruoyi-local.ps1 -Component backend` 后，本机数据库已出现 `access_rule_manually_bound` 列，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- GREEN: authenticated-access-rule-directories-after-restart -> PASS，真实登录 `http://localhost:8081/dcc/controlled-file/access-rules` 后抓取页面实际 `GET /admin-api/dcc/directories/access-rule-directories` 响应，返回 `code=0`、`data=[]`；页面左侧同步渲染 `0` 项，证明当前本机运行态已按“仅显示手动保存目录”生效。
