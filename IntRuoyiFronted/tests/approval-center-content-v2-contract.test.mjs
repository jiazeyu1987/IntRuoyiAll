import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const page = readFileSync(new URL('../src/views/approval-center/index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/approval-center/index.ts', import.meta.url), 'utf8')
test('formal eDHR dialog requests immutable preview and binds all command facts', () => {
  assert.match(api, /url: '\/approval-center\/tasks\/review-preview'/)
  for (const field of ['prepareId', 'idempotencyKey', 'expectedSubjectVersion', 'expectedContentHash', 'contentPolicyVersion']) assert.match(page, new RegExp(`${field}:`))
  assert.match(page, /canonicalBusinessContentJson/)
  assert.match(page, /allowedActions\.includes\(reviewForm\.result\)/)
  assert.match(page, /crypto\.randomUUID\(\)/)
})
test('formal success requires a receipt matching prepare and stable request', () => {
  assert.match(page, /result\.idempotencyKey !== payload\.idempotencyKey/)
  assert.match(page, /result\.prepareId !== payload\.prepareId/)
  assert.match(page, /!result\.receiptId \|\| !result\.signatureId/)
  assert.match(page, /reviewForm\.signaturePassword = ''/)
})
