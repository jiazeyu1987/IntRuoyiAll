# 20260831 修正 project_code 迁移依赖执行日志

## Log

- BDD: project_code migration dependency -> Given `20260514_mes_batch_record_report` 已 APPLIED 且无关 data migration 未应用，When code-only preflight 评估 project_code schema migration，Then action 必须是 APPLY 且不得出现 BLOCKED_SCOPE_DEPENDENCY。
- INFO: base -> branch=`codex/20260831-fix-project-code-migration-dependency`，base HEAD=`85d0d91d0a59401a4838d9ecdbd4fda5f2e70729`，worktree=`D:\IntRuoyiWorktree\20260831-fix-project-code-migration-dependency`。
- INFO: scope -> 仅修改迁移元数据、目标回归测试和任务记录；不修改 DDL 正文、不执行数据库写入。
- GREEN: experience-preflight -> PASS。已读取 `AGENTS.md`、`backend-development.md`、`database-rules.md`、`worktree-restrictions.md`、`worktree-memory.md`、`task-closeout-rules.md` 与经验索引命中的 code-only/dependsOn/target-bound preflight 门禁。
- GREEN: worktree-slot-reservation -> PASS。profile=`int_main`、slot=58、frontendPort=8313、backendPort=48313；本任务不启动前后端服务，但为提交守卫完成原子登记。

RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_batch_record_report_project_code_sql.py -q --basetemp <state>\pytest-project-code-dependency-red` -> FAIL，`1 failed, 1 passed`；预期原因：当前 metadata 不包含 `dependsOn=20260514_mes_batch_record_report`，仍依赖无关 data migration。

GREEN: 同一目标 pytest -> PASS，`2 passed`。
- INFO: regression-command-workdir -> 首次从仓库根组合运行三个既有 release tests，因它们按 `IntRuoyiBackend` 为 import root 而出现 `ModuleNotFoundError: script` 收集错误；改从后端根用正式路径复跑，非业务失败。
- GREEN: migration-regression -> `test_mes_batch_record_report_project_code_sql.py + test_release_migration_metadata.py + test_release_migration_policy_gate.py + test_release_preflight_plan.py` -> `31 passed`。
- GREEN: maintenance-migration-policy -> 实际维护仓 gate `status=passed, migrationCount=551`。
- INFO: synthetic-manifest-projections -> 首次 target-bound 复验只更新 manifest.requiredSql 投影，migrationPlan/database.schemaMigrations 仍保留旧依赖，计划继续 blocked；补齐同一迁移的三份正式 manifest 投影后重跑。
- GREEN: target-bound-code-only-preflight -> `status=passed`、items=548、blockedCount=0、目标 migration action=`APPLY`、reason=`migration is pending and prerequisites are satisfied`。
- GREEN: evidence-validators -> bug-regression evidence 与 database-schema evidence validator 均 PASS。
- GREEN: branch-runtime-port-guard -> PASS，branch profile=`int_main`、frontend=8313、backend=48313。
- INFO: integration-drift -> 修复期间 `int_main` 从 base `85d0d91d` 前进 1 个无路径重叠提交 `f23ed125`；提交本任务后将先把该主线提交合入修复分支并复验，再允许主线 ff-only。
- GREEN: implementation-commit -> PASS，commit=`7949cedc9`，5 files，仅目标 SQL、回归测试和任务记录。
- GREEN: merge-main-into-fix -> PASS，merge commit=`2c7bd07fa`；并行 PQC commit=`f23ed125` 完整保留，无路径冲突。
- GREEN: merged-branch-regression -> PASS，31 passed，实际维护 gate status=passed/migrationCount=551，branch runtime guard PASS，worktree clean。
- GREEN: int-main-fast-forward -> PASS，`int_main` 从 `f23ed125` ff-only 到 `2c7bd07fa`；目标路径无主线 dirty overlap。
- GREEN: int-main-regression -> PASS，31 passed in 13.78s，实际维护 gate 551 项通过，branch runtime guard PASS，branch ancestor=true。
