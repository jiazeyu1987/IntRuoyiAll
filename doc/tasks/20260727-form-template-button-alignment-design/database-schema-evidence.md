# Database Schema Evidence

## Data Change Goal And Entities

- Goal: 在 `bpm_form_template_version` 上持久化表单模板到批记录表单的显式绑定摘要。
- Entity: `bpm_form_template_version`。

## Database Engine And Migration Tool

- Engine: MySQL / InnoDB / utf8mb4。
- Migration file: `IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql`。

## Schema And Index Changes

- 新增可空列：`batch_record_report_id`、`batch_record_report_name`、`batch_record_name`、`batch_record_version_no`、`batch_record_form_slot_type`、`batch_record_binding_status`、`batch_record_binding_error`。
- 新增索引：`idx_bpm_form_template_batch_record_report` on `tenant_id, batch_record_report_id, deleted`。
- 迁移先检查 `bpm_form_template_version` 存在，不存在时 fail fast。

## Data Safety Analysis

- 迁移为 additive schema change，不删除、不清空、不更新业务数据。
- 新字段均为 nullable，避免对既有模板版本产生强制回填风险。
- 不做名称匹配、源文件名匹配或默认绑定，避免错误关联批记录报表。

## Rollback Or Recovery Plan

- 如需回滚，先停用依赖这些字段的新前端行为，再由数据库变更流程删除新增索引和列。
- 当前迁移本身不改写既有数据，恢复风险集中在结构回滚窗口。

## BDD Scenarios

- `BDD: 迁移缺表 fail fast -> Given 目标库缺少 bpm_form_template_version / When 执行迁移 / Then SQLSTATE 45000 且提示 bpm_form_template_version is missing。`
- `BDD: 迁移只做增量结构 -> Given 表已存在 / When 执行迁移 / Then 只新增绑定列和索引，不删除或回填业务数据。`

## RED And GREEN

- `RED: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> FAIL, 迁移文件不存在。`
- `GREEN: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> PASS, 3 tests。`

## Migration Verification

- Static contract verified release metadata, fail-fast table check, additive columns, index, and no destructive SQL keywords.
- Target-environment migration application not run in this task.

## Blockers

- 目标环境应用迁移前，应按 release migration 流程核对真实 `information_schema`。
