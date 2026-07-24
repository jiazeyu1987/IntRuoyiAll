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

function assertMatches(source, pattern, label) {
  assert(pattern.test(source), `missing ${label}: ${pattern}`)
}

assertIncludes(
  apiSource,
  "export type RuntimeControlTargetEnvironment = 'test' | 'prod' | 'backup'",
  'backup restore target environment API type'
)

assertIncludes(
  pageSource,
  "operationSupportsTargetEnvironment(operationDialog.action)",
  'shared operation target environment form'
)
assertIncludes(
  pageSource,
  "operationTargetEnvironmentOptions(operationDialog.action)",
  'target environment option source'
)
assertIncludes(
  pageSource,
  "if (action === 'restore-data') return restoreTargetEnvironmentOptions",
  'restore-data target environment options'
)
assertIncludes(
  pageSource,
  "{ label: '测试服', value: 'test' }",
  'restore-data test target option'
)
assertIncludes(
  pageSource,
  "{ label: '备份服务器', value: 'backup' }",
  'restore-data backup target option'
)
assertExcludes(
  pageSource,
  "restoreTargetEnvironmentOptions = [\n  { label: '测试服', value: 'test' },\n  { label: '正式服', value: 'prod' }",
  'production restore target option'
)

assertMatches(
  pageSource,
  /operationDialog\.targetEnvironment\s*=\s*[\s\S]{0,160}action\.action\s*===\s*'backup-now'[\s\S]{0,80}action\.action\s*===\s*'rollback-app'[\s\S]{0,80}action\.action\s*===\s*'restore-data'[\s\S]{0,80}\?\s*'test'[\s\S]{0,40}:\s*'prod'/,
  'restore-data default test target'
)
assertIncludes(
  pageSource,
  "if (action === 'backup-now' || action === 'rollback-app' || action === 'restore-data') return operationDialog.targetEnvironment",
  'restore-data owner matrix environment follows selected target'
)
assertIncludes(
  pageSource,
  "operationRequiresTargetEnvironment(operationDialog.action)",
  'target environment submit validation'
)
assertIncludes(
  pageSource,
  "return action === 'backup-now' || action === 'restore-data' || action === 'rollback-app'",
  'restore-data target environment validation condition'
)
assertIncludes(
  pageSource,
  'targetEnvironment: operationSubmitTargetEnvironment()',
  'restore-data target environment payload helper'
)
assertMatches(
  pageSource,
  /operationDialog\.action\s*===\s*'restore-data'[\s\S]{0,80}\?\s*operationDialog\.targetEnvironment/,
  'restore-data target environment payload'
)
assertIncludes(
  pageSource,
  'selectedRecoverySetCandidateId:',
  'restore-data selected candidate payload'
)
assertIncludes(
  pageSource,
  '恢复数据只覆盖所选测试服或备份服务器',
  'restore-data formal server isolation copy'
)
assertIncludes(
  pageSource,
  '禁止影响正式服务器程序和数据',
  'restore-data production no-impact copy'
)
assertIncludes(
  pageSource,
  '程序版本指纹、Redis 策略和配置清单仅作为恢复集证据展示',
  'restore-data evidence-only boundary copy'
)
assertIncludes(
  pageSource,
  '不会自动切换程序版本、执行 Redis 处理或覆盖目标运行配置',
  'restore-data non-automated runtime boundary copy'
)
assertExcludes(
  pageSource,
  "if (action === 'restore-data') {\n    return `${operationTargetEnvironmentText(operationDialog.targetEnvironment)} 当前 MySQL / MinIO / 文件对象 / 运行态`\n  }",
  'restore-data target directory must not include runtime state'
)
assertIncludes(
  pageSource,
  "if (action === 'restore-data') {\n    return `${operationTargetEnvironmentText(operationDialog.targetEnvironment)} 当前 MySQL / MinIO / 文件对象`\n  }",
  'restore-data target directory data-only copy'
)
assertExcludes(
  pageSource,
  '恢复 MySQL / MinIO / 文件对象、程序版本与 Redis 策略',
  'restore-data overstated program and redis restore copy'
)

console.log('PASS: runtime control restore-data target environment UI contract is wired')
