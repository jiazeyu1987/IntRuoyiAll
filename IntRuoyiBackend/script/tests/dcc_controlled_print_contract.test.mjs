import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const backendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(backendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertControlledPrintSchema = (schema, label) => {
  assert.match(
    schema,
    /CREATE TABLE IF NOT EXISTS `dcc_controlled_file_print_record`/,
    `${label} must create dcc_controlled_file_print_record`
  )
  for (const column of [
    '`controlled_file_id` BIGINT NOT NULL',
    '`print_no` VARCHAR(64) NOT NULL',
    '`purpose` VARCHAR(255) NOT NULL',
    '`copies` INT NOT NULL',
    '`receiving_department` VARCHAR(128) NOT NULL',
    '`use_location` VARCHAR(128) NOT NULL',
    '`print_user_id` BIGINT NOT NULL',
    '`print_time` DATETIME NOT NULL',
    '`approval_status` VARCHAR(32) NOT NULL'
  ]) {
    assert.match(schema, new RegExp(column.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${label} missing ${column}`)
  }
}

assertControlledPrintSchema(readSource('sql/mysql/20260513_dcc_base_schema.sql'), 'base schema')
assertControlledPrintSchema(
  readSource('yudao-module-dcc/src/test/resources/sql/create_tables.sql'),
  'test schema'
)

const controlledPrintMigration = readSource('sql/mysql/20260802_dcc_controlled_print_record.sql')
assert.match(
  controlledPrintMigration,
  /@dcc_controlled_print_preferred_menu_id_blocked/,
  'controlled print migration must detect occupied preferred menu id'
)
assert.match(
  controlledPrintMigration,
  /COALESCE\(MAX\(`id`\), 0\) \+ 1 FROM `system_menu`/,
  'controlled print migration must allocate a safe menu id when the preferred id is occupied'
)
assert.doesNotMatch(
  controlledPrintMigration,
  /SELECT\s+6813,\s*[\s\S]{0,200}'dcc:controlled-file:print'/,
  'controlled print migration must not rely on occupied legacy menu id 6813'
)

const controller = readSource(
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java'
)
assert.match(controller, /DccControlledFilePrintService/, 'controller must use DccControlledFilePrintService')
assert.match(controller, /dcc:controlled-file:print/, 'controlled print endpoints must require print permission')
assert.match(controller, /\/\{id:\\\\d\+\}\/controlled-print/, 'missing controlled print create endpoint')
assert.match(controller, /\/\{id:\\\\d\+\}\/controlled-print\/records/, 'missing controlled print records endpoint')
assert.match(controller, /controlled-print\/print-html/, 'missing controlled print HTML endpoint')

for (const relativePath of [
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintService.java',
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFilePrintServiceImpl.java',
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFilePrintRecordDO.java',
  'yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFilePrintRecordMapper.java'
]) {
  assert.equal(fs.existsSync(path.join(backendRoot, relativePath)), true, `missing ${relativePath}`)
}

const errors = readSource('yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/enums/ErrorCodeConstants.java')
assert.match(errors, /CONTROLLED_FILE_PRINT_NOT_ALLOWED/, 'missing controlled print permission/current error')
assert.match(errors, /CONTROLLED_FILE_PRINT_REQUIRED_FIELD_MISSING/, 'missing controlled print required-field error')

console.log('PASS: DCC controlled print backend contract')
