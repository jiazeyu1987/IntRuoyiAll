const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const approvalShared = readSource('src/views/dcc/controlled-file/shared/approval.ts')
const lifecycleShared = readSource('src/views/dcc/controlled-file/shared/lifecycle.ts')
const handlingSummary = readSource('src/views/dcc/controlled-file/shared/handlingSummary.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:detail-applicant-rework:static'],
  'node tests/e2e/dcc-detail-applicant-rework-static.spec.js',
  'package.json must expose the applicant rework static contract'
)

const returnTargetOptionsMatch = detailPage.match(
  /const returnTargetOptions = computed[\s\S]*?const taskActionDialogTitle/
)
assert.ok(returnTargetOptionsMatch, 'detail page must keep return target option logic together')
const returnTargetOptionsSource = returnTargetOptionsMatch[0]

assert.match(
  detailPage,
  /const APPLICANT_REWORK_TASK_DEFINITION_KEY = 'APPLICANT_REWORK'/,
  'detail page must define a stable applicant rework task key'
)
assert.match(
  returnTargetOptionsSource,
  /label:\s*'申请人修改'[\s\S]*value:\s*APPLICANT_REWORK_TASK_DEFINITION_KEY/,
  'return target options must include applicant rework as "申请人修改"'
)
assert.match(
  returnTargetOptionsSource,
  /currentTaskDefinitionKey === APPLICANT_REWORK_TASK_DEFINITION_KEY[\s\S]*return \[\]/,
  'applicant rework task itself must not offer another return target'
)
assert.match(
  detailPage,
  /fileStatus\.value === 'PENDING_APPLICANT_REWORK'/,
  'returned applicant task detection must use the applicant rework status'
)

const applicantActionBlockMatch = detailPage.match(
  /const approvalActionLabels = computed[\s\S]*?const actionDialogSignatureMeaning/
)
assert.ok(applicantActionBlockMatch, 'detail page must keep applicant action label logic together')
assert.doesNotMatch(
  applicantActionBlockMatch[0],
  /resubmitWithdrawnControlledFile|系统将创建新的 BPM 流程实例/,
  'applicant rework handling must not call withdrawn-flow resubmit or tell users a new BPM process will be created'
)
assert.match(
  applicantActionBlockMatch[0],
  /approveText:\s*'处理回退'/,
  'applicant rework handling must keep the explicit "处理回退" primary action'
)
assert.match(
  detailPage,
  /流程回退处理通过/,
  'applicant rework handling must preview a dedicated signature meaning'
)
assert.match(
  detailPage,
  /<el-button v-if="!isReturnedApplicantTask" type="danger" plain @click="openActionDialog\('reject'\)">/,
  'returned applicant task must hide reject action'
)
assert.match(
  detailPage,
  /<el-button v-if="!isReturnedApplicantTask" plain @click="openTaskActionDialog\('transfer'\)">/,
  'returned applicant task must hide transfer action'
)
assert.match(
  detailPage,
  /<el-button v-if="!isReturnedApplicantTask" plain @click="openTaskActionDialog\('sign'\)">/,
  'returned applicant task must hide add-sign action'
)

for (const [name, source] of [
  ['approval shared status labels', approvalShared],
  ['lifecycle shared status labels', lifecycleShared],
  ['handling summary', handlingSummary]
]) {
  assert.match(source, /PENDING_APPLICANT_REWORK/, `${name} must recognize applicant rework status`)
}

console.log('PASS: DCC detail applicant rework static contract')
