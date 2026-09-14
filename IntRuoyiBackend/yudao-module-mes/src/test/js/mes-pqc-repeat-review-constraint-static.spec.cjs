const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260811_mes_process_pool_pqc_repeat_review_constraint.sql'
)
const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')
const compactSql = sql.replace(/\s+/g, ' ')

assert.match(
  sql,
  /^-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260805_mes_process_pool_ac_m20_pqc_review_closure; type=schema; riskLevel=medium\n/
)
assert.match(
  compactSql,
  /ADD COLUMN `production_terminal_event_id` bigint GENERATED ALWAYS AS \(CASE WHEN `leader_type` = 'PRODUCTION' AND `deleted` = b'0' THEN `event_id` ELSE NULL END\) STORED/
)
assert.match(
  compactSql,
  /ADD UNIQUE KEY `uk_mes_pp_submission_review_production_terminal` \(`tenant_id`, `production_terminal_event_id`\)/
)
assert.match(sql, /DROP INDEX `uk_mes_pp_submission_review_event_terminal`/)
assert.match(sql, /DROP INDEX `uk_mes_pp_submission_review_event`/)
assert.match(
  compactSql,
  /WHERE `leader_type` = 'PRODUCTION' AND `deleted` = b'0' GROUP BY `tenant_id`, `event_id` HAVING COUNT\(1\) > 1/
)
assert.doesNotMatch(
  sql,
  /\b(?:INSERT|UPDATE|DELETE)\s+(?:INTO\s+|FROM\s+)?`mes_pro_process_pool_submission_review`/i
)

console.log('PASS: PQC repeat review constraint migration contract')
