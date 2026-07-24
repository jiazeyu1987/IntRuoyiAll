import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..', '..')
const migrationPath = resolve(repoRoot, 'sql/mysql/20260707_mes_batch_record_extra_form_slots.sql')
const baseSchemaPath = resolve(repoRoot, 'sql/mysql/20260512_mes_base_schema.sql')
const reportSchemaPath = resolve(repoRoot, 'sql/mysql/20260514_mes_batch_record_report.sql')
const tailGoalsPath = resolve(repoRoot, 'sql/mysql/20260615_mes_edhr_tail_four_goals.sql')

for (const sourcePath of [migrationPath, baseSchemaPath, reportSchemaPath, tailGoalsPath]) {
  assert.ok(existsSync(sourcePath), `missing SQL contract source: ${sourcePath}`)
}

const migration = readFileSync(migrationPath, 'utf8')
const baseSchema = readFileSync(baseSchemaPath, 'utf8')
const reportSchema = readFileSync(reportSchemaPath, 'utf8')
const tailGoals = readFileSync(tailGoalsPath, 'utf8')

assert.match(
  migration,
  /release-migration:\s*allowedEnvironments=test,backup,prod;[\s\S]+dependsOn=20260514_mes_batch_record_report,20260612_mes_edhr_multi_batch_route/,
  'migration must declare release metadata and dependency order'
)
assert.match(migration, /CREATE PROCEDURE ensure_mes_batch_record_extra_form_slots\(\)/)
assert.match(migration, /CALL ensure_mes_batch_record_extra_form_slots\(\);/)
assert.match(migration, /DROP PROCEDURE IF EXISTS ensure_mes_batch_record_extra_form_slots;\s*$/)

for (const slotType of ['MAIN', 'LOSS_REPORT', 'PROCESS_INSPECTION', 'PARAMETER_RECORD']) {
  assert.ok(migration.includes(slotType), `migration must preserve slot type ${slotType}`)
  assert.ok(baseSchema.includes(slotType), `base schema must preserve slot type ${slotType}`)
  assert.ok(reportSchema.includes(slotType), `report schema must preserve slot type ${slotType}`)
}

assert.match(
  migration,
  /ADD UNIQUE KEY `uk_mes_batch_record_report_sample_route_table`\s*\(\s*`sample_key`,\s*`form_slot_type`,\s*`route_key`,\s*`source_table_index`\s*\)/,
  'report uniqueness must include form_slot_type so multiple form slots can share one sample route/table'
)
assert.match(
  reportSchema,
  /UNIQUE KEY `uk_mes_batch_record_report_sample_route_table` \(`sample_key`, `form_slot_type`, `route_key`, `source_table_index`\)/,
  'report schema contract must include form_slot_type in uniqueness'
)
assert.match(
  baseSchema,
  /UNIQUE KEY `uk_mes_batch_record_report_sample_route_table` \(`sample_key`, `form_slot_type`, `route_key`, `source_table_index`\)/,
  'base schema contract must include form_slot_type in report uniqueness'
)

for (const tableName of [
  'mes_pro_route_use_process_batch_record',
  'mes_pro_batch_record_execution',
  'mes_pro_edhr_batch_execution_task',
]) {
  const tableBlock = new RegExp(
    "TABLE_NAME = '" + tableName + "'[\\s\\S]+?ADD COLUMN `form_slot_type` varchar\\(32\\) NOT NULL DEFAULT 'MAIN'"
  )
  assert.match(migration, tableBlock, `${tableName} must add form_slot_type`)
}

for (const requiredColumn of [
  'record_category',
  'validation_profile',
  'permission_scope_id',
  'route_binding_snapshot_hash',
  'archive_visibility',
  'slot_config_snapshot_hash',
]) {
  assert.ok(migration.includes(`COLUMN_NAME = '${requiredColumn}'`), `migration must add ${requiredColumn}`)
}

for (const businessToken of [
  'QUALITY_RECORD',
  'EQUIPMENT_RECORD',
  'QUALITY_PROCESS',
  'EQUIPMENT_PARAMETER',
  'PRODUCTION/QUALITY/EQUIPMENT',
  'FINAL_DHR/INTERNAL_REVIEW/AUDIT_ONLY/ATTACHMENT_REFERENCE',
]) {
  assert.ok(migration.includes(businessToken), `migration must retain business contract token ${businessToken}`)
}

for (const legacyVisibility of ['DOSSIER', 'CONTROLLED', 'INTERNAL']) {
  assert.ok(
    migration.includes(`WHEN '${legacyVisibility}' COLLATE utf8mb4_bin`),
    `migration must normalize legacy archive visibility ${legacyVisibility} with exact collation`
  )
}
assert.match(
  migration,
  /WHERE `archive_visibility` COLLATE utf8mb4_bin IN \('DOSSIER' COLLATE utf8mb4_bin, 'CONTROLLED' COLLATE utf8mb4_bin, 'INTERNAL' COLLATE utf8mb4_bin\);/,
  'archive visibility normalization must use exact utf8mb4_bin comparisons'
)

assert.match(
  migration,
  /UPDATE `mes_pro_route_use_process_batch_record` br[\s\S]+LEFT JOIN `mes_pro_batch_record_report` r[\s\S]+ON r\.`report_id` COLLATE utf8mb4_bin = br\.`batch_record_report_id` COLLATE utf8mb4_bin[\s\S]+SET br\.`form_slot_type` = COALESCE\(NULLIF\(r\.`form_slot_type` COLLATE utf8mb4_bin, '' COLLATE utf8mb4_bin\), 'MAIN'\)/,
  'route binding rows must backfill slot type from report configuration'
)
assert.match(
  migration,
  /WHERE br\.`form_slot_type` IS NULL OR br\.`form_slot_type` COLLATE utf8mb4_bin = '' COLLATE utf8mb4_bin;/,
  'route binding empty check must use exact utf8mb4_bin comparison'
)
assert.match(
  migration,
  /UPDATE `mes_pro_batch_record_execution` e[\s\S]+LEFT JOIN `mes_pro_batch_record_report` r[\s\S]+ON r\.`report_id` COLLATE utf8mb4_bin = e\.`batch_record_report_id` COLLATE utf8mb4_bin[\s\S]+SET e\.`form_slot_type` = COALESCE\(NULLIF\(r\.`form_slot_type` COLLATE utf8mb4_bin, '' COLLATE utf8mb4_bin\), 'MAIN'\)/,
  'execution rows must backfill slot type from report configuration'
)
assert.match(
  migration,
  /WHERE e\.`form_slot_type` IS NULL OR e\.`form_slot_type` COLLATE utf8mb4_bin = '' COLLATE utf8mb4_bin;/,
  'execution slot type empty check must use the target column collation'
)


for (const slotField of ['form_slot_type', 'record_category', 'validation_profile']) {
  assert.ok(tailGoals.includes(slotField), `tail goals schema migration must keep ${slotField}`)
}

console.log('edhr extra form slots SQL contract passed')
