import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import vm from 'node:vm'
import ts from 'typescript'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/browser/index.vue', import.meta.url)), 'utf8')
const declaration = name => {
  const start = source.indexOf(`const ${name} =`)
  const remaining = source.slice(start)
  const close = remaining.match(/\r?\n\}/)
  assert.ok(start >= 0 && close, name)
  return remaining.slice(0, close.index + close[0].length)
}
const setup = fail => {
  const calls = []
  const c = {
    checkinUpload: { value: { uploadTicket: 'SOURCE-X', sessionId: 'S1', requestId: 'R1' } },
    checkinDrawingPdfUpload: { value: undefined }, checkinSourceState: { value: 'ready' },
    checkinUploadSessionId: { value: 'S1' }, checkinCleanupLoading: { value: false },
    checkinSubmitting: { value: false }, checkinUploadLoading: { value: false }, checkinDrawingPdfLoading: { value: false },
    cleanupControlledFileUploadTicket: async (...args) => {
      calls.push(args)
      if (fail) throw new Error('storage unavailable')
      return { cleanupStatus: 'CLEANED', bindable: false, cleanedCount: 1 }
    },
    clearCheckinDrawingPdf: () => {}, resolveBrowserErrorMessage: error => error.message,
    message: { error: () => {}, warning: () => {} }
  }
  vm.createContext(c)
  let code = `let checkinSourceRequestSequence=0; ${declaration('clearCheckinUpload')}; globalThis.remove=clearCheckinUpload;`
  if (source.includes('const cleanupCheckinTicket =')) code = declaration('cleanupCheckinTicket') + ';' + code
  vm.runInContext(ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
  return { c, calls }
}
test('removing an uploaded source retires its ticket before clearing the form', async () => {
  const { c, calls } = setup(false)
  const allowed = await c.remove()
  assert.equal(calls.length, 1)
  assert.deepEqual(Array.from(calls[0]).slice(0, 2), ['S1', 'SOURCE-X'])
  assert.equal(c.checkinUpload.value, undefined)
  assert.equal(allowed, true)
})
test('cleanup failure preserves the source ticket for a retry and prevents widget removal', async () => {
  const { c } = setup(true)
  const allowed = await c.remove()
  assert.equal(allowed, false)
  assert.equal(c.checkinUpload.value.uploadTicket, 'SOURCE-X')
})
