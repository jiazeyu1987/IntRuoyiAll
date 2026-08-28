import fs from 'node:fs'
import assert from 'node:assert/strict'

const api = fs.readFileSync(new URL('../src/api/dcc/registrationCertificate/index.ts', import.meta.url), 'utf8')
const page = fs.readFileSync(new URL('../src/views/dcc/registration-certificate/index/index.vue', import.meta.url), 'utf8')
const detail = fs.readFileSync(new URL('../src/views/dcc/registration-certificate/detail/index.vue', import.meta.url), 'utf8')

for (const field of [
  'ownerCompanyName',
  'productName',
  'classification',
  'registrantName',
  'modelSpecification',
  'productionAddress',
  'entrustedEnterpriseName',
  'projectCode'
]) {
  assert.ok(api.includes(field + '?:'), 'API must expose ' + field)
  assert.ok(page.includes("queryParamKey: '" + field + "'"), 'query page must bind ' + field)
}

assert.match(page, /prop="projectCode"/, 'query results must expose actual project code')
assert.match(
  detail,
  /label="项目代码">\{\{ displayText\(detail\.projectCode\) \}\}/,
  'detail page must display the formal project code value'
)
assert.doesNotMatch(
  detail,
  /label="项目代码">\{\{ displayText\(detail\.projectCodeId\) \}\}/,
  'detail page must not display the project-code database id as business data'
)

console.log('registration certificate comprehensive query contract: PASS')
