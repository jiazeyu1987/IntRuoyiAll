const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const apiSource = fs.readFileSync(
  path.join(repoRoot, 'src/api/infra/runtimeControl/index.ts'),
  'utf8'
)
const pageSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/index.vue'),
  'utf8'
)
const pickerSource = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/runtime-control/components/OpsCandidatePicker.vue'),
  'utf8'
)

function assertIncludes(source, fragment, label) {
  assert(source.includes(fragment), `missing ${label}: ${fragment}`)
}

function assertExcludes(source, fragment, label) {
  assert(!source.includes(fragment), `forbidden ${label}: ${fragment}`)
}

for (const fragment of [
  'selectedRecoverySetCandidateId?: string',
  'recoverySetId?: string',
  'recoverySetStatus?: RuntimeControlCandidateStatus',
  'programVersion?: string',
  'redisPolicy?: string',
  'configurationManifestPath?: string',
  'recoverySetManifestHash?: string',
  'componentSummary?: Record<string, string>',
  'compatibilityStatus?: string',
  'compatibilityEvidencePath?: string',
  'compatibilityCheckedAt?: string',
  'compatibilitySummary?: string'
]) {
  assertIncludes(apiSource, fragment, `runtime-control API field ${fragment}`)
}

assertIncludes(
  pageSource,
  'selectedRecoverySetCandidateId',
  'operation dialog selected recovery-set candidate state'
)
assertIncludes(
  pageSource,
  'operationDialog.action === \'mark-release-tested\'',
  'mark-release-tested branch'
)
assertIncludes(
  pageSource,
  'operationDialog.selectedRecoverySetCandidateId',
  'mark-tested and restore payload recovery-set binding'
)
assertIncludes(
  pageSource,
  '恢复同一恢复集',
  'restore same recovery set copy'
)
assertIncludes(
  pageSource,
  '兼容性成立后只回滚程序',
  'rollback compatibility boundary copy'
)
assertExcludes(
  pageSource,
  'selectedBackupCandidateId:',
  'old restore backup candidate payload'
)

assertIncludes(pickerSource, '恢复集候选', 'recovery set picker title')
assertIncludes(pickerSource, 'recoverySetId', 'recovery set id display')
assertIncludes(pickerSource, 'programVersion', 'program version display')
assertIncludes(pickerSource, 'redisPolicy', 'redis policy display')
assertIncludes(pickerSource, 'configurationManifestPath', 'configuration manifest display')
assertIncludes(pickerSource, 'recoverySetManifestHash', 'manifest hash display')
assertIncludes(pickerSource, 'componentSummary', 'component summary display')
assertIncludes(pickerSource, 'compatibilityEvidencePath', 'rollback compatibility evidence display')
assertIncludes(pickerSource, 'compatibilityStatus', 'rollback compatibility status display')
assertIncludes(pickerSource, 'compatibilityCheckedAt', 'rollback compatibility check time display')
assertIncludes(pickerSource, 'compatibilitySummary', 'rollback compatibility summary display')

console.log('PASS: runtime-control recovery-set and rollback compatibility static contract is wired')
