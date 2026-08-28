const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '..', '..')
const viewPath = path.join(frontendRoot, 'src/views/erp/finance/invoice-voucher-print/index.vue')
const envTypesPath = path.join(frontendRoot, 'types/env.d.ts')
const prodEnvPath = path.join(frontendRoot, '.env.prod')

assert.ok(fs.existsSync(viewPath), `missing invoice voucher print assistant view: ${viewPath}`)

const viewSource = fs.readFileSync(viewPath, 'utf8')
const envTypes = fs.readFileSync(envTypesPath, 'utf8')
const prodEnv = fs.readFileSync(prodEnvPath, 'utf8')

assert.match(viewSource, /defineOptions\(\{\s*name:\s*'ErpInvoiceVoucherPrint'\s*\}\)/)
assert.match(viewSource, /VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL/)
assert.match(viewSource, /<iframe[\s\S]*:src="assistantUrl"/)
assert.match(viewSource, /发票凭证打印助手地址未配置/)
assert.doesNotMatch(viewSource, /fenbeitong-voucher/)
assert.doesNotMatch(viewSource, /分贝通凭证/)

assert.match(envTypes, /readonly VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL: string/)
assert.match(prodEnv, /^VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL=$/m)
