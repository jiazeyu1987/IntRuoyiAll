const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const workflow = fs.readFileSync(path.join(root, 'src/api/dcc/controlledFile/workflow.ts'), 'utf8')
const upload = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/upload/index.vue'), 'utf8')
const detail = fs.readFileSync(path.join(root, 'src/views/dcc/controlled-file/detail/index.vue'), 'utf8')
const browserPresentation = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/browser/presentation.ts'),
  'utf8'
)
const approvalActions = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/detail/approval-actions.ts'),
  'utf8'
)

for (const forbiddenField of ['sourceFileId', 'originalFileId', 'publishedFileId', 'stampedFileId']) {
  const ordinaryTypeBlocks = [
    workflow.slice(
      workflow.indexOf('export interface ControlledFileVersionHistoryVO'),
      workflow.indexOf('export interface ControlledFileDistributionStatusVO')
    ),
    workflow.slice(
      workflow.indexOf('export interface ControlledFileVO'),
      workflow.indexOf('export interface ExternalFileReviewVO')
    )
  ]
  ordinaryTypeBlocks.forEach((block) => {
    assert.doesNotMatch(block, new RegExp(`\\b${forbiddenField}\\b`),
      `ordinary DCC response type must not expose ${forbiddenField}`)
  })
}

assert.match(workflow, /publishedArtifactAvailable\?: boolean/)
assert.match(workflow, /stampedArtifactAvailable\?: boolean/)
assert.match(workflow, /previewUnavailableReason\?: string/)
assert.match(workflow, /interface ControlledFileRouteReadinessVO/)
assert.match(workflow, /selectedSignoffUserIds: number\[\]/)
assert.match(workflow, /Promise<ControlledFileRouteReadinessVO>/)
assert.match(workflow, /interface ControlledFileTaskReadinessVO/)
assert.match(workflow, /getControlledFileTaskActionReadiness/)
assert.match(workflow, /task-action-readiness/)

assert.match(upload, /data-testid="dcc-upload-route-readiness"/)
assert.match(upload, /checkControlledFileRouteReadiness/)
assert.match(upload, /routeReadiness\.value\.blockers/)
assert.match(upload, /await refreshRouteReadiness\(\)/)
assert.match(upload, /if \(!routeReadiness\.value\?\.ready\)/)

assert.match(detail, /data-testid="dcc-task-action-readiness"/)
assert.match(detail, /getControlledFileTaskActionReadiness/)
assert.match(detail, /taskActionReadiness\.blockers/)
assert.match(detail, /await refreshTaskActionReadiness\(\)/)
assert.match(detail, /if \(!taskActionReadiness\.ready\)/)

assert.doesNotMatch(browserPresentation, /publishedFileId|stampedFileId/)
assert.match(browserPresentation, /publishedArtifactAvailable/)
assert.match(browserPresentation, /stampedArtifactAvailable/)
assert.match(approvalActions, /1080000199\|审批人未配置系统岗位/)
assert.match(approvalActions, /1080000201\|审批路线快照与实际任务分配不一致/)
assert.ok(
  approvalActions.indexOf('1080000199|审批人未配置系统岗位') <
    approvalActions.indexOf('evidence|snapshot|hash|persist'),
  'specific approver post error must be resolved before generic evidence errors'
)

console.log('PASS: DCC readiness and business capability projections are wired end to end')
