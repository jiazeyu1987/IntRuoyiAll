# R55 批记录版本列契约回归证据

## Bug summary

R55 测试服发布在版本切换前执行 `20260829_mes_old_form_template_binding_switch.sql` 时失败，MySQL 报 `ERROR 1054`，因为迁移向 `mes_pro_batch_record_version` 写入了当前正式 schema 不存在的子表成员列。预期行为是迁移只使用当前正式表的 21 个列，并在 DML 前由 target preflight 暴露列集合漂移。

## Expected

迁移必须只写入当前正式 21 列，列集合漂移在 DML 前明确失败。

## Reproduction

- 真实路径：测试服 `deploy-release` 使用 R55 app-release 包执行 required SQL。
- 结果：`Unknown column 'child_form_member_count' in 'field list'`；operation lock 释放为 `FAILED`，`.env` 与实际镜像未切换。
- 静态回归：`python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py -k declares_contract --basetemp .tmp-r55-child-column-red` 在修复前失败。

## Root Cause

`20260829` 迁移的 INSERT 列表误引用尚未进入当前正式 schema/DO 的 `child_form_member_count` 与 `child_form_member_hash`。真实 `DESCRIBE mes_pro_batch_record_version` 与 `MesProBatchRecordVersionDO` 均不包含这两列。

## Regression test and fix

- 回归断言：迁移 SQL 不得包含两个未发布列；target preflight 必须核对 `information_schema.columns` 的 21 列。
- 最小修复：删除两个未发布列及对应值；保留正式 21 列；增加目标列集合检查。没有新增列、默认值、跳过分支或异常吞咽。

## RED / GREEN evidence

- RED: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py -k declares_contract --basetemp .tmp-r55-child-column-red` -> FAIL，旧 SQL 仍引用未发布子表成员列。
- GREEN: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py script/tests/test_release_target_preflight_files.py script/tests/test_release_preflight_plan.py --basetemp .tmp-r55-child-column-green` -> PASS，11 + 19 项。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql --output doc/tasks/one-button-app-release-20260911/migration-policy-r55-child-fix.json` -> PASS，619 migrations。
- GREEN: 本机 Docker MySQL 只读执行 recognized-schema 构建片段及测试服 target preflight -> PASS；未修改测试服业务数据。

## Risk and regression scope

修复只收敛迁移 INSERT 与部署前 schema 检查，保留非法 JSON、缺失模板版本、字段冲突等 fail-fast 约束。完整 SQL 首次/重复执行仍需由新 releaseTag 在测试服真实发布验证；R55 包包含旧 SQL，禁止复用。

## Verification

静态回归、迁移策略门禁、本机 Docker 只读 schema 构建和测试服 target readonly preflight 均已通过；新 releaseTag 的完整 publish-test 仍是后续验证项。

## Blockers and follow-up

应用修复提交后必须重新执行 source freeze、build-release、NAS READY 完整性校验和同一 releaseTag 的 publish-test 运行态验证；正式服、mark-tested、promote-prod、promote-backup 均不在本次授权范围。

# R63 target-preflight 括号闭合回归证据

## Bug summary

R63 `build-release` 在真实测试服目标只读 preflight 阶段失败，错误为 MySQL `ERROR 1064 (42000)`。预期行为是所有 target-preflight SQL 在昂贵打包前由通用合同发现外层 `SELECT CASE` 结构错误，并且新版 SQL 可在测试服只读执行。

## Expected behavior

`20260829_mes_old_form_template_binding_switch.preflight.sql` 必须保持只读、单语句、外层 `THEN` 处于括号深度 0，并在测试服返回 `TARGET_PREFLIGHT_PASS` 或明确业务 blocker，而不是 SQL 语法错误。

## Reproduction

- 真实路径：R63 `build-release` 执行目标只读 preflight。
- 失败结果：`TARGET_PREFLIGHT_QUERY_FAILED: read-only target query failed: ERROR 1064 (42000)`；报告显示 `failedMigrationId=20260829_mes_old_form_template_binding_switch`，2/17 checks 后停止。
- 静态回归：新增外层 CASE 括号闭合合同后，旧 SQL 在 `test_target_preflights_are_read_only_single_statement_contracts` 中失败。

## Root cause

第一个 `AND NOT EXISTS (` 在待迁移旧表单模板 Jimu 语义检查后少闭合一层右括号，导致后续兄弟级 `AND NOT EXISTS` 子句仍处于前一个子查询内部；MySQL 到外层 `THEN` 时发现条件表达式未闭合。

## Regression test added

`test_release_target_preflight_files.py` 新增 `_assert_outer_case_then_is_top_level`，对所有 target-preflight 的外层 `SELECT CASE` 执行通用括号深度检查，防止同类错误只在真实 MySQL 目标预检中暴露。

## Minimal fix

仅在 `20260829_mes_old_form_template_binding_switch.preflight.sql` 补齐缺失的右括号；未改变业务判断、未新增 fallback、未放宽 Jimu schema 语义、未修改测试服业务数据。

## RED / GREEN evidence

- RED: `python -X utf8 -m pytest -q script/tests/test_release_target_preflight_files.py::test_target_preflights_are_read_only_single_statement_contracts --basetemp .tmp-r63-preflight-parenthesis-red` -> FAIL，捕获缺失括号。
- GREEN: `python -X utf8 -m pytest -q script/tests/test_release_target_preflight_files.py script/tests/test_mes_old_form_template_binding_switch_sql.py --basetemp .tmp-r63-preflight-parenthesis-green` -> PASS，12 tests。
- GREEN: 测试服只读执行新版单文件 preflight -> `TARGET_PREFLIGHT_PASS:20260829_mes_old_form_template_binding_switch`。
- GREEN: 完整目标只读 preflight -> `Target data preflight: passed; 17/17 checks`，报告 `target-data-preflight-after-fix.json`。

## Risk and regression scope

影响范围限定为 target-preflight 静态合同与一个 preflight SQL 的括号修复。R63 包半成品仍不可复用；必须用新应用 commit 和全新 releaseTag 重新构建并发布测试服验证。

## Blockers and follow-up actions

应用修复提交后，需要维护仓更新 source freeze 到新应用 commit，并执行全新 releaseTag 的 `build-release -> publish-test`。正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup` 不在当前授权范围。
