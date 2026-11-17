const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { computed } = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = 'src/views/dcc/controlled-file/detail/index.vue'
const source = fs.readFileSync(filename, 'utf8')
const handlingStart = source.indexOf('<template v-if="isApprovalUploadHandlingPage">')
const historyStart = source.indexOf('<DetailApplicationHistory', handlingStart)
const handling = source.slice(handlingStart, source.indexOf('<ContentWrap class="dcc-detail-surface" v-if="!showLifecycleTraceSections', handlingStart))

test('shared layout applies to approval-center handling across review and approval stages only', () => {
  const { descriptor } = parse(source)
  const ast = ts.createSourceFile('detail.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const declaration = ast.statements.find(node => ts.isVariableStatement(node)
    && node.declarationList.declarations.some(item => item.name.text === 'isApprovalUploadHandlingPage'))
  for (const taskDefinitionKey of ['SUPERVISOR_REVIEW', 'MATRIX_REVIEW', 'MATRIX_APPROVAL', 'DOC_CONTROL_APPROVAL']) {
    for (const query of [
      { handling: 'approval', from: 'approval-center' },
      { handling: 'approval' }, { from: 'approval-center' }, {},
      { handling: 'preview', from: 'approval-center' }
    ]) {
      const env = { computed, route: { query }, approvalTodoTask: { value: { taskDefinitionKey } } }
      vm.runInNewContext(ts.transpileModule(declaration.getText(ast)
        + '\nglobalThis.applies = isApprovalUploadHandlingPage.value', {
        compilerOptions: { target: ts.ScriptTarget.ES2022 }
      }).outputText, env)
      assert.equal(env.applies, query.handling === 'approval' && query.from === 'approval-center', taskDefinitionKey)
    }
  }
  assert.match(source, /class="dcc-detail-layout" :class="\{ 'dcc-review-layout': isApprovalUploadHandlingPage \}"/)
})

test('shared header preserves live file identity, status, return and guarded withdrawal', () => {
  const header = source.slice(source.indexOf('<div class="detail-header-shell">'), source.indexOf('<el-alert', source.indexOf('<div class="detail-header-shell">')))
  for (const binding of ['fileDetail?.fileName', 'fileDetail?.fileNumber',
    'getDccVersionDisplayText(fileDetail?.versionNo)', 'getDetailStatusLabel(fileDetail?.status)',
    '@click="router.back()"', 'v-if="canWithdraw"', '@click="handleWithdraw"']) assert.ok(header.includes(binding), binding)
})

test('shared approval layout compiles and displays one protected body before the task and application history', () => {
  const { descriptor, errors } = parse(source)
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: filename })
  const compiled = compileTemplate({ source: descriptor.template.content, filename, id: filename,
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(compiled.errors, [])
  const previewSource = fs.readFileSync('src/views/dcc/controlled-file/detail/ReviewDocumentPreview.vue', 'utf8')
  assert.equal((handling.match(/<ReviewDocumentPreview\b/g) || []).length, 1)
  assert.equal((previewSource.match(/<ProtectedPdfViewer\b/g) || []).length, 1)
  assert.match(previewSource, /#document-entry>[\s\S]*?主文档[\s\S]*?<ControlledFileAttachments/)
  assert.doesNotMatch(handling, /<el-row|<el-col/)
  const preview = handling.indexOf('<ReviewDocumentPreview')
  const task = handling.indexOf('data-testid="dcc-approval-upload-action-panel"')
  assert.ok(preview > 0 && task > preview)
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
