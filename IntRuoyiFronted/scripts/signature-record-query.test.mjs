import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const ts = require('typescript')
const source = readFileSync(new URL('../src/views/signature-governance/components/signatureRecordQuery.ts', import.meta.url), 'utf8')
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
const module = { exports: {} }
new Function('module', 'exports', code)(module, module.exports)
const build = module.exports.buildSignatureRecordQuery

test('my signatures keeps all combined filters and leaves actor restriction to the authenticated endpoint', () => {
  const result = build({ pageNo: 2, pageSize: 20, recordScope: 'MY_SIGNATURES', source: 'BPM',
    signatureId: ' 62283 ', keyword: ' EDHR ', signerKeyword: ' 郑华 ', actionCode: ' APPROVE ', signerUserId: 999 })
  assert.equal(result.mine, true)
  assert.deepEqual(result.params, { pageNo: 2, pageSize: 20, sourceCodes: ['BPM'], signatureId: '62283',
    keyword: 'EDHR', signerKeyword: '郑华', actionCode: 'APPROVE' })
  assert.equal(result.params.signerUserId, undefined)
})

test('signature ID can find a modern signature without forcing the legacy batch-record source', () => {
  const { params, mine } = build({ pageNo: 1, pageSize: 10, signatureId: '62283' })
  assert.equal(params.signatureId, '62283')
  assert.equal(params.sourceCodes, undefined)
  assert.equal(mine, false)
})

test('calendar-day boundaries cover the day while exact timestamps remain unchanged', () => {
  const dates = ['2026-11-06', '2026-11-06']
  assert.deepEqual(build({ pageNo: 1, pageSize: 10, signedAt: dates }).params.signedAt,
    ['2026-11-06 00:00:00', '2026-11-06 23:59:59'])
  assert.deepEqual(dates, ['2026-11-06', '2026-11-06'])
  const exact = ['2026-11-06 12:30:00', '2026-11-06 13:30:00']
  assert.deepEqual(build({ pageNo: 1, pageSize: 10, signedAt: exact }).params.signedAt, exact)
  assert.equal(build({ pageNo: 1, pageSize: 10 }).params.signedAt, undefined)
})
