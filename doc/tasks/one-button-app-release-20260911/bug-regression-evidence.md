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
