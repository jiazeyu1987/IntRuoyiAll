const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const file = path.resolve(__dirname, '../../../../sql/mysql/20261011_dcc_training_attachment_facts.sql')
const source = fs.readFileSync(file, 'utf8')
test('IC03 migration declares dependency and additive nullable facts', () => {
  assert.match(source, /release-migration:.*dependsOn=20260920_dcc_controlled_file_attachment,20260911_dcc_upload_ticket_category_guard/)
  for (const column of ['attachment_purpose','application_process_instance_id','uploaded_by','uploaded_username_snapshot','uploaded_nickname_snapshot','uploaded_at']) {
    assert.match(source, new RegExp(`COLUMN_NAME='${column}'`))
    assert.match(source, new RegExp(`ADD COLUMN ${column}[^;]*NULL`))
  }
  assert.doesNotMatch(source, /UPDATE\s+`?dcc_controlled_file_attachment/i)
  assert.doesNotMatch(source, /INSERT\s+INTO\s+`?dcc_controlled_file_attachment/i)
})
test('IC03 uniqueness is generated only for live training records', () => {
  assert.match(source, /training_record_round_key[\s\S]*GENERATED ALWAYS AS \(CASE WHEN attachment_purpose='TRAINING_RECORD' AND deleted=0/)
  assert.match(source, /UNIQUE KEY uk_dcc_training_attachment_round\s*\(tenant_id,controlled_file_id,training_record_round_key\)/)
  assert.match(source, /GENERATION_EXPRESSION/)
  assert.match(source, /SIGNAL SQLSTATE '45000'/)
})
test('repeat execution validates actual nullable column and unique index definitions', () => {
  for (const column of ['attachment_purpose','application_process_instance_id','uploaded_by','uploaded_username_snapshot','uploaded_nickname_snapshot','uploaded_at']) {
    assert.match(source, new RegExp(`CALL check_dcc_training_attach_column\\('${column}',`))
    assert.ok(source.indexOf(`CALL check_dcc_training_attach_column('${column}',`) > source.indexOf(`ADD COLUMN ${column}`), `${column} definition is verified after first creation`)
  }
  assert.match(source, /COLUMN_TYPE/)
  assert.match(source, /IS_NULLABLE='YES'/)
  assert.match(source, /COLUMN_DEFAULT IS NULL/)
  assert.match(source, /GROUP_CONCAT\(COLUMN_NAME ORDER BY SEQ_IN_INDEX/)
  assert.match(source, /MIN\(NON_UNIQUE\)=0/)
  assert.match(source, /SUM\(SUB_PART IS NOT NULL\)=0/)
  assert.match(source, /REGEXP_REPLACE/)
  assert.match(source, /key_expression<>/)
})
