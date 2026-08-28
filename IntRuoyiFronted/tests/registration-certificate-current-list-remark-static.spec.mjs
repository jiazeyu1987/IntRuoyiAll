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

const currentColumnDefinitionsMatch = /const currentColumnDefinitions:[\s\S]*?\n\]/.exec(index)
assert.ok(currentColumnDefinitionsMatch, 'current registration-certificate column definitions must exist')
const currentColumnDefinitionKeys = [...currentColumnDefinitionsMatch[0].matchAll(/key:\s*'([^']+)'/g)]
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

const currentTableMatch = /data-user-table-key="dcc\.registrationCertificate\.current"[\s\S]*?<\/el-table>/.exec(index)
assert.ok(currentTableMatch, 'current registration-certificate table must exist')
const currentTableVisibleKeys = [...currentTableMatch[0].matchAll(/isCurrentColumnVisible\('([^']+)'\)/g)]
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
  index,
  /currentColumnDefinitions:[\s\S]{0,800}key:\s*'remark'[\s\S]{0,80}label:\s*'备注'/,
  'current registration-certificate list column definitions must include remark'
)
assert.match(
  index,
  /isCurrentColumnVisible\('remark'\)[\s\S]{0,500}label="备注"[\s\S]{0,220}prop="remark"/,
  'current registration-certificate list must render the remark column'
)
assert.doesNotMatch(
  index,
  /remark[\s\S]{0,120}(mock|placeholder|defaultSuccess)|localStorage|sessionStorage/,
  'remark column must use the server field without fake local state'
)
