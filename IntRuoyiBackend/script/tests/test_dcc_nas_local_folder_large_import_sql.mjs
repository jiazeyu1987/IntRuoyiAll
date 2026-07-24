import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const migrationPath = path.join(root, 'sql', 'mysql', '20260614_dcc_nas_local_folder_large_import.sql')
const freshSchemaPath = path.join(root, 'sql', 'mysql', '20260513_dcc_base_schema.sql')
const nasSchemaPath = path.join(root, 'sql', 'mysql', '20260523_dcc_nas_transfer_task.sql')
const h2SchemaPath = path.join(root, 'yudao-module-dcc', 'src', 'test', 'resources', 'sql', 'create_tables.sql')
const tableName = 'dcc_controlled_file_nas_transfer_task'

const progressColumns = [
  ['expected_file_count', 'bigint NOT NULL DEFAULT 0'],
  ['expected_total_bytes', 'bigint NOT NULL DEFAULT 0'],
  ['uploaded_file_count', 'bigint NOT NULL DEFAULT 0'],
  ['uploaded_total_bytes', 'bigint NOT NULL DEFAULT 0'],
  ['upload_completed_at', 'datetime DEFAULT NULL']
]

const readText = (filePath) => fs.readFileSync(filePath, 'utf8')

test('large local folder import migration adds upload progress columns idempotently', () => {
  const migration = readText(migrationPath)

  assertNoDestructiveDccSql(migration)
  for (const [columnName, columnType] of progressColumns) {
    assert.match(migration, new RegExp(`COLUMN_NAME\\s*=\\s*'${columnName}'`, 'i'))
    assert.match(migration, new RegExp(`ALTER TABLE \`${tableName}\` ADD COLUMN \`${columnName}\` ${columnType}`, 'i'))
    assert.match(migration, new RegExp(`${tableName}\\.${columnName} already exists`, 'i'))
  }
  assert.match(migration, /information_schema\.COLUMNS/i)
  assert.match(migration, /PREPARE\s+dcc_nas_transfer_task_expected_file_count_stmt/i)
  assert.match(migration, /DEALLOCATE\s+PREPARE\s+dcc_nas_transfer_task_upload_completed_at_stmt/i)
})

test('fresh MySQL schemas contain local folder upload progress columns', () => {
  for (const schemaPath of [freshSchemaPath, nasSchemaPath]) {
    const schema = readText(schemaPath)
    const block = findCreateBlock(schema, tableName)
    assert.ok(block, `Missing ${tableName} in ${schemaPath}`)
    for (const [columnName, columnType] of progressColumns) {
      assert.match(block, new RegExp(`\`${columnName}\`\\s+${columnType}`, 'i'))
    }
  }
})

test('H2 test schema contains local folder upload progress columns', () => {
  const schema = readText(h2SchemaPath)
  const block = findCreateBlock(schema, tableName)
  assert.ok(block, `Missing ${tableName} in ${h2SchemaPath}`)

  assert.match(block, /`expected_file_count`\s+BIGINT\s+NOT\s+NULL\s+DEFAULT\s+0/i)
  assert.match(block, /`expected_total_bytes`\s+BIGINT\s+NOT\s+NULL\s+DEFAULT\s+0/i)
  assert.match(block, /`uploaded_file_count`\s+BIGINT\s+NOT\s+NULL\s+DEFAULT\s+0/i)
  assert.match(block, /`uploaded_total_bytes`\s+BIGINT\s+NOT\s+NULL\s+DEFAULT\s+0/i)
  assert.match(block, /`upload_completed_at`\s+DATETIME\s+NULL/i)
})

function assertNoDestructiveDccSql(sql) {
  assert.equal(/\b(DROP\s+TABLE|TRUNCATE\s+TABLE)\b/i.test(sql), false)
  assert.equal(/\bDELETE\s+FROM\s+`?dcc_/i.test(sql), false)
}

function findCreateBlock(schema, table) {
  const match = schema.match(
    new RegExp(`CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+\`?${table}\`?\\s*\\((.*?)\\)\\s*(?:ENGINE|;)`, 'is')
  )
  return match?.[1]
}
