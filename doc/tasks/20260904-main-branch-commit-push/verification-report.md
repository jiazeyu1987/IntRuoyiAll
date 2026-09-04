# Verification Report

## Summary

当前 `int_main` 待推送提交与提交前脏改已完成定向验证。E2E 未运行，因为用户本轮只授权提交/推送主干代码，且项目规则要求 E2E 仅在当轮明确要求时执行。

## Passed Verification

- `git branch --show-current` -> `int_main`
- `git status --porcelain=v1` -> 开始前为空；提交前出现的当前脏改已纳入基线提交范围。
- `git commit -m "feat: 固化DCC来源治理与Stage1详情刷新"` -> `ed5201d258d6f4b23589e21ef7e42b8b9567311e`
- `git push origin int_main` -> PASS，`int_main -> int_main`
- `git status --short --branch` -> `## int_main...origin/int_main`，不再 ahead。
- `mvn --% -pl yudao-module-dcc -am -Dtest=cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapperTest#effectiveSourceReferenceQueries_excludeDeletedAndOutOfSnapshotRows -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.compiler.useIncrementalCompilation=false clean test` -> PASS
- `mvn --% -pl yudao-module-dcc -Dtest=cn.iocoder.yudao.module.dcc.DccSourceOwnershipSchemaTest#migrationDefinesGovernanceBatchAndEvidenceItems -Dsurefire.failIfNoSpecifiedTests=false -Dmaven.compiler.useIncrementalCompilation=false test` -> PASS
- `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-stage1-static.spec.cjs` -> PASS
- `pnpm ts:check` -> PASS
- `git diff --check` -> PASS
- `python IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260811_dcc_source_ownership.sql --sql-file IntRuoyiBackend\sql\mysql\20260905_dcc_source_governance.sql` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260904-main-branch-commit-push --mode preview` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260904-main-branch-commit-push --mode apply` -> PASS

## Scope Notes

- 不运行 E2E。
- 不启动、停止或重启本机/远程服务。
- 不操作远程服务器或数据库。
- 新迁移只做静态 schema 合同和 release policy gate 校验，未执行到任何数据库。
- 推送后新出现的源码脏改按用户“提交推送当前的就可以”指令留在工作区，未纳入本轮。
