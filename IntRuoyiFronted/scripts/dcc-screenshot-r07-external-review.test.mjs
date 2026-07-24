import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('R07 external review has independent page fields and API endpoints', () => {
  const routeSource = readText('src/router/modules/remaining.ts')
  const pageSource = readText('src/views/dcc/controlled-file/external-review/index.vue')
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  assert.match(routeSource, /controlled-file\/external-review/)
  assert.match(routeSource, /@\/views\/dcc\/controlled-file\/external-review\/index\.vue/)
  for (const label of ['外来来源', '外来归属', '评审原因', '参与人']) {
    assert.match(pageSource, new RegExp(label), `external review page must render ${label}`)
  }
  assert.match(pageSource, /submitExternalFileReview/)
  assert.match(pageSource, /participantUserIds/)
  assert.match(workflowSource, /\/dcc\/external-file-reviews\/submit/)
  assert.match(workflowSource, /dcc-external-file-review/)
  assert.doesNotMatch(pageSource, /submitControlledFile\(/)
})

test('R07 detail page renders external conclusion output and skips normal stamped release controls', () => {
  const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
  const workflowSource = readText('src/api/dcc/controlledFile/workflow.ts')

  for (const label of ['外来文件评审信息', '评审结论', '输出文件']) {
    assert.match(detailSource, new RegExp(label), `detail must render ${label}`)
  }
  assert.match(detailSource, /approveExternalFileReviewTask/)
  assert.match(detailSource, /isExternalReviewProcess/)
  assert.match(detailSource, /shouldCollectExternalReviewConclusion/)
  assert.match(workflowSource, /externalReview\?:/)
  assert.match(workflowSource, /EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY/)
})

test('R07 approval task center includes the independent external review BPM key', () => {
  const approvalTasksSource = readText('src/views/dcc/controlled-file/approval-tasks/index.vue')

  assert.match(approvalTasksSource, /CONTROLLED_FILE_PROCESS_DEFINITION_KEY/)
  assert.match(approvalTasksSource, /EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY/)
  assert.match(approvalTasksSource, /DCC_APPROVAL_PROCESS_DEFINITION_KEYS/)
})
