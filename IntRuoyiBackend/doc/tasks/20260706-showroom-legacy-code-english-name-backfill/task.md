# 展厅旧产品编号英文名补齐

## 任务目标

- 为当前 `INT-*` 展厅产品中 `legacy_product_code` 为空的项，按旧底表 `product_*` 英文名严格唯一匹配补齐旧产品编号。
- 保持基础底表导入规则不变：导入仍只通过 `legacy_product_code` 精确映射，不恢复名称自动匹配。
- 只生成可审计、受保护的本地 SQL；本任务不直接操作服务器、正式库或测试库。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；涉及中文、SQL、Markdown、Python 脚本时必须使用 UTF-8 显式读写，不使用 `&&`，不使用 PowerShell 默认重定向写中文。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务不涉及登录、真实 E2E、服务器、发布、备份或 worktree 合并，因此不触发对应高风险门禁。
- 数据库回填门禁：已读取 `database-schema-delivery` 与 `database-contract.md`；回填 SQL 必须有数据安全分析、回滚方案、BDD、RED/GREEN 与验证证据。
- 严格 TDD：先写失败测试，再实现最小脚本，最后跑定向回归；不得用 mock 成功、静默跳过、模糊匹配或 fallback 掩盖不可判定映射。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。只允许英文名规范化后严格相等且唯一；否则阻塞。
- 是否从根因和长期维护角度解决：是。通过生成受保护 SQL 补齐权威字段 `showroom_product.legacy_product_code`，导入链路继续只依赖该字段。
- 是否存在临时补丁或绕过：否。不改导入匹配逻辑、不改产品名称、不直接写库。

## 里程碑

- [x] 建立任务文档、BDD 场景、数据库证据模板。
- [x] 补充英文名唯一匹配 SQL 生成器 RED 测试。
- [x] 实现严格英文名唯一匹配 SQL 生成器与报告。
- [x] 生成正式 SQL 和报告，确认只包含 8 条映射。
- [x] 运行脚本测试与后端导入回归。
- [x] 预览收尾清理并单独提交本任务文件。

## 预期验证

- `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py`
- `python script/showroom_generate_english_name_legacy_mapping_sql.py`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test`

## 当前状态

- 已完成：生成 8 条英文名唯一匹配 SQL，16 条不可唯一项保留 blocker；脚本测试与后端导入回归通过。本任务不执行 SQL 写库。

## 生成结果

- SQL：`sql/mysql/20260706_showroom_legacy_product_code_english_name_backfill.sql`
- 报告：`doc/tasks/20260706-showroom-legacy-code-english-name-backfill/english-name-backfill-report.json`
- 初始英文名唯一可补齐映射：8 条。
  - 租户 1：`product_015 -> INT-15`、`product_081 -> INT-82`、`product_086 -> INT-87`、`product_095 -> INT-96`
  - 租户 122：`product_015 -> INT-15`、`product_081 -> INT-82`、`product_086 -> INT-87`、`product_095 -> INT-96`
- 后续任务 `20260706-showroom-legacy-code-proximity-backfill` 已在同一生成器中加入邻近编号推理，将总 SQL 扩展为 12 条；本节保留初始英文名唯一阶段记录。

## 验证结果

- RED: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> FAIL，生成器不存在。
- GREEN: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> PASS，4 tests。
- GREEN: `python script/showroom_generate_english_name_legacy_mapping_sql.py` -> PASS_WITH_BLOCKERS，生成 8 条 SQL，16 条继续人工确认。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS，53 tests。

## Cleanup Keep

- `doc/tasks/20260706-showroom-legacy-code-english-name-backfill/database-schema-evidence.md`
- `doc/tasks/20260706-showroom-legacy-code-english-name-backfill/english-name-backfill-report.json`
