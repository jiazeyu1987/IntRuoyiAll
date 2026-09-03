# IntRuoyi Database And SQL Rules

## 触发场景

- 写 SQL、迁移、菜单、权限、租户绑定、schema 相关代码或数据修复脚本前，必须先读取本文件。
- 涉及真实数据库连接、远端数据库或发布数据变更时，还必须读取 `docs/server-access.md` 和 `docs/release-backup-restore.md`。

## Schema 核对

- 写 SQL 前必须用当前真实库或当前迁移文件核对表结构。
- 优先使用 `SHOW TABLES`、`DESCRIBE <table>`、已有 migration、mapper XML、现有 SQL 模板或测试夹具作为证据。
- 不得仅凭 DO 类名、字段猜测、历史记忆或旧项目文档编写运行 SQL。

### 运行态迁移漂移系统异常门禁

- Trigger: 页面或接口在当前代码已支持的路径上提示 `系统异常`，后端栈包含缺表、缺列、`doesn't have a default value`、`cannot be null`、旧索引冲突，或源码已有对应正式迁移但运行库 schema 可能滞后。
- Preflight check: 先从后端失败栈冻结首个数据库异常、Mapper 与目标表，再以当前后端 Java 进程实际启动参数/运行态数据源作为真实连接库，不能只看 `application-local.yaml` 或默认配置；随后用 `information_schema.columns/statistics` 或 `SHOW COLUMNS/INDEX` 对比当前运行库和目标正式迁移；同时确认迁移 metadata、`dependsOn` 和 release migration policy gate 通过。不得先改业务代码适配旧库。
- Generated-column check: 正式 MySQL 中的 `GENERATED ALWAYS` 列只能由数据库计算，业务 INSERT/UPDATE 不得显式写入。若测试 H2 schema 为普通列，必须用静态 SQL 合同或 MySQL 迁移合同补位，防止 H2 通过但运行库报 `The value specified for generated column ... is not allowed`。
- Blocker: 无法确认当前后端实际连接库、目标迁移依赖未满足、运行态表结构与迁移前置不一致、迁移会破坏现有唯一性或历史数据，或只能通过默认值、吞异常、伪造上下文继续提交时必须停止。
- Verification: 迁移前用可重复运行的运行态 schema 契约记录 RED；执行正式迁移后用同一契约记录 GREEN，并运行目标服务回归和不写基线业务数据的真实页面复验。成功写入型 E2E 仍须遵守测试租户、任务自有数据和明确授权门禁。
- Diagnosis order: HTTP 200 不能证明接口成功；必须同时记录业务码/消息、Mapper 首个数据库异常和真实连接库。若本机重启脚本或运行 Jar 覆盖了数据源地址，迁移也必须打到该运行库；配置文件库迁移成功不代表页面运行库已修复。若订单初始化、排产工单主列表、个人中心聚合页或批记录建立链接的任一子请求返回业务码 500 且日志为缺列、字段过短或数据截断，先修复运行库迁移漂移和字段容量，再判断前端错误归属；不要通过隐藏该子请求错误、返回空数据或截断业务字段编码掩盖 schema 缺口。
- Policy scope: 完整 SQL 根目录门禁若被无关文件阻断，不得修改无关迁移或绕过记录；应冻结目标迁移的完整 dependsOn 闭包单独核验并同时记录根目录门禁阻断，未通过的完整门禁不能宣称全库发布就绪。
- Forbidden action: 禁止在源码已有正式迁移时新增业务 fallback、把空业务上下文伪造成默认 ID、手工只改单列而遗漏生成列/索引/相邻表、仅凭迁移文件存在宣称运行态已修复，或在未授权的 admin 基线租户自动重放正式写请求。
- Evidence: `doc/tasks/20260809-fix-frontline-chenli-submit-system-error/verification-report.md`；`doc/tasks/20260826-user-profile-system-error/verification-report.md`；`doc/tasks/20260826-schedule-order-system-exception/verification-report.md`；`doc/tasks/20260830-dcc-process-device-type-parameter-catalog/verification-report.md`。

### 一对多读模型聚合门禁

- Trigger: 时间轴、看板、列表页、详情摘要或报表读模型需要 JOIN 审核记录、修改历史、附件、字段明细、分配记录等一对多子表。
- Preflight check: 先判断主列表期望是一行业务主对象还是一行子明细；若期望一行主对象，子表必须在子查询中按租户和主对象 ID 聚合后再 JOIN，列表分页 count 与 page 查询必须使用同一主对象口径。
- Blocker: 直接把一对多子表 JOIN 到主列表导致主对象重复、pageSize 被子表数量挤占、count 与列表行数口径不一致，或缺少租户维度聚合时必须停止。
- Verification: 用静态合同或 SQL/mapper 测试断言不存在直接一对多 JOIN，并断言聚合 `GROUP BY tenant_id, <主对象ID>`；必要时用测试数据覆盖同一主对象多条子记录仍只返回一行。
- Forbidden action: 禁止用前端去重、分页后去重、默认取第一条、隐藏重复行或修改 count 掩盖 SQL 口径错误。
- Evidence: `doc/tasks/20260730-process-pool-f5-f6-implementation/execution-log.md`。

### 高频列表派生状态物化门禁

- Trigger: 高频列表、工作台或分页接口为了判断待办状态、汇总数量或放行状态，在 count/page 查询中反复执行多张明细表的相关子查询、`SUM/EXISTS` 或逐行解析 JSON，且数据量增长后时延明显上升。
- Preflight check: 先列全所有会改变派生结果的正式写入口，确定唯一主对象和正式源事实；在主对象上设计可校验的汇总字段及覆盖列表过滤、排序的组合索引，并保证新增、修改、分配、撤销、放行等入口在同一事务内同步刷新。历史迁移必须明确每类记录的正式数量来源，双来源不一致、数量非正数或来源缺失时 fail fast；仅对经业务语义确认不属于目标列表的历史事件设置明确的不适用状态。
- Blocker: 只优化 SELECT 而未覆盖全部写入口、count 与 page 读取不同口径、运行时仍从 JSON 或明细表重新推断正式汇总、历史记录只能靠猜测数量补齐，或并发更新缺少统一加锁顺序时必须停止。
- Verification: 静态合同断言高频列表不再包含相关汇总子查询和逐行 JSON 解析；单测覆盖新建、部分分配、全部分配、放行、撤销与更正后的状态同步；用真实数据副本连续执行迁移两次并核对字段、索引、状态数量及来源一致性；最后用真实页面记录首屏请求数，并在部署迁移和新后端后单独复测接口时延。
- Forbidden action: 禁止用缓存、前端去重、减少页大小、延长超时、返回默认状态或把历史数量写成猜测值掩盖读模型根因；禁止在未执行正式迁移和重启新后端时宣称运行态接口已经加速。
- Evidence: `doc/tasks/20260813-production-leader-report-management-performance/verification-report.md`。

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
- Verification: 静态合同或 Mapper/集成测试必须同时断言全局只读目录 DO 的租户忽略、SQL 种子租户口径、32/固定行数等源文件基线、以及明细表不会被租户过滤；刷新源 Excel 时必须先记录旧生成器/旧固定行数 RED，再按源文件逐行比对生成 SQL，并同步校正依赖 `original_row_no` 的后续回填映射；有运行态时再用业务租户真实页面/API 证明列表非空且行数一致。
- Forbidden action: 禁止把 `tenant_id=0` 当作业务租户可见的默认值，禁止只改前端空状态或返回空页，禁止把全局目录改成按当前租户复制多份而不说明同步和一致性策略。
- Evidence: `doc/tasks/mes-process-xlsx-sync-20260731/verification-report.md`。

