const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')

const root = path.resolve(__dirname, '../..')
function loadSource(file, imports = {}) {
  const exports = {}
  const compiled = ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  })
  vm.runInNewContext(compiled.outputText, {
    exports,
    require(id) {
      assert.ok(Object.hasOwn(imports, id), `Unexpected dependency: ${id}`)
      return imports[id]
    }
  }, { filename: file })
  return exports
}

function setup(routers, enabled = true, roles = [], permissions = new Set()) {
  const pinia = { defineStore: (_id, options) => options }
  const testMode = loadSource('src/store/modules/testMode.ts', {
    vue: require('vue'), pinia, '@/api/infra/config': {}
  })
  const signatureAccess = loadSource('src/utils/signatureAccess.ts')
  const store = loadSource('src/store/modules/permission.ts', {
    pinia,
    './testMode': { ...testMode, useTestModeStore: () => ({ enabled }) },
    '@/store': {}, 'lodash-es': {},
    '@/router/modules/remaining': { default: [] },
    '@/router/modules/showroom': { default: [] },
    '@/utils/routerHelper': {},
    '@/hooks/web/useCache': { useCache: () => ({ wsCache: {} }) },
    '@/utils/is': {}, '@/utils/signatureAccess': signatureAccess,
    '@/utils/operationLogAccess': loadSource('src/utils/operationLogAccess.ts'),
    './user': { useUserStore: () => ({ getRoles: roles, getPermissions: permissions }) }
  }).usePermissionStore
  const state = { routers, menuTabRouters: routers }
  return getter => store.getters[getter].call(state)
}

for (const getter of ['getRouters', 'getMenuTabRouters']) {
  test(`${getter}: role menus remain additive for PQC, production and administrators`, () => {
    const routes = [{ path: '/mes/edhr-batch-processing', children: [
      { path: '/mes/edhr-production-execution', children: [
        { path: '/mes/pro/feedback/edhr-batch-production-fill' },
        { path: '/mes/pro/feedback/edhr-batch-pqc-fill' },
        { path: '/mes/pro/process-pool/pqc-leader' },
        { path: '/mes/pro/feedback/edhr-batch-execution' }
      ] }
    ] }]
    const before = structuredClone(routes)
    for (const roles of [
      ['pqc_permission'], ['pqc_leader_permission'],
      ['pqc_permission', 'production_leader_permission'], ['pqc_permission', 'tenant_admin']
    ]) {
      const result = setup(routes, true, roles)(getter)
      assert.equal(result[0].children[0].children.length, 4,
        'Navigation must preserve the union of server-authorized role menus')
    }
    assert.deepEqual(routes, before)
  })

  test(`${getter}: all hidden descendants hide every ancestor without changing route registration`, () => {
    const routes = [{ path: '/parent', name: 'Parent', meta: { alwaysShow: true, hasParentRoute: true }, children: [
      { path: 'child', name: 'Child', children: [
        { path: 'detail', meta: { hidden: true, canTo: true } }
      ] }
    ] }]
    const before = structuredClone(routes)
    const result = setup(routes)(getter)
    assert.equal(result[0].meta.hidden, true)
    assert.equal(result[0].children[0].meta.hidden, true)
    assert.equal(result[0].children[0].children[0].meta.canTo, true)
    assert.deepEqual(routes, before)
  })

  test(`${getter}: one visible child keeps ancestors visible; hidden parents stay hidden`, () => {
    const result = setup([{ path: '/parent', children: [
      { path: 'hidden', meta: { hidden: true }, children: [{ path: 'leaf' }] },
      { path: 'visible', children: [{ path: 'leaf' }] }
    ] }])(getter)
    assert.equal(Boolean(result[0].meta?.hidden), false)
    assert.equal(result[0].children[0].meta.hidden, true)
    assert.equal(Boolean(result[0].children[1].meta?.hidden), false)
  })

  test(`${getter}: leaves with omitted or empty children remain visible`, () => {
    const result = setup([{ path: '/leaf' }, { path: '/empty-leaf', children: [] }])(getter)
    assert.equal(result.length, 2)
    assert.ok(result.every(route => !route.meta?.hidden))
  })

  test(`${getter}: module filtering hides a parent whose last child was removed`, () => {
    const result = setup([{ path: '/system', children: [{ path: 'dept' }] }])(getter)
    assert.equal(result[0].meta.hidden, true)
    assert.equal(result[0].children.length, 0)
  })

  test(`${getter}: test mode visibility is applied before hiding empty ancestors`, () => {
    const routes = [{ path: '/tests', children: [{ path: 'template-simulate' }] }]
    assert.equal(setup(routes, false)(getter)[0].meta.hidden, true)
    assert.equal(setup(routes, null)(getter)[0].meta.hidden, true)
    assert.equal(Boolean(setup(routes, true)(getter)[0].meta?.hidden), false)
  })

  test(`${getter}: role filtering hides ancestors with no authorized children`, () => {
    const routes = [{ path: '/parent', children: [{ path: '/signature-governance', children: [
      { path: 'authorizations' }
    ] }] }]
    assert.equal(setup(routes)(getter)[0].meta.hidden, true)
    const authorized = setup(routes, true, ['tenant_admin'], new Set(['dcc:controlled-file:signature:manage']))(getter)
    assert.equal(Boolean(authorized[0].meta?.hidden), false)
  })
}
