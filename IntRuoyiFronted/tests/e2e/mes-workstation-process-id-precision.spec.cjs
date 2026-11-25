const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
const loadFunction = (file, name, bindings = {}) => {
  const source = fs.readFileSync(path.join(root, file), 'utf8')
  const start = source.indexOf(`const ${name} =`)
  assert.ok(start >= 0, `missing ${name}`)
  const ends = ['\nconst ', '\nexport '].map((marker) => source.indexOf(marker, start + 1)).filter((index) => index > start)
  const end = ends.length ? Math.min(...ends) : source.length
  const code = source.slice(start, end)
  const js = ts.transpileModule(code, {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  return new Function(...Object.keys(bindings), `${js}; return ${name}`)(...Object.values(bindings))
}

const pageFile = 'src/views/mes/md/workstation/index.vue'
const apiFile = 'src/api/mes/md/workstation/index.ts'
const routeFile = 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
const pageNormalize = loadFunction(pageFile, 'normalizePositiveProcessId')
const apiNormalize = loadFunction(apiFile, 'normalizePositiveIdParam')
const routeNormalize = loadFunction(routeFile, 'normalizePositiveProcessId')

for (const normalize of [pageNormalize, apiNormalize, routeNormalize]) {
  for (const id of ['2117828464390517003', '2117828464390517004', '9223372036854775807']) {
    assert.equal(normalize(id), id, 'Long IDs must retain every digit')
  }
  assert.equal(normalize(' 2117828464390517003 '), '2117828464390517003')
  assert.equal(normalize(42), 42)
  for (const value of [undefined, null, '', '0', 0, -1, '-1', 'abc', 'Infinity', Infinity, NaN, '1.5', 1.5, Number.MAX_SAFE_INTEGER + 1]) {
    assert.equal(normalize(value), undefined, `invalid process ID: ${value}`)
  }
}

const buildRouteQuery = loadFunction(routeFile, 'buildWorkstationProcessQuery', {
  normalizePositiveProcessId: routeNormalize
})
const buildApiParams = loadFunction(apiFile, 'buildWorkstationPageParams', {
  normalizePositiveIdParam: apiNormalize
})
const id = '2117828464390517003'
const code = 'WS-2117828464080138258-14'
const routeQuery = buildRouteQuery(id)
const requestParams = buildApiParams({ code, processId: pageNormalize(routeQuery.processId), pageNo: 1 })
assert.deepEqual(requestParams, { code, processId: id, pageNo: 1 })
assert.deepEqual(buildRouteQuery(0), {})
assert.equal(buildApiParams({ processId: '0' }).processId, undefined)
console.log('PASS: workstation route, page and API preserve Long process IDs and reject invalid IDs')

async function verifyListFailureAndRetry() {
  const source = fs.readFileSync(path.join(root, pageFile), 'utf8')
  const start = source.indexOf('async function getList()')
  const end = source.indexOf('\n/** 添加/修改操作 */', start)
  assert.ok(start >= 0 && end > start)
  const loading = { value: false }
  const listLoadError = { value: '' }
  const failedWorkstationId = { value: undefined }
  const list = { value: [{ id: 'previous-result' }] }
  const total = { value: 1 }
  const failure = new Error('工作站缺少排产员工作台班次小时配置，workstationId=2117828464394711234')
  failure.code = 1040106006
  let fail = true
  const MdWorkstationApi = {
    getWorkstationPage: async () => {
      if (fail) throw failure
      return { list: [{ id }], total: 1 }
    }
  }
  const getListCode = ts.transpileModule(source.slice(start, end), {
    compilerOptions: { target: ts.ScriptTarget.ES2020 }
  }).outputText
  const getList = new Function('loading', 'listLoadError', 'list', 'total', 'MdWorkstationApi',
    'buildWorkstationPageParams', 'failedWorkstationId', 'route', `let workstationListSequence = 0; ${getListCode}; return getList`)(
      loading, listLoadError, list, total, MdWorkstationApi, () => ({}), failedWorkstationId, { fullPath: '/mes/md/workstation' }
    )
  await assert.rejects(getList(), (error) => error === failure)
  assert.equal(listLoadError.value, failure.message)
  assert.equal(failedWorkstationId.value, '2117828464394711234')
  assert.deepEqual(list.value, [])
  assert.equal(total.value, 0)
  assert.equal(loading.value, false)
  fail = false
  await getList()
  assert.equal(listLoadError.value, '')
  assert.equal(failedWorkstationId.value, undefined)
  assert.deepEqual(list.value, [{ id }])
  assert.equal(total.value, 1)
  assert.equal(loading.value, false)
  assert.ok(source.includes(':empty-text="listLoadError ?'))
  assert.ok(source.includes('v-if="listLoadError" :title="listLoadError"'))
  console.log('PASS: list errors remain visible, propagate, clear stale rows and recover on successful retry')
}
verifyListFailureAndRetry().catch((error) => { console.error(error); process.exitCode = 1 })