### 数据修复字符串比较排序规则门禁

- Trigger: 数据修复、测试项种子、菜单/权限补齐等 SQL 使用临时表、字面量、用户变量或存储过程局部字符串变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较，尤其包含中文名称、权限字符串、表单名称、测试项名称。
- Preflight check: 写入前用 `information_schema.COLUMNS` 核对目标字符列 `COLLATION_NAME`，并核对连接 `collation_connection`；临时表字符串列必须声明与目标列一致的 `CHARACTER SET` 和 `COLLATE`。存储过程局部字符串变量会继承创建过程时的连接排序规则，与表列比较时必须显式统一 `COLLATE`，或在要求大小写、字节、中英文标点完全一致的冻结身份校验中对两侧使用 `BINARY` 精确比较，避免 `utf8mb4_unicode_ci` 将中英文括号等字符判等导致 UPDATE 被跳过。
- Blocker: MySQL 报 `ERROR 1267 Illegal mix of collations`，或发现临时字符串列与目标字符列排序规则不一致时必须停止并回滚当前事务；MySQL 报 `ERROR 1137 Can't reopen table` 时也必须停止，不能把已提交前后的汇总 SELECT 当作成功证据。
- Verification: 重试前先确认失败事务未提交；修复后记录命中行数、目标行数、字段排序规则和关键文本扫描结果；涉及物料名、菜单名、表单名等字面修正时，最终验收必须使用 `BINARY` 比较或 `HEX()` 证明目标文本字面一致；同一事务内需要多次统计同一临时表时，先 `SELECT COUNT(*) INTO` 过程变量，或拆成多条不重复打开同一临时表的语句。
- Forbidden action: 禁止修改数据库默认排序规则、手改真实表排序规则、扩大 `WHERE` 范围、拆掉精确租户/删除标记条件，或把失败事务当作成功继续执行。
- Evidence: `doc/tasks/20260727-test-management-deterministic-closed-loop/execution-log.md`；`doc/tasks/20260801-smart-seed-collation-fix/verification-report.md`；`doc/tasks/20260802-test-server-replan-protected-task-workstation/execution-log.md`，`20260726_system_codex_smart_scheduling_test_items.sql` 的 `tmp_codex_smart_scheduling_*` 临时表必须显式 `COLLATE=utf8mb4_0900_ai_ci`，防止 `utf8mb4_general_ci` / `utf8mb4_0900_ai_ci` 混用；`doc/tasks/20260811-dcc-qa-backend-persistence/execution-log.md`，压力泵 QA 种子首次因临时工序表与正式表排序规则不一致而整事务回滚，显式统一为 `utf8mb4_unicode_ci` 后幂等迁移通过；`doc/tasks/20260817-generate-current-active-order-pqc-tasks/execution-log.md`，PQC 数据修复存储过程的局部字符串变量继承 `utf8mb4_general_ci`，与 `utf8mb4_unicode_ci` 表列比较触发 `ERROR 1267`，回滚确认后改为两侧 `BINARY` 精确身份比较并通过；`doc/tasks/20260903-align-pick-list-input-materials/verification-report.md`，领料单物料名修正时 `utf8mb4_unicode_ci` 将中英文括号判等，改用 `BINARY` 后完成字面修正与验收。

### MySQL 存储过程标识符长度门禁

- Trigger: required SQL、seed migration 或 schema migration 创建、调用或删除 MySQL 存储过程，尤其过程名由模块、业务对象和日期拼接。
- Preflight check: 发布前枚举每个 `CREATE PROCEDURE`、`CALL`、`DROP PROCEDURE` 标识符；同一迁移的三类引用必须使用同一稳定名称，且每个名称长度不超过 MySQL 64 字符限制。涉及中文模板或业务文案时，过程标识符仍使用 ASCII 安全短名，并由静态回归和隔离 MySQL 首次/重复执行共同覆盖。
- Blocker: MySQL 返回 `ERROR 1059 Identifier name ... is too long`、CREATE/CALL/DROP 名称不一致、无法证明全部标识符长度安全，或只通过文本替换但未运行隔离迁移回归时，必须停止发布并生成新的 releaseTag。
- Verification: 静态测试解析过程声明与调用并断言长度和一致性；隔离 schema 首次、重复执行目标迁移，验证目标 seed 只写入预期行、过程已清理、隔离 schema 已删除；随后运行 migration policy gate。
- Forbidden action: 禁止手工修改测试/正式库过程名、迁移 ledger 或 release lock，禁止复用失败 releaseTag，禁止只缩短 CREATE 而遗漏 CALL/DROP，禁止把过程名错误解释为环境偶发。
- Evidence: `doc/tasks/20260902-dcc-business-event-notify-template-identifier-fix/`，注册证业务事件通知模板迁移因超长过程名在测试服发布失败；改用统一短名并通过静态、隔离 MySQL 和 migration policy 验证。

### 数据修复 DML 影响行数读取顺序门禁

