# 20260829 迁移 schema 契约证据

## Data change goal and affected entities

目标是让旧表单模板绑定切换迁移在当前正式数据模型上创建 `mes_pro_batch_record_version` 版本记录，并从正式 `recognized_schema_json` 构建可审计的 Jimu 布局。受影响实体为 `mes_pro_batch_record_version`、旧模板版本/绑定表及该迁移使用的临时表；本轮不直接修改测试服数据。

## Database engine and migration tool

- 引擎：MySQL 8（本机 Docker 与测试服同一迁移 SQL 运行路径）。
- 工具：`app-release` required SQL，由 `publish-int-ruoyi.ps1 deploy-release` 顺序执行；构建前使用 migration policy、schema-only rehearsal 和 target readonly preflight。

## Migration

本次迁移只收敛当前正式列契约，并不引入新表或新列。

## Schema/migration changes

- `20260829_mes_old_form_template_binding_switch.sql` 的批记录版本 INSERT 从 23 个误声明列收敛为当前正式 21 列，删除不存在的 `child_form_member_count`、`child_form_member_hash`。
- `target-preflight/20260829_mes_old_form_template_binding_switch.preflight.sql` 使用 `information_schema.columns` 检查该 21 列集合，并继续检查模板版本、Jimu/recognized schema、绑定和冲突条件。
- 静态测试锁定列集合与禁止回退引用，避免未来迁移再次依赖未发布 DO 字段。

## Safety

## Data safety analysis

本轮只读核对本机 Docker 与测试服 target preflight；R55 在未知列错误处于 DML 前失败，未切换版本且未产生测试服业务数据写入。修复不删除、不覆盖、不默默转换既有业务数据，也不增加 fallback 或默认列。正式服和备份环境未访问。

## Rollback or recovery plan

R55 失败包不得复用；发布恢复以旧 `.env IMAGE_TAG` 和旧镜像为运行基线，使用新的 releaseTag 重新构建。若未来迁移在 DML 后失败，按发布 runbook 使用该 releaseTag 对应的数据库备份/事务恢复，并保留 operation ledger；本轮没有创建新的数据库备份，也没有声称已执行恢复。由于本次错误发生在未知列解析阶段，当前不需要人工数据回滚。

## BDD

BDD: 正式批记录版本列契约 -> Given 目标库 `mes_pro_batch_record_version` 只有当前 21 个正式列 / When 旧模板绑定迁移创建版本记录 / Then SQL 使用这 21 列并在 DML 前由 target preflight 阻断列集合漂移。

## RED / GREEN evidence

- RED: R55 测试服 `deploy-release` -> FAIL，MySQL `ERROR 1054`，`child_form_member_count` 不存在。
- RED: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py -k declares_contract --basetemp .tmp-r55-child-column-red` -> FAIL。
- GREEN: `python -X utf8 -m pytest -q script/tests/test_mes_old_form_template_binding_switch_sql.py script/tests/test_release_target_preflight_files.py script/tests/test_release_preflight_plan.py --basetemp .tmp-r55-child-column-green` -> PASS，30 tests。
- GREEN: migration policy gate -> PASS，619 migrations；本机 Docker recognized-schema 只读检查与测试服 target preflight -> PASS。

## Migration verification and blockers

静态合同、schema-only 片段和目标只读 preflight 均通过；完整 required SQL 尚未在新 releaseTag 测试服包中完成首次/重复执行验证。R55 已退休，必须提交修复后以新 releaseTag 重建并重新 publish-test；无正式服晋级动作。

## Verification

上述迁移策略、静态测试、本机 Docker 只读检查和测试服 target readonly preflight 是本轮验证证据；完整新包运行态验证待 R56。

## Blockers

R55 不得复用；修复提交和新 releaseTag publish-test 通过前，不能宣称迁移交付完成。
