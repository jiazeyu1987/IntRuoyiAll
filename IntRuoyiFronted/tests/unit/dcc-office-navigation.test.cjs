const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')

test('Word navigation selects cross-page matches, wraps hits and validates page boundaries without editing', () => {
  const messages = [], listeners = {}
  let current = 0
  const doc = {
    GetPageCount: () => 3,
    GetCurrentPage: () => current,
    GoToPage: page => { current = page; return true },
    Search: query => query === '关键词' ? [0, 1, 2].map(page => ({
      Select: () => { current = page }, GetStartPage: () => page
    })) : []
  }
  const top = { postMessage: message => messages.push(message) }
  const Asc = { scope: {}, plugin: { callCommand: (fn, close, calc, callback) => callback(fn()) } }
  const window = { top, Asc, addEventListener: (name, callback) => { listeners[name] = callback } }
  vm.runInNewContext(fs.readFileSync(path.resolve(__dirname, '../../public/dcc-office-navigation/navigation.js'), 'utf8'), {
    window, Asc, Api: { GetDocument: () => doc }, location: { origin: 'https://qms.test' }
  })
  const send = data => {
    listeners.message({ origin: 'https://qms.test', source: top, data: { type: 'dcc-office-command', id: 1, ...data } })
    return messages.at(-1).result
  }
  Asc.plugin.init()
  assert.equal(messages.at(-1).ready, true)
  assert.equal(send({ action: 'state' }).total, 3)
  let result = send({ action: 'search', query: '关键词', index: 1 })
  assert.equal(result.count, 3)
  assert.equal(result.current, 2)
  assert.equal(send({ action: 'search', query: '关键词', index: -1 }).current, 3)
  assert.equal(send({ action: 'search', query: '关键词', index: 3 }).current, 1)
  assert.equal(send({ action: 'search', query: '无匹配', index: 0 }).count, 0)
  assert.equal(send({ action: 'page', page: 2 }).current, 2)
  assert.ok(send({ action: 'page', page: 4 }).error)
  assert.ok(send({ action: 'page', page: 0 }).error)
  assert.ok(send({ action: 'delete' }).error)
  const before = messages.length
  listeners.message({ origin: 'https://other.test', source: top, data: { type: 'dcc-office-command', action: 'page', page: 3 } })
  listeners.message({ origin: 'https://qms.test', source: {}, data: { type: 'dcc-office-command', action: 'page', page: 3 } })
  assert.equal(messages.length, before)
  assert.equal(current, 1)
})
