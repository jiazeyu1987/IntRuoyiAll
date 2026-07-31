# IntRuoyi Database And SQL Rules

## 触发场景

- 写 SQL、迁移、菜单、权限、租户绑定、schema 相关代码或数据修复脚本前，必须先读取本文件。
- 涉及真实数据库连接、远端数据库或发布数据变更时，还必须读取 `docs/server-access.md` 和 `docs/release-backup-restore.md`。

## Schema 核对

- 写 SQL 前必须用当前真实库或当前迁移文件核对表结构。
- 优先使用 `SHOW TABLES`、`DESCRIBE <table>`、已有 migration、mapper XML、现有 SQL 模板或测试夹具作为证据。
- 不得仅凭 DO 类名、字段猜测、历史记忆或旧项目文档编写运行 SQL。

### 一对多读模型聚合门禁

- Trigger: 时间轴、看板、列表页、详情摘要或报表读模型需要 JOIN 审核记录、修改历史、附件、字段明细、分配记录等一对多子表。
- Preflight check: 先判断主列表期望是一行业务主对象还是一行子明细；若期望一行主对象，子表必须在子查询中按租户和主对象 ID 聚合后再 JOIN，列表分页 count 与 page 查询必须使用同一主对象口径。
- Blocker: 直接把一对多子表 JOIN 到主列表导致主对象重复、pageSize 被子表数量挤占、count 与列表行数口径不一致，或缺少租户维度聚合时必须停止。
- Verification: 用静态合同或 SQL/mapper 测试断言不存在直接一对多 JOIN，并断言聚合 `GROUP BY tenant_id, <主对象ID>`；必要时用测试数据覆盖同一主对象多条子记录仍只返回一行。
- Forbidden action: 禁止用前端去重、分页后去重、默认取第一条、隐藏重复行或修改 count 掩盖 SQL 口径错误。
- Evidence: `doc/tasks/20260730-process-pool-f5-f6-implementation/execution-log.md`。

### 只读资源池引用完整性门禁

- Trigger: 资源池、MES 工序、工艺路线资源、报工映射等只读列表复用关系表组装跨主数据读模型，出现 `Missing route`、`Missing item`、`Missing process`、`Missing machinery` 或页面 `系统异常`。
- Preflight check: 行组装前先收集关系表引用的正式主数据 ID，批量读取父表 map，并区分“全量只读资源池”与“指定对象详情/编辑”。全量只读资源池只能展示可解析到正式父表的数据；指定对象详情/编辑若缺正式父表必须 fail fast 并暴露缺失来源。
- Blocker: 单条历史孤儿关系导致整页 500、分页 total/count 包含不可解析关系、在循环中直接 `require(parent)` 拖垮全量列表、或缺失父表来源被改成空名称/未知对象/默认成功时必须停止。
- Verification: 新增回归测试或静态合同覆盖一条有效关系加一条孤儿关系时只读列表返回有效行；同时用登录态 API 和真实页面证明业务码为 `0`、表格有行、无 `系统异常`。
- Forbidden action: 禁止直接 SQL 删除业务关系来掩盖读模型缺陷，禁止 catch 后返回空页，禁止前端隐藏 toast，禁止用“未知路线/未知产品”等默认文案替代正式主数据完整性。
- Evidence: `doc/tasks/20260730-mes-process-mapping-tab/bug-regression-evidence.md`。

### 全局只读 Excel 种子租户边界门禁

- Trigger: MES 工序、产品目录、标准只读目录、Excel/外部文件基线等全局只读种子写入 `tenant_id=0`，但页面在普通业务租户下读取。
- Preflight check: 写全局只读种子前先确认读模型是否应跨租户共享；若种子固定 `tenant_id=0`，对应 DO 或表必须显式 `@TenantIgnore` / `ignore-tables`，并同步确认子表、明细表、分页 count 与 page 查询都不会被当前租户条件过滤。
- Blocker: 主表忽略租户但子表未忽略、SQL 种子写 `tenant_id=0` 但 Mapper 仍受 `tenant_id=<当前租户>` 限制、只在 Mockito 单元测试通过但未覆盖租户过滤、或用复制多租户数据掩盖全局基线设计不清时必须停止。
- Verification: 静态合同或 Mapper/集成测试必须同时断言全局只读目录 DO 的租户忽略、SQL 种子租户口径、32/固定行数等源文件基线、以及明细表不会被租户过滤；有运行态时再用业务租户真实页面/API 证明列表非空且行数一致。
- Forbidden action: 禁止把 `tenant_id=0` 当作业务租户可见的默认值，禁止只改前端空状态或返回空页，禁止把全局目录改成按当前租户复制多份而不说明同步和一致性策略。
- Evidence: `doc/tasks/mes-process-xlsx-sync-20260731/verification-report.md`。

