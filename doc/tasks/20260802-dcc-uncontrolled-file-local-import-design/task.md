# DCC 未受控文件本地下载与归类 BDD/TDD 设计

## Task Goal

基于当前 DCC NAS 目录、未受控文件统计、项目代码、item 和文件分类能力，设计“扫描未受控文件 -> 用户选择是否下载 -> 下载到本地对应目录 -> 按路径或名称归类”的正式实现方案，并将无法唯一识别项目代码、item 或文件分类的文件标记为“未分类/待处理”，不进行猜测归类。

## Scope

- 第一阶段已交付需求、系统设计、BDD、严格 TDD、真实 E2E 与测试数据文档。
- 当前阶段按上述文档推进开发实现与验证，优先完成 `dcc_nas_control_audit_file` 明细 schema、持久化模型和可执行 schema 验证。
- 本任务不操作真实 NAS 文件、不启动远端环境、不写入真实业务数据库；schema 改动仅通过迁移文件、测试 schema 和静态/单元测试验证。
- 设计优先复用当前 DCC 页面、接口、服务、数据模型、分类规则和测试结构。

## Milestones

- [x] M1：核对项目规则、经验门禁、现有 DCC/NAS 实现和测试资产。
- [x] M2：形成需求边界、领域状态、交互流程、复用方案与接口/数据设计。
- [x] M3：形成 BDD 场景、严格 TDD 顺序、真实 E2E 路径和测试数据设计。
- [x] M4：完成结构校验、一致性复核、验证报告和任务收尾阻塞记录。
- [x] M5：二次优化潜在开发问题，补齐状态枚举、目录授权时序、幂等冲突、命令工作目录和测试数据清理门禁。
- [x] M6：将浏览器本地目录写入门禁沉淀到现有 E2E 长期规则和经验索引。
- [x] M7：按严格 TDD 完成 schema 切片，新增 `dcc_nas_control_audit_file` 明细表、DO、Mapper、迁移测试和测试 schema。
- [x] M8：优化开发文档潜在问题，补齐二进制/分块下载、本地目标已存在、路径过长、状态流转、并发处理和 content 权限校验门禁。
- [x] M9: Strengthen executable docs for files page API, recognition status split, import-selected rejection, content/local-write snapshot binding, no backend mutation on directory cancel, and cross-task/signature-invalid verification.
- [x] M12：优化后续开发潜在问题，补齐识别候选摘要持久化、识别结果写入规则、import 任务快照字段、幂等请求哈希、后端相对路径重算校验和显式选择范围门禁。
- [x] M13：按严格 TDD 完成确定性预识别后端切片，新增 `/files/recognize` 服务实现、候选摘要、原因码、期望本地相对路径和相邻回归验证。
- [x] M14：优化 import-selected 与本地回写开发文档，补齐整体原子拒绝、规范化请求哈希、audit/import 绑定、重复 local-write-result 幂等和冲突终态门禁。

## Expected Verification

- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi`
- 使用 UTF-8 重新读取本任务文档，确认无乱码。
- 核对每个生产行为均有 BDD 场景和 RED/GREEN 实施入口。
- 核对真实 E2E 通过现有前端入口完成，API 仅用于最终只读核验。
- 核对“无法唯一识别”始终落到“未分类/待处理”，不存在默认项目、默认 item、默认分类或静默跳过。
- 核对 RED/GREEN 命令从 `E:\IntRuoyi` 可定位到 `IntRuoyiBackend`、`IntRuoyiFronted` 和 `IntRuoyiBackend/script/tests`。
- 核对 import task 只能在本地目录授权和相对路径校验成功后创建，取消目录选择或浏览器不支持时无后端处理任务。
- Check content and local-write-result bind current user, tenant, import task, audit file, source signature, and local relative path snapshot.
- 核对识别候选摘要必须可持久化，`MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS` 的原因码、候选摘要和期望本地相对路径均可被测试验证。
- 核对 `import-selected` 使用显式 `auditFileId` 列表、后端重算本地相对路径、相同幂等键不同 `request_hash` 返回冲突。
- 核对 `import-selected` 任一选中项无效时整体拒绝且无部分 `SELECTED`、无半创建任务。
- 核对规范化 `request_hash` 对 `selectedFiles` 顺序不敏感，重复 `auditFileId` 在 hash 前失败。
- 核对 audit 明细与 import task/item 绑定可查询，重复活动绑定被拒绝。
- 核对重复 local-write-result 幂等返回且不重复触发 DCC 归档，冲突终态回写被拒绝。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" test`
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/database-schema-evidence.md`

## Current Status

in_progress

设计文档、BDD/TDD/E2E 验收文档、潜在问题优化、M7 schema 明细切片、M11 files page API 切片、M13 确定性预识别后端切片和 M14 import-selected/local-write 文档门禁优化已完成验证；本轮文档补齐了整体原子拒绝、规范化请求哈希、audit/import 绑定和重复回写幂等边界。后续仍需按文档继续实现 import-selected、本地写入回写、content 二进制下载、前端和真实 E2E。最终 `completed` 状态暂不标记：当前工作区存在任务开始前的并发脏文件且分支 `int_main` 已 ahead 2，按项目 Git/closeout 规则需要先单独处理脏工作区基线和 push 阻塞，不能把本任务与其它并发任务资产混在一个收尾提交里。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。路径或名称无法唯一识别时进入正式的“未分类/待处理”业务状态，不视为 fallback。
- `是否从根因和长期维护角度解决`：是。设计将识别、下载、归档、待处理和重试建模为可审计状态与正式服务边界。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`：文件分类必须来自正式 DCC 文件分类树，自动归类不得写回“未分类文件类型”后宣称成功。
- `docs/database-rules.md#DCC 文件类别规则种子门禁`：`dcc_file_category_match_rule` 缺失、歧义、未知类型或插入不完整必须 fail fast，不得用硬编码 fallback 或直接 SQL 修受控文件分类。
- `docs/e2e-rules.md#规划型 E2E 前置与业务 RED 分离门禁`：本任务仅输出设计和验收 gate，不提前实现生产代码；后续实施必须先通过前置 RED。
- `docs/e2e-rules.md#浏览器本地目录写入门禁`：涉及 `showDirectoryPicker`、本地目录授权和本地写入结果回写时，必须验证目录授权前无后端写入任务、`LOCAL_WRITTEN` 前无正式归档、取消授权无 import task。
- `docs/e2e-rules.md#Element Plus 表格选择门禁` 与真实 E2E 规则：后续写入型验证必须按页面可见业务唯一文本选择文件，API 仅用于最终只读核验。
- 严格 no-fallback 门禁：无法唯一判断项目代码、item 或分类时进入正式 `未分类/待处理` 状态，不允许默认项目、默认 item、默认分类、ZIP 降级或静默成功。

## Acceptance Outputs

- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md`

## Cleanup Keep

- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/database-schema-evidence.md
- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/backend-api-evidence.md
