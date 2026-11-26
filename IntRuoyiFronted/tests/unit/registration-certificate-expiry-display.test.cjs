const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const state = {}
new Function('exports', ts.transpileModule(fs.readFileSync('src/views/dcc/registration-certificate/shared/state.ts', 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText)(state)

test('expiry uses Beijing calendar days and includes the due date', () => {
  const beforeMidnight = new Date('2026-11-01T15:59:59Z')
  const afterMidnight = new Date('2026-11-01T16:00:00Z')
  assert.equal(state.isRegistrationCertificateExpired('2026-11-01', beforeMidnight), false)
  assert.equal(state.isRegistrationCertificateExpired('2026-11-01', afterMidnight), true)
  assert.equal(state.isRegistrationCertificateExpired([2026, 11, 1], afterMidnight), true)
  for (const date of [null, undefined, '']) assert.equal(state.isRegistrationCertificateExpired(date, afterMidnight), false)
})

test('expired current certificates cannot display green valid or normal tags', () => {
  const past = '2000-11-01'
  assert.equal(state.formatRegistrationCertificateStatus('CURRENT', undefined, past), '已过期')
  assert.equal(state.getRegistrationCertificateStatusTagType('CURRENT', undefined, past), 'danger')
  assert.equal(state.formatRegistrationCertificateReminder('NONE', past), '已过期')
  assert.equal(state.getRegistrationCertificateReminderTagType('NORMAL', past), 'danger')
  for (const status of ['OLD', 'VOIDED', 'PENDING_EFFECTIVE', 'DRAFT']) {
    assert.equal(state.formatRegistrationCertificateStatus(status, undefined, past), state.formatRegistrationCertificateStatus(status))
  }
})

test('absent and future expiry preserve existing FDA and other certificate displays', () => {
  for (const date of [null, undefined, '', '2999-01-01']) {
    assert.equal(state.formatRegistrationCertificateStatus('CURRENT', undefined, date), '当前版本')
    assert.equal(state.getRegistrationCertificateStatusTagType('CURRENT', undefined, date), 'success')
    assert.equal(state.formatRegistrationCertificateReminder('NONE', date), '正常')
    assert.equal(state.getRegistrationCertificateReminderTagType('NORMAL', date), 'success')
  }
})
