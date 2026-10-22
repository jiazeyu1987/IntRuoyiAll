const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/system/menu/MenuForm.vue'), 'utf8')
const start = source.indexOf('const submitForm = async () => {')
const end = source.indexOf('/** 获取下拉框', start)
assert.ok(start >= 0 && end > start, 'the real submit handler must exist')
const body = source.slice(start, end).trim().replace(/^const submitForm = async \(\) => \{/, '').replace(/\}\s*$/, '').replace(' as unknown as MenuApi.MenuVO', '')
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor

async function submit({ parentId, routePath, type = 2, mode = 'update', valid = true, fail = false }) {
  const events = []
  const formLoading = { value: false }
  const dialogVisible = { value: true }
  const handler = new AsyncFunction('formRef', 'formLoading', 'formData', 'SystemMenuTypeEnum', 'isExternal', 'message', 'formType', 'MenuApi', 't', 'dialogVisible', 'emit', 'wsCache', 'CACHE_KEY', body)
  const action = async (data) => { events.push(['save', data]); if (fail) throw new Error('save rejected') }
  let error
  try {
    await handler({ value: { validate: async () => valid } }, formLoading, { value: { parentId, path: routePath, type } }, { DIR: 1, MENU: 2 }, p => /^(https?:|mailto:|tel:)/.test(p), { error: m => events.push(['error', m]), success: m => events.push(['success', m]) }, { value: mode }, { createMenu: action, updateMenu: action }, key => key, dialogVisible, event => events.push(['emit', event]), { delete: key => events.push(['clear', key]) }, { ROLE_ROUTERS: 'routes' })
  } catch (e) { error = e }
  return { events, formLoading, dialogVisible, error }
}

for (const [name, parentId, routePath] of [
  ['absolute child QA path', 900220, '/mes/pro/process-pool/qa-regulation'],
  ['relative child path', 900220, 'feedback/edhr-work-task'],
  ['absolute root path', 0, '/mes'],
  ['external child URL', 900220, 'https://example.org']
]) {
  test(`allows ${name} without changing its path`, async () => {
    const r = await submit({ parentId, routePath })
    assert.equal(r.error, undefined)
    assert.equal(r.events.filter(e => e[0] === 'save').length, 1)
    assert.equal(r.events.find(e => e[0] === 'save')[1].path, routePath)
    assert.equal(r.dialogVisible.value, false)
    assert.equal(r.formLoading.value, false)
  })
}

test('rejects a relative root route without a write', async () => {
  const r = await submit({ parentId: 0, routePath: 'mes' })
  assert.equal(r.events.filter(e => e[0] === 'save').length, 0)
  assert.ok(r.events.some(e => e[0] === 'error' && e[1] === '路径必须以 / 开头'))
  assert.equal(r.dialogVisible.value, true)
  assert.equal(r.formLoading.value, false)
})

test('invalid fields do not write', async () => {
  const r = await submit({ parentId: 900220, routePath: '/mes', valid: false })
  assert.equal(r.events.length, 0)
  assert.equal(r.dialogVisible.value, true)
})

test('API rejection propagates without closing or emitting success', async () => {
  const r = await submit({ parentId: 900220, routePath: '/mes', fail: true })
  assert.equal(r.error?.message, 'save rejected')
  assert.equal(r.dialogVisible.value, true)
  assert.equal(r.formLoading.value, false)
  assert.equal(r.events.filter(e => e[0] === 'success' || e[0] === 'emit').length, 0)
})
