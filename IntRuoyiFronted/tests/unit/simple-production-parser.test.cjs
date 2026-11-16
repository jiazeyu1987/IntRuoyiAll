const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const { test } = require('node:test')
const ts = require('typescript')
const root = path.resolve(__dirname, '../..')
const code = ts.transpileModule(
  fs.readFileSync(path.join(root, 'src/views/form-center/parser/simpleRecognition.ts'), 'utf8'),
  {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true
    }
  }
).outputText
const context = { exports: {}, require, crypto: require('node:crypto').webcrypto }
vm.runInNewContext(code, context)
const { checkParameter, parseRecognition, reviewIssues, signature, clone } = context.exports
const parameter = () => ({
  name: '清洗温度',
  referenceValue: '20～30℃',
  ui: { control: 'number', min: 20, max: 30, step: 1, defaultValue: 26, unit: '℃' }
})
const process = () => ({
  id: 'p1',
  sourceName: '粗洗工序',
  value: {
    name: '粗洗工序',
    inputs: [],
    outputs: [],
    equipmentGroups: [
      {
        selectionMode: 'SINGLE',
        equipmentOptions: [{ name: '清洗机', code: 'B09393' }],
        parameters: [parameter()]
      }
    ]
  }
})

test('rejects malformed recognition without replacing it with a default result', () => {
  assert.throws(() => parseRecognition('invalid'))
  assert.throws(() => parseRecognition('{"product":{},"schemaVersion":2}'), /工序结构/)
  assert.throws(
    () =>
      parseRecognition(
        JSON.stringify({
          product: {},
          schemaVersion: 2,
          processes: [
            { name: '粗洗工序', equipmentGroups: [{ parameters: [{ ui: { options: '错误' } }] }] }
          ]
        })
      ),
    /选项结构/
  )
})
test('preserves extra imported metadata and zero values through edits', () => {
  const raw = {
    product: { name: '球囊泵', code: 'IDI-01' },
    schemaVersion: 3,
    extra: '保留字段',
    processes: [process().value]
  }
  raw.processes[0].equipmentGroups[0].parameters[0].ui.defaultValue = 0
  const parsed = parseRecognition(JSON.stringify(raw))
  assert.equal(clone(parsed).extra, '保留字段')
  assert.equal(parsed.processes[0].equipmentGroups[0].parameters[0].ui.defaultValue, 0)
})
test('finds invalid defaults and ranges while accepting zero and decimals', () => {
  const value = parameter()
  assert.equal(checkParameter(value).length, 0)
  value.ui.defaultValue = 300
  assert.match(checkParameter(value).join(), /超出/)
  value.ui.min = 40
  assert.match(checkParameter(value).join(), /下限不能大于上限/)
  Object.assign(value.ui, { min: 0, max: 1, step: 0.1, defaultValue: 0 })
  assert.equal(checkParameter(value).length, 0)
  value.ui.step = 0
  assert.match(checkParameter(value).join(), /步长必须大于零/)
})
test('requires select defaults to belong to options; does not coerce unknown control types', () => {
  const value = {
    name: '介质',
    referenceValue: '纯化水',
    ui: { control: 'select', options: ['纯化水'], defaultValue: '自来水' }
  }
  assert.match(checkParameter(value).join(), /不在下拉选项/)
  value.ui.control = 'unknown'
  assert.match(checkParameter(value).join(), /填写类型/)
})
test('allows same-named processes by default while validating missing device identity', () => {
  const first = process(),
    second = process()
  assert.equal(reviewIssues([first, second]).length, 0)
  second.value.name = '组装工序'
  second.value.equipmentGroups[0].equipmentOptions[0].code = ''
  second.value.equipmentGroups[0].equipmentOptions[0].name = ''
  assert.match(
    reviewIssues([first, second])
      .map((v) => v.message)
      .join(),
    /设备编码|设备编号/
  )
  assert.equal(reviewIssues([first, second]).length, 1)
})
test('editing a reviewed process invalidates its review signature; trial copy cannot mutate template', () => {
  const value = process(),
    reviewed = signature(value),
    trial = clone(value)
  trial.value.equipmentGroups[0].parameters[0].ui.defaultValue = 29
  assert.equal(value.value.equipmentGroups[0].parameters[0].ui.defaultValue, 26)
  value.value.equipmentGroups[0].parameters[0].ui.defaultValue = 28
  assert.notEqual(signature(value), reviewed)
})
