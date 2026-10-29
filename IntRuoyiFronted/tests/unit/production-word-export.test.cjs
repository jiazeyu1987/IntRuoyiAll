const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const PizZip = require('pizzip')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(path.join(root, 'src/views/form-center/parser/productionWordExport.ts'), 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true }
}).outputText
const context = { exports: {}, require }
vm.runInNewContext(compiled, context)
const { createProductionWordCollection } = context.exports
const template = fs.readFileSync(path.join(root, 'src/assets/templates/production-process.docx'))
const mapping = {
  product: { name: '测试产品<&>', code: '001' }, schemaVersion: 2,
  processes: [
    { name: '清洗/工序', criticalProcess: false,
      inputs: [{ name: '物料A', code: '0001' }], outputs: [{ name: '物料B', code: '0002' }],
      equipmentGroups: [{ selectionMode: 'SINGLE', equipmentOptions: [{ name: '清洗机', code: 'EQ01' }],
        parameters: [{ name: '温度', referenceValue: '0–30℃', actualValue: '0',
          ui: { defaultValue: 0, min: 0, max: 30, unit: '℃', options: ['A', 'B'] } }] }] },
    { name: '清洗/工序', criticalProcess: true, equipmentGroups: [] }
  ]
}

test('one DOCX per process, same template, safe unique names, all rows and zero values retained', () => {
  const bytes = createProductionWordCollection(mapping, template, '源文件.docx')
  const zip = new PizZip(bytes)
  const names = Object.keys(zip.files).filter((name) => !zip.files[name].dir)
  assert.equal(names.length, 3)
  assert.ok(zip.file('001-清洗_工序.docx'))
  assert.ok(zip.file('002-清洗_工序.docx'))
  assert.deepEqual(Buffer.from(zip.file('模板/生产工序模板.docx').asUint8Array()), template)
  const original = new PizZip(template)
  for (const name of names.filter((name) => !name.startsWith('模板/'))) {
    const document = new PizZip(zip.file(name).asUint8Array())
    assert.equal(document.file('word/styles.xml').asText(), original.file('word/styles.xml').asText())
    assert.equal(document.file('word/footer1.xml').asText(), original.file('word/footer1.xml').asText())
    const xml = document.file('word/document.xml').asText()
    assert.ok(xml.includes('测试产品&lt;&amp;&gt;'))
    assert.ok(!/\{(?:#|\/)?(?:materials|equipment|parameters|productName|processName)/.test(xml))
    assert.equal((xml.match(/<w:tbl>/g) || []).length, 4)
  }
  const first = new PizZip(zip.file(names[0]).asUint8Array()).file('word/document.xml').asText()
  for (const value of ['物料A', '物料B', '0001', 'EQ01', '默认值：0', '最小值：0', '否']) assert.ok(first.includes(value), value)
  const second = new PizZip(zip.file(names[1]).asUint8Array()).file('word/document.xml').asText()
  assert.ok(!second.includes('物料A'))
  assert.ok(second.includes('是'))
  if (process.env.WORD_EXPORT_QA_DIR) {
    fs.mkdirSync(process.env.WORD_EXPORT_QA_DIR, { recursive: true })
    fs.writeFileSync(path.join(process.env.WORD_EXPORT_QA_DIR, 'sample.docx'), zip.file(names[0]).asUint8Array())
    fs.writeFileSync(path.join(process.env.WORD_EXPORT_QA_DIR, 'empty-process.docx'), zip.file(names[1]).asUint8Array())
  }
})

test('empty process list and invalid template fail without generating a partial archive', () => {
  assert.throws(() => createProductionWordCollection({ ...mapping, processes: [] }, template, ''), /没有可导出的工序/)
  assert.throws(() => createProductionWordCollection(mapping, new Uint8Array([1, 2, 3]), ''))
})

test('many parameters repeat template rows without leaking template fields', () => {
  const many = JSON.parse(JSON.stringify(mapping))
  many.processes[0].equipmentGroups[0].parameters = Array.from({ length: 60 }, (_, i) => ({ name: `参数${i}`, actualValue: String(i) }))
  const zip = new PizZip(createProductionWordCollection(many, template, '长记录.docx'))
  const doc = zip.file('001-清洗_工序.docx').asUint8Array()
  const xml = new PizZip(doc).file('word/document.xml').asText()
  for (let i = 0; i < 60; i++) assert.ok(xml.includes(`参数${i}`))
  if (process.env.WORD_EXPORT_QA_DIR) fs.writeFileSync(path.join(process.env.WORD_EXPORT_QA_DIR, 'long-process.docx'), doc)
})
