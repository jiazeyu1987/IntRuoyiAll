# 任务：DCC 项目别名映射识别

## 任务目标

- 在现有 DCC 基础信息识别链路前新增可维护的“项目别名/目录映射”能力。
- 已确认别名命中时优先归属到对应 `dcc_project_code.id`，避免 2 万文件重复依赖 Codex 内容识别。
- 识别记录需能追溯命中的别名或目录片段，保留现有文件名优先、目录其次、Codex 兜底规则。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本、测试输出必须显式 UTF-8；不使用 `&&`。
- 命中 `backend-api-delivery`：新增识别行为必须先写失败测试，错误不得吞掉或默认成功。
- 命中 `database-schema-delivery`：新增映射表、字段和索引必须非破坏性迁移，并同步测试 schema。
- 命中 `frontend-feature-delivery`：如新增维护入口，必须保留现有错误反馈和权限控制。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。别名映射只作为已确认规则层；未命中继续走既有正式识别链路，不吞异常。
- 是否从根因和长期维护角度解决：是。通过持久化确认映射承载业务简称、历史目录名和项目代码关系，减少重复智能识别。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 已确认文件名别名优先 -> Given 文件名命中已确认项目别名但不包含标准项目名称 / When 执行基础信息识别 / Then 使用别名绑定的 DCC 项目代码主键，识别方式记录为别名规则，且不调用 Codex。`
- `BDD: 已确认目录别名优先于标准目录规则 -> Given 文件名未命中且目录路径命中已确认项目别名 / When 执行基础信息识别 / Then 使用目录别名绑定的 DCC 项目代码主键，并记录目录命中证据。`
- `BDD: 未确认或禁用别名不得生效 -> Given 文件名或目录仅命中待确认/禁用别名 / When 执行基础信息识别 / Then 不使用该别名，继续执行既有文件名、目录、Codex 链路。`
- `BDD: 识别记录可追溯别名 -> Given 文件通过别名识别成功 / When 导出或查看识别记录 / Then 可看到别名 ID、别名文本和命中来源。`

## 里程碑

1. M1：建立任务台账、BDD 场景与 RED 测试。completed
2. M2：新增项目别名映射表、DO/Mapper 与测试 schema。completed
3. M3：接入识别链路，已确认别名优先于普通规则。completed
4. M4：补识别记录追溯字段与导出。completed
5. M5：按需补前端维护入口。completed（本轮后端闭环复用现有识别入口，未新增前端入口）
6. M6：运行验证并单独提交本任务改动。in_progress

## 预期验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q`

## 当前状态

completed

## 当前阻塞

- 暂无。

## 完成记录

- 新增 `dcc_project_code_alias_mapping` 持久化表，用于保存已确认项目别名/目录片段到 `dcc_project_code.id` 的关系。
- 单文件识别链路调整为：文件名别名 -> 文件名标准规则 -> 目录别名 -> 目录标准规则 -> Codex 内容识别。
- 识别账本新增命中别名 ID、命中别名文本、命中别名来源，并同步 Excel 导出。
- 批量识别仍按同版本账本记录去重；别名识别成功记录会被批量任务自动复用，避免重复识别。

## 最终验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_confirmedFileNameAliasWinsBeforeStandardRulesAndCodex,DccBaseSchemaTest#mysqlSchemaShouldSupportDccProjectCodeAliasMappingRecognition" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests, 0 failures。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileMetadataImportExportServiceTest#recognitionRecordExport_containsSharedLedgerRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test, 0 failures。
- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py -q` -> PASS，7 passed。
