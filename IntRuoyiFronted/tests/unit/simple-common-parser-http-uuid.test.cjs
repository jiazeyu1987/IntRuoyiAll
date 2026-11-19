const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, computed } = require('vue')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/form-center/simple-common-parser/index.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('page.ts', script, ts.ScriptTarget.Latest, true)
const methods = ast.statements.filter(node => ts.isFunctionDeclaration(node)
  && ['addProcess', 'addItem'].includes(node.name?.text)).map(node => node.getText(ast)).join('\n')
const code = ts.transpileModule(methods + '\nexports.methods = { addProcess, addItem };', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto) {
  const draft = ref({ processes: [] }), processIndex = ref(0), itemIndex = ref(0)
  const context = { exports: {}, draft, processIndex, itemIndex,
    process: computed(() => draft.value.processes[processIndex.value]), ...loadUuid(crypto) }
  vm.runInNewContext(code, context)
  return { ...context.exports.methods, draft, processIndex, itemIndex }
}

test('common parser adds process/items with prefixed UUID v4 codes on HTTP', () => {
  const p = setup({ getRandomValues: bytes => webcrypto.getRandomValues(bytes) })
  p.addProcess()
  p.addItem()
  p.addProcess()
  const processes = p.draft.value.processes
  assert.equal(processes.length, 2)
  assert.deepEqual(processes.map(process => process.sort), [1, 2])
  assert.equal(p.processIndex.value, 1)
  assert.equal(p.itemIndex.value, 0)
  const codes = []
  for (const process of processes) {
    assert.match(process.processCode, /^P-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
    codes.push(process.processCode)
    process.items.forEach((item, index) => {
      assert.match(item.itemCode, /^I-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
      assert.equal(item.itemSort, index + 1)
      assert.equal(item.resultType, 'BOOLEAN')
      codes.push(item.itemCode)
    })
  }
  assert.equal(new Set(codes).size, codes.length)
})

test('common parser stops before adding data when secure randomness is unavailable', () => {
  const p = setup({})
  assert.throws(() => p.addProcess(), /不支持安全随机数/)
  assert.equal(p.draft.value.processes.length, 0)
  p.draft.value.processes.push({ processCode: 'existing', items: [] })
  assert.throws(() => p.addItem(), /不支持安全随机数/)
  assert.equal(p.draft.value.processes[0].items.length, 0)
})
