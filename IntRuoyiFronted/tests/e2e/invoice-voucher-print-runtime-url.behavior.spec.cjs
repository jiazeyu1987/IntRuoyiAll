const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname, '../../src/views/erp/finance/invoice-voucher-print/index.vue')
const source = fs.readFileSync(filename, 'utf8')
const descriptor = parse(source, { filename }).descriptor
function harness(status, started = status, ticket = 'short-lived') {
  const calls = []
  const ctx = { ref: value => ({ value }), URL, Error, defineOptions() {}, onMounted() {},
    getInvoiceVoucherPrintAssistantStatus: async () => { calls.push('status'); if (status instanceof Error) throw status; return status },
    startInvoiceVoucherPrintAssistant: async () => { calls.push('start'); return started },
    createInvoiceVoucherPrintTicket: async () => { calls.push('ticket'); return { ticket } } }
  vm.createContext(ctx)
  const script = descriptor.scriptSetup.content.replace(/import\s*\{[\s\S]*?\}\s*from\s*'@\/api\/login'/, '')
  vm.runInContext(ts.transpileModule(script + '\nglobalThis.page = { loadAssistantEntry, handleStartAssistant, assistantAccessUrl, errorMessage, assistantNeedsLaunch, loading }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, ctx)
  return { ...ctx.page, calls }
}
test('full page compiles with the runtime API contract', () => {
  const script = compileScript(descriptor, { id: 'assistant' })
  const result = compileTemplate({ source: descriptor.template.content, filename, id: 'assistant', compilerOptions: { bindingMetadata: script.bindings } })
  assert.deepEqual(result.errors, [])
  assert.doesNotMatch(source, /import\.meta\.env|172\.30\./)
})
for (const publicUrl of ['https://test.example/print/', 'https://prod.example/assistant']) {
  test(`same page uses runtime URL ${publicUrl} with a ticket`, async () => {
    const h = harness({ running: true, publicUrl })
    await h.loadAssistantEntry()
    const actual = new URL(h.assistantAccessUrl.value)
    assert.equal(actual.origin, new URL(publicUrl).origin)
    assert.equal(actual.pathname, new URL('auth/callback', publicUrl.endsWith('/') ? publicUrl : publicUrl + '/').pathname)
    assert.equal(actual.searchParams.get('ticket'), 'short-lived')
    assert.deepEqual(h.calls, ['status', 'ticket'])
  })
}
for (const publicUrl of ['', undefined, 'javascript:alert(1)', 'https://u:p@example.test/', 'https://example.test/?secret=x']) {
  test(`invalid runtime URL blocks tickets: ${publicUrl}`, async () => {
    const h = harness({ running: true, publicUrl })
    await h.loadAssistantEntry()
    assert.equal(h.assistantAccessUrl.value, '')
    assert.ok(h.errorMessage.value)
    assert.deepEqual(h.calls, ['status'])
  })
}
test('offline assistant offers explicit start, then uses returned runtime URL', async () => {
  const h = harness({ running: false, launchable: true, publicUrl: 'https://test.example/' }, { running: true, publicUrl: 'https://started.example/' })
  await h.loadAssistantEntry()
  assert.equal(h.assistantNeedsLaunch.value, true)
  assert.deepEqual(h.calls, ['status'])
  await h.handleStartAssistant()
  assert.deepEqual(h.calls, ['status', 'start', 'ticket'])
  assert.equal(new URL(h.assistantAccessUrl.value).origin, 'https://started.example')
})
test('permission/status failure and missing ticket never render assistant', async () => {
  const denied = harness(new Error('没有权限'))
  await denied.loadAssistantEntry()
  assert.deepEqual(denied.calls, ['status'])
  assert.equal(denied.assistantAccessUrl.value, '')
  const noTicket = harness({ running: true, publicUrl: 'https://test.example/' }, undefined, '')
  await noTicket.loadAssistantEntry()
  assert.equal(noTicket.assistantAccessUrl.value, '')
  assert.match(noTicket.errorMessage.value, /票据/)
})
