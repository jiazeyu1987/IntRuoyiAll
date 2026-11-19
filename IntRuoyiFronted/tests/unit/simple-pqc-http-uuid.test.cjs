const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { ref, computed } = require('vue')
const { webcrypto } = require('node:crypto')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

function setup(crypto) {
  const filename = path.resolve(__dirname, '../../src/views/form-center/simple-pqc-parser/index.vue')
  const source = fs.readFileSync(filename, 'utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('pqc.ts', source, ts.ScriptTarget.Latest, true)
  const functions = ast.statements.filter(s => ts.isFunctionDeclaration(s) && ['addProcess', 'addItem'].includes(s.name.text))
  assert.equal(functions.length, 2)
  const draft = ref({ processes: [] }), processIndex = ref(0), itemIndex = ref(0)
  const context = { ...loadUuid(crypto), draft, processIndex, itemIndex,
    process: computed(() => draft.value.processes[processIndex.value]) }
  const code = ts.transpileModule(functions.map(s => s.getText(ast)).join('\n'), {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText
  vm.runInNewContext(code + '\nglobalThis.actions = { addProcess, addItem }', context)
  return { ...context.actions, draft, processIndex, itemIndex }
}

test('HTTP PQC editing generates process and item UUID v4 codes with existing prefixes and selection', () => {
  const state = setup({ getRandomValues: bytes => webcrypto.getRandomValues(bytes) })
  state.addProcess()
  state.addItem()
  state.addProcess()
  const processes = state.draft.value.processes
  assert.equal(processes.length, 2)
  const codes = []
  for (const [index, process] of processes.entries()) {
    assert.match(process.processCode, /^P-[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/)
    assert.equal(process.sort, index + 1)
    codes.push(process.processCode)
    for (const [itemIndex, item] of process.items.entries()) {
      assert.match(item.itemCode, /^I-[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/)
      assert.equal(item.itemSort, itemIndex + 1)
      codes.push(item.itemCode)
    }
  }
  assert.equal(new Set(codes).size, 5)
  assert.equal(state.processIndex.value, 1)
  assert.equal(state.itemIndex.value, 0)
})

test('missing random source stops creation without adding a process', () => {
  const state = setup({})
  assert.throws(() => state.addProcess(), /不支持安全随机数/)
  assert.equal(state.draft.value.processes.length, 0)
})
