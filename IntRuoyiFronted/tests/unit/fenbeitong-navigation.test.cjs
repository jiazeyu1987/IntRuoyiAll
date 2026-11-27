const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/store/modules/permission.ts'), 'utf8')
const code = source.slice(0, source.indexOf('export const usePermissionStore'))
  .replace(/^import .*from .*$/gm, '')
  .replace('const { wsCache } = useCache()', '')
const js = ts.transpileModule(code, {
  compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS }
}).outputText
const filter = new Function('cloneDeep', 'showroomRoutes', 'exports', js + '\nreturn filterHiddenModuleMenus')(
  structuredClone, [], {}
)

test('已授权的分贝通凭证保留完整导航，其他 ERP 入口继续隐藏，原始路由不变', () => {
  for (const absolute of [false, true]) {
    const leafPath = absolute ? '/erp/finance/fenbeitong-voucher' : 'fenbeitong-voucher'
    const financePath = absolute ? '/erp/finance' : 'finance'
    const routes = [{ path: '/erp', children: [
      { path: financePath, children: [{ path: leafPath }, { path: 'invoice-voucher-print' }] },
      { path: 'purchase' }
    ] }]
    const before = structuredClone(routes)
    const result = filter(routes, ['fenbeitong_voucher_view'])
    assert.deepEqual(result, [{ path: '/erp', children: [
      { path: financePath, children: [{ path: leafPath }] }
    ] }])
    assert.deepEqual(routes, before)
  }
})

test('未授权页面、仅按钮授权或无可见子菜单时不展示 ERP 空目录', () => {
  for (const children of [[], [{ path: 'invoice-voucher-print' }],
    [{ path: 'fenbeitong-voucher', meta: { hidden: true } }]]) {
    assert.deepEqual(filter([{ path: '/erp', children: [{ path: 'finance', children }] }],
      ['fenbeitong_voucher_view']), [])
  }
  assert.deepEqual(filter([], ['fenbeitong_voucher_view']), [])
})

test('以服务端菜单授权为准，不限制拥有同一菜单的其他角色', () => {
  const routes = [{ path: '/erp', children: [{ path: 'finance', children: [
    { path: 'fenbeitong-voucher' }
  ] }] }]
  assert.deepEqual(filter(routes, ['tenant_admin']), routes)
})