### 数据修复临时表排序规则门禁

- Trigger: 数据修复、测试项种子、菜单/权限补齐等 SQL 使用临时表、字面量或用户变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较，尤其包含中文名称、权限字符串、表单名称、测试项名称。
- Preflight check: 写入前用 `information_schema.COLUMNS` 核对目标字符列 `COLLATION_NAME`；临时表字符串列必须声明与目标列一致的 `CHARACTER SET` 和 `COLLATE`，或在比较表达式上显式 `COLLATE` 到目标列排序规则。
- Blocker: MySQL 报 `ERROR 1267 Illegal mix of collations`，或发现临时字符串列与目标字符列排序规则不一致时必须停止并回滚当前事务。
- Verification: 重试前先确认失败事务未提交；修复后记录命中行数、目标行数、字段排序规则和关键文本扫描结果。
- Forbidden action: 禁止修改数据库默认排序规则、手改真实表排序规则、扩大 `WHERE` 范围、拆掉精确租户/删除标记条件，或把失败事务当作成功继续执行。
- Evidence: `doc/tasks/20260727-test-management-deterministic-closed-loop/execution-log.md`。

### DCC 文件类别规则种子门禁

- Trigger: DCC 项目代码文件分类、`dcc_file_category_match_rule`、OQ/PQ、零配件图纸、类别规则 seed、批量识别 `AMBIGUOUS` / `UNCLASSIFIED` 根因修复。
- Preflight check: 写类别规则 seed 前先核对目标 `dcc_file_category` 启用且未删除类别是否存在、同租户同名类别是否唯一、规则表是否有唯一键防重复、测试 schema 是否同步；规则 seed 必须通过存储过程或等价机制在类别缺失、歧义或插入不完整时 fail fast。
- Blocker: seed 在目标类别不存在时仍成功、同一租户同名类别无法唯一定位、规则表缺唯一键、未知 `match_type` 被默认成功、或迁移直接更新 `dcc_controlled_file` 分类字段时必须停止。
- Verification: 运行目标 JUnit 覆盖 OQ/PQ 明确规则优先、图纸扩展名规则和泛化同分歧义保留；运行对应 `script/tests/test_dcc_file_category_match_rule_sql.py`、`run-release-migration-policy-gate.py`，并记录 backend/database evidence validator PASS。
- Forbidden action: 禁止直接 SQL 修 `dcc_controlled_file` 分类、禁止循环单文件 API 打补丁、禁止 seed 静默插入 0 行、禁止把 `AMBIGUOUS` / `UNCLASSIFIED` 当成功、禁止用硬编码 fallback 掩盖类别规则缺口。
- Evidence: `doc/tasks/20260731-dcc-file-category-rules/verification-report.md`。

### 中文菜单名称 ASCII 安全迁移门禁

- Trigger: 菜单、权限、租户套餐或动态路由 SQL 需要写入中文入口名称，尤其通过 MySQL 客户端、Docker `mysql < file.sql`、PowerShell/stdin 或发布迁移执行 `system_menu.name` 更新。
- Preflight check: 中文目标值必须有 ASCII 安全写入方案，例如 `CONVERT(UNHEX('<utf8-hex>') USING utf8mb4)`，或先明确证明客户端连接已使用 `utf8mb4`；目标行必须用稳定主键加权限/路径等字段精确锁定。
- Blocker: 执行后 `HEX(name)` 不是预期 UTF-8、出现 mojibake/问号、目标行定位不唯一、只验证页面文案但未核对运行库 HEX，或 SQL 缺少 release migration 元数据时必须停止。
- Verification: 记录修复前旧值或乱码 HEX、修复后 `HEX(name)`、目标行 `permission/path/component/component_name/deleted` 不变、聚焦 migration policy gate，以及真实页面动态菜单不再显示旧名称。
- Forbidden action: 禁止用前端硬编码标题遮盖动态菜单旧值；禁止直接执行含中文字符串字面量的 SQL 后不复核 HEX；禁止扩大 `WHERE` 范围或改角色/租户绑定来掩盖菜单名未更新。
- Evidence: `doc/tasks/20260728-fix-product-menu-title-runtime/execution-log.md`。

