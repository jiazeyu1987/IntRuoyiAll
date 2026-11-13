const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const qs = require('qs')

const root = path.resolve(__dirname, '../..')
const source = parse(fs.readFileSync(path.join(root, 'src/views/system/user/index.vue'), 'utf8'))
  .descriptor.scriptSetup.content
const ast = ts.createSourceFile('user.ts', source, ts.ScriptTarget.Latest, true)
const names = new Set(['userSortState', 'listRequestId', 'getList', 'handleUserSortChange'])
const declarations = ast.statements.filter(statement => ts.isVariableStatement(statement) &&
  statement.declarationList.declarations.some(declaration => names.has(declaration.name.getText(ast))))
assert.equal(declarations.length, names.size)

// Exercise the page handlers with the application's actual query serializer.
const service = ts.createSourceFile('service.ts', fs.readFileSync(path.join(root,
  'src/config/axios/service.ts'), 'utf8'), ts.ScriptTarget.Latest, true)
let serializer
function findSerializer(node) {
  if (ts.isPropertyAssignment(node) && node.name.getText(service) === 'paramsSerializer') {
    serializer = vm.runInNewContext(`(${node.initializer.getText(service)})`, { qs })
  }
  ts.forEachChild(node, findSerializer)
}
findSerializer(service)
assert.equal(typeof serializer, 'function')

function setup() {
  const requests = []
  const queryParams = { pageNo: 1, pageSize: 20, deptId: 10, listFilters: '[]' }
  const context = {
    ref: value => ({ value }), queryParams, loading: {}, list: {}, total: {},
    buildUserTableRows: rows => rows,
    UserApi: { getUserPage: async params => {
      requests.push(new URLSearchParams(serializer(params)))
      return { list: [{ id: 1 }], total: 1 }
    } }
  }
  const code = declarations.map(statement => statement.getText(ast)).join('\n') +
    '\n globalThis.handlers = { getList, handleUserSortChange }'
  vm.runInNewContext(ts.transpileModule(code, {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { ...context.handlers, context, requests, queryParams }
}

test('entering user management omits inactive sorting from the serialized request', async () => {
  const page = setup()
  await page.getList()
  assert.equal(page.requests[0].has('sortOrder'), false)
  assert.equal(page.requests[0].has('sortKey'), false)
  assert.equal(page.requests[0].get('deptId'), '10')
  assert.equal(page.context.total.value, 1)
  assert.equal(page.context.loading.value, false)
})

for (const order of ['ascending', 'descending']) {
  test(`${order} is sent unchanged and cancelling sorting removes both parameters`, async () => {
    const page = setup()
    page.queryParams.pageNo = 3
    await page.handleUserSortChange({ key: 'username', order })
    assert.equal(page.requests[0].get('sortOrder'), order)
    assert.equal(page.requests[0].get('sortKey'), 'username')
    assert.equal(page.requests[0].get('pageNo'), '1')
    await page.handleUserSortChange({ key: 'username', order: null })
    assert.equal(page.requests[1].has('sortOrder'), false)
    assert.equal(page.requests[1].has('sortKey'), false)
    assert.equal(page.requests[1].get('listFilters'), '[]')
  })
}
