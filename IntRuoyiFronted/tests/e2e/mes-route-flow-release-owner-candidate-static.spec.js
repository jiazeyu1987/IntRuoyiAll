const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteReleaseOwners.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/route/index.ts'), 'utf8')

function createPanel(getReleaseOwner) {
  const script = component.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const js = ts.transpileModule(script, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
  const requireMock = name => {
    if (name === 'vue') return { ref: value => ({ value }), onMounted: () => {} }
    assert.equal(name, '@/api/mes/pro/route')
    return { ProRouteApi: { getReleaseOwner } }
  }
  return Function('require', 'exports', js + '\nreturn { owners, loading, loadOwners }')(requireMock, {})
}

test('panel uses the business endpoint and distinguishes loading failure', () => {
  assert.doesNotMatch(component, /api\/system|loadReleaseOwner\(/)
  assert.match(api, /url: '\/mes\/pro\/route\/release-owner'/)
  assert.match(component, /owner\.error \? '加载失败'/)
})

test('both formal roles display all members including disabled accounts', async () => {
  const codes = []
  const users = Array.from({ length: 101 }, (_, id) => ({ id, status: id === 100 ? 1 : 0 }))
  const panel = createPanel(async code => {
    codes.push(code)
    return { role: { id: 23, name: code }, users }
  })
  await panel.loadOwners()
  assert.deepEqual(codes, ['MES_PQC_RELEASE_OWNER', 'MES_MANAGEMENT_REPRESENTATIVE'])
  for (const owner of panel.owners.value) {
    assert.equal(owner.role.name, owner.code)
    assert.equal(owner.users.length, 101)
    assert.equal(owner.users[100].status, 1)
    assert.equal(owner.error, '')
  }
  assert.equal(panel.loading.value, false)
})

test('one failure preserves the other card and refresh clears stale errors', async () => {
  let failed = true
  const panel = createPanel(async code => {
    if (failed && code === 'MES_PQC_RELEASE_OWNER') throw new Error('默认权限角色已停用：PQC负责人')
    return { role: { id: 23, name: code }, users: [] }
  })
  await panel.loadOwners()
  assert.match(panel.owners.value[0].error, /已停用/)
  assert.equal(panel.owners.value[0].role, undefined)
  assert.ok(panel.owners.value[1].role)
  failed = false
  await panel.loadOwners()
  assert.equal(panel.owners.value[0].error, '')
  assert.ok(panel.owners.value[0].role)
  assert.deepEqual(panel.owners.value[0].users, [])
})

test('request failures remain explicit for both cards', async () => {
  const panel = createPanel(async () => { throw new Error('没有该操作权限') })
  await panel.loadOwners()
  assert.ok(panel.owners.value.every(owner => owner.error === '没有该操作权限'))
  assert.equal(panel.loading.value, false)
})
