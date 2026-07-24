const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const executionPagePath = path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue')
const source = fs.readFileSync(executionPagePath, 'utf8')

const goldenPermission = 'mes:pro-batch-record-execution:golden-finger'

assert(
  source.includes(`const GOLDEN_FINGER_PERMISSION = '${goldenPermission}'`),
  'ExecutionPage must define the explicit eDHR golden finger permission code.'
)

assert(
  /const hasGoldenFingerPermission = computed\(\s*\(\) =>\s*userStore\.permissions\.has\(GOLDEN_FINGER_PERMISSION\)\s*\)/.test(source),
  'Frontend golden finger mode must require the explicit permission in the permission set, not only wildcard permission matching.'
)

assert(
  /const hasFieldAuditUpdatePermission = computed\([\s\S]*hasGoldenFingerPermission\.value[\s\S]*\)/.test(source),
  'Golden finger users must be able to reuse the existing field-audit save entry.'
)

assert(
  /v-hasPermi="\['mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger'\]"/.test(source),
  'Submit buttons guarded by v-hasPermi must include the golden finger permission.'
)

assert(
  /const sharedFillScopeGateError = computed\(\(\) => \{[\s\S]*hasGoldenFingerPermission\.value[\s\S]*return ''/.test(source),
  'Golden finger mode must bypass shared fill-scope gate while keeping other hard locks.'
)

assert(
  /const isFieldInCurrentFillScope = \(field: NormalizedSnapshotField\) => \{[\s\S]*hasGoldenFingerPermission\.value[\s\S]*return true/.test(source),
  'Golden finger mode must allow editing all fields on the current pre-release form.'
)

assert(
  /const formSubmitGateError = computed\(\(\) => \{[\s\S]*!hasGoldenFingerPermission\.value[\s\S]*attachmentRequirementCompletion\.value\.missingFields[\s\S]*!hasGoldenFingerPermission\.value[\s\S]*missingRequiredFieldsSubmitError\.value/.test(source),
  'Golden finger submit must skip ordinary required-field and attachment-completeness gates only.'
)

assert(
  /const isReadonly = computed\(\(\) => \{[\s\S]*!hasFillTaskContext\.value[\s\S]*isInactiveRevisionDraft\.value[\s\S]*EDHR_EXECUTION_STATUS\.DRAFT[\s\S]*isPreReleaseEditable\.value[\s\S]*return true/.test(source),
  'Golden finger mode must keep the same hard readonly locks: work task required, active revision required, and only draft or pre-release editable forms are writable.'
)

assert(
  source.includes('金手指测试权限') &&
    source.includes('可绕过放行、关闭、作废或审批锁定') &&
    !source.includes('不绕过放行、关闭、作废或审批锁定'),
  'The page must visibly warn users that golden finger can bypass action locks for testing.'
)

console.log('PASS: eDHR golden finger frontend static contract')
