# 任务：修复 DCC 文件查阅矩阵正式库历史分类重名发布阻塞

## 任务目标

修复 `20260613_dcc_file_view_matrix_seed.sql` 在正式库已有历史同名分类和新矩阵编码分类同时存在时误判为分类歧义的问题，使发布脚本按稳定编码优先解析矩阵分类，并继续保持缺失或真正多编码冲突时 fail fast。

## 经验门禁

- 命中 `docs/experience-index.md`：涉及发布失败、required SQL、正式服数据库写入和 worktree 发布隔离。
- 命中 `release-agent-checklist.md`：required SQL 失败先查 migration / manifest / SQL 契约，不手工改库绕过。
- 命中 `release-build-preflight-lessons.md`：发布失败优先只读定位真实库状态，修复应回到 SQL 契约和门禁测试。
- 命中 `release-backup-restore.md`：code-only 发布仍执行 required SQL，目标环境 schema 与迁移前置必须满足。
- 命中 `worktree-memory.md`：发布输入必须来自干净临时发布 worktree；主工作区脏改不得进入发布包。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。修复为明确的分类解析优先级：编码匹配优先，只有无编码匹配时才使用名称匹配。
- 是否从根因和长期维护角度解决：是。根因为正式库存在历史同名分类，新 SQL 使用 `code OR name` 直接计数导致稳定编码行与历史名称行被同时算作冲突。
- 是否存在临时补丁或绕过：否。不手工改正式库业务数据，不跳过 required SQL，不修改发布状态绕过。

## BDD 场景

- Given 正式库中同一矩阵分类同时存在历史同名分类和 `DCC_FVM_*` 编码分类，When 执行文件查阅矩阵 seed SQL，Then SQL 应优先解析编码分类，不因历史同名分类失败。
- Given 某矩阵分类不存在编码分类但存在唯一历史同名分类，When 执行 seed SQL，Then SQL 应沿用该唯一分类，避免重复创建。
- Given 某矩阵分类存在多个编码分类或无编码分类且存在多个同名分类，When 执行 seed SQL，Then SQL 应 fail fast 并输出 bounded SIGNAL 信息。

## 里程碑

- [x] 记录正式服失败证据和根因诊断。
- [x] 增加回归测试，先证明当前 SQL 缺少编码优先解析门禁。
- [x] 最小修改 seed SQL，保持幂等与 fail-fast 语义。
- [x] 运行 targeted pytest、migration policy gate、TDD 合规检查。
- [x] 提交后端修复前完成隔离验证；发布链路将使用新提交创建干净 worktree 和新 releaseTag。

## 预期验证

- `python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q`
- `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`
- `python -X utf8 tool/verify_tdd_compliance.py --repo D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-dir doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_dcc_view_matrix_message_text_sql.py doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/task.md doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/execution-log.md`

## 当前状态

已完成：后端 SQL 修复、回归测试、迁移门禁、TDD 合规和数据库证据校验均已通过；下一步由维护仓发布任务使用新提交重新构建发布。

## 验证结果

- RED：`python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q` 先失败，证明 SQL 缺少编码优先分类解析门禁。
- GREEN：`python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q` 通过，4 passed。
- GREEN：`python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` 通过，migrationCount=236。
- GREEN：`python -X utf8 tool/verify_tdd_compliance.py ...` 通过。
- GREEN：`python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/database-schema-evidence.md` 通过。

## 完成结论

修复已完成并可进入新 releaseTag 构建发布。失败的 `release-20260701-1720-signal-syntax-fix-v2` 不再继续复用，后续必须从包含本修复的新后端提交重新出包。
