const { test } = require('node:test')
const assert = require('node:assert/strict')
const { fixture } = require('./helpers/workbench-s3-fixture.cjs')

test('HTTP workbench reads retain identity and independent UUID epochs', () => {
  const h = fixture({ httpCrypto: true })
  try {
    const first = h.context.beginWorkbenchRead()
    const second = h.context.beginWorkbenchRead()
    assert.equal(first.identity, second.identity)
    assert.match(first.contextEpoch, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
    assert.notEqual(first.contextEpoch, second.contextEpoch)
    assert.equal(h.context.isWorkbenchReadCurrent(first), true)
    h.identity(2)
    assert.equal(h.context.isWorkbenchReadCurrent(first), false)
  } finally { h.scope.stop() }
})
