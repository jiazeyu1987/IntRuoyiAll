const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

function evaluate(path, dependencies) {
  const exports = {}
  const code = ts.transpileModule(fs.readFileSync(path, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code, { exports, require: id => {
    assert.ok(id in dependencies, `Unexpected dependency: ${id}`)
    return dependencies[id]
  } })
  return exports
}
const pagination = evaluate('src/hooks/web/useListColumnFilter.ts', { vue: require('vue') })
const api = get => evaluate('src/api/dcc/controlledFile/projectCodes.ts', {
  '@/config/axios': { default: { get } },
  '@/hooks/web/useListColumnFilter': pagination
})

test('project dropdown includes all 133 authorized projects and preserves filters', async () => {
  const rows = Array.from({ length: 133 }, (_, i) => ({ id: i + 1, projectCode: `PROJECT-${i + 1}` }))
  const calls = []
  const service = api(async ({ url, params }) => {
    assert.equal(url, '/dcc/project-codes/page')
    calls.push(params)
    const start = (params.pageNo - 1) * params.pageSize
    return { list: rows.slice(start, start + params.pageSize), total: rows.length }
  })
  const result = await service.getProjectCodeOptions({ status: 'ENABLE', keyword: 'PROJECT', routeConfigured: true })
  assert.deepEqual(Array.from(result, row => row.id), rows.map(row => row.id))
  assert.equal(calls.length, 2)
  for (const query of calls) {
    assert.equal(query.status, 'ENABLE')
    assert.equal(query.keyword, 'PROJECT')
    assert.equal(query.routeConfigured, true)
  }
})

test('dropdown returns only the authorized rows supplied by the backend', async () => {
  const service = api(async () => ({ list: [{ id: 17 }], total: 1 }))
  assert.deepEqual(Array.from(await service.getProjectCodeOptions(), row => row.id), [17])
})

test('a later page failure never returns a truncated successful dropdown', async () => {
  const failure = new Error('project query failed')
  const service = api(async ({ params }) => {
    if (params.pageNo === 2) throw failure
    return { list: Array.from({ length: 100 }, (_, i) => ({ id: i + 1 })), total: 133 }
  })
  await assert.rejects(service.getProjectCodeOptions(), error => error === failure)
})
