const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../src/views/dcc/registration-certificate/index/index.vue')
const { descriptor, errors } = parse(readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('index.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(item => item.name.getText(ast) === 'handleDeleteCertificate'))
const code = ts.transpileModule(declaration.getText(ast), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const row = { certificateId: '9007199254740993', certificateNo: '测试注册证', rowVersion: 7 }
const setup = (options = {}) => {
  const events = []
  let commandNumber = 0
  const rows = options.rows ?? [row]
  const context = {
    deletingCertificate: { value: false }, selectedCertificates: { value: rows },
    list: { value: options.list ?? rows }, queryParams: { pageNo: 2 },
    message: {
      delConfirm: async text => { events.push(['confirm', text]); if (options.confirmError) throw options.confirmError },
      success: text => events.push(['success', text]),
      error: text => events.push(['error', text])
    },
    deleteRegistrationCertificate: async (...args) => {
      events.push(['delete', ...args])
      if (options.deleteError && (!options.failedId || options.failedId === args[0])) throw options.deleteError
    },
    loadPage: async () => { events.push(['current']); if (options.refreshError) throw options.refreshError },
    loadOldIndexPage: async () => events.push(['old']),
    crypto: { randomUUID: () => `delete-command-key-${++commandNumber}` }
  }
  const handleDeleteCertificate = Function(...Object.keys(context), `${code}; return handleDeleteCertificate`)(...Object.values(context))
  return { ...context, events, handleDeleteCertificate }
}
test('registration certificate script and template compile', () => {
  const script = compileScript(descriptor, { id: 'registration' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'registration',
    compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.match(descriptor.template.content, /:disabled="selectedCertificates.length === 0 \|\| loading"/)
  assert.match(descriptor.template.content, /@click="handleDeleteCertificate"/)
})
test('delete preserves string ids, sends revision and command key, then refreshes both lists', async () => {
  const state = setup()
  await state.handleDeleteCertificate()
  assert.deepEqual(state.events.map(event => event[0]), ['confirm', 'delete', 'current', 'old', 'success'])
  assert.match(state.events[0][1], /历史版本、附件和审计证据仍保留/)
  assert.deepEqual(state.events[1], ['delete', row.certificateId, 7, 'delete-command-key-1'])
  assert.equal(state.queryParams.pageNo, 1)
  assert.deepEqual(state.selectedCertificates.value, [])
  assert.equal(state.deletingCertificate.value, false)
})
test('cancel and close never delete or refresh', async () => {
  for (const confirmError of ['cancel', 'close']) {
    const state = setup({ confirmError })
    await state.handleDeleteCertificate()
    assert.deepEqual(state.events.map(event => event[0]), ['confirm'])
    assert.equal(state.deletingCertificate.value, false)
  }
})
test('unexpected confirmation, API and refresh failures propagate and reset pending state', async () => {
  for (const key of ['confirmError', 'refreshError']) {
    const error = new Error(key)
    const state = setup({ [key]: error })
    await assert.rejects(state.handleDeleteCertificate(), actual => actual === error)
    assert.equal(state.deletingCertificate.value, false)
    if (key !== 'refreshError') assert.equal(state.events.some(event => event[0] === 'success'), false)
  }
})
test('missing selection and duplicate clicks do not issue another delete', async () => {
  const state = setup({ rows: [] })
  await state.handleDeleteCertificate()
  state.selectedCertificates.value = [row]
  state.deletingCertificate.value = true
  await state.handleDeleteCertificate()
  assert.deepEqual(state.events, [])
})

test('multiple selections each send their own id, revision and command key', async () => {
  const second = { certificateId: '9007199254740994', certificateNo: '第二条', rowVersion: 9 }
  const third = { certificateId: '9007199254740995', certificateNo: '第三条', rowVersion: 11 }
  const state = setup({ rows: [row, second, third] })
  await state.handleDeleteCertificate()
  assert.match(state.events[0][1], /选中的 3 条注册证/)
  assert.deepEqual(state.events.filter(event => event[0] === 'delete'), [
    ['delete', row.certificateId, 7, 'delete-command-key-1'],
    ['delete', second.certificateId, 9, 'delete-command-key-2'],
    ['delete', third.certificateId, 11, 'delete-command-key-3']
  ])
  assert.equal(state.queryParams.pageNo, 1)
  assert.match(state.events.at(-1)[1], /已删除 3 条/)
})

test('partial failure reports failed certificates, refreshes successful deletions and propagates errors', async () => {
  const second = { certificateId: '9007199254740994', certificateNo: '删除失败的注册证', rowVersion: 9 }
  const error = new Error('审批中不允许删除')
  const state = setup({ rows: [row, second], deleteError: error, failedId: second.certificateId })
  await assert.rejects(state.handleDeleteCertificate(), actual =>
    actual instanceof AggregateError && actual.errors[0] === error)
  assert.deepEqual(state.events.map(event => event[0]), ['confirm', 'delete', 'delete', 'error', 'current', 'old'])
  assert.match(state.events[3][1], /已删除 1 条，1 条删除失败：删除失败的注册证/)
  assert.equal(state.queryParams.pageNo, 2)
  assert.equal(state.deletingCertificate.value, false)
})

test('all failures retain selection and pagination and never show success', async () => {
  const error = new Error('无删除权限')
  const state = setup({ deleteError: error })
  await assert.rejects(state.handleDeleteCertificate(), actual =>
    actual instanceof AggregateError && actual.errors[0] === error)
  assert.deepEqual(state.events.map(event => event[0]), ['confirm', 'delete', 'error'])
  assert.deepEqual(state.selectedCertificates.value, [row])
  assert.equal(state.queryParams.pageNo, 2)
  assert.equal(state.deletingCertificate.value, false)
})
