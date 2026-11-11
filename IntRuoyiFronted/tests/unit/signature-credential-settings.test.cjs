const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate } = require('vue/compiler-sfc')

const filename = path.join(__dirname, '../../src/views/signature-governance/components/SignatureCredentialSettings.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename, 'utf8'), { filename })
assert.deepEqual(errors, [])
const template = compileTemplate({ source: descriptor.template.content, filename, id: 'signature-settings' })
assert.deepEqual(template.errors, [])
const ast = ts.createSourceFile('settings.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const body = ast.statements.filter(statement => !ts.isImportDeclaration(statement))
  .map(statement => statement.getText(ast)).join('\n')
const code = ts.transpileModule(body, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText

function settings(type = 'PIN', write = async () => {}) {
  const calls = [], scope = vue.effectScope()
  const bindings = {
    ref: vue.ref, watch: vue.watch, onMounted: () => {},
    useRouter: () => ({ push() {} }), useMessage: () => ({ success() {} }),
    getSignatureSettings: async () => ({ credentialType: type }),
    updateSignatureSettings: async data => { calls.push(data); await write(data) }
  }
  const state = scope.run(() => Function(...Object.keys(bindings), code +
    ';return { credentialType, newPin, confirmPin, password, error, saving, load, save };')(...Object.values(bindings)))
  return { ...state, calls, stop: () => scope.stop() }
}

test('PIN fields and password entry follow the selected signature method', () => {
  const form = descriptor.template.ast.children[0].children.find(node => node.tag === 'el-form')
  const row = label => form.children.find(node => node.props?.some(prop => prop.name === 'label' && prop.value?.content === label))
  const visible = (label, credentialType) => {
    const condition = row(label).props.find(prop => prop.type === 7 && prop.name === 'if')
    assert.ok(condition, `${label} requires method-specific visibility`)
    return Function('credentialType', `return ${condition.exp.content}`)(credentialType)
  }
  for (const type of ['PIN', 'PASSWORD']) {
    assert.equal(visible('新 PIN 码', type), type === 'PIN')
    assert.equal(visible('确认新 PIN 码', type), type === 'PIN')
    assert.equal(visible('密码签名', type), type === 'PASSWORD')
  }
})

test('switching method clears unfinished credentials and validation errors', async t => {
  const s = settings(); t.after(s.stop); await s.load()
  s.newPin.value = '123'; s.confirmPin.value = '456'; s.password.value = 'unit-only'
  await s.save(); assert.ok(s.error.value)
  s.credentialType.value = 'PASSWORD'
  for (const field of ['newPin', 'confirmPin', 'password', 'error']) assert.equal(s[field].value, '')
  s.password.value = 'unit-only'; await s.save()
  assert.deepEqual(s.calls, [{ credentialType: 'PASSWORD', password: 'unit-only' }])
})

test('password mode never validates or submits an inactive PIN', async t => {
  const s = settings('PASSWORD'); t.after(s.stop); await s.load()
  s.newPin.value = '123'; s.confirmPin.value = '999'; s.password.value = 'unit-only'
  await s.save()
  assert.equal(s.error.value, '')
  assert.deepEqual(s.calls, [{ credentialType: 'PASSWORD', password: 'unit-only' }])
})

test('PIN mode rejects invalid input and preserves PIN when left blank', async t => {
  const s = settings(); t.after(s.stop); await s.load(); s.password.value = 'unit-only'
  s.newPin.value = '123'; s.confirmPin.value = '123'; await s.save()
  assert.equal(s.calls.length, 0)
  s.newPin.value = '123456'; s.confirmPin.value = '654321'; await s.save()
  assert.equal(s.calls.length, 0)
  s.confirmPin.value = '123456'; await s.save()
  assert.deepEqual(s.calls[0], { credentialType: 'PIN', password: 'unit-only', newPin: '123456' })
  s.password.value = 'unit-only'; await s.save()
  assert.deepEqual(s.calls[1], { credentialType: 'PIN', password: 'unit-only' })
})

test('both modes require login password and show rejected saves as failures', async t => {
  for (const type of ['PIN', 'PASSWORD']) {
    const s = settings(type, async () => { throw new Error('登录密码不正确') }); t.after(s.stop)
    await s.load(); await s.save(); assert.equal(s.calls.length, 0)
    assert.equal(s.error.value, '请输入当前登录密码')
    s.password.value = 'unit-only'; await s.save()
    assert.equal(s.error.value, '登录密码不正确')
    assert.equal(s.saving.value, false); assert.equal(s.password.value, '')
  }
})