- Trigger: 数据修复事务在 `INSERT`、`UPDATE` 或 `DELETE` 后同时需要断言 `ROW_COUNT()`，并读取 `LAST_INSERT_ID()`、执行 `SET`、`SELECT` 或其它会改变会话诊断值的语句。
- Preflight check: 每条 DML 后必须第一时间执行 `SET @affected_rows = ROW_COUNT()` 保存影响行数；需要自增 ID 时，只能在影响行数保存完成后读取 `LAST_INSERT_ID()`，后续断言必须使用已保存变量。
- Blocker: 在读取 `ROW_COUNT()` 前执行任何其它语句，或事务断言得到与 DML 预期不符的影响行数时必须停止并确认事务未提交；不得把诊断值被覆盖误判为业务前置条件失败。
- Verification: 至少覆盖一次预期影响行数断言、目标主键/业务键查询和失败事务回滚核对；若失败发生在 `COMMIT` 前，必须重新查询所有目标表，证明没有部分业务写入。
- Forbidden action: 禁止在 DML 与 `ROW_COUNT()` 之间读取 `LAST_INSERT_ID()`、执行额外 `SET/SELECT` 或依赖客户端输出；禁止删除影响行数断言来让脚本继续执行。
- Evidence: `doc/tasks/20260807-pressure-pump-equipment-ledger-correction/execution-log.md`。

### 数据迁移多语句原子性门禁

- Trigger: 同一业务迁移需要连续执行两条或更多相互依赖的 `INSERT`、`UPDATE` 或 `DELETE`，任一语句失败会造成半迁移、类型和值不一致或新旧业务键并存。
- Preflight check: 先完成缺表、缺列、重复业务键和影响范围检查，并断言目标业务组至少存在一组且每组待迁移参数数量完整；再用 `START TRANSACTION/COMMIT` 包裹全部关联 DML。静态合同必须断言所有关联 DML 位于同一事务内，并覆盖“目标零行”明确失败。
- Blocker: 目标表不支持事务、目标业务组零行却允许迁移成功、目标组参数数量不完整、脚本在关联 DML 之间执行隐式提交 DDL、无法提供迁移前精确范围快照，或失败后不能证明连接关闭会回滚未提交事务时必须停止。
- Verification: 先用缺少事务或缺少目标零行拦截的脚本得到 RED，再补事务与 fail-fast 预检得到 GREEN；执行后按迁移前冻结主键逐行核对全部关联字段，并通过真实页面或正式读接口证明运行态没有半迁移或零行假成功。
- Forbidden action: 禁止依赖“通常不会失败”拆开提交关联 DML，禁止第一条成功后用第二条重试脚本补数据，禁止以页面只显示其中一部分字段掩盖迁移不完整。
- Evidence: `doc/tasks/20260811-fine-wash-cleaning-params/execution-log.md`；`doc/tasks/20260811-cleaning-process-medium-temperature/execution-log.md`。

### 旧表单模板绑定切换表单中心门禁

- Trigger: 旧表单模板、`form_template_id`、`last_published_template_version_id`、`FORMTPL:*`、`formBindings` 切换到统一表单中心或批记录表单列表。
- Preflight check: 先冻结旧模板版本、路线绑定表、路线版本快照和 Jimu 报表四处影响范围；目标报表 ID 必须由旧模板版本 ID 生成或读取稳定映射，不能按中文表单名、版本文本或当前最新模板猜测。迁移必须同时生成表单中心报表元数据、Jimu 设计器记录，并把路线快照中的旧 `formBindings` 转成 `batchRecordReports`。
- Blocker: 旧模板版本未发布、源文件或 Jimu schema 缺失、目标 `FORMTPL:*` 报表冲突、同一模板版本映射多个槽位、路线快照中旧绑定无法找到目标报表元数据、Jimu 报表未生成或 JSON 无效时必须停止。
- Verification: 执行前用静态合同记录 RED；执行后核对旧绑定清零、迁移绑定数量、旧模板版本 `BOUND`、路线快照无旧 `formTemplateId`、表单中心列表可查 `FORMTPL:*` 且真实页面能打开目标表单。
- Forbidden action: 禁止只更新路线绑定表而不更新路线快照，禁止保留运行态继续把已迁移行当旧 `formBindings` 输出，禁止用 API-only 或数据库行存在代替真实页面列表验证，禁止删除旧模板字段来掩盖追溯关系。
- Evidence: `doc/tasks/20260829-switch-old-form-template-bindings/verification-report.md`。

### 表单中心批记录项目代码数据修复门禁

- Trigger: 表单中心列表项目代码列、`mes_pro_batch_record_report.project_code`、批记录报表改绑定、按产品名称修正批记录显示项目代码、批记录单元格链接进入时的 DCC 项目代码上下文。
- Preflight check: 先区分列表显示字段和路线正式 DCC 绑定：表单中心列表展示来自批记录报表 `project_code`，路线/单元格链接上下文可能来自 `mes_pro_route_dcc_project_binding.dcc_project_code_id` 或路线版本身份。数据修复前必须冻结目标租户、目标产品、目标报表主键、当前项目代码、目标 DCC 项目代码在同租户启用且未删除；中文产品名只能用于缩小候选范围，最终写入必须锁定主键集合和当前值。若用户同时要求点击链接自动选择 DCC 项目代码，必须另行核对路线绑定和版本路线身份，不能把列表字段更新当作路线绑定已完成。
- Blocker: 目标租户或目标报表集合无法唯一冻结、目标 DCC 项目代码在同租户缺失/禁用/已删除、同一产品下存在非目标项目代码且未获授权、或链接上下文需要改路线绑定但当前任务未授权时必须停止。
- Verification: 执行前记录目标范围不满足期望项目代码的 RED；写入事务必须保存 `ROW_COUNT()` 并断言冻结行数、影响行数、写入后目标代码数和旧代码剩余数；写入后按租户和槽位复核列表字段，并在运行态可用时用真实表单中心页面筛选目标产品确认项目代码列显示正确。
- Forbidden action: 禁止按 DCC 项目中文名反推项目代码、禁止只改路线绑定就宣称列表列已变、禁止只改列表 `project_code` 就宣称链接上下文或路线正式绑定已变、禁止扩大到其它租户/其它产品/空项目代码行，禁止用 API-only 代替可访问页面的列表显示验证。
- Evidence: `doc/tasks/20260831-bind-pressure-pump-idi/verification-report.md`。

### DCC 文件类别规则种子门禁

