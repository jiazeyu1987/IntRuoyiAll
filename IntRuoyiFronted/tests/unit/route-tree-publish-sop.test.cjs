const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const test = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')

const filename = path.resolve(__dirname, '../../src/views/mes/pro/route/RouteTreePublishPreview.vue')
const { descriptor } = parse(fs.readFileSync(filename, 'utf8'))
const transpile = source => ts.transpileModule(source, { compilerOptions: {
  module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
} }).outputText
const fileNames = { exports: {} }
vm.runInNewContext(transpile(fs.readFileSync(path.resolve(__dirname, '../../src/utils/fileName.ts'), 'utf8')),
  { ...fileNames, URL, window: { location: { origin: 'http://localhost' } } })

function setup(changes) {
  const out = {}
  vm.runInNewContext(transpile(descriptor.scriptSetup.content +
    '\nObject.assign(exports, {parentChanges, matrixNodes, fieldLabel})'), {
    exports: out,
    defineProps: () => ({ preview: { changes, affected: [{ routeId: 2, changes }], blockers: [] } }),
    defineEmits() {},
    require: name => name === 'vue' ? vue : name === '@/utils/fileName' ? fileNames.exports : {}
  })
  return out
}
const sop = name => '/admin-api/infra/file/28/get/mes/route-sop/123/' + encodeURIComponent(name)
const nodes = url => [{ processId: 1, processName: '粗洗工序', sopFileUrl: url }]
const nodeChange = (before, after) => ({ field: 'configSnapshots.flowGraph.nodes', before: nodes(before), after: nodes(after) })

test('publish preview script and template compile', () => {
  compileScript(descriptor, { id: 'sop-preview' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename, id: 'sop-preview' }).errors, [])
})

test('SOP upload shows one named business change in parent and child previews', () => {
  const h = setup([
    { field: 'configSnapshots.craftChangeId', before: 397, after: null },
    nodeChange(null, sop('模拟SOP-仅测试.png'))
  ])
  for (const rows of [h.parentChanges.value, h.matrixNodes.value[0].changes]) {
    assert.equal(rows.length, 1)
    assert.equal(h.fieldLabel(rows[0].field), '工序结构 / 粗洗工序 / SOP附件')
    assert.equal(rows[0].before, null)
    assert.equal(rows[0].after, '模拟SOP-仅测试.png')
    assert.doesNotMatch(JSON.stringify(rows), /admin-api|craftChangeId/)
  }
})

test('internal change records alone do not count as business changes', () => {
  const h = setup([{ field: 'configSnapshots.craftChangeId', before: 397, after: null }])
  assert.equal(h.parentChanges.value.length, 0)
})

test('same-name attachment replacement remains visible and removal keeps the old name', () => {
  const url = sop('作业指导书.pdf')
  for (const after of [url.replace('/123/', '/456/'), null]) {
    const row = setup([nodeChange(url, after)]).parentChanges.value[0]
    assert.equal(row.before, '作业指导书.pdf')
    assert.equal(row.after, after == null ? null : '作业指导书.pdf')
  }
})

test('new process details display attachment names without URLs or internal change records', () => {
  const h = setup([{ field: 'configSnapshots.flowGraph.nodes', before: [],
    after: [{ ...nodes(sop('指导书.pdf'))[0], craftChangeId: 397 }] }])
  const row = h.parentChanges.value[0]
  assert.equal(row.after.SOP附件, '指导书.pdf')
  assert.doesNotMatch(JSON.stringify(row.after), /admin-api|craftChangeId|sopFileUrl/)
})
