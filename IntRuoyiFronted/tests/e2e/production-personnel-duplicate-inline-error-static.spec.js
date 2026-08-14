const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const extractBlock = (startToken, endToken, label) => {
  const start = source.indexOf(startToken)
  assert.notEqual(start, -1, `${label}: missing start token ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.notEqual(end, -1, `${label}: missing end token ${endToken}`)
  return source.slice(start, end)
}

const dialogMarkerIndex = source.indexOf('data-team-leader-personnel-add-dialog')
assert.notEqual(dialogMarkerIndex, -1, '新增人员 dialog marker must exist.')
const dialogStart = source.lastIndexOf('<el-dialog', dialogMarkerIndex)
const dialogEnd = source.indexOf('</el-dialog>', dialogMarkerIndex)
assert.notEqual(dialogStart, -1, '新增人员 dialog start must exist.')
assert.notEqual(dialogEnd, -1, '新增人员 dialog end must exist.')
const dialogBlock = source.slice(dialogStart, dialogEnd)

assert.match(
  dialogBlock,
  /<template\s+#header>[\s\S]*team-leader-workbench__personnel-dialog-title[\s\S]*新增人员[\s\S]*<\/template>/,
  '新增人员 dialog must use a custom header so the error can render in the highlighted title area.'
)
assert.match(
  dialogBlock,
  /v-if="productionPersonnelDialogError"[\s\S]*data-team-leader-personnel-dialog-error[\s\S]*role="alert"[\s\S]*aria-live="assertive"/,
  'Dialog-scoped error must render as an assertive accessible alert.'
)
assert.match(
  dialogBlock,
  /\{\{\s*productionPersonnelDialogError\s*\}\}/,
  'Dialog header must display the backend error text.'
)
assert.match(
  dialogBlock,
  /data-team-leader-personnel-dialog-error-close[\s\S]*@click="clearProductionPersonnelDialogError"/,
  'Dialog-scoped error must provide a manual close action.'
)
assert.match(
  dialogBlock,
  /@closed="clearProductionPersonnelDialogError"/,
  'Closing the dialog must clear the scoped error and its timer.'
)
assert.match(
  dialogBlock,
  /v-model="temporaryEmployeeForm\.displayName"[\s\S]*@input="clearProductionPersonnelDialogError"/,
  'Editing the temporary employee display name must clear the stale duplicate-name error.'
)

assert.match(
  source,
  /const\s+productionPersonnelDialogError\s*=\s*ref\(''\)/,
  'Dialog-scoped error must use explicit local state.'
)
assert.match(
  source,
  /const\s+PRODUCTION_PERSONNEL_DIALOG_ERROR_DURATION\s*=\s*6000/,
  'Dialog-scoped error must auto-dismiss after six seconds.'
)
assert.match(
  source,
  /const\s+clearProductionPersonnelDialogError\s*=\s*\(\)\s*=>\s*\{[\s\S]*clearTimeout\([\s\S]*productionPersonnelDialogError\.value\s*=\s*''/,
  'Clear handler must cancel the timer and remove the visible error.'
)
assert.match(
  source,
  /const\s+showProductionPersonnelDialogError\s*=\s*\(message:\s*string\)\s*=>\s*\{[\s\S]*productionPersonnelDialogError\.value\s*=\s*message[\s\S]*setTimeout\([\s\S]*PRODUCTION_PERSONNEL_DIALOG_ERROR_DURATION/,
  'Show handler must display the backend text and schedule automatic dismissal.'
)
assert.match(
  source,
  /onBeforeUnmount\(clearProductionPersonnelDialogError\)/,
  'Component teardown must cancel the scoped error timer.'
)

const submitBlock = extractBlock(
  'const submitCreateTemporaryEmployee = async () => {',
  'const updateEmployeeDisplayName = async',
  'temporary employee submit handler'
)
assert.match(
  submitBlock,
  /catch \(error\) \{[\s\S]*showProductionPersonnelDialogError\(\s*resolveErrorMessage\(/,
  'Temporary employee create errors must be routed to the dialog-scoped error.'
)
assert.doesNotMatch(
  submitBlock,
  /ElMessage\.error/,
  'Temporary employee create errors must not use the global Element Plus error message.'
)

assert.match(
  source,
  /\.team-leader-workbench__personnel-dialog-error\s*\{[\s\S]*color:\s*#f56c6c/,
  'Dialog-scoped error text must use the requested red error color.'
)

console.log('PASS: production personnel duplicate inline error static contract')