- Trigger: DCC 项目代码文件分类、`dcc_file_category_match_rule`、OQ/PQ、零配件图纸、类别规则 seed、批量识别 `AMBIGUOUS` / `UNCLASSIFIED` 根因修复。
- Preflight check: 写类别规则 seed 前先核对目标 `dcc_file_category` 启用且未删除类别是否存在、同租户同名类别是否唯一、规则表是否有唯一键防重复、测试 schema 是否同步；规则 seed 必须通过存储过程或等价机制在类别缺失、歧义或插入不完整时 fail fast。
- Blocker: seed 在目标类别不存在时仍成功、同一租户同名类别无法唯一定位、规则表缺唯一键、未知 `match_type` 被默认成功、或迁移直接更新 `dcc_controlled_file` 分类字段时必须停止。
- Verification: 运行目标 JUnit 覆盖 OQ/PQ 明确规则优先、图纸扩展名规则和泛化同分歧义保留；运行对应 `script/tests/test_dcc_file_category_match_rule_sql.py`、`run-release-migration-policy-gate.py`，并记录 backend/database evidence validator PASS。
- Forbidden action: 禁止直接 SQL 修 `dcc_controlled_file` 分类、禁止循环单文件 API 打补丁、禁止 seed 静默插入 0 行、禁止把 `AMBIGUOUS` / `UNCLASSIFIED` 当成功、禁止用硬编码 fallback 掩盖类别规则缺口。
- Evidence: `doc/tasks/20260731-dcc-file-category-rules/verification-report.md`。

### DCC 上传大小策略默认种子门禁

- Trigger: DCC 上传、`upload-preview`、`DCC upload size policy is missing or invalid`、`dcc_controlled_file_upload_policy`、上传大小策略 seed、`SOURCE` / `DRAWING_PDF` / `TRAINING_RECORD` / `EXTERNAL_REVIEW_OUTPUT`。
- Preflight check: 修复上传大小策略缺失时，先核对 `dcc_controlled_file_upload_policy` 和 `dcc_file_category` 当前迁移结构；正式方案应补可发布的目的级或全局策略种子，不得放宽 `DccUploadSizePolicyService` 的 fail-fast 校验。种子必须幂等、按租户写入、只在缺有效 `GLOBAL` 或同目的 `PURPOSE` 策略时插入，并通过存储过程或等价机制在表缺失或插入不完整时 fail fast。
- Blocker: 上传路径靠 catch、默认成功、默认 maxBytes、前端隐藏错误或临时 API-only 配置消除提示；SQL 缺 release-migration 元数据；策略 seed 覆盖/删除用户已有策略；或没有覆盖全部受支持上传 purpose 时必须停止。
- Verification: 运行 `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_upload_size_policy_seed_sql.py`、`python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --output <task-dir>\migration-policy-gate.json`，并复跑 DCC 上传大小策略服务/上传预览目标 JUnit。
- Forbidden action: 禁止把缺策略改成运行时 fallback，禁止直接改远端库不留迁移，禁止扩大为无上限上传，禁止更新或删除既有策略来绕过唯一键，禁止用类别级上传权限证明大小策略正确。
- Evidence: `doc/tasks/20260803-dcc-upload-size-policy-fix/verification-report.md`。

### DCC 项目代码 MDM 产品建档绑定门禁

- Trigger: DCC 产品立项、产品建档申请、`dcc_product_onboarding_request`、`dcc_project_code.product_master_id`、MDM 产品绑定、受控文件提交需要按项目代码带出产品主数据。
- Preflight check: 修改 schema、服务或页面前，必须同时核对 DCC 项目代码表、MDM 产品主数据、建档申请状态机、受控文件提交来源和 DCC 测试 fixture；审批通过生成项目代码时，`productMasterId` 必须来自启用 MDM 产品或审批阶段正式创建的 MDM 产品；审批阶段重复项目代码校验必须排除当前待审批申请自身，但继续拦截其它待审批申请和已存在项目代码。
- Blocker: 缺申请表、缺项目代码 MDM 绑定字段、目标项目代码已存在、其它待审批申请重复、审批把当前申请自身误判为重复、MDM 产品禁用或缺正式 DCC 产品编号、受控文件提交只能从前端 payload/项目名/空值推断产品时必须停止。
- Verification: 至少运行产品建档服务测试、受控文件提交 MDM 绑定测试、聚焦 schema 测试、前端静态契约和 backend/database/frontend evidence validator；审批重复校验回归必须覆盖“当前待审批申请自身不算重复”；真实写入 E2E 只有在确认本机运行态、测试租户/账号和可清理任务数据后执行。
- Forbidden action: 禁止用 DCC 产品目录、`formBindings`、默认项目代码、前端文案、空 `productMasterId`、直接 SQL 补字段、API-only 审批或 mock MDM 产品替代正式建档审批和 MDM 主数据绑定。
- Evidence: `doc/tasks/20260803-dcc-product-onboarding-flow/verification-report.md`。

### 中文菜单名称 ASCII 安全迁移门禁

- Trigger: 菜单、权限、租户套餐或动态路由 SQL 需要写入中文入口名称，尤其通过 MySQL 客户端、Docker `mysql < file.sql`、PowerShell/stdin 或发布迁移执行 `system_menu.name` 更新。
- Preflight check: 中文目标值必须有 ASCII 安全写入方案，例如 `CONVERT(UNHEX('<utf8-hex>') USING utf8mb4)`，或先明确证明客户端连接已使用 `utf8mb4`；若该表达式参与 `SET`、`WHERE`、存储过程局部变量比较或执行后断言，必须显式统一到目标列排序规则，例如 `COLLATE utf8mb4_unicode_ci`；目标行必须用稳定主键加权限/路径等字段精确锁定。
- Blocker: 执行后 `HEX(name)` 不是预期 UTF-8、出现 mojibake/问号、目标行定位不唯一、只验证页面文案但未核对运行库 HEX，或 SQL 缺少 release migration 元数据时必须停止。
- Verification: 记录修复前旧值或乱码 HEX、修复后 `HEX(name)`、目标行 `permission/path/component/component_name/deleted` 不变、目标列排序规则与 SQL 字符串表达式排序规则一致、聚焦 migration policy gate，以及真实页面动态菜单不再显示旧名称。
- Forbidden action: 禁止用前端硬编码标题遮盖动态菜单旧值；禁止直接执行含中文字符串字面量的 SQL 后不复核 HEX；禁止扩大 `WHERE` 范围或改角色/租户绑定来掩盖菜单名未更新。
- Evidence: `doc/tasks/20260728-fix-product-menu-title-runtime/execution-log.md`；`doc/tasks/20260829-form-center-unified-import-int-main/execution-log.md`，表单中心菜单改名首次执行后数据已更新但过程内校验因 `utf8mb4_0900_ai_ci` / `utf8mb4_unicode_ci` 混用失败，补充显式 `COLLATE utf8mb4_unicode_ci` 后幂等通过。

### 动态菜单跨父级移动路径保持门禁

