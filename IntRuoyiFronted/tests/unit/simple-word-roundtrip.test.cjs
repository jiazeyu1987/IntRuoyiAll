const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const PizZip = require('pizzip')
const root = path.resolve(__dirname, '../..')
const modules = {}
function load(name) {
  if (modules[name]) return modules[name]
  const source = fs.readFileSync(path.join(root, `src/views/form-center/parser/${name}.ts`), 'utf8')
  const code = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true
    }
  }).outputText
  const context = {
    exports: {},
    crypto: require('node:crypto').webcrypto,
    TextEncoder,
    TextDecoder,
    atob,
    btoa,
    require: (id) => (id === './simpleRecognition' ? load('simpleRecognition') : require(id))
  }
  vm.runInNewContext(code, context)
  return (modules[name] = context.exports)
}
const { exportSimpleWord, readSimpleWord } = load('simpleWord')
const template = fs.readFileSync(path.join(root, 'src/assets/templates/simple-parser-export.docx'))
const plain = (value) => JSON.parse(JSON.stringify(value))
function content(schemaVersion = 3) {
  const processes = [
    {
      id: 'p1',
      sourceName: '原工序',
      processId: 12,
      processCode: 'P012',
      craftSelections: { 0: 28 },
      value: {
        name: '粗洗 <测试> & 特殊字符 😀',
        criticalProcess: true,
        inputs: [{ name: '物料一', code: null, sourceCodeLabel: '源编码' }],
        outputs: [{ name: '零件', code: '00001' }],
        equipmentGroups: [
          {
            selectionMode: 'MULTIPLE',
            equipmentOptions: [{ name: '设备原名称', code: 'B01' }],
            parameters: [
              {
                name: '温度',
                referenceValue: '0～1℃',
                actualValue: '',
                ui: { control: 'number', defaultValue: 0, min: 0, max: 1, step: 0.1, unit: '℃' }
              },
              {
                name: '方式',
                ui: { control: 'select', options: ['甲', '乙'], defaultValue: '乙' }
              },
              { name: '说明', ui: { control: 'text', defaultValue: '第一行\n第二行' } }
            ]
          }
        ],
        extraProcessMetadata: { retained: true }
      }
    },
    {
      id: 'p2',
      sourceName: '',
      value: { name: '', criticalProcess: false, inputs: [], outputs: [], equipmentGroups: [] }
    }
  ]
  return {
    filename: '原文.docx',
    routeName: '自定义工艺流程',
    processes,
    recognition: {
      schemaVersion,
      product: { name: '产品一', code: '0001' },
      extra: { retained: true },
      processes: processes.map((p) => p.value)
    }
  }
}
test('DOCX roundtrip preserves every editor field, bindings, order, extra metadata and zero values', async () => {
  for (const version of [2, 3]) {
    const original = content(version)
    const before = JSON.stringify(original)
    const blob = await exportSimpleWord(template, original)
    assert.equal(
      blob.type,
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    )
    const bytes = await blob.arrayBuffer()
    assert.deepEqual(plain(await readSimpleWord(bytes)), original)
    assert.equal(JSON.stringify(original), before)
    const zip = new PizZip(bytes)
    const xml = zip.file('word/document.xml').asText()
    assert.match(xml, /粗洗 &lt;测试&gt; &amp; 特殊字符 😀/)
    assert.match(xml, /默认值：0/)
    assert.match(xml, /第二行/)
    assert.doesNotMatch(xml, /\{#lines\}|\{text\}/)
    assert.match(
      zip.file('[Content_Types].xml').asText(),
      /simple-parser.xml" ContentType="application\/xml"/
    )
    if (process.env.SIMPLE_WORD_VERIFY_FILE)
      fs.writeFileSync(process.env.SIMPLE_WORD_VERIFY_FILE, Buffer.from(bytes))
  }
})
test('ordinary Word stays on the existing recognition path; invalid ZIP fails', async () => {
  assert.equal(await readSimpleWord(template), undefined)
  await assert.rejects(() => readSimpleWord(new Uint8Array([1, 2, 3]).buffer))
})
test('modified visible Word and corrupted snapshots fail instead of silently re-recognizing', async () => {
  const bytes = await (await exportSimpleWord(template, content())).arrayBuffer()
  const zip = new PizZip(bytes)
  zip.file(
    'word/document.xml',
    zip.file('word/document.xml').asText().replace('自定义工艺流程', '已改动')
  )
  await assert.rejects(() => readSimpleWord(zip.generate({ type: 'arraybuffer' })), /正文已修改/)
  zip.file('customXml/simple-parser.xml', '<snapshot>broken</snapshot>')
  await assert.rejects(() => readSimpleWord(zip.generate({ type: 'arraybuffer' })), /数据损坏/)
})
test('inconsistent editor state and invalid bindings cannot produce a misleading export', async () => {
  const invalid = content()
  invalid.processes[0].craftSelections = { 5: 28 }
  await assert.rejects(() => exportSimpleWord(template, invalid), /工艺绑定无效/)
  const mismatch = content()
  mismatch.recognition.processes = []
  await assert.rejects(() => exportSimpleWord(template, mismatch), /不一致/)
})
