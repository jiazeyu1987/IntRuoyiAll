const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { webcrypto } = require('node:crypto')
const { ref } = require('vue')
const loadUuid = require('../helpers/load-secure-uuid.cjs')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/RegulationItemsEditor.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('editor.ts', script, ts.ScriptTarget.Latest, true)
const addItem = ast.statements.find(node => ts.isFunctionDeclaration(node) && node.name?.text === 'addItem')
const code = ts.transpileModule(`${addItem.getText(ast)}\nexports.addItem = addItem`, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup(crypto = { getRandomValues: bytes => webcrypto.getRandomValues(bytes) }) {
  const runtime = {
    exports: {}, ...loadUuid(crypto),
    props: { disabled: false },
    draft: ref({ regulationCode: 'REG-001', processes: [] }),
    addProcessCode: ref(''), addProcessName: ref(' 工序甲 '), addItemName: ref(' 项目甲 '),
    addVisible: ref(true), pageNo: ref(1), pageSize: ref(20),
    ElMessage: { warning() { throw new Error('unexpected validation warning') } }
  }
  runtime.rows = { get value() { return runtime.draft.value.processes.flatMap(process => process.items) } }
  vm.runInNewContext(code, runtime)
  return { ...runtime, addItem: runtime.exports.addItem }
}

test('HTTP editor retains process prefix and 8-digit code while creating UUID v4 item IDs', () => {
  assert.match(script, /import \{ createSecureUuid \} from '@\/utils\/uuid'/)
  const editor = setup()
  editor.addItem()
  const process = editor.draft.value.processes[0]
  assert.match(process.processCode, /^REG-001-P-[0-9a-f]{8}$/)
  assert.equal(process.processName, '工序甲')
  assert.equal(process.sort, 1)
  assert.match(process.items[0].itemCode, /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
  assert.equal(process.items[0].itemName, '项目甲')
  assert.equal(process.items[0].itemSort, 1)
  assert.equal(editor.addVisible.value, false)
  assert.equal(editor.addItemName.value, '')

  editor.addProcessCode.value = process.processCode
  editor.addItemName.value = '项目乙'
  editor.addItem()
  assert.equal(editor.draft.value.processes.length, 1)
  assert.equal(process.items[1].itemSort, 2)
  assert.notEqual(process.items[0].itemCode, process.items[1].itemCode)
})

test('missing secure random source fails before inserting a process or item', () => {
  const editor = setup({})
  assert.throws(() => editor.addItem(), /不支持安全随机数/)
  assert.equal(editor.draft.value.processes.length, 0)
  assert.equal(editor.addVisible.value, true)
  assert.equal(editor.addItemName.value, ' 项目甲 ')
})
