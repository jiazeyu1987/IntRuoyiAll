# 执行日志：修复 DCC 文件查阅矩阵正式库历史分类重名发布阻塞

BDD: code-first category resolution -> Given 正式库存在历史同名分类和新矩阵编码分类 / When 执行 DCC 文件查阅矩阵 seed SQL / Then SQL 使用编码分类作为稳定解析结果，不把历史同名分类计入歧义失败。

BDD: legacy unique category reuse -> Given 目标库没有矩阵编码分类但存在唯一历史同名分类 / When 执行 DCC 文件查阅矩阵 seed SQL / Then SQL 复用该唯一分类并保持幂等。

BDD: true category ambiguity still fails -> Given 目标库存在多个编码分类或无编码分类且存在多个同名分类 / When 执行 DCC 文件查阅矩阵 seed SQL / Then SQL fail fast 并输出 128 字符以内的 SIGNAL 信息。

GREEN: experience-preflight -> PASS, 已读取经验索引并命中 release-agent-checklist、release-build-preflight-lessons、release-backup-restore、worktree-memory；正式服失败后仅执行只读诊断，未手工改库绕过。

BLOCKER: promote-prod -> FAIL, releaseTag=release-20260701-1720-signal-syntax-fix-v2, operation=op-2026-07-01T134536550516100Z-98c14c30-4cce-4423-80e0-78db16ffab92, reason=DCC_FILE_VIEW_MATRIX_CATEGORY_PRECHECK_FAILED；正式库诊断显示 DCC_FVM_DHF_001/DCC_FVM_DHF_002/DCC_FVM_DHF_004 同时匹配历史同名分类和编码分类。

RED: python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> FAIL, expected reason=SQL lacks code-first category resolution for legacy duplicate names
GREEN: python -m pytest script/tests/test_dcc_view_matrix_message_text_sql.py -q -> PASS
GREEN: python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql -> PASS
GREEN: python -X utf8 tool/verify_tdd_compliance.py --repo D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-dir doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates --paths sql/mysql/20260613_dcc_file_view_matrix_seed.sql script/tests/test_dcc_view_matrix_message_text_sql.py doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/task.md doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/execution-log.md -> PASS
GREEN: python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260701-fix-dcc-fvm-category-legacy-duplicates/database-schema-evidence.md -> PASS