### 测试管理 schema 迁移门禁

- Trigger: 访问 `系统管理 > 测试管理` 提示 `系统异常`，或修改/运行 `system_codex_test_case`、Codex Runner、测试项分页、测试管理页面相关接口。
- Preflight check: 先用当前真实库或迁移脚本核对 `system_codex_test_case.project`、`node_chain_name`、`node_chain_sort`、`node_chain_execution` 等当前 DO/Mapper 必需字段是否存在，并确认本地 Docker MySQL 已应用对应测试管理迁移。
- Blocker: 当前代码引用的字段在真实库缺失、迁移未应用、迁移测试失败，或只看到前端 toast 而缺少分页 API/DB schema 证据时必须停止。
- Verification: 记录 schema 核对结果、迁移执行目标、对应迁移契约测试结果，例如 `script/tests/test_codex_test_case_project_migration.py` 或 `script/tests/test_codex_test_node_chain_migration.py`，以及真实测试管理页面 E2E 不再出现 `系统异常`。
- Forbidden action: 禁止用前端隐藏错误、后端默认 project、吞掉数据库异常、切换数据源、mock 成功或 API-only 代替真实页面恢复来绕过缺字段。
- Evidence: `doc/tasks/fix-test-management-system-exception-20260726/verification-report.md`；`doc/tasks/20260727-codex-test-node-chain/database-schema-evidence.md`。

### 个人工作台隐藏任务状态迁移门禁

- Trigger: 个人中心、个人工作台、统一待办、`profile-workbench-task-visibility`、`hidden-keys`、页面提示 `待办任务加载失败` 或 `隐藏任务状态：系统异常`。
- Preflight check: 先只读核对当前后端连接库是否存在 `system_profile_workbench_task_visibility`，并确认 `sql/mysql/20260727_system_profile_workbench_task_visibility.sql` 与依赖 `20260708_system_user_table_column_config` 均通过 release migration policy gate。
- Blocker: 目标表缺失、迁移依赖未纳入门禁、迁移契约测试缺失、或只看到前端 alert 而没有接口/DB schema 证据时必须停止。
- Verification: 记录 `information_schema.tables` 表数量、目标列清单、迁移应用目标、`script/tests/test_system_profile_workbench_task_visibility_sql.py` 结果、migration policy gate 结果，以及个人工作台真实页面不再显示加载失败。
- Forbidden action: 禁止让前端忽略隐藏状态接口失败、后端返回空隐藏列表、吞掉 SQL 异常、mock 成功或跳过迁移来让待办列表看似恢复。
- Evidence: `doc/tasks/20260727-todo-task-hidden-status/verification-report.md`。

### 数据修复与写入型 E2E 恢复并发门禁

- Trigger: 数据修复目标同时可能被 Playwright、Codex Runner、`finally` 恢复逻辑或定时测试写入。
- Preflight check: 执行前检查目标测试进程和命令行，确认恢复逻辑已自然结束；删除前重新导出精确范围快照，并把行数、主键边界和全字段校验值绑定到事务断言。
- Blocker: 同范围写入型 E2E 正在运行、目标行数或校验值在快照后变化、或测试恢复逻辑可能重新插入目标数据时必须停止；不得强停不属于当前任务的并发进程。
- Verification: 事务提交后再次检查并发进程和目标范围稳定值；若 E2E 在修复后启动，必须等其恢复完成后复验最终行数和业务字段，不能只记录事务瞬时成功。
- Forbidden action: 禁止在外部恢复任务仍活跃时把一次删除成功宣称为最终完成；禁止扩大删除范围、循环强删或终止无归属依据的并发任务。
- Evidence: `doc/tasks/20260727-delete-duplicate-fill-rules/execution-log.md`。

### 工艺路线跨租户导入导出数据包完整性门禁

