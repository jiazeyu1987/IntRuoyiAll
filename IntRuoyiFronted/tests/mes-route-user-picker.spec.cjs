const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref } = require('vue')
const root = path.resolve(__dirname, '..')
const routeFile = 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
const routeSource = fs.readFileSync(path.join(root, routeFile), 'utf8')
function functions(file, names, deps) {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const script = source.endsWith('</script>') ? source : source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)?.[1] || source
  const ast = ts.createSourceFile(file + '.ts', script, ts.ScriptTarget.Latest, true)
  const declarations = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
  assert.equal(declarations.length, names.length)
  const code = ts.transpileModule(declarations.map(s => s.getText(ast).replace(/^export\s+/, '')).join('\n'), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  return Function(...Object.keys(deps), code + ';return {' + names.join(',') + '}')(...Object.values(deps))
}
for (const kind of ['record', 'release']) {
  test(`${kind} candidate failure clears stale rows, surfaces error, and permits a real retry`, async () => {
    const prefix = kind === 'record' ? 'recordBinding' : 'releaseApprovalRule'
    const loader = kind === 'record' ? 'loadRecordBindingUserOptions' : 'loadReleaseApprovalRuleUserOptions'
    const picker = kind === 'record' ? 'loadRecordBindingPickerOptions' : 'loadReleaseApprovalRulePickerOptions'
    const rows = ref([]), loading = ref(false), error = ref(''), report = ref('')
    let reject
    const pending = new Promise((resolve, fail) => { reject = fail })
    let api = () => pending
    const state = functions(routeFile, [loader, picker], {
      [`${prefix}UserOptions`]: rows, [`${prefix}UserOptionsLoading`]: loading,
      [`${prefix}UserOptionsError`]: error, releaseApprovalRuleLoadError: report,
      getSimpleUserList: () => api(), resolveErrorMessage: e => e.message,
      message: { error() {} }
    })
    const request = state[picker]()
    assert.equal(loading.value, true)
    rows.value = [{ id: 99, nickname: 'stale' }]
    reject(new Error('candidate source unavailable'))
    await assert.rejects(request, /candidate source unavailable/)
    assert.deepEqual(rows.value, [])
    assert.equal(error.value, 'candidate source unavailable')
    assert.equal(loading.value, false)
    api = async () => [{ id: 7, nickname: 'current' }]
    assert.deepEqual(await state[picker](), [{ id: 7, nickname: 'current' }])
    assert.equal(error.value, '')
  })
}
test('route supplies saved names only for display and authoritative candidates for selection', () => {
  const blocks = [...routeSource.matchAll(/<UserSelectV2\b[\s\S]*?\/>/g)].map(m => m[0])
  for (const model of ['owner.candidateSourceIds', 'leader.candidateSourceIds', 'getRecordBindingCandidateSourceId(binding)']) {
    const block = blocks.find(b => b.includes(`:model-value="${model}"`))
    assert.ok(block)
    assert.match(block, /:user-options="recordBindingUserOptions"/)
    assert.match(block, /:selected-users="build/)
    assert.match(block, /:candidate-error="recordBindingUserOptionsError"/)
    assert.match(block, /:load-user-options="loadRecordBindingPickerOptions"/)
  }
})
test('dialog cannot confirm missing historical, newly disabled, or failed-source candidates', () => {
  const helperFile = 'src/views/system/user/components/userPicker.ts'
  const helpers = functions(helperFile, ['selectionError', 'validateSelectionLimits'], {})
  const selected = ref([{ id: 12, nickname: 'saved name' }]), candidates = ref([{ id: 7, nickname: 'current' }])
  const props = { min: 0, multiple: false, candidateError: '' }, events = [], warnings = []
  const state = functions('src/views/system/user/components/UserSelectDialogV2.vue', ['blocked', 'confirm'], {
    props, loading: ref(false), error: ref(''), selected, candidates,
    businessMode: ref(true), disabled: ref([]), limit: ref(1), visible: ref(true), activityId: ref(),
    message: { warning: text => warnings.push(text) }, emit: (...args) => events.push(args), ...helpers
  })
  state.confirm(); assert.equal(events.length, 0); assert.match(warnings.pop(), /业务候选已变更/)
  candidates.value = [{ id: 12, status: 1 }]
  state.confirm(); assert.equal(events.length, 0)
  candidates.value = [{ id: 12, status: 0 }]; props.candidateError = 'load failed'
  state.confirm(); assert.equal(events.length, 0)
  props.candidateError = ''; state.confirm(); assert.equal(events.length, 1)
})