- Trigger: 将已有动态菜单从一个父菜单移动到另一个父菜单、调整同级顺序，且要求点击后继续进入原页面地址。
- Preflight check: 先冻结移动前的完整路由、父级路径和子菜单 `path`，再按新父级路径计算移动后的子菜单 `path`；同时核对目标同级不存在完整路径冲突，并冻结菜单 ID、`type`、`component`、`component_name`、`permission`、按钮权限子菜单、角色菜单绑定和租户套餐绑定。菜单 `component` 指向前端页面时，还必须核对对应页面和正式 API 源文件存在；迁移应只接受明确的移动前状态或目标最终状态，缺少正式菜单、正式页面链路或出现冲突时 fail fast。
- Blocker: 新父级与旧父级路径不同但仍沿用旧相对子路径，导致完整 URL 漂移；目标同级已占用计算后的路径；菜单组件或权限契约无法唯一确认；前端页面/API 源文件缺失但仍要保留为可见入口；或必须改角色、套餐绑定才能掩盖菜单 ID 变化时必须停止。
- Verification: 先用聚焦迁移合同记录 RED/GREEN，断言父级、同级 `sort`、目标 `path`、组件和权限保持契约；运行 release migration policy gate 的完整依赖闭包；在真实库幂等执行后比对按钮权限子菜单数、角色绑定和套餐绑定不变；最后使用 fresh 登录按真实层级逐级展开父菜单，确认层级与同级顺序，点击菜单并断言 URL 仍为移动前完整地址。动态菜单缓存核对同时遵守 `docs/frontend-development.md#动态菜单真实可见性缓存门禁`。
- Forbidden action: 禁止只改前端静态路由或硬编码侧边栏入口，禁止为保留地址新增重定向 fallback，禁止删除重建菜单造成 ID 和授权漂移，禁止用旧登录会话、直接 URL、API-only 或数据库结果代替真实侧边栏点击验证。
- Evidence: `doc/tasks/20260813-move-form-template-menu/verification-report.md`；`doc/tasks/20260829-registration-certificate-menu-hierarchy/verification-report.md`。

### 动态菜单入口隐藏与运行权限隔离门禁

- Trigger: 用户要求删除、隐藏或下线左侧动态菜单/页签，但对应菜单树下仍有按钮权限、后台运行权限、实例处理或其它非页面能力需要保留。
- Preflight check: 先区分 `type=1/2` 的可见目录/菜单和 `type=3` 的按钮权限；冻结目标可见入口、子入口、按钮权限、角色绑定、租户套餐绑定和相邻保留入口。仅需移除页面入口且运行权限仍需保留时，应只调整目标可见菜单的 `visible/always_show`，不得连带删除运行权限或业务数据；若入口本身缺少正式页面链路且用户明确要求删除，应按菜单 ID 清理角色授权和租户套餐中的有效绑定，避免权限树继续暴露无效入口。
- Blocker: 无法确认子菜单是否仍被后台运行调用、必须删除 `type=3` 权限才能让页面消失、相邻保留菜单与目标父菜单仍存在依赖、用户未明确授权删除缺页入口，或只有前端硬编码隐藏方案时必须停止。
- Verification: 聚焦迁移合同先记录 RED/GREEN；运行完整 migration policy dependency closure；真实库执行后核对目标入口不可见、应保留的按钮权限和授权绑定不变、应删除的可见入口不再出现在角色/套餐中；fresh 登录展开父菜单，确认目标入口消失、相邻保留入口仍可点击并进入原页面。
- Forbidden action: 禁止把“删除页签”直接解释为删除表单/实例/权限数据，禁止软删除父菜单导致运行权限树丢失，禁止只删前端静态路由或用 CSS 隐藏，禁止用旧登录会话或直接 URL 代替真实侧边栏验证，禁止用空页面、默认跳转或重定向 fallback 代替正式删除或正式补页。
- Evidence: `doc/tasks/20260813-remove-form-center-menu/verification-report.md`；`doc/tasks/20260829-registration-certificate-menu-hierarchy/verification-report.md`。

### 系统角色菜单授权 tenant 1 admin 门禁

- Trigger: 新增或收敛 `system_role`、`system_role_menu`、`system_user_role`、动态菜单权限角色、admin 授权、只允许特定角色看某菜单/页签、授权公司菜单、关联公司菜单、外部工具入口只允许专用角色可见，且迁移通过 `system_tenant_package.menu_ids` 扫描目标租户。
- Preflight check: 写角色/菜单迁移前，必须核对 tenant 1 的 `system_tenant.package_id` 是否能通过套餐表命中；若 admin 用户需要被赋权，迁移必须显式把 tenant 1 纳入目标角色集合，不能只依赖套餐 menu_ids 扫描。还要先读回 `admin` 的真实有效角色，不能默认它一定是 `tenant_admin`；若实际有效角色是 `super_admin`，也必须把该角色作为正式授权对象写入/回读。普通业务角色不得加入高权限枚举或 admin 特权链路，必须用稳定 `role.code`、目标菜单和精确权限码完成隔离。维护型页面必须同时写入页面 query 权限和按钮 create/update/delete 权限，并把这些权限同步进目标套餐和真实角色菜单；菜单更新必须优先锁定本次拥有的稳定菜单 ID，若按权限名发现其它活动菜单占用同一权限，应先 fail fast，不得用宽泛 `permission IN (...)` 更新历史记录。修复某角色“源码有按钮但页面不显示”时，先核对按钮 `v-hasPermi` 对应权限、目标 `role.code` 是否绑定到正确 `type=3` 菜单 ID；同一个权限码若同时存在页面菜单和按钮菜单，行操作场景只能授权所需按钮菜单，不能顺手授权可见页面菜单或更高处置权限。
- Named account permission repair: 测试服用户明确报出账号但未明确租户时，迁移必须先证明每个启用账号在启用租户中唯一；若重名、缺用户、目标租户缺专用角色或角色未绑定目标菜单链，应 fail fast 要求补充租户/角色事实。只允许按 `tenant_id + role.code` 精确绑定目标用户到目标专用角色，禁止把同名账号跨租户批量授权、改高权限角色或按用户名宽泛授予其它菜单。
- Blocker: tenant 1 `admin` 用户存在但目标角色集合不包含 tenant 1，或没有把 `admin` 的真实有效角色（例如 `super_admin`）纳入目标集合、`system_role_category.code='menu'` 缺失、同租户目标角色 code 重复、维护型页面只新增 query 菜单但缺少 create/update/delete 按钮权限、权限码被其它活动菜单占用、目标固定菜单 ID 的最终契约无法证明、目标菜单仍授权给非目标角色、普通业务角色被接入高权限枚举，或迁移只能让租户套餐角色看到菜单而 admin 用户不能通过标准权限解析拿到权限时必须停止。若为补行操作按钮而会授予独立可见菜单、QA/审核处置权限或无关父级权限，也必须停止并收窄授权集合。
- Verification: 静态 SQL 合同必须断言 tenant 1 显式纳入目标集合、admin 被写入 `system_user_role`、目标页面与按钮权限进入套餐和正式角色、非目标角色仅软删除；同时运行聚焦 role/menu SQL 测试和 release migration policy gate 依赖闭包。若该角色控制动态菜单可见性，还必须用 fresh Playwright 分别验证已授权 admin 可见、未授权账号不可见，并回读 `get-permission-info` 证明菜单已出现在真实权限树里。
- Forbidden action: 禁止把 `tenant_admin`/`super_admin` 菜单绑定当作“只有目标角色可见”的替代；禁止默认 `admin` 一定走 `tenant_admin`；禁止用前端隐藏菜单、硬编码 admin bypass、默认成功权限、broad role grant、宽泛按权限名更新菜单或高权限枚举掩盖 role/menu/user-role 链路未命中。
- Evidence: `doc/tasks/20260829-admin-associated-company-menu-visible/verification-report.md`；`doc/tasks/20260829-erp-invoice-print-role-permission/verification-report.md`；`doc/tasks/20260830-registration-upload-optimization/verification-report.md`；`IntRuoyiBackend/sql/mysql/20260829_erp_finance_invoice_voucher_print_role_permission.sql`；`IntRuoyiBackend/sql/mysql/20260902_erp_finance_invoice_voucher_print_test_server_user_permission.sql`；`doc/tasks/20260901-pqc-management-nonconformance-action-visible/verification-report.md`，PQC 组长不合格审查行按钮缺 `create` 权限时会被 `v-hasPermi` 隐藏，授权需锁定隐藏按钮菜单 ID 并排除独立页面菜单和 QA 处置权限。

