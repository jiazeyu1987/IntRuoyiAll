# 任务：DCC 项目别名目录识别与文件类型分层

## 任务目标

- 在 DCC 产品名称/产品编码识别中新增规则层：文件名优先、目录路径其次，按 DCC 项目代码表的项目名称/项目代码及规范化别名归类。
- 支持 `一次性使用指引导管（三类）` 与 `一次性使用指引导管` 这类业务同义目录命中同一 DCC 项目代码主键，例如 `dcc_project_code.id = 117`。
- 为每个识别文件保存 5 层文件类型：第一层为 `QMS文档` 或 `技术文档`；技术文档第二层按文控权限类别列表匹配；QMS 第二层以及第三至第五层先为空。
- 识别记录导出可追溯识别方式、命中证据与文件类型分层。

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本、测试输出必须显式 UTF-8；不使用 `&&`。
- 命中 `backend-api-delivery`：新增识别行为必须以服务测试驱动，失败不静默降级。
- 命中 `database-schema-delivery`：新增字段和索引必须非破坏性迁移，并同步 H2/测试 schema。
- 命中 `frontend-feature-delivery`：如后续展示文件类型或映射状态，需保持现有 API 合同与错误态可见；本阶段先完成后端识别与导出。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过规范化别名规则先建立稳定目录/文件名映射，减少 2 万文件直接调用 Codex。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 文件名优先归类 -> Given 文件名命中项目 A 且目录路径命中项目 B / When 执行产品识别 / Then 最终使用文件名命中的项目 A，且不调用 Codex。`
- `BDD: 目录别名归类 -> Given 文件位于“81 一次性使用指引导管（三类） CEGCT/输入阶段”目录下且项目代码表存在 id=117、项目名称“一次性使用指引导管”、项目代码 CEGCT / When 执行产品识别 / Then 文件归到 id=117，识别方式为目录规则，识别记录保存命中证据。`
- `BDD: 文件类型分层 -> Given 文件位于 QMS documents、DMR 或 DHF 下 / When 执行产品识别 / Then 第一层分别为 QMS文档 或 技术文档；技术文档第二层按类别列表匹配，预留层为空。`
- `BDD: 导出可追溯 -> Given 文件已有成功或失败识别记录 / When 导出识别记录 / Then Excel 包含产品、识别方式、匹配证据、批量任务和文件类型 1-5 层。`

## 里程碑

1. M1：建立任务台账、BDD 场景与 RED 测试。completed
2. M2：实现文件名/目录规范化规则识别与文件名优先级。completed
3. M3：实现文件类型 1-5 层落库、记录和导出。completed
4. M4：补齐 MySQL migration、H2 test schema 与 schema 测试。completed
5. M5：运行目标后端测试并提交。completed

## 预期验证

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

completed

## 当前阻塞

- 暂无。

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q` -> `PASS`，8 个用例通过。
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> `PASS`。
- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest,DccFileCategoryAdminServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> `PASS`，76 个用例通过。
