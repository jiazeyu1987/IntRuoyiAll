# Execution Log：DCC 访问规则已绑定目录列表接口与整组删除

BDD: 管理端可一次获取存在访问规则的目录路径摘要 -> Given 多个目录存在访问规则 and 目录树层级完整 / When 调用 access-rule-directories 接口 / Then 返回按目录树前序排序的目录摘要列表，包含 id、name、directoryPath。
BDD: 访问规则引用不存在目录时列表接口 fail-fast -> Given dcc_directory_access_rule 中存在 directory_id 指向已删除目录 / When 调用 access-rule-directories 接口 / Then 接口直接报明确错误，不静默过滤该条规则。
BDD: 删除目录整组访问规则后该目录不再出现在摘要列表 -> Given 某目录存在多条访问规则 / When 调用删除整组访问规则接口 / Then 该目录所有访问规则被删除，随后摘要列表不再包含该目录。
BDD: 现有单目录访问规则读取与替换接口保持契约不变 -> Given 前端继续调用 /{id}/access-rules GET 与 PUT / When 读取或保存规则 / Then subjectType、subjectId、canQuery、canPreview、canDownload、active、changeReason 字段语义不变。

INFO: task-created -> 已创建后端任务文档，准备补访问规则目录摘要与整组删除 RED 回归。
INFO: command-fix -> 初次 `mvn -pl yudao-module-dcc -Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest ...` 在 PowerShell 里因逗号参数解析失败；已按线程基线改为单引号包裹后重跑，不视为业务 RED 结果。
RED: `mvn -pl yudao-module-dcc '-Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 编译报 `listAccessRuleDirectories` / `deleteAccessRules` 符号不存在，说明控制器与服务接口尚未实现访问规则目录摘要和整组删除能力。
INFO: implementation -> 已补控制器、服务、VO、前端 API 与访问规则页面改造，前端静态合同已转绿。
INFO: runtime-regression -> 用户实测命中 `/dcc/directories/access-rule-directories` 报 `Method parameter 'id'... For input string: "access-rule-directories"`，根因收敛为目录控制器宽泛 `/{id}` 路由与静态路径冲突。
RED: `mvn -pl yudao-module-dcc '-Dtest=DccDirectoryControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 新增回归 `idBasedMappings_requireNumericPathVariables` 断言当前映射仍为 `/{id}`，静态访问规则目录路径存在被 `id` 路由抢占的风险。
GREEN: `mvn -pl yudao-module-dcc '-Dtest=DccDirectoryControllerTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 已将目录控制器所有 `id` 路由收紧为 `/{id:\\d+}`，并新增 `accessRuleDirectories_staticRouteIsNotCapturedByIdRoute` 验证静态 `/access-rule-directories` 命中正确处理方法。
BLOCKER: `mvn -pl yudao-module-dcc '-Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> 当前 `DccDirectoryControllerTest` 已 PASS，但 `DccDirectoryAdminServiceImplTest` 在 Spring 上下文初始化阶段被 `src/test/resources/sql/create_tables.sql` 第 50 条 `dcc_controlled_file_batch_recognition_task` 建表语句阻塞，错误与本次访问规则功能无直接关系；需先修复当前 H2 schema 基线，才能继续完成该类集成验证。
