import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const approvalCenterView = fs.readFileSync(
  path.join(root, 'src/views/approval-center/index.vue'),
  'utf8'
)
const showroomApprovalPanel = fs.readFileSync(
  path.join(root, 'src/views/showroom-admin/approval/ApprovalTaskPanel.vue'),
  'utf8'
)
const showroomRoute = fs.readFileSync(path.join(root, 'src/router/modules/showroom.ts'), 'utf8')

assert.match(approvalCenterView, /openModuleDetail/, 'approval center must keep module detail navigation')
assert.doesNotMatch(
  approvalCenterView,
  /supervisorApprove|gaoxinApprove|BPM 通用审批|bpmApprove/i,
  'approval center must not execute Showroom approval actions'
)
assert.match(showroomRoute, /path:\s*'approval'/, 'Showroom formal approval route is required')
assert.match(
  showroomApprovalPanel,
  /route\.query\.changeRequestId/,
  'Showroom official approval page must read changeRequestId from unified center route'
)
assert.match(
  showroomApprovalPanel,
  /activeId\.value\s*=\s*.*changeRequestId/s,
  'Showroom official approval page must select the unified-center change request'
)

process.stdout.write('approval-center phase3 static contract passed\n')
