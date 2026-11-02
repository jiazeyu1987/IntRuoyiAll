const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const context = { exports: {}, URL }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,
  'src/utils/invoiceVoucherPrintAssistant.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, context)
const resolve = context.exports.resolveInvoiceVoucherPrintAssistantUrl

test('production configuration connects each frontend to its own invoice assistant', () => {
  const config = fs.readFileSync(path.join(root, '.env.prod'), 'utf8')
  const template = config.match(/^VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL=(.*)$/m)[1].trim()
  for (const host of ['172.30.30.58', '172.30.30.57', '172.30.30.59']) {
    assert.equal(resolve(template, host), `http://${host}:18733/`)
  }
  const view = fs.readFileSync(path.join(root,
    'src/views/erp/finance/invoice-voucher-print/index.vue'), 'utf8')
  assert.match(view, /resolveInvoiceVoucherPrintAssistantUrl\(/)
  assert.match(view, /window.location.hostname/)
})

test('invalid configuration fails without silently selecting an environment', () => {
  assert.equal(resolve(' ', '172.30.30.57'), '')
  assert.throws(() => resolve('invalid-address', '172.30.30.57'))
  assert.throws(() => resolve('javascript:alert(1)', '172.30.30.57'))
  assert.equal(resolve('https://invoice.example/', '172.30.30.57'), 'https://invoice.example/')
})
