const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = 'src/views/dcc/controlled-file/detail/index.vue'
const source = fs.readFileSync(filename, 'utf8')
const handlingStart = source.indexOf('<template v-if="isApprovalUploadHandlingPage">')
const historyStart = source.indexOf('<DetailApplicationHistory', handlingStart)
const handling = source.slice(handlingStart, source.indexOf('<ContentWrap class="dcc-detail-surface" v-if="!showLifecycleTraceSections', handlingStart))

test('shared approval layout compiles and displays one protected body before the task and application history', () => {
  const { descriptor, errors } = parse(source)
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: filename,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
  assert.equal((handling.match(/<ProtectedPdfViewer\b/g) || []).length, 1)
  assert.equal((handling.match(/<ControlledFileAttachments\b/g) || []).length, 1)
  assert.doesNotMatch(handling, /<el-row|<el-col/)
  const preview = handling.indexOf('<ProtectedPdfViewer')
  const attachments = handling.indexOf('<ControlledFileAttachments')
  const task = handling.indexOf('data-testid="dcc-approval-upload-action-panel"')
  assert.ok(preview > 0 && attachments > preview && task > attachments)
  assert.ok(source.indexOf('<DetailRelationsPanel', historyStart) > historyStart)
  for (const field of ['项目', '存储文件夹', '文件分类', '产品编号', '编制人', '培训要求', '提交备注']) {
    assert.ok(handling.slice(0, preview).includes(`label="${field}"`), field)
  }
})

test('assignment is inside the current task with its real context and signature handlers', () => {
  const task = handling.slice(handling.indexOf('data-testid="dcc-approval-upload-action-panel"'))
  const assignment = task.slice(task.indexOf('<DetailSignoffAssignment'), task.indexOf('</DetailSignoffAssignment>'))
  for (const binding of ['canShowSignoffAssignment', ':file-id="fileDetail.id"',
    ':process-instance-id="approvalProcessInstanceId"', ':task-id="String(approvalTodoTask.id)"',
    '@state="handleSignoffAssignmentState"', '@saved="handleSignoffAssignmentSaved"']) {
    assert.ok(assignment.includes(binding), binding)
  }
  assert.match(task, /!canShowSignoffAssignment/)
  assert.match(source, /v-if="!isApprovalUploadHandlingPage && fileDetail && approvalTodoTask && canShowSignoffAssignment"/)
  assert.match(source, /v-if="canWithdraw"[^>]*data-testid="dcc-withdraw-application"/)
})