- Trigger: 删除测试租户工艺路线后从其它租户导入、跨租户验证 `export-import-xlsx` / `import-workbook-xlsx`、或导入报 `工序BOM ... 工序编码 不能为空`、`工艺路线导入导出 Excel`、`工艺路线必须要有关键工序`。
- Preflight check: 在清空目标租户前，先用源租户正式导出接口生成 Excel，并只读校验所有必需 Sheet 的必填字段；尤其核对 `工序BOM.工序编码` 非空，且源库 `mes_pro_route_product_bom.process_id` 能解析到同租户未删除的 `mes_pro_process`，必要时同时确认该工序属于当前路线工序集合；启用路线还必须核对当前 `mes_pro_route_process.key_flag` 恰好 1 个，且 ACTIVE/DRAFT 关系图快照若存在也必须有且仅有一个布尔型 `keyFlag=true`。
- Blocker: 导出工作簿任一必填字段为空、源 BOM `process_id` 为 `0/NULL` 或无法解析正式工序、源/目标主数据编码缺失、启用路线当前表或最新关系图快照缺少关键工序/存在多个关键工序、目标租户已清空但导入失败，必须停止并记录；不得继续宣称一致性已验证。
- Verification: 记录源/目标租户 ID、导出文件路径和字节数、工作簿 Sheet 行数、源异常 BOM 明细、关键工序计数与 END/末道节点证据、正式导入接口响应，以及失败后目标租户路线相关表是否出现部分写入。
- Forbidden action: 禁止手工删除 Excel 中失败行、随机补工序编码、把 `process_id=0` 推断成首工序、把缺失关键工序默认为首工序、改用 API-only 或直接 SQL 伪造导入成功；若目标已清空，不得擅自恢复或继续修改源数据，必须先让用户在“修复源数据后重试”和“按备份恢复目标租户”之间确认。
- Evidence: `doc/tasks/20260730-route-tenant-export-import-consistency/execution-log.md`。

### MES 三页签跨环境同步完整性门禁

- Trigger: 将工序设置、工艺流程、排产工单等 MES 页面数据从本机、其它租户或其它环境同步到测试服/正式服，且用户要求“只同步这些页签、其它不同步”。
- Preflight check: 写入前必须先生成白名单表清单和显式列清单，只读核对源/目标 schema、源数据父子范围、依赖闭包、目标同 ID 业务身份、以及所有白名单外活动引用；排产配置按当前路线工序身份收敛，排产工单按 `scheduleOrderId + routeProcessId` 快照身份收敛；工序设置批记录表单必须按 `batch_record_report_id -> mes_pro_batch_record_report.report_id` 核对正式报表元数据，并继续核对其 `batch_record_definition_id`、`batch_record_version_id` 是否在目标租户可解析。
- Blocker: 目标 schema 不能承载源数据、目标缺正式依赖、同 ID 业务身份不一致、目标白名单外存在活动引用、源数据存在孤儿子记录或删除历史混入范围、或工序设置页面所需批记录报表元数据缺失时，必须停止；不得删除或改写目标数据。
- Verification: 记录源/目标租户、白名单逐表行数、缺失/不一致依赖、白名单外引用表列计数、源关键快照容量、目标 schema 列类型、批记录报表元数据缺失数、备份恢复路径和零写入/事务回滚证据；写入后还必须比对行数、主键集合、业务键和显式列 hash，并用真实页面验证三页签列表接口不再返回 `系统异常`。
- Forbidden action: 禁止用表单槽位 `formBindings` 补批记录表单、用当前路线重建历史排产快照、自动补默认日历/产能/用户/物料、随机重映射 ID、关闭外键、全库重置、API-only 伪验证或把依赖数据偷偷纳入“页签同步”。
- Evidence: `doc/tasks/20260731-mes-three-tab-test-sync/verification-report.md`。

## 租户和菜单权限

- 动态菜单页面交付必须同时核对：
  - 前端组件文件
  - `system_menu.path`
  - `system_menu.component`
  - `system_menu.component_name`
  - `system_menu.permission`
  - 目标租户角色菜单绑定
  - 登录后权限响应
- 写入型数据操作必须确认目标租户，不得污染生产租户、admin 基线数据或无关业务数据。

## 禁止做法

- 禁止 schema 缺证据时继续执行 SQL。
- 禁止用默认成功、空结果或 mock 数据掩盖缺表、缺字段、缺权限。
- 禁止未授权改远端数据库。
- 禁止把权限缺失误判为前端组件缺失。

## 验证方式

- 记录 schema 核对命令和关键字段证据。
- 记录 SQL 执行目标、租户范围、影响范围和回滚或清理方式。
- 执行后核对受影响行数、菜单权限响应或业务页面结果。
