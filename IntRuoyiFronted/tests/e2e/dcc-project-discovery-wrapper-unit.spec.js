const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const root = path.resolve(__dirname, '../..')
const load = (relative, requests) => {
  const source = fs.readFileSync(path.join(root, relative), 'utf8')
  const api = {}
  vm.runInNewContext(ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020
  } }).outputText, { exports: api, Error, require: name => {
    if (name === '@/config/axios') return { default: { get: requests } }
    if (name === './fileTypeTaxonomies' || name === './workflow') return {}
    throw new Error(`Unexpected dependency: ${name}`)
  } })
  return api
}
const file = 'src/api/dcc/controlledFile/projectDiscovery.ts'
const id = '9223372036854775700'
const leader = '9223372036854775701'
const project = extra => ({ id, projectName: 'empty-project', projectCode: 'EMPTY', status: 'ENABLE',
  projectLeaderUserId: leader, projectLeader: 'same-display-name', associatedFileCount: 0, ...extra })

test('Public project page preserves exact project/leader IDs and server total without client filtering', async () => {
  const calls = []
  const api = load(file, async args => { calls.push(args); return { list: [project()], total: 37 } })
  const page = await api.getProjectDiscoveryPage({ pageNo: 2, pageSize: 1, keyword: 'empty' })
  assert.equal(page.total, 37)
  assert.equal(page.list[0].id, id)
  assert.equal(page.list[0].projectLeaderUserId, leader)
  assert.equal(calls[0].params.pageNo, 2)
})
test('Public project detail rejects unsafe numeric IDs before issuing a request', async () => {
  const calls = []
  const api = load(file, async args => { calls.push(args); return project() })
  await assert.rejects(() => api.getProjectDiscovery(Number(id)))
  assert.equal(calls.length, 0)
})
test('Public project response rejects precision-lost project or formal leader IDs', async () => {
  for (const field of ['id', 'projectLeaderUserId']) {
    const api = load(file, async () => ({ list: [project({ [field]: Number(id) })], total: 1 }))
    await assert.rejects(() => api.getProjectDiscoveryPage({ pageNo: 1, pageSize: 1 }))
  }
})
test('Public project detail cannot accept a different project identity', async () => {
  const api = load(file, async () => project({ id: '2' }))
  await assert.rejects(() => api.getProjectDiscovery(id))
})
test('Public project permission errors are preserved and empty page remains an actual empty result', async () => {
  const error = new Error('无权读取该项目')
  const api = load(file, async () => { throw error })
  await assert.rejects(() => api.getProjectDiscoveryPage({ pageNo: 1, pageSize: 1 }), e => e === error)
  const empty = await load(file, async () => ({ list: [], total: 0 })).getProjectDiscoveryPage({ pageNo: 1, pageSize: 1 })
  assert.equal(empty.total, 0)
  assert.equal(empty.list.length, 0)
})
test('Two identically displayed leaders retain different formal identities and missing identity is never inferred', async () => {
  const api = load(file, async () => ({ list: [project(), project({ id: '9223372036854775702', projectLeaderUserId: '9223372036854775703' }),
    project({ id: '9223372036854775704', projectLeaderUserId: null })], total: 3 }))
  const page = await api.getProjectDiscoveryPage({ pageNo: 1, pageSize: 3 })
  assert.equal(page.list[0].projectLeader, page.list[1].projectLeader)
  assert.notEqual(page.list[0].projectLeaderUserId, page.list[1].projectLeaderUserId)
  assert.equal(page.list[2].projectLeaderUserId, null)
})
test('Malformed server total is rejected instead of inventing client pagination', async () => {
  const api = load(file, async () => ({ list: [project()], total: -1 }))
  await assert.rejects(() => api.getProjectDiscoveryPage({ pageNo: 1, pageSize: 1 }))
})
