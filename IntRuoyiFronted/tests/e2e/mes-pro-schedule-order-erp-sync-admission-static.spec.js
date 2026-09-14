const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/scheduleorder/index.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/scheduleorder/index.ts'), 'utf8')

assert.match(api, /reasonCode:\s*string/, 'admission diff API row must expose reasonCode')
assert.match(api, /message:\s*string/, 'admission diff API row must expose backend message')
assert.match(api, /selectable:\s*boolean/, 'admission diff API row must expose selectable')

assert.match(
  page,
  /<el-table-column type="selection" width="48" :selectable="isAdmissionRowSelectable" \/>/,
  'admission table selection must delegate to row selectable guard'
)
assert.match(
  page,
  /const isAdmissionRowSelectable = \(row: MesProScheduleOrderAdmissionDiffRowVO\) => \{[\s\S]*row\.selectable && row\.admissionStatus === 'READY_TO_ADMIT'[\s\S]*\}/,
  'admission row guard must allow only backend-selectable READY_TO_ADMIT rows'
)
assert.match(
  page,
  /row\.message \|\| getReasonCodeText\(row\.reasonCode\)/,
  'admission reason cell must render backend message before local fallback text'
)
assert.match(
  page,
  /const rows = selectedWorkOrders\.value\.filter\(isAdmissionRowSelectable\)/,
  'batch admission submit must re-filter selected rows with the same selectable guard'
)
assert.match(
  page,
  /BLOCKED_ERP_SYNC_RECORD_MISSING:\s*'缺 ERP 正式订单'/,
  'ERP formal identity blocker must have a user-visible reason label'
)
