# 展厅旧产品编号邻近编号推理补齐

## 任务目标

- 在英文名严格唯一补齐之后，对剩余同中文、同英文但多候选的旧编号，使用编号邻近关系做可审计推理补齐。
- 仅当 `product_NNN` 与候选 `INT-M` 的数字距离存在唯一最小值，且最近距离不超过 1 时生成映射。
- 不恢复导入名称自动匹配，不直接写库，只生成受保护 SQL 与证据报告。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文、SQL、Markdown、Python 读写均使用 UTF-8 显式路径，不使用 `&&`。
- 项目经验索引：已读取 `docs/experience-index.md`；本任务不涉及登录、真实 E2E、服务器、发布、备份或 worktree 合并。
- 数据库回填门禁：已读取 `database-schema-delivery` 与 `database-contract.md`；SQL 必须有数据安全分析、回滚方案、BDD、RED/GREEN 与验证证据。
- 严格 TDD：先补失败测试，再最小实现邻近推理规则；不得用模糊匹配、默认成功或 fallback 掩盖不可判定映射。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。邻近推理是本轮明确规则，且只在同中文、同英文、多候选时用唯一最近编号打破平局。
- 是否从根因和长期维护角度解决：是。补齐权威字段 `showroom_product.legacy_product_code`，导入链路继续只依赖该字段。
- 是否存在临时补丁或绕过：否。不改产品名称、不改导入匹配逻辑、不直接写库。

## 里程碑

- [x] 建立任务文档、BDD 场景、数据库证据模板。
- [x] 补充邻近编号推理 RED 测试。
- [x] 实现严格邻近推理 SQL 生成器逻辑。
- [x] 生成正式 SQL 和报告，确认新增 4 条映射。
- [x] 运行脚本测试与后端导入回归。
- [x] 收尾清理预览并单独提交本任务文件。

## 预期验证

- `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py`
- `python script/showroom_generate_english_name_legacy_mapping_sql.py`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test`

## 当前状态

- 已完成：邻近编号推理已生成，脚本测试、后端导入回归和收尾清理预览均通过。本任务不执行 SQL 写库。

## 生成结果

- 更新 SQL：`sql/mysql/20260706_showroom_legacy_product_code_english_name_backfill.sql`
- 更新报告：`doc/tasks/20260706-showroom-legacy-code-english-name-backfill/english-name-backfill-report.json`
- 总映射：12 条，其中 8 条为英文名唯一匹配，4 条为邻近编号推理。
- 新增邻近推理映射：
  - 租户 1：`product_066 -> INT-67`、`product_149 -> INT-150`
  - 租户 122：`product_066 -> INT-67`、`product_149 -> INT-150`
- 继续阻塞：12 条，仍无唯一英文/中文/邻近编号可判定。

## 验证结果

- RED: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> FAIL，当前生成器仍将同中英文多候选记录为 `ENGLISH_NAME_MATCH_NOT_UNIQUE`。
- GREEN: `python -m pytest script/tests/test_showroom_english_name_legacy_mapping_generator.py` -> PASS，6 tests。
- GREEN: `python script/showroom_generate_english_name_legacy_mapping_sql.py` -> PASS_WITH_BLOCKERS，生成 12 条 SQL，12 条继续人工确认。
- GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS，53 tests。
- CLEANUP-PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-showroom-legacy-code-proximity-backfill --mode preview` -> READY，delete `<none>`，blocked `<none>`。

## Cleanup Keep

- `doc/tasks/20260706-showroom-legacy-code-proximity-backfill/database-schema-evidence.md`
