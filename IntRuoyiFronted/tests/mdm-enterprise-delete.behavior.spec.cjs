const { test } = require('node:test')
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../src/views/mdm/enterprise/index.vue')
const { descriptor, errors } = parse(readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const ast = ts.createSourceFile('enterprise.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const declaration = ast.statements.find(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(item => item.name.getText(ast) === 'handleDelete'))
const handlerCode = ts.transpileModule(declaration.getText(ast), {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText

const row = { id: 301, name: '测试公司', enterpriseCode: 'TEST-301' }
const setup = (options = {}) => {
  const events = []
  const context = {
    deleting: { value: false },
    selectedEnterprises: { value: [row] },
    list: { value: options.rows || [row] },
    queryParams: { pageNo: options.pageNo || 1 },
    message: {
      delConfirm: async text => {
        events.push(['confirm', text])
        if (options.confirmError) throw options.confirmError
        if (options.confirm) await options.confirm()
      },
      success: text => events.push(['success', text])
    },
    MdmEnterpriseApi: {
      deleteEnterprise: async id => {
        events.push(['delete', id])
        if (options.deleteError) throw options.deleteError
      }
    },
    getList: async () => {
      events.push(['refresh'])
      if (options.refreshError) throw options.refreshError
    }
  }
  const handleDelete = Function(...Object.keys(context), `${handlerCode}; return handleDelete`)(...Object.values(context))
  return { ...context, events, handleDelete }
}

test('enterprise page script and template compile with selection and toolbar delete', () => {
  const script = compileScript(descriptor, { id: 'enterprise' })
  const template = compileTemplate({ source: descriptor.template.content, filename, id: 'enterprise',
    compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(template.errors, [])
})

test('confirmed deletion uses the selected id, refreshes and leaves the previous page when last row is removed', async () => {
  const state = setup({ pageNo: 2 })
  await state.handleDelete(row)
  assert.deepEqual(state.events.map(event => event[0]), ['confirm', 'delete', 'success', 'refresh'])
  assert.match(state.events[0][1], /测试公司/)
  assert.equal(state.events[1][1], 301)
  assert.equal(state.queryParams.pageNo, 1)
  assert.deepEqual(state.selectedEnterprises.value, [])
  assert.equal(state.deleting.value, false)
})

test('deleting one of multiple rows retains the current page', async () => {
  const state = setup({ pageNo: 2, rows: [row, { id: 302 }] })
  await state.handleDelete(row)
  assert.equal(state.queryParams.pageNo, 2)
})

test('cancel and close perform no deletion or refresh', async () => {
  for (const confirmError of ['cancel', 'close']) {
    const state = setup({ confirmError })
    await state.handleDelete(row)
    assert.deepEqual(state.events.map(event => event[0]), ['confirm'])
    assert.equal(state.deleting.value, false)
  }
})

test('confirmation, deletion and refresh failures propagate and release the pending state', async () => {
  for (const key of ['confirmError', 'deleteError', 'refreshError']) {
    const failure = new Error(key)
    const state = setup({ [key]: failure })
    await assert.rejects(state.handleDelete(row), error => error === failure)
    assert.equal(state.deleting.value, false)
    if (key !== 'refreshError') {
      assert.equal(state.events.some(event => event[0] === 'success'), false)
      assert.equal(state.events.some(event => event[0] === 'refresh'), false)
      assert.deepEqual(state.selectedEnterprises.value, [row])
    }
  }
})

test('pending confirmation prevents duplicate deletion; no selection performs no action', async () => {
  let confirm
  const waiting = new Promise(resolve => { confirm = resolve })
  const state = setup({ confirm: () => waiting })
  await state.handleDelete(undefined)
  assert.deepEqual(state.events, [])
  const first = state.handleDelete(row)
  await state.handleDelete(row)
  confirm()
  await first
  assert.equal(state.events.filter(event => event[0] === 'delete').length, 1)
})
