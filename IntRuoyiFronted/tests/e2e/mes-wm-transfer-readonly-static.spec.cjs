const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/wm/transfer/index.vue')
const form = readUtf8('src/views/mes/wm/transfer/TransferForm.vue')

assert.match(page, /openForm\('detail',\s*scope\.row\.id\)/, 'transfer list must keep a read-only detail entry.')
assert.doesNotMatch(page, /openForm\('(create|update|confirm|stock|finish)'/, 'transfer list must not expose manual write form entries.')
assert.doesNotMatch(page, /handle(Delete|Cancel)\(/, 'transfer list must not expose manual delete or cancel handlers.')
assert.doesNotMatch(page, /mes:wm-transfer:(create|update|delete|finish)/, 'transfer list must not render manual transfer write permissions.')
assert.doesNotMatch(page, />\s*(新增|编辑|删除|到货确认|执行上架|执行转移|取消)\s*</, 'transfer list must not render manual write action labels.')

assert.match(form, /const formType = ref<string>\('detail'\)/, 'transfer dialog must default to read-only detail mode.')
assert.match(form, /formType\.value = 'detail'/, 'transfer dialog must coerce every open request to detail mode.')
assert.match(form, /const isDetail = computed\(\(\) => true\)/, 'transfer dialog form must stay disabled as detail.')
assert.doesNotMatch(form, /WmTransferApi\.(createTransfer|updateTransfer|submitTransfer|confirmTransfer|stockTransfer|finishTransfer|cancelTransfer)/, 'transfer dialog must not call manual transfer write APIs.')
assert.doesNotMatch(form, /AutoCodeRecordApi|MesAutoCodeRuleCode|generateCode/, 'transfer dialog must not generate local transfer codes.')
assert.doesNotMatch(form, />\s*(保 存|提 交|到货确认|执行上架|执行转移)\s*</, 'transfer dialog footer must not render manual write buttons.')

console.log('PASS: MES transfer page is read-only for manual write operations')
