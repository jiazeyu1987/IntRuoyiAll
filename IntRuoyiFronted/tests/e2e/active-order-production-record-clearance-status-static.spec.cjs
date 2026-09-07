const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const detailPanel = path.join(
  root,
  'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const workbenchPage = path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

const detailSource = fs.readFileSync(detailPanel, 'utf8')
const workbenchSource = fs.readFileSync(workbenchPage, 'utf8')

function assertIncludes(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`${label} missing expected snippet: ${expected}`)
  }
}

assertIncludes(
  detailSource,
  "const confirmation = (confirmations ?? []).find((item) => item.key === key)",
  'detail panel clearance formatter'
)
assertIncludes(
  detailSource,
  "if (!confirmation) return '未记录'",
  'detail panel must not default empty clearance to confirmed'
)
assertIncludes(
  detailSource,
  'clearanceConfirmations: material.clearanceConfirmations?.length',
  'production record rows must prefer material-level clearance facts'
)
assertIncludes(
  detailSource,
  '? material.clearanceConfirmations',
  'production record rows must read material-level clearance facts before submission fallback'
)
assertIncludes(
  detailSource,
  "if (value === true) return '计量有效'",
  'detail panel metering formatter'
)
assertIncludes(
  detailSource,
  "if (value === false) return '计量超期'",
  'detail panel metering formatter'
)
assertIncludes(
  detailSource,
  "return '计量状态未记录'",
  'detail panel must not default empty metering to valid'
)
assertIncludes(
  workbenchSource,
  'normalizeSubmissionArray(rootPayload?.clearanceConfirmations)',
  'workbench timeline clearance formatter must read payload facts'
)
assertIncludes(
  workbenchSource,
  "return '--'",
  'workbench timeline must not default missing clearance to selected'
)

console.log('PASS: active order production record clearance and device status static contract')