### 定时任务迁移业务键与运行态注册门禁

- Trigger: 数据库迁移新增或更新 `infra_job`、Quartz 定时任务、`handler_name`，或触发任务时报 `The job (...) referenced by the trigger does not exist`。
- Preflight check: 写迁移前查询目标环境现有 `infra_job.id` 和 `handler_name`；新增任务不得假设固定自增 ID 可用，必须让数据库分配主键，并用全局稳定且唯一的 `handler_name` 作为幂等业务键。迁移落库后还要核对运行中 Quartz 是否已加载该任务。
- Blocker: 目标固定 ID 已属于其它处理器、目标 `handler_name` 不唯一、迁移按 ID 更新可能覆盖无关任务，或数据库已有任务但 `qrtz_job_details` 尚未注册时必须停止；前者先修迁移，后者按本机运行规则重载正式运行态后再触发。
- Verification: 静态迁移合同必须禁止固定任务 ID，并断言按 `handler_name` 新增/更新；落库后同时核对原有任务不变、新任务业务键与分配 ID、`qrtz_job_details` 注册状态、一次真实触发日志和任务业务结果。
- Forbidden action: 禁止改用另一个猜测的固定 ID、覆盖或删除占用 ID 的现有任务、吞掉 Quartz 未注册异常、把数据库插入成功当作运行态可触发成功，或在任务未注册时循环重试。
- Evidence: `doc/tasks/20260810-kingdee-stock-move-menu-admin-visible/verification-report.md`；`IntRuoyiBackend/script/tests/test_erp_kingdee_stock_move_readonly_sync.py`。

### 跨环境角色权限差异同步门禁

- Trigger: 将本机角色权限平移到测试服/其它环境、修复某角色缺按钮或缺权限，或提出“删除目标环境全部角色后从本机重灌”。
- Preflight check: 角色必须按 `tenant_id + role.code`、菜单按 `permission` 并核对正式菜单 ID 解析，先比较有效 `system_role_menu`、`system_tenant_package.menu_ids`、目标账号全部有效角色和 `infra_release_migration`；冻结正式迁移白名单并备份精确目标行、套餐完整 JSON、角色/用户绑定计数与业务哈希。若迁移会按旧权限继承新权限，必须提前枚举所有源授权和跨租户实际影响。
- Blocker: 同租户角色 code 不唯一、权限对应多个冲突菜单、迁移依赖未应用、套餐 JSON 无效、目标角色/菜单前置缺失、没有可执行精确恢复路径，或差异无法收敛到已测试的正式迁移时必须停止；不得扩大为角色全量覆盖。
- Verification: 迁移前先用目标环境有效权限断言形成 RED，运行迁移合同与正式 preflight；写入后核对目标/禁止权限、精确角色菜单增量、套餐增量、迁移 SHA/状态、跨租户边界，以及 `system_role`/`system_user_role` 计数和绑定哈希不变；权限缓存按租户化/非租户化真实 key 精确失效，最后由目标账号退出后重新登录走真实页面验证。
- Forbidden action: 禁止删除目标环境角色后复制本机数据、复制角色或角色菜单自增 ID、改 `system_user_role` 冒充菜单授权、恢复创建/删除/导出等非白名单权限、清空全库 Redis、读取或复用现有 token 绕过登录、用 API-only 或数据库查询宣称页面 E2E 通过。
- Evidence: `doc/tasks/20260807-test-permission-role-differential-sync/verification-report.md`。

### DCC 菜单恢复与无下载角色隔离门禁

- Trigger: 跨环境同步用户角色、恢复 `文控中心` / `电子签名` / `基础数据` 等动态菜单，或要求账号可浏览 DCC 但继续禁止下载。
- Preflight check: 写 `system_user_role` / `system_role_menu` 前，必须枚举候选角色的全部有效 `system_menu.permission`，并按后端正式下载判定同时核对用户、角色、岗位、部门链的类别和目录规则；至少显式排除 `dcc:controlled-file:directory:manage`、`dcc:controlled-file:access-rule:manage`、`dcc:controlled-file:category:manage`、`dcc:controlled-file:download`，确认目录管理权限是否会旁路类别与目录下载校验。
- Blocker: 候选共享角色包含任一下载旁路权限、任一用户/角色/岗位/部门规则可放行下载、目标账号身份不唯一、菜单白名单包含未启用或已删除菜单，或写入后不能清理目标用户精确角色缓存时必须停止；不得直接绑定该共享角色。
- Verification: 记录变更前后有效用户角色、三个目标根菜单的角色解析来源、角色危险权限计数、动态授权计数、用户/角色/岗位/部门类别与目录下载规则计数，以及精确角色缓存失效结果；没有活动登录 token 时明确记录 UI/API 未验收，不得以匿名请求或 mock 替代。
- Forbidden action: 禁止只检查 `dcc:controlled-file:download` 菜单、只恢复根菜单后推断下载仍被禁止、用共享高权限角色冒充本机入口对齐、清空全库权限缓存，或以用户看不到下载按钮代替后端下载权限链验证。
- Evidence: `doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/verification-report.md`。

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

