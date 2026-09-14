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

const currentDefinitions = extractArrayBlock(index, 'currentColumnDefinitions')
const oldDefinitions = extractArrayBlock(index, 'oldColumnDefinitions')
const currentTable = extractTableBlock(index, 'CURRENT_TABLE_KEY')
const oldTable = extractTableBlock(index, 'OLD_TABLE_KEY')

assert.match(
  api,
  /export interface DccRegistrationCertificatePageItemVO \{[\s\S]{0,900}classification:\s*string/,
  'current registration-certificate list item must expose the formal classification field'
)
assert.match(
  api,
  /export interface DccRegistrationCertificateOldIndexItemVO \{[\s\S]{0,700}classification:\s*string/,
  'old registration-certificate list item must expose the formal classification field'
)

assert.match(
  currentDefinitions,
  /key:\s*'classification'[\s\S]{0,80}label:\s*'分类'/,
  'current registration-certificate column definitions must include classification'
)
assert.match(
  oldDefinitions,
  /key:\s*'classification'[\s\S]{0,80}label:\s*'分类'/,
  'old registration-certificate column definitions must include classification'
)

assert.match(
  currentTable,
  /isCurrentColumnVisible\('classification'\)[\s\S]{0,180}label="分类"[\s\S]{0,120}prop="classification"/,
  'current registration-certificate table must render the classification column'
)
assert.match(
  oldTable,
  /isOldColumnVisible\('classification'\)[\s\S]{0,180}label="分类"[\s\S]{0,120}prop="classification"/,
  'old registration-certificate table must render the classification column'
)

assert.match(
  index,
  /queryParamKey:\s*'classification'[\s\S]{0,60}placeholder:\s*'输入分类'/,
  'classification quick filter must keep querying the formal classification field with classification wording'
)
assert.doesNotMatch(
  currentTable + oldTable,
  /classification[\s\S]{0,160}(displayText|mock|placeholder|defaultSuccess|localStorage|sessionStorage)/,
  'classification column must use the server field directly without fake local state'
)

console.log('registration certificate list classification column contract: PASS')
