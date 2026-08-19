# CI/CD Evidence

## Environment

本地修复环境为 `E:\IntRuoyi` app 仓；后续发布目标仍只允许测试服务器 `172.30.30.58`，正式服和备份服不在本阶段范围。

## Commands

- `python -X utf8 -m pytest script\tests\test_release_migration_policy_gate.py script\tests\test_mes_pressure_pump_same_name_item_convergence_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --output ..\doc\tasks\20260819-c015-release-migration-gate-fix\app-migration-policy-gate.json`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyiWorktrees\r260819b\m\ops\release\run-release-migration-policy-gate.py --sql-root E:\IntRuoyi\IntRuoyiBackend\sql\mysql --output E:\IntRuoyi\doc\tasks\20260819-c015-release-migration-gate-fix\maintenance-ops-migration-policy-gate.json`

## Secrets

不需要新增 secret；未输出、提交或记录凭据明文。

## Pipeline

变更文件：`script\release\release_migration_manifest.py`、`script\release\release_migration_policy_gate.py`、迁移 SQL 元数据和聚焦测试。

## Verification

focused pytest 12 passed；app full migration policy gate passed；maintenance actual ops gate passed。

## Rollback

回滚本次提交；后续发布若失败按维护仓 releaseTag 回滚流程处理。

## Blockers

无当前阻塞；发布包仍需在新的已提交 HEAD 后重新构建验证。