### 生产组长活跃订单正式移除链门禁

- Trigger: 删除、批量移除或数据修复生产组长活跃订单、`mes_pro_process_pool_active_order`、`REMOVE_ACTIVE_ORDER`。
- Preflight check: 先按 `tenant_id + active_order_id` 冻结目标主键和全字段快照，再通过 `active_order.work_order_id -> work_order.product_id -> md_item` 核对精确产品名、生产组长归属和 `ACTIVE` 状态；同时统计目标订单的 `CURRENT` 报工分配、放行申请和并发恢复进程。用户口述名称与正式产品名不一致时必须先确认目标主键集合，不能模糊匹配。
- Blocker: 目标产品名无法精确解析、生产组长归属不一致、目标版本或状态在快照后变化、同范围恢复任务仍在运行、或正式移除接口不能完成分配失效与维护审计时必须停止。
- Verification: 必须通过正式 `active-order/remove` 业务链逐条移除，并复核目标 `active_status/business_status=REMOVED`、`removed_at` 非空、版本递增、目标 `CURRENT` 分配为 0、`REMOVE_ACTIVE_ORDER/SUCCESS` 审计覆盖全部目标、非目标活跃订单主键集合不变，以及最终登录态列表不再返回目标订单。
- Forbidden action: 禁止物理删除活跃订单，禁止直接 SQL 只改 `active_status` / `business_status`，禁止删除历史分配来伪造失效，禁止用近似产品名扩大范围，禁止把列表接口失败或空响应当作删除成功。
- Evidence: `doc/tasks/20260811-delete-active-pressure-pump-orders/verification-report.md`。

### 工艺路线跨租户导入导出数据包完整性门禁

- Trigger: 删除测试租户工艺路线后从其它租户导入、跨租户验证 `export-import-xlsx` / `import-workbook-xlsx`、或导入报 `工序BOM ... 工序编码 不能为空`、`工艺路线导入导出 Excel`、`工艺路线必须要有关键工序`。
- Preflight check: 在清空目标租户前，先用源租户正式导出接口生成 Excel，并只读校验所有必需 Sheet 的必填字段；尤其核对 `工序BOM.工序编码` 非空，且源库 `mes_pro_route_product_bom.process_id` 能解析到同租户未删除的 `mes_pro_process`，必要时同时确认该工序属于当前路线工序集合；启用路线还必须核对当前 `mes_pro_route_process.key_flag` 恰好 1 个，且 ACTIVE/DRAFT 关系图快照若存在也必须有且仅有一个布尔型 `keyFlag=true`。
- Blocker: 导出工作簿任一必填字段为空、源 BOM `process_id` 为 `0/NULL` 或无法解析正式工序、源/目标主数据编码缺失、启用路线当前表或最新关系图快照缺少关键工序/存在多个关键工序、目标租户已清空但导入失败，必须停止并记录；不得继续宣称一致性已验证。
- Verification: 记录源/目标租户 ID、导出文件路径和字节数、工作簿 Sheet 行数、源异常 BOM 明细、关键工序计数与 END/末道节点证据、正式导入接口响应，以及失败后目标租户路线相关表是否出现部分写入。
- Forbidden action: 禁止手工删除 Excel 中失败行、随机补工序编码、把 `process_id=0` 推断成首工序、把缺失关键工序默认为首工序、改用 API-only 或直接 SQL 伪造导入成功；若目标已清空，不得擅自恢复或继续修改源数据，必须先让用户在“修复源数据后重试”和“按备份恢复目标租户”之间确认。
- Evidence: `doc/tasks/20260730-route-tenant-export-import-consistency/execution-log.md`。

### MES 三页签跨环境同步完整性门禁

- Trigger: 将工序设置、工艺流程、排产工单等 MES 页面数据从本机、其它租户或其它环境同步到测试服/正式服，且用户要求“只同步这些页签、其它不同步”。
- Preflight check: 写入前必须先生成白名单表清单和显式列清单，只读核对源/目标 schema、源数据父子范围、依赖闭包、目标同 ID 业务身份、以及所有白名单外活动引用；排产配置按当前路线工序身份收敛，排产工单按 `scheduleOrderId + routeProcessId` 快照身份收敛；工序设置批记录表单必须按 `batch_record_report_id -> mes_pro_batch_record_report.report_id` 核对正式报表元数据，并继续核对其 `batch_record_definition_id`、`batch_record_version_id` 是否在目标租户可解析；对齐路线工序时必须区分页面可见的有效路线范围与挂在已删除路线下的孤儿历史行，比较口径应使用 `rp.deleted=b'0'` 且 JOIN `mes_pro_route.deleted=b'0'` 的有效路线工序集合。
- Blocker: 目标 schema 不能承载源数据、目标缺正式依赖、同 ID 业务身份不一致、目标白名单外存在活动引用、源数据存在孤儿子记录或删除历史混入范围、或工序设置页面所需批记录报表元数据缺失时，必须停止；不得删除或改写目标数据。
- Verification: 记录源/目标租户、白名单逐表行数、缺失/不一致依赖、白名单外引用表列计数、源关键快照容量、目标 schema 列类型、批记录报表元数据缺失数、备份恢复路径和零写入/事务回滚证据；写入后还必须比对行数、主键集合、业务键和显式列 hash，并单独记录有效路线工序集合与全部 active 路线工序的差异原因；用真实页面验证三页签列表接口不再返回 `系统异常`。
- Forbidden action: 禁止用表单槽位 `formBindings` 补批记录表单、用当前路线重建历史排产快照、自动补默认日历/产能/用户/物料、随机重映射 ID、关闭外键、全库重置、API-only 伪验证或把依赖数据偷偷纳入“页签同步”。
- Evidence: `doc/tasks/20260731-mes-three-tab-test-sync/verification-report.md`。

### 生产用料清单跨环境白名单 upsert 门禁

