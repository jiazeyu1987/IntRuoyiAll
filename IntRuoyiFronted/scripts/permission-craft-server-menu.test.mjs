import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'
import vm from 'node:vm'
import ts from 'typescript'

const source = fs.readFileSync('src/store/modules/permission.ts', 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 }
}).outputText

const generateRoutes = async (menus) => {
  let receivedMenus
  const module = { exports: {} }
  const imports = {
    pinia: { defineStore: (_name, options) => options },
    'lodash-es': { cloneDeep: structuredClone },
    '@/router/modules/remaining': { default: [] },
    '@/router/modules/showroom': { default: [] },
    '@/hooks/web/useCache': {
      CACHE_KEY: { ROLE_ROUTERS: 'menus' },
      useCache: () => ({ wsCache: { get: () => menus } })
    },
    '@/utils/routerHelper': {
      generateRoute: (routes) => { receivedMenus = routes; return routes }
    },
    '@/utils/is': { isUrl: (value) => /^https?:/.test(value) },
    './testMode': {}, '@/store': {}, '@/utils/signatureAccess': {},
    '@/utils/operationLogAccess': {}, './user': {}
  }
  vm.runInNewContext(compiled, {
    module, exports: module.exports,
    require: (name) => {
      assert.ok(Object.hasOwn(imports, name), `unexpected import: ${name}`)
      return imports[name]
    }
  })
  const options = module.exports.usePermissionStore
  const state = options.state()
  await options.actions.generateRoutes.call(state)
  return JSON.parse(JSON.stringify(receivedMenus))
}

const craft = (path) => ({ path, name: '工艺库', component: 'mes/pro/craft/index' })

test('现有生产管理下的工艺库不依赖新版工序目录，菜单生成不阻断登录', async () => {
  const menus = [{ path: '/mes', children: [{ path: 'pro', children: [craft('craft')] }] }]
  assert.deepEqual(await generateRoutes(menus), menus)
})

test('已迁移的工艺库保持后端返回的目录、路径和顺序', async () => {
  const menus = [{ path: '/mes/edhr-batch-processing', children: [
    { path: '/mes/edhr-process-settings', children: [craft('/mes/pro/craft')] }
  ] }]
  assert.deepEqual(await generateRoutes(menus), menus)
})

test('未授权工艺库时不补建目录或页面', async () => {
  const menus = [{ path: '/approval-center', children: [{ path: 'done', name: '已办' }] }]
  assert.deepEqual(await generateRoutes(menus), menus)
})
