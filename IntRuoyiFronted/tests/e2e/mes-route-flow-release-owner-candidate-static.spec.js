const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/RouteReleaseOwners.vue'), 'utf8')
const helper = fs.readFileSync(path.join(root, 'src/views/mes/pro/route/routeReleaseOwners.ts'), 'utf8')
const js = ts.transpileModule(helper, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const exportsObject = {}
Function('exports', js)(exportsObject)
const { loadReleaseOwner } = exportsObject
const role = { id: 23, code: 'MES_PQC_RELEASE_OWNER', name: 'PQC负责人', status: 0 }

test('two cards match the authoritative production and market release role codes', () => {
  assert.match(component, /label: '生产放行', code: 'MES_PQC_RELEASE_OWNER'/)
  assert.match(component, /label: '上市放行', code: 'MES_MANAGEMENT_REPRESENTATIVE'/)
  assert.doesNotMatch(component, /saveEdhrRouteReleaseApprovalRule|具体人员/)
  for (const [file, code] of [
    ['processpool/team/MesTeamLeaderActiveOrderReleaseGenerationService.java', 'PQC_RELEASE_OWNER'],
    ['productionrelease/manager/MesProductionReleaseManagerStageInitializerImpl.java', 'MANAGEMENT_REPRESENTATIVE']
  ]) {
    const source = fs.readFileSync(path.join(root, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro', file), 'utf8')
    assert.ok(source.includes(`MesProductionReleaseRoleCodes.${code}`))
  }
})

test('uses exact role code and loads all member pages for the selected role', async () => {
  const users = Array.from({ length: 101 }, (_, id) => ({ id }))
  const result = await loadReleaseOwner(role.code,
    async () => ({ list: [{ ...role, id: 99, code: role.code + '_OTHER' }, role], total: 2 }),
    async query => {
      assert.equal(query.roleId, role.id)
      return { list: users.slice((query.pageNo - 1) * 100, query.pageNo * 100), total: 101 }
    })
  assert.equal(result.role.id, role.id)
  assert.equal(result.users.length, 101)
})

test('missing, duplicate and disabled roles fail before members are read', async () => {
  for (const list of [[], [role, role], [{ ...role, status: 1 }]]) {
    await assert.rejects(loadReleaseOwner(role.code, async () => ({ list, total: list.length }),
      async () => assert.fail('invalid role must not load members')))
  }
})

test('failed requests propagate and empty roles remain unassigned', async () => {
  await assert.rejects(loadReleaseOwner(role.code, async () => { throw new Error('无权限') }, async () => {}), /无权限/)
  await assert.rejects(loadReleaseOwner(role.code, async () => ({ list: [role], total: 1 }), async () => { throw new Error('用户加载失败') }), /用户加载失败/)
  const result = await loadReleaseOwner(role.code, async () => ({ list: [role], total: 1 }), async () => ({ list: [], total: 0 }))
  assert.deepEqual(result.users, [])
})
