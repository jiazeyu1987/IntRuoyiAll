const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.join(__dirname, '../src/views/erp/finance/fenbeitong-voucher/index.vue'), 'utf8')
const body = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const code = ts.transpileModule(body.replaceAll('import.meta.env', 'testBuildEnv') + `
globalThis.view = { loadAssistantEntry, handleStartAssistant, assistantAccessUrl, errorMessage, assistantNeedsLaunch }
`, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText

// Component unit tests: controlled dependencies, no server or E2E acceptance claim.
function setup(status, started = status) {
  const calls = { status: 0, ticket: 0, start: 0 }
  const api = {
    getFenbeitongAssistantStatus: async () => {
      calls.status++
      if (status instanceof Error) throw status
      return status
    },
    createFenbeitongAssistantTicket: async () => { calls.ticket++; return { ticket: 'ticket +&中文' } },
    startFenbeitongAssistant: async () => { calls.start++; return started }
  }
  const context = {
    exports: {}, require: name => { assert.equal(name, '@/api/login'); return api },
    URL, Error, defineOptions: () => {}, onMounted: () => {},
    ref: value => ({ value }), computed: fn => ({ get value() { return fn() } }),
    testBuildEnv: { VITE_FENBEITONG_ASSISTANT_URL: '' }
  }
  vm.runInNewContext(code, context)
  return { ...context.view, calls }
}

test('same frontend opens each environment using the status URL and encoded ticket', async () => {
  for (const host of ['172.30.30.58', '172.30.30.57']) {
    const view = setup({ running: true, launchable: true, publicUrl: `http://${host}:18734` })
    await view.loadAssistantEntry()
    assert.equal(view.calls.status, 1)
    assert.equal(view.calls.ticket, 1)
    const opened = new URL(view.assistantAccessUrl.value)
    assert.equal(opened.origin, `http://${host}:18734`)
    assert.equal(opened.pathname, '/auth/callback')
    assert.equal(opened.searchParams.get('ticket'), 'ticket +&中文')
    assert.equal(view.errorMessage.value, '')
  }
})

test('missing, invalid and failed runtime configuration is visible and never requests a ticket', async () => {
  for (const publicUrl of ['', undefined, 'javascript:alert(1)', 'http://user:pass@host:18734/',
    'http://host:18734/?token=x', 'http://host:18734/#fragment', 'not a URL']) {
    const view = setup({ running: true, publicUrl })
    await view.loadAssistantEntry()
    assert.equal(view.calls.ticket, 0)
    assert.equal(view.assistantAccessUrl.value, '')
    assert.ok(view.errorMessage.value)
  }
  const view = setup(new Error('权限不足'))
  await view.loadAssistantEntry()
  assert.equal(view.errorMessage.value, '权限不足')
  assert.equal(view.calls.ticket, 0)
})

test('starting uses the URL in the start response and cannot open an invalid response', async () => {
  const status = { running: false, launchable: true, publicUrl: 'http://172.30.30.58:18734/' }
  const view = setup(status, { running: true, publicUrl: 'https://assistant.example.test/' })
  await view.loadAssistantEntry()
  assert.equal(view.assistantNeedsLaunch.value, true)
  assert.equal(view.calls.ticket, 0)
  await view.handleStartAssistant()
  assert.equal(new URL(view.assistantAccessUrl.value).origin, 'https://assistant.example.test')
  assert.equal(view.calls.start, 1)
  const invalid = setup(status, { running: true, publicUrl: '' })
  await invalid.loadAssistantEntry()
  await invalid.handleStartAssistant()
  assert.equal(invalid.calls.ticket, 0)
  assert.equal(invalid.assistantAccessUrl.value, '')
  assert.ok(invalid.errorMessage.value)
})
