import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const apiPath = 'src/api/dcc/registrationCertificate/index.ts'
const indexPath = 'src/views/dcc/registration-certificate/index/index.vue'

for (const file of [apiPath, indexPath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const api = read(apiPath)
const index = read(indexPath)

const extractArrayBlock = (source, declarationName) => {
  const start = source.indexOf(`const ${declarationName}:`)
  assert.notEqual(start, -1, `${declarationName} must exist`)
  const assignment = source.indexOf('=', start)
  assert.notEqual(assignment, -1, `${declarationName} must use an assigned array literal`)
  const open = source.indexOf('[', assignment)
  assert.notEqual(open, -1, `${declarationName} must use an array literal`)
  let depth = 0
  for (let position = open; position < source.length; position += 1) {
    const char = source[position]
    if (char === '[') depth += 1
    if (char === ']') {
      depth -= 1
      if (depth === 0) return source.slice(start, position + 1)
    }
  }
  assert.fail(`${declarationName} array literal must be closed`)
}

const extractTableBlock = (source, tableKeyName) => {
  const keyPosition = source.indexOf(`:data-user-table-key="${tableKeyName}"`)
  assert.notEqual(keyPosition, -1, `${tableKeyName} table must bind the user table key`)
  const tableStart = source.lastIndexOf('<el-table', keyPosition)
  const tableEnd = source.indexOf('</el-table>', keyPosition)
  assert.notEqual(tableStart, -1, `${tableKeyName} table must start with el-table`)
  assert.notEqual(tableEnd, -1, `${tableKeyName} table must end with el-table`)
  return source.slice(tableStart, tableEnd + '</el-table>'.length)
}

const currentColumnDefinitions = extractArrayBlock(index, 'currentColumnDefinitions')
const currentColumnDefinitionKeys = [...currentColumnDefinitions.matchAll(/key:\s*'([^']+)'/g)]
  .map((match) => match[1])
assert.equal(
  currentColumnDefinitionKeys.at(-2),
  'remark',
  'current registration-certificate remark column definition must be the last business column before actions'
)
assert.equal(
  currentColumnDefinitionKeys.at(-1),
  'actions',
  'current registration-certificate operation column must remain the final fixed control column'
)

const currentTable = extractTableBlock(index, 'CURRENT_TABLE_KEY')
const currentTableVisibleKeys = [...currentTable.matchAll(/isCurrentColumnVisible\('([^']+)'\)/g)]
  .map((match) => match[1])
assert.equal(
  currentTableVisibleKeys.at(-2),
  'remark',
  'current registration-certificate rendered remark column must be the last business column before actions'
)
assert.equal(
  currentTableVisibleKeys.at(-1),
  'actions',
  'current registration-certificate rendered actions column must remain the final fixed control column'
)

assert.match(
  api,
  /export interface DccRegistrationCertificatePageItemVO \{[\s\S]{0,800}remark\??:\s*string/,
  'current registration-certificate list item must expose remark'
)
assert.match(
  currentColumnDefinitions,
  /key:\s*'remark'[\s\S]{0,80}label:\s*'备注'/,
  'current registration-certificate list column definitions must include remark'
)
assert.match(
  currentTable,
  /isCurrentColumnVisible\('remark'\)[\s\S]{0,500}label="备注"[\s\S]{0,220}prop="remark"/,
  'current registration-certificate list must render the remark column'
)
assert.doesNotMatch(
  index,
  /remark[\s\S]{0,120}(mock|placeholder|defaultSuccess)|localStorage|sessionStorage/,
  'remark column must use the server field without fake local state'
)
