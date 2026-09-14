# Database Schema Evidence

## Data Change Goal And Affected Entities

- Goal: 停止把 FormCenter 模板建模为批记录表单绑定对象。
- Affected entity: `bpm_form_template_version` 的错误绑定迁移契约。
- Current task does not execute a schema change.

## Database Engine And Migration Tool

- Engine: MySQL / InnoDB / utf8mb4。
- Migration source: `IntRuoyiBackend/sql/mysql` 下的项目 SQL 迁移文件。
- 本次删除未发现正式发布引用的错误新增迁移文件，不执行目标数据库迁移。

## Schema, Migration, Index, Or Constraint Changes

- 删除仓库中的 `20260727_bpm_form_template_batch_record_binding.sql`。
- 删除旧迁移合同 `test_form_template_batch_record_binding_sql.py`。
- 新增独立性合同，断言上述错误迁移和旧测试不存在。
- 不执行 `DROP COLUMN`、`DROP INDEX`、回填或数据修复。

## Data Safety Analysis

- 本地数据库已存在七个冗余列和一个索引，但当前代码不再映射、读取或写入。
- 未经迁移历史审计和用户授权直接删列具有不可逆风险，因此当前任务保持这些列惰性。
- 不修改远端数据库、不改变现有业务数据。

## Rollback Or Recovery Plan

- 仓库层回滚仅需恢复错误迁移和字段映射，但这会重新引入已确认缺陷，因此不作为 fallback。
- 未来物理清理前必须备份 schema、确认目标环境发布历史、评估列使用情况并提供向下恢复方案。

## BDD Scenarios

- `BDD: 错误绑定迁移停止发布 -> Given 表单模板与批记录表单无直接关系 / When 检查迁移内容 / Then 错误新增迁移和旧合同均不存在。`
- `BDD: 当前任务不做破坏性删列 -> Given 本地库已存在冗余列 / When 完成代码解耦 / Then 不执行 DROP COLUMN 或 DROP INDEX。`

## RED And GREEN

- `RED: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> FAIL, 错误迁移仍存在且 FormCenter 源码仍定义绑定字段。`
- `GREEN: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> PASS, 2 tests。`

## Migration Verification

- 静态合同确认错误迁移和旧迁移测试已删除。
- 后端/前端合同确认代码不再使用七个冗余字段。
- 本次未执行数据库 DDL，因而没有迁移 up/down 操作。

## Blockers

- 物理删除已存在列和索引需要单独授权、发布历史审计、备份与回滚设计。
- 该 blocker 不影响当前按钮功能。
