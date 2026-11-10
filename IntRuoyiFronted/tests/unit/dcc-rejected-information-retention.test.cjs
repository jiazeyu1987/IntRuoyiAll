const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')

test('actual rejected upload and revision checkin entry restores saved description, remark and training', () => {
  const source = fs.readFileSync('src/views/dcc/controlled-file/browser/index.vue', 'utf8')
  const from = source.indexOf('const handleCheckin ='), end = source.indexOf('const resetCheckinDialog =', from)
  for (const status of ['REJECTED', 'PENDING_APPLICANT_REWORK', 'WITHDRAWN', 'ACTIVE']) {
    const state = { exports: {}, checkinSubmitting: vue.ref(false), checkinCleanupLoading: vue.ref(false),
      checkinUploadLoading: vue.ref(false), checkinDrawingPdfLoading: vue.ref(false),
      checkinAttachmentState: vue.ref({ uploading: false, cleaning: false }),
      checkinTarget: vue.ref(), checkinForm: { changeDescription: 'stale', remark: 'stale', needTraining: false },
      checkinDialogVisible: vue.ref(false), isValidBrowserOptionId: id => id === '20',
      isCheckedOutByCurrentUser: () => true, requireCheckinTrainingDefault: file => file.needTraining,
      message: { error: error => { throw Error(error) }, warning: error => { throw Error(error) } },
      resetCheckinDialog: () => Object.assign(state.checkinForm, { changeDescription: '', remark: '', needTraining: false }) }
    vm.runInNewContext(ts.transpileModule(source.slice(from, end) + '\nexports.open=handleCheckin',
      { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, state)
    const file = { id: '20', status, changeDescription: '原申请说明', remark: '原申请备注', needTraining: true }
    state.exports.open(file)
    assert.equal(state.checkinDialogVisible.value, true); assert.equal(state.checkinForm.needTraining, true)
    assert.equal(state.checkinForm.changeDescription, status === 'ACTIVE' ? '' : file.changeDescription)
    assert.equal(state.checkinForm.remark, status === 'ACTIVE' ? '' : file.remark)
    assert.equal(file.remark, '原申请备注')
  }
})

const existing = fs.readFileSync('tests/unit/dcc-version-display-wiring.test.cjs', 'utf8')
const setup = existing.slice(0, existing.indexOf("test('revision without"))
const ui = new Function('require', '__dirname', setup + '\nreturn {component,mount,findAll,flush}')(require, __dirname)
test('actual revision form reuses the saved rework description and preserves edits when the same source refreshes', async () => {
  const commands = [], facts = { projectAttributes: { targetMarkets: ['CE'], licenseHolder: 'Y', actualManufacturer: 'N', documentTransfer: 'N' },
    selectedSignoffDepartmentIds: ['51'], selectedDistributionDepartmentIds: ['52'], needTraining: false,
    effectiveMode: 'AFTER_STANDARDIZATION_SIGNATURE', effectiveDate: null }
  const props = vue.reactive({ baselineId: '300', baselineVersionNo: 'A/1', partialTarget: { versionNo: 'A/2', unavailableReason: null },
    replacementTarget: {}, idempotencyKey: 'rework-request', applicationFacts: facts, applicationReady: true,
    initialSelectedIterationId: '301', initialIntent: 'PARTIAL', initialDescription: '被驳回申请已填写说明', facts: {},
    iterations: [{ id: '301', versionNo: 'A/1-1', canPartial: true, canReplacement: false, canPreview: false },
      { id: '302', versionNo: 'A/1-2', canPartial: true, canReplacement: false, canPreview: false }],
    canPartial: true, canReplacement: false, submitRevision: async command => commands.push(command) })
  const component = ui.component('revision/DccRevisionPanel.vue')
  const mounted = ui.mount({ setup: () => () => vue.h(component, props) }, {})
  const field = () => ui.findAll(mounted.root, 'el-input')[0]
  const submit = () => ui.findAll(mounted.root, 'el-button').find(row => row.props['data-testid'] === 'dcc-revision-submit')
  try {
    await ui.flush(); assert.equal(field().props.modelValue, '被驳回申请已填写说明')
    await submit().props.onClick(); assert.equal(commands[0].changeDescription, '被驳回申请已填写说明')
    field().props['onUpdate:modelValue']('用户本次修正说明'); await ui.flush()
    props.initialDescription = ''; await ui.flush(); assert.equal(field().props.modelValue, '用户本次修正说明')
    props.initialSelectedIterationId = '302'; props.initialDescription = '另一工作正文的保存说明'; await ui.flush()
    assert.equal(field().props.modelValue, '另一工作正文的保存说明')
    await submit().props.onClick(); assert.equal(commands[1].selectedIterationId, '302')
    assert.equal(commands[1].changeDescription, '另一工作正文的保存说明'); assert.deepEqual(mounted.errors, [])
  } finally { mounted.unmount() }
})
