const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/infra/runtimeControl/index.ts'),
  'utf8'
)

function assertIncludes(source, fragment, label) {
  assert(source.includes(fragment), `missing ${label}: ${fragment}`)
}

function assertExcludes(source, fragment, label) {
  assert(!source.includes(fragment), `forbidden ${label}: ${fragment}`)
}

assertIncludes(
  apiSource,
  "export type RuntimeControlTargetEnvironment = 'test' | 'prod' | 'backup'",
  'rollback target environment API type'
)

assertIncludes(
  pageSource,
  "operationSupportsTargetEnvironment(operationDialog.action)",
  'shared operation target environment form'
)
assertIncludes(
  pageSource,
  "if (action === 'rollback-app') return rollbackTargetEnvironmentOptions",
  'rollback-app target environment options'
)
assertIncludes(
  pageSource,
  "{ label: '测试服', value: 'test' }",
  'rollback-app test target option'
)
assertIncludes(
  pageSource,
  "{ label: '备份服务器', value: 'backup' }",
  'rollback-app backup target option'
)
assertExcludes(
  pageSource,
  "rollbackTargetEnvironmentOptions = [\n  { label: '测试服', value: 'test' },\n  { label: '正式服', value: 'prod' }",
  'production rollback target option'
)

assertIncludes(
  pageSource,
  "operationDialog.targetEnvironment =\n    action.action === 'backup-now' || action.action === 'rollback-app' || action.action === 'restore-data'\n      ? 'test'\n      : 'prod'",
  'rollback-app default test target'
)
assertIncludes(
  pageSource,
  "return action === 'backup-now' || action === 'restore-data' || action === 'rollback-app'",
  'rollback-app target environment validation condition'
)
assertIncludes(
  pageSource,
  "return operationDialog.action === 'rollback-app'\n    ? operationDialog.targetEnvironment",
  'rollback-app target environment payload'
)
assertIncludes(
  pageSource,
  'selectedImageCandidateId:',
  'rollback-app selected candidate payload'
)
assertIncludes(
  pageSource,
  '只回滚应用版本，只覆盖所选测试服或备份服务器应用版本',
  'rollback-app front-end operation copy'
)
assertIncludes(
  pageSource,
  '禁止影响正式服务器程序和数据',
  'rollback-app production no-impact copy'
)

console.log('PASS: runtime control rollback-app target environment UI contract is wired')