- Trigger: 将 `mes_kingdee_production_material_list`、生产用料清单、ERP 用料清单、白名单表级 upsert 或类似明细表从本机同步到测试服/其它租户。
- Preflight check: 写入前必须只读核对源/目标 schema、唯一业务键、租户集合、目标原始行数、菜单/权限/同步任务前置，以及测试服备份；关联 ID 不得直接复制本地值，必须按目标租户和正式父表唯一解析；若同租户同生产工单编码存在多条未删除工单，必须按当前排程/任务实际引用的 `work_order_id` 与 PML 归属逐一拆分；若本地缺同租户物料，应先走正式“金蝶产品/商品 -> MES 物料”同步链路，若同步被重复计量单位、重复分类或其它 `selectOne` 唯一性异常卡住，先按引用范围归并正式主数据重复项，再重跑同步，不得手工伪造物料。
- Blocker: 目标表缺列/缺唯一键、租户不匹配、备份失败、目标父表同编码不唯一、缺少同租户物料/工单/BOM、PML 挂在未排程重复工单而当前排程工单缺 PML、物料同步所需单位/分类等正式主数据不唯一、或生成跨租户 `product_id` / `child_material_id` / `work_order_id` / `work_order_bom_id` 时必须停止并修正；不得把跨租户 ID 视为已解析。
- Verification: 写入后必须比对源/目标业务键总数、按租户行数、显式业务字段 hash、staging 表清理状态，并按目标租户复核 `work_order_id`、`product_id`、`child_material_id`、`work_order_bom_id` 均指向未删除正式父表；若修复过单位/分类等主数据重复，必须复核重复项只剩唯一活动记录、原引用已迁移且无孤儿引用；排程日历验证还必须覆盖当前月份接口，防止修完首个缺口后暴露下一张工单缺 PML；页面/API 验证需区分真实登录页 E2E、只读 API、token-bootstrap 页面渲染。
- Forbidden action: 禁止复制本地自增 ID、跨租户引用物料主数据、用全库编码唯一替代同租户唯一、删除目标差异行、绕过正式同步链路手工创建物料、绕过备份、API-only 冒充页面验证、或在验证码阻塞时宣称登录页 E2E 通过。
- Evidence: `doc/tasks/20260801-production-material-list-data-sync-test/verification-report.md`；`doc/tasks/20260801-fix-test-schedule-material-item-mapping/verification-report.md`。

### MES 同名物料路线产品身份收敛门禁

- Trigger: 同一租户存在多个 `mes_md_item.name` 相同的物料，且排产、生产工单、生产组长活跃订单、工艺路线产品绑定或一线运行态因物料 ID 不同出现“活跃订单冻结工序快照不完整”、新增活跃订单失败、路线产品身份不一致、同名物料收敛或压力泵同名物料问题。
- Preflight check: 写入前必须冻结 `tenant_id + item_id + item.code + product_master_id`、目标 `route_id + route.code`、DCC 项目代码与 `product_master_id` 绑定、当前活跃订单 `work_order.product_id`、路线产品 `mes_pro_route_product` 有效绑定数量和冻结工序快照数量；中文名称只能作为排查线索，不能作为更新主键。若同一名称下有多个正式编码，应先确认当前订单实际引用的物料 ID，再判断是补路线产品绑定、迁移引用还是阻塞。
- Blocker: 只能按中文名称定位目标、目标物料缺正式 `product_master_id` 或与 DCC 项目不一致、目标路线有效产品绑定存在 product master 漂移、同一路线同物料已有多条有效绑定、活跃订单引用物料在目标路线下无有效产品绑定、或待修复范围跨租户/跨 product master 时必须停止；不得继续新增活跃订单或用其它同名物料代替。
- Verification: 先用只读 SQL 形成 RED，证明目标订单引用的 `work_order.product_id` 缺少目标路线有效 `route_product`；修复必须事务化、幂等、精确限定租户/路线/物料/DCC 项目/product master，并记录影响行数；修复后复核目标物料有效绑定数为 1、活跃订单冻结快照数量不变且 `active_route_product_binding_count=1`、目标路线 active product master 漂移数为 0，同时运行迁移静态合同和 release migration policy gate。
- Forbidden action: 禁止把所有同中文名物料物理合并、禁用、重命名或批量改工单引用；禁止按 `name` 更新、按当前路线猜测历史订单、复制其它活跃订单快照、补默认物料、前端隐藏错误、API-only 冒充页面验证或让运行态 fallback 到任意同名物料。
- Evidence: `doc/tasks/20260818-pressure-pump-same-name-material-convergence/verification-report.md`；`IntRuoyiBackend/sql/mysql/20260818_mes_pressure_pump_same_name_item_convergence.sql`。

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

### ERP 外部快照同步租户落库门禁

- Trigger: 新增或修改金蝶等外部 ERP 快照表、同步服务、定时 Job、主子表落库或真实账套同步测试。
- Preflight check: 主表和明细表都必须使用租户数据基类；同步入口必须从当前租户上下文取得必填租户 ID，并在主子记录写入前显式赋值。最小测试上下文、定时任务上下文和真实 Web 请求的租户拦截器装配可能不同，不能依赖数据库默认 `tenant_id=0` 或只依赖拦截器补值。
- Blocker: 当前租户缺失、主子任一表写入 `tenant_id=0`、主子租户不一致、或真实同步后无法按租户证明行数和样例归属时必须停止；先清理本任务错误租户数据并修正正式写入链路，不能把全局默认租户当作测试租户成功。
- Verification: 单元测试同时断言主表和明细表租户 ID；真实同步后按 `tenant_id` 分组核对主子表行数、错误租户为 0、任务种子为 0，并从目标租户抽样核对业务字段。
- 异步手动同步门禁：从当前租户页面提交 ERP 同步时，必须把当前租户 ID 与原始处理参数一起编码进任务参数；@TenantJob 收到显式范围时只执行该租户。未携带范围的任务只保留给定时自动同步的全部启用租户语义，页面按钮不得绕过业务入口直接触发通用任务接口。
- Forbidden action: 禁止依赖列默认值、测试专用 SQL 重写租户、查询时忽略租户、或仅凭总行数宣称同步通过。
- Evidence: `doc/tasks/20260813-erp-production-pick-list-sync/verification-report.md`。

## 禁止做法

- 禁止 schema 缺证据时继续执行 SQL。
- 禁止用默认成功、空结果或 mock 数据掩盖缺表、缺字段、缺权限。
- 禁止未授权改远端数据库。
- 禁止把权限缺失误判为前端组件缺失。

## 验证方式

- 记录 schema 核对命令和关键字段证据。
- 记录 SQL 执行目标、租户范围、影响范围和回滚或清理方式。
- 执行后核对受影响行数、菜单权限响应或业务页面结果。
