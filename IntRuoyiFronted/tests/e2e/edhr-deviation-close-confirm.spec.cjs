const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const test = require('node:test')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-deviation/DeviationDetail.vue'), 'utf8')
const method = source.match(/const closeHandling = (async \(\) => \{[\s\S]*?)\nconst load =/)[1]

function setup({ reject, changed, readonly = false, saved = true } = {}) {
  const calls = []
  const context = {
    props: { id: 42, readonly }, loadSequence: 7, closing: { value: false },
    canSignSavedHandling: () => saved,
    message: { confirm: async (text, title) => { calls.push(['confirm', text, title]); if (changed) context.props.id = 43; if (reject) throw reject } },
    ElMessageBox: { confirm: async () => { throw new Error('unlayered confirmation blocks the drawer') } },
    closeDeviationHandling: async id => calls.push(['close', id]),
    load: async () => calls.push(['reload']),
    ElMessage: { success: text => calls.push(['success', text]), error: text => calls.push(['error', text]) }
  }
  const compiled = ts.transpileModule(`(${method})`, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const close = vm.runInNewContext(compiled, context)
  return { close, calls, context }
}

test('drawer confirmation uses unified overlay before closing and reloads after success', async () => {
  const { close, calls, context } = setup()
  await close()
  assert.deepEqual(calls.map(x => x[0]), ['confirm', 'close', 'reload', 'success'])
  assert.equal(calls[1][1], 42)
  assert.equal(context.closing.value, false)
  assert.match(source, /const message = useMessage\(\)/)
})
for (const reject of ['cancel', 'close']) {
  test(`confirmation ${reject} performs no business close and does not throw`, async () => {
    const { close, calls } = setup({ reject })
    await close()
    assert.deepEqual(calls.map(x => x[0]), ['confirm'])
  })
}
test('changed detail identity while confirming cannot close the previous deviation', async () => {
  const { close, calls } = setup({ changed: true })
  await close()
  assert.deepEqual(calls.map(x => x[0]), ['confirm'])
})
test('readonly and unsaved records do not prompt or close', async () => {
  for (const options of [{ readonly: true }, { saved: false }]) {
    const { close, calls } = setup(options)
    await close()
    assert.equal(calls.length, 0)
  }
})
test('unexpected confirmation failure remains observable', async () => {
  const problem = new Error('confirmation initialization failed')
  const { close, calls } = setup({ reject: problem })
  await assert.rejects(close, error => error === problem)
  assert.deepEqual(calls.map(x => x[0]), ['confirm'])
})

test('actual shared confirm hook applies the registered overlay class', async () => {
  const hook = fs.readFileSync(path.join(root, 'src/hooks/web/useMessage.ts'), 'utf8')
    .replace(/^import .*\r?\n/gm, '').replace('export const useMessage', 'const useMessage')
  const calls = []
  const context = {
    useI18n: () => ({ t: key => key }),
    ElMessageBox: { confirm: async (...args) => calls.push(args) }
  }
  const compiled = ts.transpileModule(hook + '\nthis.message = useMessage()', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(compiled, context)
  await context.message.confirm('关闭内容', '关闭确认')
  assert.equal(calls[0][2].modalClass, 'app-confirm-message-box-overlay')
  const styles = fs.readFileSync(path.join(root, 'src/styles/index.scss'), 'utf8')
  assert.match(styles, /\.app-confirm-message-box-overlay\s*\{\s*z-index:\s*4000 !important;/)
})
