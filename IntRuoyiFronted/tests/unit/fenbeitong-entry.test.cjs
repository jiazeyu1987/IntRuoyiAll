const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const load = (file) => {
  const context = { exports: {}, URL }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root, file), 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText, context)
  return context.exports
}
const { resolveFenbeitongAssistantUrl: resolve } = load('src/utils/fenbeitongAssistant.ts')
const { retainFinanceAssistantMenus: retain } = load('src/utils/financeAssistantMenus.ts')
const voucherPath = '/erp/finance/fenbeitong-voucher'

test('one frontend package connects to the assistant on each of the three hosts', () => {
  for (const host of ['172.30.30.58', '172.30.30.57', '172.30.30.59']) {
    assert.equal(resolve('http://{hostname}:18734/', host), `http://${host}:18734/`)
  }
  assert.equal(resolve('https://assistant.example/', '172.30.30.57'), 'https://assistant.example/')
})

test('missing or invalid configuration never silently selects an assistant', () => {
  assert.equal(resolve('  ', '172.30.30.57'), '')
  assert.throws(() => resolve('not-an-address', '172.30.30.57'))
  assert.throws(() => resolve('javascript:alert(1)', '172.30.30.57'))
})

test('authorized voucher navigation retains its original URL and hides other ERP pages', () => {
  for (const absolute of [false, true]) {
    const menus = [
      { path: 'index', meta: { title: 'ERP 首页' } },
      { path: absolute ? '/erp/finance' : 'finance', children: [
        { path: 'payable', meta: { title: '应付账款' } },
        { path: absolute ? voucherPath : 'fenbeitong-voucher', meta: { title: '分贝通凭证' } }
      ] }
    ]
    const before = JSON.stringify(menus)
    const result = retain(menus)
    assert.equal(result.length, 1)
    assert.equal(result[0].children.length, 1)
    assert.equal(result[0].children[0].meta.title, '分贝通凭证')
    assert.equal(result[0].redirect, voucherPath)
    assert.equal(JSON.stringify(menus), before, 'must not mutate authorized router definitions')
  }
})

test('no voucher authorization produces no ERP navigation', () => {
  assert.equal(retain([]).length, 0)
  assert.equal(retain([{ path: 'finance', children: [{ path: 'payable' }] }]).length, 0)
})

test('actual navigation filter keeps only authorized finance assistants under ERP', () => {
  const source = fs.readFileSync(path.join(root, 'src/store/modules/permission.ts'), 'utf8')
  const start = source.indexOf('const filterHiddenModuleMenus =')
  const end = source.indexOf('const SIGNATURE_GOVERNANCE_ROUTE_PATH', start)
  const context = { retainFinanceAssistantMenus: retain, HIDDEN_MODULE_MENU_PATHS: new Set(['/erp', '/mes', '/infra']) }
  vm.runInNewContext(ts.transpileModule(source.slice(start, end) + '\nglobalThis.filter = filterHiddenModuleMenus', {
    compilerOptions: { module: ts.ModuleKind.CommonJS }
  }).outputText, context)
  const result = context.filter([
    { path: '/user' }, { path: '/mes' }, { path: '/infra' },
    { path: '/erp', children: [{ path: 'finance', children: [{ path: 'fenbeitong-voucher' }, { path: 'payable' }] }] }
  ])
  assert.equal(result.length, 2)
  assert.equal(result[1].path, '/erp')
  assert.equal(result[1].children[0].children.length, 1)
  assert.equal(context.filter([{ path: '/erp', children: [{ path: 'index' }] }]).length, 0)
  const invoicePath = '/erp/finance/invoice-voucher-print'
  for (const absolute of [false, true]) {
    const result = context.filter([{ path: '/erp', children: [{
      path: absolute ? '/erp/finance' : 'finance',
      children: [{ path: absolute ? invoicePath : 'invoice-voucher-print' }]
    }] }])
    assert.equal(result.length, 1, 'invoice-only accounts must retain the ERP menu')
    assert.equal(result[0].redirect, invoicePath, 'ERP must not redirect to an unauthorized voucher page')
    assert.equal(result[0].children[0].redirect, invoicePath)
  }
})

test('both authorized assistants remain visible without granting other ERP navigation', () => {
  const menus = [{ path: 'finance', children: [
    { path: 'invoice-voucher-print', meta: { title: '发票凭证打印' } },
    { path: 'fenbeitong-voucher', meta: { title: '分贝通凭证' } },
    { path: 'payable' }
  ] }]
  const before = JSON.stringify(menus)
  const result = retain(menus)
  assert.deepEqual(Array.from(result[0].children, m => m.path), ['invoice-voucher-print', 'fenbeitong-voucher'])
  assert.equal(result[0].redirect, '/erp/finance/invoice-voucher-print')
  assert.equal(JSON.stringify(menus), before)
  assert.equal(retain([{ path: 'finance', children: [{ path: 'fenbeitong-voucher' }] }])[0].children.length, 1)
})
